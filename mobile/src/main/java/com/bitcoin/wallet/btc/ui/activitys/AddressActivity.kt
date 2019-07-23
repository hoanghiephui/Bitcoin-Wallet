package com.bitcoin.wallet.btc.ui.activitys

import android.app.Activity
import android.content.*
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.RadioGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.data.AddressBookDao
import com.bitcoin.wallet.btc.data.AddressBookEntry
import com.bitcoin.wallet.btc.data.AppDatabase
import com.bitcoin.wallet.btc.data.PaymentIntent
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.ui.activitys.ScanActivity.Companion.REQUEST_CODE_SCAN
import com.bitcoin.wallet.btc.ui.adapter.AddressAdapter
import com.bitcoin.wallet.btc.ui.adapter.AddressSendAdapter
import com.bitcoin.wallet.btc.ui.fragments.BitmapBottomDialog
import com.bitcoin.wallet.btc.ui.fragments.EditAddressBookEntryFragment
import com.bitcoin.wallet.btc.ui.widget.DialogBuilder
import com.bitcoin.wallet.btc.utils.Event
import com.bitcoin.wallet.btc.utils.InputParser
import com.bitcoin.wallet.btc.utils.Qr
import com.bitcoin.wallet.btc.viewmodel.WalletAddressViewModel
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.item_network_state.*
import kotlinx.android.synthetic.main.toolbar.*
import org.bitcoinj.core.*
import org.bitcoinj.uri.BitcoinURI
import org.bitcoinj.uri.BitcoinURIParseException
import java.util.*

