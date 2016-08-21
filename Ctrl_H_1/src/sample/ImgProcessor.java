package sample;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.isNaN;
import static java.lang.Math.*;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.moments;

/**
 * Created by TANMAY on 10-01-2016.
 */
public class ImgProcessor implements Runnable{
    static VideoCapture cam;
    static Mat Frame;
    boolean ThisThreadRunning;
    static boolean ImgProcessorThreadsRunning;
    static boolean global_FrameUpdated=false; //prevent updatation if not used by this object
    //wait for other objects to process image before updating
    boolean Obj_detectesd;
    ObjImgParams params;
    ImageView op_iv;
    ImageView debug_iv;
    int inc=2;
    double ImgObjCoords[]=new double[3];
    double CurrImgObjCoords[]={0,RobotParams.RightArm.arm_y_offset,RobotParams.RightArm.x+RobotParams.RightArm.d+RobotParams.RightArm.arm_z_offset};
    boolean TrackObject;

    double posX,posY;

    ImgProcessor(ObjImgParams params, Mat Frame,boolean TrackObject, ImageView op_iv)
    {
        cam=new VideoCapture(0);
        this.params=params;
        this.op_iv=op_iv;
        this.debug_iv=op_iv;//prevent null pointer
        this.TrackObject=TrackObject;
    }
    ImgProcessor(ObjImgParams params, Mat Frame,boolean TrackObject, ImageView op_iv,ImageView debug_iv)
    {
        this.Frame=Frame;

        this.params=params;
        this.op_iv=op_iv;
        this.debug_iv=debug_iv;
        this.TrackObject=TrackObject;
        ImgProcessorThreadsRunning=true;
        ThisThreadRunning=true;
    }
    @Override
    public void run() {
        //identify();

        System.out.println("Thread running");
        //System.out.println(cam.isOpened());
        //ReadFrame();
        if(Frame!=null)
            System.out.println("Frame not null");
        System.out.println(Frame.channels());
        System.out.println(Frame.hashCode());
        System.out.println(Frame.rows());
        System.out.println(Frame.cols());
        while(ThisThreadRunning && ImgProcessorThreadsRunning)
        {
            //if (!global_FrameUpdated)
            //    ReadFrame();
            //process updated frame(in this object)
            if(Frame!=null) {
                Mat blurredImage = new Mat();
                Mat hsvImage = new Mat();
                Mat mask = new Mat();
                Mat morphOutput = new Mat();


                Imgproc.blur(Frame, blurredImage, new Size(7, 7));

                //hsvImage=blurredImage;

                Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

                Core.inRange(hsvImage, params.minValues, params.maxValues, mask);

// show the partial output
                Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, params.dilateElementSize);
                Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, params.erodeElementSize);

                Imgproc.erode(mask, morphOutput, erodeElement);
                Imgproc.erode(mask, morphOutput, erodeElement);

                Imgproc.dilate(mask, morphOutput, dilateElement);
                Imgproc.dilate(mask, morphOutput, dilateElement);


                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //op_iv.setImage(MatToImage(morphOutput));
                GUI_Event_Handler.load_image(mask,op_iv);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //if(!global_FrameUpdated)
                //    notifyAll();//notify other threads

                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();

// find contours
                Imgproc.findContours(morphOutput, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

// if any contour exist...
                Mat FrameOut;
                FrameOut=Frame.clone();
                if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
                {
                    // for each contour, display it in blue
                    for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
                        Imgproc.drawContours(FrameOut, contours, idx, new Scalar(0, 255, 0),3);

                }
                Moments moments=moments(morphOutput);
                double dM01 =moments.m01;
                double dM10 = moments.m10;
                double dArea = moments.m00;
                posX = dM10 / dArea;
                posY = dM01 / dArea;

                if (posX > 0 && posX <= 640 && posY > 0 && posY <= 480 && !isNaN(posX) && !isNaN(posY))
                {
                    Obj_detectesd = true;

                    if(TrackObject) {
                        int offset = (int) (posX - 320);
                        inc = (int) (10.0 * offset / 320.0);
                        //System.out.println("off; "+offset+"\tinc: "+inc+"\tposX: "+posX);
                        if ((RobotParams.ServoAngles[0] + inc <= 150 && inc > 0) || (RobotParams.ServoAngles[0] + inc > 30 && inc < 0))
                            RobotParams.ServoAngles[0] += inc;


                        offset = (int) (200 - posY);
                        inc = (int) (8.0 * offset / 240.0);
                        //if(posY>480*2/3 && RobotParams.ServoAngles[1]>50)
                        if ((RobotParams.ServoAngles[1] + inc < 140 && inc > 0) || (RobotParams.ServoAngles[1] + inc > 30 && inc < 0))
                            RobotParams.ServoAngles[1] += inc;
                        //else if(posY<480*1/3 && RobotParams.ServoAngles[1]<140)
                        //     RobotParams.ServoAngles[1]+=inc;
                        calcCoord();

                    }
                }
                else {
                    Obj_detectesd = false;
                    if(TrackObject) {
                        if (RobotParams.ServoAngles[0] > 90.5)
                            RobotParams.ServoAngles[0]--;
                        else if (RobotParams.ServoAngles[0] < 89.5)
                            RobotParams.ServoAngles[0]++;
                        if (RobotParams.ServoAngles[1] > 90.5)
                            RobotParams.ServoAngles[1]--;
                        else if (RobotParams.ServoAngles[1] < 89.5)
                            RobotParams.ServoAngles[1]++;
                    }
                }
                //System.out.println("X: "+posX+"\tY: "+posY);
                //Imgproc.goodFeaturesToTrack(FrameOut,contours.get(0),4,2,10);
                //1debug_iv.setImage(MatToImage(FrameOut));
                if(debug_iv!=null)
                    GUI_Event_Handler.load_image(FrameOut,debug_iv);
                global_FrameUpdated = false;
            }

        }

    }
    void identify()
    {
        Mat image;
        image = imread("E:/logo.jpg");
        MatOfKeyPoint keyPoints = null;
        DescriptorExtractor descriptorExtractor;
        DescriptorMatcher descriptorMatcher;
        Mat descriptors;

        // Create a SIFT keypoint detector.
        FeatureDetector detector = FeatureDetector.create(4);;
        descriptorExtractor=DescriptorExtractor.create(2);//SURF = 2
        descriptorMatcher=DescriptorMatcher.create(6); //BRUTEFORCE_SL2 = 6**
        detector.detect(image, keyPoints);
        System.out.println("no. of keypoints: "+keyPoints.size());


        //detector.
    }
    void calcCoord()
    {
        //RobotParams.UTS_val=140;
        double pan_rad=RobotParams.ServoAngles[0]*PI/180.0-PI/2.0;
        double tilt_rad=RobotParams.ServoAngles[1]*PI/180.0-PI/2.0;

        ImgObjCoords[2]=RobotParams.UTS_val*sin(tilt_rad)+35;//Z
        ImgObjCoords[1]=RobotParams.UTS_val*cos(tilt_rad)*sin(pan_rad);//Y
        ImgObjCoords[0]=RobotParams.UTS_val*cos(tilt_rad)*cos(pan_rad)-10;//X

        System.out.println("Obj X: "+ImgObjCoords[0]+"\tY: "+ImgObjCoords[1]+"\tZ :"+ImgObjCoords[2]+"\td: "+RobotParams.UTS_val);
        //System.out.println("Cur X: "+CurrImgObjCoords[0]+"\tY: "+CurrImgObjCoords[1]+"\tZ :"+CurrImgObjCoords[2]+"\td: "+RobotParams.UTS_val);

    }
    static void ExitAllThreads()
    {
        ImgProcessorThreadsRunning=false;
    }
    void ExitThread()
    {
        ThisThreadRunning=false;
    }
    synchronized void ReadFrame()
    {
        try {
            if(global_FrameUpdated)
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cam.read(Frame);
        global_FrameUpdated=true;
    }
    Image MatToImage(Mat im) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", im, buffer);
        Image Im = new Image(new ByteArrayInputStream(buffer.toArray()));
        return Im;
    }
}
