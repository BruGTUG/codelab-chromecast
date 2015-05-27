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

import com.google.sample.cast.refplayer.settings.CastPreference;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class VideoBrowserActivity extends ActionBarActivity {

    private static final String TAG = "VideoBrowserActivity";
    private Toolbar mToolbar;
    private VideoCastManager mCastManager;
    private MenuItem mMediaRouteMenuItem;

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_browser);
        mCastManager = VideoApplication.getCastManager(this);
        setupActionBar();
    }

    private void setupActionBar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setLogo(R.drawable.actionbar_logo_castvideos);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        mMediaRouteMenuItem = mCastManager.
                addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(VideoBrowserActivity.this, CastPreference.class);
                startActivity(i);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() was called");
        super.onResume();
        mCastManager = VideoApplication.getCastManager(this);
        if (null != mCastManager) {
            mCastManager.incrementUiCounter();
        }
    }

    @Override
    protected void onPause() {
        mCastManager.decrementUiCounter();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
