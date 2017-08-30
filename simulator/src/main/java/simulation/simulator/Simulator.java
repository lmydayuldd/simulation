package simulation.simulator;

import commons.simulation.IdGenerator;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import simulation.environment.WorldModel;
import simulation.environment.pedestrians.Pedestrian;
import simulation.environment.object.Tree;
import simulation.network.NetworkCellBaseStation;
import simulation.util.*;
import commons.simulation.SimulationLoopNotifiable;
import commons.simulation.SimulationLoopExecutable;
import simulation.vehicle.*;
import commons.simulation.PhysicalObject;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static simulation.vehicle.MassPointType.*;

/**
 * Logic and object management of the simulation
 */
@SuppressWarnings("unused")
public class Simulator {

    /** Update frequency of the simulation loop */
    private int simulationLoopFrequency;

    /** Shared instance of the simulator */
    private static Simulator sharedInstance = new Simulator(0);

    /** Simulation time with millisecond precision */
    private long simulationTime = 0;

    /** Simulation time when the loop was last executed */
    private long lastLoopTime = 0;

    /** Time between last two iteration of the loop */
    private long timeBetweenLastIterations = 0;

    /** Service calling the simulation loop */
    private Timer timer = null;

    /** System time at which simulation started */
    private long simulationStartTime = 0;

    /** Time (in ms) after which to stop simulation. Default: Infinite */
    private long stopSimulationTime = Long.MAX_VALUE;

    /** Simulation time at which the simulation computation will be paused. Default: Long.MAX_VALUE, i.e. never */
    private final AtomicLong computationPauseTime = new AtomicLong(Long.MAX_VALUE);

    /**
     * Type of the simulation as described in SimulationType class. By default use real-time simulation
     *
     * @see SimulationType
     */
    private SimulationType simulationType = SimulationType.SIMULATION_TYPE_REAL_TIME;

    /**
     * Factor by which the simulation is slowed down in fixed_time mode. See slowDownComputation() for
     * more information. Default is 1 and equals no slowing down.
     */
    private int slowDownFactor = 1;

    /**
     * Factor by which the simulation is slowed down in fixed_time mode. See
     * slowDownComputationToWallClockTime() for more information. Default is 1 and equals no
     * slowing down.
     */
    private int slowDownWallClockFactor = 0;

    /** System time at which the last loop iteration started */
    private long lastLoopStartTime = 0;

    /** Simulated daytime */
    private Calendar daytime = Calendar.getInstance();

    /** Simulated daytime at the start of the simulation */
    private Date daytimeStart = null;

    /** Factor by which the simulated day advances faster than the simulation time */
    private int daytimeSpeedup = 1;

    /**
     * Whether or not the simulation should be executed synchronous, i.e.,
     * not in its own thread and therefore blocking
     */
    private boolean synchronousSimulation = false;

    /** Number of simulated frames up to now */
    private long frameCount = 0;

    /** True iff simulation currently running */
    private boolean isRunning = false;

    /** True iff the simulation will be paused or extended in the future */
    private boolean isPausedInFuture;

    /** True iff computation of the simulation is currently paused */
    private boolean isComputationPaused = false;

    /** True if collision occurred, may be reset by used */
    private boolean collisionOccurredDuringExecution = false;

    /** All objects that want to be informed about loop executions */
    private final List<SimulationLoopNotifiable> loopObservers = Collections.synchronizedList(new LinkedList<SimulationLoopNotifiable>());

    /** All objects in the simulation that execute the simulation loop */
    private final List<SimulationLoopExecutable> simulationObjects = Collections.synchronizedList(new LinkedList<SimulationLoopExecutable>());

    /** All simulation objects */
    private final List<PhysicalObject> physicalObjects = Collections.synchronizedList(new LinkedList<PhysicalObject>());

    /** Times for which others wait using the waitForTime() method */
    private final List<Long> waitTimers = Collections.synchronizedList(new LinkedList<Long>());

    /**
     * Resets the shared instance of the simulator.
     * Should only be used for testing purposes.
     */
    public static void resetSimulator() {
        IdGenerator.resetInstance();
        PhysicalVehicleBuilder.resetInstance();
        sharedInstance = new Simulator(0);
    }

    /**
     * Simulator constructor. Should not be called directly but only by the initialization of "sharedInstance".
     *
     * @param simulationLoopFrequency Frequency (in Hz) at which the simulation loop should be called.
     */
    protected Simulator(int simulationLoopFrequency) {
        this.simulationLoopFrequency = simulationLoopFrequency;
        InformationService.getSharedInstance().offerInformation(Information.SIMULATION_TIME, this::getSimulationTime);
        isPausedInFuture = false;
    }

