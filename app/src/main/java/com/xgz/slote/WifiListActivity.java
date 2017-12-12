package com.xgz.slote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class WifiListActivity extends Activity {

    @InjectView(R.id.list_wifi)
    ListView listWifi;
    private Menu menu;
    private WifiManager wifiManager;
    private ArrayAdapter<String> adapter;
    List<ScanResult> scanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_list);
        ButterKnife.inject(this);
        setTitle("选择WiFi");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listWifi.setAdapter(adapter);
        listWifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                showEditDialog(position);
            }
        });
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, intentFilter);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wifi, menu);
        this.menu = menu;
        refreshWifiList(menu.findItem(R.id.refresh));
        return super.onCreateOptionsMenu(menu);
    }

    boolean isRefresh = false;

    public void refreshWifiList(MenuItem item) {

        if (isRefresh) {
            item.setTitle("刷新wifi");
            item.setActionView(null);
        } else {
            Toast.makeText(this, "正在刷新", Toast.LENGTH_LONG).show();
            item.setActionView(R.layout.actionbar_indeterminate_progress);
            wifiManager.startScan();
        }

        isRefresh = !isRefresh;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.refresh:
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                }
            }
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanResult = wifiManager.getScanResults();
                adapter.clear();
                for (int i = 0; i < scanResult.size(); i++) {
                    ScanResult result = scanResult.get(i);
                    adapter.add(result.SSID + "\n" + result.BSSID + "   " + result.level + "dBm");
                }
                adapter.notifyDataSetChanged();
                refreshWifiList(menu.findItem(R.id.refresh));
            }
        }
    };
    private void showEditDialog(final int position)
    {
        final EditText editText=new EditText(this);
        editText.setHint("请输入密码");
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(scanResult.get(position).SSID);
        builder.setMessage("请输入WiFi密码");
        builder.setView(editText);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String password=editText.getText().toString();
                Intent intent=new Intent(WifiListActivity.this,ConfigActivity.class);
                intent.putExtra("ssid",scanResult.get(position).SSID);
                intent.putExtra("password",password);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton("取消",null);
        builder.create();
        builder.show();
    }
}
