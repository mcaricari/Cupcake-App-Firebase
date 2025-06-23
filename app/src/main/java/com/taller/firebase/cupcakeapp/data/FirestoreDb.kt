package com.taller.firebase.cupcakeapp.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.taller.firebase.cupcakeapp.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Order(
    val quantity: Int,
    val flavor: String,
    val price: Double,
    val date: String
)

object FirestoreDb {
    private const val TAG = "FirestoreDb"
    private val _orders = MutableStateFlow(emptyList<Order>())
    val orders = _orders.asStateFlow()

    fun saveOrder(quantity: Int, flavor: String, price: Double) {
        val db = Firebase.firestore.collection("users")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val order = hashMapOf(
            "quantity" to quantity,
            "flavor" to flavor,
            "price" to price,
            "date" to FormatUtils.getCurrentDateTimeStr(),
        )
        userId?.let {
            db.document(it)
                .collection("orders")
                .add(order)
                .addOnSuccessListener {
                    Log.d(TAG, "Orden guardada para usuario $userId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al guardar orden", e)
                }
        }
    }

    fun getOrders() {
        val db = Firebase.firestore.collection("users")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            db.document(it)
                .collection("orders")
                .get()
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Ordenes recuperadas para usuario $userId")
                    val tmpOrders = mutableListOf<Order>()
                    result.forEach { document ->
                        val quantity = document.get("quantity") as Long
                        val flavor = document.get("flavor") as String
                        val price = document.get("price") as Double
                        val date = document.get("date") as String
                        tmpOrders.add(
                            Order(
                                quantity = quantity.toInt(),
                                flavor = flavor,
                                price = price,
                                date = date
                            )
                        )
                    }
                    Log.d(TAG, "Ordenes recuperadas: $tmpOrders")
                    _orders.value = tmpOrders
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al recuperar ordenes", e)
                }
        }
    }
}