package com.leaden2.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Persistent metadata for audio files selected by the user. Files remain in their original location. */
public final class AudioLibraryStore {
    public static final String PREFS = "leaden2_local_data";
    public static final String KEY_AUDIOS = "audio_library";
    public static final String KEY_NEXT_ID = "audio_library_next_id";

    private AudioLibraryStore() { }

    public static JSONArray read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try { return new JSONArray(prefs.getString(KEY_AUDIOS, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    public static void save(Context context, JSONArray audios) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_AUDIOS, audios.toString()).apply();
    }

    public static int nextId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int id = Math.max(1, prefs.getInt(KEY_NEXT_ID, 1));
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply();
        return id;
    }

    public static JSONObject findById(Context context, int id) {
        JSONArray audios = read(context);
        for (int i = 0; i < audios.length(); i++) {
            JSONObject item = audios.optJSONObject(i);
            if (item != null && item.optInt("id", -1) == id) return item;
        }
        return null;
    }

    public static String label(int id) {
        String[] words = {"", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
            "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve", "veinte"};
        return id > 0 && id < words.length ? "audio " + words[id] : "audio " + id;
    }
}
