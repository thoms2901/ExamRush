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
    val urltarget: String = "http://192.168.1.200:5000"

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
    fun onRegister(firstName: String, lastName: String, email: String, password: String) {
        Log.d("Auth", "Tentativo di registrazione: $email")
        executeAuthRequest("register", email, password)
        val body = JSONObject().apply {
            put("firstName", firstName)
            put("lastName", lastName)
            put("email", email)
            put("password", password)
        }

        makeHttpRequest("POST", "/api/register", body) { success, response ->
            if (!success || response.isNullOrEmpty()) {
                sendResultToWeb("register", "failure")
            } else {
                val userInfo = JSONObject(response)
                saveUserSession(userInfo)
                sendResultToWeb("register", "success")
            }
        }
    }

    @JavascriptInterface
    fun onDeleteUser(userId: String) {
        Log.d("Auth", "Tentativo di eliminazione utente: $userId")
        val user = auth.currentUser

        if (user == null) {
            Log.e("Auth", "❌ Nessun utente attualmente autenticato in Firebase.")
            sendResultToWeb("delete_user", "failure")
            return
        }

        Log.d("Auth", "Utente attuale Firebase UID: ${user.uid}, UserID ricevuto: $userId")


        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Auth", "✅ Utente eliminato da Firebase")

                    // Dopo aver rimosso da Firebase, elimina dal database
                    makeHttpRequest("DELETE", "/api/users/$userId", null) { success, response ->
                        if (!success || response.isNullOrEmpty()) {
                            Log.e("Auth", "❌ Errore nella cancellazione dal DB")
                            sendResultToWeb("delete_user", "failure")
                        } else {
                            Log.d("Auth", "✅ Utente eliminato dal database")
                            sendResultToWeb("delete_user", "success")
                        }
                    }
                } else {
                    Log.e("Auth", "❌ Errore eliminazione Firebase: ${task.exception?.message}")
                    sendResultToWeb("delete_user", "failure")
                }
            }

    }



    private fun makeHttpRequest(method: String, path: String, body: JSONObject?, callback: (Boolean, String?) -> Unit) {
        val url = "$urltarget$path"
        val mediaType = "application/json".toMediaType()
        val requestBody = body?.toString()?.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(url)

        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBody?.let { requestBuilder.post(it) }
            "PUT" -> requestBody?.let { requestBuilder.put(it) }
            "DELETE" -> requestBody?.let { requestBuilder.delete(it) } ?: requestBuilder.delete()
            else -> {
                Log.e("HTTP", "Metodo non supportato: $method")
                callback(false, "Metodo non supportato")
                return
            }
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Errore nella richiesta: ${e.message}")
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e("HTTP", "Errore HTTP: ${response.code}")
                    callback(false, responseBody)
                    return
                }
                callback(true, responseBody)
            }
        })
    }

    private fun fetchUserInfo(email: String) {
        val body = JSONObject().put("email", email)
        makeHttpRequest("POST", "/api/users", body) { success, response ->
            if (!success || response.isNullOrEmpty()) {
                sendResultToWeb("login", "failure")
            } else {
                val userInfo = JSONObject(response)
                saveUserSession(userInfo)
                sendResultToWeb("login", "success")
            }
        }
    }

    private fun saveUserSession(userInfo: JSONObject) {
        val editor = sharedPreferences.edit()
        editor.putString("user_info", userInfo.toString())
        editor.apply()
    }



    @JavascriptInterface
    fun getUserInfo(): String {
        val userInfoString = sharedPreferences.getString("user_info", "{}") ?: "{}"

        val userInfo = JSONObject(userInfoString)
        userInfo.put("urltarget", urltarget)

        Log.e("DEBUG", "Inviati valori: $userInfo")

        return userInfo.toString()
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


    @JavascriptInterface
    fun uploadProfileImageFromJS(base64Image: String) {
        val userInfoString = getUserInfo()
        val userInfo = JSONObject(userInfoString)

        val userId = userInfo.optString("user_id", "")
        if (userId.isEmpty()) {
            Log.e("ImageUpload", "❌ Errore: user_id non trovato")
            webView.post {
                webView.evaluateJavascript("onUploadFailed('User ID non trovato')", null)
            }
            return
        }

        val url = "/api/users/$userId/profile-image"

        val jsonBody = JSONObject().apply {
            put("image", "data:image/jpeg;base64,$base64Image")
        }

        makeHttpRequest("POST", url, jsonBody) { success, response ->
            webView.post {
                if (success) {
                    Log.i("ImageUpload", "✅ Immagine caricata con successo")
                    webView.evaluateJavascript("onUploadSuccess()", null)
                } else {
                    Log.e("ImageUpload", "❌ Errore nel caricamento dell'immagine: $response")
                    webView.evaluateJavascript("onUploadFailed('${response ?: "Errore sconosciuto"}')", null)
                }
            }
        }
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
        makeHttpRequest("GET", "/api/decks", null) { success, response ->
            if (!success || response.isNullOrEmpty()) {
                Log.e("FetchDecks", "Error fetching decks")
            } else {
                try {
                    val decksArray = JSONArray(response)
                    deckJson = decksArray.toString()
                    Log.d("First JSONdeck", "Decks fetched: $deckJson")
                } catch (e: Exception) {
                    Log.e("FetchDecks", "Error parsing JSON: ${e.message}")
                }
            }
        }
    }

    @JavascriptInterface
    fun getDeckByTitle(base64Title: String) {
        makeHttpRequest("GET", "/api/decks/$base64Title", null) { success, response ->
            if (!success || response.isNullOrEmpty()) {
                Log.e("FetchDecksByTitle", "Error fetching deck by title")
            } else {
                try {
                    val cDeck = JSONObject(response)
                    currentDeck = cDeck.toString()
                    Log.d("FetchDecksByTitle", "Deck fetched: $currentDeck")
                } catch (e: Exception) {
                    Log.e("FetchDecksByTitle", "Error parsing JSON: ${e.message}")
                }
            }
        }
    }

    @JavascriptInterface
    fun getDeck(): String {
        Log.d("CurrentDeck", currentDeck)
        return currentDeck
    }
}
