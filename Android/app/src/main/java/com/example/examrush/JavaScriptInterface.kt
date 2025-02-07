package com.example.examrush

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface

class JavaScriptInterface(private val context: Context) {

    @JavascriptInterface
    fun onLogin(username: String, password: String): String {
        Log.d("Login", "Login attempt: $username with password $password")

        return if (username == "test" && password == "password") {
            "success"
        } else {
            "failure"
        }
    }

    @JavascriptInterface
    fun onRegister(username: String, email: String, password: String): String {
        Log.d("Registration", "Registration attempt: $username, $email with password $password")

        return if (password.length >= 6) {
            "success"
        } else {
            "failure"
        }
    }

    @JavascriptInterface
    fun getUser(): String {
        return "Test"
    }
}
