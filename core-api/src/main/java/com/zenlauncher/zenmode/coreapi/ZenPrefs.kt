package com.zenlauncher.zenmode.coreapi

import android.content.Context
import android.content.SharedPreferences

/**
 * The one SharedPreferences file every on-device setting store shares — theme, sound, promise,
 * gestures, check-in and the rest of the `*Preferences` objects. Each store owns its own keys;
 * they all open the file through [zenPrefs] so its name is spelled exactly once. Lives in
 * core-api because [PromisePreferences] reads it from here, and the app's stores read it too.
 */
const val ZEN_PREFS_FILE = "zenmode_prefs"

/** This app's shared settings file ([ZEN_PREFS_FILE]). */
fun Context.zenPrefs(): SharedPreferences = getSharedPreferences(ZEN_PREFS_FILE, Context.MODE_PRIVATE)
