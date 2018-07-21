package com.bukhmastov.cdoitmo.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.PikaActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.Random;

import javax.inject.Inject;

public class AboutFragment extends ConnectedFragment {

    private static final String TAG = "AboutFragment";
    private final Random random = new Random();
    private int counterToPika = 0;
    private final int tapsToPika = 5;

    @Inject
    Log log;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        log.v(TAG, "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    @Override
    public void onViewCreated() {
        TextView app_version = container.findViewById(R.id.app_version);
        if (app_version != null) {
            app_version.setText(activity.getString(R.string.version) + " " + App.versionName + " (" + App.versionCode + " " + activity.getString(R.string.build) + ")");
        }
        // ----------
        View block_pika = container.findViewById(R.id.block_pika);
        if (block_pika != null) {
            block_pika.setOnClickListener(v -> {
                if (counterToPika >= tapsToPika) {
                    if (random.nextInt(200) % 10 == 0) {
                        activity.startActivity(new Intent(activity, PikaActivity.class));
                    }
                } else {
                    counterToPika++;
                }
            });
        }
        // ----------
        View open_log = container.findViewById(R.id.open_log);
        if (open_log != null) {
            open_log.setOnClickListener(v -> activity.openFragment(ConnectedActivity.TYPE.STACKABLE, LogFragment.class, null));
        }
        // ----------
        View block_send_mail = container.findViewById(R.id.block_send_mail);
        if (block_send_mail != null) {
            block_send_mail.setOnClickListener(v -> {
                log.v(TAG, "send_mail clicked");
                firebaseAnalyticsProvider.logBasicEvent(activity, "send mail clicked");
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bukhmastov-alex@ya.ru"});
                    activity.startActivity(Intent.createChooser(emailIntent, activity.getString(R.string.send_mail) + "..."));
                } catch (Exception e) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
        // ----------
        View block_send_vk = container.findViewById(R.id.block_send_vk);
        if (block_send_vk != null) {
            block_send_vk.setOnClickListener(v -> {
                log.v(TAG, "send_vk clicked");
                firebaseAnalyticsProvider.logBasicEvent(activity, "send vk clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/write9780714")));
                } catch (Exception e) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
        // ----------
        View block_rate = container.findViewById(R.id.block_rate);
        if (block_rate != null) {
            block_rate.setOnClickListener(v -> {
                log.v(TAG, "block_rate clicked");
                firebaseAnalyticsProvider.logBasicEvent(activity, "app rate clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.bukhmastov.cdoitmo")));
                } catch (Exception e) {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.bukhmastov.cdoitmo")));
                    } catch (Exception e2) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }
            });
        }
        // ----------
        View block_github = container.findViewById(R.id.block_github);
        if (block_github != null) {
            block_github.setOnClickListener(v -> {
                log.v(TAG, "block_github clicked");
                firebaseAnalyticsProvider.logBasicEvent(activity, "view github clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bukhmastov/cdoitmo")));
                } catch (Exception e) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
        // ----------
        View block_donate = container.findViewById(R.id.block_donate);
        if (block_donate != null) {
            block_donate.setOnClickListener(v -> {
                log.v(TAG, "block_donate clicked  ┬─┬ ノ( ゜-゜ノ)");
                firebaseAnalyticsProvider.logBasicEvent(activity, "donate clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://yasobe.ru/na/cdoifmo")));
                } catch (Exception e) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_about;
    }

    @Override
    protected int getRootId() {
        return 0;
    }
}
