package simulation.network;

import commons.simulation.PhysicalObject;
import commons.simulation.PhysicalObjectType;
import simulation.util.Log;
import simulation.util.MathHelper;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class that represents a node in the network that is able to communicate
 * Based on a physical object from the main simulation
 */
public class NetworkNode implements NetworkEventHandler {

    /** Physical object of this network node */
    private PhysicalObject physicalObject = null;

    /** MAC address of this network node */
    private String macAddress = "";

    /** IPv6 address of this network node */
    private String ipv6Address = "";

    /** List of tasks for this network node */
    private List<NetworkTask> networkTaskList = Collections.synchronizedList(new LinkedList<NetworkTask>());

    /** Map of recently received messages, key is mac address */
    private Map<String, List<NetworkMessage>> recentMacMessagesMap = Collections.synchronizedMap(new HashMap<>());

    /** Map of recently received messages, key is ipv6 address */
    private Map<String, List<NetworkMessage>> recentIpv6MessagesMap = Collections.synchronizedMap(new HashMap<>());

    /** Map of sending channels, key is channel id, value is count */
    private Map<Integer, Integer> sendingChannelsMap = Collections.synchronizedMap(new HashMap<>());

    /** Map of receiving channels, key is channel id, value is count */
    private Map<Integer, Integer> receiveChannelsMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor for a network node
     *
     * @param physicalObject Physical object this node is based on
     */
    public NetworkNode(PhysicalObject physicalObject) {
        this.physicalObject = physicalObject;
        networkTaskList.clear();
        recentMacMessagesMap.clear();
        recentIpv6MessagesMap.clear();
        sendingChannelsMap.clear();
        receiveChannelsMap.clear();
        initNetworkAddresses();
        initNetworkTasks();
    }

    /**
     * Function that handles network events
     */
    @Override
    public void handleNetworkEvent(NetworkDiscreteEvent event) {
        // Just ask all tasks to process event
        for (NetworkTask task : networkTaskList) {
            task.handleNetworkEvent(event);
        }
    }

    /**
     * Function that returns macAddress
     *
     * @return Value for macAddress
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * Function that returns ipv6Address
     *
     * @return Value for ipv6Address
     */
    public String getIpv6Address() {
        return ipv6Address;
    }

    /**
     * Function that returns a specific network task from this network node, if available
     * If it is not present, empty optional is returned
     *
     * @return Network task from node if available, otherwise empty optional
     */
    public Optional<NetworkTask> getNetworkTaskById(NetworkTaskId taskId) {
        // Iterate and check for equality
        for (NetworkTask task : networkTaskList) {
            if (task.getTaskId() == taskId) {
                return Optional.of(task);
            }
        }

        // Fallback to empty optional value
        return Optional.empty();
    }

    /**
     * Function that returns physicalObject
     *
     * @return Value for physicalObject
     */
    public PhysicalObject getPhysicalObject() {
        return physicalObject;
    }

    /**
     * Get recent messages map, key is mac address
     *
     * @return Recent mac messages map
     */
    public Map<String, List<NetworkMessage>> getRecentMacMessagesMap() {
        return recentMacMessagesMap;
    }

    /**
     * Get recent messages map, key is ipv6 address
     *
     * @return Recent ipv6 messages map
     */
    public Map<String, List<NetworkMessage>> getRecentIpv6MessagesMap() {
        return recentIpv6MessagesMap;
    }

    /**
     * Function that returns sendingChannelsMap
     *
     * @return Value for sendingChannelsMap
     */
    public Map<Integer, Integer> getSendingChannelsMap() {
        return sendingChannelsMap;
    }

    /**
     * Function that returns receiveChannelsMap
     *
     * @return Value for receiveChannelsMap
     */
    public Map<Integer, Integer> getReceiveChannelsMap() {
        return receiveChannelsMap;
    }

