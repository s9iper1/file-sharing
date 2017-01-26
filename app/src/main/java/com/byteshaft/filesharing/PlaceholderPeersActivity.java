package com.byteshaft.filesharing;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.filesharing.utils.Helpers;
import com.byteshaft.filesharing.utils.FileSentReceiver;
import com.byteshaft.filesharing.utils.RadarView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.byteshaft.filesharing.utils.Helpers.intToInetAddress;

public class PlaceholderPeersActivity extends AppCompatActivity implements View.OnClickListener {
    private String mPort;
    private boolean mConnectionRequested;
    private boolean mScanRequested;
    private WifiManager mWifiManager;
    private RadarView mRadarView;
    private FrameLayout radarLayout;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0;
    private HashMap<Integer, ScanResult> results;
    private int sendCounter = 0;
    private ArrayList<String> arrayList;
    private ScanResult selectedScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peers_list);
//        mImagePath = getIntent().getExtras().getString("image_url");
        Button refreshButton = (Button) findViewById(R.id.button_refresh_peers);
        radarLayout = (FrameLayout) findViewById(R.id.radar_layout);
        refreshButton.setOnClickListener(this);
        arrayList = new ArrayList<>();
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        registerReceiver(
                mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mScanRequested = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }else{
            if (Helpers.locationEnabled()) {
                mWifiManager.startScan();
            } else {
                Toast.makeText(this, "location not enabled", Toast.LENGTH_SHORT).show();
            }
            //do something, permission was previously granted; or legacy device
        }
        mRadarView = (RadarView) findViewById(R.id.radarView);
        mRadarView.setShowCircles(true);
        startAnimation(mRadarView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission
            if (Helpers.locationEnabled()) {
                mWifiManager.startScan();
            } else {
                Toast.makeText(this, "location not enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void stopAnimation(View view) {
        if (mRadarView != null) mRadarView.stopAnimation();
    }

    public void startAnimation(View view) {
        if (mRadarView != null) mRadarView.startAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(
                wifiStateReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiStateReceiver);
        unregisterReceiver(mWifiScanReceiver);
    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && mScanRequested) {
                mScanRequested = false;
                radarLayout = (FrameLayout) findViewById(R.id.radar_layout);
                results = new HashMap<>();
                List<ScanResult> filteredResults = new ArrayList<>();
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                int index = 0;
                for (ScanResult result : mScanResults) {
                    if (result.SSID.startsWith("SH-")) {
                        Log.i("TAG", " Name "+ result.SSID);
                        results.put(index, result);
                        filteredResults.add(result);
                        LinearLayout layout = new LinearLayout(getApplicationContext());
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setFocusable(true);
                        layout.setClickable(true);
                        Log.i("TAG", "set id " + layout.getId());
                        LinearLayout.LayoutParams params = new LinearLayout
                                .LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        layout.setLayoutParams(params);
                        ImageButton imageView = new ImageButton(getApplicationContext());
                        imageView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        imageView.setId(index);
                        imageView.setImageResource(R.mipmap.ic_launcher);
                        TextView textView = new TextView(getApplicationContext());
                        textView.setText(result.SSID);
                        layout.addView(imageView);
                        layout.addView(textView);
                        layout.setX(10);
                        layout.setY(10);
                        radarLayout.addView(layout);
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (view instanceof ImageButton) {
                                    Log.i("TAG", "true");
                                    Log.i("TAG", "id " + view.getId());
                                    processClick(results.get(view.getId()));
                                    selectedScan = results.get(view.getId());
                                }
                            }
                        });
                    }
                }
            }
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_refresh_peers:
//                getListView().setAdapter(null);
                mScanRequested = true;
                if (Helpers.locationEnabled() && ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mWifiManager.startScan();
                } else {
                    Toast.makeText(this, "location not enabled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    protected void processClick(ScanResult scanResult) {
        ScanResult device = scanResult;
        String[] ssidData = device.SSID.split("-");
        mPort = Helpers.decodeString(ssidData[2]);
        if (mWifiManager.getConnectionInfo().getSSID().contains(device.SSID)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String hostIP = intToInetAddress(
                            mWifiManager.getDhcpInfo().serverAddress).toString().replace("/", "");
                    Iterator it = ActivitySendFile.selectedHashMap.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        System.out.println(pair.getKey() + " = " + pair.getValue());
                        arrayList.add(String.valueOf(pair.getKey()));

                    }
                    Log.i("TAG", " counter " + sendCounter);
                    if (arrayList.size() > 0 && sendCounter <= arrayList.size()) {
                        Log.i("TAG" , "item " + ActivitySendFile.selectedHashMap.get(arrayList.get(sendCounter)));
                    sendFileOverNetwork(hostIP, mPort,
                            ActivitySendFile.selectedHashMap.get(arrayList.get(sendCounter))
                            , new FileSentReceiver() {
                                @Override
                                public void onFileSent() {
                                    if (arrayList.size() > 0 && sendCounter < arrayList.size()) {
                                        sendCounter  = sendCounter+1;
                                        Log.i("TAG", " counter ++" + sendCounter);
                                        processClick(selectedScan);
                                    }
                                }
                            });
                    }
                }
            }).start();
        } else {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + device.SSID + "\"";
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            wifiManager.addNetwork(conf);
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + device.SSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    mConnectionRequested = true;
                    break;
                }
            }
        }
    }

    @WorkerThread
    public static void sendFileOverNetwork(String hostIP, String port, String filePath,
                                           FileSentReceiver fileSentReceiver) {
        try {
            Socket sock = new Socket(hostIP, Integer.valueOf(port));
            File myFile = new File(filePath);
            byte[] fileBytesArray = new byte[(int) myFile.length()];
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(fileBytesArray, 0, fileBytesArray.length);
            OutputStream os = sock.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(fileBytesArray.length);
            dos.write(fileBytesArray, 0, fileBytesArray.length);
            dos.flush();

            //Closing socket
            sock.close();
            fileSentReceiver.onFileSent();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo =
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected() && mConnectionRequested) {
                    mConnectionRequested = false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String hostIP = intToInetAddress(
                                    mWifiManager.getDhcpInfo().serverAddress).toString().replace("/", "");
                            try {
                                Thread.sleep(6000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
//                            sendFileOverNetwork(hostIP, mPort, mImagePath);
                        }
                    }).start();
                }
            }
        }
    };
}
