package simulation.util;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;
import org.junit.*;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Class that tests the MathHelper class
 */
public class MathHelperTest {

    @BeforeClass
    public static void setUpClass() {
        Log.setLogEnabled(false);
    }

    @AfterClass
    public static void tearDownClass() {
        Log.setLogEnabled(true);
    }

    @Test
    public void testMatrixEquals() {
        // Simple check for equality
        double[][] matrixEntries1 = {{23.4, 3.2, 9.3}, {7.5, 9.8, 29.3}, {1.2, 0.8346, 238.3}};
        RealMatrix matrix11 = new BlockRealMatrix(matrixEntries1);
        RealMatrix matrix12 = new BlockRealMatrix(matrixEntries1);
        assertTrue(MathHelper.matrixEquals(matrix11, matrix12, 0.0000001));

        // Matrices with too much difference are not equal
        double[][] matrixEntries2 = {{0.034, 0.3, 0.853}, {0.82356, 0.378, 0.385}, {1.3534, 0.1235, 23.248}};
        RealMatrix matrix21 = new BlockRealMatrix(matrixEntries2);
        RealMatrix matrix22 = matrix21.scalarAdd(0.0000002);
        assertFalse(MathHelper.matrixEquals(matrix21, matrix22, 0.0000001));

        // Matrices with a small difference are considered equal
        RealMatrix matrix23 = matrix21.scalarAdd(0.00000002);
        assertTrue(MathHelper.matrixEquals(matrix21, matrix23, 0.0000001));
    }

    @Test
    public void testMatrixInvert() throws Exception {
        // Invert - Invert should be equal
        double[][] matrixEntries1 = {{23.4, 3.2, 9.3}, {7.5, 9.8, 29.3}, {1.2, 0.8346, 238.3}};
        RealMatrix matrix1 = new BlockRealMatrix(matrixEntries1);
        RealMatrix inverse11 = MathHelper.matrixInvert(matrix1);
        RealMatrix inverse12 = MathHelper.matrixInvert(inverse11);
        assertTrue(MathHelper.matrixEquals(matrix1, inverse12, 0.0000001));

        // Invert - Invert should be equal
        double[][] matrixEntries2 = {{0.034, 0.3, 0.853}, {0.82356, 0.378, 0.385}, {1.3534, 0.1235, 23.248}};
        RealMatrix matrix2 = new BlockRealMatrix(matrixEntries2);
        RealMatrix inverse21 = MathHelper.matrixInvert(matrix2);
        RealMatrix inverse22 = MathHelper.matrixInvert(inverse21);
        assertTrue(MathHelper.matrixEquals(matrix2, inverse22, 0.0000001));

        // Non square matrix throws exception
        boolean exceptionCaught3 = false;
        double[][] matrixEntries3 = {{0.034, 0.3}, {0.82356, 0.378}, {1.3534, 0.1235}};
        RealMatrix matrix3 = new BlockRealMatrix(matrixEntries3);

        try {
            MathHelper.matrixInvert(matrix3);
        }
        catch (Exception e) {
            exceptionCaught3 = true;
        }

        assertTrue(exceptionCaught3);

        // Non invertible matrix throws exception
        boolean exceptionCaught4 = false;
        double[][] matrixEntries4 = {{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}};
        RealMatrix matrix4 = new BlockRealMatrix(matrixEntries4);

        try {
            MathHelper.matrixInvert(matrix4);
        }
        catch (Exception e) {
            exceptionCaught4 = true;
        }

        assertTrue(exceptionCaught4);
    }

    @Test
    public void testVectorEquals() {
        // Simple check for equality
        double[] vectorEntries1 = {23.237, 239.258, 0.2374};
        RealVector vector11 = new ArrayRealVector(vectorEntries1);
        RealVector vector12 = new ArrayRealVector(vectorEntries1);
        assertTrue(MathHelper.vectorEquals(vector11, vector12, 0.00000001));

        // Vectors with too much difference are not equal
        double[] vectorEntries2 = {0.2537, 87.258, 0.05739};
        RealVector vector21 = new ArrayRealVector(vectorEntries2);
        RealVector vector22 = vector21.mapAdd(0.00000002);
        assertFalse(MathHelper.vectorEquals(vector21, vector22, 0.00000001));

        // Vectors with a small difference are considered equal
        RealVector vector23 = vector21.mapAdd(0.000000002);
        assertTrue(MathHelper.vectorEquals(vector21, vector23, 0.00000001));
    }

