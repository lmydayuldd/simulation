package simulation.util;


import org.apache.commons.math3.geometry.euclidean.twod.Euclidean2D;
import org.apache.commons.math3.geometry.euclidean.twod.PolygonsSet;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.partitioning.Region;
import org.apache.commons.math3.geometry.partitioning.RegionFactory;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class that provides static access to common math operations
 */
public final class MathHelper {

    /**
     * Empty non accessible constructor, there is no instance of this class
     */
    private MathHelper() {}

    /**
     * Function that checks for matrix equality allowing a small fixed error factor because of numeric issues
     *
     * @param matrix1 First matrix to be checked for fuzzy equality
     * @param matrix2 Second matrix to be checked for fuzzy equality
     * @param threshold Threshold value for check of norm, e.g. a value such as 0.0000001
     * @return Boolean indicating fuzzy equality of input matrices
     */
    public static boolean matrixEquals(RealMatrix matrix1, RealMatrix matrix2, double threshold) {
        Log.finest("MathHelper: matrixEquals - Input matrix1: " + matrix1.toString() + " , input matrix2:" + matrix2.toString());

        // Simple check to avoid computations if possible
        if (matrix1.getColumnDimension() != matrix2.getColumnDimension() ||
                matrix1.getRowDimension() != matrix2.getColumnDimension() ||
                matrix1.getColumnDimension() == 0 || matrix1.getRowDimension() == 0) {
            Log.finest("MathHelper: matrixEquals - returned false because of matrix dimension mismatch or zero dimension");
            return false;
        }

        // If matrices are nearly equal, norm for all entries of difference matrix must be near 0
        RealMatrix diffMatrix = matrix1.subtract(matrix2);
        double norm = diffMatrix.getFrobeniusNorm();
        boolean result = (norm < threshold);

        Log.finest("MathHelper: matrixEquals - returned " + result + " for norm value " + norm);
        return result;
    }

    /**
     * Function that computes the inverse of an input matrix, if possible
     * Perform some simple checks before actually computing the matrix inverse
     *
     * @param matrix Matrix that is used for the inverse computation, should be square and invertible matrix
     * @throws Exception If matrix is not invertible
     * @return Inverse matrix of the input matrix
     */
    public static RealMatrix matrixInvert(RealMatrix matrix) throws Exception {
        Log.finest("MathHelper: matrixInvert - Input matrix: " + matrix.toString());
        RealMatrix result = null;

        if (matrix.isSquare()) {
            if (matrix.getColumnDimension() > 0) {
                LUDecomposition lu = new LUDecomposition(matrix);

                if (lu.getDeterminant() != 0.0) {
                    DecompositionSolver solver = lu.getSolver();
                    result = solver.getInverse();
                }
            }
        }

        if (result == null) {
            Log.severe("MathHelper: matrixInvert - matrix is not invertible: " + matrix.toString());
            throw new Exception("MathHelper: matrixInvert - matrix is not invertible: " + matrix.toString());
        }

        Log.finest("MathHelper: matrixInvert - Result matrix: " + result.toString());
        return result;
    }

    /**
     * Function that performs a re-orthonormalization for a given rotation matrix
     *
     * @param matrix Input 3x3 rotation matrix that is used in re-orthonormalization
     * @return Result 3x3 rotation matrix that is now orthonormal again
     */
    public static RealMatrix matrix3DOrthonormalize(RealMatrix matrix) {
        Log.finest("MathHelper: matrix3DOrthonormalize - Input matrix: " + matrix);

        // Checks for valid input
        if (matrix.getColumnDimension() != 3 || !matrix.isSquare()) {
            Log.warning("MathHelper: matrix3DOrthonormalize - Input matrix is not a 3x3 matrix, also returned as result: " + matrix);
            return matrix;
        }

        // Prepare variables

        // Approach: Gram Schmidt re-orthonormalization
        RealVector v0 = matrix.getColumnVector(0);
        RealVector v1 = matrix.getColumnVector(1);
        RealVector v2 = matrix.getColumnVector(2);

        v0 = v0.mapDivide(v0.getNorm());

        v1 = v1.subtract(v0.mapMultiply(v0.dotProduct(v1)));
        v1 = v1.mapDivide(v1.getNorm());

        v2 = v2.subtract(v0.mapMultiply(v0.dotProduct(v2))).subtract(v1.mapMultiply(v1.dotProduct(v2)));
        v2 = v2.mapDivide(v2.getNorm());

        matrix.setColumnVector(0, v0);
        matrix.setColumnVector(1, v1);
        matrix.setColumnVector(2, v2);

        Log.finest("MathHelper: matrix3DOrthonormalize - returned " + matrix);
        return matrix;
    }

