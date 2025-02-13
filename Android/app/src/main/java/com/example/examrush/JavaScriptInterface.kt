package com.example.examrush

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.MediaStore
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class JavaScriptInterface(private val context: Context, private val webView: WebView) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    private val client = OkHttpClient() // OkHttp client per richieste HTTP
    val urltarget: String = "http://192.168.0.10:5000"


    var deckJson : String = ""// Decks in ArrayJSON
    var currentDeck : String = "" // Cards in JSon


    private fun executeAuthRequest(action: String, email: String, password: String) {
        val task = when (action) {
            "login" -> auth.signInWithEmailAndPassword(email, password)
            "register" -> auth.createUserWithEmailAndPassword(email, password)
            else -> return
        }

        task.addOnCompleteListener { taskResult ->
            if (taskResult.isSuccessful) {
                fetchUserInfo(email) // ✅ Chiamata al server dopo login riuscito
                fetchDecks()
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
        val url = urltarget + "/api/users"
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
        editor.putString("image", userInfo.getString("image"))

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
        val image = sharedPreferences.getString("image", "../images/user.png") ?: "../images/user.png"

        return JSONObject().put("email", email).put("name", name).put("role", role).put("image", image).toString()
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

    // Method to trigger the image picker
    @JavascriptInterface
    fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        (context as Activity).startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
    }

    companion object {
        const val IMAGE_PICK_REQUEST_CODE = 1001
    }




    @JavascriptInterface
    fun getAllDecks(): String{
        Log.d("JSONdeck", deckJson)
        return deckJson
    }

    fun fetchDecks() {
    val url = urltarget + "/api/decks"

    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("FetchDecks", "Error in request: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                Log.e("FetchDecks", "HTTP Error: ${response.code}")
                return
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Log.e("FetchDecks", "Empty response body")
                return
            }

            try {
                val decksArray = JSONArray(responseBody)
                deckJson = decksArray.toString()
                Log.d("First JSONdeck", "Decks fetched: $deckJson")
            } catch (e: Exception) {
                Log.e("FetchDecks", "Error parsing JSON: ${e.message}")
            }
        }
    })

    }

    @JavascriptInterface
    fun getDeckByTitle(base64Title: String) {
        Log.d("DeckByTitle", "Sono dentro")
        val url = urltarget + "/api/decks/" + base64Title

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FetchDecksByTitle", "Error in request: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("FetchDecksByTitle", "HTTP Error: ${response.code}")
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    Log.e("FetchDecksByTitle", "Empty response body")
                    return
                }

                try {
                    val cDeck = JSONObject(responseBody)
                    currentDeck = cDeck.toString()
                    Log.d("FetchDecksByTitle", "Decks fetched: $currentDeck")
                } catch (e: Exception) {
                    Log.e("FetchDecksByTitle", "Error parsing JSON: ${e.message}")
                }
            }
        })

    }

    @JavascriptInterface
    fun getDeck(): String {
        Log.d("CurrentDeck", currentDeck)
        return currentDeck
    }


}
