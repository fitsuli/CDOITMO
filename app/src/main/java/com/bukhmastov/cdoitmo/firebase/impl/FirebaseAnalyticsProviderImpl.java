package com.bukhmastov.cdoitmo.firebase.impl;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.Lazy;

public class FirebaseAnalyticsProviderImpl implements FirebaseAnalyticsProvider {

    private static final String TAG = "FirebaseAnalyticsProvider";
    private boolean enabled = true;
    private FirebaseAnalytics firebaseAnalytics = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Lazy<StoragePref> storagePref;
    @Inject
    Lazy<Static> staticUtil;

    public FirebaseAnalyticsProviderImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    private FirebaseAnalytics getFirebaseAnalytics(Context context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }
        return firebaseAnalytics;
    }

    @Override
    public boolean setEnabled(Context context) {
        return setEnabled(context, storagePref.get().get(context, "pref_allow_collect_analytics", true));
    }

    @Override
    public boolean setEnabled(Context context, boolean enabled) {
        return setEnabled(context, enabled, false);
    }

    @Override
    public boolean setEnabled(Context context, boolean enabled, boolean notify) {
        try {
            if (!enabled && notify) {
                logBasicEvent(context, "firebase_analytics_disabled");
            }
            this.enabled = enabled;
            FirebaseAnalytics firebaseAnalytics = getFirebaseAnalytics(context);
            firebaseAnalytics.setAnalyticsCollectionEnabled(this.enabled);
            firebaseAnalytics.setUserId(staticUtil.get().getUUID(context));
            log.i(TAG, "Firebase Analytics ", (this.enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            log.exception(e);
        }
        return this.enabled;
    }

    @Override
    public void setCurrentScreen(Activity activity) {
        setCurrentScreen(activity, null, null);
    }

    @Override
    public void setCurrentScreen(Activity activity, String screenOverride) {
        logCurrentScreen(activity, null, screenOverride);
    }

    @Override
    public void setCurrentScreen(Activity activity, Fragment fragment) {
        logCurrentScreen(activity, fragment, null);
    }

    @Override
    public void setCurrentScreen(Activity activity, Fragment fragment, String viewScreen) {
        if (!enabled || activity == null) return;
        thread.standalone(() -> {
            String vs = viewScreen;
            if (viewScreen == null) {
                if (fragment != null) {
                    vs = fragment.getClass().getSimpleName();
                } else {
                    vs = activity.getClass().getSimpleName();
                }
            }
            getFirebaseAnalytics(activity).setCurrentScreen(activity, vs, null);
        }, throwable -> {
            log.exception(throwable);
        });
    }

    @Override
    public void logCurrentScreen(Activity activity) {
        logCurrentScreen(activity, null, null);
    }

    @Override
    public void logCurrentScreen(Activity activity, String screenOverride) {
        logCurrentScreen(activity, null, screenOverride);
    }

    @Override
    public void logCurrentScreen(Activity activity, Fragment fragment) {
        logCurrentScreen(activity, fragment, null);
    }

    @Override
    public void logCurrentScreen(Activity activity, Fragment fragment, String viewScreen) {
        try {
            if (!enabled || activity == null) return;
            if (viewScreen == null) {
                if (fragment != null) {
                    viewScreen = fragment.getClass().getSimpleName();
                } else {
                    viewScreen = activity.getClass().getSimpleName();
                }
            }
            logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.APP_VIEW,
                    getBundle(FirebaseAnalyticsProvider.Param.APP_VIEW_SCREEN, viewScreen)
            );
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public void setUserProperties(Context context, String group) {
        try {
            group = group.trim();
            if (group.isEmpty()) return;
            Matcher m = Pattern.compile("(\\w)(\\d)(\\d)(\\d)(\\d)(\\w?)").matcher(group);
            if (m.find()) {
                String faculty = m.group(1).trim().toUpperCase();
                String level = m.group(2).trim();
                String course = m.group(3).trim();
                switch (faculty) {
                    case "B": faculty = "B - Лазерной и световой инженерии"; break;
                    case "C": faculty = "C - Дизайна и урбанистики"; break;
                    case "D": faculty = "D - ИМРиП"; break;
                    case "F": faculty = "F - Трансляционной медицины"; break;
                    case "K": faculty = "K - Инфокоммуникационных технологий"; break;
                    case "M": faculty = "M - ИТиП"; break;
                    case "N": faculty = "N - 'ИБиКТ'"; break;
                    case "O": faculty = "O - 'ИМБиП'"; break;
                    case "P": faculty = "P - КТиУ"; break;
                    case "S": faculty = "S - 'Академия ЛИМТУ'"; break;
                    case "T": faculty = "T - ФПБИ"; break;
                    case "U": faculty = "U - ФТМИ"; break;
                    case "V": faculty = "V - Фотоники и оптоинформатики"; break;
                    case "W": faculty = "W - ФХКТК"; break;
                    case "X": faculty = "X - Заочный"; break;
                    case "Y": faculty = "Y - Среднего проф. образования"; break;
                    case "Z": faculty = "Z - Физико-технический факультет"; break;
                }
                switch (level) {
                    case "0": level = "0 - Подг. отделение для ин. граждан"; break;
                    case "2": level = "2 - Среднее проф. образование"; break;
                    case "3": level = "3 - Бакалавриат"; break;
                    case "4": level = "4 - Магистратура"; break;
                    case "5": level = "5 - Специалитет"; break;
                    case "6": level = "6 - Аспирантура"; break;
                    case "9": level = "9 - Дополнительное образование"; break;
                }
                course = course + " " + "курс";
                setUserProperty(context, Property.FACULTY, faculty);
                setUserProperty(context, Property.LEVEL, level);
                setUserProperty(context, Property.COURSE, course);
            }
            setUserProperty(context, Property.GROUP, group);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public void setUserProperty(Context context, String property, String value) {
        if (!enabled) return;
        thread.standalone(() -> {
            String v = correctStringLength(value, 36);
            getFirebaseAnalytics(context).setUserProperty(property, v);
        }, throwable -> {
            log.exception(throwable);
        });
    }

    @Override
    public void logEvent(Context context, String name) {
        logEvent(context, name, null);
    }

    @Override
    public void logEvent(Context context, String name, Bundle params) {
        if (!enabled) return;
        thread.standalone(() -> {
            String n = correctStringLength(name, 40);
            if (params != null) {
                for (String key : params.keySet()) {
                    String value = params.getString(key);
                    value = correctStringLength(value, 100);
                    params.putString(key, value);
                }
            }
            getFirebaseAnalytics(context).logEvent(n, params);
        }, throwable -> {
            log.exception(throwable);
        });
    }

    @Override
    public void logBasicEvent(Context context, String content) {
        if (!enabled) return;
        thread.standalone(() -> {
            String c = correctStringLength(content, 100);
            getFirebaseAnalytics(context).logEvent(
                    Event.EVENT,
                    getBundle(Param.EVENT_EXTRA, c)
            );
        }, throwable -> {
            log.exception(throwable);
        });
    }

    @Override
    public Bundle getBundle(String key, Object value) {
        return getBundle(key, value, null);
    }

    @Override
    public Bundle getBundle(String key, int value) {
        return getBundle(key, value, null);
    }

    @Override
    public Bundle getBundle(String key, int value, Bundle bundle) {
        return getBundle(key, (Integer) value, bundle);
    }

    @Override
    public Bundle getBundle(String key, Object value, Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        if (value instanceof String) {
            bundle.putString(key, (String) value);
        }
        if (value instanceof Integer) {
            bundle.putInt(key, (Integer) value);
        }
        return bundle;
    }

    private String correctStringLength(String text, int len) {
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (len < 0 || len > text.length()) {
            return text;
        }
        return text.substring(0, len);
    }
}
