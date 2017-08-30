package simulation.network.tasks;

import commons.controller.commons.BusEntry;
import commons.controller.commons.Vertex;
import commons.map.ControllerNode;
import commons.map.IControllerNode;
import commons.simulation.Sensor;
import javafx.geometry.Point3D;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Combinations;
import simulation.network.*;
import simulation.util.Log;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.Vehicle;

import java.util.*;

import static simulation.network.NetworkDiscreteEventId.NETWORK_EVENT_ID_APP_UPDATE;

/**
 * Task that performs a traffic optimization algorithm
 * Recalculate route of vehicle, if it detects a possible traffic jam on its trajectory
 */
public class TaskAppTrafficOptimization extends NetworkTask {

    /** Range that is used to compare to previous stored position of other vehicle to decide if it moved away and near current trajectory */
    public final static double TRAFFIC_OPTIMIZATION_NOT_MOVED_SINGLE_RANGE = 1.25;

    /** Range that is used to detect a group of multiple non moving vehicles */
    public final static double TRAFFIC_OPTIMIZATION_NOT_MOVED_GROUP_RANGE = 30.0;

    /** Minimum amount of vehicles to detect a traffic jam */
    public final static int TRAFFIC_OPTIMIZATION_MIN_COUNT = 4;

    /** Amount of time that needs to pass for another vehicle to classify it as not moving, in nanoseconds */
    public final static long TRAFFIC_OPTIMIZATION_MIN_TIME_NS = 30000000000L;

    /** Last list of coordinates that have been avoided in route recomputation, use this to improve performance */
    private Set<RealVector> lastAvoidPositionSet = Collections.synchronizedSet(new HashSet<>());

