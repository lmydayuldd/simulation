package simulation.vehicle;


import commons.simulation.PhysicalObject;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import simulation.environment.WorldModel;
import simulation.util.Log;
import simulation.util.MathHelper;

import java.util.List;
import java.util.Map;

import static simulation.vehicle.MassPointType.*;
import static simulation.vehicle.VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING;

/**
 * Physics calculations for simulation
 */
public class PhysicsEngine{

    /** Average earth gravity in m/s^2 or N/kg */
    public static final double GRAVITY_EARTH = -9.81;
    /** Average air density in kg/m^3 */
    public static final double AIR_DENSITY = 1.25;
    /** Average car air drag coefficient (without unit) */
    public static final double AIR_DRAG_CAR = 0.3;
    /** Average road friction coefficient for dry roads (no unit) */
    public static final double ROAD_FRICTION_DRY = 0.7;
    /** Average road friction coefficient for wet roads (no unit) */
    public static final double ROAD_FRICTION_WET = 0.4;


    /**
     * Computes the physics of all the physical objects
     * @param object Is the physical object, which physics have to be computed
     * @param physicalObjects List of all physical objects, needed to check for collision
     * @param timeDiffMs Difference in time measured in milliseconds
     */
    public static void computePhysics(PhysicalObject object, List<PhysicalObject> physicalObjects, long timeDiffMs){

        double deltaT = timeDiffMs / 1000.0;

        // Check objects for collisions
        for (PhysicalObject physicalObject : physicalObjects) {
            // Do not compute collision if its the same object or when both objects are already in collision
            if (physicalObject.getId() == object.getId() || (physicalObject.getCollision() && object.getCollision())) {
                continue;
            }

            // Do not compute collision if both objects are more than 100 meters away from each other
            if (physicalObject.getGeometryPos().getDistance(object.getGeometryPos()) >= 100.0) {
                continue;
            }

            // Do not compute collision if objects do not overlap in height
            double minHeight = object.getGeometryPos().getEntry(2) - 0.5 * object.getHeight();
            double maxHeight = object.getGeometryPos().getEntry(2) + 0.5 * object.getHeight();
            double otherMinHeight = physicalObject.getGeometryPos().getEntry(2) - 0.5 * physicalObject.getHeight();
            double otherMaxHeight = physicalObject.getGeometryPos().getEntry(2) + 0.5 * physicalObject.getHeight();
            boolean heightOverlap = (minHeight >= otherMinHeight && minHeight <= otherMaxHeight) ||
                                    (maxHeight >= otherMinHeight && maxHeight <= otherMaxHeight) ||
                                    (otherMinHeight >= minHeight && otherMinHeight <= maxHeight) ||
                                    (otherMaxHeight >= minHeight && otherMaxHeight <= maxHeight);

            if (!heightOverlap) {
                continue;
            }

            // Perform collision computation
            List<Map.Entry<RealVector, RealVector>> boundaries = object.getBoundaryVectors();
            List<Map.Entry<RealVector, RealVector>> otherBoundaries = physicalObject.getBoundaryVectors();
            boolean collisionDetected = MathHelper.checkIntersection2D(boundaries, otherBoundaries);

            if (collisionDetected) {
                object.setCollision(true);
                physicalObject.setCollision(true);
            }
        }

        switch (object.getPhysicalObjectType()) {
            case PHYSICAL_OBJECT_TYPE_CAR_DEFAULT:
            case PHYSICAL_OBJECT_TYPE_CAR_1:
            case PHYSICAL_OBJECT_TYPE_CAR_2:
            case PHYSICAL_OBJECT_TYPE_CAR_3:
            case PHYSICAL_OBJECT_TYPE_CAR_4:
            case PHYSICAL_OBJECT_TYPE_CAR_5:
                PhysicalVehicle vehicle = (PhysicalVehicle) object;
                if(!object.getCollision()){
                    calcMassPointForces(deltaT, vehicle);
                }
                break;
            case PHYSICAL_OBJECT_TYPE_PEDESTRIAN:
                //No calculations
                break;
            case PHYSICAL_OBJECT_TYPE_TREE:
                //No physics computations for trees
                break;
            case PHYSICAL_OBJECT_TYPE_NETWORK_CELL_BASE_STATION:
                //No physics computations for trees
                break;
            default:
                //Invalid type
                Log.warning("PhysicsEngine: Invalid type in computePhysics");
                break;
        }
    }

