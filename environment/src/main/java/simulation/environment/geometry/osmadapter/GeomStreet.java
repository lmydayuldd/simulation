package simulation.environment.geometry.osmadapter;

import commons.simulation.PhysicalObject;
import javafx.geometry.Point3D;
import simulation.environment.pedestrians.PedestrianStreetParameters;
import simulation.environment.visualisationadapter.implementation.Node2D;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvObject;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;

/**
 * Created by lukas on 22.01.17.
 *
 * A container for geometric operations on Streets
 */
public class GeomStreet implements EnvObjectGeomContainer {

    private EnvStreet street;

    private SplineDeterminator deter;

    public GeomStreet(EnvStreet street) {
        this.street = street;
        this.deter = new LinearSplineDeterminator(street);
    }



    @Override
    /**
     * @return the distance from node to the middle of the street
     */
    public double getDistanceToMiddle(PhysicalObject o) {
        //ensure EnvNode-Implementation implements equals() and hashCode()!!!!!
        EnvNode node = new Node2D(o.getGeometryPos().getEntry(0),o.getGeometryPos().getEntry(1),o.getGeometryPos().getEntry(2));

        if(street.getNodes().contains(node)) {
            return 0;
        } else {
            return deter.determineSplineDistance(o);
        }
    }

    @Override
    /**
     * @return the distance from node to the middle of the street
     */
    public double getDistanceToMiddle(EnvNode node) {
        //ensure EnvNode-Implementation implements equals() and hashCode()!!!!!
        if(street.getNodes().contains(node)) {
            return 0;
        } else {
            return deter.determineSplineDistance(node);
        }
    }

    @Override
    /**
     * @return the Distance to the end of the right lane of the street from this node.
     * Note: The lane is specified in the direction of travel! The right lane is the lane the car should drive on
     * if its course is correct. The left lane is normally the lane the car isn't driving on
     */
    public double getDistanceToRight(PhysicalObject o) {
        return this.deter.determineDistanceToRight(o);
    }

    @Override
    /**
     * @return the Distance to the end of the left lane of the street from this node.
     * Note: The lane is specified in the direction of travel! The right lane is the lane the car should drive on
     * if its course is correct. The left lane is normally the lane the car isn't driving on
     */
    public double getDistanceToLeft(PhysicalObject o) {
        return this.deter.determineDistanceToLeft(o);
    }

    @Override
    public double getDistancetoFrontLeft(PhysicalObject o){
        return this.deter.determineDistanceFrontLeft(o);
    }
    @Override
    public double getDistancetoFrontRight(PhysicalObject o){
        return this.deter.determineDistanceFrontRight(o);
    }

    @Override
    public double getGround(double x, double y, double z) {
        return this.deter.getGround(x, y, z);
    }


    @Override
    /**
     * @return true iff node is on this street
     */
    public boolean contains(EnvNode node) {
        return this.deter.contains(node);
    }

    @Override
    /**
     * @return the according EnvStreet-Object
     */
    public EnvObject getObject() {
        return this.street;
    }


    /**
     *
     * @param lastParameters
     * @param distance
     * @return the new Movement encapsulated as PedestrianStreetParameter
     */
    public PedestrianStreetParameters getMovementOfPedestrian(PedestrianStreetParameters lastParameters, double distance) {
        PedestrianStreetParameters newParams = this.deter.getMovementOfPedestrian(lastParameters, distance);

        // We need to check that we are still on the street to ensure we do not walk through the ground
        Point3D calculatedPosition = newParams.getPosition();
        double locationZOnGround = this.deter.getGround(
                calculatedPosition.getX(),
                calculatedPosition.getY(),
                calculatedPosition.getZ()
        );

        // Only update if we have a actual change
        if (locationZOnGround != calculatedPosition.getZ()) {
            Point3D positionOnGround = new Point3D(
                    calculatedPosition.getX(),
                    calculatedPosition.getY(),
                    locationZOnGround
            );

            // Update with the correct z location
            newParams = new PedestrianStreetParameters(
                    newParams.isCrossing(),
                    positionOnGround,
                    newParams.isDirection(),
                    newParams.isLeftPavement()
            );
        }

        return newParams;
    }

    /**
     * initialises the first position and moving parameters of a pedestrian and spawns him onto a random spline
     * @return
     */
    public PedestrianStreetParameters spawnPedestrian() {
        return this.deter.spawnPedestrian();
    }

    /**
     * @param rightLane
     * @param p
     * @return nearest point on the corresponding lane to p
     */
    public Point3D spawnCar(boolean rightLane, Point3D p) {
        return this.deter.spawnCar(rightLane, p);
    }

    public SplineDeterminator getDeterminator() {
        return this.deter;
    }
}
