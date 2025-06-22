package com.taller.firebase.cupcakeapp

import java.text.NumberFormat

object FormatUtils {
    fun formatPrice(price: Double): String = NumberFormat.getCurrencyInstance().format(price)
}