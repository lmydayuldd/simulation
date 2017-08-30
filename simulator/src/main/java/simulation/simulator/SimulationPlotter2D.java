package simulation.simulator;

import org.apache.commons.math3.linear.RealVector;
import commons.simulation.SimulationLoopExecutable;
import commons.simulation.SimulationLoopNotifiable;
import simulation.util.Plotter2D;
import simulation.vehicle.PhysicalVehicle;

import java.util.LinkedList;
import java.util.List;

/**
 * Class that collects vehicle logging data stores them and sends them to to Plotter 2D for  plotting
 */

public class SimulationPlotter2D implements SimulationLoopNotifiable {

    // Lists to store the data separately for plotting later
    private List<Long> simulationTimePoints = new LinkedList<>();
    private List<List<RealVector>> wheelsPosition = new LinkedList<>();
    private List<RealVector> vehiclePos = new LinkedList<>();
    private List<RealVector> vehicleVelocity= new LinkedList<>();
    private PhysicalVehicle plottingVehicle;
    private Plotter2D PositionChart;
    private Plotter2D VelocityChart;
    private Plotter2D zAxisChart;

    /**
     * This function retrieves position and velocity data of the vehicle at every instant and stores it
     * for plotting by the Plotter2D function. To be executed for every iteration in the simulation
     * @param simulationObject PhysicalVehicle object that provides position and velocity data of the vehicle
     * @param totalTime Total simulation time in milliseconds
     * @param deltaTime Delta simulation time in milliseconds
     */
    @Override
    public void willExecuteLoopForObject(SimulationLoopExecutable simulationObject, long totalTime, long deltaTime) {

        // Check if the argument is of type PhysicalVehicle
        if (simulationObject instanceof PhysicalVehicle) {

            // Convert the argument to PhysicalVehicle type
            plottingVehicle = ((PhysicalVehicle) simulationObject);


            // Get current position and velocity of the vehicle
            vehiclePos.add(plottingVehicle.getPos());
            vehicleVelocity.add(plottingVehicle.getVelocity());

            // Get current position of the 4 wheels center of mass
            List<RealVector> wheelsBuffer = plottingVehicle.getWheelMassPointPositions();
            wheelsPosition.add(wheelsBuffer);


            // Store current simulation time
            simulationTimePoints.add(Simulator.getSharedInstance().getSimulationTime());
        }
    }


    /**
     * Function that creates Plots using instances of the Plotter 2D with arguments consisting of the stored data from
     * the simulation:
     * - Position of vehicle center of mass,
     * - position of wheels center of mass,
     * - Vehicle center of mass velocity,
     * - Character string defining the data to be plotted,
     * - and time steps,
     * The plots obtained are displayed on the screen
     *
     * @param simulationObjects List of all simulation objects
     * @param totalTime Total simulation time in milliseconds
     **/
    @Override
    public void simulationStopped(List<SimulationLoopExecutable> simulationObjects, long totalTime) {

        // Calling the plotter constructor for both vehicle Position and Velocity
        PositionChart = new Plotter2D(wheelsPosition, vehiclePos, vehicleVelocity, simulationTimePoints, getPlottingVehicleWheelRadius(), Plotter2D.PLOTTER_OUTPUT_POSITION_XY);
        VelocityChart = new Plotter2D(wheelsPosition, vehiclePos, vehicleVelocity, simulationTimePoints, getPlottingVehicleWheelRadius(), Plotter2D.PLOTTER_OUTPUT_VELOCITY);
        zAxisChart = new Plotter2D(wheelsPosition, vehiclePos, vehicleVelocity, simulationTimePoints, getPlottingVehicleWheelRadius(), Plotter2D.PLOTTER_OUTPUT_Z);

        // Creating and locating the resulting frames in the screen
        PositionChart.pack();
        VelocityChart.pack();
        zAxisChart.pack();

        PositionChart.setVisible(true);
        VelocityChart.setVisible(true);
        zAxisChart.setVisible(true);
    }

    // Getters
    public List<Long> getSimulationTimePoints () {return simulationTimePoints;}
    public List<List<RealVector>> getWheelsPosition () {return wheelsPosition;}
    public List<RealVector> getVehiclePos() {return vehiclePos;}
    public List<RealVector> getVehicleVelocity () {return vehicleVelocity;}
    public PhysicalVehicle getPlottingVehicle () {return plottingVehicle;}
    public Plotter2D getPositionChart () {return PositionChart;}
    public Plotter2D getVelocityChart () {return VelocityChart;}
    public double getPlottingVehicleWheelRadius () {return plottingVehicle.getSimulationVehicle().getWheelRadius();}


    @Override
    public void willExecuteLoop(List<SimulationLoopExecutable> simulationObjects, long totalTime, long deltaTime) {}

    @Override
    public void didExecuteLoop(List<SimulationLoopExecutable> simulationObjects, long totalTime, long deltaTime) {
    }

    @Override
    public void didExecuteLoopForObject(SimulationLoopExecutable simulationObject, long totalTime, long deltaTime) {}

    @Override
    public void simulationStarted(List<SimulationLoopExecutable> simulationObjects) {}
}
