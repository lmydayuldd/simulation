package simulation.network.tasks;

import simulation.network.NetworkDiscreteEvent;
import simulation.network.NetworkNode;
import simulation.network.NetworkTask;
import simulation.network.NetworkTaskId;
import simulation.util.Log;

import java.util.LinkedList;

/**
 * Empty network task that does nothing, should not be used regularly
 */
public class TaskNone extends NetworkTask {

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskNone(NetworkNode node) {
        Log.warning("TaskNone: Constructor - This class should not be used regularly, Node: " + node);
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_NONE);
        setNetworkNode(node);
        setTaskEventIdList(new LinkedList<>());
    }

    /**
     * Function that handles network events internally in the task
     *
     * @param event Network discrete event to be handled
     */
    @Override
    public void taskHandleNetworkEvent(NetworkDiscreteEvent event) {
        // Do nothing
    }
}
