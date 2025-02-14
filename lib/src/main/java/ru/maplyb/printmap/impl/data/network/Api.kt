package ru.maplyb.printmap.impl.data.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

internal interface Api {

    /**Формат - https://mt0.google.com/vt/lyrs=s&x=1&y=1&z=1
    **/
    //todo: Изменить тип возвращаемого значения
    @GET()
    suspend fun getMap(
        @Url url: String,
    ): ResponseBody
}