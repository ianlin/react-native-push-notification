package com.dieam.reactnativepushnotification.modules;

import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.os.PowerManager.WakeLock;

import java.util.List;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONObject;

import android.util.Log;

public class RNPushNotificationListenerService extends GcmListenerService {

    private static final String ReceiveNotificationExtra  = "receiveNotifExtra";
    private static Boolean autoRestartReactActivity = false;
    private PowerManager mPowerManager;
    private KeyguardManager mKeyguardManager;
    //private WakeLock mPokeFullLock = null;
    private WakeLock mPokePartialLock = null;

    private boolean checkIsInteractive() {
        if (android.os.Build.VERSION.SDK_INT >= 20) {
            return mPowerManager.isInteractive();
        } else {
            return mPowerManager.isScreenOn();
        }
    }

    private boolean checkScreenIsLocked() {
        boolean isScreenLocked = true;
        if (android.os.Build.VERSION.SDK_INT >= 22) {
            isScreenLocked = mKeyguardManager.isDeviceLocked();
        } else {
            isScreenLocked = mKeyguardManager.isKeyguardLocked();
        }
        return isScreenLocked | mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    @Override
    public void onMessageReceived(String from, Bundle bundle) {
        Log.d("RNPushNotification", "RNPushNotificationListenerService: onMessageReceived");
        if (bundle != null && bundle.containsKey("pushType") && bundle.getString("pushType").equals("NotifyIncomingCall")) {
            if (mPowerManager == null) {
                mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            }
            if (mKeyguardManager == null) {
                mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
            }
            bundle.putBoolean("isInteractive", checkIsInteractive());
            bundle.putBoolean("isScreenLocked", checkScreenIsLocked());
            //if (android.os.Build.MANUFACTURER.toLowerCase().equals("xiaomi") &&
            //        android.os.Build.BRAND.toLowerCase().equals("xiaomi")) {
            if (true) {
                if (mPokePartialLock == null) {
                    mPokePartialLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "RNPushNotification");
                    mPokePartialLock.setReferenceCounted(false);
                }
                synchronized (mPokePartialLock) {
                    if (!mPokePartialLock.isHeld()) {
                        mPokePartialLock.acquire(20000);
                        Log.d("RNPushNotification", "RNPushNotificationListenerService: acquirePokePartialWakeLockReleaseAfter(20000)");
                    }
                }
            }
        }

        JSONObject data = getPushData(bundle.getString("data"));
        if (data != null) {
            if (!bundle.containsKey("message")) {
                bundle.putString("message", data.optString("alert", "Notification received"));
            }
            if (!bundle.containsKey("title")) {
                bundle.putString("title", data.optString("title", null));
            }
        }

        sendNotification(bundle);
    }

    private JSONObject getPushData(String dataString) {
        try {
            return new JSONObject(dataString);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendNotification(Bundle bundle) {

        Boolean isRunning = isApplicationRunning();

        Intent intent = new Intent(this.getPackageName() + ".RNPushNotificationReceiveNotification");
        bundle.putBoolean("foreground", isRunning);
        bundle.putBoolean("userInteraction", false);
        intent.putExtra("notification", bundle);

        // --- custom
        autoRestartReactActivity = Boolean.parseBoolean(bundle.getString("autoRestartReactActivity"));

        sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle result = getResultExtras(true);
                String status = result.getString(ReceiveNotificationExtra, "fail");
                if (status.equals("fail")) {
                    if (autoRestartReactActivity) {
                        Log.d("RNPushNotification", "RNPushNotificationListenerService: BroadcastReceiver: restart react activity");
                        restartReactActivity(intent);
                    } else {
                        Log.d("RNPushNotification", "RNPushNotificationListenerService: BroadcastReceiver: send local notificaion");
                        sendLocalNotification(intent.getBundleExtra("notification"));
                    }
                }
            }
        }, null, Activity.RESULT_OK, null, null);

        if (false) {
            Log.d("RNPushNotification", "not running! send local notification");
            sendLocalNotification(bundle);
        }
    }

    private void sendLocalNotification(Bundle bundle) {
        new RNPushNotificationHelper(getApplication()).sendNotification(bundle);
    }

    private boolean isApplicationRunning() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.processName.equals(getApplication().getPackageName())) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String d : processInfo.pkgList) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void restartReactActivity(Intent receiveBroadcastIntent) {
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());

        intent.putExtra("wakeupNotification", receiveBroadcastIntent.getBundleExtra("notification"));
        intent.putExtra("moveTaskToBack", true);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        startActivity(intent);
    }
}
