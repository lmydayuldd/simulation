package simulation.vehicle;

/**
 * Enum for types of a VehicleActuator
 */
public enum MassPointType {
    /** Mass point located at the front left of the simulated vehicle */
    MASS_POINT_TYPE_WHEEL_FRONT_LEFT,

    /** Mass point located at the front right of the simulated vehicle */
    MASS_POINT_TYPE_WHEEL_FRONT_RIGHT,

    /** Mass point located at the back left of the simulated vehicle */
    MASS_POINT_TYPE_WHEEL_BACK_LEFT,

    /** Mass point located at the back right of the simulated vehicle */
    MASS_POINT_TYPE_WHEEL_BACK_RIGHT,

    /** Mass point located at unknown position*/
    MASS_POINT_TYPE_UNKNOWN,
}