class AddressActivity : BaseActivity(), AddressSendAdapter.SendAddressCallback, AddressAdapter.AddressCallback,
    RadioGroup.OnCheckedChangeListener {
    private val viewModel: WalletAddressViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[WalletAddressViewModel::class.java]
    }
    private val addressAdapter: AddressAdapter by lazy {
        AddressAdapter(this)
    }
    private val sendAdapter: AddressSendAdapter by lazy {
        AddressSendAdapter(this)
    }
    private val addressBookDao: AddressBookDao by lazy {
        AppDatabase.getDatabase(this).addressBookDao()
    }
    private val clipboardManager: ClipboardManager by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    private val handler by lazy {
        Handler()
    }
    private var isShowMenu = false

    override fun layoutRes(): Int {
        return R.layout.activity_network
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.address_book))
        setSupportActionBar(toolbar)
        toolbar?.title = ""
        supportActionBar?.title = ""
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AddressActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@AddressActivity, DividerItemDecoration.VERTICAL))
            adapter = addressAdapter
        }
        loadingProgressBar.gone()
        retryLoadingButton.gone()
        errorMessageTextView.text = getString(R.string.address_book_text)
        segmented.setOnCheckedChangeListener(this)

        viewModel.issuedReceiveAddresses.observe(this, Observer {
            addressAdapter.replaceDerivedAddresses(it)
        })
        viewModel.importedAddresses.observe(this, Observer {
            addressAdapter.replaceRandomAddresses(it)
        })
        viewModel.wallet.observe(this, Observer {
            addressAdapter.setWallet(it)
            invalidateOptionsMenu()
        })
        viewModel.addressBook.observe(this, Observer {
            addressAdapter.setAddressBook(AddressBookEntry.asMap(it))
        })
        viewModel.ownName.observe(this, Observer {
            addressAdapter.notifyDataSetChanged()
        })
        viewModel.showBitmapDialog.observe(this, object : Event.Observer<Bitmap>() {
            override fun onEvent(content: Bitmap?) {
                content?.let {
                    BitmapBottomDialog.show(this@AddressActivity, it)
                }
            }
        })
        viewModel.showEditAddressBookEntryDialog.observe(this, object : Event.Observer<Address>() {
            override fun onEvent(content: Address?) {
                content?.let { EditAddressBookEntryFragment.edit(supportFragmentManager, it) }
            }
        })
        viewModel.addressToExclude.observe(this, Observer {
            viewModel.addressBookSend = addressBookDao.getAllExcept(it)
            viewModel.addressBookSend?.observe(this, Observer { list ->
                sendAdapter.listItem = list as ArrayList<AddressBookEntry>
                sendAdapter.notifyDataSetChanged()
                errorMessageTextView.visibility =
                    if (list.isEmpty() && segmented.checkedRadioButtonId == R.id.btnTwo) View.VISIBLE else View.GONE
            })
        })
        viewModel.clip.observe(this, Observer {
            invalidateOptionsMenu()
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            val input = data?.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT)

            object : InputParser.StringInputParser(input) {
                override fun handlePaymentIntent(paymentIntent: PaymentIntent) {
                    handler.postDelayed({
                        if (paymentIntent.hasAddress()) {
                            val wallet = viewModel.wallet.value
                            val address = paymentIntent.address
                            if (wallet?.isAddressMine(address) == false)
                                viewModel.showEditAddressBookEntryDialog.setValue(Event(address))
                            else
                                dialog(
                                    this@AddressActivity, null, R.string.address_book_options_scan_title,
                                    R.string.address_book_options_scan_own_address
                                )
                        } else {
                            dialog(
                                this@AddressActivity, null, R.string.address_book_options_scan_title,
                                R.string.address_book_options_scan_invalid
                            )
                        }
                    }, 500)
                }

                @Throws(VerificationException::class)
                override fun handleDirectTransaction(transaction: Transaction) {
                    cannotClassify(input)
                }

                override fun error(messageResId: Int, vararg messageArgs: Any) {
                    dialog(this@AddressActivity, null, R.string.address_book_options_scan_title, messageResId, messageArgs)
                }
            }.parse()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.sending_addresses_options_paste)?.isEnabled =
            viewModel.wallet.value != null && getAddressFromPrimaryClip() != null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_send_address, menu)
        menu?.findItem(R.id.sending_addresses_options_paste)?.isVisible = isShowMenu
        menu?.findItem(R.id.sending_addresses_options_scan)?.isVisible = isShowMenu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.sending_addresses_options_paste -> {
                handlePasteClipboard()
                return true
            }

            R.id.sending_addresses_options_scan -> {
                ScanActivity.startForResult(this, REQUEST_CODE_SCAN)
                return true
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.btnOne -> {
                recyclerView.adapter = addressAdapter
                errorMessageTextView.gone()
                isShowMenu = false
                invalidateOptionsMenu()
            }
            R.id.btnTwo -> {
                recyclerView.adapter = sendAdapter
                errorMessageTextView.visibility =
                    if (sendAdapter.listItem.isEmpty() && segmented.checkedRadioButtonId == R.id.btnTwo) View.VISIBLE else View.GONE
                isShowMenu = true
                invalidateOptionsMenu()
            }
        }
    }

    override fun onClickItem(position: Int, view: View, address: Address?) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_more_address)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_browse -> {
                    true
                }
                R.id.menu_copy -> {
                    address?.let { handleCopyToClipboard(it.toString()) }
                    true
                }
                R.id.menu_edit -> {
                    address?.let {
                        viewModel.showEditAddressBookEntryDialog.value = Event(it)
                    }
                    true
                }
                R.id.menu_qr -> {
                    val label = viewModel.ownName.value
                    val uri: String
                    uri = if (address is LegacyAddress || label != null)
                        BitcoinURI.convertToBitcoinURI(address, null, label, null)
                    else
                        address.toString().toUpperCase(Locale.US)
                    viewModel.showBitmapDialog.value = Event(Qr.bitmap(uri))
                    true
                }
                else -> true
            }

        }
        popupMenu.show()
    }

    override fun onClickItemSend(position: Int, view: View, address: AddressBookEntry?) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_more_send_address)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_browse -> {
                    true
                }
                R.id.menu_copy -> {
                    address?.address.let { it?.let { it1 -> handleCopyToClipboardSend(it1) } }
                    true
                }
                R.id.menu_edit -> {
                    address?.address?.let {
                        val addres = Address.fromString(Constants.NETWORK_PARAMETERS, it)
                        viewModel.showEditAddressBookEntryDialog.value = Event(addres)
                    }
                    true
                }
                R.id.menu_qr -> {
                    val uri = BitcoinURI.convertToBitcoinURI(
                        Constants.NETWORK_PARAMETERS,
                        address?.address, null, address?.label, null
                    )
                    viewModel.showBitmapDialog.value = Event(Qr.bitmap(uri))
                    true
                }
                R.id.menu_send -> {
                    SendCoinActivity.start(this, PaymentIntent.fromAddress(address?.address, address?.label))
                    true
                }
                R.id.menu_remove -> {
                    addressBookDao.delete(address?.address)
                    true
                }
                else -> true
            }

        }
        popupMenu.show()
    }

    private fun handleCopyToClipboard(address: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Bitcoin address", address))
        Toast.makeText(this, R.string.clipboard_msg, Toast.LENGTH_SHORT).show()
    }

    private fun handleCopyToClipboardSend(address: String) {
        viewModel.clip.setClipData(ClipData.newPlainText("Bitcoin address", address))
        Toast.makeText(this, R.string.clipboard_msg, Toast.LENGTH_SHORT).show()
    }

    private fun getAddressFromPrimaryClip(): Address? {
        val clip = viewModel.clip.value ?: return null
        val clipDescription = clip.description

        when {
            clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) -> {
                val clipText = clip.getItemAt(0).text ?: return null

                return try {
                    Address.fromString(Constants.NETWORK_PARAMETERS, clipText.toString().trim { it <= ' ' })
                } catch (x: AddressFormatException) {
                    null
                }

            }
            clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) -> {
                val clipUri = clip.getItemAt(0).uri ?: return null
                return try {
                    BitcoinURI(clipUri.toString()).address
                } catch (x: BitcoinURIParseException) {
                    null
                }

            }
            else -> return null
        }
    }

    private fun handlePasteClipboard() {
        val wallet = viewModel.wallet.value
        val address = getAddressFromPrimaryClip()
        if (address == null) {
            val dialog = DialogBuilder(this)
            dialog.setTitle(R.string.address_paste)
            dialog.setMessage(R.string.address_paste_invalid)
            dialog.singleDismissButton(null)
            dialog.show()
        } else if (wallet != null && !wallet.isAddressMine(address)) {
            viewModel.showEditAddressBookEntryDialog.setValue(Event(address))
        } else {
            val dialog = DialogBuilder(this)
            dialog.setTitle(R.string.address_paste)
            dialog.setMessage(R.string.address_own_address)
            dialog.singleDismissButton(null)
            dialog.show()
        }
    }
}