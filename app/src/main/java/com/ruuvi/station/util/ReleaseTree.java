package com.ruuvi.station.util;

import android.annotation.SuppressLint;
import android.os.Environment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ReleaseTree extends Timber.Tree {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.UK);

    public ReleaseTree() {
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        deleteOldLogs();
    }

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable throwable) {
        writeLogToFile(tag, message, throwable);
    }

    @SuppressLint("CheckResult")
    private void deleteOldLogs() {
        // delete log files which are 7 days old
        // for each file get date from file name
        // delete files which are 7 days old
        Completable
                .fromAction(() -> {
                    File directory = getDirectoryPath();
                    File[] files = directory.listFiles();
                    Calendar today = Calendar.getInstance();
                    if (files != null) {
                        for (File file : files) {
                            Calendar fileDate = getDateFromFileName(file.getName());
                            if (fileDate != null && daysBetween(today, fileDate) >= 7) {
                                file.delete();
                            }
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {
                        },
                        Throwable::printStackTrace
                );
    }

    private static long daysBetween(Calendar startDate, Calendar endDate) {
        long end = endDate.getTimeInMillis();
        long start = startDate.getTimeInMillis();
        return TimeUnit.MILLISECONDS.toDays(Math.abs(end - start));
    }

    private void writeLogToFile(String tag, String message, Throwable throwable) {

        String fileName = getFileName();
        File filePath = getDirectoryPath();
        File file = new File(filePath, fileName);
        try {

            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true)));

            String tagString = "";
            if (tag != null) {
                tagString = " " + tag + " :";
            }
            printWriter.write("[" + getCurrentTimeString() + "]" + tagString + " " + message);
            if (throwable != null) {
                throwable.printStackTrace(printWriter);
            }
            printWriter.println();
            printWriter.println();

            printWriter.close();
        } catch (Throwable e) {
            e.printStackTrace();
            e.printStackTrace();
        }
    }

    private String getCurrentTimeString() {
        final Date now = new Date();
        return timeFormat.format(now);
    }

    private File getDirectoryPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    private String getFileName() {
        Calendar today = Calendar.getInstance();

        String monthString = String.valueOf(today.get(Calendar.MONTH) + 1);
        return today.get(Calendar.DAY_OF_MONTH) + "_" + monthString + "_" + today.get(Calendar.YEAR) +
                ".log";
    }

    private Calendar getDateFromFileName(String fileName) {
        try {
            String dateString = fileName.split(".log")[0];

            String[] dateValues = dateString.split("_");
            int days = Integer.parseInt(dateValues[0]);
            int month = Integer.parseInt(dateValues[1]) - 1;
            int year = Integer.parseInt(dateValues[2]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, days);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.YEAR, year);

            return calendar;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

}
