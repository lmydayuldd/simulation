package simulation.network;

/**
 * Interface for objects that want to get notified by network tasks
 */
public interface NetworkTaskNotifiable {

    /**
     * Function that is called when a network event is handled by a network task
     *
     * @param event Event that is handled
     * @param networkTask Task that handles the event
     * @return Must return true if the event should be handled by task, returning false aborts the processing of the event in the task
     */
    public boolean onTaskHandleNetworkEvent(NetworkDiscreteEvent event, NetworkTask networkTask);

    /**
     * Function that is called after a network event is handled by a network task
     *
     * @param event Event that was handled
     * @param networkTask Task that handled the event
     */
    public void afterTaskHandleNetworkEvent(NetworkDiscreteEvent event, NetworkTask networkTask);
}
