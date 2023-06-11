package com.example.esp32

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.widget.Button
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class ItemAdapter(private val items: MutableList<Item>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    val client = OkHttpClient()

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val id: TextView = itemView.findViewById(R.id.item_id)
        val isEspOn: TextView = itemView.findViewById(R.id.item_is_esp_on)
        val deleteButton: Button = itemView.findViewById(R.id.remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.id.text = item.id
        holder.isEspOn.text = "OFF"
        println(item)
        if(item.isEspOn){
            holder.isEspOn.text = "ON"
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("ITEM_NAME", item.id)
            }
            context.startActivity(intent)
        }



        holder.deleteButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {

                val request = Request.Builder()
                    .url("${Helper.link}/esp/removeEsp?espId="+item.id)
                    .build()
                client.newCall(request).execute().use { response ->
                }
            }
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }

    override fun getItemCount() = items.size
}
