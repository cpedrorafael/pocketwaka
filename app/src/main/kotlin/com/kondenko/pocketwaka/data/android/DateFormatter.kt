package com.kondenko.pocketwaka.data.android

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import com.kondenko.pocketwaka.utils.extensions.getCurrentLocale
import com.kondenko.pocketwaka.utils.extensions.secondsToHoursAndMinutes
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class DateFormatter(private val context: Context, private val stringProvider: StringProvider) {

    @SuppressLint("SimpleDateFormat")
    private val paramDateFormat = SimpleDateFormat("yyyy-MM-dd")

    enum class Format {
        Short, Long
    }

    fun formatDateAsParameter(date: LocalDate): String =
          date.format(DateTimeFormatter.ofPattern(paramDateFormat.toPattern()))

    fun parseDateParameter(date: String): Long? {
        return try {
            paramDateFormat.parse(date).time
        } catch (e: ParseException) {
            Timber.w("Couldn't parse date $date")
            null
        }
    }

    fun formatDateForDisplay(date: String): String {
        val locale = context.getCurrentLocale()
        val dateMillis = SimpleDateFormat("yyyy-MM-dd", locale).parse(date).time
        return DateUtils.formatDateTime(
              context,
              dateMillis,
              DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        )
    }

    fun formatDateForDisplay(seconds: Long, includeYear: Boolean = true): String =
          DateUtils.formatDateTime(
                context,
                TimeUnit.SECONDS.toMillis(seconds),
                (if (includeYear) DateUtils.FORMAT_SHOW_YEAR else DateUtils.FORMAT_NO_YEAR) or DateUtils.FORMAT_ABBREV_MONTH
          )

    /**
     * A shorthand version of [secondsToHumanReadableTime] which uses [Format.Long] by default.
     * This overload is necessary to keep supporting method references to this method.
     */
    fun secondsToHumanReadableTime(seconds: Number): String = secondsToHumanReadableTime(seconds, Format.Long)

    fun secondsToHumanReadableTime(seconds: Number, format: Format = Format.Long): String {
        val (hours, minutes) = seconds.secondsToHoursAndMinutes()
        return toHumanReadableTime(hours.toInt(), minutes.toInt(), format)
    }

    fun toHumanReadableTime(hours: Int, minutes: Int, format: Format = Format.Long): String {
        val templateHours = when (format) {
            Format.Long -> stringProvider.getHoursTemplate(hours)
            Format.Short -> stringProvider.getHoursTemplateShort(hours)
        }
        val templateMinutes = when (format) {
            Format.Long -> stringProvider.getMinutesTemplate(minutes)
            Format.Short -> stringProvider.getMinutesTemplateShort(minutes)
        }
        val timeBuilder = StringBuilder()
        if (hours > 0) timeBuilder.append(templateHours.format(hours)).append(' ')
        if (minutes > 0) timeBuilder.append(templateMinutes.format(minutes))
        return timeBuilder.toString()
    }

}