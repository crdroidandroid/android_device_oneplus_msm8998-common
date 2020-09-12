package org.ifaa.android.manager;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;

public abstract class IFAAManager {

    @UnsupportedAppUsage
    public abstract int getSupportBIOTypes(Context context);

    @UnsupportedAppUsage
    public abstract int startBIOManager(Context context, int authType);

    @UnsupportedAppUsage
    public native byte[] processCmd(Context context, byte[] param);

    @UnsupportedAppUsage
    public abstract String getDeviceModel();

    @UnsupportedAppUsage
    public abstract int getVersion();

    /**
     * load so to communicate from REE to TEE
     */
    static {
        System.loadLibrary("teeclientjni");//teeclientjni for TA test binary //ifaateeclient
    }
}
