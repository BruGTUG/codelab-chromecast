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

import com.google.sample.castcompanionlibrary.cast.VideoCastManager;

import android.app.Application;
import android.content.Context;

/**
 * The {@link Application} for this demo application.
 */
public class VideoApplication extends Application {

    private static String APPLICATION_ID;
    private static VideoCastManager mCastMgr = null;

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        APPLICATION_ID = getString(R.string.app_id);
    }

    public static VideoCastManager getCastManager(Context context) {
        if (null == mCastMgr) {
            mCastMgr = VideoCastManager.initialize(context, APPLICATION_ID, null, null);
            mCastMgr.enableFeatures(VideoCastManager.FEATURE_DEBUGGING);
        }
        mCastMgr.setContext(context);
        return mCastMgr;
    }
}
