package simulation.environment.collision;

import commons.simulation.PhysicalObject;
import javafx.geometry.Point2D;
import org.apache.commons.math3.linear.RealVector;

import java.util.Map;

/**
 * Created by lukas on 07.03.17.
 */
@Deprecated
public class CollisionDetector {

    /**
     * @param o1
     * @param o2
     * @return true iff the two objects have a collision
     */
    @Deprecated
    public static boolean hasCollision(PhysicalObject o1, PhysicalObject o2) {
        boolean collision = false;

        for(Map.Entry<RealVector, RealVector> bound1 : o1.getBoundaryVectors()) {
            Point2D a = new Point2D(bound1.getKey().getEntry(0), bound1.getKey().getEntry(1));
            Point2D b = new Point2D(bound1.getValue().getEntry(0), bound1.getValue().getEntry(1)).subtract(a);
            for(Map.Entry<RealVector, RealVector> bound2 : o2.getBoundaryVectors()) {
                Point2D c = new Point2D(bound2.getKey().getEntry(0), bound2.getKey().getEntry(1));
                Point2D d = new Point2D(bound2.getValue().getEntry(0), bound2.getValue().getEntry(1)).subtract(c);

                collision = checkPoints(a,b,c,d);

                if(collision) {
                    break;
                }
            }
        }



        //if collision of object is already set don't negate it
        o1.setCollision(o1.getCollision() ? o1.getCollision() : collision);

        o2.setCollision(o2.getCollision() ? o2.getCollision() : collision);

        return collision;
    }

    /**
     * Note this function won't work for infinite lines use apache-math method for Line instead
     * @param a the beginning of the first line
     * @param b let e be the end of the first line then e - a = b holds
     * @param c analogous to a for the second line
     * @param d analogous to b for the second line
     * @return true iff the two lines specified by a,b,c,d intersect before they end
     */
    @Deprecated
    public static boolean checkPoints(Point2D a, Point2D b, Point2D c, Point2D d) {
        //t is always computed for one line thus both lines have to be checked
        return compute(a,b,c,d) && compute(c,d,a,b);
    }

