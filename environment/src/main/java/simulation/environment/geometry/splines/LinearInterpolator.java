package simulation.environment.geometry.splines;


import commons.map.ControllerNode;
import commons.map.IControllerNode;
import commons.simulation.PhysicalObject;
import javafx.geometry.Point3D;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import simulation.environment.pedestrians.PedestrianStreetParameters;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by lukas on 21.01.17.
 *
 * This class uses linear interpolation to interpolate a street
 * see http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html for details on the math
 */
public class LinearInterpolator implements Spline {
    protected static final long PAVEMENT_ID = -1l;

    protected static final double PAVEMENT_WIDTH = 2;

//    protected static final double PAVEMENT_WIDTH = 1;
    //set Precision to 1 nm
    public static final double precision = Math.pow(10, -9);

    //an enum for directions
    protected enum Direction{
        LEFT, MIDDLE, RIGHT;
    }

    /**
     * construct borders and pavements of a street by parallel movement
     */

    protected class PointContainer {
        Point3D p;
        Point3D n1;
        Point3D n2;

        Point3D pavement1;
        Point3D pavement2;

        Point3D normalVec;

        protected PointContainer(Point3D p, Point3D difference, double streetWidth, double pavementWidth) {
            this.p = p;
            initPointContainer(difference, streetWidth, pavementWidth);
        }

        //TODO: compute pavement start and endPoints correct!
        protected void initPointContainer(Point3D difference, double streetWidth, double pavementWidth) {
            //normalize the difference vector
            ;

            //compute the normal-vector
            this.normalVec = new Point3D(-1 *difference.getY(), difference.getX(), 0);
            this.normalVec = this.normalVec.normalize();
            //construct other points by parallel movement
            Point3D n1 = this.p.add(normalVec.multiply(0.5*streetWidth));
            Point3D pavement1 = this.p.add(normalVec.multiply(0.5*streetWidth + 0.5*pavementWidth));
            Point3D n2 = this.p.add(normalVec.multiply(-0.5*streetWidth));

            Point3D pavement2 = this.p.add(normalVec.multiply(-0.5*streetWidth - 0.5*pavementWidth));
            //ensure that n1 is always the left lane and n2 the right lane (pavements accordingly
            if(n1.getX() < p.getX() || (n1.getX() == p.getX() && n1.getY() > p.getY())) {
                this.n1 = n1;
                this.pavement1 = pavement1;
                this.n2 = n2;
                this.pavement2 = pavement2;
            } else {
                this.n1 = n2;
                this.pavement1 = pavement2;
                this.n2 = n1;
                this.pavement2 = pavement1;
            }
        }
    }

    protected Point3D difference;

    protected Point3D difference2D;

    protected PointContainer p3D1;
    protected PointContainer p3D2;

    //some computations have to be made in 2D-Space
    protected PointContainer p2D1;
    protected PointContainer p2D2;

    protected double streetWidth;

    protected long osmId1;
    protected long osmId2;

    protected LinearInterpolator leftPavement;
    protected LinearInterpolator rightPavement;

    protected boolean withPavements;

    /**
     *
     * @param p1 first Point of the linear interpolation
     * @param p2 second Point of the linear interpolation
     * @param streetWidth width of the street
     * @param osmId1 osmId of the first point
     * @param osmId2 osmId of the second point
     * @param withPavements true if interpolator should generate pavements
     */
    public LinearInterpolator(Point3D p1, Point3D p2, double streetWidth, long osmId1, long osmId2, boolean withPavements) {
        init(p1, p2, streetWidth, osmId1, osmId2, withPavements, PAVEMENT_WIDTH);
        if(withPavements) {
            this.leftPavement = new LinearInterpolator(this.p3D1.pavement1, this.p3D2.pavement1, PAVEMENT_WIDTH, PAVEMENT_ID, PAVEMENT_ID, false);
            this.rightPavement = new LinearInterpolator(this.p3D1.pavement2, this.p3D2.pavement2, PAVEMENT_WIDTH, PAVEMENT_ID, PAVEMENT_ID, false);
        }
    }

