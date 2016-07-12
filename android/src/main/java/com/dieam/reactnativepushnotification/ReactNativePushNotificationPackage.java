package com.dieam.reactnativepushnotification;

import android.app.Activity;
import android.content.Intent;

import com.dieam.reactnativepushnotification.modules.RNPushNotification;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReactNativePushNotificationPackage implements ReactPackage {
    Activity mActivity;
    private Map<String, Object> mConstants;
    RNPushNotification mRNPushNotification;

    public ReactNativePushNotificationPackage(Activity activity, Map<String, Object> constants) {
        mActivity = activity;
        mConstants = constants;
    }

    @Override
    public List<NativeModule> createNativeModules(
            ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();

        mRNPushNotification = new RNPushNotification(reactContext, mActivity, mConstants);

        modules.add(mRNPushNotification);
        return modules;
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Arrays.asList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return new ArrayList<>();
    }

    public void newIntent(Intent intent) {
        if(mRNPushNotification == null){ return; }
        mRNPushNotification.newIntent(intent);
    }
}