    /**
     * Starts the simulation if possible. If the simulation can not be started, a log message is printed.
     * Use isCurrentlyRunning() to check whether starting the simulation was successful.
     * Use resetSimulator() before starting a new simulation. Otherwise, the old simulation will be
     * continued.
     */
    public void startSimulation() {
        //Check for sane frequency
        if (this.getSimulationLoopFrequency() == 0) {
            Log.severe("You need to set simulation loop update frequency before starting the simulation. Aborting simulation.");
        }

        //Check simulation is not already running
        if (isRunning) {
            Log.warning("Simulation already running.");
            return;
        }

        //Check for sane configuration
        if (synchronousSimulation && simulationType == SimulationType.SIMULATION_TYPE_REAL_TIME) {
            Log.severe("A simulation with type REAL_TIME cannot be executed synchronously. Set synchronousSimulation to false or change simulation type.");
            return;
        }

        //Check is the user tries to slow down a computation that cannot be slowed down
        if (simulationType != SimulationType.SIMULATION_TYPE_FIXED_TIME && slowDownFactor != 1) {
            Log.warning("slowDownComputation() only applies to simulation of type SIMULATION_TYPE_FIXED_TIME. Computation will not be slowed down.");
            return;
        }

        //Check whether we are already done
        if (simulationTime >= stopSimulationTime) {
            return;
        }

        //Reset time
        simulationStartTime = System.currentTimeMillis();

        //Set simulated daytime
        if (daytimeStart != null) {
            daytime.setTime(daytimeStart);
        } else {
            daytime.setTime(new Date());
        }


        //Convert simulation loop frequency to milliseconds between loop calls
        long timeBetweenCalls = (long) ((1.0 / getSimulationLoopFrequency()) * 1000);

        //Set internal state
        isRunning = true;

        // Inform observers about upcoming simulation start
        synchronized (loopObservers) {
            for (SimulationLoopNotifiable observer : loopObservers) {
                observer.simulationStarted(getSimulationObjects());
            }
        }

        if (simulationType == SimulationType.SIMULATION_TYPE_REAL_TIME) {
            //Schedule loop calls
            TimerTask loopIteration = new TimerTask() {
                @Override
                public void run() {
                    Simulator.getSharedInstance().executeSimulationLoop();
                }
            };
            timer = new Timer();
            timer.scheduleAtFixedRate(loopIteration, 0, timeBetweenCalls);
        }

        Log.info("Simulation " + ((frameCount == 0) ? "started." : "continued."));

        if (synchronousSimulation) {
            runSimulation();
        } else {
            new Thread(this::runSimulation).start();
        }
    }
        
    /**
     * Running a simulation. Should only be called in startSimulation()
     */
    private void runSimulation() {
        if (simulationType == SimulationType.SIMULATION_TYPE_FIXED_TIME ||
                simulationType == SimulationType.SIMULATION_TYPE_MAX_FPS) {

            //Execute simulation
            boolean successfulIteration = true;
            while (successfulIteration) {
                successfulIteration = executeSimulationLoop();
            }
        }
    }

    /**
     * Immediately stop the execution of the simulation
     */
    public synchronized void stopSimulation() {
        if (simulationType == SimulationType.SIMULATION_TYPE_REAL_TIME) {
            //Stop calling simulation loop
            timer.cancel();
            timer = null;
        }

        //Set internal state
        isRunning = false;

        // Inform observers about simulation stop if it is not a pause
        if (!isPausedInFuture()) {
            for (SimulationLoopNotifiable observer : this.loopObservers) {
                observer.simulationStopped(getSimulationObjects(), simulationTime);
            }
        }

        //Keep current daytime for future time extensions
        daytimeStart = getDaytime();

        // Count objects with collisions
        int collisionCount = getCollidedObjects().size();

        //Inform user
        Log.info("Simulation " + (isPausedInFuture ? "paused" : "stopped") + " after " + lastLoopTime + " ms. " + frameCount + " frames simulated. Objects with collisions: " + collisionCount);

        //Wake up waiting threads
        sharedInstance.wakeupWaitingThreads();
    }

    /**
     * Set the simulation duration. Ideally, this value should be set before starting the simulation.
     *
     * @param milliseconds Time in milliseconds after which the simulation should stop
     */
    public void stopAfter(long milliseconds) {
        //Check for sanity and warn user if value seems unwanted
        if (milliseconds <= this.simulationTime) {
            Log.warning("You tried to set stop the simulation time at a time in the past. Did you intended to use extendSimulationTime() instead?");
        }

        stopSimulationTime = milliseconds;
    }

    /**
     * Extend the simulation time.
     *
     * @param milliseconds Time in ms that the simulation should be extended by. Must be positive.
     */
    public void extendSimulationTime(long milliseconds) {
        //Do not allow to reduce simulation time
        if (milliseconds < 0) {
            Log.warning("prolongSimulation may only be called with positive values.");
            return;
        }

        stopSimulationTime += milliseconds;
    }

