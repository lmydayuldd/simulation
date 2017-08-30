package simulation.util;
import org.jfree.chart.axis.NumberAxis;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.linear.RealVector;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.ApplicationFrame;
import org.jfree.chart.plot.XYPlot;

import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;



/**
 * Class that Plots the a 2D Graph for two datasets
 */
public class Plotter2D extends ApplicationFrame
{
    public final static int PLOTTER_OUTPUT_POSITION_XY = 1;
    public final static int PLOTTER_OUTPUT_VELOCITY = 2;
    public final static int PLOTTER_OUTPUT_Z = 3;

    /* Variables to storing coordinates */

    /* XY coordinates of each wheel position  */
    List <Double> Wheel_OneX = new LinkedList<Double>();
    List <Double> Wheel_OneY = new LinkedList<Double>();
    List <Double> Wheel_TwoX = new LinkedList<Double>();
    List <Double> Wheel_TwoY = new LinkedList<Double>();
    List <Double> Wheel_ThreeX = new LinkedList<Double>();
    List <Double> Wheel_ThreeY = new LinkedList<Double>();
    List <Double> Wheel_FourX = new LinkedList<Double>();
    List <Double> Wheel_FourY = new LinkedList<Double>();

    List <Double> Wheel_OneZ = new LinkedList<Double>();
    List <Double> Wheel_TwoZ = new LinkedList<Double>();
    List <Double> Wheel_ThreeZ = new LinkedList<Double>();
    List <Double> Wheel_FourZ = new LinkedList<Double>();

    List <JFreeChart> charts = new LinkedList<JFreeChart>();

    /* XYZ Position of the vehicle */
    private List<Double> vehiclePosX = new LinkedList<Double>();
    private List<Double> vehiclePosY = new LinkedList<Double>();
    private List<Double> vehiclePosZ = new LinkedList<Double>();

    /* XYZ Velocity of the vehicle */
    private List<Double> vehicleVelocityX = new LinkedList<Double>();
    private List<Double> vehicleVelocityY = new LinkedList<Double>();
    private List<Double> vehicleVelocityXY = new LinkedList<Double>();
    // private List<Double> vehicleVelocityZ = new LinkedList<Double>();

    /* Datasets for poltting*/
    private XYSeriesCollection WheelsDataSet = new XYSeriesCollection();
    private XYSeriesCollection VehicleVelDataset = new XYSeriesCollection();
    private XYSeriesCollection VehiclePosDataset = new XYSeriesCollection();


    /* Create XY Series objects for storing the XY coordinates of each wheel*/
    private XYSeries WheelOne = new XYSeries("WheelOne", false, true);
    private XYSeries WheelTwo = new XYSeries("WheelTwo", false, true);
    private XYSeries WheelThree = new XYSeries("WheelThree", false, true);
    private XYSeries WheelFour = new XYSeries("WheelFour", false, true);

    private XYSeries WheelOne_zAxis = new XYSeries("WheelOne", false, true);
    private XYSeries WheelTwo_zAxis = new XYSeries("WheelTwo", false, true);
    private XYSeries WheelThree_zAxis = new XYSeries("WheelThree", false, true);
    private XYSeries WheelFour_zAxis = new XYSeries("WheelFour", false, true);


    /* */
    private XYPlot PositionPlot;
    private XYPlot VelocityPlot;
    private XYPlot zAxisPlot;
    private JFreeChart VelocityChart;
    private JFreeChart PositionChart;
    private JFreeChart zPositionChart;



    /* A circle shape to be used by the plotting renderer */
    java.awt.geom.Ellipse2D.Double shape = new java.awt.geom.Ellipse2D.Double(-2.0, -2.0,   2.0, 2.0);

