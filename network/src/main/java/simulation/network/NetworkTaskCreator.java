package simulation.network;

import simulation.network.tasks.*;
import simulation.util.Log;

/**
 * Class that creates network tasks based on their id and network node
 */
public final class NetworkTaskCreator {

    /** Empty constructor, this class has no instances, only static functions */
    private NetworkTaskCreator() {}

    /**
     * Function to create a new network task for a specific network node
     *
     * @param taskId Task id of the new task to be created
     * @param node Node for which the task is created
     * @return New created task for the specified node
     */
    public static NetworkTask createTaskForNode(NetworkTaskId taskId, NetworkNode node) {
        switch (taskId) {
            case NETWORK_TASK_ID_APP_BEACON:
                return new TaskAppBeacon(node);
            case NETWORK_TASK_ID_APP_TRAFFIC_OPTIMIZATION:
                return new TaskAppTrafficOptimization(node);
            case NETWORK_TASK_ID_APP_VELOCITY_CONTROL:
                return new TaskAppVelocityControl(node);
            case NETWORK_TASK_ID_APP_MESSAGES_SOFT_STATE:
                return new TaskAppMessagesSoftState(node);
            case NETWORK_TASK_ID_TRANSPORT_SIMPLE:
                return new TaskTransportSimple(node);
            case NETWORK_TASK_ID_NET_SIMPLE:
                return new TaskNetSimple(node);
            case NETWORK_TASK_ID_NET_CELLULAR_MULTICAST:
                return new TaskNetCellularMulticast(node);
            case NETWORK_TASK_ID_LINK_SIMPLE:
                return new TaskLinkSimple(node);
            case NETWORK_TASK_ID_LINK_CSMA:
                return new TaskLinkCSMA(node);
            case NETWORK_TASK_ID_LINK_BUFFERED_ROHC:
                return new TaskLinkBufferedROHC(node);
            case NETWORK_TASK_ID_PHY_SIMPLE:
                return new TaskPhySimple(node);
            case NETWORK_TASK_ID_PHY_INTERFERENCE:
                return new TaskPhyInterference(node);
            case NETWORK_TASK_ID_NONE:
            default:
                Log.warning("NetworkTaskCreator: createTaskForNode - Returning empty task: Unsupported task id: " + taskId);
                break;
        }

        // Default fallback with log warnings
        return new TaskNone(node);
    }
}
