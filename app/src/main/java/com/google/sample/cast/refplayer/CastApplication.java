/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cast.refplayer;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.sample.cast.refplayer.settings.CastPreference;

/**
 * The {@link Application} for this demo application.
 */
public class CastApplication extends Application {
    private static final String PREFS_KEY_VOLUME_INCREMENT = "PREFS_KEY_VOLUMNE_INCREMENT";
    private static String APPLICATION_ID;
    private static VideoCastManager mCastMgr = null;
    public static final double VOLUME_INCREMENT = 0.05;

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        APPLICATION_ID = getString(R.string.app_id);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .edit()
                .putFloat(PREFS_KEY_VOLUME_INCREMENT, (float) VOLUME_INCREMENT)
        .apply();

    }

    public static VideoCastManager getCastManager(Context context) {
        if (null == mCastMgr) {
            mCastMgr = VideoCastManager.initialize(context, APPLICATION_ID, null, null);
            mCastMgr.enableFeatures(
                    VideoCastManager.FEATURE_NOTIFICATION |
                            VideoCastManager.FEATURE_LOCKSCREEN |
                            VideoCastManager.FEATURE_WIFI_RECONNECT |
                            VideoCastManager.FEATURE_CAPTIONS_PREFERENCE |
                            VideoCastManager.FEATURE_DEBUGGING);

        }
        String destroyOnExitStr = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(CastPreference.TERMINATION_POLICY_KEY, null);
        mCastMgr.setStopOnDisconnect(null != destroyOnExitStr
                && CastPreference.STOP_ON_DISCONNECT.equals(destroyOnExitStr));
        return mCastMgr;
    }

}
