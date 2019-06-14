/**
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bitcoin.wallet.btc.repository.localdb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * No update methods necessary since for each table there is ever expecting one row, hence why
 * the primary key is hardcoded.
 */
@Dao
interface EntitlementsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(goldStatus: GoldStatus)

    @Update
    fun update(goldStatus: GoldStatus)

    @Query("SELECT * FROM gold_status LIMIT 1")
    fun getGoldStatus(): LiveData<GoldStatus>

    @Delete
    fun delete(goldStatus: GoldStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(premium: PremiumCar)

    @Update
    fun update(premium: PremiumCar)

    @Query("SELECT * FROM premium_car LIMIT 1")
    fun getPremiumCar(): LiveData<PremiumCar>

    @Delete
    fun delete(premium: PremiumCar)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(gasLevel: GasTank)

    @Update
    fun update(gasLevel: GasTank)

    @Query("SELECT * FROM gas_tank LIMIT 1")
    fun getGasTank(): LiveData<GasTank>

    @Delete
    fun delete(gasLevel: GasTank)

    /**
     * This is purely for convenience. The clients of this DAO don't have to discriminate among
     * [GasTank] vs [PremiumCar] vs [GoldStatus] but can simply send in a list of
     * [entitlements][Entitlement].
     */
    @Transaction
    fun insert(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is GasTank -> insert(it)
                is PremiumCar -> insert(it)
                is GoldStatus -> insert(it)
            }
        }
    }

    @Transaction
    fun update(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is GasTank -> update(it)
                is PremiumCar -> update(it)
                is GoldStatus -> update(it)
            }
        }
    }
}