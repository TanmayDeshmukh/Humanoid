package sample;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;

/**
 * Created by SHANTANU on 11-01-2016.
 */
public class GUI_Event_Handler implements EventHandler {
    @Override
    public void handle(Event event) {
        System.out.println("Event triggered");

    }
    static synchronized void load_image(Mat image, ImageView iv)
    {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", image, buffer);
        Image Im = new Image(new ByteArrayInputStream(buffer.toArray()));
        iv.setImage(Im);
    }
}
