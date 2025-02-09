package com.example.examrush

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class JavaScriptInterface(private val context: Context, private val webView: WebView) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    private val client = OkHttpClient() // OkHttp client per richieste HTTP

    private fun executeAuthRequest(action: String, email: String, password: String) {
        val task = when (action) {
            "login" -> auth.signInWithEmailAndPassword(email, password)
            "register" -> auth.createUserWithEmailAndPassword(email, password)
            else -> return
        }

        task.addOnCompleteListener { taskResult ->
            if (taskResult.isSuccessful) {
                fetchUserInfo(email) // ✅ Chiamata al server dopo login riuscito
            } else {
                sendResultToWeb(action, "failure")
            }
        }
    }

    @JavascriptInterface
    fun onLogin(email: String, password: String) {
        Log.d("Auth", "Tentativo di login: $email")
        executeAuthRequest("login", email, password)
    }

    @JavascriptInterface
    fun onRegister(email: String, password: String) {
        Log.d("Auth", "Tentativo di registrazione: $email")
        executeAuthRequest("register", email, password)
    }

    private fun fetchUserInfo(email: String) {
        val url = "http://192.168.1.200:5000/api/users"
        val json = JSONObject().put("email", email).toString()

        val mediaType = "application/json".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Auth", "Errore nella richiesta: ${e.message}")
                sendResultToWeb("login", "failure")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("Auth", "Errore HTTP: ${response.code}")
                    sendResultToWeb("login", "failure")
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    sendResultToWeb("login", "failure")
                    return
                }

                val userInfo = JSONObject(responseBody)
                saveUserSession(userInfo)
                sendResultToWeb("login", "success")
            }

        })
    }

    private fun saveUserSession(userInfo: JSONObject) {
        val editor = sharedPreferences.edit()
        editor.putString("user_email", userInfo.getString("email"))
        editor.putString("user_name", userInfo.getString("name"))

        // Controlla se "role" esiste, altrimenti metti un valore predefinito
        val role = if (userInfo.has("role")) userInfo.getString("role") else "user"
        editor.putString("user_role", role)

        editor.apply()
        Log.d("Auth", "Utente salvato in sessione: ${userInfo.getString("email")}")
    }


    @JavascriptInterface
    fun getUserInfo(): String {
        val email = sharedPreferences.getString("user_email", "Nessun utente loggato") ?: "Nessun utente loggato"
        val name = sharedPreferences.getString("user_name", "N/A") ?: "N/A"
        val role = sharedPreferences.getString("user_role", "N/A") ?: "N/A"

        return JSONObject().put("email", email).put("name", name).put("role", role).toString()
    }

    @JavascriptInterface
    fun logoutUser() {
        auth.signOut()
        sharedPreferences.edit().clear().apply()
        Log.d("Auth", "Utente disconnesso e sessione rimossa")
    }

    private fun sendResultToWeb(action: String, result: String) {
        webView.post {
            try {
                webView.evaluateJavascript("onAuthResult('$action', '$result')", null)
            } catch (e: Exception) {
                Log.e("Auth", "Errore nell'invio del risultato a WebView: ${e.message}")
            }
        }
    }
}
