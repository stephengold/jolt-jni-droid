/*
Copyright (c) 2024-2026 Stephen Gold

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.github.stephengold.joltjni.droid;

import android.util.Log;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.JoltPhysicsObject;

/**
 * Utility methods for automated testing of Jolt JNI.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class TestUtils {
    // *************************************************************************
    // constants

    /**
     * {@code true} to enable the automatic {@code java.lang.ref.Cleaner}
     */
    final public static boolean automateFreeing = true;
    /**
     * {@code true} to log heap allocations in glue code
     */
    final public static boolean traceAllocations = false;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private TestUtils() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Initialize the loaded native library.
     */
    public static void initializeNativeLibrary() {
        logLibraryInfo();

        Jolt.setTraceAllocations(traceAllocations);
        if (automateFreeing) {
            JoltPhysicsObject.startCleaner(); // to reclaim native memory
        }

        // callbacks for memory allocation, assertions, and execution tracing:
        Jolt.registerDefaultAllocator();
        Jolt.installDefaultAssertCallback();
        Jolt.installDefaultTraceCallback();

        // Create and configure the factory:
        boolean success = Jolt.newFactory();
        assert success;
        Jolt.registerTypes();
    }

    /**
     * Send basic library information to the Android log during initialization.
     */
    public static void logLibraryInfo() {
        String line = "Jolt JNI version " + Jolt.versionString() + '-';
        line += Jolt.buildType();
        line += Jolt.isDoublePrecision() ? "Dp" : "Sp";
        line += " initializing...";
        Log.d("jolt-jni", line);
    }
}
