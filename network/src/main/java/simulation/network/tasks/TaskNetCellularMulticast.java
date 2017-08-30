package simulation.network.tasks;

import simulation.network.*;

import java.util.Arrays;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Network task that receives messages and multicasts them from network layer for cellular base stations
 */
public class TaskNetCellularMulticast extends NetworkTask {

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskNetCellularMulticast(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_NET_CELLULAR_MULTICAST);
        setNetworkNode(node);
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_NET_RECEIVE, NETWORK_EVENT_ID_NET_SEND, NETWORK_EVENT_ID_NET_MULTICAST));
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
            case NETWORK_EVENT_ID_NET_RECEIVE: {
                // Do not receive if this is not a multicast message
                if (!event.getEventMessage().getNetworkIpv6Receiver().equals(NetworkSimulator.getInstance().getNetworkSettings().getIpv6LinkLocalMulticastAddress())) {
                    return;
                }

                // Ignore messages that originated from this node
                if (event.getEventMessage().getNetworkIpv6Sender().equals(networkNode.getIpv6Address())) {
                    return;
                }

                // Multicast message to link layer and do not forward to transport
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NETWORK_EVENT_ID_NET_MULTICAST, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_NET_SEND: {
                event.getEventMessage().setNetworkIpv6Sender(networkNode.getIpv6Address());
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 320);
                event.getEventMessage().setNetAdditionalBits(320);

                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_SEND, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_NET_MULTICAST: {
                // Remove link and physical layer sizes from total message length as they will be recomputed and added again
                int removeAdditionalBits = event.getEventMessage().getLinkAdditionalBits() + event.getEventMessage().getPhyAdditionalBits();
                event.getEventMessage().setLinkAdditionalBits(0);
                event.getEventMessage().setPhyAdditionalBits(0);
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() - removeAdditionalBits);

                // Manage wired message status
                boolean wasWiredMessage = event.getEventMessage().isWiredMessage();
                if (wasWiredMessage) {
                    event.getEventMessage().setWiredMessage(false);
                }

                // Send message to cell with wireless communication
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_SEND, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);

                // If it is not a wired message, copy message and send it to neighboring directly connected base stations as wired message
                if (!wasWiredMessage) {
                    NetworkMessage wiredMessage = event.getEventMessage().copy();
                    wiredMessage.setWiredMessage(true);
                    NetworkDiscreteEvent wiredEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_SEND, networkNode, wiredMessage);
                    NetworkSimulator.getInstance().scheduleEvent(wiredEvent);
                }
                return;
            }
            default:
                return;
        }
    }
}
