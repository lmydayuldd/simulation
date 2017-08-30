package simulation.network.tasks;

import commons.controller.commons.BusEntry;
import commons.controller.commons.Vertex;
import commons.simulation.Sensor;
import javafx.geometry.Point3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import simulation.environment.WorldModel;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;
import simulation.network.*;
import simulation.util.Log;
import simulation.util.MathHelper;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.Vehicle;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static simulation.network.NetworkDiscreteEventId.NETWORK_EVENT_ID_APP_UPDATE;
import static simulation.network.NetworkDiscreteEventId.NETWORK_EVENT_ID_SELF_PERIODIC;

/**
 * Task that controls the velocity of a vehicle network node
 * Avoid collisions
 * Priority to the right rule at intersections
 */
public class TaskAppVelocityControl extends NetworkTask {

    /** Value that determines how large the distance is in which other vehicles should be checked */
    public final static double VELOCITY_CONTROL_PRIORITY_MAX_VEHICLE_DISTANCE = 100.0;

    /** Value that determines how large the velocity is in which other vehicles are considered to be standing still */
    public final static double VELOCITY_CONTROL_PRIORITY_MAX_VEHICLE_VELOCITY_DEADLOCK = 0.1;

    /** Value that determines how long a vehicle can take priority to resolve deadlocks in nanoseconds */
    public final static double VELOCITY_CONTROL_PRIORITY_MAX_VEHICLE_TIME_DELTA_DEADLOCK = 10000000000L;

    /** Value that determines how the velocity scales near intersections */
    public final static double VELOCITY_CONTROL_PRIORITY_VEHICLE_VELOCITY_SECTOR_SCALE = 1.0;

    /** Value that determines how the right street is determined with maximum angle */
    public final static double VELOCITY_CONTROL_PRIORITY_SECTOR_MAX_ANGLE = 0.375 * Math.PI;

    /** Value that determines large a single sector is */
    public final static double VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE = 4.0;

    /** Value that determines how many sectors there are in priority algorithm */
    public final static int VELOCITY_CONTROL_PRIORITY_MAX_SECTOR_AMOUNT = 5;

    /** Value that determines how big the difference in sectors must be in priority algorithm to avoid giving priority */
    public final static int VELOCITY_CONTROL_PRIORITY_MAX_SECTOR_DIFFERENCE = 2;

    /** Value that determines how big the difference angle must be that defines if vehicle is driving towards an intersection */
    public final static double VELOCITY_CONTROL_PRIORITY_MAX_ANGLE_DIFFERENCE = 0.125 * Math.PI;

    /** Value that determines how many OSM points are extracted from each street at intersection */
    public final static int VELOCITY_CONTROL_PRIORITY_STREET_NODE_AMOUNT = 10;

    /** Value that determines how accurate nearest trajectory computations are */
    public final static int VELOCITY_CONTROL_PRIORITY_STREET_NODE_PRECISION = 50;

    /** Value that determines how many trajectory nodes are processed */
    public final static int VELOCITY_CONTROL_PRIORITY_COMMON_MAX_TRAJECTORY_SIZE = 25;



    /** Value that determines how large the distance is in which other vehicles should be checked */
    public final static double VELOCITY_CONTROL_AVOIDANCE_MAX_VEHICLE_DISTANCE = 100.0;

    /** Value that determines how large the time step is that is used in algorithm, in seconds */
    public final static double VELOCITY_CONTROL_AVOIDANCE_TIME_STEP = 0.1;

    /** Value that determines the minimal velocity in algorithm that is used, limits amounts of iterations and prevents sudden speed changes near 0 */
    public final static double VELOCITY_CONTROL_AVOIDANCE_MIN_VELOCITY = 0.005;

    /** Value that determines the maximum amount of iterations in algorithm, ensure that computation is not too expensive for small velocities */
    public final static double VELOCITY_CONTROL_AVOIDANCE_MAX_ITERATION_COUNT = 500;

    /** Value that determines when the other vehicle has to avoid collision at left curves */
    public final static double VELOCITY_CONTROL_AVOIDANCE_ANGLE_SUM_MIN_DIFFERENCE = 0.2 * Math.PI;

    /** Value that determines how large the angle is to assume that two vehicles are moving in a similar direction */
    public final static double VELOCITY_CONTROL_AVOIDANCE_SIMILAR_DIRECTION_ANGLE = 0.125 * Math.PI;

    /** Value that determines how many trajectory nodes are taken in the comparison for left turns in trajectories */
    public final static int VELOCITY_CONTROL_AVOIDANCE_LEFT_TURN_NODE_CHECK_AMOUNT = 3;

    /** Value that determines how large the angle has to be for collision avoidance from orthogonal collisions */
    public final static double VELOCITY_CONTROL_AVOIDANCE_ORTHOGONAL_ANGLE_DIFFERENCE = 0.125 * Math.PI;

    /** Value that determines how large a single sector is */
    public final static double VELOCITY_CONTROL_AVOIDANCE_SECTOR_DISTANCE = 5.0;

    /** Value that determines how large the zero sector is */
    public final static double VELOCITY_CONTROL_AVOIDANCE_ZERO_SECTOR_DISTANCE = 0.1;

    /** Value that determines how many sectors there are in collision avoidance algorithm */
    public final static int VELOCITY_CONTROL_AVOIDANCE_MAX_SECTOR_AMOUNT = 5;

    /** Value that determines how the velocity scales with sectors */
    public final static double VELOCITY_CONTROL_AVOIDANCE_VEHICLE_VELOCITY_SECTOR_SCALE = 1.0;

    /** Value that determines how accurate nearest trajectory computations are */
    public final static int VELOCITY_CONTROL_AVOIDANCE_STREET_NODE_PRECISION = 50;

    /** Value that determines how many trajectory nodes are at most processed */
    public final static int VELOCITY_CONTROL_AVOIDANCE_COMMON_MAX_TRAJECTORY_SIZE = 25;



    /** Map that stores received information. Key is IPv6 address, value is pair of last receive time and list of status floats */
    private Map<String, Map.Entry<Long, List<Float>>> statusInfoMap = Collections.synchronizedMap(new HashMap<>());

    /** Map that stores received information. Key is IPv6 address, value is pair of last receive time and list of trajectory floats */
    private Map<String, Map.Entry<Long, List<Float>>> trajectoryInfoMap = Collections.synchronizedMap(new HashMap<>());

    /** Map that stores information about which vehicles are given priority at which OSM node right now */
    private Map<String, Long> priorityInfoMap = Collections.synchronizedMap(new HashMap<>());

    /** Simulation time in nanoseconds when vehicle chooses to take priority to resolve deadlock */
    private AtomicLong priorityTakenSimulationTimeNs = new AtomicLong(-1L);

