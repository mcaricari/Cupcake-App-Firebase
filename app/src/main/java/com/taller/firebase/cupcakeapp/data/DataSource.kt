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
    val flavors = listOf(
        R.string.vanilla,
        R.string.chocolate,
        R.string.red_velvet,
        R.string.salted_caramel,
        R.string.coffee,
    )
    val quantityOptions = listOf(
        Pair(R.string.one_cupcake, 1),
        Pair(R.string.six_cupcakes, 6),
        Pair(R.string.twelve_cupcakes, 12)
    )
    var pictureVariantEnabled = false
    var showDiscount = false

    init {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 10
            fetchTimeoutInSeconds = 10
        }
        val defaults = mapOf(
            "picture_variant_enabled" to false,
            "show_discount" to false,
        )
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(settings)
            setDefaultsAsync(defaults)
            fetchAndActivate().addOnCompleteListener { task ->
                _configFetched.value = true
                if (task.isSuccessful) {
                    Log.i(TAG, "Remote config fetch successful")
                    pictureVariantEnabled = getBoolean("picture_variant_enabled")
                    showDiscount = getBoolean("show_discount")
                } else {
                    Log.e(TAG, "Remote config fetch failed")
                }
            }
        }
    }
}