    public LinearInterpolator(Point3D p1, Point3D p2, double streetWidth, long osmId1, long osmId2, boolean withPavements, double pavementWidth) {
        init(p1, p2, streetWidth, osmId1, osmId2, withPavements, pavementWidth);
        if(withPavements) {
            this.leftPavement = new LinearInterpolator(this.p3D1.pavement1, this.p3D2.pavement1, pavementWidth, PAVEMENT_ID, PAVEMENT_ID, false);
            this.rightPavement = new LinearInterpolator(this.p3D1.pavement2, this.p3D2.pavement2, pavementWidth, PAVEMENT_ID, PAVEMENT_ID, false);
        }
    }

    private void init(Point3D p1, Point3D p2, double streetWidth, long osmId1, long osmId2, boolean withPavements, double pavementWidth) {
        this.difference = computeDifference(p2, p1);
        this.difference2D = computeDifference(new Point3D(p2.getX(), p2.getY(), 0), new Point3D(p1.getX(), p1.getY(), 0));
        this.streetWidth = streetWidth;
        this.p3D1 = new PointContainer(p1, difference, streetWidth, pavementWidth);
        this.p3D2 = new PointContainer(p2, difference, streetWidth, pavementWidth);

        this.p2D1 = new PointContainer(new Point3D(p1.getX(), p1.getY(), 0), difference2D, streetWidth, pavementWidth);
        this.p2D2 = new PointContainer(new Point3D(p2.getX(), p2.getY(), 0), difference2D, streetWidth, pavementWidth);

        this.osmId1 = osmId1;
        this.osmId2 = osmId2;
        this.withPavements = withPavements;
    }

    /**
     *
     * @param p2
     * @param p1
     * @return p3D2 - p3D1
     */
    protected Point3D computeDifference(Point3D p2, Point3D p1) {
        return p2.subtract(p1);
    }

    @Override
    public Point3D computePoint(double t) {
        Point3D result = p3D1.p.add(difference.multiply(t));
        return result;
    }


    @Override
    public double computeDistanceToMiddle(Point3D p) {
        return computeDistance(p, Direction.MIDDLE, true);
    }

    @Override
    public double computeDistanceToLeft(Point3D p) {
        return computeDistance(p, Direction.LEFT, true);
    }

    @Override
    public double computeDistanceToLeft(PhysicalObject o) {
        RealVector left = o.getGeometryPos().add(o.getGeometryRot().operate(new ArrayRealVector(new double[] {-0.5 * o.getWidth(), 0.0, 0.0})));
        RealVector right = o.getGeometryPos().add(o.getGeometryRot().operate(new ArrayRealVector(new double[] {0.5 * o.getWidth(), 0.0, 0.0})));
        RealVector middle = o.getGeometryPos();

        Point3D lP = new Point3D(left.getEntry(0),left.getEntry(1),left.getEntry(2));
        Point3D rP = new Point3D(right.getEntry(0),right.getEntry(1),right.getEntry(2));
        Point3D mP = new Point3D(middle.getEntry(0),middle.getEntry(1),middle.getEntry(2));

        double lDist = computeDistance(lP, Direction.LEFT, Direction.RIGHT, false);
        double rDist = computeDistance(rP, Direction.LEFT, Direction.RIGHT, false);

        if(lDist < rDist) {
            return computeDistance(mP, Direction.LEFT, Direction.RIGHT, false);
        } else {
            return computeDistance(mP, Direction.LEFT, Direction.LEFT, false);
        }
    }

    @Override
    public double computeDistanceToRight(Point3D p) {
        return computeDistance(p, Direction.RIGHT, true);
    }

