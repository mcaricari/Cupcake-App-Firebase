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
package com.taller.firebase.cupcakeapp.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.taller.firebase.cupcakeapp.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


object DataSource {
    private const val TAG = "DataSource"
    private val _configFetched = MutableStateFlow(false)
    val configFetched = _configFetched.asStateFlow()
    lateinit var flavors: List<String>
    lateinit var quantityOptions: List<Pair<Int, Int>>
    var pictureVariantEnabled = false

    init {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 10
            fetchTimeoutInSeconds = 10
        }
        val defaults = mapOf(
            "twelve_cupcakes_enabled" to true,
            "picture_variant_enabled" to false,
            "flavour1" to "Vanilla",
            "flavour2" to "Chocolate",
            "flavour3" to "Red Velvet",
            "flavour4" to "Salted Caramel",
            "flavour5" to "Coffee",
        )
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(settings)
            setDefaultsAsync(defaults)
            fetchAndActivate().addOnCompleteListener { task ->
                _configFetched.value = true
                if (task.isSuccessful) {
                    Log.i(TAG, "Remote config fetch successful")
                    flavors = listOf(
                        getString("flavour1"),
                        getString("flavour2"),
                        getString("flavour3"),
                        getString("flavour4"),
                        getString("flavour5"),
                    )
                    pictureVariantEnabled = getBoolean("picture_variant_enabled")
                    quantityOptions = if (getBoolean("twelve_cupcakes_enabled")) {
                        listOf(
                            Pair(R.string.one_cupcake, 1),
                            Pair(R.string.six_cupcakes, 6),
                            Pair(R.string.twelve_cupcakes, 12)
                        )
                    } else {
                        listOf(
                            Pair(R.string.one_cupcake, 1),
                            Pair(R.string.six_cupcakes, 6)
                        )
                    }
                } else {
                    Log.e(TAG, "Remote config fetch failed")
                    flavors = listOf(
                        "Vanilla",
                        "Chocolate",
                        "Red Velvet",
                        "Salted Caramel",
                        "Coffee",
                    )
                    quantityOptions = listOf(
                        Pair(R.string.one_cupcake, 1),
                        Pair(R.string.six_cupcakes, 6),
                        Pair(R.string.twelve_cupcakes, 12)
                    )
                }
            }
        }
    }
}
