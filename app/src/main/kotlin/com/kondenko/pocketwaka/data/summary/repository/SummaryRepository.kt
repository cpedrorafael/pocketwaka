package com.kondenko.pocketwaka.data.summary.repository

import com.kondenko.pocketwaka.data.ContinuousCacheBackedRepository
import com.kondenko.pocketwaka.data.summary.dao.SummaryDao
import com.kondenko.pocketwaka.data.summary.model.database.SummaryDbModel
import com.kondenko.pocketwaka.data.summary.model.server.Summary
import com.kondenko.pocketwaka.data.summary.service.SummaryService
import com.kondenko.pocketwaka.utils.date.DateRange
import com.kondenko.pocketwaka.utils.date.DateRangeString
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.toObservable

class SummaryRepository(
        summaryService: SummaryService,
        private val summaryDao: SummaryDao,
        workerScheduler: Scheduler,
        reduceModels: (Params, SummaryDbModel, SummaryDbModel) -> SummaryDbModel
) : ContinuousCacheBackedRepository<SummaryRepository.Params, Summary, SummaryDbModel>(
        workerScheduler = workerScheduler,
        serverDataProvider = { (tokenHeader, _, range, project, branches): Params ->
            // TODO Reduce summaries that span over several dates
            summaryService.getSummaries(tokenHeader, range.start, range.end, project, branches)
        },
        continuousCachedDataProvider = {
            it.dateRange.run {
                summaryDao
                        .getSummaries(start.toEpochDay(), end.toEpochDay())
                        .flatMapObservable { it.toObservable() }
            }
        },
        reduceModels = reduceModels
) {

    data class Params(
            val tokenHeader: String,
            val dateRange: DateRange,
            val dateRangeString: DateRangeString,
            val project: String? = null,
            val branches: String? = null
    )

    override fun cacheData(data: SummaryDbModel) = summaryDao.cacheSummary(data)

    override fun setIsFromCache(model: SummaryDbModel, isFromCache: Boolean): SummaryDbModel = model.copy(isFromCache = isFromCache)

}