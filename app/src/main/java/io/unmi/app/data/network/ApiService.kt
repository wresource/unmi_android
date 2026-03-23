package io.unmi.app.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit API interface for all HTTP requests.
 * Using suspend functions for coroutine integration.
 */
interface ApiService {

    // Nazhumi price API
    @GET("https://www.nazhumi.com/api/v1")
    suspend fun getNazhumiPrices(
        @Query("domain") domain: String,
        @Query("order") order: String = "renew"
    ): Response<ResponseBody>

    // IANA RDAP bootstrap
    @GET("https://data.iana.org/rdap/dns.json")
    suspend fun getRdapBootstrap(): Response<ResponseBody>

    // Dynamic RDAP domain query (full URL)
    @GET
    suspend fun queryRdap(
        @Url url: String,
        @Header("Accept") accept: String = "application/rdap+json, application/json"
    ): Response<ResponseBody>
}
