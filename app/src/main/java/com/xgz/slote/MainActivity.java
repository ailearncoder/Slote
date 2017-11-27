
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
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        searchWifi();
    }

    WifiManager wifiManager;

    private void searchWifi() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            if (isWifiConnected(getApplicationContext())) {
                WifiInfo info = wifiManager.getConnectionInfo();
                String ssid = info.getSSID();
                if (!"\"solte_config\"".equals(ssid)) {
                    wifiManager.disconnect();
                    WifiConfiguration configuration = isExsits2(ssid);
                    if (configuration != null) {
                        wifiManager.removeNetwork(configuration.networkId);
                        if(wifiManager.saveConfiguration())
                            Log.i("remove","ok");
                        else
                            Log.i("remove","failed");
                    }
                    searchWifi2();
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
        if(wifiManager.startScan())
            Log.i("wifi","scan");
        else
            Log.i("wifi","scan failed");
        //List<ScanResult> scanResults = wifiManager.getScanResults();
    }

    private void linkWifi() {
        WifiConfiguration config = createWifiInfo("bong_tech", "ihuangshang", 3, "wt");
        int wcgID = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(wcgID, true);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
            }
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // wifi已成功扫描到可用wifi。
                List<ScanResult> scanResults = wifiManager.getScanResults();
                for (ScanResult result : scanResults) {
                    if ("bong_tech".equals(result.SSID)) {
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
}
