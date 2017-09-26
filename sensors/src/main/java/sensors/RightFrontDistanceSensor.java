package sensors;

import commons.controller.commons.BusEntry;
import commons.simulation.PhysicalObject;
import sensors.abstractsensors.AbstractDistanceSensor;
import simulation.environment.World;
import simulation.environment.WorldModel;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Marius on 12.09.2017.
 */
public class RightFrontDistanceSensor extends AbstractDistanceSensor {

    public RightFrontDistanceSensor(PhysicalVehicle vehicle) {
        super(vehicle);
    }

    @Override
    protected Double calculateDistance(PhysicalObject o) {
        World world = WorldModel.getInstance();
        double calculatedValue = world.getDistanceRightFrontToStreetBorder(o).doubleValue();
        //NormalDistribution normalDistribution = new NormalDistribution(calculatedValue, 0.01);
        return calculatedValue;
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_RIGHT_FRONT_DISTANCE;
    }
}