package com.bitcoin.wallet.btc.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.TypefaceSpan;

import com.bitcoin.wallet.btc.Constants;
import com.bitcoin.wallet.btc.FilesWallet;
import com.bitcoin.wallet.btc.service.BlockchainService;
import com.google.common.base.Stopwatch;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;

public class WalletUtils {
    public static final FileFilter BACKUP_FILE_FILTER = file -> {
        try (final InputStream is = new FileInputStream(file)) {
            return WalletProtobufSerializer.isWallet(is);
        } catch (final IOException x) {
            return false;
        }
    };
    //private static final Logger log = LoggerFactory.getLogger(WalletUtils.class);

    public static Spanned formatAddress(final Address address, final int groupSize, final int lineSize, boolean isN) {
        return formatHash(address.toString(), groupSize, lineSize, isN);
    }

    public static Spanned formatAddress(@Nullable final String prefix, final Address address, final int groupSize,
                                        final int lineSize) {
        return formatHash(prefix, address.toString(), groupSize, lineSize, Constants.CHAR_THIN_SPACE, true);
    }

    public static Spanned formatHash(final String address, final int groupSize, final int lineSize) {
        return formatHash(null, address, groupSize, lineSize, Constants.CHAR_THIN_SPACE, true);
    }

    public static Spanned formatHash(final String address, final int groupSize, final int lineSize, boolean isN) {
        return formatHash(null, address, groupSize, lineSize, Constants.CHAR_THIN_SPACE, isN);
    }

    public static long longHash(final Sha256Hash hash) {
        final byte[] bytes = hash.getBytes();

        return (bytes[31] & 0xFFl) | ((bytes[30] & 0xFFl) << 8) | ((bytes[29] & 0xFFl) << 16)
                | ((bytes[28] & 0xFFl) << 24) | ((bytes[27] & 0xFFl) << 32) | ((bytes[26] & 0xFFl) << 40)
                | ((bytes[25] & 0xFFl) << 48) | ((bytes[23] & 0xFFl) << 56);
    }

    public static Spanned formatHash(@Nullable final String prefix, final String address, final int groupSize,
                                     final int lineSize, final char groupSeparator, boolean isN) {
        final SpannableStringBuilder builder = prefix != null ? new SpannableStringBuilder(prefix)
                : new SpannableStringBuilder();

        final int len = address.length();
        for (int i = 0; i < len; i += groupSize) {
            final int end = i + groupSize;
            final String part = address.substring(i, end < len ? end : len);

            builder.append(part);
            builder.setSpan(new MonospaceSpan(), builder.length() - part.length(), builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (end < len) {
                final boolean endOfLine = lineSize > 0 && end % lineSize == 0;
                builder.append(endOfLine && isN ? '\n' : groupSeparator);
            }
        }

        return SpannedString.valueOf(builder);
    }

    @Nullable
    public static Address getToAddressOfSent(final Transaction tx, final Wallet wallet) {
        for (final TransactionOutput output : tx.getOutputs()) {
            try {
                if (!output.isMine(wallet)) {
                    final Script script = output.getScriptPubKey();
                    return script.getToAddress(Constants.NETWORK_PARAMETERS, true);
                }
            } catch (final ScriptException x) {
                // swallow
            }
        }

        return null;
    }

    @Nullable
    public static Address getWalletAddressOfReceived(final Transaction tx, final Wallet wallet) {
        for (final TransactionOutput output : tx.getOutputs()) {
            try {
                if (output.isMine(wallet)) {
                    final Script script = output.getScriptPubKey();
                    return script.getToAddress(Constants.NETWORK_PARAMETERS, true);
                }
            } catch (final ScriptException x) {
                // swallow
            }
        }

        return null;
    }

    public static boolean isEntirelySelf(final Transaction tx, final Wallet wallet) {
        for (final TransactionInput input : tx.getInputs()) {
            final TransactionOutput connectedOutput = input.getConnectedOutput();
            if (connectedOutput == null || !connectedOutput.isMine(wallet))
                return false;
        }

        for (final TransactionOutput output : tx.getOutputs()) {
            if (!output.isMine(wallet))
                return false;
        }

        return true;
    }

    public static void autoBackupWallet(final android.content.Context context, final Wallet wallet) {
        if (context == null || wallet == null) {
            return;
        }
        final Stopwatch watch = Stopwatch.createStarted();
        final Protos.Wallet.Builder builder = new WalletProtobufSerializer().walletToProto(wallet).toBuilder();

        // strip redundant
        builder.clearTransaction();
        builder.clearLastSeenBlockHash();
        builder.setLastSeenBlockHeight(-1);
        builder.clearLastSeenBlockTimeSecs();
        final Protos.Wallet walletProto = builder.build();

        try (final OutputStream os = context.openFileOutput(FilesWallet.WALLET_FILENAME_PROTOBUF,
                android.content.Context.MODE_PRIVATE)) {
            walletProto.writeTo(os);
            watch.stop();
            //log.info("wallet backed up to: '{}', took {}", FilesWallet.WALLET_FILENAME_PROTOBUF, watch); todo log
        } catch (final IOException x) {
            //log.error("problem writing wallet backup", x);
        }
    }

    public static Wallet restoreWalletFromAutoBackup(final android.content.Context context) {
        try (final InputStream is = context.openFileInput(FilesWallet.WALLET_KEY_BACKUP_PROTOBUF)) {
            final Wallet wallet = new WalletProtobufSerializer().readWallet(is, true, null);
            if (!wallet.isConsistent())
                throw new Error("inconsistent backup");

            BlockchainService.resetBlockchain(context);
            //log.info("wallet restored from backup: '" + FilesWallet.WALLET_KEY_BACKUP_PROTOBUF + "'"); todo log
            return wallet;
        } catch (final IOException | UnreadableWalletException x) {
            throw new Error("cannot read backup", x);
        }
    }

    public static Wallet restoreWalletFromProtobuf(final InputStream is,
                                                   final NetworkParameters expectedNetworkParameters) throws IOException {
        try {
            final Wallet wallet = new WalletProtobufSerializer().readWallet(is, true, null);

            if (!wallet.getParams().equals(expectedNetworkParameters))
                throw new IOException("bad wallet backup network parameters: " + wallet.getParams().getId());
            if (!wallet.isConsistent())
                throw new IOException("inconsistent wallet backup");

            return wallet;
        } catch (final UnreadableWalletException x) {
            throw new IOException("unreadable wallet", x);
        }
    }

    public static boolean isPayToManyTransaction(final Transaction transaction) {
        return transaction.getOutputs().size() > 20;
    }

    private static class MonospaceSpan extends TypefaceSpan {
        public MonospaceSpan() {
            super("monospace");
        }

        // TypefaceSpan doesn't implement this, and we need it so that Spanned.equals() works.
        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            return o != null && o.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    public static int dpToPx(Context context, int dp) {
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
        if (dpi == 0) {
            return 0;
        }
        return (int) (dp * (dpi / 160.0));
    }

    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}