    /**
     * Function that calculates the forces for each mass point of the vehicle
     *
     * @param deltaT Time difference to previous step in seconds
     * @param vehicle Physical vehicle for which force should be computed
     */
    private static void calcMassPointForces(double deltaT, PhysicalVehicle vehicle){
        Log.finest("PhysicsEngine: calcMassPointForces - PhysicalVehicle at start: " + vehicle);

        MassPoint[] massPoints = vehicle.getSimulationVehicle().getWheelMassPoints();

        // Iterate over wheel mass points
        for (MassPoint mp : massPoints) {

            // Force result of wheel mass point
            RealVector forceResult = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

            // Force: Acceleration force F = mass * acceleration
            RealVector forceAcceleration = vehicle.calcAccelerationForce(mp, deltaT);
            forceResult = forceResult.add(forceAcceleration);

            Double forceAccelerationNorm = forceAcceleration.getNorm();
            if (forceAccelerationNorm.isInfinite() || forceAccelerationNorm.isNaN() || forceAccelerationNorm > 1.0E10) {
                Log.warning("Large forceAcceleration: " + forceAcceleration + " in MassPoint: " + mp + " for PhysicalVehicle: " + vehicle);
            }

            // Force: Brake force F = mass * acceleration
            // Consider amount of acceleration, do not cause negative acceleration due to brakes
            RealVector forceBrake = vehicle.calcBrakeForce(mp, deltaT);
            forceResult = forceResult.add(forceBrake);

            Double forceBrakeNorm = forceBrake.getNorm();
            if (forceBrakeNorm.isInfinite() || forceBrakeNorm.isNaN() || forceBrakeNorm > 1.0E10) {
                Log.warning("Large forceBrake: " + forceBrake + " in MassPoint: " + mp + " for PhysicalVehicle: " + vehicle);
            }

            // Forces: Gravity, road friction, downhill force
            RealVector forcesRelatedToGravity = calcGravityRelatedForces(mp, vehicle, deltaT);
            forceResult = forceResult.add(forcesRelatedToGravity);

            Double forcesRelatedToGravityNorm = forcesRelatedToGravity.getNorm();
            if (forcesRelatedToGravityNorm.isInfinite() || forcesRelatedToGravityNorm.isNaN() || forcesRelatedToGravityNorm > 1.0E10) {
                Log.warning("Large forcesRelatedToGravity: " + forcesRelatedToGravityNorm + " in MassPoint: " + mp + " for PhysicalVehicle: " + vehicle);
            }

            // Force: Centripetal force Fc = mass * acceleration centrifugal = mass * (angularVelocity x (angularVelocity x radiusVector))
            RealVector forceCentripetal = calcCentripetalForce(mp, vehicle);
            forceResult = forceResult.add(forceCentripetal);

            Double forceCentripetalNorm = forceCentripetal.getNorm();
            if (forceCentripetalNorm.isInfinite() || forceCentripetalNorm.isNaN() || forceCentripetalNorm > 1.0E10) {
                Log.warning("Large forceCentripetal: " + forceCentripetal + " in MassPoint: " + mp + " for PhysicalVehicle: " + vehicle);
            }

            // Force: Air friction Fa = -0.5 * air density * velocity^2 * drag coefficient * area hit by wind
            RealVector forceAirFriction = calcAirFrictionForce(mp, vehicle);
            forceResult = forceResult.add(forceAirFriction);

            Double forceAirFrictionNorm = forceAirFriction.getNorm();
            if (forceAirFrictionNorm.isInfinite() || forceAirFrictionNorm.isNaN() || forceAirFrictionNorm > 1.0E10) {
                Log.warning("Large forceAirFriction: " + forceAirFriction + " in MassPoint: " + mp + " for PhysicalVehicle: " + vehicle);
            }

            // Set force to mass point
            mp.setForce(forceResult);
        }

        Log.finest("PhysicsEngine: calcMassPointForces - PhysicalVehicle at end: " + vehicle);
    }

