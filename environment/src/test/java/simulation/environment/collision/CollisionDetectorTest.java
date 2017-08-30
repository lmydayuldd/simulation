package simulation.environment.collision;

import javafx.geometry.Point2D;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertFalse;

/**
 * Created by lukas on 07.03.17.
 */
public class CollisionDetectorTest {
    @Test
    public void testHasCollision() {

//        //'normal' case
        Point2D a = new Point2D(0,0);
        Point2D b = new Point2D(4,4);

        Point2D c = new Point2D(3,0);
        Point2D d = new Point2D(-2, 1);

        Point2D d1 = new Point2D(-1,0.5);

        assertTrue(CollisionDetector.checkPoints(a,b,c,d));
        assertFalse(CollisionDetector.checkPoints(a,b,c,d1));

        //check point against line
        a = new Point2D(0,0);
        b  = new Point2D(0,0);
        c = new Point2D(-1,-1);
        d = new Point2D(2,2);
        d1 = new Point2D(-2,-2);

        assertTrue(CollisionDetector.checkPoints(a,b,c,d));
        assertFalse(CollisionDetector.checkPoints(a,b,c,d1));

        //check two points
        a = new Point2D(0,0);
        b  = new Point2D(0,0);
        c = new Point2D(0,0);
        Point2D c1 = new Point2D(1,1);
        d = new Point2D(0,0);

        assertTrue(CollisionDetector.checkPoints(a,b,c,d));
        assertFalse(CollisionDetector.checkPoints(a,b,c1,d));

        //check same directions
        a = new Point2D(0,0);
        b  = new Point2D(2,2);
        c = new Point2D(1,1);
        c1 = new Point2D(3,3);
        Point2D c2 = new Point2D(1,0);
        d = new Point2D(2,2);

        assertTrue(CollisionDetector.checkPoints(a,b,c,d));
        assertFalse(CollisionDetector.checkPoints(a,b,c1,d));
        assertFalse(CollisionDetector.checkPoints(a,b,c2, d));

        //check parallel lines
        a = new Point2D(0,0);
        b  = new Point2D(2,2);
        c = new Point2D(1,1);
        c1 = new Point2D(3,3);
        d = new Point2D(4,4);

        assertTrue(CollisionDetector.checkPoints(a,b,c,d));
        assertFalse(CollisionDetector.checkPoints(a,b,c1,d));

        //check movements in y-direction
        a = new Point2D(0,0);
        Point2D a1 = new Point2D(1,0);
        b = new Point2D(0,2);
        c = new Point2D(0,3);
        d = new Point2D(0,-1);
        d1 = new Point2D(0,1);

        assertTrue(CollisionDetector.checkPoints(a,b,c,d));
        assertFalse(CollisionDetector.checkPoints(a,b,c,d1));

        assertFalse(CollisionDetector.checkPoints(a1,b,c,d1));
        assertFalse(CollisionDetector.checkPoints(a1,b,c,d));


        a = new Point2D(0, 5);
        a1 = new Point2D(1,1);
        b = new Point2D(0, 0);
        c = new Point2D(0, 0);
        d = new Point2D(6,6);

        assertFalse(CollisionDetector.checkPoints(a,b,c,d));
        assertTrue(CollisionDetector.checkPoints(a1, b, c, d));

        a = new Point2D(1,1);
        a1 = new Point2D(2,1);
        b = new Point2D(5,0);
        c = new Point2D(1,0);
        d = new Point2D(0,3);

        assertTrue(CollisionDetector.checkPoints(a,b,c,d));
        assertFalse(CollisionDetector.checkPoints(a1,b,c,d));

        a = new Point2D(6,6);
        b = new Point2D(-2,-4);
        c = new Point2D(5,4);
        d = new Point2D(-1,-2);
        assertTrue(CollisionDetector.checkPoints(a,b,c,d));


    }
}
