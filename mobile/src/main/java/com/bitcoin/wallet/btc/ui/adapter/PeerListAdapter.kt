package com.bitcoin.wallet.btc.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.inflate
import org.bitcoinj.core.Peer
import java.net.InetAddress
import java.util.*

class PeerListAdapter :
    ListAdapter<PeerListAdapter.ListItem, PeerListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.ip == newItem.ip
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            if (oldItem.hostname != newItem.hostname)
                return false
            if (oldItem.ping != newItem.ping)
                return false
            return oldItem.isDownloading == newItem.isDownloading
        }
    }) {


    class ListItem(context: Context, peer: Peer, hostnames: Map<InetAddress, String>) {

        val ip: InetAddress = peer.address.addr
        val hostname: String?
        val height: Long
        val version: String
        val protocol: String
        val services: String
        val ping: String?
        val isDownloading: Boolean

        init {
            this.hostname = hostnames[ip]
            this.height = peer.bestHeight
            val versionMessage = peer.peerVersionMessage
            this.version = versionMessage.subVer
            this.protocol = "protocol: " + versionMessage.clientVersion
            this.services = peer.toStringServices(versionMessage.localServices).toLowerCase(Locale.US)
            val pingTime = peer.pingTime
            this.ping = if (pingTime < java.lang.Long.MAX_VALUE)
                context.getString(R.string.ping_time, pingTime)
            else
                null
            this.isDownloading = peer.isDownloadData
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_peer))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listItem = getItem(position)
        holder.ipView.text = listItem.hostname ?: listItem.ip.hostAddress
        holder.heightView.text = if (listItem.height > 0) listItem.height.toString() + " blocks" else null
        holder.heightView.typeface = if (listItem.isDownloading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.versionView.text = listItem.version
        holder.versionView.typeface = if (listItem.isDownloading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.protocolView.text = listItem.protocol
        holder.protocolView.typeface = if (listItem.isDownloading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.servicesView.text = listItem.services
        holder.servicesView.typeface = if (listItem.isDownloading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.pingView.text = listItem.ping
        holder.pingView.typeface = if (listItem.isDownloading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    class ViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ipView: TextView = itemView.findViewById<View>(R.id.peer_list_row_ip) as TextView
        val heightView: TextView = itemView.findViewById<View>(R.id.peer_list_row_height) as TextView
        val versionView: TextView = itemView.findViewById<View>(R.id.peer_list_row_version) as TextView
        val protocolView: TextView = itemView.findViewById<View>(R.id.peer_list_row_protocol) as TextView
        val servicesView: TextView = itemView.findViewById<View>(R.id.peer_list_row_services) as TextView
        val pingView: TextView = itemView.findViewById<View>(R.id.peer_list_row_ping) as TextView

    }

    companion object {
        fun buildListItems(
            context: Context, peers: List<Peer>,
            hostnames: Map<InetAddress, String>
        ): List<ListItem> {
            val items = ArrayList<ListItem>(peers.size)
            for (peer in peers)
                items.add(ListItem(context, peer, hostnames))
            return items
        }
    }
}