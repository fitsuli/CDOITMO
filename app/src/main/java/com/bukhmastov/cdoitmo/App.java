package com.bukhmastov.cdoitmo;

import android.app.Application;
import android.content.res.Configuration;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.Locale;

public class App extends Application {

    private static final String TAG = "Application";
    private Locale locale;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.setEnabled(Storage.pref.get(this, "pref_allow_collect_logs", false));
            locale = Static.getLocale(this);
            Log.i(TAG, "Language | locale=" + locale.toString());
            setLocale();
            setFirebase();
        } catch (Throwable e) {
            Static.error(e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            setLocale();
        } catch (Throwable e) {
            Static.error(e);
        }
    }

    private void setLocale() throws Throwable {
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void setFirebase() {
        FirebaseCrashProvider.setEnabled(this);
        FirebaseAnalyticsProvider.setEnabled(this);
    }
}

/*
 * -------------
 * TODO ROAD MAP
 * -------------
 * -- Расписания --
 * ?? Добавить переключатель источника расписаний: ису или итмо
 * Обеспечение обратной совместимости
 *
 * -- Авторизация ИСУ --
 * ...
 *
 * -- ИСУ контент --
 * Информация о группе
 * Стипендии
 * Информация о неделе
 * Информация о пользователе
 *
 * -- Пофиксить баги из огненной базы --
 *
 */

/*
 * -------
 * СДЕЛАНО
 * -------
 * -- Расписания --
 * Расписание занятий
 *      Новый провайдер расписания
 *      Создание, редактирование и удаление занятий
 *      Фрагменты расписаний занятий:
 *          Разобраться с перезагрузкой фрагмента при resume (жизненный цикл)
 *          Добавить запоминание положения скролла
 *      Применить новое расписание в виджете
 *      Кэширование запросов расписания для предотвращения одновременного поиска по одинаковому запросу
 *      Применить новое расписание в приложении оставшегося времени
 *      Применить новое расписание в создании приложения оставшегося времени
 *      Применить новое расписание в настройках
 * Расписание занятий - старые классы удалены
 * Расписание экзаменов
 *      Новый провайдер расписания
 *      Применить в основном фрагменте
 *      Применить новое расписание в приложении оставшегося времени
 *      Применить новое расписание в создании приложения оставшегося времени
 *      Применить новое расписание в настройках
 * Расписание экзаменов - старые классы удалены
 *
 *
 */