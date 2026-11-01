package com.worshipflow.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.activity.result.ActivityResult;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CapacitorPlugin(name = "NativeAudio")
public class NativeAudioPlugin extends Plugin {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private boolean loopEnabled = false;
    private Runnable progressTask;

    @PluginMethod
    public void chooseAudioFolder(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/root/primary"));
        }
        startActivityForResult(call, intent, "folderResult");
    }

    @ActivityCallback
    private void folderResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            call.reject("Aucun dossier sélectionné.");
            return;
        }
        Uri tree = result.getData().getData();
        try {
            int flags = result.getData().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContext().getContentResolver().takePersistableUriPermission(tree, flags | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            call.reject("Impossible de conserver l’accès au dossier : " + e.getMessage());
            return;
        }
        JSObject out = new JSObject();
        out.put("treeUri", tree.toString());
        out.put("displayName", tree.getLastPathSegment() == null ? "Dossier audio" : tree.getLastPathSegment());
        call.resolve(out);
    }

    @PluginMethod
    public void listAudioFiles(PluginCall call) {
        String raw = call.getString("treeUri");
        if (raw == null || raw.isEmpty()) {
            call.reject("Sélectionnez d’abord un dossier audio.");
            return;
        }
        worker.execute(() -> {
            try {
                Uri tree = Uri.parse(raw);
                String root = DocumentsContract.getTreeDocumentId(tree);
                JSArray files = new JSArray();
                Set<String> visited = new HashSet<>();
                scan(tree, root, files, visited, 0);
                JSObject out = new JSObject();
                out.put("files", files);
                call.resolve(out);
            } catch (Exception e) {
                call.reject("La recherche des fichiers audio a échoué : " + e.getMessage());
            }
        });
    }

    private void scan(Uri tree, String parentId, JSArray files, Set<String> visited, int depth) throws JSONException {
        if (depth > 12 || visited.contains(parentId) || files.length() >= 10000) return;
        visited.add(parentId);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        String[] projection = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };
        try (Cursor cursor = getContext().getContentResolver().query(children, projection, null, null, null)) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int typeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
            while (cursor.moveToNext() && files.length() < 10000) {
                String id = idCol >= 0 ? cursor.getString(idCol) : null;
                String name = nameCol >= 0 ? cursor.getString(nameCol) : "Audio";
                String mime = typeCol >= 0 ? cursor.getString(typeCol) : "";
                if (id == null) continue;
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    scan(tree, id, files, visited, depth + 1);
                } else if (isAudio(name, mime)) {
                    Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    JSObject item = new JSObject();
                    item.put("uri", doc.toString());
                    item.put("name", name == null ? "Audio" : name);
                    item.put("mime", mime == null ? "" : mime);
                    item.put("size", sizeCol >= 0 && !cursor.isNull(sizeCol) ? cursor.getLong(sizeCol) : 0);
                    files.put(item);
                }
            }
        }
    }

    private boolean isAudio(String name, String mime) {
        if (mime != null && mime.toLowerCase().startsWith("audio/")) return true;
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".aac") || n.endsWith(".ogg") || n.endsWith(".wav") || n.endsWith(".flac") || n.endsWith(".opus");
    }

    @PluginMethod
    public void getDuration(PluginCall call) {
        String raw = call.getString("uri");
        if (raw == null) { call.reject("Fichier audio introuvable."); return; }
        worker.execute(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(getContext(), Uri.parse(raw));
                String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                JSObject out = new JSObject();
                out.put("duration", value == null ? 0 : Long.parseLong(value) / 1000.0);
                call.resolve(out);
            } catch (Exception e) {
                call.reject("Durée audio indisponible : " + e.getMessage());
            } finally {
                try { retriever.release(); } catch (Exception ignored) {}
            }
        });
    }

    @PluginMethod
    public void play(PluginCall call) {
        String raw = call.getString("uri");
        if (raw == null) { call.reject("Fichier audio introuvable."); return; }
        releasePlayer();
        try {
            MediaPlayer next = new MediaPlayer();
            player = next;
            next.setLooping(loopEnabled);
            next.setOnPreparedListener(mp -> {
                Integer seek = call.getInt("startAt", 0);
                if (seek != null && seek > 0) mp.seekTo(seek * 1000);
                mp.start();
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                startProgress();
                call.resolve(new JSObject());
            });
            next.setOnCompletionListener(mp -> {
                stopProgress();
                notifyListeners("ended", new JSObject());
            });
            next.setOnErrorListener((mp, what, extra) -> {
                stopProgress();
                JSObject error = new JSObject(); error.put("message", "Lecture audio impossible (" + what + "/" + extra + ").");
                notifyListeners("error", error);
                return true;
            });
            next.setDataSource(getContext(), Uri.parse(raw));
            next.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            call.reject("Impossible d’ouvrir ce fichier audio : " + e.getMessage());
        }
    }

    @PluginMethod public void pause(PluginCall call) {
        if (player != null && player.isPlaying()) player.pause();
        resolveState(call);
    }
    @PluginMethod public void resume(PluginCall call) {
        if (player != null) player.start();
        startProgress();
        resolveState(call);
    }
    @PluginMethod public void seekTo(PluginCall call) {
        if (player != null) {
            Integer seconds = call.getInt("seconds", 0);
            player.seekTo(Math.max(0, seconds == null ? 0 : seconds) * 1000);
        }
        resolveState(call);
    }
    @PluginMethod public void setLoop(PluginCall call) {
        loopEnabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        if (player != null) player.setLooping(loopEnabled);
        call.resolve();
    }
    @PluginMethod public void stop(PluginCall call) {
        releasePlayer();
        Window window = getActivity().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        call.resolve();
    }
    @PluginMethod public void setImmersive(PluginCall call) {
        boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        Window window = getActivity().getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                if (enabled) {
                    controller.hide(WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else controller.show(WindowInsets.Type.systemBars());
            }
        } else {
            View decor = window.getDecorView();
            int flags = enabled ? (View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE) : View.SYSTEM_UI_FLAG_VISIBLE;
            decor.setSystemUiVisibility(flags);
        }
        call.resolve();
    }

    private void resolveState(PluginCall call) {
        JSObject out = new JSObject();
        if (player != null) {
            out.put("time", player.getCurrentPosition() / 1000.0);
            out.put("duration", player.getDuration() / 1000.0);
            out.put("playing", player.isPlaying());
        } else {
            out.put("time", 0); out.put("duration", 0); out.put("playing", false);
        }
        call.resolve(out);
    }
    private void startProgress() {
        stopProgress();
        progressTask = new Runnable() {
            @Override public void run() {
                if (player == null) return;
                try {
                    JSObject state = new JSObject();
                    state.put("time", player.getCurrentPosition() / 1000.0);
                    state.put("duration", player.getDuration() / 1000.0);
                    state.put("playing", player.isPlaying());
                    notifyListeners("progress", state);
                    if (player.isPlaying()) handler.postDelayed(this, 350);
                } catch (Exception ignored) {}
            }
        };
        handler.post(progressTask);
    }
    private void stopProgress() {
        if (progressTask != null) handler.removeCallbacks(progressTask);
        progressTask = null;
    }
    private void releasePlayer() {
        stopProgress();
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }
}