    @Test
    public void testVector3DToCrossProductMatrix() throws Exception {
        // Compare with default cross product computation
        double[] vectorEntries1 = {23.237, 239.258, 0.2374};
        double[] vectorEntries2 = {0.2537, 87.258, 0.05739};
        RealVector vector1 = new ArrayRealVector(vectorEntries1);
        RealVector vector2 = new ArrayRealVector(vectorEntries2);
        RealMatrix crossProductMatrixVector1 = MathHelper.vector3DToCrossProductMatrix(vector1);
        RealVector crossProduct1 = crossProductMatrixVector1.operate(vector2);
        Vector3D vector3 = new Vector3D(vectorEntries1);
        Vector3D vector4 = new Vector3D(vectorEntries2);
        Vector3D crossProduct2tmp = vector3.crossProduct(vector4);
        double[] vectorEntries3 = {crossProduct2tmp.getX(), crossProduct2tmp.getY(), crossProduct2tmp.getZ()};
        RealVector crossProduct2 = new ArrayRealVector(vectorEntries3);
        assertTrue(MathHelper.vectorEquals(crossProduct1, crossProduct2, 0.00000001));

        // Non 3D vector throws exception
        boolean exceptionCaught5 = false;
        double[] vectorEntries5 = {0.2537, 87.258, 0.05739, 23.5295};
        RealVector vector5 = new ArrayRealVector(vectorEntries5);

        try {
            MathHelper.vector3DToCrossProductMatrix(vector5);
        }
        catch (Exception e) {
            exceptionCaught5 = true;
        }

        assertTrue(exceptionCaught5);
    }

    @Test
    public void testVector3DCrossProduct() throws Exception {
        // Compare with default cross product computation
        double[] vectorEntries1 = {23.237, 239.258, 0.2374};
        double[] vectorEntries2 = {0.2537, 87.258, 0.05739};
        RealVector vector1 = new ArrayRealVector(vectorEntries1);
        RealVector vector2 = new ArrayRealVector(vectorEntries2);
        RealVector crossProduct1 = MathHelper.vector3DCrossProduct(vector1, vector2);
        Vector3D vector3 = new Vector3D(vectorEntries1);
        Vector3D vector4 = new Vector3D(vectorEntries2);
        Vector3D crossProduct2tmp = vector3.crossProduct(vector4);
        double[] vectorEntries3 = {crossProduct2tmp.getX(), crossProduct2tmp.getY(), crossProduct2tmp.getZ()};
        RealVector crossProduct2 = new ArrayRealVector(vectorEntries3);
        assertTrue(MathHelper.vectorEquals(crossProduct1, crossProduct2, 0.00000001));

        // Non 3D vector throws exception
        boolean exceptionCaught5 = false;
        double[] vectorEntries5 = {0.2537, 87.258};
        double[] vectorEntries6 = {23.2537, 0.34258};
        RealVector vector5 = new ArrayRealVector(vectorEntries5);
        RealVector vector6 = new ArrayRealVector(vectorEntries6);

        try {
            MathHelper.vector3DCrossProduct(vector5, vector6);
        }
        catch (Exception e) {
            exceptionCaught5 = true;
        }

        assertTrue(exceptionCaught5);
    }

