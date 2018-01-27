package com.bukhmastov.cdoitmo.objects.schedule;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.parse.ScheduleAttestationsParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

import java.util.Calendar;

public class ScheduleAttestations extends Schedule {

    private static final String TAG = "ScheduleExams";
    public static final String TYPE = "attestations";

    public ScheduleAttestations(Handler handler) {
        super(handler);
    }

    @Override
    protected void searchGroup(Context context, String group, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        Log.v(TAG, "searchGroup | group=" + group + " | refreshRate=" + refreshRate + " | forceToCache=" + Log.lBool(forceToCache) + " | withUserChanges=" + Log.lBool(withUserChanges));
        Static.T.runThread(() -> searchByQuery(context, "group", group, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                final int term = getTerm(context);
                DeIfmoClient.get(context, "index.php?node=schedule&index=sched&semiId=" + String.valueOf(term) + "&group=" + Static.prettifyGroupNumber(group), null, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Static.T.runThread(new ScheduleAttestationsParse(response, term, json -> restResponseHandler.onSuccess(statusCode, headers, json, null)));
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        restResponseHandler.onFailure(statusCode, headers, state);
                    }
                    @Override
                    public void onProgress(int state) {
                        restResponseHandler.onProgress(state);
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        restResponseHandler.onNewRequest(request);
                    }
                });
            }
            @Override
            public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                Static.T.runThread(() -> {
                    try {
                        template.put("title", Static.prettifyGroupNumber(group));
                        template.put("schedule", data.getJSONArray("schedule"));
                        onFound(query, template, true, false);
                    } catch (Exception e) {
                        Static.error(e);
                        invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
                    }
                });
            }
            @Override
            public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                invokePending(group, withUserChanges, true, handler -> handler.onFailure(statusCode, headers, state));
            }
            @Override
            public void onWebRequestProgress(final int state) {
                invokePending(group, withUserChanges, false, handler -> handler.onProgress(state));
            }
            @Override
            public void onWebNewRequest(final Client.Request request) {
                invokePending(group, withUserChanges, false, handler -> handler.onNewRequest(request));
            }
            @Override
            public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                Static.T.runThread(() -> {
                    try {
                        if (context == null || query == null || data == null) {
                            Log.w(TAG, "onFound | some values are null | context=" + Log.lNull(context) + " | query=" + Log.lNull(query) + " | data=" + Log.lNull(data));
                            if (query == null) {
                                return;
                            }
                            invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
                            return;
                        }
                        if (putToCache) {
                            putCache(context, query, data.toString(), forceToCache);
                        }
                        invokePending(query, withUserChanges, true, handler -> handler.onSuccess(data, fromCache));
                    } catch (Exception e) {
                        Static.error(e);
                        invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
                    }
                });
            }
        }));
    }
    @Override
    protected void searchMine(Context context, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        Log.v(TAG, "searchMine | personal schedule is unavailable at schedule of attestations");
        invokePending("mine", withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected void searchRoom(Context context, String room, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        Log.v(TAG, "searchRoom | room schedule is unavailable at schedule of attestations");
        invokePending(room, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected void searchTeacher(Context context, String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        Log.v(TAG, "searchTeacher | teacher schedule is unavailable at schedule of attestations");
        invokePending(teacherId, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected boolean searchTeachersAvailable() {
        return false;
    }
    @Override
    protected String getType() {
        return TYPE;
    }
    @Override
    protected SOURCE getDefaultSource() {
        return SOURCE.IFMO;
    }

    public int getTerm(Context context) {
        int term;
        try {
            term = Integer.parseInt(Storage.pref.get(context, "pref_schedule_attestations_term", "0"));
            if (term == 0) {
                int month = Static.getCalendar().get(Calendar.MONTH);
                if (month >= Calendar.SEPTEMBER || month == Calendar.JANUARY) {
                    term = 1;
                } else {
                    term = 2;
                }
            } else if (term < 1) {
                term = 1;
            } else if (term > 2) {
                term = 2;
            }
        } catch (Exception e) {
            term = 1;
        }
        return term;
    }
}