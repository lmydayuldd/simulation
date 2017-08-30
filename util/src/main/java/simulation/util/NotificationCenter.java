package simulation.util;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Allows to broadcast information within the program
 */
public class NotificationCenter {
    /** Singleton instance of the class */
    private static final NotificationCenter sharedInstance = new NotificationCenter();

    /** Every object and method waiting for certain notifications */
    private final HashMap< String, List< Pair<String, Consumer> > > receivers;

    /** Constructor that clears receivers */
    private NotificationCenter() {
        receivers = new HashMap<>();
    }

    /**
     * @return The shared instance of the of the notification center
     */
    public static NotificationCenter getSharedInstance() {
        return sharedInstance;
    }

    /**
     * Register an object to listen to notifications
     *
     * @param notificationName Name of the notification to listen to
     * @param reaction         Method to be called when notification is posted
     * @param receiver         Receiver of the notification
     */
    public void registerListener(String notificationName, Consumer reaction, Object receiver) {
        //Get current receivers
        List<Pair<String, Consumer>> receiversForNotification = receivers.get(notificationName);

        //If none existing -> create List
        if (receiversForNotification == null) {
            receiversForNotification = new ArrayList<>();
        }

        //Add new receiver to list
        String id = "" + System.identityHashCode(receiver);
        Pair<String, Consumer> pair = new Pair<>(id, reaction);
        final boolean success = receiversForNotification.add(pair);
        if (!success) {
            Log.warning("Could not add object to notification center (" + notificationName + ", " + reaction + ")");
        }

        //Store list back to hash map
        receivers.put(notificationName, receiversForNotification);
    }

    /**
     * Stop listening for a specific message
     * @param notificationName Name of the notification to listen to
     * @param reaction         Method to be called when event happens
     * @param receiver         Receiver of the notification
     */
    public void removeListener(String notificationName, Consumer reaction, Object receiver) {
        //Get current receivers
        List<Pair<String, Consumer>> receiversForNotification = receivers.get(notificationName);

        //No receivers. We're done.
        if (receiversForNotification == null) return;

        //Remove consumer with matching id.
        for (Pair<String, Consumer> pair : receiversForNotification) {
            if (pair.getKey().equals("" + System.identityHashCode(receiver))) {
                final boolean success = receiversForNotification.remove(pair);

                if (!success) {
                    Log.warning("Could not remove object from notification center (" + notificationName + ", " + reaction + ")");
                }
            }
        }

        //Store list back to hash map
        receivers.put(notificationName, receiversForNotification);
    }

    /**
     * Broadcast a notification including some information
     * @param notificationName Name of the notification, used for identification of message type
     * @param context          Information contained in the notification. May be null.
     */
    public void postNotification(String notificationName, Object context) {
        List<Pair<String, Consumer>> registeredListeners = receivers.get(notificationName);
        if (registeredListeners == null) return;
        for (Pair<String, Consumer> pair : registeredListeners) {
            pair.getValue().accept(context);
        }
    }
}