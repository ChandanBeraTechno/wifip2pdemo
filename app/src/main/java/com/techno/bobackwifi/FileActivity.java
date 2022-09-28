package com.techno.bobackwifi;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class FileActivity extends AppCompatActivity {

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WiFiDirectBroadcastReceiver receiver;
    IntentFilter intentFilter;

    private RecyclerView rvSendingFilesList;
    private RecyclerView rvReceivingFilesList;
    private TextView tvSendOrReceive;
    private TextView btnSend;
    private final ArrayList peerList = new ArrayList();
    private PeersAdapter peersAdapter;
    private FilesSendAdapter sendFilesAdapter;
    private FilesAdapter receiveFilesAdapter;
    private FileServerAsyncTask fileServerAsyncTask;
    private InetAddress serverAddress;
    private ServerSocket serverSocket;
    private ServerSocket serverSocketDevice;
    private DeviceInfoServerAsyncTask deviceInfoServerAsyncTask;

    private Callback callbackReInitFileServer;
    private Callback callbackReInitDeviceServer;
    private Callback callbackSendThisDeviceName;
    private boolean isSend = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
                    @Override public void onPermissionsChecked(MultiplePermissionsReport report) {
                        /* ... */}
                    @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();}
                }).check();

        tvSendOrReceive = findViewById(R.id.tvSendOrReceive);
        btnSend = findViewById(R.id.btnSend);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        WifiP2pManager.ConnectionInfoListener infoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                serverAddress = wifiP2pInfo.groupOwnerAddress;

                if (serverAddress == null) return;
               callbackSendThisDeviceName.call();
                btnSend.setVisibility(View.VISIBLE);
                //
            }
        };

        WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                Log.d("TAG", "onPeersAvailable: " + wifiP2pDeviceList.getDeviceList());
                peerList.clear();
                peerList.addAll(wifiP2pDeviceList.getDeviceList());
                peersAdapter.updateList(peerList);
                peersAdapter.notifyDataSetChanged();
            }
        };
        //Set callback for send device name to device, who was connected. (not available in Android’s official API)
        callbackSendThisDeviceName = () -> {
            TransferNameDevice transferNameDevice = new TransferNameDevice(serverAddress);
            transferNameDevice.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        };

        //On the specified device p2p are disabled, to enable it I use
        try {
            Class<?> wifiManager = Class
                    .forName("android.net.wifi.p2p.WifiP2pManager");

            Method method = wifiManager
                    .getMethod("enableP2p",
                            WifiP2pManager.Channel.class);
            method.invoke(manager, channel);
        } catch (Exception ignored) {
        }

        //Just in case, I delete the group, since after an instant restart of the application,
        // Dalvik doesn't clean the application immediately, but with it both the manager and the channel
        manager.removeGroup(channel, null);



        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, infoListener);
        receiver.setPeerListListener(peerListListener);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //Setup peers adapter
        peersAdapter = new PeersAdapter(peerList, this,
                manager, channel, this, infoListener);
        RecyclerView rvDevicesList = findViewById(R.id.rvDevicesList);
        rvDevicesList.setAdapter(peersAdapter);


        //Find sending file list and set adapter
        rvSendingFilesList = findViewById(R.id.rvSendingFilesList);
        LinearLayoutManager filesListLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false);
        rvSendingFilesList.setLayoutManager(filesListLayoutManager);
        sendFilesAdapter = new FilesSendAdapter();
        rvSendingFilesList.setAdapter(sendFilesAdapter);

        //Find receiving file list and set adapter
        rvReceivingFilesList = findViewById(R.id.rvReceivingFilesList);
        LinearLayoutManager receiveFilesListLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false);
        rvReceivingFilesList.setLayoutManager(receiveFilesListLayoutManager);
        receiveFilesAdapter = new FilesAdapter(this);
        rvReceivingFilesList.setAdapter(receiveFilesAdapter);


        //init sockets for transport servers and callbacks for reinit servers
        this.initSockets();
        callbackReInitFileServer = FileActivity.this::initFileServer;
        callbackReInitDeviceServer = FileActivity.this::initDeviceInfoServers;
        this.initFileServer(); // Init file server for receiving data

        // Init Device info server for receiving device name who connected
        this.initDeviceInfoServers();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false);
        rvDevicesList.setLayoutManager(mLayoutManager);

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("TAG", "onSuccess: ");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d("TAG", "onFailure: ");
                if (reasonCode == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d("TAG", "P2P isn't supported on this device.");

                }
            }
        });

        tvSendOrReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isSend) {
                    isSend = false;
                    tvSendOrReceive.setText("Sending");
                    rvSendingFilesList.setVisibility(View.VISIBLE);
                    rvReceivingFilesList.setVisibility(View.INVISIBLE);
                } else {
                    isSend = true;
                    tvSendOrReceive.setText("Receiving");
                    rvSendingFilesList.setVisibility(View.INVISIBLE);
                    rvReceivingFilesList.setVisibility(View.VISIBLE);
                }
            }

        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChooseFile.fileChooser(FileActivity.this);

            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);

    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        manager.cancelConnect(channel, null);
        unregisterReceiver(receiver);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            manager.stopPeerDiscovery(channel, null);
        }

        try {
            serverSocket.close();
            serverSocketDevice.close();
            fileServerAsyncTask.cancel(true);
            deviceInfoServerAsyncTask.cancel(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        deletePersistentGroups();

    }

    private void deletePersistentGroups() {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        method.invoke(manager, channel, netid, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("TAG", "onActivityResult: "+requestCode);
        Log.d("TAG", "onActivityResult: "+resultCode);
        switch (requestCode) {
            case ChooseFile.FILE_TRANSFER_CODE:
                if (data == null) return;

                ArrayList<Uri> uris = new ArrayList<>();
                ArrayList<Long> filesLength = new ArrayList<>();
                ArrayList<String> fileNames = new ArrayList<>();

                try {
                    ClipData clipData = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        clipData = data.getClipData();
                    }

                    if (clipData != null) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            uris.add(clipData.getItemAt(i).getUri());

                            String fileName =
                                    PathUtil.getPath(getApplicationContext(), clipData.getItemAt(i).getUri());
                            filesLength.add(new File(fileName).length());

                            fileName = FilesUtil.getFileName(fileName);
                            fileNames.add(fileName);

                            Log.d("File URI", clipData.getItemAt(i).getUri().toString());
                            Log.d("File Path", fileName);
                        }
                    } else {
                        Uri uri = data.getData();
                        uris.add(uri);

                        String fileName = PathUtil.getPath(getApplicationContext(), uri);
                    //    String fileName = uriToFilename(uri);
                        Log.d("TAG", "onActivityResult: "+fileName);
                        Log.d("TAG", "onActivityResult:2 "+filesLength.add(new File(fileName).length()));
                        filesLength.add(new File(fileName).length());

                        fileName = FilesUtil.getFileName(fileName);
                        fileNames.add(fileName);
                    }
                    sendFilesAdapter.notifyAdapter(uris, filesLength, fileNames);

                    TransferData transferData = new TransferData(FileActivity.this,
                            uris, filesLength, fileNames, (sendFilesAdapter), serverAddress, manager, channel);
                    transferData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }


    private void initDeviceInfoServers() {

        deviceInfoServerAsyncTask = new DeviceInfoServerAsyncTask(
                (serverSocketDevice),
                (FileActivity.this.peersAdapter), callbackReInitDeviceServer);
        deviceInfoServerAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initFileServer() {

        fileServerAsyncTask = new FileServerAsyncTask(
                (FileActivity.this),
                (serverSocket),
                (FileActivity.this.receiveFilesAdapter), callbackReInitFileServer);
        fileServerAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private void initSockets() {
        try {
            serverSocketDevice = new ServerSocket(8887);
            serverSocket = new ServerSocket(8888);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String uriToFilename(Uri uri) {
        String path = null;

        if (Build.VERSION.SDK_INT < 11) {
            path = RealPathUtil.getRealPathFromURI_BelowAPI11(this, uri);
        } else if (Build.VERSION.SDK_INT < 19) {
            path = RealPathUtil.getRealPathFromURI_API11to18(this, uri);
        } else {
            path = RealPathUtil.getRealPathFromURI_API19(this, uri);
        }

        return path;
    }
}