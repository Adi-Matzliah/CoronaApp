package com.exercise.supercom.coronaapp.network
import com.exercise.supercom.coronaapp.network.response.CountryResponse
import com.exercise.supercom.coronaapp.network.response.CountryTotalCasesByDateResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface Covid19Api {

    @GET("countries")
    suspend fun getCountries(): List<CountryResponse>

    @GET("country/{country}")
    suspend fun getCountryAllStatusesByPeriod(@Path("country") country: String, @Query("fromDate") fromDate: String, @Query("toDate") toDate:String): List<CountryTotalCasesByDateResponse>
}