    /**
     * Constructor of the Plotter 2D object which responsible of creating
     * the charts for the different data sets related to the vehicle's
     * movement.
     * It returns a Plotter 2D object that contains graphs of the data set
     * specified by the "OutputType" string.
     * The charts could be displayed using specific functions of the JFree library
     * " pack()" and  "setVisible()"
     * @param  WheelsPosition The relative positions of the wheels respective center of mass
     * @param  vehiclePos Relative position of the center of mass of the vehicle
     * @param  vehicleVelocity Velocity of the vehicle's center of mass
     * @param  simulationTimePoints Discrete time vector duration of the simulation
     * @param  WheelRadius Radius of the wheels
     * @param  outputType an integer to define which data set to be returned for display
     */

    public Plotter2D(List<List<RealVector>> WheelsPosition, List<RealVector> vehiclePos, List<RealVector> vehicleVelocity, List<Long> simulationTimePoints, double WheelRadius, int outputType)
    {
        super("Vehicle Simulation Charts");

        // Creating charts for both position and velocity based on the provided datasets

        PositionChart = VehiclePositionChart ( WheelsPosition, vehiclePos, simulationTimePoints );
        zPositionChart = zAxisChart ( WheelsPosition, WheelRadius, simulationTimePoints );
        VelocityChart = VehicleVelocityChart ( vehicleVelocity, simulationTimePoints );

        final ChartPanel PositionPanel = new ChartPanel(PositionChart, true, true, true, true, true);
        final ChartPanel VelocityPanel = new ChartPanel(VelocityChart, true, true, true, true, true);
        final ChartPanel zAxisPanel = new ChartPanel(zPositionChart, true, true, true, true, true);

        // Chart dimensions
        VelocityPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        PositionPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        zAxisPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        // Selecting which data charts to return for display
        switch (outputType) {
            case PLOTTER_OUTPUT_POSITION_XY: setContentPane(PositionPanel); break;
            case PLOTTER_OUTPUT_VELOCITY: setContentPane(VelocityPanel); break;
            case PLOTTER_OUTPUT_Z: setContentPane(zAxisPanel); break;
            default: Log.warning("Plotter: Incorrect plotter output type"); break;
        }
    }


    /**
     * Function that creates the Vehicle position 2D plot charts
     * for both the position of the center of mass of the vehicle
     * and the four wheels centers of mass.
     *
     * @param Wheels_Positions The Relative positions of the wheels respective center of mass
     * @param VehicleMassPointPosition Relative position of the center of mass of the vehicle
     * @param  SimTime Discrete time vector of duration of the simulation
     * @return Returns a JFree chart object for which a panel needs to be created for display
     */

    private JFreeChart VehiclePositionChart (List<List<RealVector>> Wheels_Positions, List<RealVector> VehicleMassPointPosition, List<Long> SimTime )
    {
            // Axis name setting
            final NumberAxis xAxis = new NumberAxis("X Axis");
            final ValueAxis yAxis = new NumberAxis("Y axis");

            // Creating Position Dataset for vehicle and wheels center of mass
            final XYDataset Wheels = WheelsDataset(Wheels_Positions);
            final XYDataset Position = VehiclePositionDataset(VehicleMassPointPosition, SimTime);

            // Creating and customizing renderer setting
            final XYLineAndShapeRenderer VehiclePositionRenderer = new XYLineAndShapeRenderer();
            VehiclePositionRenderer.setBaseShapesFilled(false);
            VehiclePositionRenderer.setSeriesPaint( 0 , Color.BLUE );
            VehiclePositionRenderer.setSeriesShape(0, shape);
            VehiclePositionRenderer.setSeriesPaint( 1 , Color. RED );
            VehiclePositionRenderer.setSeriesShape(1, shape);
            VehiclePositionRenderer.setSeriesPaint( 2 , Color.GREEN );
            VehiclePositionRenderer.setSeriesShape(2, shape);
            VehiclePositionRenderer.setSeriesPaint( 3 , Color.YELLOW );
            VehiclePositionRenderer.setSeriesShape(3, shape);
            VehiclePositionRenderer.setSeriesPaint( 4 , Color.BLACK );

            VehiclePositionRenderer.setSeriesLinesVisible(0, false);
            VehiclePositionRenderer.setSeriesLinesVisible(1, false);
            VehiclePositionRenderer.setSeriesLinesVisible(2, false);
            VehiclePositionRenderer.setSeriesLinesVisible(3, false);
            VehiclePositionRenderer.setSeriesLinesVisible(4, false);

            // Creating Position Plot object for wheels and adding the vehicle Center of mass
            PositionPlot = new XYPlot(Wheels, xAxis, yAxis, VehiclePositionRenderer);
            PositionPlot.setDataset(1, Position);


            return new JFreeChart("Vehicle Simulation : Vehicle Position Chart", JFreeChart.DEFAULT_TITLE_FONT, PositionPlot, true);

    }

