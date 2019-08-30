package com.mhyun.starspeed.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.mhyun.starspeed.K;
import com.mhyun.starspeed.R;
import com.mhyun.starspeed.Utils;
import com.mhyun.starspeed.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by MHyun on 2017/11/3.
 */

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    ActivityMainBinding binding;
    int REQUEST_CODE_STATUS = 0x1024;
    JSONObject SKIP_MODE = new JSONObject();
    JSONObject QUICKEN_MODE = new JSONObject();
    JSONObject PROGRESS_MODE = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initView();
        onListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STATUS);
    }

    @SuppressLint("WorldReadableFiles")
    private void initView() {
        try {
            SKIP_MODE.put(K.SKIP_MODE, false);
            QUICKEN_MODE.put(K.QUICKEN_MODE, false);
            PROGRESS_MODE.put(K.PROGRESS_MODE, false);
            JSONArray settings = Utils.getSettings();
            if (settings != null) {
                SKIP_MODE.put(K.SKIP_MODE, settings.getJSONObject(0).getBoolean(K.SKIP_MODE));
                QUICKEN_MODE.put(K.QUICKEN_MODE, settings.getJSONObject(1).getBoolean(K.QUICKEN_MODE));
                PROGRESS_MODE.put(K.PROGRESS_MODE, settings.getJSONObject(2).getBoolean(K.PROGRESS_MODE));
                binding.switchSkip.setChecked(SKIP_MODE.getBoolean(K.SKIP_MODE));
                binding.switchQuicken.setChecked(QUICKEN_MODE.getBoolean(K.QUICKEN_MODE));
                binding.switchProgress.setChecked(PROGRESS_MODE.getBoolean(K.PROGRESS_MODE));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void onListener() {
        binding.switchSkip.setOnCheckedChangeListener(this);
        binding.switchQuicken.setOnCheckedChangeListener(this);
        binding.switchProgress.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        try {
            switch (compoundButton.getId()) {
                case R.id.switch_skip:
                    SKIP_MODE.put(K.SKIP_MODE, b);
                    break;
                case R.id.switch_quicken:
                    QUICKEN_MODE.put(K.QUICKEN_MODE, b);
                    break;
                case R.id.switch_progress:
                    PROGRESS_MODE.put(K.PROGRESS_MODE, b);
                    break;
            }
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(SKIP_MODE);
            jsonArray.put(QUICKEN_MODE);
            jsonArray.put(PROGRESS_MODE);
            Utils.saveSettings(jsonArray.toString());
        } catch (JSONException | IOException e) {
            Log.i(K.PackageName, "存储JSON错误");
        }
        Toast.makeText(MainActivity.this, getString(R.string.prompt), Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_STATUS) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED)
                this.finish();
        }
    }
}
