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
package testjoltjni;

import android.util.Log;
import com.github.stephengold.joltjni.BroadPhaseLayerInterface;
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.JoltPhysicsObject;
import com.github.stephengold.joltjni.ObjectLayerPairFilter;
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilter;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.readonly.ConstBroadPhaseLayerInterface;
import com.github.stephengold.joltjni.readonly.ConstJoltPhysicsObject;
import com.github.stephengold.joltjni.readonly.ConstObjectLayerPairFilter;
import com.github.stephengold.joltjni.readonly.ConstObjectVsBroadPhaseLayerFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

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
     * {@code true} to explicitly free native objects via {@code testClose()}
     */
    final public static boolean explicitFreeing = true;
    /**
     * {@code true} to log heap allocations in glue code
     */
    final public static boolean traceAllocations = false;
    /**
     * customary number of object layers
     */
    final public static int numObjLayers = 2;
    /**
     * object layer for moving objects (but the Jolt samples assign 5 instead,
     * and Sport-Jolt assigns 0)
     */
    final public static int objLayerMoving = 1;
    /**
     * object layer for non-moving objects (but the Jolt samples assign 4
     * instead, and Sport-Jolt assigns 1)
     */
    final public static int objLayerNonMoving = 0;
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
     * Clean up after a test.
     */
    public static void cleanup() {
        Jolt.unregisterTypes();
        Jolt.destroyFactory();

        System.gc();
    }

    /**
     * Clean up the specified {@code PhysicsSystem}.
     *
     * @param physicsSystem the system to clean up (not {@code null})
     */
    public static void cleanupPhysicsSystem(PhysicsSystem physicsSystem) {
        ConstBroadPhaseLayerInterface mapObj2Bp
                = physicsSystem.getBroadPhaseLayerInterface();
        ConstObjectLayerPairFilter ovoFilter
                = physicsSystem.getObjectLayerPairFilter();
        ConstObjectVsBroadPhaseLayerFilter ovbFilter
                = physicsSystem.getObjectVsBroadPhaseLayerFilter();

        testClose(physicsSystem, ovbFilter, ovoFilter, mapObj2Bp);
    }

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
        Jolt.installCrashAssertCallback();
        Jolt.installJavaTraceCallback(System.err);
        //Jolt.installAndroidTraceCallback(Log.INFO, "jolt-jni");

        // Create and configure the factory:
        boolean success = Jolt.newFactory();
        assert success;
        Jolt.registerTypes();
        Jolt.registerHair();
    }

    /**
     * Load raw bytes from the specified file.
     *
     * @param path a filesystem path to the file (not {@code null})
     * @return a new direct buffer
     */
    public static ByteBuffer loadFileAsBytes(String path) {
        String q = MyString.quote(path);

        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("file doesn't exist:  " + q);
        } else if (!file.isFile()) {
            throw new RuntimeException("file isn't normal:  " + q);
        } else if (!file.canRead()) {
            throw new RuntimeException("file isn't readable:  " + q);
        }
        InputStream inputStream = null;

        // Read the file to determine its size in bytes:
        try {
            InputStream is = new FileInputStream(file);
            inputStream = is;
        } catch (IOException exception) {
            // do nothing
        }
        if (inputStream == null) {
            throw new RuntimeException("no input stream for file:  " + q);
        }
        int totalBytes = 0;
        byte[] tmpArray = new byte[4096];
        try {
            while (true) {
                int numBytesRead = inputStream.read(tmpArray);
                if (numBytesRead < 0) {
                    break;
                }
                totalBytes += numBytesRead;
            }
            inputStream.close();

        } catch (IOException exception) {
            throw new RuntimeException("failed to read file " + q);
        }
        ByteBuffer result = Jolt.newDirectByteBuffer(totalBytes);

        // Read the file again to fill the buffer with data:
        inputStream = null;
        try {
            InputStream is = new FileInputStream(file);
            inputStream = is;
        } catch (IOException exception) {
            // do nothing
        }
        if (inputStream == null) {
            throw new RuntimeException("no input stream for file:  " + q);
        }
        try {
            while (true) {
                int numBytesRead = inputStream.read(tmpArray);
                if (numBytesRead < 0) {
                    break;

                } else if (numBytesRead == tmpArray.length) {
                    result.put(tmpArray);

                } else {
                    for (int i = 0; i < numBytesRead; ++i) {
                        byte b = tmpArray[i];
                        result.put(b);
                    }
                }
            }
            inputStream.close();

        } catch (IOException exception) {
            throw new RuntimeException("failed to read file " + q);
        }

        result.flip();
        return result;
    }

    /**
     * Allocate and initialize a {@code PhysicsSystem} in the customary
     * configuration.
     *
     * @param maxBodies the desired number of bodies (&ge;1)
     * @return a new system
     */
    public static PhysicsSystem newPhysicsSystem(int maxBodies) {
        // broadphase layer IDs:
        int bpLayerNonMoving = 0;
        int bpLayerMoving = 1;
        int numBpLayers = 2;

        BroadPhaseLayerInterface mapObj2Bp
                = new BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers)
                        .mapObjectToBroadPhaseLayer(
                                objLayerNonMoving, bpLayerNonMoving)
                        .mapObjectToBroadPhaseLayer(
                                objLayerMoving, bpLayerMoving);
        ObjectLayerPairFilter objVsObjFilter
                = new ObjectLayerPairFilterTable(numObjLayers)
                        .enableCollision(objLayerMoving, objLayerMoving)
                        .enableCollision(objLayerMoving, objLayerNonMoving);
        ObjectVsBroadPhaseLayerFilter objVsBpFilter
                = new ObjectVsBroadPhaseLayerFilterTable(
                        mapObj2Bp, numBpLayers, objVsObjFilter, numObjLayers);

        int numBodyMutexes = 0; // 0 means "use the default value"
        int maxBodyPairs = 65_536;
        int maxContacts = 20_480;
        PhysicsSystem result = new PhysicsSystem();
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
                mapObj2Bp, objVsBpFilter, objVsObjFilter);

        return result;
    }

    /**
     * Log basic library information during initialization.
     */
    public static void logLibraryInfo() {
        String message = String.format(
                "Jolt JNI version %s-%s%s initializing%n",
                Jolt.versionString(), Jolt.buildType(),
                Jolt.isDoublePrecision() ? "Dp" : "Sp");
        Log.i("jolt-jni", message);
    }

    /**
     * Return the recommended number of worker threads.
     *
     * @return the count (&ge;1)
     */
    public static int numThreads() {
        int numCpus = Runtime.getRuntime().availableProcessors();
        int result = (int) Math.floor(0.9 * numCpus);
        if (result < 1) {
            result = 1;
        }

        return result;
    }

    /**
     * If explicit freeing is enabled, test the {@code close()} methods of the
     * specified physics objects.
     *
     * @param collection the objects to test (not {@code null})
     */
    public static void testClose(
            Collection<? extends ConstJoltPhysicsObject> collection) {
        if (explicitFreeing) {
            int numObjects = collection.size();
            ConstJoltPhysicsObject[] array
                    = new ConstJoltPhysicsObject[numObjects];
            collection.toArray(array);
            testClose(array);
        }
    }

    /**
     * If explicit freeing is enabled, test the {@code close()} methods of the
     * specified physics objects.
     *
     * @param objects the objects to test (none of them {@code null})
     */
    public static void testClose(ConstJoltPhysicsObject... objects) {
        if (explicitFreeing) {
            for (ConstJoltPhysicsObject object : objects) {
                if (object instanceof PhysicsSystem) {
                    PhysicsSystem system = (PhysicsSystem) object;
                    system.forgetMe();
                }
                object.close();
            }
        }
    }
}
