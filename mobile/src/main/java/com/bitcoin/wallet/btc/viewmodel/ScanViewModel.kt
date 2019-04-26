package com.bitcoin.wallet.btc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.utils.Event
import javax.inject.Inject

class ScanViewModel @Inject constructor() : ViewModel() {
    val showPermissionWarnDialog = MutableLiveData<Event<Void>>()
    val showProblemWarnDialog = MutableLiveData<Event<Void>>()
}