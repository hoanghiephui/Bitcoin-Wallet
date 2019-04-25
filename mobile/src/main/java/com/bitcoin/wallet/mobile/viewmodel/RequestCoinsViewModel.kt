package com.bitcoin.wallet.mobile.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.Constants
import com.bitcoin.wallet.mobile.data.live.ConfigOwnNameLiveData
import com.bitcoin.wallet.mobile.data.live.FreshReceiveAddressLiveData
import com.bitcoin.wallet.mobile.data.live.SelectedExchangeRateLiveData
import com.bitcoin.wallet.mobile.utils.Event
import com.bitcoin.wallet.mobile.utils.Qr
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.uri.BitcoinURI
import javax.inject.Inject

class RequestCoinsViewModel @Inject constructor(application: Application) : ViewModel() {
    val amount = MutableLiveData<Coin>()
    val bluetoothMac = MutableLiveData<String>()
    val qrCode = MediatorLiveData<Bitmap>()
    val paymentRequest = MediatorLiveData<ByteArray>()
    val bitcoinUri = MediatorLiveData<Uri>()
    val showBitmapDialog = MutableLiveData<Event<Bitmap>>()
    val showHelpDialog = MutableLiveData<Event<Int>>()
    val freshReceiveAddress by lazy {
        FreshReceiveAddressLiveData(application = application as BitcoinApplication)
    }
    val ownName by lazy {
        ConfigOwnNameLiveData(application as BitcoinApplication)
    }
    val exchangeRate by lazy {
        SelectedExchangeRateLiveData(application as BitcoinApplication)
    }

    init {
        this.qrCode.addSource(
            freshReceiveAddress
        ) { maybeGenerateQrCode() }
        this.qrCode.addSource(ownName) { maybeGenerateQrCode() }
        this.qrCode.addSource(amount) { maybeGenerateQrCode() }
        this.qrCode.addSource(bluetoothMac) { maybeGenerateQrCode() }
        this.paymentRequest.addSource(
            freshReceiveAddress
        ) { maybeGeneratePaymentRequest() }
        this.paymentRequest.addSource(
            ownName
        ) { maybeGeneratePaymentRequest() }
        this.paymentRequest.addSource(
            amount
        ) { maybeGeneratePaymentRequest() }
        this.paymentRequest.addSource(
            bluetoothMac
        ) { maybeGeneratePaymentRequest() }
        this.bitcoinUri.addSource(
            freshReceiveAddress
        ) { maybeGenerateBitcoinUri() }
        this.bitcoinUri.addSource(ownName) { maybeGenerateBitcoinUri() }
        this.bitcoinUri.addSource(
            amount
        ) { maybeGenerateBitcoinUri() }
    }

    private fun maybeGenerateQrCode() {
        val address = freshReceiveAddress.value
        if (address != null) {
            AsyncTask.execute {
                qrCode.postValue(
                    Qr.bitmap(uri(address, amount.value, ownName.value, bluetoothMac.value))
                )
            }
        }
    }

    private fun maybeGeneratePaymentRequest() {
        val address = freshReceiveAddress.value
        if (address != null) {
            val bluetoothMac = this.bluetoothMac.value
            val paymentUrl = if (bluetoothMac != null) "bt:$bluetoothMac" else null
            paymentRequest.value = PaymentProtocol.createPaymentRequest(
                Constants.NETWORK_PARAMETERS,
                amount.value, address, ownName.value, paymentUrl, null
            ).build().toByteArray()
        }
    }

    private fun maybeGenerateBitcoinUri() {
        val address = freshReceiveAddress.value
        if (address != null) {
            bitcoinUri.value = Uri.parse(uri(address, amount.value, ownName.value, null))
        }
    }

    private fun uri(address: Address?, amount: Coin?, label: String?, bluetoothMac: String?): String {
        val uri = StringBuilder(BitcoinURI.convertToBitcoinURI(address!!, amount, label, null))
        return uri.toString()
    }
}