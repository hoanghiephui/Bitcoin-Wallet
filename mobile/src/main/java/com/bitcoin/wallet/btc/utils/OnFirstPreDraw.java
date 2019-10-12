package com.bitcoin.wallet.btc.utils;

import android.view.View;
import android.view.ViewTreeObserver;

import java.util.concurrent.atomic.AtomicBoolean;

public class OnFirstPreDraw implements ViewTreeObserver.OnPreDrawListener {
    private final ViewTreeObserver viewTreeObserver;
    private final Callback callback;
    private final AtomicBoolean fired = new AtomicBoolean(false);
    private OnFirstPreDraw(final ViewTreeObserver viewTreeObserver, final Callback callback) {
        this.viewTreeObserver = viewTreeObserver;
        this.callback = callback;
        viewTreeObserver.addOnPreDrawListener(this);
    }

    public static void listen(final View view, final Callback callback) {
        new OnFirstPreDraw(view.getViewTreeObserver(), callback);
    }

    @Override
    public boolean onPreDraw() {
        if (viewTreeObserver.isAlive())
            viewTreeObserver.removeOnPreDrawListener(this);

        if (!fired.getAndSet(true))
            return callback.onFirstPreDraw();
        return true;
    }

    public interface Callback {
        boolean onFirstPreDraw();
    }
}