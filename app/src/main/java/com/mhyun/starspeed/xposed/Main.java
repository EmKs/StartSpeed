package com.mhyun.starspeed.xposed;


import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.mhyun.starspeed.K;
import com.mhyun.starspeed.R;
import com.mhyun.starspeed.Utils;
import com.mhyun.starspeed.databinding.DialogSeekBarBinding;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by MHyun on 2017/11/3.
 */

@SuppressWarnings({"FieldCanBeLocal", "ConstantConditions"})
public class Main implements IXposedHookLoadPackage {

    private int Item = 0;
    private long progress;
    private Context context;
    private Object mediaPlayer;
    private Dialog bottomDialog;
    private boolean isInit = false;
    private boolean SKIP = false;
    private boolean QUICKEN = false;
    private boolean PROGRESS = false;
    private long duration, currentPosition;
    private DialogSeekBarBinding binding;
    private int TOTAL = Integer.MAX_VALUE;
    private String OnItemSelectedListener = "com.chaoxing.fanya.aphone.ui.video.s";
    private String VideoPlayerActicity = "com.chaoxing.fanya.aphone.ui.video.VideoPlayerActicity";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals("com.chaoxing.mobile")) return;
        initSettings();
        SkipMode(loadPackageParam);
        QuickenMode(loadPackageParam);
        ProgressMode(loadPackageParam);
    }

    private void initSettings() {
        try {
            XposedBridge.log("找到超星学习通,初始化设置");
            JSONArray settings = Utils.getSettings();
            if (settings != null) {
                SKIP = settings.getJSONObject(0).getBoolean(K.SKIP_MODE);
                QUICKEN = settings.getJSONObject(1).getBoolean(K.QUICKEN_MODE);
                PROGRESS = settings.getJSONObject(2).getBoolean(K.PROGRESS_MODE);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            XposedBridge.log("找到超星学习通,初始化失败");
        }
    }

    private void SkipMode(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (SKIP) {
            XposedBridge.log("跳过模式开启,查找a方法");
            XposedHelpers.findAndHookMethod(VideoPlayerActicity, loadPackageParam.classLoader, "a", Integer.TYPE, Integer.TYPE, Integer.TYPE, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    methodHookParam.args[1] = TOTAL;
                    methodHookParam.args[0] = 4;
                }
            });
        }
    }

    private void QuickenMode(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (QUICKEN) {
            XposedBridge.log("加速模式开启，查找b方法");
            XposedHelpers.findAndHookMethod(VideoPlayerActicity, loadPackageParam.classLoader, "b", Integer.TYPE, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Field XSpinner = methodHookParam.thisObject.getClass().getDeclaredField("X"); //通过反射拿到X控件，即Spinner(倍速播放)
                    XSpinner.setAccessible(true);   //设置可以访问
                    ((Spinner) XSpinner.get(methodHookParam.thisObject)).setEnabled(true);
                    ((Spinner) XSpinner.get(methodHookParam.thisObject)).setSelection(Item);
                }
            });

            XposedHelpers.findAndHookMethod(OnItemSelectedListener, loadPackageParam.classLoader, "onItemSelected", AdapterView.class, View.class, Integer.TYPE, Long.TYPE, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Item = (int) methodHookParam.args[2];
                }
            });
        }
    }

    private void ProgressMode(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (PROGRESS) {
            XposedBridge.log("进度条模式开启，获取Context，查找onItemSelected方法");
            XposedHelpers.findAndHookMethod(VideoPlayerActicity, loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    context = (Context) methodHookParam.thisObject;
                }
            });

            XposedHelpers.findAndHookMethod(OnItemSelectedListener, loadPackageParam.classLoader, "onItemSelected", AdapterView.class, View.class, Integer.TYPE, Long.TYPE, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (bottomDialog != null && bottomDialog.isShowing())
                        return;
                    if (bottomDialog == null) {
                        Field a = methodHookParam.thisObject.getClass().getDeclaredField("a");
                        a.setAccessible(true);
                        Object object = a.get(methodHookParam.thisObject);
                        Object u = XposedHelpers.getObjectField(object, "u");
                        Method gMethod = u.getClass().getDeclaredMethod("g");
                        Class<?> returnType = gMethod.getReturnType();
                        mediaPlayer = gMethod.invoke(u);
                        Method getDuration = returnType.getMethod("getDuration");
                        Method seekTo = returnType.getMethod("seekTo", Long.TYPE);
                        duration = (long) getDuration.invoke(mediaPlayer);
                        initDialog(seekTo);
                    }
                }

                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (!isInit)
                        isInit = true;
                    else Item = (int) methodHookParam.args[2];
                }
            });

            XposedHelpers.findAndHookMethod(VideoPlayerActicity, loadPackageParam.classLoader, "b", Integer.TYPE, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Field XSpinner = methodHookParam.thisObject.getClass().getDeclaredField("X"); //通过反射拿到X控件，即Spinner(倍速播放)
                    XSpinner.setAccessible(true);   //设置可以访问
                    ((Spinner) XSpinner.get(methodHookParam.thisObject)).setEnabled(true);
                    ((Spinner) XSpinner.get(methodHookParam.thisObject)).setSelection(Item);
                    if (bottomDialog != null && bottomDialog.isShowing()) {
                        currentPosition = (int) methodHookParam.args[0];
                        binding.progress.setText(Format(currentPosition));
                        binding.seekBarDialog.setProgress((int) currentPosition);
                    }
                    if (!isInit)
                        ((Spinner) XSpinner.get(methodHookParam.thisObject)).setSelection(2);

                }
            });

            XposedHelpers.findAndHookMethod(VideoPlayerActicity, loadPackageParam.classLoader, "onDestroy", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Item = 0;
                    isInit = false;
                    if (bottomDialog != null) bottomDialog.dismiss();
                    bottomDialog = null;
                    XposedBridge.log(VideoPlayerActicity + "onDestroy");
                }
            });
        }
    }

    private String Format(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss", Locale.getDefault());
        return formatter.format(time);
    }

    private void initDialog(final Method seekTo) throws PackageManager.NameNotFoundException {
        bottomDialog = new Dialog(context, R.style.BottomDialog);
        View view = LayoutInflater.from(context.createPackageContext(K.PackageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY)).inflate(R.layout.dialog_seek_bar, null);
        binding = DataBindingUtil.bind(view);
        binding.total.setText(Format(duration));
        binding.seekBarDialog.setMax((int) duration);
        binding.total.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomDialog.dismiss();
                bottomDialog = null;
            }
        });
        binding.seekBarDialog.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    seekTo.invoke(mediaPlayer, progress);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    bottomDialog.dismiss();
                    e.printStackTrace();
                }
            }
        });
        bottomDialog.setContentView(view);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = context.getResources().getDisplayMetrics().widthPixels;
        params.height = (int) (context.getResources().getDisplayMetrics().heightPixels*0.15);
        view.setLayoutParams(params);
        bottomDialog.setCancelable(false);
        bottomDialog.getWindow().setBackgroundDrawable(new ColorDrawable());
        bottomDialog.show();
    }
}
