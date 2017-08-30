package simulation.environment.geometry.osmadapter;

import commons.simulation.PhysicalObject;
import javafx.geometry.Point3D;
import simulation.environment.geometry.splines.LinearInterpolator;
import simulation.environment.geometry.splines.Spline;
import simulation.environment.pedestrians.PedestrianStreetParameters;
import simulation.environment.visualisationadapter.interfaces.EnvBounds;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

/**
 * Created by lukas on 22.01.17.
 *
 * A street of n Nodes consists of n - 1 splines. This class is used to get the right spline for a given operation
 */
public abstract class SplineDeterminator {

    /**
     * A unique key for a spline
     */
    protected class Key {
        Point3D p1;
        Point3D p2;

        protected Key(Point3D p1, Point3D p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!p1.equals(key.p1)) return false;
            return p2.equals(key.p2);

        }

        @Override
        public int hashCode() {
            int result = p1.hashCode();
            result = 31 * result + p2.hashCode();
            return result;
        }
    }

    protected EnvStreet street;

    protected HashMap<Key, Spline> splines;

    protected EnvBounds bounds;

    public SplineDeterminator(EnvStreet street) {
        this.street = street;
        this.splines = new HashMap<>();
    }

    protected abstract void initBounds();

    /**
     * @param o
     * @return the distance to the middle spline
     */
    public double determineSplineDistance(PhysicalObject o) {
        Point3D p = new Point3D(o.getGeometryPos().getEntry(0),o.getGeometryPos().getEntry(1),o.getGeometryPos().getEntry(2));
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), p);
        return s.computeDistanceToMiddle(p);
    }

    /**
     * @param n
     * @return the distance to the middle spline
     */
    public double determineSplineDistance(EnvNode n) {
        Point3D p = n.getPoint();
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), p);
        return s.computeDistanceToMiddle(p);
    }


    /**
     * @param o
     * @return the distance to the left spline
     */
    public double determineDistanceToLeft(PhysicalObject o) {
        Point3D p = new Point3D(o.getGeometryPos().getEntry(0),o.getGeometryPos().getEntry(1),o.getGeometryPos().getEntry(2));
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), p);
        return s.computeDistanceToLeft(o);
    }

    /**
     * @param o
     * @return the distance to the right spline
     */
    public double determineDistanceToRight(PhysicalObject o) {
        Point3D p = new Point3D(o.getGeometryPos().getEntry(0),o.getGeometryPos().getEntry(1),o.getGeometryPos().getEntry(2));
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), p);
        return s.computeDistanceToRight(o);
    }

    /**
     * @param n
     * @return true iff n is on the street or pavement of the street
     */
    public boolean contains(EnvNode n) {
        boolean xBounds = n.getX().doubleValue() <= bounds.getMaxX() && n.getX().doubleValue() >= bounds.getMinX();
        boolean yBounds = n.getY().doubleValue() <= bounds.getMaxY() && n.getY().doubleValue() >= bounds.getMinY();
        boolean zBounds = n.getZ().doubleValue() <= bounds.getMaxZ() && n.getZ().doubleValue() >= bounds.getMinZ();

        if(!(xBounds && yBounds && zBounds)) {
            return false;
        }

        Point3D p = n.getPoint();
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), p);
        return s.isOnStreet(p) || s.isOnPavement(p);
    }

    /**
     * @param x
     * @param y
     * @param lastKnownZ
     * @return the corresponding z-Coordinate for given x, y and the last known z-Coordinate
     */
    public double getGround(double x, double y, double lastKnownZ) {
        Point3D p = new Point3D(x, y, lastKnownZ);
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), p);
        return s.getFloorForPoint(p);
    }

    /**
     *
     * @param keys
     * @param p
     * @return the element of keys with Minimum Distance to p
     */

    private Spline getMinimumSplineForSetAndPoints(Set<Key> keys, Point3D p) {
        double minDist = Double.MAX_VALUE;
        Spline result = null;

        if (keys.size() > 0) {
            result = this.splines.get(keys.toArray()[0]);
        }

        for(Key k : keys) {
            double dist = this.splines.get(k).computeDistanceToMiddle(p);
            if(dist < minDist) {
                result = this.splines.get(k);
            }
        }
        return result;
    }


    /**
     * @return corresponding EnvStreet
     */
    public EnvStreet getStreet() {
        return this.street;
    }

    /**
     *
     * @param lastParameters
     * @param distance
     * @return the new Movement encapsulated as PedestrianStreetParameter
     */
    public PedestrianStreetParameters getMovementOfPedestrian(PedestrianStreetParameters lastParameters, double distance) {
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), lastParameters.getPosition());
        return s.computePointForPedestrian(lastParameters, distance);
    }

    /**
     * initialises the first position and moving parameters of a pedestrian and spawns him onto a random spline
     * @return
     */
    public PedestrianStreetParameters spawnPedestrian() {
        Random r = new Random();
        int splineIndex = r.nextInt(this.splines.keySet().size());
        Spline s = new ArrayList<Spline>(this.splines.values()).get(splineIndex);
        double t = r.nextDouble();
        boolean isCrossing = r.nextBoolean();
        boolean direction = r.nextBoolean();
        boolean leftPavement = r.nextBoolean();

        LinearInterpolator pavement = s.getPavement(leftPavement);
        Point3D position = pavement.computePoint(t);

        return new PedestrianStreetParameters(isCrossing, position, direction, leftPavement);

    }

    public Point3D spawnCar(boolean rightLane, Point3D p) {
        Spline s = getMinimumSplineForSetAndPoints(this.splines.keySet(), p);
        return s.spawnCar(rightLane, p);
    }

    public Spline getSplineForPoints(Point3D p1, Point3D p2) {
        return this.splines.get(new Key(p1, p2));
    }
}
