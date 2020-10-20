package com.exercise.supercom.coronaapp.util

import android.text.format.DateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DateUtils @Inject constructor() {

    @Synchronized
    fun formatDateToApiFormat(
        date: Date = Date(),
        patternOutput: String = "yyyy-MM-dd"
    ): String =
        DateFormat.format(patternOutput, date) as String

}
