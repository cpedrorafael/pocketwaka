package com.kondenko.pocketwaka.domain.summary.usecase

import android.net.Uri
import com.kondenko.pocketwaka.data.android.DateFormatter
import com.kondenko.pocketwaka.data.branches.DurationsRepository
import com.kondenko.pocketwaka.data.branches.model.DurationsServerModel
import com.kondenko.pocketwaka.data.commits.CommitsRepository
import com.kondenko.pocketwaka.data.commits.model.CommitServerModel
import com.kondenko.pocketwaka.data.common.model.database.StatsEntity
import com.kondenko.pocketwaka.domain.UseCaseObservable
import com.kondenko.pocketwaka.domain.summary.model.Branch
import com.kondenko.pocketwaka.domain.summary.model.Commit
import com.kondenko.pocketwaka.domain.summary.model.Project
import com.kondenko.pocketwaka.utils.SchedulersContainer
import com.kondenko.pocketwaka.utils.date.DateRange
import com.kondenko.pocketwaka.utils.extensions.asHttpException
import com.kondenko.pocketwaka.utils.extensions.concatMapEagerDelayError
import com.kondenko.pocketwaka.utils.rx.mapLatestOnError
import com.kondenko.pocketwaka.utils.rx.scanMap
import com.kondenko.pocketwaka.utils.types.KOptional
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toObservable
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.net.HttpURLConnection

// (secondary) TODO Split up into two use cases for branches and commits
class FetchBranchesAndCommits(
      private val schedulersContainer: SchedulersContainer,
      private val durationsRepository: DurationsRepository,
      private val commitsRepository: CommitsRepository,
      private val dateFormatter: DateFormatter
) : UseCaseObservable<FetchBranchesAndCommits.Params, Project>(schedulersContainer) {

    data class Params(val tokenHeader: String, val date: DateRange.SingleDay, val project: StatsEntity)

    override fun build(params: Params?): Observable<Project> = params?.run {
        fetchBranchesAndCommits(tokenHeader, date.date, project)
    } ?: Observable.error(NullPointerException("Params are null"))

    private fun fetchBranchesAndCommits(token: String, date: LocalDate, projectEntity: StatsEntity): Observable<Project> {
        val projectName = projectEntity.name
              ?: throw NullPointerException("A project with a null name wasn't filtered out: $projectEntity")
        val projectObservable: Observable<Project> = Observable.just(
              Project(
                    projectName,
                    projectEntity.totalSeconds.toLong(),
                    isRepoConnected = true,
                    branches = emptyMap(),
                    repositoryUrl = connectRepoLink(projectName)
              )
        )
        val projectWithBranchesObservable: Observable<Project> = projectObservable.concatMapEagerDelayError { project ->
            getBranches(date, token, projectName)
                  .map { branches ->
                      project.copy(branches = branches.associateBy { it.name })
                  }
                  .toObservable()
        }
        val projectWithCommitsObservable: Observable<Project> = projectWithBranchesObservable.concatMapEagerDelayError { project ->
            Observable.fromIterable(project.branches.values)
                  .concatMapEagerDelayError { branch ->
                      getCommits(date, token, projectName, branch.name)
                            .map { KOptional.of(it) }
                            .scanMap(KOptional.empty()) { prevCommits, newCommits ->
                                branch.copy(commits = (prevCommits orElse emptyList()) + (newCommits orElse emptyList()))
                            }
                  }
                  .map { KOptional.of(it) }
                  .scanMap(KOptional.empty()) { prev, next ->
                      project.branches
                            .toMutableMap()
                            .also { updatedBranches ->
                                fun KOptional<Branch>.addBranch() = item?.let { branch ->
                                    updatedBranches += branch.name to branch
                                }
                                prev.addBranch()
                                next.addBranch()
                            }
                            .let { project.copy(branches = it) }
                  }
        }
        return Observable.concatArrayEagerDelayError(projectObservable, projectWithBranchesObservable, projectWithCommitsObservable)
              .mapLatestOnError { project, throwable ->
                  val noRepoException = throwable.asHttpException() ?: throwable?.let { throw it }
                  val isRepoMissing = noRepoException?.code() == HttpURLConnection.HTTP_FORBIDDEN
                  project.copy(isRepoConnected = !isRepoMissing)
              }
    }

    private fun getBranches(date: LocalDate, token: String, projectName: String): Single<List<Branch>> =
          durationsRepository.getData(DurationsRepository.Params(token, dateFormatter.formatDateAsParameter(date), projectName))
                .subscribeOn(schedulersContainer.workerScheduler)
                .flatMapObservable { durationsModel -> groupByBranches(durationsModel).toObservable() }
                .toList()
                .onErrorReturnItem(emptyList())

    private fun getCommits(date: LocalDate, token: String, projectName: String, branch: String): Observable<List<Commit>> =
          commitsRepository.getData(CommitsRepository.Params(token, projectName, branch))
                .subscribeOn(schedulersContainer.workerScheduler)
                .mapLatestOnError(emptyList()) { list, throwable ->
                    when {
                        throwable.asHttpException()?.code() == HttpURLConnection.HTTP_NOT_FOUND -> emptyList()
                        throwable != null -> throw throwable
                        else -> list
                    }
                }
                .flatMap { it.toObservable() }
                .filter { it.getLocalDate() == date }
                // (secondary) TODO Uncomment and test if this works
                // .takeUntil { it.getLocalDate().isBefore(date) }
                .map { it.toUiModel() }
                .toList()
                .toObservable()

    private fun groupByBranches(durations: DurationsServerModel): List<Branch> =
          durations.branchesData
                .groupBy { it.branch }
                .map { (name, durations) -> name to durations.sumByDouble { it.duration } }
                .filter { (branch, _) -> branch != null }
                .map { (branch: String?, duration) -> Branch(branch!!, duration.toLong(), null) }

    private fun CommitServerModel.toUiModel() = Commit(hash, message, totalSeconds.toLong())

    private fun connectRepoLink(project: String) = "https://wakatime.com/projects/${Uri.encode(project)}/edit"

    private fun CommitServerModel.getLocalDate() = LocalDate.parse(authorDate, DateTimeFormatter.ISO_DATE_TIME)

}