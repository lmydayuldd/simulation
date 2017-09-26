package sensors;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import simulation.environment.World;
import simulation.environment.WorldModel;
import simulation.environment.geometry.osmadapter.GeomStreet;
import simulation.vehicle.PhysicalVehicle;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;

/**
 * Created by Henk on 04.08.2017.
 */
public class StreetTypeSensor extends AbstractSensor {

    private String value;

    public StreetTypeSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_STREETTYPE;
    }

    @Override
    protected void calculateValue() {
        World world = WorldModel.getInstance();
        GeomStreet geom = world.getStreet(getPhysicalVehicle());
        EnvStreet env = (EnvStreet) geom.getObject();
        String result = env.getStreetType();

        // Null values are not allowed as values in controller
        if (result == null) {
            result = "";
        }

        this.value = result;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getTypeName() {
        return String.class.getTypeName();
    }
}