    /**
     * Function that calculates the fraction between 0.0 and 1.0 describing how much ground contact
     * the mass point will probably have in the current time step to adjust forces, this is just an approximation
     *
     * @param mp MassPoint for which ground fraction should be computed
     * @param vehicle Physical vehicle for which force should be computed
     * @param deltaT Time difference to previous step in seconds
     * @return Fraction of ground contact in current time step between 0.0 and 1.0
     */
    protected static double calcGroundContact(MassPoint mp, PhysicalVehicle vehicle, double deltaT) {

        double groundFraction = 0.0;

        double velocityZ = mp.getVelocity().getEntry(2);
        double accelerationZ = mp.getAcceleration().getEntry(2);

        double groundZ = mp.getGroundZ();
        double limitZ = groundZ + vehicle.getSimulationVehicle().getWheelRadius();
        double groundDistance = (mp.getPos().getEntry(2) - limitZ);

        if (groundDistance > 1.0E-8) {
            accelerationZ = GRAVITY_EARTH;
        } else {
            return 1.0;
        }

        double zDistance = 0.5 * accelerationZ * deltaT * deltaT + velocityZ * deltaT;

        if (groundDistance > 0.0 && groundDistance + zDistance < 0.0) {
            groundFraction = 1.0 - (groundDistance / Math.abs(zDistance));
        }

        return groundFraction;
    }