    /**
     * Function that creates the Vehicle position 2D plot charts
     * for both the position of the center of mass of the vehicle
     * and the four wheels centers of mass.
     *
     * @param WheelsPosition Relative position of the center of mass of the vehicle
     * @param radius Radius of the wheels
     * @param  SimTime Discrete time vector of duration of the simulation
     * @return Returns a JFree chart object for which a panel needs to be created for display
     */

    private JFreeChart zAxisChart (List<List<RealVector>> WheelsPosition, double radius,  List<Long> SimTime )
    {
        // Axis name setting
        final NumberAxis xAxis = new NumberAxis("Time ");
        final ValueAxis yAxis = new NumberAxis("Z ");

        // Creating Position Dataset for vehicle and wheels center of mass
        final XYDataset VehicleZAxis = zAxisDataset(WheelsPosition, radius, SimTime);

        // Creating and customizing renderer setting
        final XYLineAndShapeRenderer zPositionRenderer = new XYLineAndShapeRenderer();

        zPositionRenderer.setBaseShapesFilled(false);
        zPositionRenderer.setSeriesPaint( 0 , Color.BLUE );
        zPositionRenderer.setSeriesShape(0, shape);
        zPositionRenderer.setSeriesPaint( 1 , Color. RED );
        zPositionRenderer.setSeriesShape(1, shape);
        zPositionRenderer.setSeriesPaint( 2 , Color.GREEN );
        zPositionRenderer.setSeriesShape(2, shape);
        zPositionRenderer.setSeriesPaint( 3 , Color.YELLOW );
        zPositionRenderer.setSeriesShape(3, shape);
        zPositionRenderer.setSeriesPaint( 4 , Color.BLACK );

        zPositionRenderer.setSeriesLinesVisible(0, false);
        zPositionRenderer.setSeriesLinesVisible(1, false);
        zPositionRenderer.setSeriesLinesVisible(2, false);
        zPositionRenderer.setSeriesLinesVisible(3, false);
        zPositionRenderer.setSeriesLinesVisible(4, false);

        // Creating Position Plot object for wheels and adding the vehicle Center of mass
        zAxisPlot  = new XYPlot(VehicleZAxis, xAxis, yAxis, zPositionRenderer);

        return new JFreeChart("Vehicle Simulation : Vehicle zAxis Position Chart", JFreeChart.DEFAULT_TITLE_FONT, zAxisPlot, true);

    }

