package com.aurora.device.DeviceSettings;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.UserHandle;

import java.util.List;

public class ActionUtils {

    private static Intent getLaunchableIntent(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resInfo = pm.queryIntentActivities(intent, 0);
        if (resInfo.isEmpty()) {
            return null;
        }
        return pm.getLaunchIntentForPackage(resInfo.get(0).activityInfo.packageName);
    }

    private static Intent getBrowserIntent(Context context) {
        return getLaunchableIntent(context,
                new Intent(Intent.ACTION_VIEW, Uri.parse("http:")));
    }

    private static Intent getDialerIntent() {
        return new Intent(Intent.ACTION_DIAL, null);
    }

    private static Intent getEmailIntent(Context context) {
        return getLaunchableIntent(context,
                new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:")));
    }

    private static Intent getMessagesIntent(Context context) {
        return getLaunchableIntent(context,
                new Intent(Intent.ACTION_VIEW, Uri.parse("sms:")));
    }

    public static Intent getIntentByAction(Context context, int action){
        Intent intent = null;
        if (action == Constants.ACTION_BROWSER){
            intent = getBrowserIntent(context);
        }else if (action == Constants.ACTION_DIALER){
            intent = getDialerIntent();
        }else if (action == Constants.ACTION_EMAIL){
            intent = getEmailIntent(context);
        }else if (action == Constants.ACTION_MESSAGES){
            intent = getMessagesIntent(context);
        }
        return intent;
    }

    public static void triggerAction(Context context, int action) {
        Intent intent = getIntentByAction(context, action);
        if (intent == null){
            return;
        }
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.isKeyguardLocked()){
            intent = new Intent();
            intent.setClassName("com.android.touch.gestures", "com.android.touch.gestures.ScreenOffLaunchGestureActivity");
            intent.putExtra(ScreenOffLaunchGestureActivity.ACTION_KEY, action);
        }
        startActivitySafely(context, intent);
    }

    public static void startActivitySafely(Context context, Intent intent) {
        if (intent == null){
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            UserHandle user = new UserHandle(UserHandle.USER_CURRENT);
            context.startActivityAsUser(intent, null, user);
        } catch (Exception e) {
            // Ignore
        }
    }
}