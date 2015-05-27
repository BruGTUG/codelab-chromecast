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

package com.google.sample.cast.refplayer.mediaplayer;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.sample.cast.refplayer.R;
import com.google.sample.cast.refplayer.VideoApplication;
import com.google.sample.cast.refplayer.settings.CastPreference;
import com.google.sample.cast.refplayer.utils.MediaItem;
import com.google.sample.cast.refplayer.utils.Utils;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.androidquery.AQuery;

import java.util.Timer;
import java.util.TimerTask;

public class LocalPlayerActivity extends ActionBarActivity {

    private static final String TAG = "LocalPlayerActivity";
    private VideoView mVideoView;
    private TextView mTitleView;
    private TextView mDescriptionView;
    private TextView mStartText;
    private TextView mEndText;
    private SeekBar mSeekbar;
    private ImageView mPlayPause;
    private ProgressBar mLoading;
    private View mControllers;
    private View mContainer;
    private ImageView mCoverArt;
    private Timer mSeekbarTimer;
    private Timer mControllersTimer;
    private PlaybackLocation mLocation;
    private PlaybackState mPlaybackState;
    private final Handler mHandler = new Handler();
    private Point mDisplaySize;
    private final float mAspectRatio = 72f / 128;
    private AQuery mAquery;
    private boolean mControllersVisible;
    private int mDuration;
    private TextView mAuthorView;
    private MediaItem mSelectedMedia;
    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mCastConsumer;

    /*
     * indicates whether we are doing a local or a remote playback
     */
    public static enum PlaybackLocation {
        LOCAL,
        REMOTE;
    }