    /**
     * Executes one iteration of the Euler simulation loop. Should only by called by simulator objects loopService
     *
     * @return true if the loop was actually executed
     */
    private boolean executeSimulationLoop() {
        if (simulationType == SimulationType.SIMULATION_TYPE_REAL_TIME ||
                simulationType == SimulationType.SIMULATION_TYPE_MAX_FPS) {
            //Update time
            simulationTime = System.currentTimeMillis() - simulationStartTime;
        } else if (simulationType == SimulationType.SIMULATION_TYPE_FIXED_TIME) {
            //Convert simulation loop frequency to milliseconds between loop calls
            long timeBetweenCalls = (long) ((1.0 / getSimulationLoopFrequency()) * 1000);

            simulationTime += timeBetweenCalls;
        }

        //Check whether we are done
        if (simulationTime >= stopSimulationTime) {
            simulationTime = lastLoopTime;
            stopSimulation();
            return false;
        }

        //Slow down computations to wall-clock time if requested by user
        if (simulationType == SimulationType.SIMULATION_TYPE_FIXED_TIME && slowDownWallClockFactor != 0) {
            long timeDifference = System.currentTimeMillis() - lastLoopStartTime;
            long expectedTimeDifference = (long) ((1.0 / getSimulationLoopFrequency()) * 1000);

            //Only slow down if computation was not already slow enough without slowing it down
            if (timeDifference < expectedTimeDifference * slowDownWallClockFactor) {
                long slowDownTime = expectedTimeDifference * slowDownWallClockFactor - timeDifference;
                try {
                    Thread.sleep(slowDownTime);
                } catch (InterruptedException e) {
                    Log.warning("Failed to slow down simulation computation.");
                    e.printStackTrace();
                }
            }
        }

        //Check if computation should be paused
        while (simulationTime >= computationPauseTime.get()) {
            //Pause computation until unlocked by new time limit
            synchronized (computationPauseTime) {
                try {
                    isComputationPaused = true;
                    computationPauseTime.wait();
                } catch (InterruptedException e) {
                    Log.warning("Thread of simulation with paused computation was interrupted");
                }
            }
        }

        //We're after the pausing while loop, so we are allowed to continue simulation
        isComputationPaused = false;

        //Remember computation start time
        long loopStartTime = System.currentTimeMillis();
        lastLoopStartTime = loopStartTime;

        //Update time between last two iterations (i.e. the last iteration and the current one)
        timeBetweenLastIterations = simulationTime - lastLoopTime;
        lastLoopTime = simulationTime;

        //Update simulated daytime
        daytime.add(Calendar.MILLISECOND, (int)timeBetweenLastIterations * daytimeSpeedup);
        


        synchronized (loopObservers) {
            //Inform observers about upcoming loop iteration
            for (SimulationLoopNotifiable observer : loopObservers) {
                observer.willExecuteLoop(getSimulationObjects(), simulationTime, timeBetweenLastIterations);
            }

            NotificationCenter.getSharedInstance().postNotification(Notification.NOTIFICATION_LOOP_UPCOMING, null);
        }

        synchronized (simulationObjects) {
            for (SimulationLoopExecutable object : simulationObjects) {

                // Inform observers about upcoming loop iteration for each object
                synchronized (loopObservers) {
                    for (SimulationLoopNotifiable observer : loopObservers) {
                        observer.willExecuteLoopForObject(object, simulationTime, timeBetweenLastIterations);
                    }
                }

                //Execute loop
                if(object instanceof PhysicalObject){
                    PhysicsEngine.computePhysics((PhysicalObject) object, getPhysicalObjects(), timeBetweenLastIterations);
                }
                object.executeLoopIteration(timeBetweenLastIterations);

                // Inform observers about completed loop iteration for each object
                synchronized (loopObservers) {
                    for (SimulationLoopNotifiable observer : loopObservers) {
                        observer.didExecuteLoopForObject(object, simulationTime, timeBetweenLastIterations);
                    }
                }
            }
        }

        synchronized (loopObservers) {
            //Inform observers about completed loop iteration
            for (SimulationLoopNotifiable observer : loopObservers) {
                observer.didExecuteLoop(getSimulationObjects(), simulationTime, timeBetweenLastIterations);
            }

            NotificationCenter.getSharedInstance().postNotification(Notification.NOTIFICATION_LOOP_DONE, null);
        }

        synchronized (waitTimers) {
            //See if any thread waited for this simulation time
            for (long time : waitTimers) {
                if (simulationTime >= time) {
                    wakeupWaitingThreads();
                    break;
                }
            }

            //Clean up list
            waitTimers.removeIf(time -> simulationTime >= time);
        }

        //Remember if collisions occurred. Only check this if we don't
        //already know about collisions to save processing power
        if (!collisionOccurredDuringExecution) {
            collisionOccurredDuringExecution = collisionPresent();
        }

        Log.finest("Did loop iteration at simulation time " + simulationTime);
        frameCount++;

        //Slow down computation if requested by user
        if (simulationType == SimulationType.SIMULATION_TYPE_FIXED_TIME && slowDownFactor != 1) {
            long timeDifference = System.currentTimeMillis() - loopStartTime;
            try {
                Thread.sleep(timeDifference * (long)(slowDownFactor-1));
            } catch (InterruptedException e) {
                Log.warning("Failed to slow down simulation computation.");
                e.printStackTrace();
            }
        }

        return true;
    }


