package simulation.util;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Allows to request information from others without knowing them
 */
public class InformationService {
    /** Singleton instance of the class */
    private static final InformationService sharedInstance = new InformationService();

    /** All functions that provide information */
    private final HashMap<String, Supplier> providers = new HashMap<>();

    /** Don't let anyone instantiate this class */
    private InformationService() {}

    /**
     * Return the singleton instance of the class
     * @return Singleton instance of InformationService
     */
    public static InformationService getSharedInstance() {return sharedInstance;}

    /**
     * Allows to offer an information with a specific title. Stop providing the information
     * by setting "null" as information provider.
     * @param information Title of the information
     * @param provider    Method that returns the offered information
     */
    public void offerInformation(String information, Supplier provider) {
        providers.put(information, provider);
    }

    /**
     * Provides a specific information to the caller
     * @param information Title of the requested information
     * @return The requested information or null if no provider exists for that information
     */
    public Object requestInformation(String information) {
        Supplier<Object> s = providers.get(information);
        if (s == null) return null;
        return s.get();
    }
}
