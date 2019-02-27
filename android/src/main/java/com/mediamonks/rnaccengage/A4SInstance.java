package com.mediamonks.rnaccengage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.ad4screen.sdk.A4S;

/**
 * Calls to A4S.get() should be done from this class, because A4S calls Toast(),
 * which needs to be guaranteed to be called from the main thread, and the
 * Accengage module does not take care of this.
 */
public class A4SInstance {
    private static Handler uiHandler = new Handler(Looper.getMainLooper());

    public static void getA4S(final Context context, final Consumer<A4S> consumer) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                final A4S instance = A4S.get(context);
                consumer.accept(instance);
            }
        });
    }
}
