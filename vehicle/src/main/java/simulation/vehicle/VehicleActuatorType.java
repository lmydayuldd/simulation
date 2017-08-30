package simulation.vehicle;

/**
 * Enum for types of a VehicleActuator
 */
public enum VehicleActuatorType {
    /** The motor of the vehicle */
    VEHICLE_ACTUATOR_TYPE_MOTOR,

    /** The brakes of the vehicle */
    VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT,
    VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_RIGHT,
    VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_LEFT,
    VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_RIGHT,


    /** A motor turing the steering wheel of the vehicle */
    VEHICLE_ACTUATOR_TYPE_STEERING,

    /** Unknown actuator*/
    VEHICLE_ACTUATOR_TYPE_UNKNOWN,
}
