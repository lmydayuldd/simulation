package simulation.network.tasks;

import simulation.network.*;

import java.util.Arrays;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Network task that just receives messages and forwards them to the next layer with small delay
 */
public class TaskNetSimple extends NetworkTask {

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskNetSimple(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_NET_SIMPLE);
        setNetworkNode(node);
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_NET_RECEIVE, NETWORK_EVENT_ID_NET_SEND));
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
                // Do not receive if this node is not supposed to receive
                if (!event.getEventMessage().getNetworkIpv6Receiver().equals(networkNode.getIpv6Address()) && !event.getEventMessage().getNetworkIpv6Receiver().equals(NetworkSimulator.getInstance().getNetworkSettings().getIpv6LinkLocalMulticastAddress())) {
                    return;
                }

                // Ignore messages that originated from this node
                if (event.getEventMessage().getNetworkIpv6Sender().equals(networkNode.getIpv6Address())) {
                    return;
                }

                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_TRANSPORT_RECEIVE, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_NET_SEND: {
                event.getEventMessage().setNetworkHopLimit(2);
                event.getEventMessage().setNetworkIpv6Sender(networkNode.getIpv6Address());
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 320);
                event.getEventMessage().setNetAdditionalBits(320);

                if (event.getEventMessage().getNetworkIpv6Receiver().equals(NetworkSimulator.getInstance().getNetworkSettings().getIpv6LinkLocalMulticastAddress())) {
                    event.getEventMessage().setMacReceiver(NetworkSimulator.getInstance().getNetworkSettings().getMacBroadcastAddress());
                }

                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_SEND, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            default:
                return;
        }
    }
}
