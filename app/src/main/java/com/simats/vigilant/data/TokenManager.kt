package com.simats.vigilant.data

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences("vigilant_prefs", Context.MODE_PRIVATE)

    fun saveInternalToken(token: String) {
        val editor = prefs.edit()
        editor.putString("ACCESS_TOKEN", token)
        editor.apply()
    }

    fun getToken(): String? {
        return prefs.getString("ACCESS_TOKEN", null)
    }
    
    fun clearToken() {
        prefs.edit().remove("ACCESS_TOKEN").apply()
    }
}
