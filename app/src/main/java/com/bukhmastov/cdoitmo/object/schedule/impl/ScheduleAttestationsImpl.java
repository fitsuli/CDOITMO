package com.bukhmastov.cdoitmo.object.schedule.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.parse.schedule.ScheduleAttestationsParse;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.TextUtils;

import org.json.JSONObject;

import java.util.Calendar;

import javax.inject.Inject;

public class ScheduleAttestationsImpl extends ScheduleImpl implements ScheduleAttestations {

    private static final String TAG = "ScheduleExams";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    StoragePref storagePref;
    @Inject
    DeIfmoClient deIfmoClient;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;

    public ScheduleAttestationsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    protected void searchGroup(Context context, String group, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        log.v(TAG, "searchGroup | group=", group, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges);
        thread.run(() -> searchByQuery(context, "group", group, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                final int term = getTerm(context);
                deIfmoClient.get(context, "index.php?node=schedule&index=sched&semiId=" + String.valueOf(term) + "&group=" + textUtils.prettifyGroupNumber(group), null, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        thread.run(new ScheduleAttestationsParse(response, term, json -> restResponseHandler.onSuccess(statusCode, headers, json, null)));
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
                thread.run(() -> {
                    try {
                        template.put("title", textUtils.prettifyGroupNumber(group));
                        template.put("schedule", data.getJSONArray("schedule"));
                        onFound(query, template, true, false);
                    } catch (Exception e) {
                        log.exception(e);
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
                thread.run(() -> {
                    try {
                        if (context == null || query == null || data == null) {
                            log.w(TAG, "onFound | some values are null | context=", context, " | query=", query, " | data=", data);
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
                        log.exception(e);
                        invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
                    }
                });
            }
        }));
    }
    @Override
    protected void searchMine(Context context, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        log.v(TAG, "searchMine | personal schedule is unavailable at schedule of attestations");
        invokePending("mine", withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected void searchRoom(Context context, String room, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        log.v(TAG, "searchRoom | room schedule is unavailable at schedule of attestations");
        invokePending(room, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected void searchTeacher(Context context, String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        log.v(TAG, "searchTeacher | teacher schedule is unavailable at schedule of attestations");
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
    protected @Source String getDefaultSource() {
        return SOURCE.IFMO;
    }

    private int getTerm(Context context) {
        int term;
        try {
            term = Integer.parseInt(storagePref.get(context, "pref_schedule_attestations_term", "0"));
            if (term == 0) {
                int month = time.getCalendar().get(Calendar.MONTH);
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