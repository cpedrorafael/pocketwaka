package com.kondenko.pocketwaka.screens.stats

import com.kondenko.pocketwaka.data.stats.model.database.StatsDbModel
import com.kondenko.pocketwaka.domain.StatefulUseCase
import com.kondenko.pocketwaka.domain.stats.model.StatsUiModel
import com.kondenko.pocketwaka.domain.stats.usecase.GetStatsForRange
import com.kondenko.pocketwaka.screens.base.BaseViewModel
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.plusAssign
import java.util.concurrent.TimeUnit

class StatsViewModel(
      private val range: String?,
      private val getStats: StatefulUseCase<GetStatsForRange.Params, List<StatsUiModel>, StatsDbModel>,
      private val uiScheduler: Scheduler
) : BaseViewModel<List<StatsUiModel>>() {

    init {
        update()
    }

    fun update() {
        disposables += getStats
                .build(GetStatsForRange.Params(range, refreshRate = 3))
                .debounce(50, TimeUnit.MILLISECONDS, uiScheduler)
                .subscribe(::setState, this::handleError)
    }

}