    @Override
    public double computeDistanceToRight(PhysicalObject o) {
        RealVector left = o.getGeometryPos().add(o.getGeometryRot().operate(new ArrayRealVector(new double[] {-0.5 * o.getWidth(), 0.0, 0.0})));
        RealVector right = o.getGeometryPos().add(o.getGeometryRot().operate(new ArrayRealVector(new double[] {0.5 * o.getWidth(), 0.0, 0.0})));
        RealVector middle = o.getGeometryPos();

        Point3D lP = new Point3D(left.getEntry(0),left.getEntry(1),left.getEntry(2));
        Point3D rP = new Point3D(right.getEntry(0),right.getEntry(1),right.getEntry(2));
        Point3D mP = new Point3D(middle.getEntry(0),middle.getEntry(1),middle.getEntry(2));


        double lDist = computeDistance(lP, Direction.RIGHT, Direction.RIGHT, false);
        double rDist = computeDistance(rP, Direction.RIGHT, Direction.RIGHT, false);

        if(rDist < lDist) {
            return computeDistance(mP, Direction.RIGHT, Direction.RIGHT, false);
        } else {
            return computeDistance(mP, Direction.RIGHT, Direction.LEFT, false);
        }

    }

    @Override
    public boolean isOnStreet(Point3D p) {
        return Math.abs(streetWidth - (computeDistance(p,Direction.RIGHT, false) + computeDistance(p, Direction.LEFT, false))) <= precision
                && p.getZ() == getFloorForPoint(p);
    }

    @Override
    public boolean isOnPavement(Point3D p) {
        return leftPavement.isOnStreet(p) || rightPavement.isOnStreet(p);
    }



    private boolean isOnPavement(Point3D p, boolean leftPavement) {
        if(leftPavement) {
            return this.leftPavement.isOnStreet(p);
        } else {
            return rightPavement.isOnStreet(p);
        }
    }

    /**
     * @param p
     * @param d
     * @return the distance for a given point p and direction d
     */
    protected double computeDistance(Point3D p, Direction d, boolean in3D) {
        Point3D s;
        Direction lane = determineLaneOfPoint(p);

        if(d == Direction.LEFT) {
            if(lane == Direction.RIGHT) {
                s = in3D ? p3D1.n1 : p2D1.n1;
            } else {
                s = in3D ? p3D1.n2 : p2D1.n2;
            }

        } else if(d == Direction.MIDDLE) {
            s = in3D ? p3D1.p : p2D1.p;
        } else {
            if(lane == Direction.RIGHT) {
                s = in3D ? p3D1.n2 : p2D1.n2;
            } else {
                s = in3D ? p3D1.n1 : p2D1.n1;
            }

        }

        double squaredNormOfPointsOnLine = getSquaredNorm(this.difference);
        Point3D differenceOfP1AndP = computeDifference(s, p);

        double numerator = getSquaredNorm(differenceOfP1AndP) * squaredNormOfPointsOnLine - Math.pow(differenceOfP1AndP.dotProduct(difference),2);
        double denominator = squaredNormOfPointsOnLine;

        // Prevent invalid math operations here to avoid exceptions
        if (numerator < 0.0) {
            numerator = 0.0;
        }

        if (denominator <= 0.0) {
            denominator = 1E-10;
        }

        return Math.sqrt(numerator/denominator);
    }

    private double computeDistance(Point3D p, Direction d, Direction lane, boolean in3D) {
        Point3D s;

        if(d == Direction.LEFT) {
            if(lane == Direction.RIGHT) {
                s = in3D ? p3D1.n1 : p2D1.n1;
            } else {
                s = in3D ? p3D1.n2 : p2D1.n2;
            }

        } else if(d == Direction.MIDDLE) {
            s = in3D ? p3D1.p : p2D1.p;
        } else {
            if(lane == Direction.RIGHT) {
                s = in3D ? p3D1.n2 : p2D1.n2;
            } else {
                s = in3D ? p3D1.n1 : p2D1.n1;
            }

        }

        double squaredNormOfPointsOnLine = getSquaredNorm(this.difference);
        Point3D differenceOfP1AndP = computeDifference(s, p);

        double numerator = getSquaredNorm(differenceOfP1AndP) * squaredNormOfPointsOnLine - Math.pow(differenceOfP1AndP.dotProduct(difference),2);
        double denominator = squaredNormOfPointsOnLine;

        return Math.sqrt(numerator/denominator);
    }