    /**
     * Blocks the calling thread until simulation is finished
     */
    public synchronized void waitUntilSimulationFinished() {
        if (synchronousSimulation) {
            Log.warning("Waiting not available in synchronous simulation.");
            return;
        }

        while (isSimulationRunning()) {
            //Go to wait state
            try {
                sharedInstance.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.warning("Could not block thread.");
            }
        }
    }

    /**
     * Blocks the calling thread for a given amount of time
     *
     * @param milliseconds Milliseconds of simulation time for which the caller will be blocked
     */
    public synchronized void waitForTime(long milliseconds) {
        if (synchronousSimulation) {
            Log.warning("Waiting not available in synchronous simulation.");
            return;
        }

        //Save time at which waiting should be ended
        Long endTime = simulationTime + milliseconds;
        synchronized (waitTimers) {
            waitTimers.add(endTime);
        }

        while (simulationTime < endTime) {
            //Go to wait state
            try {
                sharedInstance.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.warning("Could not block thread.");
            }
        }
    }

    private synchronized void wakeupWaitingThreads() {
        sharedInstance.notifyAll();
    }

    /**
     * Register an object to receive information about the loop execution
     *
     * @param observer the object to be informed
     */
    public void registerLoopObserver(SimulationLoopNotifiable observer) {
        synchronized (loopObservers) {
            if (!loopObservers.contains(observer)) {
                loopObservers.add(observer);
            }
        }
    }

    /**
     * Unregister an object to not longer receive information about the loop execution
     *
     * @param observer The observer to be removed
     */
    public void unregisterLoopObserver(SimulationLoopNotifiable observer) {
        synchronized (loopObservers) {
            loopObservers.remove(observer);
        }
    }

    /**
     * Adds an object that is capable of updating its state during the loop iterations. In case the object
     * implements PhysicalObject, it is also added to the physical objects list without explicitly calling
     * registerPhysicalObject(). However, it can only be removed from the physical objects list by calling
     * unregisterSimulationObject(). Removing it only from
     *
     * @param object the object to be updated during the loop iterations
     */
    public void registerSimulationObject(SimulationLoopExecutable object) {
        synchronized (simulationObjects) {
            if (!simulationObjects.contains(object)) {
                simulationObjects.add(object);
            }

            //Register also as physical object if applicable
            if (object instanceof PhysicalObject) {
                registerPhysicalObject((PhysicalObject) object);
            }
        }
    }

    /**
     * Removes an object from the simulation execution loop. In case the object implements PhysicalObject,
     * it is also added to the physical objects list without explicitly calling unregisterPhysicalObject()
     *
     * @param object the object to not longer be updated during simulation execution
     */
    public void unregisterSimulationObject(SimulationLoopExecutable object) {
        synchronized (simulationObjects) {
            simulationObjects.remove(object);

            //Unregister also as physical object if applicable
            if (object instanceof PhysicalObject) {
                unregisterPhysicalObject((PhysicalObject) object);
            }
        }
    }

    /**
     * Adds a physical object to the simulation. The added object will not be notified about simulation
     * progress unless it's added by using registerSimulationObject() or registerLoopObserver()
     *
     * @param object the physical object to be added to simulation
     */
    public void registerPhysicalObject(PhysicalObject object) {
        synchronized (physicalObjects) {
            if (!physicalObjects.contains(object)) {
                physicalObjects.add(object);
            }
        }
    }

    /**
     * Removes a physical object to the simulation. However, objects that were registered as using
     * registerSimulationObject() need to be unregistered using unregisterSimulationObject() and
     * will not be removed from physical objects list to keep the two lists consistent.
     *
     * @param object the physical object to be added to simulation
     */
    public void unregisterPhysicalObject(PhysicalObject object) {
        synchronized (physicalObjects) {
            if (!(object instanceof SimulationLoopExecutable)) {
                //Objects that are not SimulationLoopExecutables can be removed
                physicalObjects.remove(object);
            } else if (!(simulationObjects.contains(object))) {
                //Objects that are SimulationLoopExecutables but not registered as such can also be removed
                physicalObjects.remove(object);
            } else {
                //Objects that are registered SimulationLoopExecutables may not be removed
                Log.warning("You cannot unregister a physical object that is registered as a simulation object");
            }
        }
    }

