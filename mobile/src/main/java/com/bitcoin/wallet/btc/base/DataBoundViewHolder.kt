/*
 * Created by Hoang Hiep on 11/7/18 9:32 AM
 * Copyright (c) 2018 Living Solutions. All rights reserved.
 */

package com.bitcoin.wallet.btc.base

import androidx.recyclerview.widget.RecyclerView

class DataBoundViewHolder<out T : RecyclerView.ViewHolder> constructor(val binding: T) :
    RecyclerView.ViewHolder(binding.itemView)
