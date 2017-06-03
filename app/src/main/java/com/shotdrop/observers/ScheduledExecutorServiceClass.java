package com.shotdrop.observers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.shotdrop.utils.Prefs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

public class ScheduledExecutorServiceClass implements Runnable {

    private ScreenshotCallback callback;

    private File screenshotsFolder;

    private Prefs prefs;

    public ScheduledExecutorServiceClass(@NonNull String path, @NonNull Context context,
                                         @NonNull ScreenshotCallback callback) {
        this.screenshotsFolder = new File(path);
        this.prefs = new Prefs(context);
        this.callback = callback;
    }

    @Override
    public void run() {
        List<File> files = Arrays.asList(screenshotsFolder.listFiles());
        int count = files.size();
        if (count <= 0) {
            return;
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });
        long lastModifiedMemory = lastModifiedFromMemory();
        if (lastModifiedMemory == 0) {
            lastModifiedMemory = files.get(count - 1).lastModified();
            prefs.putString(Prefs.LAST_SCREENSHOT_MODIFIED, lastModifiedMemory);
        }
        if (isCompletelyWritten(files.get(count - 1)) &&
                files.get(count - 1).lastModified() > lastModifiedMemory) {
            prefs.putString(Prefs.LAST_SCREENSHOT_MODIFIED,
                    files.get(count - 1).lastModified());
            callback.onScreenshotTaken(files.get(count - 1).getName());
        }
    }

    private boolean isCompletelyWritten(File file) {
        RandomAccessFile stream = null;
        try {
            stream = new RandomAccessFile(file, "rw");
            return true;
        } catch (Exception e) {
            Timber.d("Skipping file " + file.getName() +
                    " for this iteration due it's not completely written");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Timber.e("Exception during closing file " + file.getName());
                }
            }
        }
        return false;
    }

    private long lastModifiedFromMemory() {
        try {
            return Long.parseLong(prefs.getString(Prefs.LAST_SCREENSHOT_MODIFIED, "0"));
        } catch (NumberFormatException e) {
            Timber.e(e.getLocalizedMessage());
            return 0;
        }
    }
}