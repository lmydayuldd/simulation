package simulation.environment.geometry.splines;

import javafx.geometry.Point3D;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import simulation.environment.geometry.osmadapter.GeomStreet;
import simulation.environment.pedestrians.PedestrianStreetParameters;
import simulation.environment.visualisationadapter.interfaces.EnvNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StreetInterpolator {

    private static final double TOLERANCE = 1E-9;

    private static Line getLineBetweenPoints(Point3D p1, Point3D p2) {
        Vector2D v1 = new Vector2D(p1.getX(), p1.getY());
        Vector2D v2 = new Vector2D(p2.getX(), p2.getY());
        return new Line(v1, v2, TOLERANCE);
    }

    private Point3D getIntersectionPoint(Line l1, Line l2) {
        Vector2D intersection = l1.intersection(l2);
        return new Point3D(
                intersection.getX(),
                intersection.getY(),
                0
        );
    }

    private class MovementBetweenSplinePoints {
        private Point3D mBasePointPreLeft;
        private Point3D mBasePointNextLeft;

        private Point3D mBasePointPreRight;
        private Point3D mBasePointNextRight;

        private Point3D mDirectionVector;
        private Point3D mOrthogonalVector;

        private Point3D mCenterOfMovement;

        private Line mLeftLine;
        private Line mRightLine;

        private boolean mPreShorterSideIsLeft;
        private boolean mNextShorterSideIsLeft;

        private Point3D mPreLastCrossingPoint;
        private Point3D mNextLastCrossingPoint;

        MovementBetweenSplinePoints(Point3D basePre, Point3D baseNext) {
            mDirectionVector = baseNext.subtract(basePre).normalize();
            mDirectionVector = new Point3D(mDirectionVector.getX(), mDirectionVector.getY(), 0);
            mOrthogonalVector = new Point3D(
                    -mDirectionVector.getY(),
                    mDirectionVector.getX(),
                    0
            ).normalize();
            mCenterOfMovement = basePre.add(mDirectionVector.multiply(basePre.distance(baseNext) * 0.5));
        }
    }

    private GeomStreet mBaseStreet;
    private List<MovementBetweenSplinePoints> mSplineDirections;

    private int mMovingDirection = -1;
    private Point3D mCrossingStartPoint = null;

    private double mStreetAndPavementWidth = 8;

    public StreetInterpolator(GeomStreet base) {
        mBaseStreet = base;
        mSplineDirections = new ArrayList<>();

        precomputePavements();
    }

    public PedestrianStreetParameters spawnAtRandomLocation(Random random) {
        mMovingDirection = random.nextInt(mSplineDirections.size());
        MovementBetweenSplinePoints curMovement = mSplineDirections.get(mMovingDirection);

        double howFar = random.nextDouble();
        boolean direction = random.nextBoolean();
        boolean isOnLeftPavement = random.nextBoolean();

        Point3D curPosition;

        if (isOnLeftPavement) {
            double maxDistance = curMovement.mBasePointPreLeft.distance(curMovement.mBasePointNextLeft);
            curPosition = curMovement.mBasePointPreLeft.add(
                    curMovement.mDirectionVector.multiply(maxDistance * howFar)
            );
        } else {
            double maxDistance = curMovement.mBasePointPreRight.distance(curMovement.mBasePointNextRight);
            curPosition = curMovement.mBasePointPreRight.add(
                    curMovement.mDirectionVector.multiply(maxDistance * howFar)
            );
        }

        return new PedestrianStreetParameters(
                false,
                curPosition,
                direction,
                isOnLeftPavement
        );
    }

    private void precomputePavements() {
        List<EnvNode> envNodeList = mBaseStreet.getObject().getNodes();

        for (int i = 0; i < envNodeList.size() - 1; i++) {
            EnvNode n1 = envNodeList.get(i);
            EnvNode n2 = envNodeList.get(i + 1);

            MovementBetweenSplinePoints msp = new MovementBetweenSplinePoints(
                    n1.getPoint(),
                    n2.getPoint()
            );

            mSplineDirections.add(msp);

            msp.mBasePointPreLeft = n1.getPoint().add(msp.mOrthogonalVector.multiply(mStreetAndPavementWidth / 2));
            msp.mBasePointPreRight = n1.getPoint().add(msp.mOrthogonalVector.multiply(-mStreetAndPavementWidth / 2));
            msp.mBasePointNextLeft = n2.getPoint().add(msp.mOrthogonalVector.multiply(mStreetAndPavementWidth / 2));
            msp.mBasePointNextRight = n2.getPoint().add(msp.mOrthogonalVector.multiply(-mStreetAndPavementWidth / 2));

            msp.mLeftLine = getLineBetweenPoints(msp.mBasePointPreLeft, msp.mBasePointNextLeft);
            msp.mRightLine = getLineBetweenPoints(msp.mBasePointPreRight, msp.mBasePointNextRight);

            if (i > 0) {
                MovementBetweenSplinePoints preMsp = mSplineDirections.get(i - 1);

                Point3D leftLineIntersection = getIntersectionPoint(preMsp.mLeftLine, msp.mLeftLine);
                Point3D rightLineIntersection = getIntersectionPoint(preMsp.mRightLine, msp.mRightLine);

                preMsp.mBasePointNextLeft = leftLineIntersection;
                msp.mBasePointPreLeft = leftLineIntersection;
                preMsp.mBasePointNextRight = rightLineIntersection;
                msp.mBasePointPreRight = rightLineIntersection;

                boolean leftIsFarerAway = leftLineIntersection.distance(msp.mCenterOfMovement)
                        > rightLineIntersection.distance(msp.mCenterOfMovement);

                msp.mPreShorterSideIsLeft = leftIsFarerAway;
                msp.mNextShorterSideIsLeft = leftIsFarerAway;

                if (leftIsFarerAway) {
                    msp.mPreLastCrossingPoint = msp.mBasePointPreRight
                            .add(msp.mOrthogonalVector.multiply(mStreetAndPavementWidth));
                    preMsp.mNextLastCrossingPoint = preMsp.mBasePointNextRight
                            .add(preMsp.mOrthogonalVector.multiply(mStreetAndPavementWidth));
                } else {
                    msp.mPreLastCrossingPoint = msp.mBasePointPreLeft
                            .add(msp.mOrthogonalVector.multiply(-mStreetAndPavementWidth));
                    preMsp.mNextLastCrossingPoint = preMsp.mBasePointNextLeft
                            .add(preMsp.mOrthogonalVector.multiply(-mStreetAndPavementWidth));
                }
            }
        }
    }

    public PedestrianStreetParameters calculateNewMovement(PedestrianStreetParameters preParams, double distance) {
        Point3D curPosition = preParams.getPosition();
        boolean directionForward = preParams.isDirection();
        boolean isCrossingTheStreet = preParams.isCrossing();
        boolean isOnLeftPavement = preParams.isLeftPavement();

        if (mMovingDirection == -1) {
            directionForward = true;
            curPosition = mBaseStreet.getObject().getNodes().get(0).getPoint();
            mMovingDirection = 0;
        }

        MovementBetweenSplinePoints curMovement = mSplineDirections.get(mMovingDirection);

        // Test if when we cross the street if we even can cross it
        if (isCrossingTheStreet &&
                (mCrossingStartPoint != null || canCrossStreetAtPosition(curPosition, isOnLeftPavement, curMovement))) {
            Point3D crossingDirection = curMovement.mOrthogonalVector;

            if (isOnLeftPavement) {
                crossingDirection = crossingDirection.multiply(-1);
            }

            // Set the starting and ending points
            if (mCrossingStartPoint == null) {
                mCrossingStartPoint = curPosition;
            }

            // Move as far as we can and test how far we have gone
            Point3D endPosition = curPosition.add(crossingDirection.multiply(distance));
            double newDistance = mCrossingStartPoint.distance(endPosition);

            // If we have passed our destination point we will stop crossing the street
            // and reset all saved local helper variables
            if (newDistance >= mStreetAndPavementWidth) {
                endPosition = mCrossingStartPoint.add(crossingDirection.multiply(mStreetAndPavementWidth));
                isOnLeftPavement = !isOnLeftPavement;
                mCrossingStartPoint = null;
                isCrossingTheStreet = false;
            }

            return new PedestrianStreetParameters(
                    isCrossingTheStreet,
                    endPosition,
                    directionForward,
                    isOnLeftPavement
            );
        }

        Point3D direction = curMovement.mDirectionVector;
        Point3D targetingPoint = getTargetingPoint(directionForward, isOnLeftPavement, curMovement);

        if (!directionForward) {
            direction = direction.multiply(-1);
        }

        // Second: Move as far as possible to the specific node
        Point3D newPosition = curPosition.add(direction.multiply(distance));

        double preDistance = targetingPoint.distance(curPosition);
        double newDistance = targetingPoint.distance(newPosition);

        if (newDistance >= preDistance) {
            newPosition = targetingPoint;

            if ((mMovingDirection == 0 && !directionForward)
                    || (mMovingDirection == mSplineDirections.size() - 1 && directionForward)) {
                directionForward = !directionForward;
            } else {
                mMovingDirection = mMovingDirection + (directionForward ? 1 : -1);
            }
        }

        return new PedestrianStreetParameters(
                false,
                newPosition,
                directionForward,
                isOnLeftPavement
        );
    }

    private Point3D getTargetingPoint(boolean directionForward, boolean isOnLeftPavement,
                                      MovementBetweenSplinePoints curMovement) {
        if (directionForward) {
            return isOnLeftPavement ? curMovement.mBasePointNextLeft : curMovement.mBasePointNextRight;
        } else {
            return isOnLeftPavement ? curMovement.mBasePointPreLeft : curMovement.mBasePointPreRight;
        }
    }

    private boolean isInNonCrossingZone(Point3D curPosition, Point3D endPoint, Point3D lastCrossingPoint) {
        double distanceNonCrossing = endPoint.distance(lastCrossingPoint);
        double distanceToEnd = endPoint.distance(curPosition);

        return distanceToEnd <= distanceNonCrossing;
    }

    private boolean canCrossStreetAtPosition(Point3D curPosition, boolean isOnLeftPavement, MovementBetweenSplinePoints msp) {
        if (isOnLeftPavement) {
            if (msp.mPreShorterSideIsLeft && msp.mPreLastCrossingPoint != null
                    && isInNonCrossingZone(curPosition, msp.mBasePointPreLeft, msp.mPreLastCrossingPoint)) {
                return false;
            }

            if (msp.mNextShorterSideIsLeft && msp.mNextLastCrossingPoint != null
                    && isInNonCrossingZone(curPosition, msp.mBasePointNextLeft, msp.mNextLastCrossingPoint)) {
                return false;
            }
        } else {
            if (!msp.mPreShorterSideIsLeft && msp.mPreLastCrossingPoint != null
                    && isInNonCrossingZone(curPosition, msp.mBasePointPreRight, msp.mPreLastCrossingPoint)) {
                return false;
            }

            if (!msp.mNextShorterSideIsLeft && msp.mNextLastCrossingPoint != null
                    && isInNonCrossingZone(curPosition, msp.mBasePointNextRight, msp.mNextLastCrossingPoint)) {
                return false;
            }
        }

        return true;
    }
}