    @Test
    public void testCheckIntersection2D() {
        List<Map.Entry<RealVector, RealVector>> list1 = new LinkedList<>();
        Map.Entry<RealVector, RealVector> e1 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{0.0, 0.0, 0.0}), new ArrayRealVector(new double[]{4.0, 0.0, 0.0}));
        Map.Entry<RealVector, RealVector> e2 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{4.0, 0.0, 0.0}), new ArrayRealVector(new double[]{4.0, 4.0, 0.0}));
        Map.Entry<RealVector, RealVector> e3 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{4.0, 4.0, 0.0}), new ArrayRealVector(new double[]{0.0, 4.0, 0.0}));
        Map.Entry<RealVector, RealVector> e4 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{0.0, 4.0, 0.0}), new ArrayRealVector(new double[]{0.0, 0.0, 0.0}));
        list1.add(e1); list1.add(e2); list1.add(e3); list1.add(e4);

        List<Map.Entry<RealVector, RealVector>> list2 = new LinkedList<>();
        assertFalse(MathHelper.checkIntersection2D(list1, list2));

        Map.Entry<RealVector, RealVector> e5 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{0.0, 10.0, 0.0}), new ArrayRealVector(new double[]{4.0, 10.0, 0.0}));
        Map.Entry<RealVector, RealVector> e6 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{4.0, 10.0, 0.0}), new ArrayRealVector(new double[]{4.0, 14.0, 0.0}));
        Map.Entry<RealVector, RealVector> e7 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{4.0, 14.0, 0.0}), new ArrayRealVector(new double[]{0.0, 14.0, 0.0}));
        Map.Entry<RealVector, RealVector> e8 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{0.0, 14.0, 0.0}), new ArrayRealVector(new double[]{0.0, 10.0, 0.0}));
        list2.add(e5); list2.add(e6); list2.add(e7); list2.add(e8);
        assertFalse(MathHelper.checkIntersection2D(list1, list2));

        List<Map.Entry<RealVector, RealVector>> list3 = new LinkedList<>();
        Map.Entry<RealVector, RealVector> e9 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{2.0, 12.0, 0.0}), new ArrayRealVector(new double[]{-3.0, 7.0, 0.0}));
        Map.Entry<RealVector, RealVector> e10 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{-3.0, 7.0, 0.0}), new ArrayRealVector(new double[]{2.0, 2.0, 0.0}));
        Map.Entry<RealVector, RealVector> e11 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{2.0, 2.0, 0.0}), new ArrayRealVector(new double[]{7.0, 7.0, 0.0}));
        Map.Entry<RealVector, RealVector> e12 = new AbstractMap.SimpleEntry<RealVector, RealVector>(new ArrayRealVector(new double[]{7.0, 7.0, 0.0}), new ArrayRealVector(new double[]{2.0, 12.0, 0.0}));
        list3.add(e9); list3.add(e10); list3.add(e11); list3.add(e12);
        assertTrue(MathHelper.checkIntersection2D(list1, list3));
        assertTrue(MathHelper.checkIntersection2D(list2, list3));
    }

    @Test
    public void testRandomLong() {
        assertTrue(MathHelper.randomLong(0L, 0L) == 0L);
        assertTrue(MathHelper.randomLong(-1L, -1L) == -1L);
        assertTrue(MathHelper.randomLong(Long.MIN_VALUE, Long.MIN_VALUE) == Long.MIN_VALUE);
        assertTrue(MathHelper.randomLong(Long.MAX_VALUE, Long.MAX_VALUE) == Long.MAX_VALUE);
        assertTrue(MathHelper.randomLong(20L, 20L) == 20L);

        long result1 = MathHelper.randomLong(-10L, 10L);
        assertTrue(result1 >= -10L && result1 <= 10L);

        long result2 = MathHelper.randomLong(Long.MIN_VALUE / 2L, Long.MAX_VALUE / 2L);
        assertTrue(result2 >= Long.MIN_VALUE / 2L && result2 <= Long.MAX_VALUE / 2L);
    }

    @Test
    public void testRandomInt() {
        assertTrue(MathHelper.randomInt(0, 0) == 0);
        assertTrue(MathHelper.randomInt(-1, -1) == -1);
        assertTrue(MathHelper.randomInt(Integer.MIN_VALUE, Integer.MIN_VALUE) == Integer.MIN_VALUE);
        assertTrue(MathHelper.randomInt(Integer.MAX_VALUE, Integer.MAX_VALUE) == Integer.MAX_VALUE);
        assertTrue(MathHelper.randomInt(20, 20) == 20);

        long result1 = MathHelper.randomInt(-10, 10);
        assertTrue(result1 >= -10 && result1 <= 10);

        long result2 = MathHelper.randomInt(Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
        assertTrue(result2 >= Integer.MIN_VALUE / 2 && result2 <= Integer.MAX_VALUE / 2);
    }
}