    /**
     * Function that creates the Vehilcle Velocity 2D plot charts
     * for the center of mass of the vehicle
     *
     * @param VehicleMassPointVelocity Relative Velocity of the center of mass of the vehicle
     * @param  SimTime Discrete time vector of duration of the simulation
     * @return Returns a JFree chart object for which a panel needs to be created for display
     */
    private JFreeChart VehicleVelocityChart (List<RealVector> VehicleMassPointVelocity,   List<Long> SimTime )
    {
        // Axis name setting
        final NumberAxis xAxis = new NumberAxis("Simulation Time (ms)");
        final ValueAxis yAxis = new NumberAxis("Vehicle Velocity");

        // Creating Velocity Dataset
        final XYDataset Velocity = VehicleVelocityDataset(VehicleMassPointVelocity, SimTime);


        // Creating and customizing renderer setting
        final XYLineAndShapeRenderer VelocityPlotRenderer = new XYLineAndShapeRenderer();
        VelocityPlotRenderer.setBaseShapesFilled(false);
        VelocityPlotRenderer.setSeriesPaint( 0 , Color.BLUE );
        VelocityPlotRenderer.setSeriesShape(0, shape);

        VelocityPlotRenderer.setSeriesLinesVisible(0, false);

        // Creating Velocity Plot object for vehicle
        VelocityPlot = new XYPlot(Velocity, xAxis, yAxis, VelocityPlotRenderer);


        return new JFreeChart("Vehicle Simulation : Velocity Chart", JFreeChart.DEFAULT_TITLE_FONT, VelocityPlot, true);

    }

    /**
     * Function that creates XY datasets for every wheel center of mass
     *
     * @param Wheels_Position a vector containing the XYZ coordinates of the wheels obtained during the simulation
     * @return Returns an XY series representing the relative position of every wheel in the plane for plotting
     */
    private XYDataset WheelsDataset( List<List<RealVector>> Wheels_Position)
    {


        // Retrieving XY corrdinates of each of the 4 wheels of the vehicle
        for(int i=0; i<Wheels_Position.size(); i++){

            // getting XY coordinates from the input vector
            Wheel_OneX.add(Wheels_Position.get(i).get(0).getEntry(0));
            Wheel_OneY.add(Wheels_Position.get(i).get(0).getEntry(1));
            Wheel_TwoX.add(Wheels_Position.get(i).get(1).getEntry(0));
            Wheel_TwoY.add(Wheels_Position.get(i).get(1).getEntry(1));
            Wheel_ThreeX.add(Wheels_Position.get(i).get(2).getEntry(0));
            Wheel_ThreeY.add(Wheels_Position.get(i).get(2).getEntry(1));
            Wheel_FourX.add(Wheels_Position.get(i).get(3).getEntry(0));
            Wheel_FourY.add(Wheels_Position.get(i).get(3).getEntry(1));

            // storing XY coordinates in their respective series
            WheelOne.add( Wheel_OneX.get(i) , Wheel_OneY.get(i) );
            WheelTwo.add( Wheel_TwoX.get(i) , Wheel_TwoY.get(i) );
            WheelThree.add( Wheel_ThreeX.get(i) , Wheel_ThreeY.get(i) );
            WheelFour.add( Wheel_FourX.get(i) , Wheel_FourY.get(i) );

        }

            // Create a parallel series collection to store data of the 4 wheels

            WheelsDataSet.addSeries( WheelOne );
            WheelsDataSet.addSeries( WheelTwo );
            WheelsDataSet.addSeries( WheelThree );
            WheelsDataSet.addSeries( WheelFour );

            return WheelsDataSet;
    }


    /**
     * Function that creates XY position dataset for vehicle center of mass
     *
     * @param VehicleMassPointPosition vector containing XYZ coordinates of the vehicle COM obtained during simulation
     * @param  SimTime Discrete time vector of duration of the simulation
     * @return Returns an XY series representing the relative position of the vehicle COM  in the plane for plotting
     * COM = Center of Mass
     */

