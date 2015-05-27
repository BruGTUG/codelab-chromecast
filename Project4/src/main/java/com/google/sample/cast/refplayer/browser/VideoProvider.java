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

package com.google.sample.cast.refplayer.browser;

import com.google.android.gms.cast.MediaTrack;
import com.google.sample.cast.refplayer.utils.MediaItem;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VideoProvider {

    private static final String TAG = "VideoProvider";
    private static String TAG_MEDIA = "videos";
    private static String THUMB_PREFIX_URL =
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/";
    private static String TAG_CATEGORIES = "categories";
    private static String TAG_NAME = "name";
    private static String TAG_STUDIO = "studio";
    private static String TAG_SOURCES = "sources";
    private static String TAG_SUBTITLE = "subtitle";
    private static String TAG_TRACKS = "tracks";
    private static String TAG_TRACK_ID = "id";
    private static String TAG_TRACK_TYPE = "type";
    private static String TAG_TRACK_SUBTYPE = "subtype";
    private static String TAG_TRACK_CONTENT_ID = "contentId";
    private static String TAG_TRACK_NAME = "name";
    private static String TAG_TRACK_LANGUAGE = "language";
    private static String TAG_THUMB = "image-480x270"; // "thumb";
    private static String TAG_IMG_780_1200 = "image-780x1200";
    private static String TAG_TITLE = "title";

    private static List<MediaItem> mediaList;

    protected JSONObject parseUrl(String urlString) {
        InputStream is = null;
        try {
            java.net.URL url = new java.net.URL(urlString);
            URLConnection urlConnection = url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static List<MediaItem> buildMedia(String url) throws JSONException {

        if (null != mediaList) {
            return mediaList;
        }
        mediaList = new ArrayList<MediaItem>();
        JSONObject jsonObj = new VideoProvider().parseUrl(url);
        JSONArray categories = jsonObj.getJSONArray(TAG_CATEGORIES);
        if (null != categories) {
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                category.getString(TAG_NAME);
                JSONArray videos = category.getJSONArray(getJsonMediaTag());
                if (null != videos) {
                    for (int j = 0; j < videos.length(); j++) {
                        JSONObject video = videos.getJSONObject(j);
                        String subTitle = video.getString(TAG_SUBTITLE);
                        JSONArray videoUrls = video.getJSONArray(TAG_SOURCES);
                        if (null == videoUrls || videoUrls.length() == 0) {
                            continue;
                        }
                        String videoUrl = videoUrls.getString(0);
                        String imageurl = getThumbPrefix() + video.getString(TAG_THUMB);
                        String bigImageurl = getThumbPrefix() + video.getString(TAG_IMG_780_1200);
                        String title = video.getString(TAG_TITLE);
                        String studio = video.getString(TAG_STUDIO);

                        /* Adding tracks, if any */
                        ArrayList<MediaTrack> tracks = new ArrayList<MediaTrack>();
                        if (video.has(TAG_TRACKS)) {
                            JSONArray tracksArray = video.getJSONArray(TAG_TRACKS);
                            if (tracksArray != null) {
                                tracks = new ArrayList<MediaTrack>();
                                for (int k = 0; k < tracksArray.length(); k++) {
                                    JSONObject track = tracksArray.getJSONObject(k);
                                    tracks.add(buildTrack(track.getLong(TAG_TRACK_ID),
                                            track.getString(TAG_TRACK_TYPE),
                                            track.getString(TAG_TRACK_SUBTYPE),
                                            track.getString(TAG_TRACK_CONTENT_ID),
                                            track.getString(TAG_TRACK_NAME),
                                            track.getString(TAG_TRACK_LANGUAGE)
                                    ));
                                }
                            }
                        }

                        mediaList.add(buildMediaInfo(title, studio, subTitle, videoUrl, imageurl,
                                bigImageurl, tracks));
                    }
                }
            }
        }
        return mediaList;
    }

    private static MediaItem buildMediaInfo(String title, String subTitle, String studio,
            String url, String imgUrl, String bigImageUrl, ArrayList<MediaTrack> tracks) {
        MediaItem media = new MediaItem();
        media.setUrl(url);
        media.setTitle(title);
        media.setSubTitle(subTitle);
        media.setStudio(studio);
        media.addImage(imgUrl);
        media.addImage(bigImageUrl);
        media.setTracks(tracks);

        return media;
    }

    private static MediaTrack buildTrack(long id, String type, String subType, String contentId,
            String name, String language) {
        int trackType = MediaTrack.TYPE_UNKNOWN;
        if ("text".equals(type)) {
            trackType = MediaTrack.TYPE_TEXT;
        } else if ("video".equals(type)) {
            trackType = MediaTrack.TYPE_VIDEO;
        } else if ("audio".equals(type)) {
            trackType = MediaTrack.TYPE_AUDIO;
        }

        int trackSubType = MediaTrack.SUBTYPE_NONE;
        if (subType != null) {
            if ("captions".equals(type)) {
                trackSubType = MediaTrack.SUBTYPE_CAPTIONS;
            } else if ("subtitle".equals(type)) {
                trackSubType = MediaTrack.SUBTYPE_SUBTITLES;
            }
        }

        return new MediaTrack.Builder(id, trackType)
                .setName(name)
                .setSubtype(trackSubType)
                .setContentId(contentId)
                .setLanguage(language).build();
    }

    private static String getJsonMediaTag() {
        return TAG_MEDIA;
    }

    private static String getThumbPrefix() {
        return THUMB_PREFIX_URL;
    }
}
