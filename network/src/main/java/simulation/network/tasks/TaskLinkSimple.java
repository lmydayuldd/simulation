package simulation.network.tasks;

import simulation.network.*;

import java.util.Arrays;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Network task that just receives messages and forwards them to the next layer with small delay
 */
public class TaskLinkSimple extends NetworkTask {

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskLinkSimple(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_LINK_SIMPLE);
        setNetworkNode(node);
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_LINK_RECEIVE, NETWORK_EVENT_ID_LINK_SEND));
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
            case NETWORK_EVENT_ID_LINK_RECEIVE: {

                // Do not receive if this node is not supposed to receive
                if (!event.getEventMessage().getMacReceiver().equals(networkNode.getMacAddress()) && !event.getEventMessage().getMacReceiver().equals(NetworkSimulator.getInstance().getNetworkSettings().getMacBroadcastAddress())) {
                    return;
                }

                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_NET_RECEIVE, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_LINK_SEND: {
                event.getEventMessage().setMacSender(networkNode.getMacAddress());
                event.getEventMessage().setMacSequenceNumber(NetworkUtils.getRandomPositiveNumberBits(16));
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 288);
                event.getEventMessage().setLinkAdditionalBits(288);
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_SEND_START, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            default:
                return;
        }
    }
}
