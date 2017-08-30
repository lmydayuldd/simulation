package simulation.vehicle;

/**
 * State of vehicle parts
 */
public enum VehicleStatus {
    /** Status is OK. No problems sensed. */
    VEHICLE_STATUS_OK(1000),

    /** Status is OK, but component should be inspected in workshop */
    VEHICLE_STATUS_SERVICE_REQUIRED(2000),

    /** Component damaged, but still working */
    VEHICLE_STATUS_DAMAGED(3000),

    /** Failure of the component imminent */
    VEHICLE_STATUS_CRITICAL(4000),

    /** Component is broken */
    VEHICLE_STATUS_FAILURE(5000);

    /** Severeness of the status. Higher means more critical. */
    private Integer severeness;

    /**
     * Constructor for a new status
     * @param severeness Severeness of the status. Higher means more critical.
     */
    VehicleStatus(int severeness) {
        this.severeness = severeness;
    }

    /**
     * Compares two states
     * @param other Level to compare caller to
     * @return True iff the caller has a higher or equal priority than other
     */
    public boolean isWorseThanOrEqual(VehicleStatus other) {
        return this.severeness >= other.severeness;
    }
}
