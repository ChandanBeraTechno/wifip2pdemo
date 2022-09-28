package com.techno.bobackwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    public static String thisDeviceName = "";
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private FileActivity activity;
    WifiP2pManager.PeerListListener myPeerListListener;
    WifiP2pManager.ConnectionInfoListener infoListener;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       FileActivity activity, WifiP2pManager.ConnectionInfoListener infoListener) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
        this.infoListener=infoListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
            } else {
                // Wi-Fi P2P is not enabled
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                manager.requestPeers(channel, myPeerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                //connected
                manager.requestConnectionInfo(channel, infoListener);
                Log.d("TAG", "onReceive: connected");
                Toast.makeText(activity,"Connected",Toast.LENGTH_LONG).show();
            } else {
                //disconnected
                Log.d("TAG", "onReceive: disconnected");
                Toast.makeText(activity,"disconnected",Toast.LENGTH_LONG).show();
            }

            Log.d("BroadCast", "WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION");

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            WiFiDirectBroadcastReceiver.thisDeviceName = device.deviceName;
        }

    }

    public void setPeerListListener(WifiP2pManager.PeerListListener peerListListener) {
        this.myPeerListListener = peerListListener;
    }

}