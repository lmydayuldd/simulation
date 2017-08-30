package simulation.network.tasks;

import simulation.network.*;

import java.util.Arrays;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Network task that just receives messages and forwards them to the next layer with small delay
 */
public class TaskTransportSimple extends NetworkTask {

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskTransportSimple(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_TRANSPORT_SIMPLE);
        setNetworkNode(node);
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_TRANSPORT_RECEIVE, NETWORK_EVENT_ID_TRANSPORT_SEND));
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
            case NETWORK_EVENT_ID_TRANSPORT_RECEIVE: {
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_APP_RECEIVE, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_TRANSPORT_SEND: {
                event.getEventMessage().setTransportSequenceNumber(NetworkUtils.getRandomPositiveNumberBits(16));
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 64);
                event.getEventMessage().setTransportAdditionalBits(64);
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_NET_SEND, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            default:
                return;
        }
    }
}
