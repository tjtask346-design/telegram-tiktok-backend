package com.aim.earny.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    @Multipart
    @POST("upload")
    suspend fun upload(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("caption") caption: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part("width") width: RequestBody,
        @Part("height") height: RequestBody,
    ): UploadResponse

    @POST("view/{id}")
    suspend fun view(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
    )
}