    /**
     * @param p
     * @return the squared euclidean norm of given point p
     */
    protected double getSquaredNorm(Point3D p) {
        return Math.pow(p.getX(),2) + Math.pow(p.getY(), 2) + Math.pow(p.getZ(), 2);
    }

    /**
     * @param p
     * @return returns t for the nearest point on the spline
     */
    public double computeT(Point3D p) {
        return computeT(p, true);
    }

    public double computeT(Point3D p, boolean in3D) {
        Point3D tmpP;
        Point3D tmpDifference;
        Point3D tmpPC;

        if(in3D) {
            tmpP = p;
            tmpDifference = this.difference;
            tmpPC = p3D1.p;
        } else {
            tmpP = new Point3D(p.getX(), p.getY(), 0);
            tmpDifference = difference2D;
            tmpPC = p2D1.p;
        }
        Point3D differenceOfp1AndP = computeDifference(tmpPC, tmpP);
        return -1*(differenceOfp1AndP.dotProduct(tmpDifference) / getSquaredNorm(tmpDifference));
    }

    @Override
    public double getFloorForPoint(Point3D p) {
        return computePoint(computeT(p, false)).getZ();
    }

    @Override
    @Deprecated
    public ArrayList<IControllerNode> convertToControllerList() {
        ArrayList<IControllerNode> result = new ArrayList<>();
        Point3D normDiff = difference.normalize().multiply(IControllerNode.INTERPOLATION_DISTANCE);

        result.add(new ControllerNode(p3D1.p, osmId1));
        double distance = p3D1.p.distance(p3D2.p);


        double loops = distance / IControllerNode.INTERPOLATION_DISTANCE;
        loops = Math.floor(loops);

        Point3D tmpP = p3D1.p;
        for(int i = 0; i < loops; i++) {
            tmpP = tmpP.add(normDiff);
            result.add(new ControllerNode(tmpP, IControllerNode.INTERPOLATED_NODE));
        }

        result.add(new ControllerNode(p3D2.p, osmId2));
        return result;
    }

    @Override
    public Point3D getDifference() {
        return this.difference;
    }

    @Override
    public Point3D getP1() { return this.p3D1.p;}

    @Override
    public Point3D getP2() { return this.p3D2.p;}

    @Override
    public Point3D getBorder(boolean left, boolean isP1) {
        PointContainer tmp;
        if(isP1) {
            tmp = p3D1;
        } else {
            tmp = p3D2;
        }

        if(left) {
            return tmp.n1;
        } else {
            return tmp.n2;
        }
    }

    public ArrayList<Point3D> getAllBorders() {
        ArrayList<Point3D> result = new ArrayList<>();
        result.add(getBorder(false, false));
        result.add(getBorder(false, true));
        result.add(getBorder(true, false));
        result.add(getBorder(true, true));

        if(this.leftPavement != null) {
            result.addAll(leftPavement.getAllBorders());
        }

        if(this.rightPavement != null) {
            result.addAll(rightPavement.getAllBorders());
        }

        return result;
    }


    /**
     * @param p
     * @return the lane of a given Point
     */
    //TODO: check back with other groups
    private Direction determineLaneOfPoint(Point3D p) {
        Point3D p0 = computePoint(computeT(p));
        if(p0.getX() == p.getX() && p0.getY() == p.getY()) {
            return Direction.MIDDLE;
        }

        if(p.getX() < p0.getX()) {
            return Direction.LEFT;
        } else if(p.getX() == p0.getX() && p.getY() > p0.getY()) {
            return Direction.LEFT;
        } else {
            return Direction.RIGHT;
        }
    }

    /**
     *
     * @param lastResult
     * @param distance
     * @return a container which encapsulates the new movement of the pedestrian
     */
    @Override
    public PedestrianStreetParameters computePointForPedestrian(PedestrianStreetParameters lastResult, double distance) {
        Point3D position = lastResult.getPosition();
        boolean isCrossing = lastResult.isCrossing();
        boolean isDirection = lastResult.isDirection();
        boolean leftPavement = lastResult.isLeftPavement();

        if(!isCrossing) {
            return walkNormal(position, distance, isDirection, leftPavement);
        } else {
            return crossStreet(position, distance, leftPavement);
        }

    }

