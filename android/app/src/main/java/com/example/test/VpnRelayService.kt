package com.example.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileDescriptor

class VpnRelayService : VpnService() {
    private var tunInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()

        val builder = Builder()
            .setSession("Testt Relay")
            .setMtu(1500)
            .addAddress("10.8.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .addDnsServer("1.1.1.1")

        tunInterface = builder.establish()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "vpn_relay_channel"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Relay VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Testt Relay")
            .setContentText("VPN relay is active")
            .setSmallIcon(android.R.drawable.stat_sys_data_connected)
            .build()

        startForeground(101, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        tunInterface?.close()
    }
}
