package sample;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;

/**
 * Created by TANMAY on 11-01-2016.
 */
public class Capture_Grabber implements Runnable{
    VideoCapture cam;
    Mat Frame;
    boolean Thread_running;
    ImageView op;

    Capture_Grabber(int ID, Mat Frame,ImageView output)
    {
        cam= new VideoCapture(ID);
        this.Frame=Frame;
        Thread_running=true;
        this.op=output;

    }

    @Override
    public void run() {

        System.out.print("Capture Thread Running: ");
        System.out.println(this.hashCode());
        while (Thread_running)
        {
            cam.read(Frame);
            //op.setImage(MatToImgage(Frame));
            GUI_Event_Handler.load_image(Frame,op);
        }

    }
    void ExitThread()
    {
        Thread_running=false;
        cam.release();
    }
    Image MatToImgage(Mat im) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", im, buffer);
        Image Im = new Image(new ByteArrayInputStream(buffer.toArray()));
        return Im;
    }
}