    private XYDataset VehiclePositionDataset( List<RealVector> VehicleMassPointPosition,  List<Long> SimTime)
    {

        // Create XY Series objects for storing the XY coordinates of vehicle

        // X = f(t), Y = f(t)
        final XYSeries VehiclePosX = new XYSeries("X = f(t) ", false, true);
        final XYSeries VehiclePosY = new XYSeries(" Y = f(t) ", false, true);
        // X = f(Y)
        final XYSeries VehicleXYPos = new XYSeries("Y = f(X)", false, true);

        // Retrieving XY corrdinates of the vehicle
        for(int i=0; i<VehicleMassPointPosition.size(); i++){

            // getting XY coordinates from the input vector
            vehiclePosX.add(VehicleMassPointPosition.get(i).getEntry(0));
            vehiclePosY.add(VehicleMassPointPosition.get(i).getEntry(1));
            //vehiclePosZ.add(VehicleMassPointPosition.get(i).getEntry(2));

            // Create three element vectors : X versus time, Y versus time, and Y versus X
//            VehiclePosX.add( SimTime.get(i) , vehiclePosX.get(i));
//            VehiclePosY.add( SimTime.get(i) , vehiclePosY.get(i));
              VehicleXYPos.add( vehiclePosX.get(i) , vehiclePosY.get(i));

        }

        // Create a parallel series collection to store data X=f(t), Y=f(t), Y=f(X)
        final XYSeriesCollection VehiclePosDataset = new XYSeriesCollection();
//            VehiclePosDataset.addSeries( VehiclePosX );
//            VehiclePosDataset.addSeries( VehiclePosY );
              VehiclePosDataset.addSeries( VehicleXYPos );

        return VehiclePosDataset;
    }

    /**
     * Function that creates Z position dataset for vehicle center of mass
     *
     * @param Wheels_Position vector containing XYZ coordinates of the vehicle COM obtained during simulation
     * @param  SimTime Discrete time vector of duration of the simulation
     * @param WheelRadius Radius of the wheels
     * @return Returns an XY series representing the relative Z position of the vehicle COM  in the plane for plotting
     * COM = Center of Mass
     */

    private XYDataset zAxisDataset( List<List<RealVector>> Wheels_Position, double WheelRadius,  List<Long> SimTime)
    {

        // Create XY Series objects for storing the Z coordinates of vehicle

        // X = f(t), Y = f(t)
        double groundZ = 0.0;
        double WheelOne_zAxisValue;
        double WheelTwo_zAxisValue;
        double WheelThree_zAxisValue;
        double WheelFour_zAxisValue;

        // Retrieving Z corrdinates of the vehicle
        for(int i=0; i<Wheels_Position.size(); i++){

            // getting XY coordinates from the input vector
            //vehiclePosX.add(VehicleMassPointPosition.get(i).getEntry(0));
            //vehiclePosY.add(VehicleMassPointPosition.get(i).getEntry(1));

            WheelOne_zAxisValue = Wheels_Position.get(i).get(0).getEntry(2) - groundZ - WheelRadius;
            WheelTwo_zAxisValue = Wheels_Position.get(i).get(1).getEntry(2) - groundZ - WheelRadius;
            WheelThree_zAxisValue = Wheels_Position.get(i).get(2).getEntry(2) - groundZ - WheelRadius;
            WheelFour_zAxisValue = Wheels_Position.get(i).get(3).getEntry(2) - groundZ - WheelRadius;


            // getting XY coordinates from the input vector
            Wheel_OneZ.add(WheelOne_zAxisValue);
            Wheel_TwoZ.add(WheelTwo_zAxisValue);
            Wheel_ThreeZ.add(WheelThree_zAxisValue);
            Wheel_FourZ.add(WheelFour_zAxisValue);


            // storing XY coordinates in their respective series
            WheelOne_zAxis.add( SimTime.get(i) , Wheel_OneZ.get(i) );
            WheelTwo_zAxis.add( SimTime.get(i) , Wheel_TwoZ.get(i) );
            WheelThree_zAxis.add( SimTime.get(i) , Wheel_ThreeZ.get(i) );
            WheelFour_zAxis.add( SimTime.get(i) , Wheel_FourZ.get(i) );
            // Create three element vectors : X versus time, Y versus time, and Y versus X
//            VehiclePosX.add( SimTime.get(i) , vehiclePosX.get(i));
//            VehiclePosY.add( SimTime.get(i) , vehiclePosY.get(i));

        }

        // Create a parallel series collection to store data Z=f(t)
        final XYSeriesCollection VehicleZAxisDataset = new XYSeriesCollection();
              VehicleZAxisDataset.addSeries( WheelOne_zAxis );
              VehicleZAxisDataset.addSeries( WheelTwo_zAxis );
              VehicleZAxisDataset.addSeries( WheelThree_zAxis );
              VehicleZAxisDataset.addSeries( WheelFour_zAxis );

        return VehicleZAxisDataset;
    }



