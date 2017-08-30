package simulation.vehicle;

import org.apache.commons.math3.linear.RealVector;

/**
 * Class that represents a mass point of a rigid body
 */
public class MassPoint {

    /** Type of mass point */
    private MassPointType type;

    /** x_i bar of formula: Position relative to center of mass of rigid body (local coordinate system) */
    private RealVector localPos;

    /** r_i bar of formula: Vector pointing from center of mass of rigid body to mass point (local coordinate system) */
    private RealVector localCenterDiff;

    /** x_i of formula: Position relative to global coordinate system */
    private RealVector pos;

    /** r_i of formula: Vector pointing from center of mass of rigid body to mass point (global coordinate system) */
    private RealVector centerDiff;

    /** x_i dot of formula: Velocity relative to global coordinate system */
    private RealVector velocity;

    /** x_i dot dot of formula: Acceleration relative to global coordinate system */
    private RealVector acceleration;

    /** f_i of formula: Acceleration relative to global coordinate system */
    private RealVector force;

    /** m_i of formula: Mass of the mass point */
    private double mass;

    /** Ground Z position of mass point (for performance improvements) */
    private double groundZ;

    /** Pressure value for the mass point (e.g. tire pressure) initialized to 0.0 */
    private double pressure;

    /**
     * Constructor for a mass point that takes all variables
     * Uses deep copy of vectors to avoid that vectors can be modified externally
     *
     * @param type Type of the mass point
     * @param localPos Position vector of mass point in local coordinate system
     * @param localCenterDiff Center difference vector of mass point in local coordinate system
     * @param pos Position vector of mass point in global coordinate system
     * @param centerDiff Center difference vector of mass point in global coordinate system
     * @param velocity Velocity vector of the mass point
     * @param acceleration Acceleration vector of the mass point
     * @param force Force vector of the mass point
     * @param mass Mass of the mass point
     */
    public MassPoint(MassPointType type, RealVector localPos, RealVector localCenterDiff, RealVector pos, RealVector centerDiff, RealVector velocity, RealVector acceleration, RealVector force, double mass) {
        this.type = type;
        this.localPos = localPos.copy();
        this.localCenterDiff = localCenterDiff.copy();
        this.pos = pos.copy();
        this.centerDiff = centerDiff.copy();
        this.velocity = velocity.copy();
        this.acceleration = acceleration.copy();
        this.force = force.copy();
        this.mass = mass;
        this.groundZ = 0.0;
        this.pressure = 0.0;
    }

    /**
     * Getter for type
     * @return Type of the mass point
     */
    public MassPointType getType() {
        return type;
    }

    /**
     * Getter for local position
     * @return Deep copy of the actual vector to avoid external modifications of vector data
     */
    public RealVector getLocalPos() {
        return localPos.copy();
    }

    /**
     * Getter for local center difference vector
     * @return Deep copy of the actual vector to avoid external modifications of vector data
     */
    public RealVector getLocalCenterDiff() {
        return localCenterDiff.copy();
    }

    /**
     * Getter for global position
     * @return Deep copy of the actual vector to avoid external modifications of vector data
     */
    public RealVector getPos() {
        return pos.copy();
    }

    /**
     * Getter for center difference vector
     * @return Deep copy of the actual vector to avoid external modifications of vector data
     */
    public RealVector getCenterDiff() {
        return centerDiff.copy();
    }

    /**
     * Getter for velocity
     * @return Deep copy of the actual vector to avoid external modifications of vector data
     */
    public RealVector getVelocity() {
        return velocity.copy();
    }

    /**
     * Getter for acceleration
     * @return Deep copy of the actual vector to avoid external modifications of vector data
     */
    public RealVector getAcceleration() {
        return acceleration.copy();
    }

    /**
     * Getter for force
     * @return Deep copy of the actual vector to avoid external modifications of vector data
     */
    public RealVector getForce() {
        return force.copy();
    }

    /**
     * Getter for mass
     * @return Mass of the mass point
     */
    public double getMass() {
        return mass;
    }

    /**
     * Setter for local position
     * @param localPos Input vector data that is deep copied to the mass point data to avoid external modifications
     */
    public void setLocalPos(RealVector localPos) {
        this.localPos = localPos.copy();
    }

    /**
     * Setter for local center difference vector
     * @param localCenterDiff Input vector data that is deep copied to the mass point data to avoid external modifications
     */
    public void setLocalCenterDiff(RealVector localCenterDiff) {
        this.localCenterDiff = localCenterDiff.copy();
    }

    /**
     * Setter for position
     * @param pos Input vector data that is deep copied to the mass point data to avoid external modifications
     */
    public void setPos(RealVector pos) {
        this.pos = pos.copy();
    }

    /**
     * Setter for center difference vector
     * @param centerDiff Input vector data that is deep copied to the mass point data to avoid external modifications
     */
    public void setCenterDiff(RealVector centerDiff) {
        this.centerDiff = centerDiff.copy();
    }

    /**
     * Setter for velocity
     * @param velocity Input vector data that is deep copied to the mass point data to avoid external modifications
     */
    public void setVelocity(RealVector velocity) {
        this.velocity = velocity.copy();
    }

    /**
     * Setter for acceleration
     * @param acceleration Input vector data that is deep copied to the mass point data to avoid external modifications
     */
    public void setAcceleration(RealVector acceleration) {
        this.acceleration = acceleration.copy();
    }

    /**
     * Setter for force
     * @param force Input vector data that is deep copied to the mass point data to avoid external modifications
     */
    public void setForce(RealVector force) {
        this.force = force.copy();
    }

    /**
     * Setter for mass
     * @param mass Mass for the mass point
     */
    public void setMass(double mass) {
        this.mass = mass;
    }

    /**
     * Getter for ground Z
     * @return Ground z
     */
    public double getGroundZ() {
        return groundZ;
    }

    /**
     * Setter for ground Z
     * @param groundZ Ground Z for the mass point
     */
    public void setGroundZ(double groundZ) {
        this.groundZ = groundZ;
    }

    /**
     * Getter for pressure
     * @return Pressure value
     */
    public double getPressure() {
        return pressure;
    }

    /**
     * Setter for pressure
     * @param pressure Pressure for the mass point
     */
    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    /**
     * Overwrite toString() to get a nice output for mass points
     * @return String that contains all information of a mass point
     */
    @Override
    public String toString() {
        return  "MassPoint " + hashCode() + ": type: " + type +
                " , localPos: " + localPos +
                " , localCenterDiff: " + localCenterDiff +
                " , pos: " + pos +
                " , centerDiff: " + centerDiff +
                " , velocity: " + velocity +
                " , acceleration: " + acceleration +
                " , force: " + force +
                " , mass: " + mass +
                " , groundZ: " + groundZ +
                " , pressure: " + pressure;
    }
}