    //
    /**
     * Function that checks for vector equality allowing a small fixed error factor because of numeric issues
     *
     * @param vector1 First vector to be checked for fuzzy equality
     * @param vector2 Second vector to be checked for fuzzy equality
     * @param threshold Threshold value for check of norm, e.g. a value such as 0.00000001
     * @return Boolean indicating fuzzy equality of input vectors
     */
    public static boolean vectorEquals(RealVector vector1, RealVector vector2, double threshold) {
        Log.finest("MathHelper: vectorEquals - Input vector1: " + vector1.toString() + " , input vector2:" + vector2.toString());

        // Simple check to avoid computations if possible
        if (vector1.getDimension() != vector2.getDimension() || vector1.getDimension() == 0) {
            Log.finest("MathHelper: vectorEquals - returned false because of vector dimension mismatch or zero dimension");
            return false;
        }

        // If vectors are nearly equal, norm for all entries of difference vectors must be near 0
        RealVector diffVector = vector1.subtract(vector2);
        double norm = diffVector.getNorm();
        boolean result = (norm < threshold);

        Log.finest("MathHelper: vectorEquals - returned " + result + " for norm value " + norm);
        return result;
    }

    /**
     * Function that converts a vector to a matrix such that the matrix multiplied with another vector is the cross product
     * Perform some simple checks before actually computing the matrix
     * Only supports 3-dimensional vectors and 3x3 matrices as result
     *
     * @param vector 3-dimensional vector for which the corresponding cross product matrix should be computed
     * @throws Exception If vector is not convertible
     * @return 3x3 cross product matrix for the input 3-dimensional vector
     */
    public static RealMatrix vector3DToCrossProductMatrix(RealVector vector) throws Exception {
        Log.finest("MathHelper: vector3DToCrossProductMatrix - Input vector: " + vector.toString());
        RealMatrix result = null;

        if (vector.getDimension() == 3) {
            double[][] matrixEntries = {{0.0, -vector.getEntry(2), vector.getEntry(1)}, {vector.getEntry(2), 0.0, -vector.getEntry(0)}, {-vector.getEntry(1), vector.getEntry(0), 0.0}};
            result = new BlockRealMatrix(matrixEntries);
        }

        if (result == null) {
            Log.severe("MathHelper: vector3DToCrossProductMatrix - vector is not convertible: " + vector.toString());
            throw new Exception("MathHelper: vector3DToCrossProductMatrix - vector is not convertible: " + vector.toString());
        }

        Log.finest("MathHelper: vector3DToCrossProductMatrix - Result matrix: " + result.toString());
        return result;
    }

    /**
     * Function that computes the cross product of two 3-dimensional vectors
     * Only supports 3-dimensional vectors and 3x3 matrices as result
     *
     * @param vector1 First 3-dimensional vector that is used in cross product computation
     * @param vector2 Second 3-dimensional vector that is used in cross product computation
     * @throws Exception If cross product cannot be computed
     * @return 3-dimensional cross product for the input 3-dimensional vectors
     */
    public static RealVector vector3DCrossProduct(RealVector vector1, RealVector vector2) throws Exception {
        Log.finest("MathHelper: vector3DCrossProduct - Input vector1: " + vector1.toString() + " , input vector2:" + vector2.toString());
        RealVector result = null;

        // This is also an example of how to use the vector3DToCrossProductMatrix function
        if (vector1.getDimension() == 3 && vector2.getDimension() == 3) {
            RealMatrix crossProductMatrixVector1 = vector3DToCrossProductMatrix(vector1);
            result = crossProductMatrixVector1.operate(vector2);
        }

        if (result == null) {
            Log.severe("MathHelper: vector3DCrossProduct - vector cross product can not be computed: Input vector1: " + vector1.toString() + " , input vector2:" + vector2.toString());
            throw new Exception("MathHelper: vector3DCrossProduct - vector cross product can not be computed: Input vector1: " + vector1.toString() + " , input vector2:" + vector2.toString());
        }

        Log.finest("MathHelper: vector3DCrossProduct - Result vector: " + result.toString());
        return result;
    }

