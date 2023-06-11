package com.example.esp32

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.NumberPicker
import android.widget.NumberPicker.OnValueChangeListener
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var loaderDialog: Dialog
    private var bound = false
    private lateinit var myService: MyService
    lateinit var temperature: TextView
    lateinit var humidity: TextView
    lateinit var switchText: TextView
    lateinit var fanSwitch: Switch
    lateinit var autoFan: Switch
    lateinit var save: Button
    lateinit var picker1: NumberPicker
    lateinit var espId: String
    lateinit var newCard: Button
    lateinit var topicSensorTemperature: String
    lateinit var topicSensorHumidity: String
    lateinit var topicFanStatus: String
    lateinit var topicFanAuto: String
    lateinit var topicMaxTemperature: String
    lateinit var topicNewCardStatus: String
    lateinit var topicNewCardGet: String
    var alertDialog: AlertDialog? = null
    var cancelButton: Button? = null
    val client = OkHttpClient()
    val link = Helper.link


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MyService.LocalBinder
            myService = binder.getService()
            bound = true
            subscribeTopic(topicSensorTemperature)
            subscribeTopic(topicSensorHumidity)
            subscribeTopic(topicFanStatus)
            subscribeTopic(topicMaxTemperature)
            subscribeTopic(topicNewCardGet)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val topic = intent.getStringExtra("topic")
            val message = intent.getStringExtra("message")
            if (topic.equals(topicNewCardGet)) {
                alertDialog?.dismiss()
                timer.cancel()
            }
            updateUI(topic, message)
        }
    }


    override fun onResume() {

        super.onResume()


        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, IntentFilter("MqttMessage"))
    }
    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }


    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }

    fun publishMessage(topic: String, message: String) {
        if (bound) {
            myService.publish(topic, message)
        }
    }

    fun subscribeTopic(topic: String) {
        if (bound) {
            myService.subscribe(topic)
        }
    }

    fun updateUI(topic: String?, message: String?) {
        runOnUiThread {
            when (topic) {
                topicSensorTemperature -> {

                    temperature.text = "Temperature: $message"

                }
                topicSensorHumidity -> {
                    humidity.text = "Humidity: $message"
                }
                topicFanStatus -> {
                    fanSwitch.isChecked = message == "on"
                    switchText.text = "Fan status: " + message
                }
                topicMaxTemperature -> {
                    picker1.value = message?.toInt() ?: 18
                }
            }
        }
    }



    @SuppressLint("SetTextI18n", "UseSwitchCompatOrMaterialCode", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        temperature = findViewById(R.id.temperature)
        humidity = findViewById(R.id.humidity)
        switchText = findViewById(R.id.switchText)
        fanSwitch = findViewById(R.id.fanSwitch)
        autoFan = findViewById(R.id.autoFan)
        save = findViewById(R.id.save)
        newCard = findViewById(R.id.newCard)
        espId = intent.getStringExtra("ITEM_NAME").toString()
        topicFanAuto = "ptcpapesvrwe/"+espId+"/fan/auto"
        topicFanStatus = "ptcpapesvrwe/"+espId+"/fan/status"
        topicMaxTemperature = "ptcpapesvrwe/"+espId+"/max/temperature"
        topicSensorTemperature = "ptcpapesvrwe/"+espId+"/sensor/temperature"
        topicSensorHumidity = "ptcpapesvrwe/"+espId+"/sensor/humidity"
        topicNewCardGet = "ptcpapesvrwe/"+espId+"/newCard/get"
        topicNewCardStatus = "ptcpapesvrwe/"+espId+"/newCard/status"


        var temperatureValue = 18 ;

        var pendingIntent = PendingIntent.getForegroundService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
         Intent(this, MyService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }


        picker1 = findViewById(R.id.numberPicker);
        setupLoaderDialog()
        showLoader()

        CoroutineScope(Dispatchers.IO).launch {

            picker1.maxValue = 26;
            picker1.minValue = 18;
            picker1.wrapSelectorWheel = false;
            val request = Request.Builder()
                .url("$link/getEsp?espId=esp001")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val gson = Gson()
                val myObject = gson.fromJson(response.body?.string(), Esp::class.java)
                runOnUiThread {
                    var fanStatus = "";
                    fanStatus = if(myObject.isFanOn) "on";
                    else "off";
                    switchText.text = "Fan status: $fanStatus"
                    picker1.value = myObject.temperature.toInt()
                    fanSwitch.isChecked = myObject.isFanOn
                    autoFan.isChecked = myObject.isFanAuto
                    hideLoader()
                }
            }


        }

        fanSwitch.setOnClickListener() {
            runOnUiThread {

                showLoader()

                publishMessage(topicFanAuto, "off")

                autoFan.isChecked = false

                hideLoader()
            }
        }

        fanSwitch.setOnCheckedChangeListener { _, isChecked ->

            showLoader()
            val message = if (isChecked) "on" else "off"
            Toast.makeText(
                this@MainActivity, "Switch1: $message",
                Toast.LENGTH_SHORT
            ).show()

            CoroutineScope(Dispatchers.IO).launch {
                publishMessage(topicFanStatus, message)

            }
            hideLoader()
        }

        autoFan.setOnClickListener{
            runOnUiThread {

                showLoader()
                val statusFan: Boolean = autoFan.isChecked
                val message = if (statusFan) "on" else "off"
                publishMessage(topicFanAuto, message)
                hideLoader()
            }
        }
        picker1.setOnValueChangedListener(OnValueChangeListener { picker, oldVal, newVal ->
            temperatureValue = newVal;
        })

        save.setOnClickListener {
            showLoader()
            publishMessage(topicMaxTemperature, temperatureValue.toString())
            hideLoader()

        }

        newCard.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(this)

            alertDialogBuilder.setTitle("Time remaining: 30")
            alertDialogBuilder.setMessage("Apply the card to the card reader")
            alertDialogBuilder.setCancelable(false)

            alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                publishMessage(topicNewCardStatus, "off")
                dialog.dismiss()
                timer.cancel()

            }

            alertDialog = alertDialogBuilder.create()
            alertDialog?.setCanceledOnTouchOutside(false)
            alertDialog?.show()
            publishMessage(topicNewCardStatus, "on")

            cancelButton = alertDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)

            timer.start()
        }


    }

    private val timer = object : CountDownTimer(30000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            alertDialog?.setTitle("Time remaining: " + (millisUntilFinished / 1000).toString())
        }

        @SuppressLint("SetTextI18n")
        override fun onFinish() {
            publishMessage(topicNewCardStatus, "off")
            alertDialog?.setTitle("Time's up!")
            alertDialog?.setMessage("If you applied the card, the card may have been added.")
            cancelButton?.text = "OK"
        }
    }

    private fun setupLoaderDialog() {
        loaderDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.loader_layout)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun showLoader() {
        if (!loaderDialog.isShowing) {
            loaderDialog.show()
        }
    }

    private fun hideLoader() {
        if (loaderDialog.isShowing) {
            loaderDialog.dismiss()
        }
    }


}


data class Esp(
    val id: String,
    val temperature: Double,
    val isFanOn: Boolean,
    val isFanAuto: Boolean
)