    /** Map that stores received information. Key is IPv6 address, value is pair of last detected movement position and time */
    private Map<String, Map.Entry<Long, RealVector>> infoMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskAppTrafficOptimization(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_APP_TRAFFIC_OPTIMIZATION);
        setNetworkNode(node);
        this.lastAvoidPositionSet.clear();
        this.infoMap.clear();
        setTaskEventIdList(new LinkedList<>(Arrays.asList(NETWORK_EVENT_ID_APP_UPDATE)));
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
            case NETWORK_EVENT_ID_APP_UPDATE: {
                // Skip if message is not status message
                if (event.getEventMessage().getTransportPortDestNumber() != TaskAppBeacon.APP_BEACON_PORT_NUMBER_STATUS_MSG) {
                    return;
                }

                // Continue processing only if this network node is a vehicle
                if (networkNode.getPhysicalObject() instanceof PhysicalVehicle) {
                    PhysicalVehicle physicalVehicle = (PhysicalVehicle) (networkNode.getPhysicalObject());
                    Vehicle vehicle = physicalVehicle.getSimulationVehicle();

                    // Remove nodes which were not received for a long time
                    // Changes to key set or value set are reflected in the map as well, thus retainAll call removes outdated map entries
                    Set<String> recentIpv6Addresses = Collections.synchronizedSet(new HashSet<>(networkNode.getRecentIpv6MessagesMap().keySet()));
                    infoMap.keySet().retainAll(recentIpv6Addresses);

                    // Get all received data from other network node
                    List<Float> floatValues = NetworkUtils.bitStringToFloatList(event.getEventMessage().getMessageContent());

                    if (floatValues.size() != 8) {
                        Log.warning("TaskAppTrafficOptimization - handleNetworkEvent: Float list does not match with specified format of size 8, skipped processing! " + floatValues);
                        return;
                    }

                    RealVector otherGpsPosition = new ArrayRealVector(new double[]{floatValues.get(0), floatValues.get(1), floatValues.get(2)});
                    double otherVelocity = floatValues.get(3);
                    double otherCompass = floatValues.get(4);
                    double otherLength = floatValues.get(5);
                    double otherWidth = floatValues.get(6);
                    double otherHeight = floatValues.get(7);
                    String otherIpv6 = event.getEventMessage().getNetworkIpv6Sender();

                    // Check if node that sent the message is even near the trajectory of this vehicle
                    List<Vertex> trajectory = vehicle.getTrajectory();

                    if (trajectory.isEmpty()) {
                        // Access GPS sensor of this node
                        Optional<Sensor> gpsSensor = vehicle.getSensorByType(BusEntry.SENSOR_GPS_COORDINATES);
                        RealVector thisGpsPosition = new ArrayRealVector(new double[]{0.0, 0.0, 0.0});

                        if (gpsSensor.isPresent()) {
                            thisGpsPosition = (RealVector)(gpsSensor.get().getValue());
                        } else {
                            Log.warning("TaskAppTrafficOptimization - handleNetworkEvent: Vehicle does not have GPS sensor! " + physicalVehicle);
                            return;
                        }

                        trajectory.add(new Vertex(-1L, -1L, thisGpsPosition, 0.0));
                    }

                    Map.Entry<Integer, RealVector> otherNearestPositionData = Vehicle.getNearestPositionOnTrajectory(trajectory, otherGpsPosition, 20);
                    RealVector otherNearestTrajectoryPos = otherNearestPositionData.getValue();

                    // No need to continue processing if other node is not on our trajectory anymore, clear possible old data
                    if (otherNearestTrajectoryPos.getDistance(otherGpsPosition) > TRAFFIC_OPTIMIZATION_NOT_MOVED_SINGLE_RANGE) {
                        infoMap.remove(otherIpv6);
                        return;
                    }

                    // Create new entry if it does not exist for new received data or update if node moved away
                    if ((!infoMap.containsKey(otherIpv6)) ||
                        (infoMap.containsKey(otherIpv6) && infoMap.get(otherIpv6).getValue().getDistance(otherGpsPosition) > TRAFFIC_OPTIMIZATION_NOT_MOVED_SINGLE_RANGE)) {
                        Map.Entry<Long, RealVector> newEntry = new AbstractMap.SimpleEntry<>(event.getEventMessage().getSimReceiveTimeNs(), otherGpsPosition);
                        infoMap.put(otherIpv6, newEntry);
                    }

                    // Detect traffic jams
                    Set<RealVector> avoidPositionSet = Collections.synchronizedSet(new HashSet<>());
                    trafficJamDetection(infoMap, avoidPositionSet);

                    // If list of positions to avoid is not empty and not the same as before, try to create new navigation
                    if (!avoidPositionSet.isEmpty() && !lastAvoidPositionSet.equals(avoidPositionSet)) {
                        lastAvoidPositionSet = Collections.synchronizedSet(new HashSet<>(avoidPositionSet));
                        Optional<IControllerNode> lastNavigationTarget = vehicle.getLastNavigationTarget();

                        // If last navigation target is present, use it again, otherwise use last trajectory node
                        if (lastNavigationTarget.isPresent()) {
                            vehicle.navigateTo(lastNavigationTarget.get(), new LinkedList(lastAvoidPositionSet));
                        } else {
                            RealVector trajectoryTarget = trajectory.get(trajectory.size() - 1).getPosition();
                            Point3D point3D = new Point3D(trajectoryTarget.getEntry(0), trajectoryTarget.getEntry(1), trajectoryTarget.getEntry(2));
                            ControllerNode node = new ControllerNode(point3D, trajectory.get(trajectory.size() - 1).getOsmId());
                            vehicle.navigateTo(node, new LinkedList(lastAvoidPositionSet));
                        }
                    }
                }

                return;
            }
            default:
                return;
        }
    }

    /**
     * Function that detects traffic jams with constant configured values
     *
     * @param inputMap Input map with time of last movement and last position
     * @param outputPositionSet Output set with positions to be avoided
     */
    public static void trafficJamDetection(Map<String, Map.Entry<Long, RealVector>> inputMap, Set<RealVector> outputPositionSet) {
        // From map data create a list of nodes that did not move for a long time
        List<String> nonMovingList = Collections.synchronizedList(new LinkedList<>());

        synchronized (inputMap) {
            for (String ipv6 : inputMap.keySet()) {
                Map.Entry<Long, RealVector> entry = inputMap.get(ipv6);

                if (NetworkUtils.simTimeWithDelay(0) - entry.getKey() >= TRAFFIC_OPTIMIZATION_MIN_TIME_NS) {
                    nonMovingList.add(ipv6);
                }
            }
        }

        // From list of non moving nodes search for groups of non moving nodes, if there are enough nodes
        // and create a list of positions to avoid in navigation
        if (nonMovingList.size() < TRAFFIC_OPTIMIZATION_MIN_COUNT) {
            return;
        }

        Combinations nodeCombinations = new Combinations(nonMovingList.size(), TRAFFIC_OPTIMIZATION_MIN_COUNT);

        for (int[] nodeCombination : nodeCombinations) {
            boolean combinationIsInRange = true;
            for (int i = 0; i < nodeCombination.length; ++i) {
                for (int j = 0; j < nodeCombination.length; ++j) {
                    RealVector gpsPosition1 = inputMap.get(nonMovingList.get(nodeCombination[i])).getValue();
                    RealVector gpsPosition2 = inputMap.get(nonMovingList.get(nodeCombination[j])).getValue();

                    if (gpsPosition1.getDistance(gpsPosition2) > TRAFFIC_OPTIMIZATION_NOT_MOVED_GROUP_RANGE) {
                        combinationIsInRange = false;
                    }
                }
            }

            if (combinationIsInRange) {
                for (int i = 0; i < nodeCombination.length; ++i) {
                    RealVector avoidPosition = inputMap.get(nonMovingList.get(nodeCombination[i])).getValue();
                    outputPositionSet.add(avoidPosition);
                }
            }
        }
    }
}