    /**
     * Function that checks if two 2D spaces defined by real vectors have a non empty 2D space intersection
     *
     * @param vectorsOne First pairs of vector start points and end points that create a 2D space, ordered such that left halves of lines between following start points include the 2D space
     * @param vectorsTwo Second pairs of vector start points and end points that create a 2D space, ordered such that left halves of lines between following start points include the 2D space
     * @return True if 2D intersection is non-empty, otherwise false
     */
    public static boolean checkIntersection2D(List<Map.Entry<RealVector, RealVector>> vectorsOne, List<Map.Entry<RealVector, RealVector>> vectorsTwo) {
        // If any list is empty, return false
        if (vectorsOne.isEmpty() || vectorsTwo.isEmpty()) {
            return false;
        }

        // Convert vector start and end pairs to lists
        List<Vector2D> listOne = new LinkedList<>();
        List<Vector2D> listTwo = new LinkedList<>();

        for (Map.Entry<RealVector, RealVector> entry : vectorsOne) {
            // Vectors must have at least dimension 2, otherwise result is always false
            if (entry.getKey().getDimension() < 2 || entry.getValue().getDimension() < 2) {
                return false;
            }

            Vector2D vertexOne = new Vector2D(entry.getKey().getEntry(0), entry.getKey().getEntry(1));
            listOne.add(vertexOne);
        }

        for (Map.Entry<RealVector, RealVector> entry : vectorsTwo) {
            // Vectors must have at least dimension 2, otherwise result is always false
            if (entry.getKey().getDimension() < 2 || entry.getValue().getDimension() < 2) {
                return false;
            }

            Vector2D vertexOne = new Vector2D(entry.getKey().getEntry(0), entry.getKey().getEntry(1));
            listTwo.add(vertexOne);
        }

        // From lists create regions
        Region<Euclidean2D> regionOne = new PolygonsSet(1.0e-5, listOne.toArray(new Vector2D[listOne.size()]));
        Region<Euclidean2D> regionTwo = new PolygonsSet(1.0e-5, listTwo.toArray(new Vector2D[listTwo.size()]));

        // Check intersection for emptiness
        RegionFactory<Euclidean2D> regionFactory = new RegionFactory<>();
        Region<Euclidean2D> regionIntersection = regionFactory.intersection(regionOne, regionTwo);
        return !regionIntersection.isEmpty();
    }

    /**
     * Function that generates uniformly distributed random long values in a specified interval
     *
     * @param lower Lower end of the interval, included
     * @param upper Upper end of the interval, included
     * @return Uniformly random long value within the interval
     */
    public static long randomLong(long lower, long upper) {
        // For same value return the same value
        if (lower == upper) {
            return lower;
        }

        // Otherwise generate random value
        RandomDataGenerator random = new RandomDataGenerator();
        return random.nextLong(lower, upper);
    }

    /**
     * Function that generates uniformly distributed random int values in a specified interval
     *
     * @param lower Lower end of the interval, included
     * @param upper Upper end of the interval, included
     * @return Uniformly random int value within the interval
     */
    public static int randomInt(int lower, int upper) {
        // For same value return the same value
        if (lower == upper) {
            return lower;
        }

        // Otherwise generate random value
        RandomDataGenerator random = new RandomDataGenerator();
        return random.nextInt(lower, upper);
    }
}
