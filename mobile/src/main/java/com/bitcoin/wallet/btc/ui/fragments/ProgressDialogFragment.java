package com.bitcoin.wallet.btc.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class ProgressDialogFragment extends DialogFragment {
    public static class Observer implements androidx.lifecycle.Observer<String> {
        private final FragmentManager fm;

        public Observer(final FragmentManager fm) {
            this.fm = fm;
        }

        @Override
        public void onChanged(final String message) {
            if (message != null) {
                final ProgressDialogFragment fragment = new ProgressDialogFragment();
                final Bundle args = new Bundle();
                args.putString(KEY_MESSAGE, message);
                fragment.setArguments(args);
                fragment.show(fm, FRAGMENT_TAG);
            } else {
                final DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(FRAGMENT_TAG);
                if (fragment != null)
                    fragment.dismiss();
            }
        }
    }

    private static final String FRAGMENT_TAG = ProgressDialogFragment.class.getName();
    private static final String KEY_MESSAGE = "message";

    private Activity activity;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        this.activity = (Activity) context;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final String message = args.getString(KEY_MESSAGE);

        return ProgressDialog.show(activity, null, message, true);
    }
}