    /**
     * Function that calculates the sum of all forces that are caused by gravity for a given mass point
     *
     * @param mp MassPoint for which force should be computed
     * @param vehicle Physical vehicle for which force should be computed
     * @param deltaT Time difference to previous step in seconds
     * @return RealVector that represents the force
     */
    private static RealVector calcGravityRelatedForces(MassPoint mp, PhysicalVehicle vehicle, double deltaT){
        // Check wheels with ground contact
        double groundContact = calcGroundContact(mp, vehicle, deltaT);

        RealVector forcesGravityAll = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        RealVector forcesGravityGround = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        RealVector forcesGravityFalling = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        // Skip invalid mass points to avoid out of array bounds accesses
        if (mp.getType().ordinal() > MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()) {
            return forcesGravityAll;
        }

        // Force: Gravity force Fg = mass * gravity constant
        RealVector forceGravity = new ArrayRealVector(new double[] {0.0, 0.0, mp.getMass() * PhysicsEngine.GRAVITY_EARTH});

        // Compute forces related to ground contact
        if (groundContact > 0.0) {

            // Add gravity force
            forcesGravityGround = forcesGravityGround.add(forceGravity);

            // Determine angle information of vehicle
            RealVector backFront1 = (vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getPos().subtract(vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getPos()));
            RealVector backFront2 = (vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getPos().subtract(vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getPos()));
            RealVector leftRight1 = (vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getPos().subtract(vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getPos()));
            RealVector leftRight2 = (vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getPos().subtract(vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getPos()));

            // Compute average vectors
            RealVector vectorBackFront = backFront1.add(backFront2).mapMultiply(0.5);
            RealVector vectorLeftRight = leftRight1.add(leftRight2).mapMultiply(0.5);

            // Force: Normal force Fn = mass * gravity constant (with correct angles)
            RealVector forceNormal = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

            try {
                forceNormal = MathHelper.vector3DCrossProduct(vectorBackFront, vectorLeftRight);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Ensure that normal force points upwards
            if (forceNormal.getEntry(2) < 0.0) {
                forceNormal = forceNormal.mapMultiply(-1.0);
            }

            // Normalize vector
            double forceNormalNorm = forceNormal.getNorm();
            if (forceNormalNorm != 0.0) {
                forceNormal = forceNormal.mapDivide(forceNormalNorm);
            }

            // Set correct length of vector and add it to final result
            double forceNormalAmount = Math.abs(mp.getMass() * PhysicsEngine.GRAVITY_EARTH);
            forceNormal = forceNormal.mapMultiply(forceNormalAmount);
            forcesGravityGround = forcesGravityGround.add(forceNormal);

            // Split normal force vector to get fractions in backFront and leftRight directions
            // Compute angle between normal force and z plane, with this angle compute x, y normal force component vector in z plane
            double angleNormalZ = 0.0;
            RealVector planeZVector = new ArrayRealVector(new double[] {0.0, 0.0, 1.0});

            if (forceNormal.getNorm() != 0.0 && planeZVector.getNorm() != 0.0) {
                angleNormalZ = Math.acos(forceNormal.cosine(planeZVector));
            }

            RealVector componentXYVector = new ArrayRealVector(new double[] {forceNormal.getEntry(0), forceNormal.getEntry(1), 1.0});
            double normComponentXYVector = componentXYVector.getNorm();

            if (normComponentXYVector > 0.0) {
                componentXYVector = componentXYVector.mapDivide(normComponentXYVector);
            }

            // Rotate x, y normal force component vector to origin, then split based on axis
            componentXYVector = componentXYVector.mapMultiply(angleNormalZ * forceNormal.getNorm());
            componentXYVector = vehicle.getRotationMatrix().transpose().operate(componentXYVector);

            double fractionNormalLeftRight = 0.0;
            double fractionNormalBackFront = 0.0;
            double fractionNormalLengths = Math.abs(componentXYVector.getEntry(0)) + Math.abs(componentXYVector.getEntry(1));

            if (fractionNormalLengths != 0.0) {
                fractionNormalBackFront = 2.0 * (Math.abs(componentXYVector.getEntry(1)) / fractionNormalLengths);
                fractionNormalLeftRight = 2.0 * (Math.abs(componentXYVector.getEntry(0)) / fractionNormalLengths);
            } else {
                fractionNormalBackFront = 1.0;
                fractionNormalLeftRight = 1.0;
            }

            // Compute amounts of normal forces in different directions
            double forceNormalLengthLeftRight = fractionNormalLeftRight * forceNormal.getNorm();
            double forceNormalLengthBackFront = fractionNormalBackFront * forceNormal.getNorm();

            // Split velocity vector
            RealVector mpVelocityWheels = new ArrayRealVector(new double[] {0.0, 1.0, 0.0});
            RealVector mpVelocityWheelsOrthogonal = new ArrayRealVector(new double[] {1.0, 0.0, 0.0});
            double steeringAngle = vehicle.getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_STEERING).getActuatorValueCurrent();

            // For rotation matrix it holds transpose(matrix) = inverse(matrix)
            RealVector mpVelocityOrigin = vehicle.getRotationMatrix().transpose().operate(mp.getVelocity());

            // Front wheels: Consider steering
            if (mp.getType().ordinal() == MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal() || mp.getType().ordinal() == MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()) {
                Rotation steerRotZ = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, steeringAngle);
                RealMatrix steerRotZMatrix = new BlockRealMatrix(steerRotZ.getMatrix());
                mpVelocityOrigin = steerRotZMatrix.operate(mpVelocityOrigin);
            }

            // Fractions of velocity vector in x and y directions
            double fractionWheels = 0.0;
            double fractionWheelsOrthogonal = 0.0;
            double fractionWheelsLengths = Math.abs(mpVelocityOrigin.getEntry(0)) + Math.abs(mpVelocityOrigin.getEntry(1));

            if (fractionWheelsLengths != 0.0) {
                fractionWheels = (Math.abs(mpVelocityOrigin.getEntry(1)) / fractionWheelsLengths);
                fractionWheelsOrthogonal = (Math.abs(mpVelocityOrigin.getEntry(0)) / fractionWheelsLengths);
            }

            mpVelocityWheels.setEntry(1, mpVelocityOrigin.getEntry(1));
            mpVelocityWheels.setEntry(2, fractionWheels * mpVelocityOrigin.getEntry(2));
            mpVelocityWheelsOrthogonal.setEntry(0, mpVelocityOrigin.getEntry(0));
            mpVelocityWheelsOrthogonal.setEntry(2, fractionWheelsOrthogonal * mpVelocityOrigin.getEntry(2));

            // Front wheels: Consider steering
            if (mp.getType().ordinal() == MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal() || mp.getType().ordinal() == MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()) {
                Rotation steerRotZ = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, -steeringAngle);
                RealMatrix steerRotZMatrix = new BlockRealMatrix(steerRotZ.getMatrix());
                mpVelocityWheels = steerRotZMatrix.operate(mpVelocityWheels);
                mpVelocityWheelsOrthogonal = steerRotZMatrix.operate(mpVelocityWheelsOrthogonal);
            }

            mpVelocityWheels = vehicle.getRotationMatrix().operate(mpVelocityWheels);
            mpVelocityWheelsOrthogonal = vehicle.getRotationMatrix().operate(mpVelocityWheelsOrthogonal);

