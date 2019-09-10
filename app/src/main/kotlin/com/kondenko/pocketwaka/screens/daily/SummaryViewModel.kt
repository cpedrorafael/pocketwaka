package com.kondenko.pocketwaka.screens.daily

import com.kondenko.pocketwaka.domain.daily.model.SummaryUiModel
import com.kondenko.pocketwaka.domain.daily.usecase.GetDefaultSummaryRange
import com.kondenko.pocketwaka.domain.daily.usecase.GetSummary
import com.kondenko.pocketwaka.domain.daily.usecase.GetSummaryState
import com.kondenko.pocketwaka.screens.BaseViewModel
import com.kondenko.pocketwaka.utils.date.DateRange
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.toSingle

class SummaryViewModel(
        private val getDefaultSummaryRange: GetDefaultSummaryRange,
        private val getSummaryState: GetSummaryState
) : BaseViewModel<List<SummaryUiModel>>() {

    private val refreshRate = 1

    private val retryAttempts = 1

    init {
        getSummaryForRange() // For today
    }

    fun getSummaryForRange(range: DateRange? = null) {
        val rangeSource = range?.toSingle() ?: getDefaultSummaryRange()
        disposables += rangeSource.flatMapObservable { range ->
            getSummaryState(GetSummary.Params(range, refreshRate = refreshRate, retryAttempts = retryAttempts))
        }.subscribe(_state::postValue, this::handleError)
    }

}