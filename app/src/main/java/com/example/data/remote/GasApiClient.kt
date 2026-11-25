package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GasResponse(
    val status: String,
    val message: String = "",
    val code: String = "",
    val raw: JsonObject
) {
    val isOk: Boolean get() = status == "ok" || status == "found"
    val isNetworkError: Boolean
        get() = status == "timeout" ||
            (status == "error" && Regex("fetch error|network|timeout|empty response|failed to connect|unable to resolve|socket", RegexOption.IGNORE_CASE).containsMatchIn(message))
}

class GasApiClient(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val longClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun get(baseUrl: String, params: Map<String, String>, longTimeout: Boolean = false): GasResponse = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext errorResponse("network", "इंटरनेट उपलब्ध नाही (Offline).")
        }
        val parsed = baseUrl.toHttpUrlOrNull()
            ?: return@withContext errorResponse("nourl", "Google Sheets URL अयोग्य आहे.")
        val urlBuilder = parsed.newBuilder()
        params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        urlBuilder.addQueryParameter("t", System.currentTimeMillis().toString())

        val req = Request.Builder().url(urlBuilder.build()).get().build()
        executeRequest(req, if (longTimeout) longClient else client)
    }

    suspend fun post(baseUrl: String, params: Map<String, String>, longTimeout: Boolean = false): GasResponse = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext errorResponse("network", "इंटरनेट उपलब्ध नाही (Offline).")
        }
        if (baseUrl.toHttpUrlOrNull() == null) {
            return@withContext errorResponse("nourl", "Google Sheets URL अयोग्य आहे.")
        }
        val formBuilder = FormBody.Builder()
        params.forEach { (k, v) -> formBuilder.add(k, v) }
        val req = Request.Builder().url(baseUrl).post(formBuilder.build()).build()
        executeRequest(req, if (longTimeout) longClient else client)
    }

    private fun executeRequest(request: Request, activeClient: OkHttpClient = client): GasResponse {
        return try {
            activeClient.newCall(request).execute().use { resp ->
                val body = resp.body?.string()?.trim().orEmpty()
                if (body.isEmpty()) {
                    return errorResponse("error", "Empty response (HTTP ${resp.code})")
                }
                val cleanJson = stripJsonpWrapper(body)
                val obj = try {
                    json.parseToJsonElement(cleanJson).jsonObject
                } catch (_: Exception) {
                    return if (resp.isSuccessful) {
                        GasResponse(
                            status = "ok",
                            message = cleanJson.take(120),
                            raw = buildJsonObject {
                                put("status", "ok")
                                put("message", cleanJson.take(120))
                            }
                        )
                    } else {
                        errorResponse("error", "HTTP ${resp.code}: ${cleanJson.take(120)}")
                    }
                }
                val status = obj.str("status").ifBlank { if (resp.isSuccessful) "ok" else "error" }
                val message = obj.str("message")
                val code = obj.str("code")
                GasResponse(status = status, message = message, code = code, raw = obj)
            }
        } catch (e: IOException) {
            errorResponse("timeout", "Network error: ${e.message ?: "Timeout"}")
        } catch (e: Exception) {
            errorResponse("error", e.message ?: "Request failed")
        }
    }

    private fun stripJsonpWrapper(txt: String): String {
        val m = Regex("""^[^(]+\((.+)\);?\s*$""", RegexOption.DOT_MATCHES_ALL).find(txt)
        return m?.groupValues?.getOrNull(1) ?: txt
    }

    private fun errorResponse(status: String, msg: String): GasResponse =
        GasResponse(
            status = status,
            message = msg,
            raw = buildJsonObject {
                put("status", status)
                put("message", msg)
            }
        )
}

fun JsonObject.str(key: String): String {
    val el = this[key] ?: return ""
    return (el as? JsonPrimitive)?.content ?: el.toString().trim('"')
}

fun JsonObject.intVal(key: String, default: Int = 0): Int = str(key).toDoubleOrNull()?.toInt() ?: default
fun JsonObject.doubleVal(key: String, default: Double = 0.0): Double = str(key).toDoubleOrNull() ?: default
fun JsonObject.boolVal(key: String): Boolean = str(key).equals("true", ignoreCase = true)
