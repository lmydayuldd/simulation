package simulation.environment.geometry.height;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import org.apache.commons.math3.distribution.NormalDistribution;
import simulation.environment.visualisationadapter.interfaces.EnvBounds;

import java.util.ArrayList;

/**
 * Created by lukas on 16.02.17.
 *
 * A class that generates a z-Coordinate using Concentric Circles around the midpoint of the Environment.
 * All points that lie on the same circle share the same height. Circles are generated in a fixed intervals
 * and for each interval a height difference can be generated randomly or static. Heights of points between
 * two intervals are computed by a linear interpolation between those two intervals.
 *
 * Exception is the first intervall from the midpoint to the first circle. To avoid a pyramide-like
 * form all points within the first circle share the same height. Thus no interpolation is computed
 * in the first circle
 */
public class ConcentricCircleGenerator implements HeightGenerator{
    private static ConcentricCircleGenerator instance;

    //for testing purposes
    public static final double fixedSlope = 0.02;
    private static final double fixedStartGround = 1000;

    //set intervalLength to 10 meters
    public static final double intervalLength = 10;

    //set meanSlope to 0 %
    private static final double meanSlope = 0;

    //set variance to 10 %
    //Note: real maximum slope in the whole world is 35%
    //by using a normal distribution the probability of getting a slope
    // which is higher than 30% is really small (around 0.27 %)
    private static final double slopeVar = 0.1;

    public static ConcentricCircleGenerator getInstance() {
        return instance;
    }

    public static void init(EnvBounds bounds) {
        instance = new ConcentricCircleGenerator(bounds, ConcentricCircleGenerator.intervalLength, false);
    }

    public static void init(EnvBounds bounds, boolean fixedSlopes) {
        instance = new ConcentricCircleGenerator(bounds, ConcentricCircleGenerator.intervalLength, fixedSlopes);
    }

    private EnvBounds bounds;

    private Point3D midPoint3D;
    private Point2D midPoint2D;

    private double length;

    private ArrayList<ConcentricCircle> circles;

    private int numberOfIntervals;

    private NormalDistribution normalDist;

    private boolean fixedSlopes;

    /**
     * simple container for concentric circles
     */
    private class ConcentricCircle {
        //slope beginning at the circle
        public double slope;
        //height of the circle
        public double height;

        public ConcentricCircle(double slope, double height) {
            this.slope = slope;
            this.height = height;
        }
    }



    private ConcentricCircleGenerator(EnvBounds bounds, double intervalLength, boolean fixedSlopes) {
        this.bounds = bounds;
        this.length = intervalLength;
        this.normalDist = new NormalDistribution(meanSlope, slopeVar);
        this.fixedSlopes = fixedSlopes;
        initMidPoint();
        initIntervals();
    }

    private void initMidPoint() {
        Point2D maxPoint = new Point2D(this.bounds.getMaxX(), this.bounds.getMaxY());
        Point2D minPoint = new Point2D(this.bounds.getMinX(), this.bounds.getMinY());

        this.midPoint2D = maxPoint.midpoint(minPoint);

        //add 1 to ensure last interval is constructed
        numberOfIntervals = (int) (Math.ceil(midPoint2D.distance(minPoint) / intervalLength)) + 1;

        this.midPoint3D = new Point3D(midPoint2D.getX(), midPoint2D.getY(),fixedSlopes ? fixedStartGround : Math.random());
    }

    private void initIntervals() {
        this.circles = new ArrayList<>();

        //ensure there is no height difference between midpoint and first circle
        this.circles.add(0,new ConcentricCircle(0.d, this.midPoint3D.getZ()));

        //ensure first circle has height of midpoint
        this.circles.add(1,new ConcentricCircle(fixedSlopes ? fixedSlope : normalDist.sample(), this.midPoint3D.getZ()));

        for(int i = 2; i < numberOfIntervals; i++) {
            double slope = circles.get(i-1).slope;
            double lastHeight = circles.get(i-1).height;

            double difference = slope * this.length;

            double newHeight = lastHeight + difference;

            //check for negative heights
            if(newHeight < 0) {
                difference *= -1;
                circles.get(i - 1).slope *= -1;
                newHeight = lastHeight + difference;
            }

            double newSlope = fixedSlopes ? fixedSlope : Math.abs(normalDist.sample());


            //if height is 0 set slope to a positive value
            if(circles.get(i -1).height != 0 && !fixedSlopes) {
                //change sign of slope with a probability of ~0.25
                boolean slopeSwitch = Math.random() > 0.75;

                //get sign of last slope (set to 1 if last slope is 0
                double signOfLastSlope =  Math.signum(circles.get(i -1).slope) != 0 ? Math.signum(circles.get(i -1).slope) : 1;

                //switch sign if slopeSwitch is true
                double newSign = slopeSwitch ? signOfLastSlope * -1 : signOfLastSlope;
                newSlope *= newSign;
            }

            this.circles.add(i,new ConcentricCircle(newSlope, newHeight));

        }
    }

    @Override
    public double getGround(double x, double y) {
        int index = getCircleIndex(x, y);
        if(index >= this.circles.size()) {
            if(index - this.circles.size() - 1 > 10) {
                throw new IllegalArgumentException("The coordinates aren't part of the environment");
            } else {
                index = this.circles.size() - 1;
            }
        }
        double heightOfCircle = circles.get(index).height;
        double slope = circles.get(index).slope;
        double differenceOfNextCircleToMidpoint = this.length * index;

        double difference = (new Point2D(x, y).distance(this.midPoint2D) - differenceOfNextCircleToMidpoint) * slope;
        return (heightOfCircle + difference);
    }

    @Override
    public double[][] toHeightMap() {
        double[][] result = new double[3][this.circles.size()];
        for(int i = 0; i < this.circles.size(); i++) {
         ConcentricCircle circle = circles.get(i);
            result[0][i] = circle.height;
            result[1][i] = circle.slope;
            result[2][i] = this.length;
        }
        return result;
    }


    public int getCircleIndex(double x, double y) {
        Point2D point = new Point2D(x, y);
        double distance = point.distance(this.midPoint2D);

        //under the assumption that all variables are defined in km
        double factor = 1 / this.length;

        double normDist = distance * factor;
        double normLength = this.length * factor;
        //compute index of next inner circle
        int index = (int) (Math.floor(normDist / normLength));
        return index;
    }

    public int getNumberOfIntervals() {
        return this.numberOfIntervals;
    }
}
