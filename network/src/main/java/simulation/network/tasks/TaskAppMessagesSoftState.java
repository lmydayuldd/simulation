package simulation.network.tasks;

import simulation.network.*;

import java.util.*;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Task that enables the network node to act as a soft state system for other known network neighbors
 * by periodically cleaning up outdated messages from the received messages maps
 */
public class TaskAppMessagesSoftState extends NetworkTask {

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskAppMessagesSoftState(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_APP_MESSAGES_SOFT_STATE);
        setNetworkNode(node);
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_RANDOM_START_INITIALIZE, NETWORK_EVENT_ID_SELF_PERIODIC));
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
            case NETWORK_EVENT_ID_RANDOM_START_INITIALIZE: {
                // Schedule periodic event
                NetworkMessage messageTaskName = new NetworkMessage();
                messageTaskName.setMessageContent(getTaskId().name());
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_SELF_PERIODIC, networkNode, messageTaskName);
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_SELF_PERIODIC: {
                // Return if periodic update that is not from this task
                if (!event.getEventMessage().getMessageContent().equals(getTaskId().name())) {
                    return;
                }

                // Perform periodic operations
                softStateCleanup(networkNode.getRecentMacMessagesMap());
                softStateCleanup(networkNode.getRecentIpv6MessagesMap());

                // Create new event to repeat periodic call
                NetworkMessage messageTaskName = new NetworkMessage();
                messageTaskName.setMessageContent(getTaskId().name());
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(2000000000L), NetworkDiscreteEventId.NETWORK_EVENT_ID_SELF_PERIODIC, networkNode, messageTaskName);
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            default:
                return;
        }
    }

    /**
     * Function that performs the cleanup of old messages to give the system a soft state characteristic
     *
     * @param inputMap Map that will be cleaned up by this function
     */
    public static void softStateCleanup(Map<String, List<NetworkMessage>> inputMap) {
        List<String> removeKeys = Collections.synchronizedList(new LinkedList<>());

        for (String key : inputMap.keySet()) {
            List<NetworkMessage> messageList = inputMap.get(key);
            List<NetworkMessage> removeList = Collections.synchronizedList(new LinkedList<>());

            synchronized (messageList) {
                for (NetworkMessage message : messageList) {
                    if ((NetworkSimulator.getInstance().getSimulationTimeNs() - message.getSimReceiveTimeNs()) >= NetworkSimulator.getInstance().getNetworkSettings().getMessageBufferMaxTime()) {
                        removeList.add(message);
                    }
                }
            }

            messageList.removeAll(removeList);

            if (messageList.isEmpty()) {
                removeKeys.add(key);
            }
        }

        inputMap.keySet().removeAll(removeKeys);
    }
}
