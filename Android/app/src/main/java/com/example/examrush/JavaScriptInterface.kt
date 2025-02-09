package com.example.examrush

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.firebase.auth.FirebaseAuth

class JavaScriptInterface(private val context: Context, private val webView: WebView) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private fun executeAuthRequest(action: String, email: String, password: String) {
        val task = when (action) {
            "login" -> auth.signInWithEmailAndPassword(email, password)
            "register" -> auth.createUserWithEmailAndPassword(email, password)
            else -> return
        }

        task.addOnCompleteListener { taskResult ->
            val result = if (taskResult.isSuccessful) "success" else "failure"
            sendResultToWeb(action, result)
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

    @JavascriptInterface
    fun getUser(): String {
        return auth.currentUser?.email ?: "Nessun utente loggato"
    }

    private fun sendResultToWeb(action: String, result: String) {
        webView.post {
            webView.evaluateJavascript("onAuthResult('$action', '$result')", null)
        }
    }
}
