package com.leaden2.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioLibraryActivity extends AppCompatActivity {
    private static final int REQUEST_AUDIO = 7101;
    private static final int BG = Color.rgb(4, 9, 18);
    private static final int PANEL = Color.rgb(10, 20, 34);
    private static final int CYAN = Color.rgb(0, 229, 255);
    private static final int TEXT = Color.rgb(230, 246, 255);
    private static final int MUTED = Color.rgb(133, 166, 188);
    private LinearLayout list;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        renderAudios();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("MIS AUDIOS");
        title.setTextColor(CYAN);
        title.setTextSize(27);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(8));
        root.addView(title, matchWrap());

        TextView info = new TextView(this);
        info.setText("Agrega canciones o audiolibros del teléfono. Cada pista conserva su número aunque elimines otra.");
        info.setTextColor(MUTED);
        info.setTextSize(14);
        info.setGravity(Gravity.CENTER);
        info.setPadding(dp(10), dp(8), dp(10), dp(14));
        root.addView(info, matchWrap());

        Button add = button("＋  AGREGAR AUDIO");
        add.setOnClickListener(v -> chooseAudio());
        root.addView(add, buttonParams());

        Button back = button("VOLVER A ORVEX");
        back.setBackground(background(Color.rgb(10, 20, 34), Color.rgb(26, 68, 88), 12, 1));
        back.setTextColor(TEXT);
        back.setOnClickListener(v -> finish());
        root.addView(back, buttonParams());

        TextView section = new TextView(this);
        section.setText("BIBLIOTECA GUARDADA");
        section.setTextColor(CYAN);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        section.setPadding(dp(4), dp(20), dp(4), dp(8));
        root.addView(section, matchWrap());

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, matchWrap());
        setContentView(scroll);
    }

    private void chooseAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, REQUEST_AUDIO); }
        catch (Exception e) { Toast.makeText(this, "No se pudo abrir el selector de archivos.", Toast.LENGTH_LONG).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_AUDIO || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception e) {
            // Some document providers do not support persistable grants; playback may still work for this session.
        }
        String name = getDisplayName(uri);
        JSONArray audios = AudioLibraryStore.read(this);
        int id = AudioLibraryStore.nextId(this);
        JSONObject item = new JSONObject();
        try {
            item.put("id", id);
            item.put("name", name);
            item.put("uri", uri.toString());
            audios.put(item);
            AudioLibraryStore.save(this, audios);
            renderAudios();
            Toast.makeText(this, AudioLibraryStore.label(id) + " agregado.", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "No se pudo guardar el registro del audio.", Toast.LENGTH_LONG).show();
        }
    }

    private String getDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) return name;
                }
            }
        } catch (Exception ignored) { }
        String tail = uri.getLastPathSegment();
        return tail == null || tail.trim().isEmpty() ? "Archivo de audio" : tail;
    }

    private void renderAudios() {
        if (list == null) return;
        list.removeAllViews();
        JSONArray audios = AudioLibraryStore.read(this);
        if (audios.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Todavía no has agregado audios.");
            empty.setTextColor(MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(24), dp(12), dp(24));
            empty.setBackground(background(PANEL, Color.rgb(26, 68, 88), 12, 1));
            list.addView(empty, buttonParams());
            return;
        }
        for (int i = 0; i < audios.length(); i++) {
            JSONObject item = audios.optJSONObject(i);
            if (item == null) continue;
            final int id = item.optInt("id", -1);
            final String uri = item.optString("uri", "");
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(13), dp(12), dp(13), dp(12));
            card.setBackground(background(PANEL, Color.rgb(26, 68, 88), 12, 1));
            LinearLayout.LayoutParams cp = buttonParams();
            cp.bottomMargin = dp(8);
            list.addView(card, cp);

            TextView label = new TextView(this);
            label.setText(AudioLibraryStore.label(id));
            label.setTextColor(CYAN);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setTextSize(15);
            card.addView(label, matchWrap());

            TextView name = new TextView(this);
            name.setText(item.optString("name", "Archivo de audio"));
            name.setTextColor(TEXT);
            name.setTextSize(14);
            name.setPadding(0, dp(4), 0, dp(8));
            card.addView(name, matchWrap());

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(actions, matchWrap());
            Button play = button("REPRODUCIR");
            play.setTextSize(11);
            play.setOnClickListener(v -> playAudio(id, uri));
            actions.addView(play, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button delete = button("ELIMINAR");
            delete.setTextSize(11);
            delete.setBackground(background(Color.rgb(49, 17, 31), Color.rgb(255, 93, 112), 12, 1));
            delete.setTextColor(Color.WHITE);
            delete.setOnClickListener(v -> confirmDelete(id));
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            dp.leftMargin = dp(8);
            actions.addView(delete, dp);
        }
    }

    private void playAudio(int id, String uri) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_PLAY_LIBRARY_AUDIO);
        intent.putExtra(PlaybackService.EXTRA_AUDIO_URI, uri);
        intent.putExtra(PlaybackService.EXTRA_AUDIO_ID, id);
        ContextCompat.startForegroundService(this, intent);
        Toast.makeText(this, "Reproduciendo " + AudioLibraryStore.label(id), Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(int id) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar de la biblioteca")
            .setMessage("Se quitará " + AudioLibraryStore.label(id) + " de ORVEX. El archivo original del teléfono no se borrará.")
            .setPositiveButton("Eliminar", (dialog, which) -> deleteAudio(id))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void deleteAudio(int id) {
        JSONArray current = AudioLibraryStore.read(this);
        JSONArray updated = new JSONArray();
        String removedUri = null;
        for (int i = 0; i < current.length(); i++) {
            JSONObject item = current.optJSONObject(i);
            if (item == null) continue;
            if (item.optInt("id", -1) == id) removedUri = item.optString("uri", null);
            else updated.put(item);
        }
        AudioLibraryStore.save(this, updated);
        if (removedUri != null) {
            try { getContentResolver().releasePersistableUriPermission(Uri.parse(removedUri), Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (Exception ignored) { }
        }
        renderAudios();
        Toast.makeText(this, "Audio eliminado de la biblioteca.", Toast.LENGTH_SHORT).show();
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(2, 16, 25));
        b.setBackgroundTintList(null);
        b.setBackground(background(CYAN, CYAN, 12, 1));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setElevation(dp(2));
        return b;
    }
    private GradientDrawable background(int fill, int stroke, int radius, int width) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius)); d.setStroke(dp(width), stroke); return d;
    }
    private LinearLayout.LayoutParams buttonParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.topMargin = dp(8); return p; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