    /*
     * List of various states that we can be in
     */
    public static enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        mAquery = new AQuery(this);
        loadViews();
        mCastManager = VideoApplication.getCastManager(this);
        setupActionBar();
        setupControlsCallbacks();
        setupCastListener();
        // see what we need to play and were
        Bundle b = getIntent().getExtras();
        if (null != b) {
            mSelectedMedia = MediaItem.fromBundle(getIntent().getBundleExtra("media"));
            mVideoView.setVideoURI(Uri.parse(mSelectedMedia.getUrl()));
            Log.d(TAG, "Setting url of the VideoView to: " + mSelectedMedia.getUrl());
            updateAlbumArt(PlaybackLocation.LOCAL);
            mPlaybackState = PlaybackState.PAUSED;
            updatePlayButton(mPlaybackState);

            if (mCastManager.isConnected()) {
                updateAlbumArt(PlaybackLocation.REMOTE);
            } else {
                updateAlbumArt(PlaybackLocation.LOCAL);
            }
        }
        if (null != mTitleView) {
            updateMetadata(true);
        }
    }

    private void setupCastListener() {
        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata,
                    String sessionId, boolean wasLaunched) {
                Log.d(TAG, "onApplicationLaunched() is reached");
                if (null != mSelectedMedia) {

                    if (mPlaybackState == PlaybackState.PLAYING) {
                        mVideoView.pause();
                        try {
                            loadRemoteMedia(mSeekbar.getProgress(), true);
                            finish();
                        } catch (Exception e) {
                            Utils.handleException(LocalPlayerActivity.this, e);
                        }
                    } else {
                        updateAlbumArt(PlaybackLocation.REMOTE);
                    }
                }
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {
                Log.d(TAG, "onApplicationDisconnected() is reached with errorCode: " + errorCode);
                updateAlbumArt(PlaybackLocation.LOCAL);
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "onDisconnected() is reached");
                mPlaybackState = PlaybackState.PAUSED;
                mLocation = PlaybackLocation.LOCAL;
            }

            @Override
            public void onConnectionSuspended(int cause) {
                Utils.showToast(LocalPlayerActivity.this,
                        R.string.connection_temp_lost);
            }

            @Override
            public void onConnectivityRecovered() {
                Utils.showToast(LocalPlayerActivity.this,
                        R.string.connection_recovered);
            }

        };
    }

    private void updateAlbumArt(PlaybackLocation location) {
        this.mLocation = location;
        if (location == PlaybackLocation.LOCAL) {
            if (mPlaybackState == PlaybackState.PLAYING ||
                    mPlaybackState == PlaybackState.BUFFERING) {
                setCoverArtStatus(null);
                startControllersTimer();
            } else {
                stopControllersTimer();
                setCoverArtStatus(mSelectedMedia.getImage(0));
            }

            getSupportActionBar().setTitle("");
        } else {
            stopControllersTimer();
            setCoverArtStatus(mSelectedMedia.getImage(0));
            updateControllersVisibility(true);
        }
    }

    private void play(int position) {
        startControllersTimer();
        switch (mLocation) {
            case LOCAL:
                mVideoView.seekTo(position);
                mVideoView.start();
                break;
            case REMOTE:
                mPlaybackState = PlaybackState.BUFFERING;
                updatePlayButton(mPlaybackState);
                try {
                    mCastManager.play(position);
                } catch (Exception e) {
                    Utils.handleException(this, e);
                }
                break;
            default:
                break;
        }
        restartTrickplayTimer();
    }

    private void togglePlayback() {
        stopControllersTimer();
        switch (mPlaybackState) {
            case PAUSED:
                switch (mLocation) {
                    case LOCAL:
                        mVideoView.start();
                        if (!mCastManager.isConnecting() ) {
                            Log.d(TAG, "Playing locally...");
                            mCastManager.clearPersistedConnectionInfo(
                                    VideoCastManager.CLEAR_SESSION);
                        }
                        mPlaybackState = PlaybackState.PLAYING;
                        startControllersTimer();
                        restartTrickplayTimer();
                        updateAlbumArt(PlaybackLocation.LOCAL);
                        break;
                    case REMOTE:
                        try {
                            mCastManager.checkConnectivity();
                            Log.d(TAG, "Playing remotely...");
                            loadRemoteMedia(0, true);
                            finish();
                        } catch (Exception e) {
                            Utils.handleException(LocalPlayerActivity.this, e);
                            return;
                        }
                        break;
                    default:
                        break;
                }
                break;

            case PLAYING:
                mPlaybackState = PlaybackState.PAUSED;
                mVideoView.pause();
                break;

            case IDLE:
                mVideoView.setVideoURI(Uri.parse(mSelectedMedia.getUrl()));
                mVideoView.seekTo(0);
                mVideoView.start();
                mPlaybackState = PlaybackState.PLAYING;
                restartTrickplayTimer();
                break;

            default:
                break;
        }
        updatePlayButton(mPlaybackState);
    }

    private void loadRemoteMedia(int position, boolean autoPlay) {
        mCastManager
                .startCastControllerActivity(this, mSelectedMedia.toBundle(), position, autoPlay);
    }

    private void setCoverArtStatus(String url) {
        if (null != url) {
            mAquery.id(mCoverArt).image(url);
            mCoverArt.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.INVISIBLE);
        } else {
            mCoverArt.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);
        }
    }

    private void stopTrickplayTimer() {
        Log.d(TAG, "Stopped TrickPlay Timer");
        if (null != mSeekbarTimer) {
            mSeekbarTimer.cancel();
        }
    }

    private void restartTrickplayTimer() {
        stopTrickplayTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        Log.d(TAG, "Restarted TrickPlay Timer");
    }

    private void stopControllersTimer() {
        if (null != mControllersTimer) {
            mControllersTimer.cancel();
        }
    }

    private void startControllersTimer() {
        if (null != mControllersTimer) {
            mControllersTimer.cancel();
        }
        if (mLocation == PlaybackLocation.REMOTE) {
            return;
        }
        mControllersTimer = new Timer();
        mControllersTimer.schedule(new HideControllersTask(), 5000);
    }

    // should be called from the main thread
    private void updateControllersVisibility(boolean show) {
        if (show) {
            getSupportActionBar().show();
            mControllers.setVisibility(View.VISIBLE);
        } else {
            getSupportActionBar().hide();
            mControllers.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() was called");

        if (null != mSeekbarTimer) {
            mSeekbarTimer.cancel();
            mSeekbarTimer = null;
        }
        if (null != mControllersTimer) {
            mControllersTimer.cancel();
        }
        // since we are playing locally, we need to stop the playback of
        // video (if user is not watching, pause it!)
        mVideoView.pause();
        mPlaybackState = PlaybackState.PAUSED;
        updatePlayButton(PlaybackState.PAUSED);
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        mCastManager.decrementUiCounter();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() was called");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() is called");
        stopControllersTimer();
        stopTrickplayTimer();
        mCastManager.clearContext(this);
        mCastConsumer = null;
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart was called");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() was called");
        super.onResume();
        mCastManager = VideoApplication.getCastManager(this);
        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.incrementUiCounter();
    }

    private class HideControllersTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateControllersVisibility(false);
                    mControllersVisible = false;
                }
            });

        }
    }

    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    int currentPos = 0;
                    if (mLocation == PlaybackLocation.LOCAL) {
                        currentPos = mVideoView.getCurrentPosition();
                        updateSeekbar(currentPos, mDuration);
                    }
                }
            });
        }
    }

    private void setupControlsCallbacks() {
        mVideoView.setOnErrorListener(new OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "OnErrorListener.onError(): VideoView encountered an " +
                        "error, what: " + what + ", extra: " + extra);
                String msg = "";
                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                    msg = getString(R.string.video_error_media_load_timeout);
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    msg = getString(R.string.video_error_server_unaccessible);
                } else {
                    msg = getString(R.string.video_error_unknown_error);
                }
                Utils.showErrorDialog(LocalPlayerActivity.this, msg);
                mVideoView.stopPlayback();
                mPlaybackState = PlaybackState.IDLE;
                updatePlayButton(mPlaybackState);
                return true;
            }
        });

        mVideoView.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, "onPrepared is reached");
                mDuration = mp.getDuration();
                mEndText.setText(Utils
                        .formatMillis(mDuration));
                mSeekbar.setMax(mDuration);
                restartTrickplayTimer();
            }
        });

        mVideoView.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                stopTrickplayTimer();
                mPlaybackState = PlaybackState.IDLE;
                updatePlayButton(PlaybackState.IDLE);
            }
        });

        mVideoView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mControllersVisible) {
                    updateControllersVisibility(true);
                }
                startControllersTimer();
                return false;
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    play(seekBar.getProgress());
                } else if (mPlaybackState != PlaybackState.IDLE) {
                    mVideoView.seekTo(seekBar.getProgress());
                }
                startControllersTimer();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopTrickplayTimer();
                mVideoView.pause();
                stopControllersTimer();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                mStartText.setText(Utils
                        .formatMillis(progress));
            }
        });

        mPlayPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                togglePlayback();
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    private void updateSeekbar(int position, int duration) {
        mSeekbar.setProgress(position);
        mSeekbar.setMax(duration);
        mStartText.setText(Utils
                .formatMillis(position));
        mEndText.setText(Utils.formatMillis(duration));
    }

    private void updatePlayButton(PlaybackState state) {
        switch (state) {
            case PLAYING:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_pause_dark));
                break;
            case PAUSED:
            case IDLE:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_play_dark));
                break;
            case BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
            updateMetadata(false);
            mContainer.setBackgroundColor(getResources().getColor(R.color.black));

        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
            updateMetadata(true);
            mContainer.setBackgroundColor(getResources().getColor(R.color.white));

        }
    }

    private void updateMetadata(boolean visible) {
        if (!visible) {
            mDescriptionView.setVisibility(View.GONE);
            mTitleView.setVisibility(View.GONE);
            mAuthorView.setVisibility(View.GONE);
            mDisplaySize = Utils.getDisplaySize(this);
            RelativeLayout.LayoutParams lp = new
                    RelativeLayout.LayoutParams(mDisplaySize.x,
                    mDisplaySize.y + getSupportActionBar().getHeight());
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            mVideoView.setLayoutParams(lp);
            mVideoView.invalidate();
        } else {
            mDescriptionView.setText(mSelectedMedia.getStudio());
            mTitleView.setText(mSelectedMedia.getTitle());
            mAuthorView.setText(mSelectedMedia.getSubTitle());
            mDescriptionView.setVisibility(View.VISIBLE);
            mTitleView.setVisibility(View.VISIBLE);
            mAuthorView.setVisibility(View.VISIBLE);
            mDisplaySize = Utils.getDisplaySize(this);
            RelativeLayout.LayoutParams lp = new
                    RelativeLayout.LayoutParams(mDisplaySize.x,
                    (int) (mDisplaySize.x * mAspectRatio));
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mVideoView.setLayoutParams(lp);
            mVideoView.invalidate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(LocalPlayerActivity.this, CastPreference.class);
                startActivity(i);
                break;

        }
        return true;
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
    }

    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.videoView1);
        mTitleView = (TextView) findViewById(R.id.textView1);
        mDescriptionView = (TextView) findViewById(R.id.textView2);
        mDescriptionView.setMovementMethod(new ScrollingMovementMethod());
        mAuthorView = (TextView) findViewById(R.id.textView3);
        mStartText = (TextView) findViewById(R.id.startText);
        mEndText = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        mPlayPause = (ImageView) findViewById(R.id.imageView2);
        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        mControllers = findViewById(R.id.controllers);
        mContainer = findViewById(R.id.container);
        mCoverArt = (ImageView) findViewById(R.id.coverArtView);
    }
}
