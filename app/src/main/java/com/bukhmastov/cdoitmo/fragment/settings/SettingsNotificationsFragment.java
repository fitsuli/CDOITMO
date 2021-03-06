package com.bukhmastov.cdoitmo.fragment.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceBasic;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.StoragePref;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;

public class SettingsNotificationsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsNotificationsFragment";
    private static final int TONE_PICKER = 5523;
    private static class RingtoneHelper {
        private static String preference_key = null;
        private static PreferenceBasic.OnPreferenceClickedCallback callback = null;
    }
    public static final List<Preference> preferences;
    static {
        preferences = new LinkedList<>();
        preferences.add(new PreferenceSwitch(
                "pref_use_notifications",
                true,
                R.string.pref_use_notifications,
                R.string.pref_use_notifications_summary,
                new LinkedList<>((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ?
                        Arrays.asList("pref_notify_type", "pref_notify_frequency", "pref_notify_network_unmetered", "pref_notify_sound", "pref_notify_vibrate", "pref_open_system_notifications_settings") :
                        Arrays.asList("pref_notify_type", "pref_notify_frequency", "pref_notify_network_unmetered", "pref_open_system_notifications_settings")
                )),
                null
        ));
        preferences.add(new PreferenceList("pref_notify_type", "1", R.string.pref_notify_type, 0, R.array.pref_notify_type_titles, R.array.pref_notify_type_desc, R.array.pref_notify_type_values, true));
        preferences.add(new PreferenceList("pref_notify_frequency", "30", R.string.pref_notify_frequency, R.array.pref_notifications_interval_titles, R.array.pref_notifications_interval_values, true));
        preferences.add(new PreferenceSwitch("pref_notify_network_unmetered", false, R.string.pref_notify_network_unmetered, R.string.pref_notify_network_unmetered_summary, null, null));
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            preferences.add(new PreferenceBasic("pref_notify_sound", "content://settings/system/notification_sound", R.string.pref_notify_sound, true, new PreferenceBasic.Callback() {
                @Override
                public void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, PreferenceBasic.OnPreferenceClickedCallback callback) {
                    final String value = injectProvider.getStoragePref().get(activity, preference.key, (String) preference.defaultValue).trim();
                    final Uri currentTone = value.isEmpty() ? null : Uri.parse(value);
                    final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, activity.getString(R.string.notification_sound));
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    intent.putExtra("com.bukhmastov.cdoitmo.PREFERENCE_KEY", preference.key);
                    final FragmentManager fragmentManager = activity.getSupportFragmentManager();
                    if (fragmentManager != null) {
                        final List<Fragment> fragments = fragmentManager.getFragments();
                        for (Fragment fragment : fragments) {
                            if ("SettingsNotificationsFragment".equals(fragment.getClass().getSimpleName())) {
                                RingtoneHelper.preference_key = preference.key;
                                RingtoneHelper.callback = callback;
                                fragment.startActivityForResult(intent, TONE_PICKER);
                                break;
                            }
                        }
                    }
                }
                @Override
                public String onGetSummary(Context context, String value) {
                    try {
                        if (TextUtils.isEmpty(value)) {
                            return context.getString(R.string.pref_ringtone_silent);
                        } else {
                            Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(value));
                            if (ringtone == null) {
                                return null;
                            } else {
                                return ringtone.getTitle(context);
                            }
                        }
                    } catch (Exception e) {
                        return null;
                    }
                }
            }));
            preferences.add(new PreferenceSwitch("pref_notify_vibrate", false, R.string.pref_notify_vibrate, null, null));
        }
        preferences.add(new PreferenceBasic("pref_open_system_notifications_settings", null, R.string.pref_open_system_notifications_settings, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, PreferenceBasic.OnPreferenceClickedCallback callback) {
                try {
                    Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
                    intent.putExtra("android.provider.extra.APP_PACKAGE", activity.getPackageName());
                    intent.putExtra("app_package", activity.getPackageName());
                    intent.putExtra("app_uid", activity.getApplicationInfo().uid);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    injectProvider.getEventBus().fire(new OpenIntentEvent(intent));
                } catch (Exception e) {
                    injectProvider.getNotificationMessage().snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
            @Override
            public String onGetSummary(Context context, String value) {
                return null;
            }
        }));
    }

    @Inject
    Lazy<StoragePref> storagePref;

    @Override
    public void onAttach(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.onAttach(context);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case TONE_PICKER: {
                    if (RingtoneHelper.preference_key != null && activity() != null) {
                        Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        String value = ringtone == null ? "" : ringtone.toString();
                        storagePref.get().put(activity(), RingtoneHelper.preference_key, value);
                        RingtoneHelper.callback.onSetSummary(activity(), value);
                        RingtoneHelper.preference_key = null;
                        RingtoneHelper.callback = null;
                    }
                    break;
                }
            }
        }
    }
}
