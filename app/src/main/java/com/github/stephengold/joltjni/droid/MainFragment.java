/*
 Copyright (c) 2025 Stephen Gold

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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.github.stephengold.joltjni.droid.databinding.FragmentMainBinding;
import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable;
import com.github.stephengold.joltjni.JobSystem;
import com.github.stephengold.joltjni.JobSystemThreadPool;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.JoltPhysicsObject;
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilter;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Plane;
import com.github.stephengold.joltjni.PlaneShape;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.TempAllocator;
import com.github.stephengold.joltjni.TempAllocatorMalloc;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EPhysicsUpdateError;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;

public class MainFragment extends Fragment {
    // *************************************************************************
    // constants

    /**
     * number of object layers
     */
    final private static int numObjLayers = 2;
    /**
     * object layer for moving objects
     */
    final private static int objLayerMoving = 0;
    /**
     * object layer for non-moving objects
     */
    final private static int objLayerNonMoving = 1;
    // *************************************************************************
    // fields

    /**
     * falling rigid body
     */
    private static ConstBody ball;
    /**
     * system to simulate
     */
    private static PhysicsSystem physicsSystem;
    /**
     * binding to the UI fragment
     */
    private FragmentMainBinding binding;
    // *************************************************************************
    // Fragment methods

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        this.binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        System.loadLibrary("joltjni");

        //Jolt.setTraceAllocations(true); // to log Jolt-JNI heap allocations
        JoltPhysicsObject.startCleaner(); // to reclaim native memory
        Jolt.registerDefaultAllocator(); // tell Jolt Physics to use malloc/free
        Jolt.installDefaultAssertCallback();
        Jolt.installDefaultTraceCallback();
        boolean success = Jolt.newFactory();
        assert success;
        Jolt.registerTypes();

        physicsSystem = createSystem();
        populateSystem();
        physicsSystem.optimizeBroadPhase();

        TempAllocator tempAllocator = new TempAllocatorMalloc();
        int numWorkerThreads = Runtime.getRuntime().availableProcessors();
        JobSystem jobSystem = new JobSystemThreadPool(Jolt.cMaxPhysicsJobs,
                Jolt.cMaxPhysicsBarriers, numWorkerThreads);

        float timePerStep = 0.02f; // in seconds
        for (int iteration = 0; iteration < 50; ++iteration) {
            int collisionSteps = 1;
            int errors = physicsSystem.update(
                    timePerStep, collisionSteps, tempAllocator, jobSystem);
            assert errors == EPhysicsUpdateError.None : errors;

            RVec3Arg location = ball.getPosition();
            String line = String.format("%s%n", location);
            binding.textview.append(line);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.binding = null;
    }
    // *************************************************************************
    // private methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    private static PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        int numBpLayers = 1;

        ObjectLayerPairFilterTable ovoFilter
                = new ObjectLayerPairFilterTable(numObjLayers);
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(objLayerMoving, objLayerMoving);
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving);
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving);

        // Map both object layers to broadphase layer 0:
        BroadPhaseLayerInterfaceTable layerMap
                = new BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers);
        layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0);
        layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0);
        /*
         * Pre-compute the rules for colliding object layers
         * with broadphase layers:
         */
        ObjectVsBroadPhaseLayerFilter ovbFilter
                = new ObjectVsBroadPhaseLayerFilterTable(
                        layerMap, numBpLayers, ovoFilter, numObjLayers);

        PhysicsSystem result = new PhysicsSystem();

        // Set high limits, even though this sample app uses only 2 bodies:
        int maxBodies = 5_000;
        int numBodyMutexes = 0; // 0 means "use the default number"
        int maxBodyPairs = 65_536;
        int maxContacts = 20_480;
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
                layerMap, ovbFilter, ovoFilter);

        return result;
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    private static void populateSystem() {
        BodyInterface bi = physicsSystem.getBodyInterface();

        // Add a static horizontal plane at y=-1:
        float groundY = -1f;
        Vec3Arg normal = Vec3.sAxisY();
        ConstPlane plane = new Plane(normal, -groundY);
        ConstShape floorShape = new PlaneShape(plane);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setShape(floorShape);
        Body floor = bi.createBody(bcs);
        bi.addBody(floor, EActivation.DontActivate);

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        float ballRadius = 0.3f;
        ConstShape ballShape = new SphereShape(ballRadius);
        bcs.setMotionType(EMotionType.Dynamic);
        bcs.setObjectLayer(objLayerMoving);
        bcs.setShape(ballShape);
        ball = bi.createBody(bcs);
        bi.addBody(ball, EActivation.Activate);
    }
}
