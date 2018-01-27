package com.bukhmastov.cdoitmo.fragments.settings;

import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.objects.SettingsScheduleAttestations;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceBasic;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceList;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceSwitch;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsScheduleAttestationsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsSAFragment";
    public static final ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceBasic("pref_schedule_attestations_default", "{\"query\":\"auto\",\"title\":\"\"}", R.string.default_schedule, true, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                new SettingsScheduleAttestations(activity, preference, value -> Static.T.runThread(() -> {
                    Storage.pref.put(activity, "pref_schedule_attestations_default", value);
                    callback.onSetSummary(activity, value);
                })).show();
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
                try {
                    final JSONObject json = new JSONObject(value);
                    switch (json.getString("query")) {
                        case "mine": return activity.getString(R.string.personal_schedule);
                        case "auto": return activity.getString(R.string.current_group);
                        default: return json.getString("title");
                    }
                } catch (Exception e) {
                    Static.error(e);
                    return null;
                }
            }
        }));
        preferences.add(new PreferenceList("pref_schedule_attestations_term", "0", R.string.term_picker, R.array.pref_schedule_attestations_term_titles, R.array.pref_schedule_attestations_term_values, true));
        preferences.add(new PreferenceSwitch("pref_schedule_attestations_use_cache", false, R.string.cache_schedule, null, null));
        preferences.add(new PreferenceBasic("pref_schedule_attestations_clear_cache", null, R.string.clear_schedule_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Static.T.runThread(() -> {
                    Log.v(TAG, "pref_schedule_attestations_clear_cache clicked");
                    if (activity != null) {
                        boolean success = Storage.file.general.cache.clear(activity, "schedule_attestations");
                        Static.snackBar(activity, activity.getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                    }
                });
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
                return null;
            }
        }));
    }

    @Override
    protected List<Preference> getPreferences() {
        return preferences;
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    @Override
    protected Fragment getSelf() {
        return this;
    }
}