    /**
     * Retrieves a copy of the physical objects managed by the simulator
     *
     * @return Copy of the physical objects
     */
    public List<PhysicalObject> getPhysicalObjects() {
        return new LinkedList<>(physicalObjects);
    }

    /**
     * Retrieves a copy of the simulation objects managed by the simulator
     *
     * @return Copy of the simulation objects
     */
    public List<SimulationLoopExecutable> getSimulationObjects() {
        return new LinkedList<>(simulationObjects);
    }

    /**
     * Retrieves a list of all objects that are known to be collided.
     *
     * @return List of collided objects
     */
    public List<PhysicalObject> getCollidedObjects() {
        LinkedList<PhysicalObject> collidedObjects = new LinkedList<>(physicalObjects);
        collidedObjects.removeIf(physicalObject -> !physicalObject.getCollision());
        return collidedObjects;
    }

    /**
     * Checks whether a collision is currently present in the simulation
     *
     * @return True iff there is at least one physical objects with a collision
     */
    public boolean collisionPresent() {
        return getCollidedObjects().size() > 0;
    }

    /**
     * Checks whether a collision occurred during the execution of the simulation.
     * This does NOT imply that a collision is currently present, but only that
     * a collision happened at some point in the simulation. This information may be
     * reset while the simulation is not running using resetCollisionOccurred(). This
     * method together with resetCollisionOccurred() can be handy to check if a collision
     * happened during the last extension of the simulation.
     *
     * @return True iff a collision occurred since resetCollisionOccurred()
     */
    public boolean collisionOccurred() {
        return collisionOccurredDuringExecution;
    }

    /**
     * Resets the collision detection used by collisionOccurred(). If the collision
     * is still present, collisionOccurred() will return true after the next simulation
     * loop iteration.
     */
    public void resetCollisionOccurred() {
        if (isSimulationRunning()) {
            Log.severe("Cannot reset collisionOccurred. Simulation currently running.");
            return;
        }

        collisionOccurredDuringExecution = false;
    }

    /**
     * Provides access to the shared instance of the Simulator.
     *
     * @return The shared instance of the simulator.
     */
    public static Simulator getSharedInstance() {
        return sharedInstance;
    }

    /**
     * Returns the frequency at which the Euler loop
     *
     * @return true if the simulation is currently running
     */
    public int getSimulationLoopFrequency() {
        return simulationLoopFrequency;
    }

    /**
     * Return the time between the current loop iteration and the previous loop iteration
     *
     * @return time in ms between last two iterations
     */
    public long getTimeBetweenLastIterations() {
        return timeBetweenLastIterations;
    }

    /**
     * Set the frequency at which the simulation loop should be executed. May be lower in the
     * actual execution. Ask for actual time between last two iterations using
     * getTimeBetweenLastIterations()
     *
     * @param simulationLoopFrequency frequency in Hz
     */
    public void setSimulationLoopFrequency(int simulationLoopFrequency) {
        if (simulationLoopFrequency < 0) {
            Log.severe("Can't set negative simulation loop frequency.");
            return;
        }

        this.simulationLoopFrequency = simulationLoopFrequency;
    }

    /**
     * Checks if the simulation is currently running
     *
     * @return true if the simulation is currently running
     */
    public boolean isSimulationRunning() {
        return isRunning;
    }

    /**
     * Get the current simulation time in milliseconds
     *
     * @return Current simulation time in milliseconds
     */
    public Long getSimulationTime() {
        return simulationTime;
    }

    /**
     * Sets the daytime at which the simulation should start. May only be set while simulation is not running.
     * @param daytime Daytime at which the simulation starts
     * @param speedup Factor by which the simulated day advances in relation to simulated time. E.g. a factor of 2
     *                means that 1 second of simulated time equals 2 seconds in the simulated daytime
     */
    public void setStartDaytime(Date daytime, int speedup) {
        //Setting only allowed if simulation not running
        if (isSimulationRunning()) {
            Log.warning("Cannot update simulation daytime while simulation is running");
            return;
        }

        //Only positive speedups allowed, i.e. not going back in time
        if (speedup > 1) {
            daytimeSpeedup = speedup;
        } else {
            Log.warning("Cannot set daytime speedup to less than 1. Only setting daytime");
        }

        daytimeStart = daytime;
    }

    /**
     * Returns the current date in the simulation
     * @return Date giving the current daytime in the simulation
     */
    public Date getDaytime() {
        return daytime.getTime();
    }

