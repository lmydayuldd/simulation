package simulation.environment.osm;

import simulation.environment.geometry.height.AllZeroGenerator;
import simulation.environment.geometry.height.ConcentricCircleGenerator;
import simulation.environment.geometry.height.HeightGenerator;
import simulation.environment.geometry.height.StaticHeightGenerator;
import simulation.environment.visualisationadapter.implementation.Bounds2D;
import simulation.environment.visualisationadapter.implementation.EnvironmentContainer2D;
import simulation.environment.visualisationadapter.implementation.Node2D;
import simulation.environment.visualisationadapter.interfaces.Building;
import simulation.environment.visualisationadapter.interfaces.EnvBounds;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;

/**
 * Created by lukas on 16.02.17.
 *
 * Generate z-Coordinates for a given point or container
 */
public class ZCoordinateGenerator {
    /**
     * Generate z-Coordinates for container using the Strategy specified in strategy
     * @param container
     * @param strategy
     */

    private static HeightGenerator heightGenerator;

    public static void generateZCoordinates(EnvironmentContainer2D container, ParserSettings.ZCoordinates strategy) {
        if(strategy == ParserSettings.ZCoordinates.ALLZERO) {
            heightGenerator = new AllZeroGenerator();
        } else if(strategy == ParserSettings.ZCoordinates.STATIC) {
            heightGenerator = new StaticHeightGenerator(container.getBounds());
        } else {
            ConcentricCircleGenerator.init(container.getBounds());
            heightGenerator = ConcentricCircleGenerator.getInstance();
        }

        double maxZ = Double.MIN_VALUE;
        double minZ = Double.MAX_VALUE;

        for(EnvStreet s : container.getStreets()) {
            for(EnvNode n : s.getNodes()) {
                Node2D n1 = (Node2D) n;
                n1.setZ(heightGenerator.getGround(n1.getX().doubleValue(), n1.getY().doubleValue()));

                if(n1.getZ().doubleValue() < 0) {
                    System.out.println(n1);
                }

                if(n1.getZ().doubleValue() > maxZ) {
                    maxZ = n1.getZ().doubleValue();
                }

                if(n1.getZ().doubleValue() < minZ) {
                    minZ = n1.getZ().doubleValue();
                }

            }

            for(EnvNode n : s.getIntersections()) {
                Node2D n1 = (Node2D) n;
                n1.setZ(heightGenerator.getGround(n1.getX().doubleValue(), n1.getY().doubleValue()));
                if(n1.getZ().doubleValue() > maxZ) {
                    maxZ = n1.getZ().doubleValue();
                }

                if(n1.getZ().doubleValue() < minZ) {
                    minZ = n1.getZ().doubleValue();
                }
            }
        }

        for(Building b : container.getBuildings()) {
            for(EnvNode n : b.getNodes()) {
                Node2D n1 = (Node2D) n;
                n1.setZ(heightGenerator.getGround(n1.getX().doubleValue(), n1.getY().doubleValue()));

                if(n1.getZ().doubleValue() > maxZ) {
                    maxZ = n1.getZ().doubleValue();
                }

                if(n1.getZ().doubleValue() < minZ) {
                    minZ = n1.getZ().doubleValue();
                }
            }
        }
        EnvBounds oldBounds = container.getBounds();
        container.setBounds(new Bounds2D(oldBounds.getMinX(), oldBounds.getMaxX(), oldBounds.getMinY(), oldBounds.getMaxY(), minZ, maxZ));
    }

    /**
     * @param x
     * @param y
     * @return Ground in the environment for given x and y
     */
    public static double getGround(double x, double y) {
        if(heightGenerator != null) {
            return heightGenerator.getGround(x, y);
        } else {
            //this case should never occur!
            return 0.d;
        }

    }

    public static double[][] getHeightMap() {
        return heightGenerator.toHeightMap();
    }

}
