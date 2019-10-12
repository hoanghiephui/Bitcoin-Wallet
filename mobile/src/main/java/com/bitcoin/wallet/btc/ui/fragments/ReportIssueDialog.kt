package com.bitcoin.wallet.btc.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.extension.Bundle
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.ui.activitys.WalletTransactionsActivity
import com.bitcoin.wallet.btc.viewmodel.ReportIssueViewModel
import kotlinx.android.synthetic.main.dialog_report_issue.*
import java.io.IOException
import java.util.*
import javax.inject.Inject

class ReportIssueDialog : BaseBottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[ReportIssueViewModel::class.java]
    }

    override fun layoutRes(): Int {
        return R.layout.dialog_report_issue
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        report_issue_dialog_message.text = arguments?.getInt(KEY_TITLE)?.let { getString(it) }
        val contextualData = arguments?.getString(KEY_CONTEXTUAL_DATA)
        val subject = arguments?.getString(KEY_SUBJECT)
        viewModel.wallet.observeNotNull(viewLifecycleOwner) {
            viewGo.isEnabled = true
        }
        viewGo.setOnClickListener {
            val text = StringBuilder()
            text.append("Issue description: " + report_issue_dialog_description.text).append('\n')

            try {
                if (contextualData != null) {
                    text.append("\n\n\n=== contextual data ===\n\n")
                    text.append(contextualData)
                }
            } catch (x: IOException) {
                text.append(x.toString()).append('\n')
            }


            try {
                text.append("\n\n\n=== application info ===\n\n")
                val applicationInfo =
                    appendApplicationInfo(
                        StringBuilder(),
                        (activity as WalletTransactionsActivity).application
                    )

                text.append(applicationInfo)
            } catch (x: IOException) {
                text.append(x.toString()).append('\n')
            }

            startSend(subject(subject ?: ""), text)

        }
        viewCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun startSend(subject: String?, text: CharSequence) {
        val builder = ShareCompat.IntentBuilder.from(activity)
        builder.addEmailTo("living.solutions.vn@gmail.com")
        if (subject != null)
            builder.setSubject(subject)
        builder.setText(text)
        builder.setType("text/plain")
        builder.setChooserTitle(R.string.mail_intent_chooser)
        builder.startChooser()
    }

    private fun subject(title: String): String {
        val builder = StringBuilder(title).append(": ")
        val application = (activity as WalletTransactionsActivity).application
        val pi = application.packageInfo
        builder.append(BitcoinApplication.versionLine(pi))
        builder.append(", android ").append(Build.VERSION.RELEASE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            builder.append(" (").append(Build.VERSION.SECURITY_PATCH).append(")")
        builder.append(", ").append(Build.MANUFACTURER)
        if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true))
            builder.append(' ').append(Build.BRAND)
        builder.append(' ').append(Build.MODEL)
        return builder.toString()
    }

    @Throws(IOException::class)
    private fun appendApplicationInfo(
        report: Appendable,
        application: BitcoinApplication
    ): Appendable {
        val pi = application.packageInfo
        val configuration = application.config
        val calendar = GregorianCalendar(UTC)

        report.append("Version: " + pi.versionName + " (" + pi.versionCode + ")\n")
        report.append("Package: " + pi.packageName + "\n")
        report.append("Test/Prod: " + (if (Constants.TEST) "test" else "prod") + "\n")
        report.append("Timezone: " + TimeZone.getDefault().id + "\n")
        calendar.timeInMillis = System.currentTimeMillis()
        report.append(
            "Time: " + String.format(
                Locale.US,
                "%tF %tT %tZ",
                calendar,
                calendar,
                calendar
            ) + "\n"
        )
        calendar.timeInMillis = BitcoinApplication.TIME_CREATE_APPLICATION
        report.append(
            "Time of launch: " + String.format(
                Locale.US,
                "%tF %tT %tZ",
                calendar,
                calendar,
                calendar
            ) + "\n"
        )
        calendar.timeInMillis = pi.lastUpdateTime
        report.append(
            "Time of last update: " + String.format(
                Locale.US,
                "%tF %tT %tZ",
                calendar,
                calendar,
                calendar
            ) + "\n"
        )
        calendar.timeInMillis = pi.firstInstallTime
        report.append(
            "Time of first install: " + String.format(
                Locale.US,
                "%tF %tT %tZ",
                calendar,
                calendar,
                calendar
            )
                    + "\n"
        )
        val lastBackupTime = configuration.getLastBackupTime()
        calendar.timeInMillis = lastBackupTime
        report.append(
            "Time of backup: "
                    + (if (lastBackupTime > 0) String.format(
                Locale.US,
                "%tF %tT %tZ",
                calendar,
                calendar,
                calendar
            ) else "none")
                    + "\n"
        )
        report.append("Network: " + Constants.NETWORK_PARAMETERS.id + "\n")
        val wallet = viewModel.wallet.value
        report.append("Encrypted: " + wallet?.isEncrypted + "\n")
        report.append("Keychain size: " + wallet?.keyChainGroupSize + "\n")

        val transactions = wallet?.getTransactions(true)
        var numInputs = 0
        var numOutputs = 0
        var numSpentOutputs = 0
        if (transactions != null) {
            for (tx in transactions) {
                numInputs += tx.inputs.size
                val outputs = tx.outputs
                numOutputs += outputs.size
                for (txout in outputs) {
                    if (!txout.isAvailableForSpending)
                        numSpentOutputs++
                }
            }
        }
        report.append("Transactions: " + transactions?.size + "\n")
        report.append("Inputs: $numInputs\n")
        report.append("Outputs: $numOutputs (spent: $numSpentOutputs)\n")
        report.append(
            "Last block seen: " + wallet?.lastBlockSeenHeight + " (" + wallet?.lastBlockSeenHash + ")\n"
        )

        report.append("Databases:")
        for (db in application.databaseList())
            report.append(" $db")
        report.append("\n\nBy clicking this report. You have agreed to the terms of privacy.\n")
        return report
    }

    companion object {
        fun show(
            activity: AppCompatActivity,
            titleResId: Int,
            messageResId: Int,
            subject: String,
            contextualData: String
        ) {
            val bundle = Bundle {
                putInt(KEY_TITLE, titleResId)
                putInt(KEY_MESSAGE, messageResId)
                putString(KEY_SUBJECT, subject)
                putString(KEY_CONTEXTUAL_DATA, contextualData)
            }
            val fragment = ReportIssueDialog()
            fragment.arguments = bundle
            fragment.isCancelable = false
            fragment.show(activity.supportFragmentManager, ReportIssueDialog::class.java.name)
        }

        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_SUBJECT = "subject"
        private const val KEY_CONTEXTUAL_DATA = "contextual_data"
        private val UTC = TimeZone.getTimeZone("UTC")
    }
}