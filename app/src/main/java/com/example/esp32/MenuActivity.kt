package com.example.esp32

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException

class MenuActivity : AppCompatActivity() {
    private lateinit var items: MutableList<Item>
    private lateinit var adapter: ItemAdapter
    val client = OkHttpClient()
    private lateinit var loaderDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_layout)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val addButton = findViewById<Button>(R.id.add_button)
        setupLoaderDialog()

        CoroutineScope(Dispatchers.IO).launch {

            val request = Request.Builder()
                .url("${Helper.link}/getAllEsps")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val gson = Gson()
                val itemType = object : TypeToken<List<Item>>() {}.type
                items = gson.fromJson(response.body?.string(), itemType)
                withContext(Dispatchers.Main) {
                    adapter = ItemAdapter(items)
                    recyclerView.adapter = adapter
                }
            }
        }

        addButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Input device ID")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()

                CoroutineScope(Dispatchers.IO).launch {
                    runOnUiThread {
                        showLoader()

                    }
                    var request = Request.Builder()
                        .url("${Helper.link}/esp/addEsp?espId=${input.text.toString()}")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    }
                    request = Request.Builder()
                        .url("${Helper.link}/getAllEsps")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        val gson = Gson()
                        val itemType = object : TypeToken<List<Item>>() {}.type
                        items = gson.fromJson(response.body?.string(), itemType)

                        withContext(Dispatchers.Main) {
                            adapter = ItemAdapter(items)
                            recyclerView.adapter = adapter
                        }
                    }
                    runOnUiThread {
                        hideLoader()
                    }
                }

            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()

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
