package com.bitcoin.wallet.btc.service;

import android.content.Intent;

import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

public class BlockchainState {
    private static final String EXTRA_BEST_CHAIN_DATE = "best_chain_date";
    private static final String EXTRA_BEST_CHAIN_HEIGHT = "best_chain_height";
    private static final String EXTRA_REPLAYING = "replaying";
    private static final String EXTRA_IMPEDIMENTS = "impediment";

    public enum Impediment {
        STORAGE, NETWORK
    }

    public final Date bestChainDate;
    public final int bestChainHeight;
    public final boolean replaying;
    public final EnumSet<Impediment> impediments;

    public BlockchainState(final Date bestChainDate, final int bestChainHeight, final boolean replaying,
                           final Set<Impediment> impediments) {
        this.bestChainDate = bestChainDate;
        this.bestChainHeight = bestChainHeight;
        this.replaying = replaying;
        this.impediments = EnumSet.copyOf(impediments);
    }

    public static BlockchainState fromIntent(final Intent intent) {
        final Date bestChainDate = (Date) intent.getSerializableExtra(EXTRA_BEST_CHAIN_DATE);
        final int bestChainHeight = intent.getIntExtra(EXTRA_BEST_CHAIN_HEIGHT, -1);
        final boolean replaying = intent.getBooleanExtra(EXTRA_REPLAYING, false);
        @SuppressWarnings("unchecked")
        final Set<Impediment> impediments = (Set<Impediment>) intent.getSerializableExtra(EXTRA_IMPEDIMENTS);
        if (bestChainDate != null && bestChainHeight != -1 && impediments != null)
            return new BlockchainState(bestChainDate, bestChainHeight, replaying, impediments);
        else
            return null;
    }

    public void putExtras(final Intent intent) {
        intent.putExtra(EXTRA_BEST_CHAIN_DATE, bestChainDate);
        intent.putExtra(EXTRA_BEST_CHAIN_HEIGHT, bestChainHeight);
        intent.putExtra(EXTRA_REPLAYING, replaying);
        intent.putExtra(EXTRA_IMPEDIMENTS, impediments);
    }
}

