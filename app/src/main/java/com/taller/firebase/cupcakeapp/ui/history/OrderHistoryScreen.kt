package com.taller.firebase.cupcakeapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taller.firebase.cupcakeapp.data.Order

@Composable
fun OrderHistoryScreen(viewModel: OrderHistoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.orders.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.orders) {
                OrderItem(order = it)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay órdenes registradas.")
        }
    }
}

@Composable
fun OrderItem(order: Order) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "🧁 ${order.flavor}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Quantity: ${order.quantity}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Price: $${order.price}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Date time: ${order.date}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}