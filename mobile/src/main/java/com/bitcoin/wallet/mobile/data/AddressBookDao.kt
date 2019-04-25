package com.bitcoin.wallet.mobile.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AddressBookDao {

    @get:Query("SELECT * FROM address_book ORDER BY label COLLATE LOCALIZED ASC")
    val all: LiveData<List<AddressBookEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(addressBookEntry: AddressBookEntry)

    @Query("DELETE FROM address_book WHERE address = :address")
    fun delete(address: String?)

    @Query("SELECT label FROM address_book WHERE address = :address")
    fun resolveLabel(address: String): String

    @Query("SELECT * FROM address_book WHERE address LIKE '%' || :constraint || '%' OR label LIKE '%' || :constraint || '%' ORDER BY label COLLATE LOCALIZED ASC")
    operator fun get(constraint: String): List<AddressBookEntry>

    @Query("SELECT * FROM address_book WHERE address NOT IN (:except) ORDER BY label COLLATE LOCALIZED ASC")
    fun getAllExcept(except: Set<String>): LiveData<List<AddressBookEntry>>
}
