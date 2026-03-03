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

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import com.github.stephengold.joltjni.enumerate.EPhysicsUpdateError;
import com.github.stephengold.joltjni.std.OfStream;

import java.io.File;
import java.nio.ByteBuffer;
import testjoltjni.TestUtils;
import testjoltjni.app.samples.BPLayerInterfaceImpl;
import testjoltjni.app.samples.ObjectLayerPairFilterImpl;
import testjoltjni.app.samples.ObjectVsBroadPhaseLayerFilterImpl;
import testjoltjni.app.samples.PreUpdateParams;
import testjoltjni.app.samples.SamplesContactListener;
import testjoltjni.app.samples.Test;
import testjoltjni.app.samples.broadphase.*;
import testjoltjni.app.samples.character.*;
import testjoltjni.app.samples.constraints.*;
import testjoltjni.app.samples.convexcollision.*;
import testjoltjni.app.samples.general.*;
import testjoltjni.app.samples.hair.*;
import testjoltjni.app.samples.rig.*;
import testjoltjni.app.samples.scaledshapes.*;
import testjoltjni.app.samples.shapes.*;
import testjoltjni.app.samples.softbody.*;
import testjoltjni.app.samples.tools.*;
import testjoltjni.app.samples.vehicle.*;
import testjoltjni.app.samples.water.*;

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
     * runtime environment of this app
     */
    private static Context context;
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
     * @param c the app's runtime environment (not {@code null})
     * @param view for displaying progress (not {@code null})
     */
    static void run(Context c, TextView view) {
        context = c;
        textView = view;

        System.loadLibrary("joltjni");
        TestUtils.initializeNativeLibrary();

        println(Jolt.getConfigurationString());
        printf(" built-in compute systems:%s%s%s%s%n",
                Jolt.implementsComputeCpu() ? " CPU" : "",
                Jolt.implementsComputeDx12() ? " DX12" : "",
                Jolt.implementsComputeMtl() ? " MTL" : "",
                Jolt.implementsComputeVk() ? " VK" : ""
        );

        createSharedObjects();

        try {
            smokeTestAll();
        } catch (Exception e) {
            Log.wtf("jolt-jni", e);
        }
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
        File dir = context.getExternalFilesDir(null);
        File file = new File(dir, "SmokeTestAll.jor");
        String fileName = file.getAbsolutePath();
        Log.i("jolt-jni", "DebugRenderer will record to " + fileName);
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
        String lineSeparator = System.lineSeparator();
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
        // broadphase package:
        smokeTest(new BroadPhaseCastRayTest());
        smokeTest(new BroadPhaseInsertionTest());

        // character package:
        smokeTest(new CharacterPlanetTest());
        smokeTest(new CharacterSpaceShipTest());
        smokeTest(new CharacterTest());
        smokeTest(new CharacterVirtualTest());

        // constraints package:
        smokeTestConstraints();

        // convex-collision package:
        smokeTest(new CapsuleVsBoxTest());
        smokeTest(new ClosestPointTest());
        smokeTest(new ConvexHullShrinkTest());
        smokeTest(new ConvexHullTest());
        smokeTest(new EPATest());
        smokeTest(new InteractivePairsTest());
        // TODO RandomRayTest (uses templates)

        smokeTestGeneral();

        // hair package:
        smokeTest(new HairCollisionTest());
        smokeTest(new HairGravityPreloadTest());
        smokeTest(new HairTest());

        smokeTestRig();
        smokeTestScaledShapes();
        smokeTestShapes();
        smokeTestSoftBody();

        // tools package:
        smokeTest(new LoadSnapshotTest());

        // vehicle package:
        smokeTest(new MotorcycleTest());
        smokeTest(new TankTest());
        smokeTest(new VehicleConstraintTest());
        smokeTest(new VehicleSixDOFTest());
        smokeTest(new VehicleStressTest());

        // water package:
        smokeTest(new BoatTest());
        smokeTest(new WaterShapeTest());
    }

    /**
     * Smoke test the "constraints" package.
     */
    private static void smokeTestConstraints() {
        smokeTest(new ConeConstraintTest());
        smokeTest(new ConstraintPriorityTest());
        smokeTest(new ConstraintSingularityTest());
        smokeTest(new ConstraintVsCOMChangeTest());
        smokeTest(new DistanceConstraintTest());
        smokeTest(new FixedConstraintTest());
        smokeTest(new GearConstraintTest());
        smokeTest(new HingeConstraintTest());
        smokeTest(new PathConstraintTest());
        smokeTest(new PointConstraintTest());
        smokeTest(new PoweredHingeConstraintTest());
        smokeTest(new PoweredSliderConstraintTest());
        smokeTest(new PoweredSwingTwistConstraintTest());
        smokeTest(new PulleyConstraintTest());
        smokeTest(new RackAndPinionConstraintTest());
        smokeTest(new SixDOFConstraintTest());
        smokeTest(new SliderConstraintTest());
        smokeTest(new SpringTest());
        smokeTest(new SwingTwistConstraintFrictionTest());
        smokeTest(new SwingTwistConstraintTest());
    }

    /**
     * Smoke test the "general" package.
     */
    private static void smokeTestGeneral() {
        smokeTest(new ActivateDuringUpdateTest());
        smokeTest(new ActiveEdgesTest());
        smokeTest(new AllowedDOFsTest());
        smokeTest(new BigVsSmallTest());
        smokeTest(new CenterOfMassTest());
        smokeTest(new ChangeMotionQualityTest());
        smokeTest(new ChangeMotionTypeTest());
        smokeTest(new ChangeObjectLayerTest());
        smokeTest(new ChangeShapeTest());
        smokeTest(new ContactListenerTest());
        smokeTest(new ContactManifoldTest());
        smokeTest(new ConveyorBeltTest());
        smokeTest(new DampingTest());
        smokeTest(new DynamicMeshTest());
        smokeTest(new EnhancedInternalEdgeRemovalTest());
        smokeTest(new FrictionPerTriangleTest());
        smokeTest(new FrictionTest());
        smokeTest(new FunnelTest());
        smokeTest(new GravityFactorTest());
        smokeTest(new GyroscopicForceTest());
        smokeTest(new HeavyOnLightTest());
        smokeTest(new HighSpeedTest());
        smokeTest(new IslandTest());
        smokeTest(new KinematicTest());
        smokeTest(new LoadSaveBinaryTest());
        smokeTest(new LoadSaveSceneTest());
        smokeTest(new ManifoldReductionTest());
        smokeTest(new ModifyMassTest());
        // TODO MultithreadedTest
        smokeTest(new PyramidTest());
        smokeTest(new RestitutionTest());
        smokeTest(new SensorTest());
        smokeTest(new ShapeFilterTest());
        // TODO SimCollideBodyVsBodyTest (uses templates)
        // TODO SimShapeFilterTest
        smokeTest(new SimpleTest());
        smokeTest(new StackTest());
        smokeTest(new TwoDFunnelTest());
        smokeTest(new WallTest());
    }

    /**
     * Smoke test the "rig" package.
     */
    private static void smokeTestRig() {
        smokeTest(new BigWorldTest());
        smokeTest(new CreateRigTest());
        smokeTest(new KinematicRigTest());
        smokeTest(new LoadRigTest());
        smokeTest(new LoadSaveBinaryRigTest());
        smokeTest(new LoadSaveRigTest());
        smokeTest(new PoweredRigTest());
        smokeTest(new RigPileTest());
        smokeTest(new SkeletonMapperTest());
        smokeTest(new SoftKeyframedRigTest());
    }

    /**
     * Smoke test the "scaledshapes" package.
     */
    private static void smokeTestScaledShapes() {
        smokeTest(new DynamicScaledShape());
        smokeTest(new ScaledBoxShapeTest());
        smokeTest(new ScaledCapsuleShapeTest());
        smokeTest(new ScaledConvexHullShapeTest());
        smokeTest(new ScaledCylinderShapeTest());
        smokeTest(new ScaledHeightFieldShapeTest());
        smokeTest(new ScaledMeshShapeTest());
        smokeTest(new ScaledMutableCompoundShapeTest());
        smokeTest(new ScaledOffsetCenterOfMassShapeTest());
        smokeTest(new ScaledPlaneShapeTest());
        smokeTest(new ScaledSphereShapeTest());
        smokeTest(new ScaledStaticCompoundShapeTest());
        smokeTest(new ScaledTaperedCapsuleShapeTest());
        smokeTest(new ScaledTaperedCylinderShapeTest());
        smokeTest(new ScaledTriangleShapeTest());
    }

    /**
     * Smoke test the "shapes" package.
     */
    private static void smokeTestShapes() {
        smokeTest(new BoxShapeTest());
        smokeTest(new CapsuleShapeTest());
        smokeTest(new ConvexHullShapeTest());
        smokeTest(new CylinderShapeTest());
        smokeTest(new DeformedHeightFieldShapeTest());
        smokeTest(new EmptyShapeTest());
        smokeTest(new HeightFieldShapeTest());
        smokeTest(new MeshShapeTest());
        smokeTest(new MeshShapeUserDataTest());
        smokeTest(new MutableCompoundShapeTest());
        smokeTest(new OffsetCenterOfMassShapeTest());
        smokeTest(new PlaneShapeTest());
        smokeTest(new RotatedTranslatedShapeTest());
        smokeTest(new SphereShapeTest());
        smokeTest(new StaticCompoundShapeTest());
        smokeTest(new TaperedCapsuleShapeTest());
        smokeTest(new TaperedCylinderShapeTest());
        smokeTest(new TriangleShapeTest());
    }

    /**
     * Smoke test the "softbody" package.
     */
    private static void smokeTestSoftBody() {
        smokeTest(new SoftBodyBendConstraintTest());
        smokeTest(new SoftBodyContactListenerTest());
        smokeTest(new SoftBodyCosseratRodConstraintTest());
        smokeTest(new SoftBodyCustomUpdateTest());
        smokeTest(new SoftBodyForceTest());
        smokeTest(new SoftBodyFrictionTest());
        smokeTest(new SoftBodyGravityFactorTest());
        smokeTest(new SoftBodyKinematicTest());
        smokeTest(new SoftBodyLRAConstraintTest());
        smokeTest(new SoftBodyPressureTest());
        smokeTest(new SoftBodyRestitutionTest());
        smokeTest(new SoftBodySensorTest());
        smokeTest(new SoftBodyShapesTest());
        smokeTest(new SoftBodySkinnedConstraintTest());
        smokeTest(new SoftBodyStressTest());
        smokeTest(new SoftBodyUpdatePositionTest());
        smokeTest(new SoftBodyVertexRadiusTest());
        smokeTest(new SoftBodyVsFastMovingTest());
    }
}