    /**
     * Function that creates Velocity dataset for vehicle center of mass
     *
     * @param VehicleMassPointVelocity vector containing XYZ velocities of the vehicle COM obtained during simulation
     * @param SimTime SimTime Discrete time vector of duration of the simulation
     * @return Returns an XY series representing the relative position of the vehicle COM  in the plane for plotting
     * COM = Center of Mass
     */

    private XYDataset VehicleVelocityDataset(List<RealVector> VehicleMassPointVelocity, List<Long> SimTime)
    {

        // Create XY Series objects for storing the velocityof vehicle

        // Vx = f(t), Vy = f(t)
        final XYSeries VehicleVelX = new XYSeries("Vx = f(t)", false, true);
        final XYSeries VehicleVelY = new XYSeries("Vy = f(t)", false, true);
        // V = f(t)
        final XYSeries VehicleVelXY = new XYSeries("V = f(t)", false, true);
//        final XYSeries VehicleVelZ = new XYSeries("WheelFour", false, true);


        // Retrieving velocity corrdinates of the vehicle
        for(int i=0; i<VehicleMassPointVelocity.size(); i++){

            // getting Vx Vy velocity coordinates from the input vector
            vehicleVelocityX.add(VehicleMassPointVelocity.get(i).getEntry(0));
            vehicleVelocityY.add(VehicleMassPointVelocity.get(i).getEntry(1));
//            vehicleVelocityZ.add(VehicleMassPointVelocity.get(i).getEntry(2));

            // Calculating the vehicle's velocity based on Vx Vy
            vehicleVelocityXY.add(Math.sqrt(Math.pow(vehicleVelocityX.get(i),2) + Math.pow(vehicleVelocityY.get(i),2)));
//            VehicleVelX.add( SimTime.get(i) , vehicleVelocityX.get(i));
//            VehicleVelY.add( SimTime.get(i) , vehicleVelocityY.get(i));

            // Adding the vehicle velocity V = f(Vx,Vy)
            VehicleVelXY.add( SimTime.get(i) , vehicleVelocityXY.get(i));
//            VehicleVelZ.add( SimTime.get(i) , vehiclePosZ.get(i));

        }


        // Create a parallel series collection to store data Vx=f(t), Vy=f(t), V=f(Vx, Vy)


//            VehicleVelDataset.addSeries( VehicleVelX );
//            VehicleVelDataset.addSeries( VehicleVelY );
            VehicleVelDataset.addSeries( VehicleVelXY );

        // Return Data for display
        return VehicleVelDataset;
    }


    /* Getters */
    /* */
    public XYSeriesCollection getWheelsDataSet() {return WheelsDataSet;}

    public XYSeriesCollection getVehicleVelDataSet() {return VehicleVelDataset;}

    public XYSeriesCollection getVehiclePosDataSet() {return VehiclePosDataset;}

    public JFreeChart getPositionChart() {return PositionChart;}

    public JFreeChart getVelocityChart() {return VelocityChart;}

    public JFreeChart getZPositionChart() {return zPositionChart;}

    public XYSeries getWheelOne() {return WheelOne;}

    public XYSeries getWheelTwo() {return WheelTwo;}

    public XYSeries getWheelThree() {return WheelThree;}

    public XYSeries getWheelFour() {return WheelFour;}

    public XYPlot getPositionPlot(){return PositionPlot;}

    public XYPlot getVelocityPlot(){return VelocityPlot;}

    }