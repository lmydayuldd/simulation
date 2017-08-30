package simulation.network;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static simulation.network.NetworkTaskId.NETWORK_TASK_ID_NONE;

/**
 * Abstract class that defines all basic functions that are implemented in a network task
 * A network task describes common activities in networks, such as discovery procedures or medium access control
 */
public abstract class NetworkTask implements NetworkEventHandler {

    /** Task id of the task */
    private NetworkTaskId taskId = NETWORK_TASK_ID_NONE;

    /** Network node of the task */
    protected NetworkNode networkNode = null;

    /** List of all network event ids that are processed by this task */
    private final List<NetworkDiscreteEventId> taskEventIdList = Collections.synchronizedList(new LinkedList<>());

    /** List of all task notifiable objects */
    private final List<NetworkTaskNotifiable> taskNotifiableList = Collections.synchronizedList(new LinkedList<>());

    /**
     * Function that returns the task id of the task
     *
     * @return Task id of the task
     */
    public NetworkTaskId getTaskId() {
        return taskId;
    }

    /**
     * Function that sets the task id of the task
     *
     * @param taskId New task id of the task
     */
    protected void setTaskId(NetworkTaskId taskId) {
        this.taskId = taskId;
    }

    /**
     * Function that gets the network node
     *
     * @return Network node of the task
     */
    public NetworkNode getNetworkNode() {
        return networkNode;
    };

    /**
     * Function that sets the network node
     *
     * @param networkNode Network node of the task
     */
    protected void setNetworkNode(NetworkNode networkNode) {
        this.networkNode = networkNode;
    };

    /**
     * Function that returns the list of all network event ids that are processed by this task
     *
     * @return Task type of the task
     */
    public List<NetworkDiscreteEventId> getTaskEventIdList() {
        return Collections.synchronizedList(new LinkedList<>(taskEventIdList));
    };

    /**
     * Set list of task event ids
     *
     * @param taskEventIdList New task event id list
     */
    protected void setTaskEventIdList(List<NetworkDiscreteEventId> taskEventIdList) {
        this.taskEventIdList.clear();
        this.taskEventIdList.addAll(taskEventIdList);
    }

    /**
     * Function that checks if an event id is handled by this network task
     *
     * @return True if this task handles the given event id, otherwise false
     */
    public boolean handlesEventId(NetworkDiscreteEventId eventId) {
        return taskEventIdList.contains(eventId);
    }

    /**
     * Function that adds a new task notifiable to this task
     *
     * @param taskNotifiable Task notifiable to be added
     */
    public void registerDiscreteEventSimulationNotifiable(NetworkTaskNotifiable taskNotifiable) {
        taskNotifiableList.add(taskNotifiable);
    }

    /**
     * Function that removes a task notifiable from this task
     *
     * @param taskNotifiable Task notifiable to be removed
     */
    public void unregisterDiscreteEventSimulationNotifiable(NetworkTaskNotifiable taskNotifiable) {
        taskNotifiableList.remove(taskNotifiable);
    }

    /**
     * Function that handles network events internally in the task
     *
     * @param event Network discrete event to be handled
     */
    public abstract void taskHandleNetworkEvent(NetworkDiscreteEvent event);

    /**
     * Function that handles network events
     *
     * @param event Network discrete event to be handled
     */
    @Override
    public void handleNetworkEvent(NetworkDiscreteEvent event) {
        // Skip events that are not handled by this task
        if (!handlesEventId(event.getNetworkEventId())) {
            return;
        }

        // Inform notifiable objects and process return values
        boolean continueProcessing = true;
        synchronized (taskNotifiableList) {
            for (NetworkTaskNotifiable taskNotifiable : taskNotifiableList) {
                boolean notifiableResult = taskNotifiable.onTaskHandleNetworkEvent(event, this);
                continueProcessing = (continueProcessing && notifiableResult);
            }
        }

        // Task notifiables are allowed to prevent further processing with false as return value
        if (!continueProcessing) {
            return;
        }

        // Handle event internally
        taskHandleNetworkEvent(event);

        // Inform notifiable objects
        synchronized (taskNotifiableList) {
            for (NetworkTaskNotifiable taskNotifiable : taskNotifiableList) {
                taskNotifiable.afterTaskHandleNetworkEvent(event, this);
            }
        }
    }
}