    /**
     * Sets the type of the simulation as described in the class SimulationType.
     * May only be set while the simulation is not running.
     *
     * @param type Type of the simulation
     */
    public void setSimulationType(SimulationType type) {
        if (isSimulationRunning()) {
            Log.severe("Cannot change simulation type. Simulation already running.");
            return;
        }

        simulationType = type;
    }

    /**
     * Get number of simulated frames
     *
     * @return Count of simulated frames
     */
    public long getFrameCount() {
        return frameCount;
    }

    /**
     * Checks if the simulation is synchronous
     *
     * @return True iff the simulation is synchronous
     */
    public boolean isSynchronousSimulation() {
        return synchronousSimulation;
    }

    /**
     * Configure whether the simulation should be synchronous, i.e., run its own
     * thread or be blocking. Default: false.
     *
     * @param synchronousSimulation True iff the simulation should be blocking.
     */
    public void setSynchronousSimulation(boolean synchronousSimulation) {
        this.synchronousSimulation = synchronousSimulation;
    }

    /**
     * Checks whether the simulator assumes the simulation will be paused or extended
     * in the future
     *
     * @return True iff the simulator assumes the simulation will be paused or extended
     */
    public boolean isPausedInFuture() {
        return isPausedInFuture;
    }

    /**
     * Inform the simulator if the simulation will be paused or extended in the future.
     * This knowledge is used to inform objects waiting for the simulation to be finished
     * only after it is really finished.
     *
     * @param pausedInFuture True iff the simulation will be paused or extended
     */
    public void setPausedInFuture(boolean pausedInFuture) {
        isPausedInFuture = pausedInFuture;
    }

    /**
     * A factor by which the simulation computation is slowed down. This does not affect the
     * simulated time between two frames. For example a factor of 2 doubles the wall-clock
     * time needed to run the simulation and a factor of 3 triples the wall-clock time needed
     * to execute the simulation. Default is 1, i.e., no slowing down. This mechanism is only
     * applied if the simulation is run with type SIMULATION_TYPE_FIXED_TIME because slowing
     * down the computation time contradicts the intention of the other types.
     *
     * However, keep in mind that this method only sets a lower bound. The simulation may take
     * more time to be calculated.
     *
     * This method cannot be used simultaneously with slowDownComputationToWallClockTime().
     *
     * @param factor Factor by which the computations are slowed down.
     */
    public synchronized void slowDownComputation(int factor) {
        if (factor < 1) {
            Log.warning("You cannot slow down the simulation by a factor of less than 1. 1 equals as fast as possible computation.");
            return;
        }

        if (slowDownWallClockFactor != 0) {
            Log.warning("slowDownComputation() cannot be used simultaneously with slowDownComputationToWallClockTime(). Call slowDownComputationToWallClockTime(0) before using this method.");
            return;
        }

        slowDownFactor = factor;
    }

    /**
     * This method slows down the computation of the simulation roughly to wall-clock time.
     * For example, a simulation with 30 frames per second, i.e., 33 ms per frame will take 33 ms
     * of wall-clock time to compute a frame. A factor can be provided to further slow down the computation.
     * The factor will be multiplied by the wall-clock time to calculate the lower bound for the
     * computation time. For example, simulating 6 seconds with factor 2 will take about 12 secs
     * of wall clock time. Default is a factor of 0, i.e., no slowing down.
     *
     * However, keep in mind that this method only sets a rough lower bound. The simulation may take
     * more time to be calculated.
     *
     * This method cannot be used simultaneously with slowDownComputationToWallClockTime().
     *
     * @param factor Factor of wall-clock time. Factor*wall-clock time is lower bound for runtime.
     */
    public synchronized void slowDownComputationToWallClockTime(int factor) {
        if (factor < 1) {
            Log.warning("You cannot slow down the simulation by a factor of less than 1. 1 equals as fast as possible computation.");
            return;
        }

        if (slowDownFactor != 1) {
            Log.warning("slowDownComputationToWallClockTime() cannot be used simultaneously with slowDownComputation(). Call slowDownComputation(1) before using this method.");
            return;
        }

        slowDownWallClockFactor = factor;
    }

