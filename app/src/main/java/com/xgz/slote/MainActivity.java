package com.xgz.slote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private final String SLOTE_AP_NANE = "bong_tech";
    private final String SLOTE_AP_PASSWORD = "ihuangshang";
    @InjectView(R.id.button_search)
    Button buttonSearch;
    @InjectView(R.id.progress_search)
    ProgressBar progressSearch;
    @InjectView(R.id.text_search)
    TextView textSearch;
    @InjectView(R.id.imageView)
    ImageView imageView;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        progressSearch.setVisibility(View.GONE);
        textSearch.setVisibility(View.GONE);
    }

    WifiManager wifiManager;

    private void searchWifi() {
        Animation animation=AnimationUtils.loadAnimation(this,R.anim.search_anim);
        imageView.startAnimation(animation);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, intentFilter);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            if (isWifiConnected(getApplicationContext())) {
                WifiInfo info = wifiManager.getConnectionInfo();
                String ssid = info.getSSID();
                if (!("\"" + SLOTE_AP_NANE + "\"").equals(ssid)) {
                    wifiManager.disconnect();
                    WifiConfiguration configuration = isExsits2(ssid);
                    if (configuration != null) {
                        wifiManager.removeNetwork(configuration.networkId);
                        if (wifiManager.saveConfiguration())
                            Log.i("remove", "ok");
                        else
                            Log.i("remove", "failed");
                    }
                    searchWifi2();
                } else {
                    textSearch.setText("已经连接上斯洛特");
                }
            } else {
                searchWifi2();
            }
        } else {
            wifiManager.setWifiEnabled(true);
            searchWifi2();
        }
    }

    private void searchWifi2() {
        if (wifiManager.startScan())
            Log.i("wifi", "scan");
        else
            Log.i("wifi", "scan failed");
        //List<ScanResult> scanResults = wifiManager.getScanResults();
    }

    private void linkWifi() {
        WifiConfiguration config = createWifiInfo(SLOTE_AP_NANE, SLOTE_AP_PASSWORD, 3, "wt");
        int wcgID = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(wcgID, true);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                switch (wifistate) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        break;
                }
            }
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                if (intent.hasExtra("networkInfo")) {
                    NetworkInfo networkInfo = intent.getParcelableExtra("networkInfo");
                    if (networkInfo.isConnected()) {
                        String ssid = networkInfo.getExtraInfo();
                        if (("\"" + SLOTE_AP_NANE + "\"").equals(ssid)) {
                            progressRunnable.progressText = "已连接到设备，正在设置...";
                            new MyTask().execute(0);
                        } else {
                            handler.removeCallbacks(progressRunnable);
                            progressRunnable.progressText = "设备连接异常，请重试";
                            handler.post(progressRunnable);
                            reset();
                        }
                    } else {

                    }
                }
            }
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // wifi已成功扫描到可用wifi。
                List<ScanResult> scanResults = wifiManager.getScanResults();
                for (ScanResult result : scanResults) {
                    if (SLOTE_AP_NANE.equals(result.SSID)) {
                        progressRunnable.progressText = "已搜索到设备，正在连接...";
                        linkWifi();
                        break;
                    }
                }
            }
        }
    };

    //是否连接WIFI
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private int getMaxPriority() {
        List<WifiConfiguration> localList = this.wifiManager
                .getConfiguredNetworks();
        int i = 0;
        Iterator<WifiConfiguration> localIterator = localList.iterator();
        while (true) {
            if (!localIterator.hasNext())
                return i;
            WifiConfiguration localWifiConfiguration = localIterator
                    .next();
            if (localWifiConfiguration.priority <= i)
                continue;
            i = localWifiConfiguration.priority;
        }
    }

    private int shiftPriorityAndSave() {
        List<WifiConfiguration> localList = this.wifiManager
                .getConfiguredNetworks();
        sortByPriority(localList);
        int i = localList.size();
        for (int j = 0; ; ++j) {
            if (j >= i) {
                this.wifiManager.saveConfiguration();
                return i;
            }
            WifiConfiguration localWifiConfiguration = (WifiConfiguration) localList
                    .get(j);
            localWifiConfiguration.priority = j;
            this.wifiManager.updateNetwork(localWifiConfiguration);
        }
    }

    private void sortByPriority(List<WifiConfiguration> paramList) {
        Collections.sort(paramList, new SjrsWifiManagerCompare());
    }

    private class ProgressRunnable implements Runnable {

        String progressText;
        int progress = 0;

        @Override
        public void run() {
            progress++;
            progressSearch.setProgress(progress);
            textSearch.setText(progressText + progress + "%");
            if (progress < 100)
                handler.postDelayed(progressRunnable, 500);
            else {
                textSearch.setText("操作超时，请重试！");
            }
        }
    }

    private ProgressRunnable progressRunnable = new ProgressRunnable();

    private void reset() {
        buttonSearch.setText("点击搜索附近的斯洛特");
    }

    @OnClick(R.id.button_search)
    public void onViewClicked() {
        if (buttonSearch.getText().toString().contains("点击搜索")) {
            buttonSearch.setText("取消搜索");
            progressSearch.setVisibility(View.VISIBLE);
            textSearch.setVisibility(View.VISIBLE);
            progressRunnable.progressText = "正在搜索...请耐心等待。";
            progressRunnable.progress = 0;
            handler.post(progressRunnable);
            searchWifi();
        } else if (buttonSearch.getText().toString().contains("取消搜索")) {
            handler.removeCallbacks(progressRunnable);
            buttonSearch.setText("点击搜索附近的斯洛特");
            progressSearch.setVisibility(View.GONE);
            textSearch.setVisibility(View.GONE);
        }
    }

    class SjrsWifiManagerCompare implements Comparator<WifiConfiguration> {
        public int compare(WifiConfiguration paramWifiConfiguration1,
                           WifiConfiguration paramWifiConfiguration2) {
            return paramWifiConfiguration1.priority
                    - paramWifiConfiguration2.priority;
        }
    }

    public WifiConfiguration setMaxPriority(WifiConfiguration config) {
        int priority = getMaxPriority() + 1;
        if (priority > 99999) {
            priority = shiftPriorityAndSave();
        }

        config.priority = priority;
        wifiManager.updateNetwork(config);

        // 本机之前配置过此wifi热点，直接返回
        return config;
    }

    /**
     * 是否存在网络信息
     *
     * @param str 热点名称
     * @return
     */
    private WifiConfiguration isExsits(String str) {
        Iterator localIterator = wifiManager.getConfiguredNetworks().iterator();
        WifiConfiguration localWifiConfiguration;
        do {
            if (!localIterator.hasNext()) return null;
            localWifiConfiguration = (WifiConfiguration) localIterator.next();
        } while (!localWifiConfiguration.SSID.equals("\"" + str + "\""));
        return localWifiConfiguration;
    }

    /**
     * 是否存在网络信息
     *
     * @param str 热点名称
     * @return
     */
    private WifiConfiguration isExsits2(String str) {
        Iterator localIterator = wifiManager.getConfiguredNetworks().iterator();
        WifiConfiguration localWifiConfiguration;
        do {
            if (!localIterator.hasNext()) return null;
            localWifiConfiguration = (WifiConfiguration) localIterator.next();
        } while (!localWifiConfiguration.SSID.equals(str));
        return localWifiConfiguration;
    }

    /**
     * 创建一个wifi信息
     *
     * @param ssid     名称
     * @param passawrd 密码
     * @param paramInt 有3个参数，1是无密码，2是简单密码，3是wap加密
     * @param type     是"ap"还是"wifi"
     * @return
     */
    public WifiConfiguration createWifiInfo(String ssid, String passawrd, int paramInt, String type) {
        //配置网络信息类
        WifiConfiguration localWifiConfiguration1 = new WifiConfiguration();
        //设置配置网络属性
        localWifiConfiguration1.allowedAuthAlgorithms.clear();
        localWifiConfiguration1.allowedGroupCiphers.clear();
        localWifiConfiguration1.allowedKeyManagement.clear();
        localWifiConfiguration1.allowedPairwiseCiphers.clear();
        localWifiConfiguration1.allowedProtocols.clear();

        if (type.equals("wt")) { //wifi连接
            localWifiConfiguration1.SSID = ("\"" + ssid + "\"");
            WifiConfiguration localWifiConfiguration2 = isExsits(ssid);
            if (localWifiConfiguration2 != null) {
                wifiManager.removeNetwork(localWifiConfiguration2.networkId); //从列表中删除指定的网络配置网络
            }
            if (paramInt == 1) { //没有密码
                localWifiConfiguration1.wepKeys[0] = "";
                localWifiConfiguration1.allowedKeyManagement.set(0);
                localWifiConfiguration1.wepTxKeyIndex = 0;
            } else if (paramInt == 2) { //简单密码
                localWifiConfiguration1.hiddenSSID = true;
                localWifiConfiguration1.wepKeys[0] = ("\"" + passawrd + "\"");
            } else { //wap加密
                localWifiConfiguration1.preSharedKey = ("\"" + passawrd + "\"");
                localWifiConfiguration1.hiddenSSID = true;
                localWifiConfiguration1.allowedAuthAlgorithms.set(0);
                localWifiConfiguration1.allowedGroupCiphers.set(2);
                localWifiConfiguration1.allowedKeyManagement.set(1);
                localWifiConfiguration1.allowedPairwiseCiphers.set(1);
                localWifiConfiguration1.allowedGroupCiphers.set(3);
                localWifiConfiguration1.allowedPairwiseCiphers.set(2);
            }
        } else {//"ap" wifi热点
            localWifiConfiguration1.SSID = ssid;
            localWifiConfiguration1.allowedAuthAlgorithms.set(1);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            localWifiConfiguration1.allowedKeyManagement.set(0);
            localWifiConfiguration1.wepTxKeyIndex = 0;
            if (paramInt == 1) {  //没有密码
                localWifiConfiguration1.wepKeys[0] = "";
                localWifiConfiguration1.allowedKeyManagement.set(0);
                localWifiConfiguration1.wepTxKeyIndex = 0;
            } else if (paramInt == 2) { //简单密码
                localWifiConfiguration1.hiddenSSID = true;//网络上不广播ssid
                localWifiConfiguration1.wepKeys[0] = passawrd;
            } else if (paramInt == 3) {//wap加密
                localWifiConfiguration1.preSharedKey = passawrd;
                localWifiConfiguration1.allowedAuthAlgorithms.set(0);
                localWifiConfiguration1.allowedProtocols.set(1);
                localWifiConfiguration1.allowedProtocols.set(0);
                localWifiConfiguration1.allowedKeyManagement.set(1);
                localWifiConfiguration1.allowedPairwiseCiphers.set(2);
                localWifiConfiguration1.allowedPairwiseCiphers.set(1);
            }
        }
        return localWifiConfiguration1;
    }

    private class MyTask extends AsyncTask<Integer, Integer, String> {
        private Socket socket = new Socket();
        private final String hostname = "192.168.1.240";
        private final int port = 8888;
        OutputStream outputStream;

        @Override
        protected void onPreExecute() {
            //textSearch.setText("已连接到设备，正在设置...");
            //handler.removeCallbacks(progressRunnable);
        }

        @Override
        protected String doInBackground(Integer... integers) {
            try {
                SocketAddress address = new InetSocketAddress(hostname, port);
                socket.connect(address, 10000);
                outputStream = socket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                handler.removeCallbacks(progressRunnable);
                progressRunnable.progressText = "设置出错：" + result;
                textSearch.setText(progressRunnable.progressText);
            } else
                progressRunnable.progressText = "正在传输数据...";
        }

        public void write(byte... data) throws Exception {
            outputStream.write(data);
        }
    }
}
