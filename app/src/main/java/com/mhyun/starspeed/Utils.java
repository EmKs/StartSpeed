package com.mhyun.starspeed;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by 叫我阿喵 on 2017/11/10.
 */

public class Utils {

    private static final String Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/StarSpeed/";
    private static final String Txt = "StarSpeed.txt";

    public static void saveSettings(String setting) throws IOException {
        File path = new File(Path);
        if (!path.exists())
            Log.i(K.PackageName, Path + String.valueOf(path.mkdirs()));
        File txt = new File(Path + Txt);
        if (txt.exists())
            Log.i(K.PackageName, "Delete" + txt.delete());
        Log.i(K.PackageName, Txt + String.valueOf(txt.createNewFile()));
        FileOutputStream outputStream = new FileOutputStream(Path + Txt);
        outputStream.write(setting.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    private static String getTxt() throws IOException {
        File path = new File(Path);
        File txt = new File(Path + Txt);
        if (!path.exists() || !txt.exists())
            return null;
        byte[] bytes = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(Path + Txt);
        while (inputStream.read(bytes) != -1) {
            baos.write(bytes, 0, bytes.length);
        }
        inputStream.close();
        return new String(baos.toByteArray(), "UTF-8").trim();
    }

    public static JSONArray getSettings() throws IOException, JSONException {
        String txt = getTxt();
        if (txt == null)
            return null;
        return new JSONArray(txt);
    }
}
