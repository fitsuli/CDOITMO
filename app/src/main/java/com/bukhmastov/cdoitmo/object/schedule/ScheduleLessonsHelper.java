package com.bukhmastov.cdoitmo.object.schedule;

import android.content.Context;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONException;
import org.json.JSONObject;

public interface ScheduleLessonsHelper {

    boolean clearChanges(final Context context, final Storage storage, final String query, final Callable callback);

    boolean reduceLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback);

    boolean restoreLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback);

    boolean createLesson(final ConnectedActivity activity, final String query, final String title, final String type, final int weekday, final JSONObject lesson, final Callable callback);

    boolean createLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback);

    boolean deleteLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback);

    boolean editLesson(final ConnectedActivity activity, final String query, final String title, final String type, final int weekday, final JSONObject lesson, final Callable callback);

    // Returns the hash of the lesson
    String getLessonHash(JSONObject lesson) throws JSONException;

    // Returns the signature of the lesson
    String getLessonSignature(JSONObject lesson) throws JSONException;
}
