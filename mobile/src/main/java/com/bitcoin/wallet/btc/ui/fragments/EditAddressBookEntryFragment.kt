package com.bitcoin.wallet.btc.ui.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.data.AddressBookDao
import com.bitcoin.wallet.btc.data.AddressBookEntry
import com.bitcoin.wallet.btc.data.AppDatabase
import com.bitcoin.wallet.btc.ui.widget.DialogBuilder
import com.bitcoin.wallet.btc.utils.WalletUtils
import org.bitcoinj.core.Address
import org.bitcoinj.wallet.Wallet

class EditAddressBookEntryFragment : DialogFragment() {

    private var activity: BaseActivity? = null
    private var addressBookDao: AddressBookDao? = null
    private var wallet: Wallet? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.activity = context as BaseActivity?
        val application = activity?.application
        this.addressBookDao = AppDatabase.getDatabase(context).addressBookDao()
        this.wallet = application?.getWallet()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments
        val address = Address.fromString(Constants.NETWORK_PARAMETERS, args!!.getString(KEY_ADDRESS)!!)
        val suggestedAddressLabel = args.getString(KEY_SUGGESTED_ADDRESS_LABEL)

        val inflater = LayoutInflater.from(activity)

        val label = addressBookDao!!.resolveLabel(address.toString())

        val isAdd = label == null
        val isOwn = wallet!!.isAddressMine(address)

        val dialog = DialogBuilder(activity)

        if (isOwn)
            dialog.setTitle(
                if (isAdd)
                    R.string.edit_address_add_receive
                else
                    R.string.edit_address_edit_receive
            )
        else
            dialog.setTitle(
                if (isAdd)
                    R.string.edit_address_book_entry
                else
                    R.string.edit_address
            )

        val view = inflater.inflate(R.layout.dialog_edit_address_book_entry, null)

        val viewAddress = view.findViewById<TextView>(R.id.edit_address_book_entry_address)
        viewAddress.text = WalletUtils.formatAddress(
            address, Constants.ADDRESS_FORMAT_GROUP_SIZE,
            Constants.ADDRESS_FORMAT_LINE_SIZE, true
        )

        val viewLabel = view.findViewById<TextView>(R.id.edit_address_book_entry_label)
        viewLabel.text = label

        dialog.setView(view)
        val onClickListener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                val newLabel = viewLabel.text.toString().trim { it <= ' ' }
                if (!newLabel.isEmpty())
                    addressBookDao?.insertOrUpdate(AddressBookEntry(address.toString(), newLabel))
                else if (!isAdd)
                    addressBookDao?.delete(address.toString())
            } else if (which == DialogInterface.BUTTON_NEUTRAL) {
                addressBookDao?.delete(address.toString())
            }

            dismiss()
        }

        dialog.setPositiveButton(
            if (isAdd) R.string.btn_add else R.string.edit_address_btn_edit,
            onClickListener
        )
        if (!isAdd)
            dialog.setNeutralButton(R.string.btn_delete, onClickListener)
        dialog.setNegativeButton(R.string.btn_cancel) { _, _ -> dismissAllowingStateLoss() }

        return dialog.create()
    }

    companion object {
        private val FRAGMENT_TAG = EditAddressBookEntryFragment::class.java.name

        private const val KEY_ADDRESS = "address"
        private const val KEY_SUGGESTED_ADDRESS_LABEL = "suggested_address_label"

        fun edit(fm: FragmentManager, address: Address) {
            edit(fm, address, null)
        }

        private fun edit(
            fm: FragmentManager, address: Address,
            suggestedAddressLabel: String?
        ) {
            val newFragment = instance(
                address,
                suggestedAddressLabel
            )
            newFragment.show(
                fm,
                FRAGMENT_TAG
            )
        }

        private fun instance(
            address: Address,
            suggestedAddressLabel: String?
        ): EditAddressBookEntryFragment {
            val fragment = EditAddressBookEntryFragment()

            val args = Bundle()
            args.putString(KEY_ADDRESS, address.toString())
            args.putString(KEY_SUGGESTED_ADDRESS_LABEL, suggestedAddressLabel)
            fragment.arguments = args

            return fragment
        }
    }
}