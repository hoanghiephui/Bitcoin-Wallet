package com.bitcoin.wallet.mobile.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import com.bitcoin.wallet.mobile.Constants;
import com.bitcoin.wallet.mobile.R;
import com.bitcoin.wallet.mobile.data.AddressBookDao;
import com.bitcoin.wallet.mobile.data.AddressBookEntry;
import com.bitcoin.wallet.mobile.utils.WalletUtils;
import com.bitcoin.wallet.mobile.viewmodel.SendViewModel;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ReceivingAddressViewAdapter extends ArrayAdapter<AddressBookEntry> {
    private final LayoutInflater inflater;
    private SendViewModel viewModel;
    private AddressBookDao addressBookDao;

    public ReceivingAddressViewAdapter(final Context context,
                                       SendViewModel viewModel,
                                       AddressBookDao addressBookDao) {
        super(context, 0);
        this.inflater = LayoutInflater.from(context);
        this.addressBookDao = addressBookDao;
        this.viewModel = viewModel;
    }

    @NotNull
    @Override
    public View getView(final int position, View view, @NotNull final ViewGroup parent) {
        if (view == null)
            view = inflater.inflate(R.layout.address_book_row, parent, false);
        final AddressBookEntry entry = getItem(position);
        if (entry != null) {
            ((TextView) view.findViewById(R.id.address_book_row_label)).setText(entry.getLabel());
            ((TextView) view.findViewById(R.id.address_book_row_address)).setText(WalletUtils.formatHash(
                    entry.getAddress(), Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE));
        }
        return view;
    }

    @NotNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                final String trimmedConstraint = constraint.toString().trim();
                final FilterResults results = new FilterResults();
                if (viewModel.getValidatedAddress() == null && !trimmedConstraint.isEmpty()) {
                    final List<AddressBookEntry> entries = addressBookDao.get(trimmedConstraint);
                    results.values = entries;
                    results.count = entries.size();
                } else {
                    results.values = Collections.emptyList();
                    results.count = 0;
                }
                return results;
            }

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults results) {
                setNotifyOnChange(false);
                clear();
                if (results.count > 0)
                    addAll((List<AddressBookEntry>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
