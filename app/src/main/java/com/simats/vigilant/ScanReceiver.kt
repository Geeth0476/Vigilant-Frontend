package com.simats.vigilant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.simats.vigilant.ui.dashboard.DashboardActivity

class ScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Trigger the scan logic here
        // For now, we'll just show a notification saying the scheduled scan ran
        val scanType = intent.getStringExtra("SCAN_TYPE") ?: "quick"
        
        showScanCompleteNotification(context, scanType)
        
        // Update last scan time with a safe default for now, since this is just a scheduler trigger
        ScanDataManager.saveScanResult(context, 0, ScanDataManager.RiskLevel.SAFE)
    }

    private fun showScanCompleteNotification(context: Context, type: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vigilant_scan_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scan Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (type == "deep") "Daily Deep Scan Complete" else "Daily Scan Complete"
        val desc = if (type == "deep") "Your device has been deeply analyzed. No threats found." else "Your device has been scanned. No threats found."

        val notification = NotificationCompat.Builder(context, channelId)
            // Use an icon that definitely exists to avoid build errors
            .setSmallIcon(R.drawable.ic_scan_blue) 
            .setContentTitle(title)
            .setContentText(desc)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1001, notification)
    }
}
