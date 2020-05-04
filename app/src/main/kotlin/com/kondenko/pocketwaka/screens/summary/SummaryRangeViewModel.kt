package com.kondenko.pocketwaka.screens.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kizitonwose.calendarview.model.CalendarDay
import com.kondenko.pocketwaka.data.android.HumanReadableDateFormatter
import com.kondenko.pocketwaka.utils.WakaLog
import com.kondenko.pocketwaka.utils.date.DateProvider
import com.kondenko.pocketwaka.utils.date.DateRange
import org.threeten.bp.LocalDate
import java.util.concurrent.TimeUnit

class SummaryRangeViewModel(dateProvider: DateProvider, private val dateFormatter: HumanReadableDateFormatter) : ViewModel() {

    private val today = DateRange.SingleDay(dateProvider.getToday())

    private val selectedRange = MutableLiveData<SummaryRangeState>()

    private val titles = MutableLiveData<String>()

    private var startDate: LocalDate? = null

    private var endDate: LocalDate? = null

    init {
        selectDate(today)
    }

    fun dateChanges(): LiveData<SummaryRangeState> = selectedRange

    fun titleChanges(): LiveData<String> = titles

    fun selectDate(dateRange: DateRange) {
        // TODO Limit scrolling by two weeks for free accounts
        titles.value = dateFormatter.format(dateRange)
        if (dateRange is DateRange.SingleDay) dateRange.load()
        else TODO("Load ranges")
    }

    fun onDayClicked(day: CalendarDay) {
        WakaLog.d("Day selected: $day")
        val date = day.date
        if (startDate != null) {
            if (date < startDate || endDate != null) {
                startDate = date
                endDate = null
            } else if (date != startDate) {
                endDate = date
            }
        } else {
            startDate = date
        }
        WakaLog.d("Start date: $startDate")
        WakaLog.d("End date: $endDate")
    }

    fun confirmDateSelection() {
        val startDate = startDate
        val endDate = endDate
        if (startDate == null && endDate == null) return
        selectedRange.value = when {
            (startDate != null) xor (endDate != null) -> {
                DateRange.SingleDay(
                      startDate
                            ?: endDate
                            ?: throw IllegalArgumentException("One of dates is null")
                )
            }
            startDate != null && endDate != null -> {
                DateRange.Range(startDate, endDate)
            }
            else -> {
                throw IllegalArgumentException("Both start date and end date are null")
            }
        }.let { SummaryRangeState(listOf(it)) }
    }

    private fun DateRange.SingleDay.load() {
        val currentValue = selectedRange.value
        if (currentValue == null || date != today.date) {
            WakaLog.d("Loading items around $date")
            val newDates = if (today.date == date) {
                listOf(date.minusDays(2), date.minusDays(1), date)
            } else {
                listOf(date.minusDays(1), date, date.plusDays(1))
            }
                  .map { DateRange.SingleDay(it) }
                  .let { SummaryRangeState(it) }
            selectedRange.postValue(newDates)
        }
    }

}