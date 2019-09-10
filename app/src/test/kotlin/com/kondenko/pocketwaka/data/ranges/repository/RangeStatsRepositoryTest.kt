package com.kondenko.pocketwaka.data.ranges.repository

import com.kondenko.pocketwaka.data.ranges.dao.StatsDao
import com.kondenko.pocketwaka.data.ranges.model.database.StatsDbModel
import com.kondenko.pocketwaka.data.ranges.model.server.Stats
import com.kondenko.pocketwaka.data.ranges.model.server.StatsServerModel
import com.kondenko.pocketwaka.data.ranges.service.RangeStatsService
import com.kondenko.pocketwaka.testutils.TestException
import com.kondenko.pocketwaka.utils.extensions.testWithLogging
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RangeStatsRepositoryTest {

    private val service = mock<RangeStatsService>()

    private val dao = mock<StatsDao>()

    private val serverModelConverter: (RangeStatsRepository.Params, StatsServerModel) -> Maybe<StatsDbModel> = mock()

    private val repository = RangeStatsRepository(
            service,
            dao
    )

    private val token = "token"

    private val range = "7_days"

    private val cachedStats = StatsDbModel(range, 0, false, false, emptyList())

    @Before
    fun setupConverters() {
        whenever(serverModelConverter(any(), any())).doReturn(Maybe.just(cachedStats))
    }

    @Test
    fun `should fetch stats from the database and from the server`() {
        val newStatsResponse = mock<Stats>()
        whenever(dao.getCachedStats(range))
                .doReturn(Maybe.just(cachedStats))
        whenever(service.getCurrentUserStats(token, range))
                .doReturn(Single.just(StatsServerModel(newStatsResponse)))
        whenever(dao.cacheStats(anyOrNull()))
                .doReturn(Completable.complete())
        with(repository.getData(RangeStatsRepository.Params(token, range), serverModelConverter).testWithLogging()) {
            assertNoErrors()
            assertValueAt(0) { it.isFromCache }
            assertValueAt(1) { !it.isFromCache }
            assertComplete()
        }
    }

    @Test
    fun `should show stats from the database when the server returns an error`() {
        whenever(dao.getCachedStats(range))
                .doReturn(Maybe.just(cachedStats))
        whenever(service.getCurrentUserStats(token, range))
                .doReturn(Single.error(TestException()))
        with(repository.getData(RangeStatsRepository.Params(token, range), serverModelConverter).testWithLogging()) {
            assertValue { it.isFromCache }
            assertTerminated()
        }
    }

    @Test
    fun `should only show stats from the server`() {
        val newStatsResponse = StatsServerModel(Stats())
        whenever(dao.getCachedStats(range))
                .doReturn(Maybe.empty())
        whenever(service.getCurrentUserStats(token, range))
                .doReturn(Single.just(newStatsResponse))
        whenever(dao.cacheStats(anyOrNull()))
                .doReturn(Completable.complete())
        with(repository.getData(RangeStatsRepository.Params(token, range), serverModelConverter).testWithLogging()) {
            verify(dao, times(1)).getCachedStats(range)
            inOrder(service, dao) {
                verify(dao).getCachedStats(range)
                verify(service).getCurrentUserStats(token, range)
                verify(dao).cacheStats(anyOrNull())
            }
            assertValue { !it.isFromCache }
            assertComplete()
        }
    }

    @Test
    fun `should return an error when no data is loaded`() {
        val error = TestException()
        whenever(dao.getCachedStats(range))
                .doReturn(Maybe.empty())
        whenever(service.getCurrentUserStats(token, range))
                .doReturn(Single.error(error))
        with(repository.getData(RangeStatsRepository.Params(token, range), serverModelConverter).testWithLogging()) {
            verify(dao, times(1)).getCachedStats(range)
            inOrder(service, dao) {
                verify(dao).getCachedStats(range)
                verify(service).getCurrentUserStats(token, range)
            }
            assertError(error)
            assertNoValues()
            assertTerminated()
        }
    }

    // TODO Ann a test case for empty data

}