            // Road friction: Back-Front: Rolling resistance in the direction of the wheels
            RealVector forceRoadFrictionBackFront = mpVelocityWheels.mapMultiply(-1.0);
            double forceRoadFrictionBackFrontNorm = forceRoadFrictionBackFront.getNorm();
            double pressure = (mp.getPressure() > 0.0 ? mp.getPressure() : Vehicle.VEHICLE_DEFAULT_TIRE_PRESSURE);
            double rollingCoefficient = 0.005 + (1 / pressure) * (0.01 + 0.0095 * (forceRoadFrictionBackFrontNorm * 3.6 / 100) * (forceRoadFrictionBackFrontNorm * 3.6 / 100));

            if (forceRoadFrictionBackFrontNorm > 0.0) {
                forceRoadFrictionBackFront = forceRoadFrictionBackFront.mapDivide(forceRoadFrictionBackFrontNorm);
            } else {
                // If there is no velocity, there should not be any rolling coefficient
                rollingCoefficient = 0.0;
            }

            // Scale force down when near zero velocity to avoid permanent positive / negative changes
            if (forceRoadFrictionBackFrontNorm >= 0.0 && forceRoadFrictionBackFrontNorm < 0.35) {
                rollingCoefficient = forceRoadFrictionBackFrontNorm * rollingCoefficient;
            }

            forceRoadFrictionBackFront = forceRoadFrictionBackFront.mapMultiply(rollingCoefficient * forceNormalLengthBackFront);
            forcesGravityGround = forcesGravityGround.add(forceRoadFrictionBackFront);

            // Road friction: Left-Right: Resistance against wheels moving sideways
            RealVector forceRoadFrictionLeftRight = mpVelocityWheelsOrthogonal.mapMultiply(-1.0);
            double forceRoadFrictionLeftRightNorm = forceRoadFrictionLeftRight.getNorm();

            if (forceRoadFrictionLeftRightNorm > 0.0) {
                forceRoadFrictionLeftRight = forceRoadFrictionLeftRight.mapDivide(forceRoadFrictionLeftRightNorm);
            }

            double forceRoadFrictionLeftRightAmount = ((WorldModel.getInstance().isItRaining()) ? PhysicsEngine.ROAD_FRICTION_WET : PhysicsEngine.ROAD_FRICTION_DRY) * forceNormalLengthLeftRight;

            // Scale force down when near zero velocity to avoid permanent positive / negative changes
            if (forceRoadFrictionLeftRightNorm >= 0.0 && forceRoadFrictionLeftRightNorm < 0.35) {
                forceRoadFrictionLeftRightAmount = forceRoadFrictionLeftRightNorm * forceRoadFrictionLeftRightAmount;
            }

