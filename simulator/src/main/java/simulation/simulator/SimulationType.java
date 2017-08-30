package simulation.simulator;

/**
 * Different run modes for the simulator
 */
public enum SimulationType {
    /**
     * Fixed time interval of simulated time between each two simulation steps.
     * Calculated in advance. Leads to reproducible results.
     */
    SIMULATION_TYPE_FIXED_TIME,

    /**
     * Tries to stick to a fixed time interval, calculated
     * in real time.
     */
    SIMULATION_TYPE_REAL_TIME,

    /**
     * Gets you as many frames per second as your CPU can
     * deliver in real time.
     */
    SIMULATION_TYPE_MAX_FPS
}