    /**
     * This method allows to pause the computation of the simulation after a specified amount
     * of simulated time. For example, if the simulator already simulated 5000 ms (i.e. getSimulationTime()
     * returns 5000), and this method is called with 3000 as parameter then the simulator will pause the
     * computation of the simulation after roughly 8000 ms. To continue the computations either call this
     * method again with a specified amount of time or call continueComputations() to continue the
     * computation without a new limit. Use isComputationPaused() to check whether or not the simulator is
     * currently pausing its computations.
     * @param milliseconds Simulated time in milliseconds after which the simulator will pause further computations
     */
    public synchronized void pauseComputationsAfter(long milliseconds) {
        //Check for sane parameter
        if (milliseconds < 0) {
            Log.warning("Called pauseComputationsAfter() with negative parameter. May only be called with positive parameter.");
            return;
        }

        synchronized (computationPauseTime) {
            //Set new pause time
            computationPauseTime.getAndSet(getSimulationTime() + milliseconds);

            //Wake up possibly paused computation
            computationPauseTime.notifyAll();
        }
    }

    /**
     * Returns whether the computation of the simulation is currently paused
     * @return True iff the computation is currently paused
     */
    public synchronized boolean isComputationPaused() {
        return isComputationPaused;
    }

    /**
     * Continues the computation of the simulation without a time limit
     */
    public synchronized void continueComputations() {
        synchronized (computationPauseTime) {
            //Set new pause time to 'never'
            computationPauseTime.getAndSet(Long.MAX_VALUE);

            //Wake up possibly paused computation
            computationPauseTime.notifyAll();
        }
    }

