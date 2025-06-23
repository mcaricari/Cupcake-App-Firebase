package com.taller.firebase.cupcakeapp

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object FormatUtils {
    fun formatPrice(price: Double): String = NumberFormat.getCurrencyInstance().format(price)

    fun getCurrentDateTimeStr(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = Calendar.getInstance().time
        return formatter.format(date)
    }
}