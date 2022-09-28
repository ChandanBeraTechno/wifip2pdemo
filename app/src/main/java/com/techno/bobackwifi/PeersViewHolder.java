package com.techno.bobackwifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class PeersViewHolder extends RecyclerView.ViewHolder {

  private TextView peerName;
  public ImageView statePeer;
  public TextView itemSyncing;

  public View peerView;
  public WifiP2pDevice device;


  public PeersViewHolder(View itemView) {
    super(itemView);
    peerName = itemView.findViewById(R.id.tvItemTitle);
  statePeer = itemView.findViewById(R.id.ivItemSyncStatus);
    itemSyncing = itemView.findViewById(R.id.tvItemSyncing);

    peerView = itemView;
  }

  public void setPeer(WifiP2pDevice peer) {
    device = peer;
    peerName.setText(peer.deviceName);
    Log.d("Set Peer", "setPeer");
    Log.d("Set Peer Name", "setPeer"+peer.deviceName);
  }
}
