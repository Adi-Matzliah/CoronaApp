package com.exercise.supercom.coronaapp.feature
import com.exercise.supercom.coronaapp.data.mapper.NetworkMapper
import com.exercise.supercom.coronaapp.network.Covid19Api
import javax.inject.Inject

class RemoteRepository @Inject constructor(private val api: Covid19Api) {

    suspend fun getAvailableCountries() =
        api.getCountries()
            .map { NetworkMapper.CountryMapper.mapFromEntity(it) }

    suspend fun getCountryAllStatusesByPeriod(country: String, fromDate: String, toDate: String) =
        api.getCountryAllStatusesByPeriod(country, fromDate, toDate)
            .map { NetworkMapper.CountryTotalCasesMapper.mapFromEntity(it) }
}


