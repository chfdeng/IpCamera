package org.zero.ipcamera;

import android.app.Application;
import android.content.Context;

/**
 * Created by cfd on 2019/7/31.
 */
public class BaseApplication extends Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return mContext;
    }
}
