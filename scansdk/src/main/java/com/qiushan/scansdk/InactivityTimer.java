package com.qiushan.scansdk;

import android.app.Activity;
import android.content.DialogInterface;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class InactivityTimer {
    private static final int INACTIVITY_DELAY_SECONDS = 300;
    private final Activity activity;
    private final ScheduledExecutorService inactivityTimer = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private ScheduledFuture<?> inactivityFuture = null;

    public InactivityTimer(Activity activity) {
        this.activity = activity;
        this.onActivity();
    }

    public void onActivity() {
        this.cancel();
        this.inactivityFuture = this.inactivityTimer.schedule(new FinishListener(this.activity), 300L, TimeUnit.SECONDS);
    }

    public void shutdown() {
        this.cancel();
        this.inactivityTimer.shutdown();
    }

    private void cancel() {
        if (this.inactivityFuture != null) {
            this.inactivityFuture.cancel(true);
            this.inactivityFuture = null;
        }

    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private DaemonThreadFactory() {
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    }

    private static final class FinishListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, Runnable {
        private final Activity activityToFinish;

        public FinishListener(Activity activityToFinish) {
            this.activityToFinish = activityToFinish;
        }

        public void onCancel(DialogInterface dialogInterface) {
            this.run();
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            this.run();
        }

        public void run() {
            this.activityToFinish.finish();
        }
    }
}
