package sensors;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;

import com.jhlabs.image.MotionBlurFilter;
import com.jhlabs.image.PerspectiveFilter;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Aklima Zaman on 2/8/2017.
 */
public class CameraSensor extends AbstractSensor {
    private Optional<Image> value = Optional.empty();
    private Optional<Image> rightImage = Optional.empty();
    private Optional<Image> originalImage = Optional.empty();
    // camera parameter TODO: get them from the real used camera
    private double cameraHFOV = 90.4642140657; // [deg]
    private double focalDistance = 0.1369274884123756; // [m]
    private double baseline = 0.02; // [m]

    public Optional<Image> getOriginalImage() {
        return originalImage;
    }

    public CameraSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_CAMERA;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getTypeName() {
        return Image.class.getTypeName();
    }

    public Optional<Image> getRightImage() {
        return this.rightImage;
    }

    public Optional<Image> getLeftImage() {
        return this.value;
    }

    @Override
    protected void calculateValue() {
        Optional<Image> temp = getPhysicalVehicle().getSimulationVehicle().getCameraImage();
        if (temp.isPresent()) {
            this.originalImage = Optional.of(temp.get());
            // TODO: Add noise in Image
            ImagePlus imagePlus = new ImagePlus();
            imagePlus.setImage(temp.get());
            try {
                int width = imagePlus.getWidth() / 2;
                this.value = Optional.of(cropImage(imagePlus, 0, 0, width, imagePlus.getHeight()));
                this.rightImage = Optional
                        .of(cropImage(imagePlus, imagePlus.getWidth() / 2, 0, width, imagePlus.getHeight()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private BufferedImage cropImage(ImagePlus imp, int x, int y, int width, int height) throws Exception {

        ImageProcessor ip = imp.getProcessor();
        ip.setInterpolationMethod(ImageProcessor.BILINEAR);

        Rectangle roi = new Rectangle();
        roi.setBounds(x, y, width, height);
        ip.setRoi(roi);
        ImageProcessor cropped = ip.crop();
        return cropped.getBufferedImage();
    }

    /**
     * @return the horizontal field of view (opening angle) in [deg]
     */
    public double getCameraHFOV() {
        return cameraHFOV;
    }

    /**
     * @return the focal distance of the used camera in [m]
     */
    public double getFocalDistance() {
        return focalDistance;
    }

    /**
     * @return the distance between the stereo images in [m]
     */
    public double getBaseline() {
        return baseline;
    }

    public static BufferedImage perspectivefilture(BufferedImage bi, String perspectiveDirection) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        float perspectiveTop = 3f;
        float perspectiveBottom = 3f;

        // TODO read from artwork profile
        int top3d = (int) (h * perspectiveTop / 10);
        // int top3d = (int) (h * perspectiveTop / 100);
        int bot3d = (int) (h * perspectiveBottom / 100);

        PerspectiveFilter perspectiveFilter = new PerspectiveFilter();
        // Top Left (x/y), Top Right (x/y), Bottom Right (x/y), Bottom Left
        // (x/y)

        if ("right".equalsIgnoreCase(perspectiveDirection)) {
            perspectiveFilter.setCorners(0, 0, w, top3d, w, h - bot3d, 0, h);
        } else {
            perspectiveFilter.setCorners(0, top3d, w, 0, w, h, 0, h - bot3d);
        }
        return perspectiveFilter.filter(bi, null);
    }

    public static BufferedImage motionBlurFilter(BufferedImage bi) {
        MotionBlurFilter blurFilter = new MotionBlurFilter(0.1f, 0.1f, 0.12f, 0.001f);

        BufferedImage im = blurFilter.filter(bi, null);

        return im;
    }

}
