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

package com.google.sample.cast.refplayer.settings;

import com.google.sample.cast.refplayer.R;
import com.google.sample.cast.refplayer.VideoApplication;
import com.google.sample.cast.refplayer.utils.Utils;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

public class CastPreference extends PreferenceActivity {

    private VideoCastManager mCastManager;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.application_preference);
        mCastManager = VideoApplication.getCastManager(this);

        EditTextPreference versionPref = (EditTextPreference) findPreference("app_version");
        versionPref.setTitle(getString(R.string.version, Utils.getAppVersionName(this),
                getString(R.string.ccl_version)));
    }

    @Override
    protected void onResume() {
        if (null != mCastManager) {
            mCastManager.incrementUiCounter();
            mCastManager.updateCaptionSummary("caption", getPreferenceScreen());
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (null != mCastManager) {
            mCastManager.decrementUiCounter();
        }
        super.onPause();
    }

}
