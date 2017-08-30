package sensors;

import static org.junit.Assert.fail;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

import ij.ImagePlus;
import sensors.CameraSensor;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.PhysicalVehicleBuilder;

public class CameraSensorTest {
    private Image image;

    @Before
    public void LoadImage() {
        try {
            InputStream in = getClass().getResourceAsStream("/testImage.jpg");
            BufferedImage bufferedImage = ImageIO.read(in);

            PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
            PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(),
                    Optional.empty(), Optional.empty());
            Optional<Image> optional = Optional.of(bufferedImage);
            physicalVehicle.getSimulationVehicle().setCameraImage(optional);

            CameraSensor cameraSensor = new CameraSensor(physicalVehicle);
            cameraSensor.update();
            cameraSensor.getValue();
            this.image = cameraSensor.getLeftImage().get();
            // To see the image
/*            new ImagePlus("Original", cameraSensor.getOriginalImage().get()).show();
            new ImagePlus("LeftImage", (Image) cameraSensor.getLeftImage().get()).show();
            new ImagePlus("RightImage", cameraSensor.getRightImage().get()).show();*/

        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void applyPerspectiveFilter() {
        try {
            BufferedImage filteredImage = CameraSensor
                    .perspectivefilture((new ImagePlus("Original", this.image).getProcessor()).getBufferedImage(), "right");
            // ImagePlus ip = new ImagePlus("Filtured", filteredImage);
            // ip.show();
            
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void applyMotionBlurFilter() {
        try {
            BufferedImage filteredImage = CameraSensor
                    .motionBlurFilter((new ImagePlus("Original", this.image).getProcessor()).getBufferedImage());

            // new ImagePlus("MotionBlurFilter", filteredImage).show();
        } catch (Exception e) {
            fail();
        }


    }

}
