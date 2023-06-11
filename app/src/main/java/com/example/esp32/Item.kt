package com.example.esp32

data class Item(
    val id: String,
    val temperature: Double,
    val isFanOn: Boolean,
    val isFanAuto: Boolean,
    val isEspOn: Boolean
)
