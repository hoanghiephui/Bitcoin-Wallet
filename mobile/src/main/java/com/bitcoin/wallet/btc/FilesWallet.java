package com.bitcoin.wallet.btc;

import android.os.Environment;
import android.text.format.DateUtils;
import org.bitcoinj.core.NetworkParameters;

import java.io.File;

public class FilesWallet extends Constants {
    public static final long WALLET_AUTOSAVE_DELAY_MS = 3 * DateUtils.SECOND_IN_MILLIS;
    public static final File EXTERNAL_STORAGE_DIR = Environment.getExternalStorageDirectory();
    public static final File EXTERNAL_WALLET_BACKUP_DIR = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    public static final String ELECTRUM_SERVERS_FILENAME = "electrum-servers.txt";
    private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId()
            .equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet";
    public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf" + FILENAME_NETWORK_SUFFIX;
    public static final String WALLET_KEY_BACKUP_BASE58 = "key-backup-base58" + FILENAME_NETWORK_SUFFIX;
    public static final String WALLET_KEY_BACKUP_PROTOBUF = "key-backup-protobuf" + FILENAME_NETWORK_SUFFIX;
    public static final String EXTERNAL_WALLET_BACKUP = "bitcoin-wallet-backup" + FILENAME_NETWORK_SUFFIX;
    public static final String BLOCKCHAIN_FILENAME = "blockchain" + FILENAME_NETWORK_SUFFIX;
    public static final String CHECKPOINTS_FILENAME = "checkpoints" + FILENAME_NETWORK_SUFFIX + ".txt";
    public static final String FEES_FILENAME = "fees" + FILENAME_NETWORK_SUFFIX + ".txt";
}