    /**
     * Register a PhysicalObject in the simulation and place it at
     * specified x, y coordinate on the ground with given rotation around z axis
     *
     * @param physicalObject PhysicalObject to be registered and placed in simulation
     * @param posX Position X of the object
     * @param posY Position Y of the object
     * @param rotZ Rotation Z of the object
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public void registerAndPutObject(PhysicalObject physicalObject, double posX, double posY, double rotZ) {

        // Register as full simulation object if possible
        if (physicalObject instanceof SimulationLoopExecutable) {
            registerSimulationObject((SimulationLoopExecutable) (physicalObject));
        // Otherwise just register as physical object
        } else {
            registerPhysicalObject(physicalObject);
        }

        // Preparation for automatic relocation with help of Environment, provided information might be bad for
        // current object type (e.g. Tree should not be spawned within road)
        double finalPosX = posX;
        double finalPosY = posY;
        double finalRotZ = rotZ;

        // Decide per object type how to set position, ugly but better than giving public access to most stuff
        // in the PhysicalObject interface

        // Handle PhysicalVehicle
        if (physicalObject instanceof PhysicalVehicle) {

            // Set center point of physicalVehicle
            PhysicalVehicle physicalVehicle = (PhysicalVehicle)(physicalObject);
            double groundZ = WorldModel.getInstance().getGround(finalPosX, finalPosY, physicalVehicle.getGeometryPos().getEntry(2)).doubleValue();
            physicalVehicle.setGlobalPos(finalPosX, finalPosY, groundZ + physicalVehicle.getOffsetZ() + 0.5 * physicalVehicle.getHeight());
            physicalVehicle.updateMassPointPositions();

            // Create rotation for Z, needed to get the correct ground values of wheel mass points
            Rotation rot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, finalRotZ);
            RealMatrix rotationMatrix = new BlockRealMatrix(rot.getMatrix());

            // Compute rotated positions of wheel mass points with new Z rotation
            RealVector frontLeft = physicalVehicle.getGeometryPos().add(rotationMatrix.operate(physicalVehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getLocalCenterDiff()));
            RealVector frontRight = physicalVehicle.getGeometryPos().add(rotationMatrix.operate(physicalVehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getLocalCenterDiff()));
            RealVector backLeft = physicalVehicle.getGeometryPos().add(rotationMatrix.operate(physicalVehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getLocalCenterDiff()));
            RealVector backRight = physicalVehicle.getGeometryPos().add(rotationMatrix.operate(physicalVehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getLocalCenterDiff()));

            // Get all ground values for new mass point X and Y coordinates
            double frontLeftGroundZ = WorldModel.getInstance().getGround(frontLeft.getEntry(0), frontLeft.getEntry(1), frontLeft.getEntry(2)).doubleValue();
            double frontRightGroundZ = WorldModel.getInstance().getGround(frontRight.getEntry(0), frontRight.getEntry(1), frontRight.getEntry(2)).doubleValue();
            double backLeftGroundZ = WorldModel.getInstance().getGround(backLeft.getEntry(0), backLeft.getEntry(1), backLeft.getEntry(2)).doubleValue();
            double backRightGroundZ = WorldModel.getInstance().getGround(backRight.getEntry(0), backRight.getEntry(1), backRight.getEntry(2)).doubleValue();

            // Store elevated ground values in all vectors
            frontLeft.setEntry(2, frontLeftGroundZ + physicalVehicle.getSimulationVehicle().getWheelRadius());
            frontRight.setEntry(2, frontRightGroundZ + physicalVehicle.getSimulationVehicle().getWheelRadius());
            backLeft.setEntry(2, backLeftGroundZ + physicalVehicle.getSimulationVehicle().getWheelRadius());
            backRight.setEntry(2, backRightGroundZ + physicalVehicle.getSimulationVehicle().getWheelRadius());

            // Compute relative vectors to estimate angles for rotations around X and Y axis
            RealVector backFront1 = frontLeft.subtract(backLeft);
            RealVector backFront2 = frontRight.subtract(backRight);
            RealVector leftRight1 = frontRight.subtract(frontLeft);
            RealVector leftRight2 = backRight.subtract(backLeft);

            // Compute all estimation angles between Z plane and relative vectors
            RealVector planeXYNormVector = new ArrayRealVector(new double[] {0.0, 0.0, 1.0});
            double angleBackFront1 = 0.0;
            double angleBackFront2 = 0.0;
            double angleLeftRight1 = 0.0;
            double angleLeftRight2 = 0.0;

            double normBackFront1 = backFront1.getNorm() * planeXYNormVector.getNorm();
            if (normBackFront1 != 0.0) {
                double dotProduct = backFront1.dotProduct(planeXYNormVector);
                double sinAngle = Math.abs(dotProduct) / normBackFront1;
                angleBackFront1 = Math.asin(sinAngle);
                angleBackFront1 = (dotProduct < 0.0 ? -angleBackFront1 : angleBackFront1);
            }

            double normBackFront2 = backFront2.getNorm() * planeXYNormVector.getNorm();
            if (normBackFront2 != 0.0) {
                double dotProduct = backFront2.dotProduct(planeXYNormVector);
                double sinAngle = Math.abs(dotProduct) / normBackFront2;
                angleBackFront2 = Math.asin(sinAngle);
                angleBackFront2 = (dotProduct < 0.0 ? -angleBackFront2 : angleBackFront2);
            }

            double normLeftRight1 = leftRight1.getNorm() * planeXYNormVector.getNorm();
            if (normLeftRight1 != 0.0) {
                double dotProduct = leftRight1.dotProduct(planeXYNormVector);
                double sinAngle = Math.abs(dotProduct) / normLeftRight1;
                angleLeftRight1 = Math.asin(sinAngle);
                angleLeftRight1 = (dotProduct > 0.0 ? -angleLeftRight1 : angleLeftRight1);
            }

            double normLeftRight2 = leftRight2.getNorm() * planeXYNormVector.getNorm();
            if (normLeftRight2 != 0.0) {
                double dotProduct = leftRight2.dotProduct(planeXYNormVector);
                double sinAngle = Math.abs(dotProduct) / normLeftRight2;
                angleLeftRight2 = Math.asin(sinAngle);
                angleLeftRight2 = (dotProduct > 0.0 ? -angleLeftRight2 : angleLeftRight2);
            }

            // From vector angles compute and set optimal rotation values based on ground levels
            double finalRotX = 0.5 * (angleBackFront1 + angleBackFront2);
            double finalRotY = 0.5 * (angleLeftRight1 + angleLeftRight2);

            // Set optimal angles
            physicalVehicle.setGlobalRotation(finalRotX, finalRotY, finalRotZ);
            physicalVehicle.updateMassPointPositions();

        // Handle Pedestrian
        } else if (physicalObject instanceof Pedestrian) {
            Pedestrian pedestrian = (Pedestrian)(physicalObject);
            double groundZ = WorldModel.getInstance().getGround(finalPosX, finalPosY, pedestrian.getGeometryPos().getEntry(2)).doubleValue();
            pedestrian.setPosition(new ArrayRealVector(new double[] {finalPosX, finalPosY, groundZ}));
            pedestrian.setRotationZ(finalRotZ);

        // Handle Tree
        } else if (physicalObject instanceof Tree) {
            Tree tree = (Tree)(physicalObject);
            double groundZ = WorldModel.getInstance().getGround(finalPosX, finalPosY, tree.getGeometryPos().getEntry(2)).doubleValue();
            tree.setPosition(new ArrayRealVector(new double[] {finalPosX, finalPosY, groundZ + 0.5 * tree.getHeight()}));
            tree.setRotationZ(finalRotZ);

        // Handle NetworkCellBaseStation
        } else if (physicalObject instanceof NetworkCellBaseStation) {
            NetworkCellBaseStation networkCellBaseStation = (NetworkCellBaseStation)(physicalObject);
            double groundZ = WorldModel.getInstance().getGround(finalPosX, finalPosY, networkCellBaseStation.getGeometryPos().getEntry(2)).doubleValue();
            networkCellBaseStation.setPosition(new ArrayRealVector(new double[] {finalPosX, finalPosY, groundZ + 0.5 * networkCellBaseStation.getHeight()}));
            networkCellBaseStation.setRotationZ(finalRotZ);
        }
    }
}