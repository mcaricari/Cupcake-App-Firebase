package com.taller.firebase.cupcakeapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taller.firebase.cupcakeapp.data.FirestoreDb
import com.taller.firebase.cupcakeapp.data.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryScreenState(
    val orders: List<Order> = emptyList(),
)

class OrderHistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryScreenState())
    val uiState: StateFlow<HistoryScreenState> = _uiState.asStateFlow()

    init {
        fetchOrders()
    }

    private fun fetchOrders() {
        FirestoreDb.getOrders()
        viewModelScope.launch {
            FirestoreDb.orders.collect { orders ->
                _uiState.value = _uiState.value.copy(orders = orders)
            }
        }
    }
}