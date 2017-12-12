package com.xgz.slote;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity2 extends PermissionReqiureActivity {

    public static MyTask myTask;
    @InjectView(R.id.ip_addr)
    EditText ipAddr;
    @InjectView(R.id.ip_port)
    EditText ipPort;
    @InjectView(R.id.button)
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.inject(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.link_wifi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick(R.id.button)
    public void onViewClicked() {
        showDialog();
    }

    private void showDialog() {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("请确保连接到 斯洛特 配置WiFi\n并输入了正确的IP地址和端口号。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        myTask = new MyTask();
                        myTask.hostname = ipAddr.getText().toString();
                        myTask.port = Integer.valueOf(ipPort.getText().toString());
                        myTask.execute();
                    }
                })
                .create()
                .show();
    }

    public class MyTask extends AsyncTask<Integer, Integer, String> {
        private Socket socket = new Socket();
        public String hostname = "192.168.1.210";
        public int port = 8000;
        OutputStream outputStream;
        ProgressDialog progressDialog = new ProgressDialog(MainActivity2.this);

        @Override
        protected void onPreExecute() {
            //textSearch.setText("已连接到设备，正在设置...");
            //handler.removeCallbacks(progressRunnable);
            progressDialog.setTitle("提示");
            progressDialog.setCancelable(false);
            progressDialog.setMessage("正在连接...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Integer... integers) {
            try {
                SocketAddress address = new InetSocketAddress(hostname, port);
                socket.connect(address, 10000);
                outputStream = socket.getOutputStream();
                outputStream.write("Hello".getBytes());
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
            progressDialog.cancel();
            if (result != null) {
                Toast.makeText(MainActivity2.this,"连接失败："+result,Toast.LENGTH_LONG).show();
            } else {
                startActivity(new Intent(MainActivity2.this, WifiListActivity.class));
                //finish();
            }
        }

        public void write(byte... data) throws Exception {
            outputStream.write(data);
            outputStream.flush();
        }

        public void close() {
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (Exception e) {

            }
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e) {

            }
        }
    }

    @Override
    protected void onDestroy() {
        if (myTask != null)
            myTask.close();
        myTask = null;
        super.onDestroy();
    }
}
