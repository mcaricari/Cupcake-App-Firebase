/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taller.firebase.cupcakeapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.taller.firebase.cupcakeapp.analytics.CustomEvents
import com.taller.firebase.cupcakeapp.analytics.CustomParameters
import com.taller.firebase.cupcakeapp.data.DataSource
import com.taller.firebase.cupcakeapp.data.FirestoreDb
import com.taller.firebase.cupcakeapp.data.OrderUiState
import com.taller.firebase.cupcakeapp.exception.DateTooCloseException
import com.taller.firebase.cupcakeapp.exception.OutOfStockException
import com.taller.firebase.cupcakeapp.ui.OrderSummaryScreen
import com.taller.firebase.cupcakeapp.ui.OrderViewModel
import com.taller.firebase.cupcakeapp.ui.SelectOptionScreen
import com.taller.firebase.cupcakeapp.ui.StartOrderScreen
import com.taller.firebase.cupcakeapp.ui.history.OrderHistoryScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * enum values that represent the screens in the app
 */
enum class CupcakeScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Flavor(title = R.string.choose_flavor),
    Pickup(title = R.string.choose_pickup_date),
    Summary(title = R.string.order_summary),
    History(title = R.string.order_history)
}

/**
 * Composable that displays the topBar and displays back button if back navigation is possible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupcakeAppBar(
    currentScreen: CupcakeScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    navigateToMyOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        },
        actions = {
            if (currentScreen == CupcakeScreen.Start) {
                IconButton(
                    onClick = navigateToMyOrders
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null
                    )
                }
            }
            IconButton(
                onClick = {
                    AuthUI.getInstance().signOut(context)
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun CupcakeApp(
    userName: String?,
    viewModel: OrderViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = CupcakeScreen.valueOf(
        backStackEntry?.destination?.route ?: CupcakeScreen.Start.name
    )

    Scaffold(
        topBar = {
            CupcakeAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                navigateToMyOrders = { navController.navigate(CupcakeScreen.History.name) }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        NavHost(
            navController = navController,
            startDestination = CupcakeScreen.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = CupcakeScreen.Start.name) {
                StartOrderScreen(
                    userName = userName ?: "",
                    quantityOptions = DataSource.quantityOptions,
                    onNextButtonClicked = {
                        viewModel.setQuantity(it)
                        Firebase.crashlytics.log("Selected quantity: $it")
                        navController.navigate(CupcakeScreen.Flavor.name)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.padding_medium))
                )
            }
            composable(route = CupcakeScreen.Flavor.name) {
                val context = LocalContext.current
                SelectOptionScreen(
                    subtotal = FormatUtils.formatPrice(uiState.price),
                    onNextButtonClicked = {
                        Firebase.crashlytics.log("Selected flavour: ${uiState.flavor}")
                        navController.navigate(CupcakeScreen.Pickup.name)
                    },
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(viewModel, navController)
                        logOrderCancelledEvent(CupcakeScreen.Flavor.name)
                    },
                    options = DataSource.flavors.map { id -> context.resources.getString(id) },
                    onSelectionChanged = { viewModel.setFlavor(it) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = CupcakeScreen.Pickup.name) {
                SelectOptionScreen(
                    subtotal = FormatUtils.formatPrice(uiState.price),
                    onNextButtonClicked = {
                        Firebase.crashlytics.log("Selected date: ${uiState.date}")
                        navController.navigate(CupcakeScreen.Summary.name)
                    },
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(viewModel, navController)
                        logOrderCancelledEvent(CupcakeScreen.Pickup.name)
                    },
                    options = uiState.pickupOptions,
                    onSelectionChanged = { viewModel.setDate(it) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = CupcakeScreen.Summary.name) {
                val context = LocalContext.current
                OrderSummaryScreen(
                    orderUiState = uiState,
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(viewModel, navController)
                        logOrderCancelledEvent(CupcakeScreen.Summary.name)
                    },
                    onSendButtonClicked = { subject: String, summary: String ->
                        //shareOrder(context, subject = subject, summary = summary)
                        checkStock(uiState.quantity, uiState.flavor)
                        checkDate(uiState.date)
                        fakeSend(context, viewModel, navController)
                        logOrderSentEvent(uiState.quantity, uiState.flavor)
                        val price = if (DataSource.showDiscount) {
                            uiState.price.times(0.9)
                        } else {
                            uiState.price
                        }
                        FirestoreDb.saveOrder(uiState.quantity, uiState.flavor, price)
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = CupcakeScreen.History.name) {
                OrderHistoryScreen()
            }
        }
    }
}

/**
 * Resets the [OrderUiState] and pops up to [CupcakeScreen.Start]
 */
private fun cancelOrderAndNavigateToStart(
    viewModel: OrderViewModel,
    navController: NavHostController
) {
    viewModel.resetOrder()
    navController.popBackStack(CupcakeScreen.Start.name, inclusive = false)
}

/**
 * Creates an intent to share order details
 */
private fun shareOrder(context: Context, subject: String, summary: String) {
    // Create an ACTION_SEND implicit intent with order details in the intent extras
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.new_cupcake_order)
        )
    )
}

private fun fakeSend(
    context: Context,
    viewModel: OrderViewModel,
    navController: NavHostController
) {
    val toast = Toast.makeText(context, "Order sent!", Toast.LENGTH_SHORT)
    toast.show()
    viewModel.resetOrder()
    navController.popBackStack(CupcakeScreen.Start.name, inclusive = false)
}

private fun checkStock(quantity: Int, flavour: String) {
    if (quantity == 12 && flavour == "Coffee") {
        throw OutOfStockException("Can't order 12 Coffee Cupcakes. Out of stock")
    }
}

private fun checkDate(dateStr: String) {
    val formatter = SimpleDateFormat("E MMM d", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val today = formatter.format(calendar.time)
    if (today == dateStr) {
        throw DateTooCloseException("Date too close. Order won't be satisfied")
    }
}

private fun logOrderCancelledEvent(screen: String) {
    val params = Bundle().apply {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screen)
    }
    Log.i("CupcakeScreen", "enviando orer cancelled")
    Firebase.analytics.logEvent(CustomEvents.ORDER_CANCELLED, params)
}

private fun logOrderSentEvent(quantity: Int, flavour: String) {
    val params = Bundle().apply {
        putInt(FirebaseAnalytics.Param.QUANTITY, quantity)
        putString(CustomParameters.FLAVOUR, flavour)
    }
    Firebase.analytics.logEvent(CustomEvents.ORDER_SENT, params)
}
