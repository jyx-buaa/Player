    /**
     * @author Aekasitt Guruvanich
     * on 7/22/2015.
     */
package com.nined.player.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtils {
    private static SharedPreferences prefs;
    private static void buildPrefs(Context context) {
        prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static void save(Context context, String key, int value) {
        if (prefs==null) buildPrefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void save(Context context, String key, String value) {
        if (prefs==null) buildPrefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void save(Context context, String key, long value) {
        if (prefs==null) buildPrefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static String getString(Context context, String key, String defaultValue) {
        if (prefs==null) buildPrefs(context);
        return (prefs.contains(key))? prefs.getString(key, defaultValue) : defaultValue;
    }

    public static int getInt(Context context, String key, int defaultValue) {
        if (prefs==null) buildPrefs(context);
        return (prefs.contains(key))? prefs.getInt(key, defaultValue) : defaultValue;
    }

    public static long getLong(Context context, String key, long defaultValue) {
        if (prefs==null) buildPrefs(context);
        return (prefs.contains(key))? prefs.getLong(key, defaultValue) : defaultValue;
    }
}