            forceRoadFrictionLeftRight = forceRoadFrictionLeftRight.mapMultiply(forceRoadFrictionLeftRightAmount);
            forcesGravityGround = forcesGravityGround.add(forceRoadFrictionLeftRight);
            forcesGravityGround = forcesGravityGround.mapMultiply(groundContact);
        }

        // No ground contact, mass point is falling
        if (groundContact < 1.0) {
            // Add gravity force
            forcesGravityFalling = forcesGravityFalling.add(forceGravity);
            forcesGravityFalling = forcesGravityFalling.mapMultiply(1.0 - groundContact);

            // Impact impulse J = mass * delta velocity = F_average * delta time (for change)
            if (groundContact > 0.0) {
                double velocityZ = (1.0 - groundContact) * deltaT * GRAVITY_EARTH + mp.getVelocity().getEntry(2);

                if (velocityZ < 0.0) {
                    double impactImpulse = mp.getMass() * Math.abs(velocityZ);
                    double forceImpactAverageAmount = impactImpulse / 0.01;
                    RealVector forceImpactAverage = new ArrayRealVector(new double[] {0.0, 0.0, 1.0});
                    forceImpactAverage = forceImpactAverage.mapMultiply(forceImpactAverageAmount);
                    forcesGravityFalling = forcesGravityFalling.add(forceImpactAverage);
                }
            }
        }

        forcesGravityAll = forcesGravityGround.add(forcesGravityFalling);

        return forcesGravityAll;
    }

    /**
     * Function that calculates the centripetal force for a given mass point
     *
     * @param mp MassPoint for which force should be computed
     * @param vehicle Physical vehicle for which force should be computed
     * @return RealVector that represents the force
     */
    private static RealVector calcCentripetalForce(MassPoint mp, PhysicalVehicle vehicle){
        // Force: Centripetal force Fc = mass * acceleration centrifugal = mass * (angularVelocity x (angularVelocity x radiusVector))
        RealVector forceCentripetal = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        double steeringAngle = vehicle.getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_STEERING).getActuatorValueCurrent();

        // Do not compute centripetal force for very small steering angles
        // Values very close to 0 are equivalent to a nearly infinite turning radius, leading to wrong force results
        // 0.02 rad = 1.1 deg means that turning radius is limited to a value of about 150 meters
        if (Math.abs(steeringAngle) < 0.02) {
            return forceCentripetal;
        }

        double curveRadiusSin = Math.sin(steeringAngle);

        if (curveRadiusSin != 0.0) {
            double wheelDistFrontBack = vehicle.getSimulationVehicle().getWheelDistFrontBack();
            double curveRadiusVectorLength = (wheelDistFrontBack / curveRadiusSin);
            RealVector curveRadiusVector = (vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getPos().subtract(vehicle.getSimulationVehicle().getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getPos()));

            if (curveRadiusVector.getNorm() > 0.0) {
                curveRadiusVector = curveRadiusVector.mapDivide(curveRadiusVector.getNorm());
            }

            curveRadiusVector = curveRadiusVector.mapMultiply(curveRadiusVectorLength);

            try {
                forceCentripetal = MathHelper.vector3DCrossProduct(vehicle.getAngularVelocity(), MathHelper.vector3DCrossProduct(vehicle.getAngularVelocity(), curveRadiusVector)).mapMultiply(mp.getMass());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return forceCentripetal;
    }

    /**
     * Function that calculates the air friction force for a given mass point
     *
     * @param mp MassPoint for which force should be computed
     * @param vehicle Physical vehicle for which force should be computed
     * @return RealVector that represents the force
     */
    private static RealVector calcAirFrictionForce(MassPoint mp, PhysicalVehicle vehicle){
        // Force: Air friction Fa = -0.5 * air density * velocity^2 * drag coefficient * area hit by wind
        RealVector forceAirFriction = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        RealVector velocity = mp.getVelocity();

        if (velocity.getNorm() == 0.0) {
            return forceAirFriction;
        }

        double areaX = 0.25 * vehicle.getSimulationVehicle().getHeight() * vehicle.getSimulationVehicle().getLength();
        double areaY = 0.25 * vehicle.getSimulationVehicle().getHeight() * vehicle.getSimulationVehicle().getWidth();
        double areaZ = 0.25 * vehicle.getSimulationVehicle().getLength() * vehicle.getSimulationVehicle().getWidth();

        // For a rotation matrix it holds: inverse(matrix) = transpose(matrix)
        // Rotate velocityOrientation back to global coordinate system axis to match up with area values
        RealVector velocityOrigin = vehicle.getRotationMatrix().transpose().operate(velocity);

        // Fractions of area values for each axis according to velocity vector orientation and with no more vehicle rotation
        areaX = areaX * (velocityOrigin.getEntry(0) / velocityOrigin.getL1Norm());
        areaY = areaY * (velocityOrigin.getEntry(1) / velocityOrigin.getL1Norm());
        areaZ = areaZ * (velocityOrigin.getEntry(2) / velocityOrigin.getL1Norm());

        // Sum of all fractions for area values yields correct approximation of total area for air resistance
        double area = Math.abs(areaX) + Math.abs(areaY) + Math.abs(areaZ);

        // Scalar for air friction force computations
        double scalarCoefficient = -0.5 * PhysicsEngine.AIR_DENSITY * PhysicsEngine.AIR_DRAG_CAR * area;

        // Final force computation, preserve direction that we need for computations in the 3D space
        RealVector direction = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        direction.setEntry(0, (velocity.getEntry(0) < 0.0 ? -1.0 : 1.0));
        direction.setEntry(1, (velocity.getEntry(1) < 0.0 ? -1.0 : 1.0));
        direction.setEntry(2, (velocity.getEntry(2) < 0.0 ? -1.0 : 1.0));
        forceAirFriction = velocity.ebeMultiply(velocity).ebeMultiply(direction).mapMultiply(scalarCoefficient);
        return forceAirFriction;
    }
}
