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

/**
 * For this app the decision was made to create an [Entity] class for each product and feature that
 * the app sells. It was further decided to put all of them in one file. You don't have to do it
 * this way. But here is why it's done that way here:
 *
 * 1 - The only way for the users to have offline access to their entitlements is if they are saved
 *     somehow. It is more convenient to use Room instead of, say, SharedPreferences.
 *
 * 2 - It may seem like an overkill to create an entire class just to persist a Boolean, as in the
 *      case of [GoldStatus]. Yes and no. Putting it inside Room means it can be tracked using
 *      LiveData without the clients having to write too much code. Also maybe later there will
 *      be needs to add more fields, such as the status of the subscription.
 *
 * 3 - Grouping all the app's products and subscriptions in one file called Entitlements seem
 *      effective.
 */
package com.bitcoin.wallet.btc.repository.localdb

import androidx.room.Entity
import androidx.room.PrimaryKey

private const val FULL_TANK = 4
private const val EMPTY_TANK = 0
const val GAS_PURCHASE = 1

/**
 * Normally this would just be an interface. But since each of the entitlements only has
 * one item/row and so primary key is fixed, we can put the primary key here and so make
 * the class abstract.
 **/
abstract class Entitlement {
    @PrimaryKey
    var id: Int = 1

    /**
     * This method tells clients whether a user __should__ buy a particular item at the moment. For
     * example, if the gas tank is full the user should not be buying gas. This method is __not__
     * a reflection on whether Google Play Billing can make a purchase.
     */
    abstract fun mayPurchase(): Boolean
}

/**
 * Indicates whether the user owns a premium car.
 */
@Entity(tableName = "premium_car")
data class PremiumCar(val entitled: Boolean) : Entitlement() {
    override fun mayPurchase(): Boolean = !entitled
}

/**
 * Subscription is kept simple in this project. And so here the user either has a subscription
 * to gold status or s/he doesn't. For more on subscriptions, see the Classy Taxi sample app.
 */
@Entity(tableName = "gold_status")
data class GoldStatus(val entitled: Boolean) : Entitlement() {
    override fun mayPurchase(): Boolean = !entitled
}

/**
 * The level inside this gas tank goes up when user buys gas and goes down when user drives. This
 * means level can be updated from clients and from servers. By clients are Activities, Fragments
 * and Views; by servers are the Play BillingClient or your own secure server. Therefore you
 * should code your repository against race conditions and interleaves.
 */
@Entity(tableName = "gas_tank")
class GasTank(private var level: Int) : Entitlement() {

    /**
     * In order to exercise great control over how clients use the API, [setLevel] is made
     * private while keeping [getLevel] public. There is no idiomatic way to do this
     * in Kotlin for an [Entity] data class. So instead of going for "idiomatic", the favor is given
     * to "simple".  But in your own app feel free to go for idiomatic Kotlin.
     */
    fun getLevel() = level

    override fun mayPurchase(): Boolean = level < FULL_TANK

    fun needGas(): Boolean = level <= EMPTY_TANK

    fun decrement(by: Int = 1) {
        level -= by
    }
}