    /**
     * @param position
     * @param distance
     * @param leftPavement
     * @return the next point if the pedestrian crosses the street in dependence of his position and the pavement he is supposed to be walking on
     * Note: distance(position, next point) = distance
     */
    private PedestrianStreetParameters crossStreet(Point3D position, double distance, boolean leftPavement) {
        LinearInterpolator pavement;

        if(leftPavement) {
            pavement = this.rightPavement;
        } else {
            pavement = this.leftPavement;
        }

        Point3D result = position.add(p3D1.normalVec.normalize().multiply(distance));

        // distance to other pavement has to become smaller if the pedestrian crosses the street
        if(pavement.computeDistanceToMiddle(result) > pavement.computeDistanceToMiddle(position)) {
            if(isOnPavement(result,leftPavement)) {
                return new PedestrianStreetParameters(false, result, new Random().nextBoolean(), !leftPavement);
            } else {
                result = position.subtract(p3D1.normalVec.normalize().multiply(distance));
            }
        }
        //since the pedestrian continues to cross the street direction doesn't matter
        return new PedestrianStreetParameters(true, result, false, leftPavement);
    }

    /**
     * @param position
     * @param distance
     * @param isDirection
     * @param leftPavement
     * @return the new Position if the pedestrian continues to walk on his pavement.
     * Note: If a pedestrian reaches the end of the spline he turns around
     */
    private PedestrianStreetParameters walkNormal(Point3D position, double distance, boolean isDirection, boolean leftPavement) {
        LinearInterpolator pavement;

        if(leftPavement) {
            pavement = this.leftPavement;
        } else {
            pavement = this.rightPavement;
        }

        Point3D result = pavement.getPointWithDistance(position, distance, isDirection);
        boolean resultDir = isDirection;

        if(getDistanceFromInterpolationPoint(isDirection, result) >= Math.sqrt(getSquaredNorm(pavement.getDifference()))) {
            result = pavement.getPointWithDistance(position, distance, !isDirection);
            resultDir = !isDirection;

            if(getDistanceFromInterpolationPoint(resultDir, result) >= Math.sqrt(getSquaredNorm(pavement.getDifference()))) {
                return new PedestrianStreetParameters(false, position, resultDir, leftPavement);
            }
        }

        return new PedestrianStreetParameters(false, result, resultDir, leftPavement);
    }

    /**
     * @param p1
     * @param distance
     * @param isDirection
     * @return a point p such that distance(p3D1, p) = distance holds and in dependence of a direction
     */
    public Point3D getPointWithDistance(Point3D p1, double distance, boolean isDirection) {
        Point3D movement = this.difference.normalize().multiply(distance);
        if(isDirection) {
            return p1.add(movement);
        } else {
            return p1.subtract(movement);
        }
    }

    /**
     * @param isP1
     * @param p
     * @return the distance of p to p3D1 (or p3D2)
     */
    public double getDistanceFromInterpolationPoint(boolean isP1, Point3D p) {
        if(isP1) {
            return this.p3D1.p.distance(p);
        } else {
            return this.p3D2.p.distance(p);
        }
    }

    public LinearInterpolator getPavement(boolean isLeft) {
        if(isLeft) {
            return this.leftPavement;
        } else {
            return this.rightPavement;
        }
    }

    @Override
    public Point3D spawnCar(boolean rightLane, Point3D p) {
        Point3D endPoint;

        if(rightLane) {
            endPoint = this.p3D1.n2;
        } else {
            endPoint = this.p3D1.n1;
        }

        Point3D movement = endPoint.subtract(this.p3D1.p);
        Point3D movementToMiddle = movement.multiply(0.5);

        Point3D pointOnMiddleLane = computePoint(computeT(p));
        return pointOnMiddleLane.add(movementToMiddle);
    }
}
