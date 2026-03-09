/*
 Copyright (c) 2025-2026 Stephen Gold

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.stephengold.joltjni.droid;

import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    // *************************************************************************
    // constants

    /**
     * tag for log output
     */
    final private static String logTag = MainActivity.class.getName();
    // *************************************************************************
    // fields

    /**
     * true while tests are running
     */
    static boolean running = true;
    /**
     * buffer holding text output from the tests, to be displayed
     */
    static StringBuffer buffer;
    // *************************************************************************
    // AppCompatActivity methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(logTag, "create MainActivity");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // edge-to-edge layout with system-bar insets:
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });

        TextView textView = findViewById(R.id.textview);
        textView.setMovementMethod(new ScrollingMovementMethod());

        if (buffer == null) { // for the original activity only!
            buffer = new StringBuffer();
            Context context = mainView.getContext();
            HelloJoltJni hjj = new HelloJoltJni(context);
            Thread testThread = new Thread(hjj, "HelloJoltJni");
            testThread.start();
        }

        Runnable update = () -> {
            Log.i(logTag, "execute update");
            String text = buffer.toString();
            textView.setText(text);
            if (!running) {
                textView.setGravity(11);
            }
        };

        Executor executor = ContextCompat.getMainExecutor(this);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                executor.execute(update);
            }
        };

        Timer timer = new Timer();
        long delayMilliseconds = 0L;
        long intervalMilliseconds = 500L;
        timer.schedule(task, delayMilliseconds, intervalMilliseconds);
    }
}
