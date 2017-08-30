package simulation.environment;

import commons.map.ControllerContainer;
import commons.map.IControllerNode;
import commons.simulation.PhysicalObject;
import javafx.geometry.Point3D;
import simulation.environment.osm.IParser;
import simulation.environment.osm.Parser2D;
import simulation.environment.osm.ParserSettings;
import simulation.environment.pedestrians.PedestrianContainer;
import simulation.environment.visualisationadapter.interfaces.VisualisationEnvironmentContainer;

import java.util.List;

/**
 * Created by Philipp Nolte on 07.11.2016.
 *
 * This class is deprecated use WorldModel instead
 */
@Deprecated
public class WorldModelDummy implements World {
    private VisualisationEnvironmentContainer container;

    private IParser parser;

    public WorldModelDummy() {
    }

    @Override
    public Number getGround(Number x, Number y, Number z) {
        return null;
    }

    @Override
    public Number getGroundForNonStreet(Number x, Number y) {
        return null;
    }

    @Override
    public Number getDistanceToMiddleOfStreet(PhysicalObject o) {
        return null;
    }

    @Override
    public Number getDistanceToLeftStreetBorder(PhysicalObject o) {
        return null;
    }

    @Override
    public Number getDistanceToRightStreetBorder(PhysicalObject o) {
        return null;
    }

    @Override
    public VisualisationEnvironmentContainer getContainer() throws Exception {
        if (container == null) {
            parser = new Parser2D(new ParserSettings("/map_ahornstrasse.osm", ParserSettings.ZCoordinates.ALLZERO));
            parser.parse();

            this.container = parser.getContainer();
        }

        return container;

    }

    @Override
    public boolean isItRaining() {
        return false;
    }

    @Override
    public double getWeather() {
        return 0;
    }

    @Override
    public ControllerContainer getControllerMap() {
        return null;
    }

    public IParser getParser() {
        return this.parser;
    }

    @Override
    public PedestrianContainer getPedestrianContainer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Point3D spawnOnStreet(Number x, Number y, Number z, boolean rightLane) {
        return null;
    }

    @Override
    public Point3D spawnNotOnStreet(Number x, Number y, Number z) {
        return null;
    }

    @Override
    public List<Long> getChangedTrafficSignals() {
        return null;
    }

    @Override
    public IControllerNode getRandomNode() {
        return null;
    }

    @Override
    public Number getDistanceFrontLeftWheelToLeftStreetBorder(PhysicalObject o) {return null;}

    @Override
    public Number getDistanceBackLeftWheelToLeftStreetBorder(PhysicalObject o) {return null;}

    @Override
    public Number getDistanceFrontRightWheelToRightStreetBorder(PhysicalObject o) {return null;}

    @Override
    public Number getDistanceBackRightWheelToRightStreetBorder(PhysicalObject o) {return null;}
}