    @Deprecated
    private static boolean compute(Point2D a, Point2D b, Point2D c, Point2D d) {
        double t;
        if(b.getX() == 0 && b.getY() == 0 && d.getX() == 0 && d.getY() == 0) {
            //a and b are points
            return a.distance(c) == 0;
        } else if(b.getX() == 0 && b.getY() == 0 ) {
            //one component of d has to be not equal to 0
            double  t1 = -1;
            double  t2 = -1;

            if(d.getX() != 0) {
                 t1 = (a.getX() - c.getX()) / d.getX();
            }


            if(d.getY() != 0){
                t2 = (a.getY() - c.getY()) / d.getY();
            }

            boolean result = false;
            if(t1 != -1 && t2 != -1) {
                result = t1 == t2 && t1 <= 1 && t1 >= 0;
            } else if (t1 != -1){
                result = t1 >= 0 && t1 <= 1 && a.getY() == c.getY();
            } else {
                result = t2 >= 0 && t2 <= 1 && a.getX() == c.getX();
            }

            return result;
        } else if(d.getX() == 0 && d.getY() == 0) {
            double t1 = -1;
            double t2 = -1;
            if(b.getX() != 0) {
                t1 = (c.getX() - a.getX()) / b.getX();
            }
            if(b.getY() != 0) {
                t2 = (c.getY() - a.getY()) / b.getY();
            }

            boolean result = false;
            if(t1 != -1 && t2 != -1) {
                result = t1 == t2 && t1 <= 1 && t1 >= 0;
            } else if(t1 != -1) {
                result = t1 >= 0 && t1 <= 1 && c.getY() == a.getY();
            } else {
                result = t2 >= 0 && t2 <= 1 && a.getX() == c.getX();
            }

            return result;
        } else if(b.getX() == 0 && d.getX() == 0) {
            //b and d move in y-direction there might be a collision if a and c have the same
            //x-coordinate
            if(a.getX() == c.getX()) {
                boolean first;
                boolean second;
                //b.getX() can't be equal to 0
                //check for direction if b.getX is positive c has to lie in front of a for
                // a collision
                if(b.getY() > 0) {
                    if(d.getY() > 0) {
                        first = a.getY() <= c.getY() && a.getY() + b.getY() >= c.getY();
                    } else {
                        //end points could overlap
                        first = a.getY() <= c.getY() + d.getY() && a.getY() + b.getY() >= c.getY() + d.getY();
                    }

                } else {
                    if(d.getY() > 0) {
                        first = a.getY() >= c.getY() + d.getY() && a.getY() + b.getY() <= c.getY() + d.getY();
                    } else {
                        first = a.getY() >= c.getY() && a.getY() + b.getY() <= c.getY();
                    }

                }

                if(d.getY() > 0) {
                    if(b.getY() > 0) {
                        second = c.getY() <= a.getY() && c.getY() + d.getY() >= a.getY();
                    } else {
                        second = c.getY() <= a.getY() + b.getY() && c.getY() + d.getY() >= a.getY() + b.getY();
                    }

                } else {
                    if(b.getY() > 0) {
                        second = c.getY() >= a.getY() + b.getY() && c.getY() - d.getY() <= a.getY() + b.getY();
                    } else {
                        second = c.getY() >= a.getY() && c.getY() - d.getY() <= a.getY();
                    }

                }

                return first || second;
            } else {
                return false;
            }

        } else if(b.getY() == 0 && d.getY() == 0){
            //analogous to previous case
            if(a.getY() == c.getY()) {
                boolean first;
                boolean second;
                //b.getX() can't be equal to 0
                //check for direction if b.getX is positive c has to lie in front of a for
                // a collision
                if(b.getX() > 0) {
                    if(d.getX() > 0) {
                        first = a.getX() <= c.getX() && a.getX() + b.getX() >= c.getX();
                    } else {
                        //end points could overlap
                        first = a.getX() <= c.getX() + d.getX() && a.getX() + b.getX() >= c.getX() + d.getX();
                    }

                } else {
                    if(d.getX() > 0) {
                        first = a.getX() >= c.getX() + d.getX() && a.getX() + b.getX() <= c.getX() + d.getX();
                    } else {
                        first = a.getX() >= c.getX() && a.getX() + b.getX() <= c.getX();
                    }

                }

                if(d.getX() > 0) {
                    if(b.getX() > 0) {
                        second = c.getX() <= a.getX() && c.getX() + d.getX() >= a.getX();
                    } else {
                        second = c.getX() <= a.getX() + b.getX() && c.getX() + d.getX() >= a.getX() + b.getX();
                    }

                } else {
                    if(b.getX() > 0) {
                        second = c.getX() >= a.getX() + b.getX() && c.getX() - d.getX() <= a.getX() + b.getX();
                    } else {
                        second = c.getX() >= a.getX() && c.getX() - d.getX() <= a.getX();
                    }

                }

                return first || second;
            } else {
                return false;
            }

        } else if(b.getX()*d.getY() != b.getY()*d.getX()) {
            double numerator = d.getY() * (a.getX() - c.getX()) - a.getY()*d.getX() + c.getY()*d.getX();
            double denominator = b.getY()*d.getX() - b.getX()*d.getY();
            t = numerator /denominator;
            return t <= 1 && t >= 0;
        } else {
            //b and d have to be equal now so there is a collision if a and b have a shorter distance
            //than the length of b or d
            double t1 = (c.getX() - a.getX()) / b.getX();
            double t2 = (c.getY() - a.getY()) / b.getY();

            double t3 = (a.getX() - c.getX()) / d.getX();
            double t4 = (a.getY() - c.getY()) / d.getY();

            boolean first = t1 == t2 && t1 >= 0 && t1 <= 1;
            boolean second = t3 == t4 && t3 >= 0 && t3 <= 1;
            return first || second;
        }
    }
}
