package com.google.sample.cast.refplayer.utils;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;

import android.os.Bundle;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by anaddaf on 11/19/14.
 */
public class MediaItem {

    private static final String TAG = "MediaItem";
    private String mTitle;
    private String mSubTitle;
    private String mStudio;
    private String mUrl;
    private String mContentType;
    private ArrayList<String> mImageList = new ArrayList<String>();
    private ArrayList<MediaTrack> mTracks = new ArrayList<MediaTrack>();

    public static final String KEY_URL= "movie-urls";
    public static final String KEY_IMAGES = "images";
    public static final String KEY_CONTENT_TYPE = "content-type";
    private static final String KEY_TRACK_ID = "track-id";
    private static final String KEY_TRACK_CONTENT_ID = "track-custom-id";
    private static final String KEY_TRACK_NAME = "track-name";
    private static final String KEY_TRACK_TYPE = "track-type";
    private static final String KEY_TRACK_SUBTYPE = "track-subtype";
    private static final String KEY_TRACK_LANGUAGE = "track-language";
    private static final String KEY_TRACK_CUSTOM_DATA = "track-custom-data";
    private static final String KEY_TRACKS_DATA = "track-data";

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getTitle() {

        return mTitle;
    }

    public ArrayList<MediaTrack> getTracks() {
        return mTracks;
    }

    public void setTracks(ArrayList<MediaTrack> tracks) {
        mTracks = tracks;
    }

    public String getContentType() {
        return mContentType;
    }

    public void setContentType(String contentType) {
        mContentType = contentType;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSubTitle() {
        return mSubTitle;
    }

    public void setSubTitle(String subTitle) {
        mSubTitle = subTitle;
    }

    public String getStudio() {
        return mStudio;
    }

    public void setStudio(String studio) {
        mStudio = studio;
    }

    public void addImage(String url) {
        mImageList.add(url);
    }

    public void addImage(String url, int index) {
        if (index < mImageList.size()) {
            mImageList.set(index, url);
        }
    }

    public String getImage(int index) {
        if (index < mImageList.size()) {
            return mImageList.get(index);
        }
        return null;
    }

    public boolean hasImage() {
        return !mImageList.isEmpty();
    }

    public ArrayList<String> getImages() {
        return mImageList;
    }

    public Bundle toBundle() {
        Bundle wrapper = new Bundle();
        wrapper.putString(MediaMetadata.KEY_TITLE, mTitle);
        wrapper.putString(MediaMetadata.KEY_SUBTITLE, mSubTitle);
        wrapper.putString(KEY_URL, mUrl);
        wrapper.putString(MediaMetadata.KEY_STUDIO, mStudio);
        wrapper.putStringArrayList(KEY_IMAGES, mImageList);
        wrapper.putString(KEY_CONTENT_TYPE, "video/mp4");
        if (!mTracks.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray();
                for (MediaTrack mt : mTracks) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(KEY_TRACK_NAME, mt.getName());
                    jsonObject.put(KEY_TRACK_CONTENT_ID, mt.getContentId());
                    jsonObject.put(KEY_TRACK_ID, mt.getId());
                    jsonObject.put(KEY_TRACK_LANGUAGE, mt.getLanguage());
                    jsonObject.put(KEY_TRACK_TYPE, mt.getType());
                    jsonObject.put(KEY_TRACK_SUBTYPE, mt.getSubtype());
                    if (null != mt.getCustomData()) {
                        jsonObject.put(KEY_TRACK_CUSTOM_DATA, mt.getCustomData().toString());
                    }
                    jsonArray.put(jsonObject);
                }
                wrapper.putString(KEY_TRACKS_DATA, jsonArray.toString());
            } catch (JSONException e) {
                LOGE(TAG, "fromMediaInfo(): Failed to convert Tracks data to json", e);
            }
        }
        return wrapper;
    }

    public static final MediaItem fromBundle(Bundle wrapper) {
        if (null == wrapper) {
            return null;
        }
        MediaItem media = new MediaItem();
        media.setUrl(wrapper.getString(KEY_URL));
        media.setTitle(wrapper.getString(MediaMetadata.KEY_TITLE));
        media.setSubTitle(wrapper.getString(MediaMetadata.KEY_SUBTITLE));
        media.setStudio(wrapper.getString(MediaMetadata.KEY_STUDIO));
        media.mImageList.addAll(wrapper.getStringArrayList(KEY_IMAGES));
        media.setContentType(wrapper.getString(KEY_CONTENT_TYPE));

        /* Adding Closed Caption information */
        ArrayList<MediaTrack> mediaTracks = null;
        if (wrapper.getString(KEY_TRACKS_DATA) != null) {
            try {
                JSONArray jsonArray = new JSONArray(wrapper.getString(KEY_TRACKS_DATA));
                mediaTracks = new ArrayList<MediaTrack>();
                if (jsonArray != null && jsonArray.length() > 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObj = (JSONObject) jsonArray.get(i);
                        MediaTrack.Builder builder = new MediaTrack.Builder(
                                jsonObj.getLong(KEY_TRACK_ID), jsonObj.getInt(KEY_TRACK_TYPE));
                        if (jsonObj.has(KEY_TRACK_NAME)) {
                            builder.setName(jsonObj.getString(KEY_TRACK_NAME));
                        }
                        if (jsonObj.has(KEY_TRACK_SUBTYPE)) {
                            builder.setSubtype(jsonObj.getInt(KEY_TRACK_SUBTYPE));
                        }
                        if (jsonObj.has(KEY_TRACK_CONTENT_ID)) {
                            builder.setContentId(jsonObj.getString(KEY_TRACK_CONTENT_ID));
                        }
                        if (jsonObj.has(KEY_TRACK_LANGUAGE)) {
                            builder.setLanguage(jsonObj.getString(KEY_TRACK_LANGUAGE));
                        }
                        if (jsonObj.has(KEY_TRACKS_DATA)) {
                            builder.setCustomData(
                                    new JSONObject(jsonObj.getString(KEY_TRACKS_DATA)));
                        }
                        mediaTracks.add(builder.build());
                    }
                }
            } catch (JSONException e) {
                LOGE(TAG, "Failed to build media tracks from the wrapper bundle", e);
            }
            media.setTracks(mediaTracks);
        }
        return media;
    }


}