    /**
     * Function that computes network addresses
     */
    private void initNetworkAddresses() {
        // Physical object ID is unique and can be combined with prefixes
        String physicalObjectId = Long.toString(physicalObject.getId());
        Pattern hexPattern = Pattern.compile("[a-fA-F0-9]*");

        String stringForId = "";
        String ipv6Prefix = NetworkSimulator.getInstance().getNetworkSettings().getIpv6Prefix();
        String macPrefix = NetworkSimulator.getInstance().getNetworkSettings().getMacPrefix();

        if (ipv6Prefix.length() != 16) {
            Log.warning("NetworkNode - IPv6 prefix is not 16 characters long, check network settings: " + ipv6Prefix);
        }

        if (!hexPattern.matcher(ipv6Prefix).matches()) {
            Log.warning("NetworkNode - IPv6 prefix does not match Hex string, check network settings: " + ipv6Prefix);
        }

        if (macPrefix.length() != 2 && NetworkSimulator.getInstance().getNetworkSettings().getSettingsId() != NetworkSettingsId.NETWORK_SETTINGS_ID_CELLULAR) {
            Log.warning("NetworkNode - MAC prefix is not 2 characters long, check network settings: " + macPrefix);
        }

        if (!hexPattern.matcher(macPrefix).matches()) {
            Log.warning("NetworkNode - MAC prefix does not match Hex string, check network settings: " + macPrefix);
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(physicalObjectId.getBytes(), 0, physicalObjectId.length());
            stringForId = new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Above procedure might not always get a result with 32 characters, fix that
        while (stringForId.length() < 32) {
            stringForId = stringForId + "0";
        }

        if (stringForId.length() != 32) {
            Log.warning("NetworkNode - MD5 result is not 32 characters long: " + stringForId);
        }

        if (!hexPattern.matcher(stringForId).matches()) {
            Log.warning("NetworkNode - MD5 result does not match Hex string, check network settings: " + stringForId);
        }

        // Append 16 * 4 = 64 bits for ipv6
        ipv6Address = ipv6Prefix + stringForId.substring(16);

        // Append 10 * 4 = 40 bits for mac
        macAddress = macPrefix + stringForId.substring(22);
        if (NetworkSimulator.getInstance().getNetworkSettings().getSettingsId() == NetworkSettingsId.NETWORK_SETTINGS_ID_CELLULAR) {
            macAddress = "";
        }
    }

    /**
     * Function that adds all specified tasks in network settings to this network node
     */
    private void initNetworkTasks() {
        // Return if there are no tasks specified for this physical object type
        Map<PhysicalObjectType, List<NetworkTaskId>> taskMap = NetworkSimulator.getInstance().getNetworkSettings().getNetworkTaskIdMap();
        if (!taskMap.containsKey(physicalObject.getPhysicalObjectType()) || taskMap.get(physicalObject.getPhysicalObjectType()).isEmpty()) {
            return;
        }

        // Iterate over all task ids specified in network settings in simulation and add tasks
        for (NetworkTaskId taskId : taskMap.get(physicalObject.getPhysicalObjectType())) {
            networkTaskList.add(NetworkTaskCreator.createTaskForNode(taskId, this));
        }

        // Compute random start time for tasks with network settings
        long startTime = MathHelper.randomLong(NetworkSimulator.getInstance().getNetworkSettings().getMinTaskStartTimeNs(), NetworkSimulator.getInstance().getNetworkSettings().getMaxTaskStartTimeNs());

        // Create event for initial tasks with random start time and empty message
        NetworkMessage message = new NetworkMessage();
        NetworkDiscreteEvent event = new NetworkDiscreteEvent(startTime, NetworkDiscreteEventId.NETWORK_EVENT_ID_RANDOM_START_INITIALIZE, this, message);
        NetworkSimulator.getInstance().scheduleEvent(event);
    }

    /**
     * Improved toString() method to get more information
     *
     * @return String of information about object
     */
    @Override
    public String toString() {
        return "NetworkNode{" +
                "physicalObject=" + physicalObject +
                ", macAddress='" + macAddress + '\'' +
                ", ipv6Address='" + ipv6Address + '\'' +
                ", networkTaskList=" + networkTaskList +
                ", recentMacMessagesMap=" + recentMacMessagesMap +
                ", recentIpv6MessagesMap=" + recentIpv6MessagesMap +
                ", sendingChannelsMap=" + sendingChannelsMap +
                ", receiveChannelsMap=" + receiveChannelsMap +
                '}';
    }
}
