package ru.maplyb.printmap.impl.data.network

import retrofit2.http.GET
import retrofit2.http.Path

internal interface Api {

    /**Формат - https://mt0.google.com/vt/lyrs=s&x=1&y=1&z=1
    **/
    //todo: Изменить тип возвращаемого значения
    @GET("{url}")
    suspend fun getMap(
        @Path("url") url: String
    ): ByteArray
}