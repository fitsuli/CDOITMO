package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.builders.Room101ReviewBuilder;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.interfaces.Room101ClientResponseHandler;
import com.bukhmastov.cdoitmo.objects.Room101AddRequest;
import com.bukhmastov.cdoitmo.parse.Room101ViewRequestParse;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class Room101Fragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, Room101ReviewBuilder.register {

    private static final String TAG = "Room101Fragment";
    private boolean loaded = false;
    public static RequestHandle fragmentRequestHandle = null;
    private String action_extra = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        action_extra = getActivity().getIntent().getStringExtra("action_extra");
        if (action_extra != null) getActivity().getIntent().removeExtra("action_extra");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room101, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onRefresh() {
        load(true);
    }

    @Override
    public void onDenyRequest(final int reid, final int status) {
        if(Static.OFFLINE_MODE) {
            snackBar(getString(R.string.device_offline_action_refused));
        } else {
            (new AlertDialog.Builder(getContext())
                    .setTitle(R.string.request_deny)
                    .setMessage(getString(R.string.request_deny_1) + "\n" + getString(R.string.request_deny_2))
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            denyRequest(reid, status);
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create()
            ).show();
        }
    }
    private void denyRequest(final int reid, final int status){
        RequestParams params = new RequestParams();
        switch(status){
            case 1: params.put("getFunc", "snatRequest"); break;
            default: params.put("getFunc", "delRequest"); break;
        }
        params.put("reid", reid);
        params.put("login", Storage.file.perm.get(getContext(), "user#login"));
        params.put("password", Storage.file.perm.get(getContext(), "user#password"));
        Room101Client.post(getContext(), "delRequest.php", params, new Room101ClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                draw(R.layout.state_try_again);
                TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            denyRequest(reid, status);
                        }
                    });
                }
            }
            @Override
            public void onProgress(int state) {
                draw(R.layout.state_loading);
                TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
                if (loading_message != null) {
                    switch (state) {
                        case Room101Client.STATE_HANDLING: loading_message.setText(R.string.deny_request); break;
                    }
                }
            }
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                if(statusCode == 302){
                    load(true);
                } else {
                    draw(R.layout.state_try_again);
                    TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
                    if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                    View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
                    if (try_again_reload != null) {
                        try_again_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                denyRequest(reid, status);
                            }
                        });
                    }
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                fragmentRequestHandle = requestHandle;
            }
        });
    }

    private void addRequest(){
        if(Static.OFFLINE_MODE) {
            snackBar(getString(R.string.device_offline_action_refused));
        } else {
            draw(R.layout.layout_room101_add_request);
            final View room101_close_add_request = getActivity().findViewById(R.id.room101_close_add_request);
            final LinearLayout room101_back = (LinearLayout) getActivity().findViewById(R.id.room101_back);
            final LinearLayout room101_forward = (LinearLayout) getActivity().findViewById(R.id.room101_forward);
            final TextView room101_back_text = (TextView) getActivity().findViewById(R.id.room101_back_text);
            final TextView room101_forward_text = (TextView) getActivity().findViewById(R.id.room101_forward_text);
            final ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.room101_progress_bar);
            final Room101AddRequest room101AddRequest = new Room101AddRequest(getActivity(), new Room101AddRequest.callback() {
                @Override
                public void onProgress(int stage) {
                    try {
                        progressBar.setProgress((stage * 100) / Room101AddRequest.STAGES_COUNT);
                        room101_back.setAlpha(1f);
                        room101_forward.setAlpha(1f);
                        switch (stage) {
                            case Room101AddRequest.STAGE_PICK_DATE_LOAD:
                            case Room101AddRequest.STAGE_PICK_TIME_START_LOAD:
                            case Room101AddRequest.STAGE_PICK_TIME_END_LOAD:
                            case Room101AddRequest.STAGE_PICK_CONFIRMATION_LOAD:
                            case Room101AddRequest.STAGE_PICK_CREATE:
                                room101_back.setAlpha(0.2f);
                                room101_forward.setAlpha(0.2f);
                                if (stage == Room101AddRequest.STAGE_PICK_DATE_LOAD) {
                                    room101_back_text.setText(R.string.do_cancel);
                                    room101_forward_text.setText(R.string.next);
                                }
                                if (stage == Room101AddRequest.STAGE_PICK_TIME_START_LOAD) {
                                    room101_back_text.setText(R.string.back);
                                }
                                break;
                            case Room101AddRequest.STAGE_PICK_DATE:
                                room101_back_text.setText(R.string.do_cancel);
                                room101_forward_text.setText(R.string.next);
                                break;
                            case Room101AddRequest.STAGE_PICK_TIME_START:
                            case Room101AddRequest.STAGE_PICK_TIME_END:
                                room101_back_text.setText(R.string.back);
                                room101_forward_text.setText(R.string.next);
                                break;
                            case Room101AddRequest.STAGE_PICK_CONFIRMATION:
                                room101_back_text.setText(R.string.back);
                                room101_forward_text.setText(R.string.create);
                                break;
                            case Room101AddRequest.STAGE_PICK_DONE:
                                room101_back.setAlpha(0.0f);
                                room101_back_text.setText(R.string.close);
                                room101_forward_text.setText(R.string.close);
                                break;
                        }
                    } catch (Exception e){
                        Static.error(e);
                        snackBar(getString(R.string.error_occurred_while_room101_request));
                        load(false);
                    }
                }
                @Override
                public void onDraw(View view) {
                    try {
                        ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.room101_add_request_container));
                        if (vg != null) {
                            vg.removeAllViews();
                            vg.addView(view);
                        }
                    } catch (Exception e){
                        Static.error(e);
                        snackBar(getString(R.string.error_occurred));
                        load(false);
                    }
                }
                @Override
                public void onClose() {
                    load(false);
                }
                @Override
                public void onDone() {
                    load(true);
                }
            });
            if (room101_close_add_request != null) {
                room101_close_add_request.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        room101AddRequest.close(false);
                    }
                });
            }
            if (room101_back != null) {
                room101_back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        room101AddRequest.back();
                    }
                });
            }
            if (room101_forward != null) {
                room101_forward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        room101AddRequest.forward();
                    }
                });
            }
        }
    }

    private void load(){
        load(Storage.pref.get(getContext(), "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(getContext(), "pref_tab_refresh", "0")) : 0);
    }
    private void load(int refresh_rate){
        String cache = Storage.file.cache.get(getContext(), "room101#core");
        if (Objects.equals(cache, "") || refresh_rate == 0) {
            load(true);
        } else if (refresh_rate >= 0){
            try {
                if (new JSONObject(cache).getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                    load(true);
                } else {
                    load(false);
                }
            } catch (JSONException e) {
                Static.error(e);
                load(true);
            }
        } else {
            load(false);
        }
    }
    private void load(boolean force){
        if (!force || Static.OFFLINE_MODE) {
            if (!Objects.equals(Storage.file.cache.get(getContext(), "room101#core"), "")) {
                display();
                return;
            }
        }
        if (!Static.OFFLINE_MODE) {
            execute(getContext(), "delRequest", new Room101ClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new Room101ViewRequestParse(new Room101ViewRequestParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (json != null) {
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                        jsonObject.put("data", json);
                                        Storage.file.cache.put(getContext(), "room101#core", jsonObject.toString());
                                    } catch (JSONException e) {
                                        Static.error(e);
                                    }
                                }
                                display();
                            }
                        }, getContext()).execute(response);
                    } else {
                        loadFailed();
                    }
                }
                @Override
                public void onProgress(int state) {
                    draw(R.layout.state_loading);
                    Activity activity = getActivity();
                    if (activity != null) {
                        TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
                        if (loading_message != null) {
                            switch (state) {
                                case Room101Client.STATE_HANDLING:
                                    loading_message.setText(R.string.loading);
                                    break;
                            }
                        }
                    }
                }
                @Override
                public void onFailure(int state, int statusCode, Header[] headers) {
                    Activity activity = getActivity();
                    switch (state) {
                        case Room101Client.FAILED_OFFLINE:
                            try {
                                if (!Objects.equals(Storage.file.cache.get(getContext(), "room101#core"), "")) {
                                    display();
                                    return;
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                            draw(R.layout.state_offline);
                            if (activity != null) {
                                View offline_reload = activity.findViewById(R.id.offline_reload);
                                if (offline_reload != null) {
                                    offline_reload.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            load(true);
                                        }
                                    });
                                }
                            }
                            break;
                        case Room101Client.FAILED_TRY_AGAIN:
                        case Room101Client.FAILED_EXPECTED_REDIRECTION:
                            draw(R.layout.state_try_again);
                            if (activity != null) {
                                if (state == Room101Client.FAILED_EXPECTED_REDIRECTION) {
                                    TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                                    if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                                }
                                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                if (try_again_reload != null) {
                                    try_again_reload.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            load(true);
                                        }
                                    });
                                }
                            }
                            break;
                        case Room101Client.FAILED_AUTH:
                            draw(R.layout.state_try_again);
                            if (activity != null) {
                                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                if (try_again_reload != null) {
                                    ((ViewGroup) try_again_reload.getParent()).removeView(try_again_reload);
                                    TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                                    if (try_again_message != null) try_again_message.setText(R.string.room101_auth_failed);
                                }
                            }
                            break;
                    }
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    fragmentRequestHandle = requestHandle;
                }
            });
        } else {
            draw(R.layout.state_offline);
            Activity activity = getActivity();
            if (activity != null) {
                View offline_reload = getActivity().findViewById(R.id.offline_reload);
                if (offline_reload != null) {
                    offline_reload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            load(true);
                        }
                    });
                }
            }
        }
    }
    private void display(){
        try {
            JSONObject viewRequest = null;
            String cache = Storage.file.cache.get(getContext(), "room101#core");
            if (!cache.isEmpty()) {
                viewRequest = new JSONObject(cache).getJSONObject("data");
            }
            if (viewRequest == null) throw new NullPointerException("viewRequest cannot be null");
            if (action_extra != null) {
                switch (action_extra) {
                    case "create":
                        action_extra = null;
                        addRequest();
                        return;
                }
                action_extra = null;
            }
            draw(R.layout.layout_room101_review);
            TextView room101_limit = (TextView) getActivity().findViewById(R.id.room101_limit);
            TextView room101_last = (TextView) getActivity().findViewById(R.id.room101_last);
            TextView room101_penalty = (TextView) getActivity().findViewById(R.id.room101_penalty);
            if (room101_limit != null) room101_limit.setText(viewRequest.getString("limit"));
            if (room101_last != null) room101_last.setText(viewRequest.getString("left"));
            if (room101_penalty != null) room101_penalty.setText(viewRequest.getString("penalty"));
            final LinearLayout room101_review_container = (LinearLayout) getActivity().findViewById(R.id.room101_review_container);
            (new Room101ReviewBuilder(getActivity(), this, viewRequest.getJSONArray("sessions"), new Room101ReviewBuilder.response(){
                public void state(final int state, final View layout){
                    try {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (room101_review_container != null) {
                                    room101_review_container.removeAllViews();
                                    if (state == Room101ReviewBuilder.STATE_DONE || state == Room101ReviewBuilder.STATE_LOADING) {
                                        room101_review_container.addView(layout);
                                    } else if (state == Room101ReviewBuilder.STATE_FAILED) {
                                        loadFailed();
                                    }
                                }
                            }
                        });
                    } catch (NullPointerException e){
                        Static.error(e);
                        loadFailed();
                    }
                }
            })).start();
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.room101_review_swipe);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            // плавающая кнопка
            FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
            if (fab != null) {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addRequest();
                    }
                });
            }
            Static.showUpdateTime(getActivity(), viewRequest.getLong("timestamp"), R.id.room101_review_swipe, false);
        } catch (Exception e){
            Static.error(e);
            loadFailed();
        }
    }
    private void loadFailed(){
        try {
            draw(R.layout.state_try_again);
            View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load(true);
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_room101));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    public static void execute(final Context context, String scope, final Room101ClientResponseHandler responseHandler){
        RequestParams params = new RequestParams();
        params.put("getFunc", "isLoginPassword");
        params.put("view", scope);
        params.put("login", Storage.file.perm.get(context, "user#login"));
        params.put("password", Storage.file.perm.get(context, "user#password"));
        Room101Client.post(context, "index.php", params, new Room101ClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if (response.contains("Доступ запрещен") || (response.contains("Неверный") && response.contains("логин/пароль"))) {
                    responseHandler.onFailure(Room101Client.FAILED_AUTH, Room101Client.STATUS_CODE_EMPTY, null);
                } else {
                    responseHandler.onFailure(Room101Client.FAILED_EXPECTED_REDIRECTION, Room101Client.STATUS_CODE_EMPTY, null);
                }
            }
            @Override
            public void onProgress(int state) {
                responseHandler.onProgress(state);
            }
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                if (statusCode == 302) {
                    boolean found = false;
                    for (Header header : headers) {
                        if (Objects.equals(header.getName(), "Location")) {
                            String url = header.getValue().trim();
                            if (!url.isEmpty()) {
                                found = true;
                                Room101Client.get(context, url, null, new Room101ClientResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, String response) {
                                        responseHandler.onSuccess(statusCode, response);
                                    }
                                    @Override
                                    public void onProgress(int state) {
                                        responseHandler.onProgress(state);
                                    }
                                    @Override
                                    public void onFailure(int state, int statusCode, Header[] headers) {
                                        responseHandler.onFailure(state, statusCode, headers);
                                    }
                                    @Override
                                    public void onNewHandle(RequestHandle requestHandle) {
                                        responseHandler.onNewHandle(requestHandle);
                                    }
                                });
                            }
                            break;
                        }
                    }
                    if (!found) {
                        responseHandler.onFailure(Room101Client.FAILED_EXPECTED_REDIRECTION, Room101Client.STATUS_CODE_EMPTY, null);
                    }
                } else {
                    responseHandler.onFailure(Room101Client.FAILED_EXPECTED_REDIRECTION, Room101Client.STATUS_CODE_EMPTY, null);
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                responseHandler.onNewHandle(requestHandle);
            }
        });
    }
    private void snackBar(String text){
        View room101_review_swipe = getActivity().findViewById(R.id.room101_review_swipe);
        if (room101_review_swipe != null) {
            Snackbar snackbar = Snackbar.make(room101_review_swipe, text, Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(Static.colorBackgroundSnackBar);
            snackbar.show();
        }
    }

}