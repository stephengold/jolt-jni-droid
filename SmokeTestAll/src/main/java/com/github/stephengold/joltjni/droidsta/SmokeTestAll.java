/*
 Copyright (c) 2024-2026 Stephen Gold

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
package com.github.stephengold.joltjni.droidsta;

import com.github.stephengold.joltjni.*;
import android.widget.TextView;
import com.github.stephengold.joltjni.enumerate.EPhysicsUpdateError;
import com.github.stephengold.joltjni.std.OfStream;

import java.nio.ByteBuffer;
import testjoltjni.TestUtils;

/**
 * Perform a "smoke test" on each of the Samples tests.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class SmokeTestAll {
    // *************************************************************************
    // constants

    /**
     * default number of physics steps to simulate during each invocation of
     * {@code smokeTest()}
     */
    final private static int defaultNumSteps = 1;
    // *************************************************************************
    // fields

    /**
     * compute queue shared by all test objects
     */
    private static ComputeQueue queue;
    /**
     * compute system shared by all test objects
     */
    private static ComputeSystem computeSystem;
    /**
     * renderer shared by all test objects
     */
    private static DebugRenderer renderer;
    /**
     * count invocations of {@code smokeTest()}
     */
    private static int numTests;
    /**
     * allocator shared by all test objects
     */
    private static TempAllocator tempAllocator;
    /**
     * view for displaying text
     */
    private static TextView textView;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SmokeTestAll() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Execute the tests, logging progress to the specified view.
     *
     * @param view for displaying progress (not {@code null})
     */
    static void run(TextView view) {
        System.loadLibrary("joltjni");
        TestUtils.initializeNativeLibrary();

        textView = view;
        print(Jolt.getConfigurationString());
        println();
        print(" built-in compute systems:");
        print(Jolt.implementsComputeCpu() ? " CPU" : "");
        print(Jolt.implementsComputeDx12() ? " DX12" : "");
        print(Jolt.implementsComputeMtl() ? " MTL" : "");
        print(Jolt.implementsComputeVk() ? " VK" : "");
        println();

        createSharedObjects();

        smokeTestAll();
    }
    // *************************************************************************
    // private methods

    /**
     * Allocate and initialize the shared DebugRenderer, TempAllocator,
     * ComputeSystem, and ComputeQueue.
     */
    private static void createSharedObjects() {
        // All tests share a single DebugRenderer:
        assert Jolt.implementsDebugRendering();
        String fileName = "SmokeTestAll.jor";
        int mode = StreamOutWrapper.out()
                | StreamOutWrapper.binary() | StreamOutWrapper.trunc();
        OfStream ofStream = new OfStream(fileName, mode);
        StreamOut streamOut = new StreamOutWrapper(ofStream);
        renderer = new DebugRendererRecorder(streamOut);

        // All tests share a single TempAllocator:
        int numBytes = 1 << 24; // 16 MiB
        tempAllocator = new TempAllocatorImpl(numBytes);

        // All tests share a single ComputeSystem:
        ComputeSystemResult csResult = ComputeSystem.createComputeSystem();
        if (csResult.hasError()) {
            println(csResult.getError());
            // If no GPU or driver, fall back upon a CPU compute system:
            csResult = ComputeSystem.createComputeSystemCpu();
        }
        assert !csResult.hasError();
        computeSystem = csResult.get().getPtr();
        Rtti rtti = computeSystem.getRtti();
        String systemName = rtti.getName();
        systemName = systemName.replace("ComputeSystem", "");
        systemName = systemName.replace("Impl", "");
        printf("  using a %s compute system%n%n", systemName);

        switch (systemName) {
            case "CPU":
                // Register CPU compute shaders:
                ComputeSystem.hairRegisterShaders(computeSystem);
                break;

            case "MTL":
                // Assign a loader for Metal compute shaders:
                Loader mtlLoader = makeLoader("/mtl/com/github/stephengold");
                computeSystem.setShaderLoader(mtlLoader);
                break;

            case "VK":
                // Assign a loader for Vulkan compute shaders:
                Loader vkLoader = makeLoader("/vk/com/github/stephengold");
                computeSystem.setShaderLoader(vkLoader);
                break;

            default:
                throw new RuntimeException("typeName = " + systemName);
        }

        // All tests share a single ComputeQueue:
        ComputeQueueResult queueResult = computeSystem.createComputeQueue();
        assert !queueResult.hasError();
        ComputeQueueRef queueRef = queueResult.get();
        queue = queueRef.getPtr();
    }

    /**
     * Create a custom loader that loads from the specified resource directory.
     *
     * @param resourcePath the path to the resource directory (not {@code null})
     * @return a new loader
     */
    private static Loader makeLoader(String resourcePath) {
        Loader result = new CustomLoader() {
            @Override
            public ByteBuffer loadShader(String shaderName) {
                String path = resourcePath + "/" + shaderName;
                ByteBuffer result = Jolt.loadResourceAsBytes(path);
                return result;
            }
        };

        return result;
    }

    /**
     * Allocate and initialize a {@code PhysicsSystem} in the customary
     * configuration.
     *
     * @param maxBodies the desired number of bodies (&ge;1)
     * @return a new system
     */
    private static PhysicsSystem newPhysicsSystem(int maxBodies) {
        BPLayerInterfaceImpl mapObj2Bp = new BPLayerInterfaceImpl();
        ObjectVsBroadPhaseLayerFilterImpl objVsBpFilter
                = new ObjectVsBroadPhaseLayerFilterImpl();
        ObjectLayerPairFilterImpl objVsObjFilter
                = new ObjectLayerPairFilterImpl();

        int numBodyMutexes = 0; // 0 means "use the default value"
        int maxBodyPairs = 5_000;
        int maxContacts = 9_000;
        PhysicsSystem result = new PhysicsSystem();
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
                mapObj2Bp, objVsBpFilter, objVsObjFilter);

        return result;
    }

    /**
     * Append the specified text to the view.
     *
     * @param text the text to append (not {@code null})
     */
    private static void print(String text) {
        textView.append(text);
    }

    /**
     * Format the specified arguments and append them to the view.
     *
     * @param text the text to append (not {@code null})
     */
    private static void printf(String format, Object... args) {
        String text = String.format(format, args);
        print(text);
    }

    /**
     * Append a line separator to the view.
     */
    private static void println() {
        String lineSeparator = System.getProperty("line.separator");
        print(lineSeparator);
    }

    /**
     * Append the specified text to the view, followed by a line separator.
     *
     * @param text the text to append (not {@code null})
     */
    private static void println(String text) {
        print(text);
        println();
    }

    /**
     * Invoke key methods of the specified Test to see whether they crash.
     *
     * @param test the Test object to use (not {@code null})
     */
    private static void smokeTest(Test test) {
        smokeTest(test, defaultNumSteps);
    }

    /**
     * Invoke key methods of the specified Test to see whether they crash.
     *
     * @param test the Test object to use (not {@code null})
     * @param numSteps the number of physics steps to simulate (&ge;0,
     * default=defaultNumSteps)
     */
    private static void smokeTest(Test test, int numSteps) {
        ++numTests;

        // Log the name of the test:
        String testName = test.getClass().getSimpleName();
        printf("=== Test #%d:  %s for %d step%s%n",
                numTests, testName, numSteps, (numSteps == 1) ? "" : "s");

        test.SetDebugRenderer(renderer);
        test.SetTempAllocator(tempAllocator);
        test.SetComputeSystem(computeSystem, queue);

        // Create new job/physics systems for each test:
        int numThreads = -1; // autodetect
        JobSystem jobSystem = new JobSystemThreadPool(
                Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, numThreads);
        test.SetJobSystem(jobSystem);

        int maxBodies = 1_300;
        PhysicsSystem physicsSystem = newPhysicsSystem(maxBodies);
        test.SetPhysicsSystem(physicsSystem);

        // Create a new contact listener for each test:
        ContactListener listener = new SamplesContactListener(test);
        physicsSystem.setContactListener(listener);

        test.Initialize();

        // Single-step the physics numSteps times:
        for (int i = 0; i < numSteps; ++i) {
            PreUpdateParams params = new PreUpdateParams();
            params.mDeltaTime = 0.02f;
            test.PrePhysicsUpdate(params);

            int collisionSteps = 1;
            int errors = physicsSystem.update(params.mDeltaTime, collisionSteps,
                    tempAllocator, jobSystem);
            assert errors == EPhysicsUpdateError.None : errors;

            test.PostPhysicsUpdate(params.mDeltaTime);
        }

        test.Cleanup();
        physicsSystem.forgetMe();
        System.gc();
    }

    /**
     * Smoke test all the packages.
     */
    private static void smokeTestAll() {
    }
}
