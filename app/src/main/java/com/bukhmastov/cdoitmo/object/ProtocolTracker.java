package com.bukhmastov.cdoitmo.object;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.interfaces.Callable;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private final @NonNull Context context;
    private final @NonNull JobScheduler jobScheduler;
    private final int jobID = 0;

    public ProtocolTracker(@NonNull Context context) {
        final JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) {
            throw new RuntimeException("JobScheduler is null. Unable to use ProtocolTracker");
        }
        this.context = context;
        this.jobScheduler = js;
    }

    public ProtocolTracker check() {
        return check(null);
    }
    public ProtocolTracker check(@Nullable final Callable callback) {
        Log.v(TAG, "check");
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = "1".equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"));
        if (enabled && !running) {
            start(callback);
        } else if (!enabled && running) {
            stop(callback);
        } else if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (jobScheduler.getPendingJob(jobID) == null) throw new Exception("job is null");
                if (callback != null) {
                    callback.call();
                }
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
                restart(callback);
            }
        } else {
            if (callback != null) {
                callback.call();
            }
        }
        return this;
    }

    public ProtocolTracker restart() {
        return restart(null);
    }
    public ProtocolTracker restart(@Nullable final Callable callback) {
        Log.v(TAG, "restart");
        stop(() -> start(callback));
        return this;
    }

    private ProtocolTracker start() {
        return start(null);
    }
    private ProtocolTracker start(@Nullable final Callable callback) {
        Log.v(TAG, "start");
        if (App.UNAUTHORIZED_MODE) {
            Log.v(TAG, "start | UNAUTHORIZED_MODE");
            stop(callback);
            return this;
        }
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = "1".equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"));
        if (enabled && !running) {
            Log.v(TAG, "Starting");
            try {
                int frequency = Integer.parseInt(Storage.pref.get(context, "pref_notify_frequency", "30"));
                boolean network_unmetered = Storage.pref.get(context, "pref_notify_network_unmetered", false);
                JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(context, ProtocolTrackerJobService.class));
                builder.setPeriodic(frequency * 60000);
                builder.setPersisted(true);
                builder.setRequiredNetworkType(network_unmetered ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY);
                jobScheduler.schedule(builder.build());
                Storage.file.perm.put(context, "protocol_tracker#job_service_running", "1");
                Storage.file.perm.put(context, "protocol_tracker#protocol", "");
                Log.i(TAG, "Started | user = " + Storage.file.general.perm.get(context, "users#current_login") + " | frequency = " + frequency);
                if (callback != null) {
                    callback.call();
                }
            } catch (Exception e){
                Log.e(TAG, "Failed to schedule job");
                Log.exception(e);
                stop(callback);
            }
        } else {
            if (callback != null) {
                callback.call();
            }
        }
        return this;
    }

    public ProtocolTracker stop() {
        return stop(null);
    }
    public ProtocolTracker stop(@Nullable final Callable callback) {
        Log.v(TAG, "stop");
        boolean running = "1".equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"));
        if (running) {
            Log.v(TAG, "Stopping");
            jobScheduler.cancel(jobID);
            Storage.file.perm.put(context, "protocol_tracker#job_service_running", "0");
            Storage.file.perm.put(context, "protocol_tracker#protocol", "");
            Log.i(TAG, "Stopped");
        }
        if (callback != null) {
            callback.call();
        }
        return this;
    }

    public ProtocolTracker reset() {
        return reset(null);
    }
    public ProtocolTracker reset(@Nullable final Callable callback) {
        Log.v(TAG, "reset");
        jobScheduler.cancelAll();
        Storage.file.perm.put(context, "protocol_tracker#job_service_running", "0");
        Storage.file.perm.put(context, "protocol_tracker#protocol", "");
        check(callback);
        return this;
    }

    public static void setup(final Context context, final int attempt) {
        Thread.run(Thread.BACKGROUND, () -> {
            Log.v(TAG, "setup | attempt=" + attempt);
            if (!Storage.pref.get(context, "pref_protocol_changes_track", true)) {
                Log.v(TAG, "setup | pref_protocol_changes_track=false");
                return;
            }
            if (attempt < 3) {
                DeIfmoRestClient.get(context, "eregisterlog?days=126", null, new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, Client.Headers headers, JSONObject responseObj, final JSONArray responseArr) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            if (statusCode == 200 && responseArr != null) {
                                new ProtocolConverter(context, responseArr, 18, json -> Log.i(TAG, "setup | uploaded")).run();
                            } else {
                                setup(context, attempt + 1);
                            }
                        });
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        Thread.run(Thread.BACKGROUND, () -> setup(context, attempt + 1));
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {}
                });
            }
        });
    }
}