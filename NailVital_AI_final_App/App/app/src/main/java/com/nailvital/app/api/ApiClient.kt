package com.nailvital.app.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface NailVitalApi {
    
    @POST("register")
    suspend fun register(@Body user: Map<String, String>): Any

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") pass: String
    ): LoginResponse

    @Multipart
    @POST("scan")
    suspend fun analyzeNail(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Query("finger") finger: String
    ): ScanResponse

    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null
    ): List<ScanResponse>

    @DELETE("scans/{id}")
    suspend fun deleteScan(
        @Header("Authorization") token: String,
        @Path("id") scanId: Int
    ): okhttp3.ResponseBody

    @Streaming
    @GET("scans/{id}/export-pdf")
    suspend fun exportScan(
        @Header("Authorization") token: String,
        @Path("id") scanId: Int
    ): Response<ResponseBody>

    @POST("verify-otp")
    suspend fun verifyOtp(
        @Query("email") email: String,
        @Query("otp") otp: String
    ): LoginResponse

    @POST("resend-otp")
    suspend fun resendOtp(
        @Query("email") email: String
    ): Any

    @POST("forgot-password")
    suspend fun forgotPassword(
        @Query("email") email: String
    ): Any

    @POST("reset-password")
    suspend fun resetPassword(
        @Query("email") email: String,
        @Query("otp") otp: String,
        @Query("new_password") newPass: String
    ): Any

    @GET("users/me")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserResponse

    @PUT("users/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body updates: Map<String, String?>
    ): UserResponse

    @HTTP(method = "DELETE", path = "users/me", hasBody = true)
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Body request: DeleteAccountReq
    ): Response<ResponseBody>

    @Streaming
    @GET("history/export-pdf")
    suspend fun exportGlobalHistoryPdf(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @GET("users/me/export-data")
    suspend fun exportData(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("chat")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): ChatResponse

    @POST("voice-command")
    suspend fun voiceCommand(
        @Header("Authorization") token: String,
        @Body request: VoiceCommandRequest
    ): VoiceCommandResponse
}

data class DeleteAccountReq(val password: String)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user: UserResponse
)

data class Finding(
    val result_class: String,
    val display_name: String? = null,
    val description: String? = null,
    val recommendation: String? = null,
    val confidence: Float
)

data class ScanResponse(
    val id: Int,
    val image_path: String,
    val result_class: String,
    val display_name: String? = null,
    val description: String? = null,
    val confidence: Float,
    val finger: String? = null,
    val recommendation: String? = null,
    val findings: List<Finding> = emptyList(),
    val created_at: String
)

data class UserResponse(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val height: String? = null,
    val created_at: String
)

data class ChatRequest(
    val message: String
)

data class ChatResponse(
    val reply: String
)

data class VoiceCommandRequest(
    val message: String
)

data class VoiceCommandItem(
    val type: String,
    val target: String
)

data class VoiceCommandResponse(
    val action_type: String,
    val message: String? = null,
    val target: String? = null,
    val commands: List<VoiceCommandItem>? = null
)

object ApiClient {
    // Development URL (Your local IP) - switch to PROD_URL when deploying
    // NOTE: If using Android Emulator, change this to "http://10.0.2.2:8001/"
    private const val DEV_URL = "http://10.205.245.73:8000/"

    // Production URL - fill this in after deploying to Render/Hugging Face
    private const val PROD_URL = "https://your-production-url-here.com/"

    // Currently using DEV for local testing
    const val BASE_URL = DEV_URL

    // OkHttp client with extended timeouts for AI vision processing
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val instance: NailVitalApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(NailVitalApi::class.java)
    }
}
