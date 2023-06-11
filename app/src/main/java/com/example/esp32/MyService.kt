package com.example.esp32

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*
import android.provider.Settings
class MyService : Service() {
    private val binder = LocalBinder()

    private val CHANNEL_ID = "my_channel"
    private lateinit var mqttClient: MqttClient
    var options = MqttConnectOptions()
    override fun onBind(intent: Intent): IBinder {
        val broker       = "tcp://broker.emqx.io:1883"
        val clientId     = "KotlinMQTTClient2"
        val persistence = MemoryPersistence()
        options.keepAliveInterval = 100000000
        options.isCleanSession = false
        options.isAutomaticReconnect = true
        mqttClient = MqttClient(broker, clientId, persistence)
        mqttClient.connect(options)
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d("MQTT", "Message arrived from topic: $topic, message: ${message.toString()}")
                if(topic.equals("ptcpapesvrwe/esp001/fan/status") && message.toString().equals("on")) {
                    createNotificationChannel()
                    val notificationId = generateUniqueId()

                    val notification = createNotification("Warning!", "High temperature, fan ON!")
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(notificationId, notification)
                }

                val intent = Intent("MqttMessage")
                intent.putExtra("topic", topic)
                intent.putExtra("message", message.toString())
                LocalBroadcastManager.getInstance(this@MyService).sendBroadcast(intent)
            }

            override fun connectionLost(cause: Throwable?) {}

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        return binder
    }


    fun publish(topic: String, message: String) {
        if(::mqttClient.isInitialized) {
            val msg = MqttMessage(message.toByteArray())
            mqttClient.publish(topic, msg)
        }
    }

    fun subscribe(topic: String) {
        Log.d("MQTT", "Sub: $topic")
        if(::mqttClient.isInitialized) {
            mqttClient.subscribe(topic, 1)
        }
    }



    inner class LocalBinder : Binder() {
        fun getService(): MyService = this@MyService
    }

    fun generateUniqueId(): Int {
        return System.currentTimeMillis().toInt()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("oncreate", "test")
        val notificationId = generateUniqueId()
        createNotificationChannel()
        val notification = createNotification("ESP32","Aplikacja działa w tle!")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
        Log.d("oncreate", "test2")

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .build()
    }


}