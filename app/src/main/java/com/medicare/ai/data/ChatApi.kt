package com.medicare.ai.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ChatMsg(val role: String, val content: String)
data class ChatReq(val messages: List<ChatMsg>)
data class ChatRes(val reply: String?, val emergency: Boolean? = false, val error: String? = null)

interface ChatApi {
    @POST("api/chat")
    suspend fun chat(@Header("Authorization") bearer: String, @Body body: ChatReq): ChatRes
}