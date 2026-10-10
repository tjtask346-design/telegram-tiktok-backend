package com.aim.earny.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @Multipart
    @POST("upload")
    suspend fun upload(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("caption") caption: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part("width") width: RequestBody,
        @Part("height") height: RequestBody
    ): UploadResponse

    @POST("view/{videoId}")
    suspend fun incrementView(
        @Header("Authorization") auth: String,
        @Path("videoId") videoId: String
    )

    @Multipart
    @POST("profile-pic")
    suspend fun uploadProfilePic(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part
    ): UploadResponse
}
