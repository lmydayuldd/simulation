package simulation.network;

import commons.simulation.DiscreteEvent;
import commons.simulation.DiscreteEventSimulator;
import commons.simulation.PhysicalObject;
import commons.simulation.SimulationLoopExecutable;
import simulation.network.settings.SettingsCellular;
import simulation.util.Log;
import simulation.vehicle.PhysicalVehicle;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Singleton class that can be registered to the simulator for autonomously driving vehicles
 * to add a network simulation that is based on discrete event simulation for communication
 */
public class NetworkSimulator extends DiscreteEventSimulator {

    /** Singleton instance of this class */
    private static NetworkSimulator instance = null;

    /** List of all physical objects in network simulation */
    private final List<PhysicalObject> physicalObjects = Collections.synchronizedList(new LinkedList<>());

    /** List of all network nodes in network simulation */
    private final List<NetworkNode> networkNodes = Collections.synchronizedList(new LinkedList<>());

    /** Network settings for this network simulator */
    private NetworkSettings networkSettings = null;

    /**
     * NetworkSimulator constructor, only called by getInstance singleton function
     * All settings for the network simulation are defined here
     */
    private NetworkSimulator() {
        // Empty lists
        physicalObjects.clear();
        networkNodes.clear();

        // Load network settings
        networkSettings = new SettingsCellular();
    }

    /**
     * Function to get or create a new singleton instance of this class
     *
     * @return Singleton instance of NetworkSimulator
     */
    public static NetworkSimulator getInstance() {
        if (instance == null) {
            instance = new NetworkSimulator();
        }

        return instance;
    }

    /**
     * Function to reset the singleton class, useful for tests
     */
    public static void resetInstance() {
        NetworkStatistics.resetInstance();
        instance = null;
    }

    /**
     * Get list copy of physical objects in simulation
     *
     * @return List copy of physical objects
     */
    public List<PhysicalObject> getPhysicalObjects() {
        return Collections.synchronizedList(new LinkedList<>(physicalObjects));
    }

    /**
     * Set list copy of physical objects in simulation
     *
     * @param physicalObjects New list copy of physical objects
     */
    protected void setPhysicalObjects(List<PhysicalObject> physicalObjects) {
        this.physicalObjects.clear();
        this.physicalObjects.addAll(Collections.synchronizedList(new LinkedList<>(physicalObjects)));
    }

    /**
     * Get list copy of network nodes in simulation
     *
     * @return List copy of network nodes
     */
    public List<NetworkNode> getNetworkNodes() {
        return Collections.synchronizedList(new LinkedList<>(networkNodes));
    }

    /**
     * Set list copy of network nodes in simulation
     *
     * @param networkNodes New list copy of network nodes
     */
    protected void setNetworkNodes(List<NetworkNode> networkNodes) {
        this.networkNodes.clear();
        this.networkNodes.addAll(Collections.synchronizedList(new LinkedList<>(networkNodes)));
    }

    /**
     * Get network settings of simulation run
     *
     * @return Network settings of simulation run
     */
    public NetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    /**
     * Set network settings of simulation run
     *
     * @param networkSettings New network settings of simulation run
     */
    protected void setNetworkSettings(NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
    }

    /**
     * Function that needs to be implemented in subclasses of DiscreteEventSimulator for processing events
     *
     * @param event Event to be processed
     */
    @Override
    protected void processEvent(DiscreteEvent event) {
        // Only process events for this simulator
        if (event instanceof NetworkDiscreteEvent) {
            // Convert event type
            NetworkDiscreteEvent networkEvent = (NetworkDiscreteEvent)(event);

            // Forward event to network node to allow for some changes before general mechanisms begin
            networkEvent.getNetworkNode().handleNetworkEvent(networkEvent);

            // General processing and statistics
            switch (networkEvent.getNetworkEventId()) {
                case NETWORK_EVENT_ID_PHY_RECEIVE_INTERRUPTION_DETECTED:
                    NetworkStatistics.getInstance().processReceiveInterruptionPhy();
                    break;
                case NETWORK_EVENT_ID_PHY_SEND_START:
                    networkSettings.getNetworkChannelModel().handleNetworkEvent(networkEvent);
                    NetworkStatistics.getInstance().processSendMessageStartPhy(networkEvent.getEventMessage());
                    break;
                case NETWORK_EVENT_ID_LINK_RECEIVE:
                    NetworkStatistics.getInstance().processReceivedMessageLink(networkEvent.getEventMessage());
                    break;
                case NETWORK_EVENT_ID_APP_SEND:
                    networkEvent.getEventMessage().setSimCreateTimeNs(getSimulationTimeNs());
                    NetworkStatistics.getInstance().processSendMessageApp(networkEvent.getEventMessage());
                    break;
                case NETWORK_EVENT_ID_APP_RECEIVE:
                    networkEvent.getEventMessage().setSimReceiveTimeNs(getSimulationTimeNs());
                    NetworkUtils.putMessageInNode(networkEvent.getNetworkNode(), networkEvent.getEventMessage());
                    NetworkStatistics.getInstance().processReceivedMessageApp(networkEvent.getEventMessage());
                    break;
                default:
                    break;
            }
        } else {
            // Warning for invalid events
            Log.warning("NetworkSimulator - processEvent: Skipped invalid event: " + event);
        }
    }

    /**
     * Is called just before the simulation starts
     * Use this to convert desired objects to network nodes and to add them to network simulation
     *
     * @param simulationObjects List of all simulation objects
     */
    @Override
    public void simulationStarted(List<SimulationLoopExecutable> simulationObjects) {
        for (SimulationLoopExecutable simulationObject : simulationObjects) {
            if (simulationObject instanceof PhysicalObject) {
                PhysicalObject physicalObject = (PhysicalObject)(simulationObject);
                physicalObjects.add(physicalObject);

                // Create network node and add it to simulation
                if (simulationObject instanceof PhysicalVehicle || simulationObject instanceof NetworkCellBaseStation) {
                    NetworkNode node = new NetworkNode(physicalObject);
                    networkNodes.add(node);
                }
            }
        }

        networkSettings.getNetworkChannelModel().networkSimulationStart();
    }

    /**
     * Is called just after the simulation ends
     * Change function to show log output from network statistics
     *
     * @param simulationObjects List of all simulation objects
     * @param totalTime Total simulation time in milliseconds
     */
    @Override
    public void simulationStopped(List<SimulationLoopExecutable> simulationObjects, long totalTime) {
        // Log network statistics output
        Log.info(NetworkStatistics.getInstance().generateStatisticsOutput());
    }

    /**
     * Improved toString() method to get more information
     *
     * @return String of information about object
     */
    @Override
    public String toString() {
        return "NetworkSimulator{" +
                "physicalObjects=" + physicalObjects +
                ", networkNodes=" + networkNodes +
                ", networkSettings=" + networkSettings +
                '}';
    }
}
