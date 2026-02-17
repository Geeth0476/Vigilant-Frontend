package com.simats.vigilant.data

import android.content.Context
import java.util.UUID

object DeviceIdManager {
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("vigilant_prefs", Context.MODE_PRIVATE)
        var uuid = prefs.getString("device_uuid", null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", uuid).apply()
        }
        return uuid!!
    }
}
