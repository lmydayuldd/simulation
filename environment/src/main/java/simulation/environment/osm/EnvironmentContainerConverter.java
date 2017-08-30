package simulation.environment.osm;

import simulation.environment.visualisationadapter.implementation.*;
import simulation.environment.visualisationadapter.interfaces.*;
import simulation.environment.visualisationadapter.implementation.EnvironmentContainer2D;

import java.util.ArrayList;

/**
 * Created by lukas on 02.02.17.
 * This class encapsulates the whole conversion process and converts all objects in a given EnvironmentContainer to kilometric units
 */
public class EnvironmentContainerConverter {
    private VisualisationEnvironmentContainer containerLongLat;

    private simulation.environment.visualisationadapter.implementation.EnvironmentContainer2D containerMeters;

    private ApproximateConverter converter;

    private EnvBounds bounds;



    public EnvironmentContainerConverter(VisualisationEnvironmentContainer containerLongLat) {
        this.containerLongLat = containerLongLat;
        computeMinLongMinLat();
        convertLatLongToMeters();
    }

    public EnvironmentContainerConverter(VisualisationEnvironmentContainer containerLongLat, double minLong, double minLat) {
        this.containerLongLat = containerLongLat;
        converter = new ApproximateConverter(minLong, minLat);
        convertLatLongToMeters();
    }

    /**
     * converts all Street Nodes in the container to kilometric units
     */
    private void convertLatLongToMeters() {
        ArrayList<EnvStreet> meterStreets = new ArrayList<>();
        for(EnvStreet longLatStreet : containerLongLat.getStreets()) {
            ArrayList<EnvNode> nodes = new ArrayList<>();
            for(EnvNode node : longLatStreet.getNodes()) {
                double nLong = node.getX().doubleValue();
                double nLat = node.getY().doubleValue();


                double mY = converter.convertLatToMeters(nLat);
                double mX = converter.convertLongToMeters(nLong, nLat);

                double mZ = node.getZ().doubleValue();
                long osmId = node.getOsmId();
                nodes.add(new Node2D(mX, mY, mZ, osmId));
            }

            ArrayList<EnvIntersection> intersections = new ArrayList<>();
            for(EnvNode intersection : longLatStreet.getIntersections()) {
                double nLong = intersection.getX().doubleValue();
                double nLat = intersection.getY().doubleValue();


                double mY = converter.convertLatToMeters(nLat);
                double mX = converter.convertLongToMeters(nLong, nLat);

                double mZ = intersection.getZ().doubleValue();
                long osmId = intersection.getOsmId();
                intersections.add(new Intersection2D(mX, mY, mZ, osmId));
            }

            meterStreets.add(new Street2D(nodes, longLatStreet.getSpeedLimit(), intersections, longLatStreet.getOsmId(), longLatStreet.isOneWay()));
        }
        computeMinMax(meterStreets);
        containerMeters = new EnvironmentContainer2D(bounds, meterStreets, new ArrayList<>());
    }

    /**
     * computes the min and max values for x,y,z and thus the bounds of the environment
     * @param streets
     */
    private void computeMinMax(ArrayList<EnvStreet> streets) {
        //assure that every street (including pavements lies in the bounds of the environment
        double minX = 0 - EnvStreet.STREET_WIDTH;
        double minY = 0 - EnvStreet.STREET_WIDTH;
        double minZ = 0;

        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double maxZ = Double.MIN_VALUE;

        for(EnvStreet street: streets) {
            for(EnvNode nodes: street.getNodes()) {
                if(nodes.getX().doubleValue() > maxX) {
                    maxX = nodes.getX().doubleValue();
                }

                if(nodes.getY().doubleValue() > maxY) {
                    maxY = nodes.getY().doubleValue();
                }

                if(nodes.getZ().doubleValue() > maxZ) {
                    maxZ = nodes.getZ().doubleValue();
                }
            }
        }

        this.bounds = new Bounds2D(minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * computes the minimum longitude and latitude and initialises the converter
     */
    private void computeMinLongMinLat() {
        double minLat = Double.MAX_VALUE;
        double minLong = Double.MAX_VALUE;

        for(EnvObject object : containerLongLat.getStreets()) {
            for(EnvNode node : object.getNodes()) {
                double nLat = node.getY().doubleValue();
                double nLong = node.getX().doubleValue();

                if(nLat <= minLat) {
                    minLat = nLat;
                }

                if(nLong <= minLong) {
                    minLong = nLong;
                }
            }
        }

        converter = new ApproximateConverter(minLong, minLat);
    }

    public EnvironmentContainer2D getContainer() {
        return this.containerMeters;
    }
}
