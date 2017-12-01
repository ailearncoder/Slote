package com.xgz.slote;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ConfigActivity extends AppCompatActivity {

    @InjectView(R.id.ssid)
    EditText ssid;
    @InjectView(R.id.password)
    EditText password;
    @InjectView(R.id.user_key)
    EditText userKey;
    @InjectView(R.id.svn_ip)
    EditText svnIp;
    @InjectView(R.id.svn_port)
    EditText svnPort;
    @InjectView(R.id.config_button)
    Button configButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        ButterKnife.inject(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("用户配置");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String txt = getIntent().getStringExtra("ssid");
        if (txt != null)
            ssid.setText(txt);
        txt = getIntent().getStringExtra("password");
        if (txt != null)
            password.setText(txt);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isEditOk(EditText editText) {
        if (editText.getText().toString().equals("")) {
            editText.requestFocus();
            editText.setError("输入不能为空");
            return false;
        }
        return true;
    }

    public boolean isInputOk() {
        if (!isEditOk(ssid))
            return false;
        if (!isEditOk(password))
            return false;
        if (!isEditOk(userKey))
            return false;
        if (!isEditOk(svnIp))
            return false;
        if (!isEditOk(svnPort))
            return false;
        return true;
    }

    @OnClick(R.id.config_button)
    public void onViewClicked() {
        if (isInputOk()) {
            new MyTask().execute();
        }
    }

    class MyTask extends AsyncTask<Integer, String, Integer> {
        private ProgressDialog progressDialog = new ProgressDialog(ConfigActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle("正在配置");
            progressDialog.setMessage("请稍后...");
            progressDialog.setCancelable(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    progressDialog.cancel();
                }
            });
            progressDialog.show();
            progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            if (MainActivity.myTask == null) {
                publishProgress("通信连接中断");
                return 0;
            }
            try {
                String config = "%SSID=" + ssid.getText().toString() + "&%" + password.getText().toString() + "&#";
                MainActivity.myTask.write(config.getBytes());
                publishProgress(config,"33");
                Thread.sleep(1000);
                config = "%USERKEY=" + userKey.getText().toString() + "&#";
                MainActivity.myTask.write(config.getBytes());
                publishProgress(config,"66");
                Thread.sleep(1000);
                config = "%SVN=" + svnIp.getText().toString() + "&%" + svnPort.getText().toString() + "&#";
                MainActivity.myTask.write(config.getBytes());
                publishProgress(config,"100");
                Thread.sleep(1000);
            }catch (Exception e)
            {
                publishProgress("通信出错："+e.getMessage());
                return 0;
            }
            return 1;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(values[0]);
            progressDialog.setProgress(Integer.valueOf(values[1]));
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer==1)
            {
                progressDialog.setMessage("配置成功");
            }
            progressDialog.setCancelable(true);
            progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        }
    }
}