    /** Boolean indicating if the periodic self update event has been scheduled already */
    private boolean periodicUpdateScheduled = false;

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskAppVelocityControl(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_APP_VELOCITY_CONTROL);
        setNetworkNode(node);
        this.statusInfoMap.clear();
        this.trajectoryInfoMap.clear();
        this.priorityInfoMap.clear();
        this.priorityTakenSimulationTimeNs = new AtomicLong(-1L);
        this.periodicUpdateScheduled = false;
        setTaskEventIdList(new LinkedList<>(Arrays.asList(NETWORK_EVENT_ID_APP_UPDATE, NETWORK_EVENT_ID_SELF_PERIODIC)));
    }

    /**
     * Function that handles network events internally in the task
     *
     * @param event Network discrete event to be handled
     */
    @Override
    public void taskHandleNetworkEvent(NetworkDiscreteEvent event) {
        // Handle events
        switch (event.getNetworkEventId()) {
            case NETWORK_EVENT_ID_SELF_PERIODIC: {
                // Return if periodic update that is not from this task
                if (!event.getEventMessage().getMessageContent().equals(getTaskId().name())) {
                    return;
                }

                // Perform periodic operations
                performVelocityControl();

                // Create new event to repeat periodic call
                NetworkMessage messageTaskName = new NetworkMessage();
                messageTaskName.setMessageContent(getTaskId().name());
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkSimulator.getInstance().getNetworkSettings().getApplicationBeaconUpdateInterval()), NETWORK_EVENT_ID_SELF_PERIODIC, networkNode, messageTaskName);
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_APP_UPDATE: {
                // Skip if message is not status or trajectory message
                if (event.getEventMessage().getTransportPortDestNumber() != TaskAppBeacon.APP_BEACON_PORT_NUMBER_STATUS_MSG &&
                    event.getEventMessage().getTransportPortDestNumber() != TaskAppBeacon.APP_BEACON_PORT_NUMBER_TRAJECTORY_MSG) {
                    return;
                }

                // Continue processing only if this network node is a vehicle
                if (networkNode.getPhysicalObject() instanceof PhysicalVehicle) {
                    PhysicalVehicle physicalVehicle = (PhysicalVehicle) (networkNode.getPhysicalObject());
                    Vehicle vehicle = physicalVehicle.getSimulationVehicle();

                    // Remove nodes which were not received for a long time
                    // Changes to key set or value set are reflected in the map as well, thus retainAll call removes outdated map entries
                    Set<String> recentIpv6Addresses = Collections.synchronizedSet(new HashSet<>(networkNode.getRecentIpv6MessagesMap().keySet()));
                    statusInfoMap.keySet().retainAll(recentIpv6Addresses);
                    trajectoryInfoMap.keySet().retainAll(recentIpv6Addresses);

                    // Get all received data from other network node
                    List<Float> floatValues = Collections.synchronizedList(NetworkUtils.bitStringToFloatList(event.getEventMessage().getMessageContent()));

                    // Put status message in map
                    if (event.getEventMessage().getTransportPortDestNumber() == TaskAppBeacon.APP_BEACON_PORT_NUMBER_STATUS_MSG) {
                        // Format check
                        if (floatValues.size() != 8) {
                            Log.warning("TaskAppVelocityControl - handleNetworkEvent: Float list does not match with specified format of size 8, skipped processing! " + floatValues);
                            return;
                        }

                        // Insert or update map entry
                        Map.Entry<Long, List<Float>> mapEntry = new AbstractMap.SimpleEntry<>(event.getEventMessage().getSimReceiveTimeNs(), floatValues);
                        statusInfoMap.put(event.getEventMessage().getNetworkIpv6Sender(), mapEntry);

                    // Put trajectory message in map
                    } else if (event.getEventMessage().getTransportPortDestNumber() == TaskAppBeacon.APP_BEACON_PORT_NUMBER_TRAJECTORY_MSG) {
                        // Insert or update map entry
                        Map.Entry<Long, List<Float>> mapEntry = new AbstractMap.SimpleEntry<>(event.getEventMessage().getSimReceiveTimeNs(), floatValues);
                        trajectoryInfoMap.put(event.getEventMessage().getNetworkIpv6Sender(), mapEntry);
                    }

                    // Create new event to repeat periodic call if not already scheduled
                    if (!periodicUpdateScheduled) {
                        periodicUpdateScheduled = true;
                        NetworkMessage messageTaskName = new NetworkMessage();
                        messageTaskName.setMessageContent(getTaskId().name());
                        NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(500000000L), NETWORK_EVENT_ID_SELF_PERIODIC, networkNode, messageTaskName);                       NetworkSimulator.getInstance().scheduleEvent(newEvent);
                    }

                    // Perform computation for velocity control
                    performVelocityControl();
                }

                return;
            }
            default:
                return;
        }
    }

    /**
     * Function that performs the velocity control computations with values provided from this task
     */
    private void performVelocityControl() {
        // Only perform this for vehicles
        if (networkNode.getPhysicalObject() instanceof PhysicalVehicle) {
            PhysicalVehicle physicalVehicle = (PhysicalVehicle) (networkNode.getPhysicalObject());
            Vehicle vehicle = physicalVehicle.getSimulationVehicle();

            // Get velocity sensor value
            double currentVelocity = 0.0;
            Optional<Sensor> velocitySensor = vehicle.getSensorByType(BusEntry.SENSOR_VELOCITY);

            if (velocitySensor.isPresent()) {
                currentVelocity = (Double)(velocitySensor.get().getValue());
            } else {
                Log.warning("TaskAppVelocityControl - performVelocityControl: Vehicle has missing velocity sensor, skipped! Vehicle: " + vehicle);
                return;
            }

            // Get compass sensor value
            double currentCompass = 0.0;
            Optional<Sensor> compassSensor = vehicle.getSensorByType(BusEntry.SENSOR_COMPASS);

            if (compassSensor.isPresent()) {
                currentCompass = (Double) (compassSensor.get().getValue());
            } else {
                Log.warning("TaskAppVelocityControl - performVelocityControl: Vehicle has missing compass sensor, skipped! Vehicle: " + vehicle);
                return;
            }

            // Get GPS sensor value
            RealVector currentGps = new ArrayRealVector(new double[]{0.0, 0.0, 0.0});
            Optional<Sensor> gpsSensor = vehicle.getSensorByType(BusEntry.SENSOR_GPS_COORDINATES);

            if (gpsSensor.isPresent()) {
                currentGps = (RealVector) (gpsSensor.get().getValue());
            } else {
                Log.warning("TaskAppVelocityControl - performVelocityControl: Vehicle has missing GPS sensor, skipped! Vehicle: " + vehicle);
                return;
            }

            // Process new data
            double maxAllowedVelocityTotal = Double.MAX_VALUE;
            double maxAllowedVelocityPriorityToRight = computePriorityToTheRightVelocity(statusInfoMap, trajectoryInfoMap, vehicle.getTrajectory(), currentVelocity, currentCompass, currentGps, vehicle.getLength(), vehicle.getWidth(), vehicle.getHeight(), getNetworkNode().getIpv6Address(), priorityInfoMap, priorityTakenSimulationTimeNs);
            double maxAllowedVelocityCollisionAvoidance = computeCollisionAvoidanceVelocity(statusInfoMap, trajectoryInfoMap, vehicle.getTrajectory(), currentVelocity, currentCompass, currentGps, vehicle.getLength(), vehicle.getWidth(), vehicle.getHeight(), getNetworkNode().getIpv6Address());
            maxAllowedVelocityTotal = Math.min(maxAllowedVelocityTotal, Math.min(maxAllowedVelocityPriorityToRight, maxAllowedVelocityCollisionAvoidance));

            // Set new data in vehicle
            vehicle.setMaxTemporaryAllowedVelocity(maxAllowedVelocityTotal);
        }
    }

    /**
     * Function that computes the maximum allowed velocity for this vehicle network node based on information from other vehicle network nodes
     * Compute values for priority to the right rule, resolve deadlocks by prioritizing vehicles based on positions
     *
     * @param statusMap Status map for function, key is IPv6 of other vehicle network node, value is pair of last receive time and list of status float values
     * @param trajectoryMap Trajectory map for function, key is IPv6 of other vehicle network node, value is pair of last receive time and list of trajectory float values
     * @param trajectory Trajectory of the vehicle that performs the computation
     * @param velocity Velocity of the vehicle that performs the computation
     * @param compass Compass value of the vehicle that performs the computation
     * @param gps GPS value of the vehicle that performs the computation
     * @param length Length of the vehicle that performs the computation
     * @param width Width of the vehicle that performs the computation
     * @param height Height of the vehicle that performs the computation
     * @param ipv6 IPv6 of the vehicle that performs the computation
     * @param priorityMap Map of vehicles that this vehicle is giving priority and the OSM node id, modified by function
     * @param priorityTakenTime Simulation time in nanoseconds when vehicle chooses to take priority to resolve deadlock, modified by function
     * @return Maximum allowed velocity for priority to the right, ranges from 0.0 to Double.MAX_VALUE
     */
    public static double computePriorityToTheRightVelocity(Map<String, Map.Entry<Long, List<Float>>> statusMap, Map<String, Map.Entry<Long, List<Float>>> trajectoryMap, List<Vertex> trajectory, double velocity, double compass, RealVector gps, double length, double width, double height, String ipv6, Map<String, Long> priorityMap, AtomicLong priorityTakenTime) {
        // Nothing to do if there is no vehicle nearby
        boolean noVehicleNearby = true;

        for (Map.Entry<Long, List<Float>> statusEntry : statusMap.values()) {
            RealVector otherGps = new ArrayRealVector(new double[]{statusEntry.getValue().get(0), statusEntry.getValue().get(1), statusEntry.getValue().get(2)});
            if (otherGps.getDistance(gps) <= VELOCITY_CONTROL_PRIORITY_MAX_VEHICLE_DISTANCE) {
                noVehicleNearby = false;
                break;
            }
        }

        if (noVehicleNearby) {
            priorityMap.clear();
            return Double.MAX_VALUE;
        }

        // For empty trajectory, no intersection node can be detected in trajectory, just return max value
        if (trajectory.isEmpty()) {
            priorityMap.clear();
            return Double.MAX_VALUE;
        }

        // Handle deadlocks, ignore rules when priority is taken to resolve deadlocks
        if (priorityTakenTime.get() != -1L && (NetworkUtils.simTimeWithDelay(0) - priorityTakenTime.get()) < VELOCITY_CONTROL_PRIORITY_MAX_VEHICLE_TIME_DELTA_DEADLOCK) {
            return Double.MAX_VALUE;
        }
        priorityTakenTime.set(-1L);

        // Get information needed for algorithm
        RealVector frontGps = gps.copy();
        RealVector addVector = new ArrayRealVector(new double[]{0.0, 1.0, 0.0});
        Rotation rot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, compass);
        RealMatrix rotationMatrix = new BlockRealMatrix(rot.getMatrix());
        addVector = rotationMatrix.operate(addVector);
        addVector = addVector.mapMultiply(0.5 * length);
        frontGps = frontGps.add(addVector);

        Map.Entry<Integer, RealVector> trajectoryInfo = Vehicle.getNearestPositionOnTrajectory(trajectory, frontGps, 20);
        int nextIndex = trajectoryInfo.getKey();
        double travelDistance = 0.0;
        Collection<EnvStreet> streetContainer = null;

        try {
            streetContainer = WorldModel.getInstance().getContainer().getStreets();
        } catch (Exception e) {
            e.printStackTrace();
            priorityMap.clear();
            return Double.MAX_VALUE;
        }

        // Scan for first intersection node that really needs to be handled
        for (int i = nextIndex; i < Math.min(nextIndex + VELOCITY_CONTROL_PRIORITY_COMMON_MAX_TRAJECTORY_SIZE, trajectory.size()); ++i) {
            // Handle travel distance
            if (i == nextIndex) {
                travelDistance += trajectory.get(nextIndex).getPosition().getDistance(frontGps);
            } else {
                travelDistance += trajectory.get(i).getPosition().getDistance(trajectory.get(i-1).getPosition());
            }

            // Skip non intersection nodes
            if (!trajectory.get(i).isIntersectionNode()) {
                continue;
            }

            long intersectionNodeOsmId = trajectory.get(i).getOsmId();

            // If travel distance to next intersection node is still too big, this can be skipped
            int currentSector = (int)(travelDistance / VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE);
            if (currentSector > VELOCITY_CONTROL_PRIORITY_MAX_SECTOR_AMOUNT) {
                priorityMap.clear();
                return Double.MAX_VALUE;
            }

            // Process intersection node, get neighbor node coordinates for this node
            List<List<RealVector>> neighborPositionLists = Collections.synchronizedList(new LinkedList<>());
            RealVector originalIntersectionPos = new ArrayRealVector(new double[]{0.0, 0.0, 0.0});

            for (EnvStreet street : streetContainer) {
                for (int j = 0; j < street.getNodes().size(); ++j) {
                    if (street.getNodes().get(j).getOsmId() == intersectionNodeOsmId) {
                        // Add nodes
                        Point3D point = street.getNodes().get(j).getPoint();
                        originalIntersectionPos = new ArrayRealVector(new double[]{point.getX(), point.getY(), point.getZ()});

                        // Add as many nodes from previous neighbor list as possible
                        List<RealVector> prevList = Collections.synchronizedList(new LinkedList<>());
                        for (int k = j - VELOCITY_CONTROL_PRIORITY_STREET_NODE_AMOUNT; k < j; ++k) {
                            if (k >= 0 && k < street.getNodes().size()) {
                                Point3D pointPrev = street.getNodes().get(k).getPoint();
                                RealVector pos = new ArrayRealVector(new double[]{pointPrev.getX(), pointPrev.getY(), pointPrev.getZ()});
                                prevList.add(pos);
                            }
                        }

                        if (!prevList.isEmpty()) {
                            prevList.add(originalIntersectionPos);
                            neighborPositionLists.add(prevList);
                        }

                        // Add as many nodes from next neighbor list as possible
                        List<RealVector> nextList = Collections.synchronizedList(new LinkedList<>());
                        for (int k = j + VELOCITY_CONTROL_PRIORITY_STREET_NODE_AMOUNT; k > j; --k) {
                            if (k >= 0 && k < street.getNodes().size()) {
                                Point3D pointNext = street.getNodes().get(k).getPoint();
                                RealVector pos = new ArrayRealVector(new double[]{pointNext.getX(), pointNext.getY(), pointNext.getZ()});
                                nextList.add(pos);
                            }
                        }

                        if (!nextList.isEmpty()) {
                            nextList.add(originalIntersectionPos);
                            neighborPositionLists.add(nextList);
                        }
                    }
                }
            }

            // Determine the neighbor node list for vehicle
            List<Float> trajectoryPositions = Collections.synchronizedList(new LinkedList<>());
            for (int j = nextIndex; j < Math.min(j + VELOCITY_CONTROL_PRIORITY_COMMON_MAX_TRAJECTORY_SIZE, trajectory.size()); ++j) {
                trajectoryPositions.add((float)(trajectory.get(j).getPosition().getEntry(0)));
                trajectoryPositions.add((float)(trajectory.get(j).getPosition().getEntry(1)));
                trajectoryPositions.add((float)(trajectory.get(j).getPosition().getEntry(2)));
            }

            int neighborPositionListIndex = findNeighborNodeListForVehicle(neighborPositionLists, trajectoryPositions, originalIntersectionPos, frontGps, compass);
            Set<Integer> blockedNeighborIndices = Collections.synchronizedSet(new HashSet<>());
            List<String> blockedNeighborIPv6 = Collections.synchronizedList(new LinkedList<>());

            // Find neighbor node to the right
            int rightListIndex = -1;
            if (neighborPositionListIndex >= 0 && neighborPositionListIndex < neighborPositionLists.size()) {

                // Store information from this vehicle in blocked neighbor indices when needed
                if (velocity <= VELOCITY_CONTROL_PRIORITY_MAX_VEHICLE_VELOCITY_DEADLOCK && frontGps.getDistance(originalIntersectionPos) <= VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE) {
                    blockedNeighborIndices.add(neighborPositionListIndex);
                    blockedNeighborIPv6.add(ipv6);
                }

                List<RealVector> nearestList = neighborPositionLists.get(neighborPositionListIndex);
                if (nearestList.size() >= 2) {
                    RealVector prevToIntersectionVector = nearestList.get(nearestList.size() - 1).subtract(nearestList.get(nearestList.size() - 2));

                    double minRightListIndexAngle = Math.PI;
                    for (int j = 0; j < neighborPositionLists.size(); ++j) {
                        List<RealVector> otherList = neighborPositionLists.get(j);
                        if (otherList.size() >= 2) {
                            RealVector intersectionToOtherVector = otherList.get(otherList.size() - 2).subtract(otherList.get(otherList.size() - 1));

                            double directedAngleToXAxis = -Math.atan2(prevToIntersectionVector.getEntry(1), prevToIntersectionVector.getEntry(0)) + 0.5 * Math.PI;
                            Rotation rotationRight = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, directedAngleToXAxis);
                            RealMatrix rotationMatrixRight = new BlockRealMatrix(rotationRight.getMatrix());
                            RealVector intersectionToOtherVectorRotated = rotationMatrixRight.operate(intersectionToOtherVector);

                            double rotatedDirectedAngleToXAxis = Math.atan2(intersectionToOtherVectorRotated.getEntry(1), intersectionToOtherVectorRotated.getEntry(0));
                            if (Math.abs(rotatedDirectedAngleToXAxis) <= VELOCITY_CONTROL_PRIORITY_SECTOR_MAX_ANGLE) {
                                if (Math.abs(rotatedDirectedAngleToXAxis) < minRightListIndexAngle) {
                                    minRightListIndexAngle = Math.abs(rotatedDirectedAngleToXAxis);
                                    rightListIndex = j;
                                }
                            }
                        }
                    }
                }
            }

            // Check other vehicles, get more deadlock information, possibly add new entries to priority map
            Set<String> newPriorityEntries = Collections.synchronizedSet(new HashSet<>());
            if (rightListIndex != -1) {
                for (Map.Entry<String, Map.Entry<Long, List<Float>>> statusMapEntry : statusMap.entrySet()) {
                    if (trajectoryMap.containsKey(statusMapEntry.getKey())) {
                        // Compute values for other vehicle and check if it is on the street to the right
                        List<Float> otherTrajectoryValues = trajectoryMap.get(statusMapEntry.getKey()).getValue();
                        double otherLength = statusMapEntry.getValue().getValue().get(5);
                        double otherCompass = statusMapEntry.getValue().getValue().get(4);
                        double otherVelocity = statusMapEntry.getValue().getValue().get(3);
                        RealVector otherFrontGps = new ArrayRealVector(new double[]{statusMapEntry.getValue().getValue().get(0), statusMapEntry.getValue().getValue().get(1), statusMapEntry.getValue().getValue().get(2)});
                        RealVector otherAddVector = new ArrayRealVector(new double[]{0.0, 1.0, 0.0});
                        Rotation otherRot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, otherCompass);
                        RealMatrix otherRotationMatrix = new BlockRealMatrix(otherRot.getMatrix());
                        otherAddVector = otherRotationMatrix.operate(otherAddVector);
                        otherAddVector = otherAddVector.mapMultiply(0.5 * otherLength);
                        otherFrontGps = otherFrontGps.add(otherAddVector);
                        int otherPositionListIndex = findNeighborNodeListForVehicle(neighborPositionLists, otherTrajectoryValues, originalIntersectionPos, otherFrontGps, otherCompass);

                        // Store information for deadlocks
                        if (otherPositionListIndex != -1 && otherVelocity <= VELOCITY_CONTROL_PRIORITY_MAX_VEHICLE_VELOCITY_DEADLOCK && otherFrontGps.getDistance(originalIntersectionPos) <= VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE) {
                            blockedNeighborIndices.add(otherPositionListIndex);

                            if (!blockedNeighborIPv6.contains(statusMapEntry.getKey())) {
                                blockedNeighborIPv6.add(statusMapEntry.getKey());
                            }
                        }

                        // If indices match and not yet in priority map, then other vehicle is new to the right of current vehicle
                        if (!priorityMap.keySet().contains(statusMapEntry.getKey()) && otherPositionListIndex == rightListIndex) {
                            // Compute travel distance and sector for other vehicle
                            List<RealVector> otherPositionList = Collections.synchronizedList(new LinkedList<>());
                            for (int j = 0; j < otherTrajectoryValues.size() / 3; ++j) {
                                RealVector vectorPos = new ArrayRealVector(new double[]{otherTrajectoryValues.get(3 * j), otherTrajectoryValues.get((3 * j) + 1), otherTrajectoryValues.get((3 * j) + 2)});
                                otherPositionList.add(vectorPos);
                            }

                            Map.Entry<Integer, RealVector> otherTrajectoryInfo1 = Vehicle.getNearestPositionOnPositionList(otherPositionList, otherFrontGps, VELOCITY_CONTROL_PRIORITY_STREET_NODE_PRECISION);
                            RealVector otherNearestPos1 = otherTrajectoryInfo1.getValue();
                            int otherNearestNextIndex1 = otherTrajectoryInfo1.getKey();

                            Map.Entry<Integer, RealVector> otherTrajectoryInfo2 = Vehicle.getNearestPositionOnPositionList(otherPositionList, originalIntersectionPos, VELOCITY_CONTROL_PRIORITY_STREET_NODE_PRECISION);
                            RealVector otherNearestPos2 = otherTrajectoryInfo2.getValue();
                            int otherNearestNextIndex2 = otherTrajectoryInfo2.getKey();

                            if (otherNearestPos2.getDistance(originalIntersectionPos) <= VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE) {
                                List<RealVector> otherTravelPositions = Collections.synchronizedList(new LinkedList<>());
                                otherTravelPositions.add(otherNearestPos1);

                                for (int j = otherNearestNextIndex1; j < otherNearestNextIndex2; ++j) {
                                    otherTravelPositions.add(otherPositionList.get(j));
                                }

                                otherTravelPositions.add(otherNearestPos2);
                                double otherTravelDistance = 0.0;

                                for (int j = 1; j < otherTravelPositions.size(); ++j) {
                                    otherTravelDistance += otherTravelPositions.get(j).getDistance(otherTravelPositions.get(j-1));
                                }

                                // From travel distance compute sector counts and compare them with each other
                                // If vehicle on the right is too far away, it does not get priority
                                int otherSector = (int)(otherTravelDistance / VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE);

                                if (otherSector - currentSector <= VELOCITY_CONTROL_PRIORITY_MAX_SECTOR_DIFFERENCE) {
                                    newPriorityEntries.add(statusMapEntry.getKey());
                                }
                            }
                        }
                    }
                }
            }

            // Handle deadlocks
            // When all neighbor positions are blocked, this vehicle can take priority if it has lowest ipv6 address
            if (blockedNeighborIndices.size() > 0 && blockedNeighborIndices.size() == neighborPositionLists.size() && blockedNeighborIPv6.size() >= blockedNeighborIndices.size()) {
                Collections.sort(blockedNeighborIPv6);

                // In this case, take priority for this intersection
                if (ipv6.equals(blockedNeighborIPv6.get(0))) {
                    priorityMap.values().remove(intersectionNodeOsmId);
                    priorityTakenTime.set(NetworkUtils.simTimeWithDelay(0));
                    return Double.MAX_VALUE;
                }
            }

            // Manage priority map, check old entries, add new ones
            Set<String> removeEntries = Collections.synchronizedSet(new HashSet<>());

            for (String priorityEntry : priorityMap.keySet()) {
                if (priorityMap.get(priorityEntry).equals(intersectionNodeOsmId)) {
                    if (statusMap.containsKey(priorityEntry)) {
                        RealVector otherGps = new ArrayRealVector(new double[]{statusMap.get(priorityEntry).getValue().get(0), statusMap.get(priorityEntry).getValue().get(1), statusMap.get(priorityEntry).getValue().get(2)});
                        float otherLength = statusMap.get(priorityEntry).getValue().get(5);
                        boolean skipThisPriorityEntry = false;

                        if (otherGps.getDistance(originalIntersectionPos) >= VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE + otherLength) {
                            if (trajectoryMap.containsKey(priorityEntry)) {
                                trajectoryMap.get(priorityEntry).getValue();

                                List<RealVector> positionList = Collections.synchronizedList(new LinkedList<>());
                                for (int j = 0; j < trajectoryMap.get(priorityEntry).getValue().size() / 3; ++j) {
                                    RealVector vectorPos = new ArrayRealVector(new double[]{trajectoryMap.get(priorityEntry).getValue().get(3 * j), trajectoryMap.get(priorityEntry).getValue().get((3 * j) + 1), trajectoryMap.get(priorityEntry).getValue().get((3 * j) + 2)});
                                    positionList.add(vectorPos);
                                }

                                for (int j = 0; j < positionList.size(); ++j) {
                                    if (positionList.get(j).getDistance(originalIntersectionPos) <= VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE) {
                                        skipThisPriorityEntry = true;
                                        break;
                                    }
                                }
                            }
                        } else {
                            skipThisPriorityEntry = true;
                        }

                        if (skipThisPriorityEntry) {
                            continue;
                        }
                    }

                    removeEntries.add(priorityEntry);
                }
            }

            priorityMap.keySet().removeAll(removeEntries);

            for (String newPriority : newPriorityEntries) {
                priorityMap.put(newPriority, intersectionNodeOsmId);
            }

            // Determine maximum velocity if priority map for this OSM intersection node is not empty
            if (priorityMap.containsValue(intersectionNodeOsmId)) {
                return currentSector * VELOCITY_CONTROL_PRIORITY_VEHICLE_VELOCITY_SECTOR_SCALE;
            }
        }

        // Fallback to default action
        priorityMap.clear();
        return Double.MAX_VALUE;
    }

    /**
     * Function that determines which neighbor list of an intersection node are similar to the trajectory of a vehicle
     *
     * @param neighborPositionLists List of lists of neighbor positions of intersection node
     * @param trajectoryPositions List of floats with trajectory positions for vehicle
     * @param originalIntersectionPos RealVector of original intersection node coordinates
     * @param frontGps Front GPS position of vehicle
     * @param compass Compass value for vehicle
     * @return Integer for index in neighborPositionLists, if nothing suitable is found -1
     */
    public static int findNeighborNodeListForVehicle(List<List<RealVector>> neighborPositionLists, List<Float> trajectoryPositions, RealVector originalIntersectionPos, RealVector frontGps, double compass) {
        // Convert trajectory floats to real vectors
        List<RealVector> positionList = Collections.synchronizedList(new LinkedList<>());
        for (int i = 0; i < trajectoryPositions.size() / 3; ++i) {
            RealVector vectorPos = new ArrayRealVector(new double[]{trajectoryPositions.get(3 * i), trajectoryPositions.get((3 * i) + 1), trajectoryPositions.get((3 * i) + 2)});
            positionList.add(vectorPos);
        }

        if (positionList.isEmpty()) {
            return -1;
        }

        // Needs to have point near original intersection in next trajectory nodes
        boolean drivingTowardsIntersectionDist = false;
        for (int i = 0; i < positionList.size(); ++i) {
            if (positionList.get(i).getDistance(originalIntersectionPos) <= VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE) {
                drivingTowardsIntersectionDist = true;
                break;
            }
        }

        if (!drivingTowardsIntersectionDist) {
            return -1;
        }

        int nearestListIndex = -1;
        double nearestListDist = Double.MAX_VALUE;
        for (int i = 0; i < neighborPositionLists.size(); ++i) {
            Map.Entry<Integer, RealVector> neighborInfo = Vehicle.getNearestPositionOnPositionList(neighborPositionLists.get(i), frontGps, VELOCITY_CONTROL_PRIORITY_STREET_NODE_PRECISION);
            RealVector nearestPos = neighborInfo.getValue();

            double distance = nearestPos.getDistance(frontGps);
            if (distance < nearestListDist) {
                nearestListDist = distance;
                nearestListIndex = i;
            }
        }

        // When near to intersection node, compass vector must have similar angle as previous node to intersection node vector
        if (nearestListIndex >= 0 && nearestListIndex < neighborPositionLists.size()) {
            if (frontGps.getDistance(originalIntersectionPos) <= VELOCITY_CONTROL_PRIORITY_SECTOR_DISTANCE) {
                // Compute compass vector
                RealVector compassVector = new ArrayRealVector(new double[]{0.0, 1.0, 0.0});
                Rotation rot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, compass);
                RealMatrix rotationMatrix = new BlockRealMatrix(rot.getMatrix());
                compassVector = rotationMatrix.operate(compassVector);

                // Get vector from previous node to intersection node
                List<RealVector> nearestList = neighborPositionLists.get(nearestListIndex);
                if (nearestList.size() < 2) {
                    return -1;
                }

                RealVector prevToIntersectionVector = nearestList.get(nearestList.size() - 1).subtract(nearestList.get(nearestList.size() - 2));

                // Compare angle of vectors, if possible
                if (prevToIntersectionVector.getNorm() == 0.0 || compassVector.getNorm() == 0.0) {
                    return -1;
                }

                double vectorAngle = Math.acos(prevToIntersectionVector.cosine(compassVector));
                if (vectorAngle > VELOCITY_CONTROL_PRIORITY_MAX_ANGLE_DIFFERENCE) {
                    return -1;
                }
            }
        }

        return nearestListIndex;
    }

    /**
     * Function that computes the maximum allowed velocity for this vehicle network node based on information from other vehicle network nodes
     * Avoid collisions by using that information
     *
     * @param statusMap Status map for function, key is IPv6 of other vehicle network node, value is pair of last receive time and list of status float values
     * @param trajectoryMap Trajectory map for function, key is IPv6 of other vehicle network node, value is pair of last receive time and list of trajectory float values
     * @param trajectory Trajectory of the vehicle that performs the computation
     * @param velocity Velocity of the vehicle that performs the computation
     * @param compass Compass value of the vehicle that performs the computation
     * @param gps GPS value of the vehicle that performs the computation
     * @param length Length of the vehicle that performs the computation
     * @param width Width of the vehicle that performs the computation
     * @param height Height of the vehicle that performs the computation
     * @param ipv6 IPv6 of the vehicle that performs the computation
     * @return Maximum allowed velocity for collision avoidance, ranges from 0.0 to Double.MAX_VALUE
     */
    public static double computeCollisionAvoidanceVelocity(Map<String, Map.Entry<Long, List<Float>>> statusMap, Map<String, Map.Entry<Long, List<Float>>> trajectoryMap, List<Vertex> trajectory, double velocity, double compass, RealVector gps, double length, double width, double height, String ipv6) {
        // For empty trajectory, nothing to do, just return max value
        if (trajectory.isEmpty()) {
            return Double.MAX_VALUE;
        }

        // Build short trajectory for this vehicle
        RealVector frontGps = gps.copy();
        RealVector addVector = new ArrayRealVector(new double[]{0.0, 1.0, 0.0});
        Rotation rot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, compass);
        RealMatrix rotationMatrix = new BlockRealMatrix(rot.getMatrix());
        addVector = rotationMatrix.operate(addVector);
        RealVector compassVector = addVector.copy();
        addVector = addVector.mapMultiply(0.5 * length);
        frontGps = frontGps.add(addVector);

        Map.Entry<Integer, RealVector> trajectoryInfo = Vehicle.getNearestPositionOnTrajectory(trajectory, frontGps, VELOCITY_CONTROL_AVOIDANCE_STREET_NODE_PRECISION);
        RealVector nearestPos = trajectoryInfo.getValue();
        int nextIndex = trajectoryInfo.getKey();

        List<RealVector> shortTrajectory = Collections.synchronizedList(new LinkedList<>());
        shortTrajectory.add(nearestPos);

        for (int i = nextIndex; i < Math.min(nextIndex + VELOCITY_CONTROL_AVOIDANCE_COMMON_MAX_TRAJECTORY_SIZE, trajectory.size()); ++i) {
            RealVector vector = new ArrayRealVector(new double[]{trajectory.get(i).getPosition().getEntry(0), trajectory.get(i).getPosition().getEntry(1), trajectory.get(i).getPosition().getEntry(2)});
            shortTrajectory.add(vector);
        }

        // Find minimum collision distance value for all other vehicles to avoid collisions
        double maxCheckDistance = VELOCITY_CONTROL_AVOIDANCE_MAX_SECTOR_AMOUNT * VELOCITY_CONTROL_AVOIDANCE_SECTOR_DISTANCE;
        double minCollisionDistance = Double.MAX_VALUE;

        // Iterate for all known vehicles with trajectory data
        for (Map.Entry<String, Map.Entry<Long, List<Float>>> statusEntry : statusMap.entrySet()) {
            if (trajectoryMap.containsKey(statusEntry.getKey())) {
                // Get information from other vehicle
                List<Float> otherTrajectoryValues = trajectoryMap.get(statusEntry.getKey()).getValue();
                RealVector otherGps = new ArrayRealVector(new double[]{statusEntry.getValue().getValue().get(0), statusEntry.getValue().getValue().get(1), statusEntry.getValue().getValue().get(2)});
                double otherVelocity = statusEntry.getValue().getValue().get(3);
                double otherCompass = statusEntry.getValue().getValue().get(4);
                double otherLength = statusEntry.getValue().getValue().get(5);
                double otherWidth = statusEntry.getValue().getValue().get(6);

                RealVector otherFrontGps = otherGps.copy();
                RealVector otherAddVector = new ArrayRealVector(new double[]{0.0, 1.0, 0.0});
                Rotation otherRot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, otherCompass);
                RealMatrix otherRotationMatrix = new BlockRealMatrix(otherRot.getMatrix());
                otherAddVector = otherRotationMatrix.operate(otherAddVector);
                RealVector otherCompassVector = addVector.copy();
                otherAddVector = otherAddVector.mapMultiply(0.5 * otherLength);
                otherFrontGps = otherFrontGps.add(otherAddVector);

                List<RealVector> otherTrajectoryPositions = Collections.synchronizedList(new LinkedList<>());
                for (int i = 0; i < otherTrajectoryValues.size() / 3; ++i) {
                    RealVector vectorPos = new ArrayRealVector(new double[]{otherTrajectoryValues.get(3 * i), otherTrajectoryValues.get((3 * i) + 1), otherTrajectoryValues.get((3 * i) + 2)});
                    otherTrajectoryPositions.add(vectorPos);
                }

                // Only check other vehicles in range
                if (otherGps.getDistance(gps) > VELOCITY_CONTROL_AVOIDANCE_MAX_VEHICLE_DISTANCE) {
                    continue;
                }

                // Compute modified trajectory for other vehicle
                Map.Entry<Integer, RealVector> otherTrajectoryInfo = Vehicle.getNearestPositionOnPositionList(otherTrajectoryPositions, otherFrontGps, VELOCITY_CONTROL_AVOIDANCE_STREET_NODE_PRECISION);
                RealVector otherNearestPos = otherTrajectoryInfo.getValue();
                int otherNextIndex = otherTrajectoryInfo.getKey();

                List<RealVector> otherShortTrajectory = Collections.synchronizedList(new LinkedList<>());
                otherShortTrajectory.add(otherNearestPos);

                for (int i = otherNextIndex; i < Math.min(otherNextIndex + VELOCITY_CONTROL_AVOIDANCE_COMMON_MAX_TRAJECTORY_SIZE, otherTrajectoryPositions.size()); ++i) {
                    RealVector vector = new ArrayRealVector(new double[]{otherTrajectoryPositions.get(i).getEntry(0), otherTrajectoryPositions.get(i).getEntry(1), otherTrajectoryPositions.get(i).getEntry(2)});
                    otherShortTrajectory.add(vector);
                }

                // Advance time and distance for both vehicles if at least one is moving with their trajectories and check for collisions
                double travelDistance = 0.0;
                double otherTravelDistance = 0.0;
                double checkDistance = Math.min(maxCheckDistance, minCollisionDistance);
                int iterationCount = 0;

                while (iterationCount < VELOCITY_CONTROL_AVOIDANCE_MAX_ITERATION_COUNT && travelDistance < checkDistance && otherTravelDistance < checkDistance) {
                    iterationCount++;
                    travelDistance += (Math.max(VELOCITY_CONTROL_AVOIDANCE_MIN_VELOCITY, velocity) * VELOCITY_CONTROL_AVOIDANCE_TIME_STEP);
                    otherTravelDistance += (otherVelocity * VELOCITY_CONTROL_AVOIDANCE_TIME_STEP);

                    if (travelDistance < minCollisionDistance) {
                        // Travel distance on trajectories
                        RealVector gpsOnTrajectory = gps.copy();
                        RealVector compassOnTrajectory = compassVector.copy();
                        boolean trajectoryEndReached = travelDistanceOnPositionList(shortTrajectory, travelDistance, gpsOnTrajectory, compassOnTrajectory);

                        RealVector otherGpsOnTrajectory = otherGps.copy();
                        RealVector otherCompassOnTrajectory = otherCompassVector.copy();
                        boolean otherTrajectoryEndReached = travelDistanceOnPositionList(otherShortTrajectory, otherTravelDistance, otherGpsOnTrajectory, otherCompassOnTrajectory);

                        if (trajectoryEndReached && otherTrajectoryEndReached) {
                            break;
                        }

                        // Compute boundaries for vehicles at new positions
                        List<Map.Entry<RealVector, RealVector>> vehicleBoundaries = compute2DBoundaryVectors(gpsOnTrajectory, compassOnTrajectory, length, width);
                        List<Map.Entry<RealVector, RealVector>> otherVehicleBoundaries = compute2DBoundaryVectors(otherGpsOnTrajectory, otherCompassOnTrajectory, otherLength, otherWidth);

                        // Detect collision with boundaries
                        boolean collisionDetected = MathHelper.checkIntersection2D(vehicleBoundaries, otherVehicleBoundaries);

                        if (collisionDetected) {
                            // Collision angle may only be within specified range, angles close to orthogonal should be handled by priority rules
                            double collisionAngle = Math.PI;

                            if (compassOnTrajectory.getNorm() > 0.0 && otherCompassOnTrajectory.getNorm() > 0.0) {
                                collisionAngle = Math.acos(compassOnTrajectory.cosine(otherCompassOnTrajectory));
                            }

                            if (Math.abs(0.5 * Math.PI - collisionAngle) <= VELOCITY_CONTROL_AVOIDANCE_ORTHOGONAL_ANGLE_DIFFERENCE) {
                                break;
                            }

                            // Decide if this vehicle has to avoid collision or the other one has to avoid collision
                            boolean otherVehicleAvoidsCollision = false;

                            // Other vehicle has to avoid collision if this vehicle is moving slower and both move in same direction
                            // and other vehicle is behind this vehicle
                            if (velocity <= otherVelocity && collisionAngle < VELOCITY_CONTROL_AVOIDANCE_SIMILAR_DIRECTION_ANGLE) {
                                RealVector gpsDiffOnTrajectory = otherGpsOnTrajectory.subtract(gpsOnTrajectory);
                                double compassOnTrajectoryNorm = compassOnTrajectory.getNorm();
                                double gpsDiffOnTrajectoryNorm = gpsDiffOnTrajectory.getNorm();

                                if (compassOnTrajectoryNorm > 0.0 && gpsDiffOnTrajectoryNorm > 0.0) {
                                    double gpsCompassAngle = Math.acos(compassOnTrajectory.cosine(gpsDiffOnTrajectory));

                                    if (gpsCompassAngle > Math.PI - VELOCITY_CONTROL_AVOIDANCE_SIMILAR_DIRECTION_ANGLE) {
                                        otherVehicleAvoidsCollision = true;
                                    }
                                }
                            }

                            // Other vehicle has to avoid collision if it is performing a sharper left turn than this vehicle at collision position
                            if (!otherVehicleAvoidsCollision) {
                                double leftAngleSum = computeLeftAngleSumPositionList(shortTrajectory, gpsOnTrajectory);
                                double otherLeftAngleSum = computeLeftAngleSumPositionList(otherShortTrajectory, otherGpsOnTrajectory);

                                if (otherLeftAngleSum - leftAngleSum > VELOCITY_CONTROL_AVOIDANCE_ANGLE_SUM_MIN_DIFFERENCE) {
                                    otherVehicleAvoidsCollision = true;
                                }
                            }

                            // In this case this vehicle has to avoid collision, set collision distance
                            if (!otherVehicleAvoidsCollision) {
                                minCollisionDistance = travelDistance;
                            }

                            break;
                        }
                    }
                }
            }
        }

        // If collision distance is found, compute velocity value
        if (minCollisionDistance <= maxCheckDistance) {
            int sectorNumber = Math.max(1, (int)(minCollisionDistance / VELOCITY_CONTROL_AVOIDANCE_SECTOR_DISTANCE));

            // Compute sector zero with shorter distance
            if (minCollisionDistance <= VELOCITY_CONTROL_AVOIDANCE_ZERO_SECTOR_DISTANCE) {
                sectorNumber = 0;
            }

            return sectorNumber * VELOCITY_CONTROL_AVOIDANCE_VEHICLE_VELOCITY_SECTOR_SCALE;
        }

        // By default, return maximum velocity
        return Double.MAX_VALUE;
    }

    /**
     * Function that computes the 2D boundary vectors for a vehicle
     *
     * @param gps GPS value for vehicle center position for the computation
     * @param compass Compass unit vector for the computation
     * @param length Length of the vehicle for the computation
     * @param width Width of the vehicle for the computation
     * @return List of pairs of 2D vectors defining the start and end points of vectors of the vehicle boundaries
     */
    public static List<Map.Entry<RealVector, RealVector>> compute2DBoundaryVectors(RealVector gps, RealVector compass, double length, double width) {
        List<Map.Entry<RealVector, RealVector>> resultList = new LinkedList<>();

        // Compute compass vectors
        RealVector compassVector = compass.copy();
        Rotation rotOrthogonal = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, -0.5 * Math.PI);
        RealMatrix rotationMatrixOrthogonal = new BlockRealMatrix(rotOrthogonal.getMatrix());
        RealVector compassVectorOrthogonal = rotationMatrixOrthogonal.operate(compassVector);

        // Change vectors to 2D vectors, in error case return empty list and log warning
        RealVector compassVector2D = compassVector.getSubVector(0,2);
        RealVector compassVectorOrthogonal2D = compassVectorOrthogonal.getSubVector(0,2);
        RealVector gps2D = gps.getSubVector(0,2);

        if (compassVector2D.getNorm() <= 0.0 || compassVectorOrthogonal2D.getNorm() <= 0.0) {
            Log.warning("TaskAppVelocityControl - compute2DBoundaryVectors: Compass vector has zero norm, no boundary was computed!");
            return resultList;
        }

        compassVector2D.unitize();
        compassVectorOrthogonal2D.unitize();

        // Compute boundary corners of vehicle
        RealVector backLeft2D = gps2D.add(compassVector2D.mapMultiply(-0.5 * length)).add(compassVectorOrthogonal2D.mapMultiply(-0.5 * width));
        RealVector backRight2D = gps2D.add(compassVector2D.mapMultiply(-0.5 * length)).add(compassVectorOrthogonal2D.mapMultiply(0.5 * width));
        RealVector frontLeft2D = gps2D.add(compassVector2D.mapMultiply(0.5 * length)).add(compassVectorOrthogonal2D.mapMultiply(-0.5 * width));
        RealVector frontRight2D = gps2D.add(compassVector2D.mapMultiply(0.5 * length)).add(compassVectorOrthogonal2D.mapMultiply(0.5 * width));

        // Create map entries and insert them into list
        // Ordering is important here,
        Map.Entry<RealVector, RealVector> entryBack = new AbstractMap.SimpleEntry<RealVector, RealVector>(backLeft2D, backRight2D);
        Map.Entry<RealVector, RealVector> entryRight = new AbstractMap.SimpleEntry<RealVector, RealVector>(backRight2D, frontRight2D);
        Map.Entry<RealVector, RealVector> entryFront = new AbstractMap.SimpleEntry<RealVector, RealVector>(frontRight2D, frontLeft2D);
        Map.Entry<RealVector, RealVector> entryLeft = new AbstractMap.SimpleEntry<RealVector, RealVector>(frontLeft2D, backLeft2D);
        resultList.add(entryBack);
        resultList.add(entryRight);
        resultList.add(entryFront);
        resultList.add(entryLeft);

        // Return result
        return resultList;
    }

    /**
     * Function that computes the gps and compass values if a certain distance is traveled along a list of positions
     *
     * @param positionList List of positions along which should be traveled
     * @param travelDistance Total distance that should be traveled on position list
     * @param gps GPS result value of computation, modified by function
     * @param compass Compass result value for the computation, modified by function
     * @return True when end of trajectory is reached and no result is available, otherwise false
     */
    public static boolean travelDistanceOnPositionList(List<RealVector> positionList, double travelDistance, RealVector gps, RealVector compass) {
        // Return true if input list is empty
        if (positionList.isEmpty()) {
            return true;
        }

        double distanceOnTrajectory = 0.0;

        for (int i = 0; i < positionList.size(); ++i) {
            // When last node is reached, no computation result is available
            if (i == positionList.size() - 1) {
                return true;
            }

            // Now it is safe to access current and next node, compute difference vector
            RealVector currentToNext = positionList.get(i + 1).subtract(positionList.get(i));
            double currentToNextLength = currentToNext.getNorm();

            // If sum of distances is smaller than desired travel distance, sum up and continue
            if (distanceOnTrajectory + currentToNextLength < travelDistance) {
                distanceOnTrajectory += currentToNextLength;

            // When norm is not zero, then compute correct partial values between positions
            } else if (currentToNextLength > 0.0 && travelDistance > distanceOnTrajectory) {
                double distanceFactor = (travelDistance - distanceOnTrajectory) / currentToNextLength;
                RealVector gpsResult = positionList.get(i).add(currentToNext.mapMultiply(distanceFactor));
                RealVector compassResult = currentToNext.unitVector();
                gps.setEntry(0, gpsResult.getEntry(0));
                gps.setEntry(1, gpsResult.getEntry(1));
                gps.setEntry(2, gpsResult.getEntry(2));
                compass.setEntry(0, compassResult.getEntry(0));
                compass.setEntry(1, compassResult.getEntry(1));
                compass.setEntry(2, compassResult.getEntry(2));
                break;
            }
        }

        return false;
    }

    /**
     * Function that computes the sum of all angles that are oriented to the left in the position list
     *
     * @param positionList List of positions along which left angles should be computed
     * @param gps GPS value to be used for computation
     * @return Sum of angles that are directed to the left in the position left
     */
    public static double computeLeftAngleSumPositionList(List<RealVector> positionList, RealVector gps) {
        double leftTurnAngleSum = 0.0;
        Map.Entry<Integer, RealVector> trajectoryInfoLeftTurn = Vehicle.getNearestPositionOnPositionList(positionList, gps, VELOCITY_CONTROL_AVOIDANCE_STREET_NODE_PRECISION);
        int nextIndexLeftTurn = trajectoryInfoLeftTurn.getKey();

        for (int i = nextIndexLeftTurn - VELOCITY_CONTROL_AVOIDANCE_LEFT_TURN_NODE_CHECK_AMOUNT; i < 1 + nextIndexLeftTurn + VELOCITY_CONTROL_AVOIDANCE_LEFT_TURN_NODE_CHECK_AMOUNT; ++i) {
            if (i >= 0 && (i + 2) < positionList.size()) {
                RealVector startToMiddle = positionList.get(i + 1).subtract(positionList.get(i));
                RealVector middleToEnd = positionList.get(i + 2).subtract(positionList.get(i + 1));
                double startToMiddleNorm = startToMiddle.getNorm();
                double middleToEndNorm = middleToEnd.getNorm();

                if (startToMiddleNorm > 0.0) {
                    startToMiddle.unitize();
                }

                if (middleToEndNorm > 0.0) {
                    middleToEnd.unitize();
                }

                double directedAngleToXAxis = -Math.atan2(startToMiddle.getEntry(1), startToMiddle.getEntry(0)) + 0.5 * Math.PI;
                Rotation rotationLeftCurve = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, directedAngleToXAxis);
                RealMatrix rotationMatrixLeftCurve = new BlockRealMatrix(rotationLeftCurve.getMatrix());
                RealVector middleToEndRotated = rotationMatrixLeftCurve.operate(middleToEnd);

                // This is a movement to the left in relation to the previous vector, small difference to ensure value is not near to zero
                if (middleToEndRotated.getEntry(0) <= -0.01 && startToMiddleNorm > 0.0 && middleToEndNorm > 0.0) {
                    double leftAngle = Math.acos(startToMiddle.cosine(middleToEnd));
                    leftTurnAngleSum += leftAngle;
                }
            }
        }

        return leftTurnAngleSum;
    }
}
