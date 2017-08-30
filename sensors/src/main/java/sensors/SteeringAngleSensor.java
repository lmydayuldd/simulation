package sensors;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.Vehicle;
import simulation.vehicle.VehicleActuator;
import simulation.vehicle.VehicleActuatorType;

/**
 * Created by Aklima Zaman on 20-Jan-17.
 */
public class SteeringAngleSensor extends AbstractSensor {

    private Double value;

    public SteeringAngleSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    protected void calculateValue() {
        Vehicle vehicle = getPhysicalVehicle().getSimulationVehicle();
        VehicleActuator steering = vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING);
        double tempValue = steering.getActuatorValueCurrent();
        //NormalDistribution normalDistribution = new NormalDistribution(tempValue, .001);
        //tempValue = DoubleMath.mean(normalDistribution.sample(10));
        this.value = new Double(tempValue);
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_STEERING;
    }

    @Override
    public String getTypeName() {
        return Double.class.getTypeName();
    }
}
