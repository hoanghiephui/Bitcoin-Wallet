package com.bitcoin.wallet.btc.ui.fragments

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.crypto.Crypto
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.ui.activitys.MainActivity
import com.bitcoin.wallet.btc.ui.adapter.FileAdapter
import com.bitcoin.wallet.btc.ui.widget.DialogBuilder
import com.bitcoin.wallet.btc.utils.Event
import com.bitcoin.wallet.btc.utils.ImportListener
import com.bitcoin.wallet.btc.utils.WalletUtils
import com.bitcoin.wallet.btc.viewmodel.RestoreWalletViewModel
import com.google.common.io.CharStreams
import kotlinx.android.synthetic.main.dialog_restore.*
import org.bitcoinj.wallet.Wallet
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

class RestoreWalletDialog : BaseBottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[RestoreWalletViewModel::class.java]
    }
    private val config by lazy {
        (requireActivity().application as BitcoinApplication).config
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_RESTORE_WALLET)
        }
    }

    override fun layoutRes(): Int {
        return R.layout.dialog_restore
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        fileView.adapter = object : FileAdapter(requireContext()) {
            override fun getDropDownView(position: Int, row: View?, parent: ViewGroup): View {
                var views = row
                val file = getItem(position)
                var isExternal = false
                if (file != null) {
                    isExternal = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR == file.parentFile
                }
                val isEncrypted = Crypto.OPENSSL_FILE_FILTER.accept(file)

                if (views == null)
                    views = inflater.inflate(R.layout.restore_wallet_file_row, parent, false)

                val filenameView = views?.findViewById<TextView>(R.id.wallet_import_keys_file_row_filename)
                if (file != null) {
                    filenameView?.text = file.name
                }

                val securityView = views?.findViewById<TextView>(R.id.wallet_import_keys_file_row_security)
                val encryptedStr = context
                    .getString(
                        if (isEncrypted)
                            R.string.import_security_encrypted
                        else
                            R.string.import_security_unencrypted
                    )
                val storageStr = context
                    .getString(
                        if (isExternal)
                            R.string.import_security_external
                        else
                            R.string.import_security_internal
                    )
                securityView?.text = "$encryptedStr, $storageStr"

                val createdView = views?.findViewById<TextView>(R.id.wallet_import_keys_file_row_created)
                if (file != null) {
                    createdView?.text = context.getString(
                        if (isExternal)
                            R.string.import_manual
                        else
                            R.string.import_created_automatic,
                        DateUtils.getRelativeTimeSpanString(context, file.lastModified(), true)
                    )
                }

                return views!!
            }
        }

        val dialogButtonEnabler = object : ImportListener(
            import_keys_from_storage_password, viewGo
        ) {
            override fun hasFile(): Boolean {
                return fileView.selectedItem != null
            }

            override fun needsPassword(): Boolean {
                if (fileView.selectedItem == null) {
                    return false
                }
                val selectedFile = fileView.selectedItem as File
                return selectedFile != null && Crypto.OPENSSL_FILE_FILTER.accept(selectedFile)
            }
        }
        import_keys_from_storage_password.addTextChangedListener(dialogButtonEnabler)
        fileView.onItemSelectedListener = dialogButtonEnabler
        updateView()

        viewModel.balance.observeNotNull(viewLifecycleOwner) { balance ->
            val hasCoins = balance.signum() > 0
            replaceWarningView.visibility = if (hasCoins) View.VISIBLE else View.GONE
        }
        viewGo.setOnClickListener {
            onRestore()
        }
        viewCancel.setOnClickListener {
            import_keys_from_storage_password.text = null
            dismissDialog()
        }
    }

    override fun onResume() {
        updateView()
        super.onResume()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_RESTORE_WALLET) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                dismissDialog()
                fragmentManager?.let { PermissionDeniedDialogFragment.showDialog(it) }
            }
        }
    }

    private fun onRestore() {
        val file = fileView.selectedItem as File
        val password = import_keys_from_storage_password.text.toString().trim { it <= ' ' }
        import_keys_from_storage_password.text = null
        if (WalletUtils.BACKUP_FILE_FILTER.accept(file))
            restoreWalletFromProtobuf(file)
        else if (Crypto.OPENSSL_FILE_FILTER.accept(file))
            restoreWalletFromEncrypted(file, password)
    }

    private fun restoreWalletFromEncrypted(file: File, password: String) {
        try {
            val cipherIn = BufferedReader(
                InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
            )
            val cipherText = StringBuilder()
            CharStreams.copy(cipherIn, cipherText)
            cipherIn.close()

            val plainText = Crypto.decryptBytes(cipherText.toString(), password.toCharArray())
            val `is` = ByteArrayInputStream(plainText)

            restoreWallet(WalletUtils.restoreWalletFromProtobuf(`is`, Constants.NETWORK_PARAMETERS))
        } catch (x: IOException) {
            dismissDialog()
            (requireActivity() as MainActivity).viewModel.showFailureDialog.postValue(Event<String>(x.message))
        }

    }

    private fun restoreWalletFromProtobuf(file: File) {
        try {
            FileInputStream(file).use { `is` ->
                restoreWallet(WalletUtils.restoreWalletFromProtobuf(`is`, Constants.NETWORK_PARAMETERS))
            }
        } catch (x: IOException) {
            dismissDialog()
            (requireActivity() as MainActivity).viewModel.showFailureDialog.postValue(Event<String>(x.message))
        }

    }

    private fun restoreWallet(restoredWallet: Wallet) {
        (requireActivity().application as BitcoinApplication).replaceWallet(restoredWallet)
        config.disarmBackupReminder()
        dismissDialog()
        (requireActivity() as MainActivity).viewModel.showSuccessDialog.value = Event(restoredWallet.isEncrypted)
    }

    private fun updateView() {
        val path: String
        val backupPath = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.absolutePath
        val storagePath = Constants.Files.EXTERNAL_STORAGE_DIR.absolutePath
        if (backupPath.startsWith(storagePath))
            path = backupPath.substring(storagePath.length)
        else
            path = backupPath

        val files = LinkedList<File>()

        // external storage
        val externalFiles = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.listFiles()
        if (externalFiles != null) {
            for (file in externalFiles) {
                val looksLikeBackup = Crypto.OPENSSL_FILE_FILTER.accept(file)
                if (looksLikeBackup)
                    files.add(file)
            }
        }

        // app-private storage
        for (filename in requireActivity().fileList()) {
            if (filename.startsWith(Constants.Files.WALLET_KEY_BACKUP_PROTOBUF + '.')) {
                files.add(File(requireActivity().filesDir, filename))
            }
        }

        // sort
        files.sortWith(Comparator { lhs, rhs -> lhs.name.compareTo(rhs.name, ignoreCase = true) })

        messageView.text = getString(
            if (!files.isEmpty()) R.string.restore_wallet_mes else R.string.restore_empty,
            path
        )

        fileView.visibility = if (!files.isEmpty()) View.VISIBLE else View.GONE
        val adapter = fileView.adapter as FileAdapter
        adapter.setFiles(files)

        passwordView.visibility = if (!files.isEmpty()) View.VISIBLE else View.GONE
        import_keys_from_storage_password.text = null
    }

    companion object {
        fun show(activity: AppCompatActivity) {
            val fragment = RestoreWalletDialog()
            fragment.show(activity.supportFragmentManager, RestoreWalletDialog::class.java.simpleName)
        }

        const val REQUEST_CODE_RESTORE_WALLET = 1
    }
}

class PermissionDeniedDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = DialogBuilder(requireContext())
        dialog.setTitle(R.string.restore_wallet_permission)
        dialog.setMessage(getString(R.string.restore_wallet_permission_detail))
        dialog.singleDismissButton (object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val fragment: DialogFragment?
                if (fragmentManager != null) {
                    fragment = fragmentManager!!
                        .findFragmentByTag(FRAGMENT_TAG) as DialogFragment?
                    fragment?.dismiss()
                }
            }
        })
        return dialog.create()
    }

    companion object {
        private val FRAGMENT_TAG = PermissionDeniedDialogFragment::class.java.name

        fun showDialog(fm: FragmentManager) {
            val newFragment = PermissionDeniedDialogFragment()
            newFragment.show(fm, FRAGMENT_TAG)
        }
    }
}