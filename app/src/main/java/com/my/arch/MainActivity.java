package com.my.arch;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import dalvik.system.DexClassLoader;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.my.githubdemo.GithubMainActivity;

import java.io.File;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "hnl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.hello).setOnClickListener(this);
        findViewById(R.id.plugin).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.hello:
                startActivity(new Intent(MainActivity.this, GithubMainActivity.class));
                break;
            case R.id.plugin:
                loadPlugin();
                break;
        }
    }

    private void loadPlugin() {
        try {
            String apkName = "littleplugin-debug.apk";
            Utils.extractAssets(this, apkName);

            File extractFile = this.getFileStreamPath(apkName);
            String dexpath = extractFile.getPath();
            File fileRelease = getDir("dex", 0); //0 表示Context.MODE_PRIVATE

            Log.d(TAG, "dexpath:" + dexpath);
            Log.d(TAG, "fileRelease.getAbsolutePath():" +
                    fileRelease.getAbsolutePath());

            DexClassLoader classLoader = new DexClassLoader(dexpath,
                    fileRelease.getAbsolutePath(), null, getClassLoader());

            Class mLoadClassBean = classLoader.loadClass("com.my.littleplugin.Bean");
            Object beanObject = mLoadClassBean.newInstance();

            Method getNameMethod = mLoadClassBean.getMethod("getName");
            getNameMethod.setAccessible(true);
            String name = (String) getNameMethod.invoke(beanObject);

            Toast.makeText(getApplicationContext(), name, Toast.LENGTH_LONG).show();

        } catch (Throwable e) {

        }
    }
}
