package ru.thelv.lib;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences
{
    private static SharedPreferences this_;

    public static void init(Activity activity)
    {
        this_=PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public static int getInt(String key, int def)
    {
        return Integer.parseInt(this_.getString(key, Integer.toString(def)));
    }

    public static float getFloat(String key, float def)
    {
        return Float.parseFloat(this_.getString(key, Float.toString(def)));
    }

    public static void setFloat(String key, float val)
    {
        setString(key, Float.toString(val));
    }

    public static String getString(String key, String def)
    {
        return this_.getString(key, def);
    }

    public static boolean setString(String key, String val)
    {
        SharedPreferences.Editor e=this_.edit();
        e.putString(key, val);
        return e.commit();
    }

    public static boolean getBoolean(String key, boolean def)
    {
        return this_.getBoolean(key, def);
    }

    public static boolean setBoolean(String key, boolean val)
    {
        SharedPreferences.Editor e=this_.edit();
        e.putBoolean(key, val);
        return e.commit();
    }
}
