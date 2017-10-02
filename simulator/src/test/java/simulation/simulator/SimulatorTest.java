package simulation.simulator;

import commons.simulation.PhysicalObject;
import commons.simulation.PhysicalObjectType;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.*;
import commons.simulation.SimulationLoopExecutable;
import commons.simulation.SimulationLoopNotifiable;
import simulation.environment.pedestrians.Pedestrian;
import simulation.environment.object.Tree;
import simulation.util.*;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.PhysicalVehicleBuilder;

import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * JUnit Test-suite for simulation logic and object management
 */
public class SimulatorTest {

    @BeforeClass
    public static void setUpClass() {
        Log.setLogEnabled(false);
    }

    @AfterClass
    public static void tearDownClass() {
        Log.setLogEnabled(true);
    }

    @Before
    public void setUp() {
        Simulator.resetSimulator();

        //Set update frequency to 30 loop iterations per second
        Simulator sim = Simulator.getSharedInstance();
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_FIXED_TIME);
        sim.setSimulationLoopFrequency(30);
    }

    /**
     * Prevent simulation with invalid settings
     */
    @Test
    public void startSimulation() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSimulationLoopFrequency(0);

        //Try to run simulation
        sim.stopAfter(500);
        sim.startSimulation();

        //Simulation did not start
        assertTrue(sim.getFrameCount() == 0);

        //Set invalid parameters synchronousSimulation & SIMULATION_TYPE_REAL_TIME
        sim.setSynchronousSimulation(true);
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_REAL_TIME);

        //Try to run simulation
        sim.startSimulation();

        //Simulation did not start
        assertTrue(sim.getFrameCount() == 0);

        //Not allow to slow down simulation that is not SIMULATION_TYPE_FIXED_TIME
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_MAX_FPS);
        sim.slowDownComputation(2);

        //Try to run simulation
        sim.startSimulation();

        //Simulation did not start
        assertTrue(sim.getFrameCount() == 0);

        sim.setSimulationType(SimulationType.SIMULATION_TYPE_REAL_TIME);
        sim.slowDownComputation(2);

        //Try to run simulation
        sim.startSimulation();

        //Simulation did not start
        assertTrue(sim.getFrameCount() == 0);
    }

    /**
     * Unregistering simulation objects should stop calling the methods of the interface
     */
    @Test
    public void unregisteringObjects() {
        Simulator sim = Simulator.getSharedInstance();

        //Add notifiable objects
        NotificationCounter notNotified = new NotificationCounter();
        NotificationCounter alwaysNotified = new NotificationCounter();
        NotificationCounter firstSecsNotified = new NotificationCounter();
        sim.registerLoopObserver(notNotified);
        sim.registerLoopObserver(alwaysNotified);
        sim.unregisterLoopObserver(notNotified);
        sim.registerLoopObserver(firstSecsNotified);

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);

        //Run simulation
        sim.startSimulation();

        //Get loop iterations after 1 sec and unregister notifiable
        sim.waitForTime(1000);
        sim.unregisterLoopObserver(firstSecsNotified);
        long firstIterationsCountDid = firstSecsNotified.didExecCounter;
        long firstIterationsCountWill = firstSecsNotified.willExecCounter;

        sim.waitUntilSimulationFinished();
        long iterationCount = sim.getFrameCount();


        assertTrue(0 == notNotified.didExecCounter);
        assertTrue(0 == notNotified.willExecCounter);
        assertTrue(firstIterationsCountDid == firstSecsNotified.didExecCounter);
        assertTrue(firstIterationsCountWill
                == firstSecsNotified.willExecCounter);
        assertTrue(iterationCount == alwaysNotified.didExecCounter);
        assertTrue(iterationCount == alwaysNotified.willExecCounter);
    }

    /**
     * Unregistering simulation objects should stop calling the methods of the interface
     */
    @Test
    public void unregisteringPhysicalObjects() {
        Simulator sim = Simulator.getSharedInstance();

        //Create dummy objects
        SimObject a = new SimObject();
        SimObject b = new SimObject();

        //(Un)registering physical objects
        sim.registerPhysicalObject(a);
        assertTrue(sim.getPhysicalObjects().contains(a));
        assertTrue(!sim.getPhysicalObjects().contains(b));
        sim.registerPhysicalObject(b);
        assertTrue(sim.getPhysicalObjects().contains(a));
        assertTrue(sim.getPhysicalObjects().contains(b));

        sim.unregisterPhysicalObject(a);
        assertTrue(!sim.getPhysicalObjects().contains(a));
        assertTrue(sim.getPhysicalObjects().contains(b));
        sim.unregisterPhysicalObject(b);
        assertTrue(!sim.getPhysicalObjects().contains(a));
        assertTrue(!sim.getPhysicalObjects().contains(b));

        //Unregistering physical objects that are registered as simulation objects is illegal
        sim.registerSimulationObject(a);
        sim.unregisterPhysicalObject(a);
        assertTrue(sim.getPhysicalObjects().contains(a));

        //Unregistering simulation objects also removes physical object
        sim.unregisterSimulationObject(a);
        assertTrue(!sim.getPhysicalObjects().contains(a));
    }

    /**
     * Returning collided objects returns collided objects only
     */
    @Test
    public void returnCollidedObjects() {
        Simulator sim = Simulator.getSharedInstance();
        SimObject a = new SimObject();
        SimObject b = new SimObject();
        SimObject c = new SimObject();

        sim.registerPhysicalObject(a);
        sim.registerPhysicalObject(b);
        sim.registerPhysicalObject(c);

        //No collided objects -> empty list
        List<PhysicalObject> collided = sim.getCollidedObjects();
        assertTrue(collided.size() == 0);

        //Collision detected -> return collided objects
        a.setCollision(true);
        collided = sim.getCollidedObjects();
        assertTrue(collided.contains(a) && !collided.contains(b) && !collided.contains(c));
        b.setCollision(true);
        collided = sim.getCollidedObjects();
        assertTrue(collided.contains(a) && collided.contains(b) && !collided.contains(c));

        //Collision resolved -> Not returned anymore
        a.setCollision(false);
        collided = sim.getCollidedObjects();
        assertTrue(!collided.contains(a) && collided.contains(b) && !collided.contains(c));
        b.setCollision(false);
        collided = sim.getCollidedObjects();
        assertTrue(collided.size() == 0);
    }

    /**
     * Checking collision memory
     */
    @SuppressWarnings("PointlessBooleanExpression")
    @Test
    public void collidedObjectsMemory() {
        Simulator sim = Simulator.getSharedInstance();
        PhysicalVehicleBuilder physicalVehicleBuilder1 = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle a = physicalVehicleBuilder1.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        PhysicalVehicleBuilder physicalVehicleBuilder2 = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle b = physicalVehicleBuilder2.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        PhysicalVehicleBuilder physicalVehicleBuilder3 = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle c = physicalVehicleBuilder3.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

        sim.registerAndPutObject(a, 966.6905532033019, 498.1714592002669, 0.8 * Math.PI);
        sim.registerAndPutObject(b, 982.3084859322336, 425.53842059972903, 0.3 * Math.PI);
        sim.registerAndPutObject(c, 906.8272188834519, 410.8760650003208, 1.75 * Math.PI);

        //No collided objects
        List<PhysicalObject> collided = sim.getCollidedObjects();
        assertTrue(sim.collisionPresent() == false);
        assertTrue(sim.collisionOccurred() == false);

        //Collision detected
        a.setCollision(true);
        sim.stopAfter(100);
        sim.startSimulation();
        sim.waitUntilSimulationFinished();
        assertTrue(sim.collisionPresent() == false);
        assertTrue(sim.collisionOccurred() == false);

        //Reset collision detection
        sim.resetCollisionOccurred();
        assertTrue(sim.collisionPresent() == false);
        assertTrue(sim.collisionOccurred() == false);

        //Extending simulation updates memory
        sim.extendSimulationTime(100);
        sim.startSimulation();
        sim.waitUntilSimulationFinished();
        assertTrue(sim.collisionPresent() == false);
        assertTrue(sim.collisionOccurred() == false);

        //Collision resolved
        a.setCollision(false);
        assertTrue(sim.collisionPresent() == false);
        assertTrue(sim.collisionOccurred() == false);
    }

    /**
     * Returning objects with error returns objects with error only
     */
    @Test
    public void returnErrorObjects() {
        Simulator sim = Simulator.getSharedInstance();
        SimObject a = new SimObject();
        SimObject b = new SimObject();
        SimObject c = new SimObject();

        sim.registerPhysicalObject(a);
        sim.registerPhysicalObject(b);
        sim.registerPhysicalObject(c);

        //No objects with error -> empty list
        List<PhysicalObject> collided = sim.getCollidedObjects();
        assertTrue(collided.size() == 0);

        //error occurred -> return objects with error
        a.setError(true);
        collided = sim.getCollidedObjects();
        assertTrue(collided.contains(a) && !collided.contains(b) && !collided.contains(c));
        b.setError(true);
        collided = sim.getCollidedObjects();
        assertTrue(collided.contains(a) && collided.contains(b) && !collided.contains(c));

        //Error resolved -> Not returned anymore
        a.setError(false);
        collided = sim.getCollidedObjects();
        assertTrue(!collided.contains(a) && collided.contains(b) && !collided.contains(c));
        b.setError(false);
        collided = sim.getCollidedObjects();
        assertTrue(collided.size() == 0);
    }

    /**
     * Checking error memory
     */
    @SuppressWarnings("PointlessBooleanExpression")
    @Test
    public void errorObjectsMemory() {
        Simulator sim = Simulator.getSharedInstance();
        PhysicalVehicleBuilder physicalVehicleBuilder1 = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle a = physicalVehicleBuilder1.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        PhysicalVehicleBuilder physicalVehicleBuilder2 = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle b = physicalVehicleBuilder2.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        PhysicalVehicleBuilder physicalVehicleBuilder3 = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle c = physicalVehicleBuilder3.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

        sim.registerAndPutObject(a, 966.6905532033019, 498.1714592002669, 0.8 * Math.PI);
        sim.registerAndPutObject(b, 982.3084859322336, 425.53842059972903, 0.3 * Math.PI);
        sim.registerAndPutObject(c, 906.8272188834519, 410.8760650003208, 1.75 * Math.PI);

        //No objects with error
        List<PhysicalObject> collided = sim.getCollidedObjects();
        assertTrue(sim.collisionPresent() == false);
        assertTrue(sim.collisionOccurred() == false);

        //Error occurred
        a.setError(true);
        sim.stopAfter(100);
        sim.startSimulation();
        sim.waitUntilSimulationFinished();
        assertTrue(sim.collisionPresent() == true);
        assertTrue(sim.collisionOccurred() == true);

        //Reset collision detection
        sim.resetCollisionOccurred();
        assertTrue(sim.collisionPresent() == true);
        assertTrue(sim.collisionOccurred() == false);

        //Extending simulation updates memory
        sim.extendSimulationTime(100);
        sim.startSimulation();
        sim.waitUntilSimulationFinished();
        assertTrue(sim.collisionPresent() == true);
        assertTrue(sim.collisionOccurred() == true);

        //Error resolved
        a.setError(false);
        assertTrue(sim.collisionPresent() == false);
        assertTrue(sim.collisionOccurred() == true);
    }

    /**
     * Checks that didExecute and willExecute are called for every loop iteration
     */
    @Test
    public void notificationsCalledForEveryLoopIteration() {
        Simulator sim = Simulator.getSharedInstance();

        //Add a notifiable object
        NotificationCounter nc = new NotificationCounter();
        sim.registerLoopObserver(nc);

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);

        //Run simulation
        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        //Function calls should match actual loop iterations
        long frames = sim.getFrameCount();
        assertTrue(frames == nc.didExecCounter);
        assertTrue(frames == nc.willExecCounter);
    }

    /**
     * Checks that executeLoopIteration is called for every loop iteration
     */
    @Test
    public void executable() {
        Simulator sim = Simulator.getSharedInstance();

        //Add a notifiable object
        Executable exec = new Executable();
        sim.registerSimulationObject(exec);

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);

        //Run simulation
        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        //Function calls should match actual loop iterations
        long frames = sim.getFrameCount();
        assertTrue(frames == exec.execCounter);
    }

    /**
     * Slowing down the simulation actually slows down computation
     */
    @Test
    public void slowDownSynchronousComputation() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);

        //Add a notifiable object
        SlowExecutable exec = new SlowExecutable();
        sim.registerSimulationObject(exec);

        //Remember start time of simulation
        long startTime = System.currentTimeMillis();

        sim.startSimulation();

        //Calculate runtime of normal running simulation
        long referenceRuntime = System.currentTimeMillis() - startTime;

        //Second run with slowed down simulation
        sim.slowDownComputation(3);
        startTime = System.currentTimeMillis();
        sim.extendSimulationTime(5000);
        sim.startSimulation();

        //Compare runtimes
        long slowedRuntime = System.currentTimeMillis() - startTime;

        assertTrue(referenceRuntime * 2.25 < slowedRuntime);
        assertTrue(referenceRuntime * 3.75 > slowedRuntime);
    }

    /**
     * Slowing down the simulation actually slows down computation
     */
    @Test
    public void slowDownSynchronousComputationToWallClockTime() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);

        // Set simulation duration
        sim.stopAfter(1000);

        long startTime = System.currentTimeMillis();

        //Run slowed down simulation
        sim.slowDownComputationToWallClockTime(3);
        sim.startSimulation();

        long runtime = System.currentTimeMillis() - startTime;

        //Compare runtime
        assertTrue(runtime >= 2800);

        //Try with different parameters for frequency and slow down factor
        Simulator.resetSimulator();
        sim = Simulator.getSharedInstance();
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_FIXED_TIME);
        sim.setSimulationLoopFrequency(66);
        sim.setSynchronousSimulation(true);

        // Set simulation duration
        sim.stopAfter(800);

        startTime = System.currentTimeMillis();

        //Run slowed down simulation
        sim.slowDownComputationToWallClockTime(5);
        sim.startSimulation();

        runtime = System.currentTimeMillis() - startTime;

        //Compare runtime
        assertTrue(runtime >= 3800);
    }

    /**
     * Slowing down the simulation actually slows down computation
     */
    @Test
    public void slowDownAsynchronousComputation() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(false);

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);

        //Add a notifiable object
        SlowExecutable exec = new SlowExecutable();
        sim.registerSimulationObject(exec);

        //Remember start time of simulation
        long startTime = System.currentTimeMillis();

        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        //Calculate runtime of normal running simulation
        long referenceRuntime = System.currentTimeMillis() - startTime;

        //Second run with slowed down simulation
        sim.slowDownComputation(3);
        startTime = System.currentTimeMillis();
        sim.extendSimulationTime(5000);
        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        //Compare runtimes
        long slowedRuntime = System.currentTimeMillis() - startTime;

        assertTrue(referenceRuntime * 2.25 < slowedRuntime);
        assertTrue(referenceRuntime * 3.75 > slowedRuntime);
    }

    /**
     * Slowing down the simulation actually slows down computation
     */
    @Test
    public void slowDownAsynchronousComputationToWallClockTime() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(false);

        // Set simulation duration
        sim.stopAfter(1000);

        long startTime = System.currentTimeMillis();

        //Run slowed down simulation
        sim.slowDownComputationToWallClockTime(3);
        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        long runtime = System.currentTimeMillis() - startTime;

        //Compare runtime
        assertTrue(runtime >= 2800);

        //Try with different parameters for frequency and slow down factor
        Simulator.resetSimulator();
        sim = Simulator.getSharedInstance();
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_FIXED_TIME);
        sim.setSimulationLoopFrequency(66);
        sim.setSynchronousSimulation(false);

        // Set simulation duration
        sim.stopAfter(800);

        startTime = System.currentTimeMillis();

        //Run slowed down simulation
        sim.slowDownComputationToWallClockTime(5);
        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        runtime = System.currentTimeMillis() - startTime;

        //Compare runtime
        assertTrue(runtime >= 3800);
    }

    /**
     * Test the manual pausing of computations of asynchronous simulation
     */
    @Test
    public void pauseAsyncComputations() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(false);

        // Set simulation duration
        sim.stopAfter(1000);

        //Run paused simulation
        sim.pauseComputationsAfter(500);
        sim.startSimulation();
        sim.waitForTime(500 - sim.getSimulationTime() - 10);

        //Some extra time to enter pausing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(sim.isComputationPaused());
        long simTime = sim.getSimulationTime();

        //We waited the right amount of time + one frame tolerance
        assertTrue(simTime > 500 && simTime <= 533);

        //We're not advancing in simulation time
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(sim.isComputationPaused());
        assertTrue(sim.getSimulationTime() == simTime);

        //Run to finish
        sim.continueComputations();
    }

    /**
     * Test the manual pausing of computations of synchronous simulation
     */
    @Test
    public void pauseSyncComputations() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);

        // Set simulation duration
        sim.stopAfter(1000);

        //Run paused simulation
        sim.pauseComputationsAfter(500);

        //The simulator needs to be in an extra thread because otherwise we would block ourselves forever
        Runnable thread = () -> sim.startSimulation();
        new Thread(thread).start();

        //After 1 second we should have entered pausing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(sim.isComputationPaused());
        long simTime = sim.getSimulationTime();

        //We waited the right amount of time + one frame tolerance
        assertTrue(simTime > 500 && simTime <= 533);

        //We're not advancing in simulation time
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(sim.isComputationPaused());
        assertTrue(sim.getSimulationTime() == simTime);

        //Run to finish
        sim.continueComputations();
    }

    /**
     * The simulation frequency may be set to positive, but not to negative values.
     * Trying to set a negative frequency should not change the current frequency.
     */
    @Test
    public void simulationFrequencySaneValues () {
        //Sane example
        Simulator sim = Simulator.getSharedInstance();
        sim.setSimulationLoopFrequency(20);
        assertTrue(sim.getSimulationLoopFrequency() == 20);

        //Illegal examples should not change frequency
        sim.setSimulationLoopFrequency(-1);
        assertTrue(sim.getSimulationLoopFrequency() == 20);
    }

    /**
     * The desired simulation time should be (more or less) met.
     */
    @Test
    public void stopTime () {
        Simulator sim = Simulator.getSharedInstance();

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);

        //Start simulation
        sim.startSimulation();

        //Wait till simulation is over
        sim.waitUntilSimulationFinished();

        //Stop time should be equal to desired runtime. Tolerance: 1 loop iteration.
        Long stopTime = sim.getSimulationTime();
        assertTrue(stopTime <= 5030 && stopTime >= 4970);
    }

    /**
     * A simulation that should stop after 0 ms should stop immediately
     */
    @Test
    public void immediatelyStopSimulation() {
        Simulator sim = Simulator.getSharedInstance();

        //Immediately stopping simulation
        sim.stopAfter(0);
        sim.startSimulation();

        //Wait till simulation is over
        sim.waitUntilSimulationFinished();

        //Test no simulation time has passed
        Long stopTime = sim.getSimulationTime();
        assertTrue(stopTime == 0);
    }

    /**
     * Continuing a simulation should increase simulation time
     */
    @Test
    public void continueSimulationIncreasesSimTime() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);

        //Start a simulation
        sim.stopAfter(1000);
        sim.startSimulation();

        //Get simulated time
        Long time = sim.getSimulationTime();

        //Continue for the same amount of time as before
        sim.stopAfter(2000);
        sim.startSimulation();
        assertTrue(time * 2 == sim.getSimulationTime());
    }

    /**
     * Running the same simulation twice should lead to same amount of simulated time and frames
     */
    @Test
    public void synchronousSimulationIsDeterministic() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);
        sim.setSimulationLoopFrequency(33);

        //Start a simulation
        sim.stopAfter(1000);
        sim.startSimulation();

        //Get simulated time
        long timeRun1 = sim.getSimulationTime();
        long framesRun1 = sim.getFrameCount();

        //Reset simulator
        Simulator.resetSimulator();
        sim.setSynchronousSimulation(true);
        sim.setSimulationLoopFrequency(33);

        //Start another run
        sim.stopAfter(1000);
        sim.startSimulation();

        //Get simulated time
        long timeRun2 = sim.getSimulationTime();
        long framesRun2 = sim.getFrameCount();

        //Compare to first run
        assertTrue(timeRun1 == timeRun2);
        assertTrue(framesRun1 == framesRun2);
    }

    /**
     * Test that extendSimulationTime() actually increases simulation time
     */
    @Test
    public void extendSimulationTime() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);

        //Start a simulation
        sim.stopAfter(1000);
        sim.startSimulation();

        //Get simulated time
        Long time = sim.getSimulationTime();

        //Zero extension
        sim.extendSimulationTime(0);
        sim.startSimulation();
        assertTrue(time.longValue() == sim.getSimulationTime());


        //Invalid extension
        sim.extendSimulationTime(-5);
        sim.startSimulation();
        assertTrue(time.longValue() == sim.getSimulationTime());


        //Continue for the same amount of time as before
        sim.extendSimulationTime(1000);
        sim.startSimulation();
        assertTrue(time * 2 == sim.getSimulationTime());
    }

    /**
     * Test the simulated daytime and daytime speedup
     */
    @Test
    public void daytimeSimulation() {
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);

        // Set daytime, speedup: 1 second = 1 day
        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        Date startTime = cal.getTime();
        sim.setStartDaytime(startTime, 86400);

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);
        sim.startSimulation();

        // Test if 5 days passed. 50 minutes tolerance (~1 frame)
        cal.add(Calendar.DAY_OF_MONTH, 5);
        cal.add(Calendar.MINUTE, -50);
        Date before = cal.getTime();
        cal.add(Calendar.MINUTE, 100);
        Date after = cal.getTime();

        assertTrue(after.after(sim.getDaytime()));
        assertTrue(before.before(sim.getDaytime()));

        // Extend by another 10 seconds
        sim.extendSimulationTime(10000);
        sim.setStartDaytime(startTime, 3600);
        sim.startSimulation();

        //Test if 10 hours passed. 2 minutes tolerance (~1 frame)
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        cal.add(Calendar.HOUR_OF_DAY, 10);
        cal.add(Calendar.MINUTE, -2);
        before = cal.getTime();
        cal.add(Calendar.MINUTE, 4);
        after = cal.getTime();

        assertTrue(after.after(sim.getDaytime()));
        assertTrue(before.before(sim.getDaytime()));
    }

    /**
     * Test that registerAndPutObject() puts cars at desired location and rotation
     */
    @Test
    public void registerAndPutPhysicalVehicle() {
        Simulator sim = Simulator.getSharedInstance();

        //Create car
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

        //Register and put object
        sim.registerAndPutObject(physicalVehicle, 10, 20, 0);

        //Test object is at desired location
        RealVector pos = physicalVehicle.getPos();
        assertTrue(pos.getEntry(0) == 10);
        assertTrue(pos.getEntry(1) == 20);

        //Test object is correctly rotated
        RealMatrix rotation = physicalVehicle.getGeometryRot();
        RealVector yAxis = new ArrayRealVector(new double[] {0.0, 0.0, 1.0});
        RealVector v = rotation.operate(yAxis);
        double cos = v.cosine(yAxis);
        double angle = Math.acos(cos);
        assertTrue(angle == 0);

        //Test object is registered
        assertTrue(sim.getPhysicalObjects().contains(physicalVehicle));
    }

    /**
     * Test that registerAndPutObject() puts pedestrians at desired location and rotation
     */
    @Ignore
    @Test
    public void registerAndPutPedestrian() {
        Simulator sim = Simulator.getSharedInstance();

        //Create pedestrian
        Pedestrian pedestrian = new Pedestrian(null);

        //Register and put object
        sim.registerAndPutObject(pedestrian, 10, 20, 0);

        //Test object is at desired location
        RealVector pos = pedestrian.getGeometryPos();
        assertTrue(pos.getEntry(0) == 10);
        assertTrue(pos.getEntry(1) == 20);

        //Test object is correctly rotated
        RealMatrix rotation = pedestrian.getGeometryRot();
        RealVector yAxis = new ArrayRealVector(new double[] {0.0, 0.0, 1.0});
        RealVector v = rotation.operate(yAxis);
        double cos = v.cosine(yAxis);
        double angle = Math.acos(cos);
        assertTrue(angle == 0);

        //Test object is registered
        assertTrue(sim.getPhysicalObjects().contains(pedestrian));
    }

    /**
     * Test that registerAndPutObject() puts trees at desired location and rotation
     */
    @Test
    public void registerAndPutTree() {
        Simulator sim = Simulator.getSharedInstance();

        //Create car
        Tree tree = new Tree();

        //Register and put object
        sim.registerAndPutObject(tree, 10, 20, 0);

        //Test object is at desired location
        RealVector pos = tree.getGeometryPos();
        assertTrue(pos.getEntry(0) == 10);
        assertTrue(pos.getEntry(1) == 20);

        //Test object is correctly rotated
        RealMatrix rotation = tree.getGeometryRot();
        RealVector yAxis = new ArrayRealVector(new double[] {0.0, 0.0, 1.0});
        RealVector v = rotation.operate(yAxis);
        double cos = v.cosine(yAxis);
        double angle = Math.acos(cos);
        assertTrue(angle == 0);

        //Test object is registered
        assertTrue(sim.getPhysicalObjects().contains(tree));
    }

    /**
     * Ensures getters provide right results
     */
    @Test
    public void getters() {
        Simulator sim = Simulator.getSharedInstance();

        //Synchronous simulation
        sim.setSynchronousSimulation(true);
        assertTrue(sim.isSynchronousSimulation());
        sim.setSynchronousSimulation(false);
        assertTrue(!sim.isSynchronousSimulation());

        //Paused in future
        sim.setPausedInFuture(true);
        assertTrue(sim.isPausedInFuture());
        sim.setPausedInFuture(false);
        assertTrue(!sim.isPausedInFuture());
    }

    /**
     * Fixed time simulation has fixed time
     */
    @Test
    public void fixedTimeIntervals() {
        //Simulator setup
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(true);
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_FIXED_TIME);
        sim.setSimulationLoopFrequency(30);
        sim.stopAfter(5000);

        //Set time that should be between each two frames
        long timeBetweenIterations = (long) ((1.0 / 30) * 1000);
        TimeChecker checker = new TimeChecker();
        checker.referenceTime = timeBetweenIterations;

        //Test that time is between each two iterations
        sim.registerLoopObserver(checker);
        sim.startSimulation();
    }

    /**
     * Real time simulation actually takes real time
     */
    @Test
    public void realTimeSimTakesRealTime() {
        //Simulator setup
        Simulator sim = Simulator.getSharedInstance();
        sim.setSynchronousSimulation(false);
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_REAL_TIME);
        sim.setSimulationLoopFrequency(30);
        sim.stopAfter(2000);

        long millisBefore = System.currentTimeMillis();

        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        long runtime = System.currentTimeMillis() - millisBefore;

        //Runtime should be within 10% deviance
        //assertTrue(runtime - 2000 < 200);
        assertTrue(runtime >= 2000 - 200);
        assertTrue(sim.getSimulationTime() > 1800);
    }

    private class NotificationCounter implements SimulationLoopNotifiable {
        public long willExecCounter = 0;
        public long didExecCounter = 0;

        public void simulationStarted(List<SimulationLoopExecutable> simulationObjects) {}
        public void simulationStopped(List<SimulationLoopExecutable> simulationObjects, long totalTime) {}
        public void willExecuteLoopForObject(SimulationLoopExecutable simulationObject, long totalTime, long deltaTime) {}
        public void didExecuteLoopForObject(SimulationLoopExecutable simulationObject, long totalTime, long deltaTime) {}

        public void willExecuteLoop(List<SimulationLoopExecutable> simulationObjects, long totalTime, long deltaTime) {
            willExecCounter++;
        }

        public void didExecuteLoop(List<SimulationLoopExecutable> simulationObjects, long totalTime, long deltaTime) {
            didExecCounter++;
        }
    }

    private class TimeChecker implements SimulationLoopNotifiable {
        public long referenceTime = 0;
        public void simulationStarted(List<SimulationLoopExecutable> simulationObjects) {}
        public void simulationStopped(List<SimulationLoopExecutable> simulationObjects, long totalTime) {}
        public void willExecuteLoopForObject(SimulationLoopExecutable simulationObject, long totalTime, long deltaTime) {}
        public void didExecuteLoopForObject(SimulationLoopExecutable simulationObject, long totalTime, long deltaTime) {}
        public void willExecuteLoop(List<SimulationLoopExecutable> simulationObjects, long totalTime, long deltaTime) {}
        public void didExecuteLoop(List<SimulationLoopExecutable> simulationObjects, long totalTime, long deltaTime) {
            assertTrue(Simulator.getSharedInstance().getTimeBetweenLastIterations() == referenceTime);
        }
    }

    private class Executable implements SimulationLoopExecutable {
        public long execCounter = 0;

        public void executeLoopIteration(long timeDiffMs) {
            execCounter++;
        }
    }

    private class SlowExecutable implements SimulationLoopExecutable {
        public long execCounter = 0;

        public void executeLoopIteration(long timeDiffMs) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            execCounter++;
        }
    }

    private class SimObject implements SimulationLoopExecutable, PhysicalObject {
        boolean collision = false;
        boolean error = false;
        public void executeLoopIteration(long timeDiffMs) {}
        public PhysicalObjectType getPhysicalObjectType() {return PhysicalObjectType.PHYSICAL_OBJECT_TYPE_TREE;}
        public RealVector getGeometryPos() {return new ArrayRealVector(new double[]{0.0, 0.0, 0.0});}
        public RealMatrix getGeometryRot() {return MatrixUtils.createRealIdentityMatrix(3);}
        public double getWidth() {return 0;}
        public double getLength() {return 0;}
        public double getHeight() {return 0;}
        public double getOffsetZ() {return 0;}
        public boolean getCollision() {return collision;}
        public void setCollision(boolean collision) {this.collision = collision;}
        public boolean getError() {return error;}
        public void setError(boolean error) {this.error = error;}
        public List<Map.Entry<RealVector, RealVector>> getBoundaryVectors() {return new ArrayList<>();}
        public long getId() {return 0;}
        public RealVector getAcceleration() {return new ArrayRealVector(new double[] {0.0, 0.0, 0.0});}
        public RealVector getVelocity() {return new ArrayRealVector(new double[] {0.0, 0.0, 0.0});}
        public double getSteeringAngle() {return 0;}
        public RealVector getBackLeftWheelGeometryPos() {return null;}
        public RealVector getBackRightWheelGeometryPos() {return null;}
        public RealVector getFrontLeftWheelGeometryPos() {return null;}
        public RealVector getFrontRightWheelGeometryPos() {return null;}


    }
}