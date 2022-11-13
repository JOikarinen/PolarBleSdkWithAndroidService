package com.example.polarblesdkwithandroidservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import java.util.*

class ExampleService : Service() {

    private lateinit var polarBleApi: PolarBleApi

    private lateinit var polarId: String

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    companion object {
        private const val SERVICE_ID = 875642
        const val POLAR_ID = "key.polar.id"
        private const val CHANNEL_ID = "channel_id"
        private const val CHANNEL_NAME = "channel_name"
        private const val CHANNEL_DESCRIPTION = "channel_description"
        private const val TAG = "HeartbeatService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SERVICE_ID, serviceNotification())

        polarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
            applicationContext, PolarBleApi.ALL_FEATURES)

        if (intent != null) {
            polarId = intent.getStringExtra(POLAR_ID).toString()
        }
        connectPolar(polarId)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun serviceNotification(): Notification {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val name = CHANNEL_NAME
        val descriptionText = CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setContentTitle("HearBeat service")
            .setContentText("Heartbeat service in execution")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    }

    private fun connectPolar(polarId: String) {
        polarBleApi.run {
            setApiLogger { s: String? -> Log.d(TAG, "POLAR BLE API LOGGER $s") }
            connectToDevice(polarId)
            setAutomaticReconnection(true)
            setPolarCallback()
        }
    }

    private fun setPolarCallback() {
        polarBleApi.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId)
            }

            override fun streamingFeaturesReady(
                identifier: String,
                features: Set<PolarBleApi.DeviceStreamingFeature>
            ) {
                for (feature in features) {
                    Log.d(TAG, "Streaming feature $feature is ready")
                }
            }

            override fun hrFeatureReady(identifier: String) {
                Log.d(TAG, "HR READY: $identifier")
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {}
            override fun batteryLevelReceived(identifier: String, level: Int) {}
            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                Log.d(TAG, "HR: " + data.hr)
            }

            override fun polarFtpFeatureReady(s: String) {}
        })
    }
}
