package com.kondenko.pocketwaka.di.modules

import com.kondenko.pocketwaka.data.summary.converters.SummaryResponseConverter
import com.kondenko.pocketwaka.data.summary.converters.TimeTrackedConverter
import com.kondenko.pocketwaka.data.summary.repository.SummaryRepository
import com.kondenko.pocketwaka.domain.auth.MockGetTokenHeaderValue
import com.kondenko.pocketwaka.domain.summary.usecase.FetchProjects
import com.kondenko.pocketwaka.domain.summary.usecase.GetAverage
import com.kondenko.pocketwaka.domain.summary.usecase.GetSummary
import org.koin.dsl.module

val mockSummaryModule = module(override = true) {
    single {
        GetAverage(
              schedulersContainer = get(),
              statsRepository = get(),
              getTokenHeaderValue = get<MockGetTokenHeaderValue>()
        )
    }
    single {
        GetSummary(
              schedulers = get(),
              summaryRepository = get<SummaryRepository>(),
              getTokenHeader = get<MockGetTokenHeaderValue>(),
              dateFormatter = get(),
              summaryResponseConverter = get<SummaryResponseConverter>(),
              timeTrackedConverter = get<TimeTrackedConverter>(),
              fetchProjects = get<FetchProjects>()
        )
    }
}