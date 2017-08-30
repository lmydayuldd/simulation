package simulation.network;

/**
 * Interface that enforces implementation of a method to handle network events
 */
public interface NetworkEventHandler {

    /**
     * Function that handles network events
     */
    public void handleNetworkEvent(NetworkDiscreteEvent event);
}
