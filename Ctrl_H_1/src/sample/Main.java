package sample;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static java.lang.Math.*;

public class Main extends Application {

    Button con,discon,follow,cameraTrack;
    ImageView imv_1, imv_1a,imv_1b,imv_1c,imv_2;
    Text txt_fps;
    GridPane root;
    ComboBox Com_sel;
    Mat Frame;
    com.fazecast.jSerialComm.SerialPort Ports[];
    Button B1,B2,B3,B4,B5,B6,B7,B8;
    Slider S1,S2,S3;
    Text T1,T2,T3,T4,T5,T6;
    Text Sn1,Sn2,Sn3,Sn4;

    final NumberFormat format = new DecimalFormat("#000.0");

    ObjImgParams Img_params;
    ObjImgParams orange,red;
    RobotParams BotParams;
    ImgProcessor Processor1;

    boolean FollowObject;
    boolean TargetReached=false;
    boolean CVcorrectionDone=false;
    boolean IRCorrectionDone=false;
    boolean CameraTrack=false;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        BotParams=new RobotParams();
        //
        // Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        //StackPane root=new StackPane();
        root = new GridPane();
        // root.getColumnConstraints().add(new ColumnConstraints(100)); // column 0 is 100 wide
        //root.getColumnConstraints().add(new ColumnConstraints(200)); // column 1 is 200 wide
        //root.getRowConstraints().add(0,new RowConstraints(200));
        Ports= SerialPort.getCommPorts();
        if(Ports.length!=0)
            for (SerialPort Port : Ports) System.out.println(Port.getSystemPortName());

            OperatingSystemMXBean bean =
                    ManagementFactory.getOperatingSystemMXBean( );

        System.out.println(bean.getAvailableProcessors());
        Scene scene=new Scene(root, 1000, 550, new Color(0.1, 0.1, 0.1, 0));
        primaryStage.setScene(scene);
        primaryStage.setTitle("CTRL GUI V0.1");

        Setup_UI();

        //port=Ports[0];
        //port.openPort();
        //port.setBaudRate(115200);

        root.setPadding(new Insets(10, 10, 10, 10));
        root.setHgap(8);
        root.setVgap(10);

        primaryStage.show();


        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
        System.out.println("mat = " + mat.dump());

        Lower_Body_SetXZ(0,140);

        /*cam=new VideoCapture();
        System.out.println(cam.isOpened());
        cam.open(0);

        System.out.println(cam.isOpened());
        Frame=new Mat();
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Cam Thread running");
                while (true) {
                    long tme = System.currentTimeMillis();
                    cam.read(Frame);
                    imv_1.setImage(MatToImgage(Frame));
                    int t = (int) (System.currentTimeMillis() - tme);
                    txt_fps.setText(Integer.toString(1000/t));
                }
            }
        }) .start();*/

        Frame =new Mat();
        new Thread(new Runnable() {
            @Override
            public void run() {
               // BufferedInputStream br=new BufferedInputStream(SerialInputStream);
                //InputStreamReader br=new InputStreamReader(SerialInputStream);
                while(true) {

                        if (RobotParams.port.bytesAvailable() > 0) {
                            byte b[] = new byte[RobotParams.port.bytesAvailable()];

                            //int l = port[1].readBytes(b, port[1].bytesAvailable());
                            try {
                                System.out.println((char)RobotParams.SerialInputStream.read());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println(RobotParams.port.bytesAvailable());

                        }
                }
            }
        }).start();


        final Capture_Grabber grabber=new Capture_Grabber(0,Frame,imv_1b);
        Thread GrabberThread=new Thread(grabber);
        GrabberThread.start();

        Thread.sleep(1000);

        Img_params=new ObjImgParams();
        orange=new ObjImgParams();
        red=new ObjImgParams();
        orange.minValues.set(new double[]{21,195,223});
        orange.maxValues.set(new double[]{26,220,255});

        red.minValues.set(new double[]{0,195,223});
        red.maxValues.set(new double[]{20,220,255});

        final ImgProcessor Processor2=new ImgProcessor(red,Frame,false,imv_1a,imv_2);
        Processor1=new ImgProcessor(Img_params,Frame,CameraTrack, imv_1c,imv_1);

        Thread t1=new Thread(Processor1);
        t1.start();
        Thread t2=new Thread(Processor2);
        t2.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                RobotParams.RightArm.UpdateXYZ_continuous=true;
                RobotParams.RightArm.Start_Update();

                RobotParams.LeftArm.UpdateXYZ_continuous=true;
                RobotParams.LeftArm.Start_Update();
                while (ImgProcessor.ImgProcessorThreadsRunning) {
                    double Xdiff=10,Ydiff=10,Zdiff=10;
                    if (RobotParams.UTS_val < 300)// && -ImgObjCoords[1] < RobotParams.RightArm.arm_z_offset && ImgObjCoords[0] > 100)
                    {
                        //System.out.println("Entered");
                        ////RobotParams.RightArm.Set_End_Effector_XYZ_abs(ImgObjCoords[0], -ImgObjCoords[1], -ImgObjCoords[2]);

                        double off = (Processor1.CurrImgObjCoords[0] - Processor1.ImgObjCoords[0]) / 10.0;
                        Processor1.CurrImgObjCoords[0] -= off;
                        off = (Processor1.CurrImgObjCoords[1] - Processor1.ImgObjCoords[1]) /10.0;
                        Processor1.CurrImgObjCoords[1] -= off;
                        off = (Processor1.CurrImgObjCoords[2] - Processor1.ImgObjCoords[2]) / 10.0;
                        Processor1.CurrImgObjCoords[2] -= off;

                        Xdiff=abs(Processor1.CurrImgObjCoords[0]-Processor1.ImgObjCoords[0]);
                        Ydiff=abs(Processor1.CurrImgObjCoords[1]-Processor1.ImgObjCoords[1]);
                        Zdiff=abs(Processor1.CurrImgObjCoords[2]-Processor1.ImgObjCoords[2]);

                    }
                    if(FollowObject) {
                        RobotParams.LeftArm.XYZ[0] = Processor1.CurrImgObjCoords[0];
                        RobotParams.LeftArm.XYZ[1] = -Processor1.CurrImgObjCoords[1];
                        RobotParams.LeftArm.XYZ[2] = -Processor1.CurrImgObjCoords[2];

                        //RobotParams.RightArm.XYZ[0] = Processor1.CurrImgObjCoords[0];
                        //RobotParams.RightArm.XYZ[1] = -Processor1.CurrImgObjCoords[1];
                        //RobotParams.RightArm.XYZ[2] = -Processor1.CurrImgObjCoords[2];
                        //Correction
                        if(Xdiff<1.5 &&Ydiff<1.5 &&Zdiff<1.5)
                        {
                            System.out.println("Target reached");
                            TargetReached=true;
                            FollowObject=false;
                        }
                    }
                    //RobotParams.RightArm.Set_End_Effector_XYZ_abs(Processor1.CurrImgObjCoords[0], -Processor1.CurrImgObjCoords[1], -Processor1.CurrImgObjCoords[2]);
                    //System.out.println("XYZ: "+RobotParams.RightArm.XYZ[0]+","+RobotParams.RightArm.XYZ[1]+","+RobotParams.RightArm.XYZ[2]);
                    //RobotParams.LeftArm.Set_End_Effector_XYZ_abs(Processor1.CurrImgObjCoords[0], Processor1.CurrImgObjCoords[1], -Processor1.CurrImgObjCoords[2]);
                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (ImgProcessor.ImgProcessorThreadsRunning)
                {
                    if(TargetReached)
                    {

                        //CV correction
                        if(Processor2.Obj_detectesd && Processor1.Obj_detectesd &&!CVcorrectionDone)
                        {

                            double Yoffset=(Processor1.posX-Processor2.posX)*3.0/200.0;
                            double Zoffset=(Processor2.posY-Processor1.posY)*3.0/200.0;
                            System.out.println("Offset: Y"+Yoffset+"\t Z: "+Zoffset);
                            RobotParams.LeftArm.XYZ[1]-=Yoffset;
                            RobotParams.LeftArm.XYZ[2]-=Zoffset;
                            if( RobotParams.LeftArm.XYZ[2]<RobotParams.LeftArm.arm_z_offset)
                                RobotParams.LeftArm.XYZ[2]=RobotParams.LeftArm.arm_z_offset;
                            if(abs(Yoffset)<0.4 && abs(Zoffset)<1.6)
                            {
                                CVcorrectionDone=true;
                                System.out.println("CV Correction Done");
                            }

                        }
                        //IR correction
                        if(CVcorrectionDone && !IRCorrectionDone)
                        {
                           int Sensors_Percentage[]=new int[4];
                        for(int i=0;i<4;i++)
                            Sensors_Percentage[i]= (int) (RobotParams.Sensors[i]*100/1023.0);
                        //double offsetX=0.5-(Sensors_Percentage[1]+Sensors_Percentage[2]+Sensors_Percentage[3])*2.5/300.0;
                        //double offsetY=(Sensors_Percentage[0]-Sensors_Percentage[1])*30.0/100;
                            double offsetX=0;
                            double offsetY=0;
                            double offsetZ=(Sensors_Percentage[2]-Sensors_Percentage[3])*3.0/100;
                        RobotParams.LeftArm.XYZ[0]+=offsetX;
                        //RobotParams.RightArm.XYZ[1]-=offsetY;
                        RobotParams.LeftArm.XYZ[2]-=offsetZ;
                        System.out.println("X: "+offsetX+"\tY: "+offsetY+"\tZ: "+offsetZ);
                            if(abs(offsetX)<0.2 && abs(offsetY)<0.8 && abs(offsetZ)<0.8)
                            {
                                IRCorrectionDone=true;
                                System.out.println("IR Correction Done");
                                RobotParams.LeftArm.XYZ[2]-=10;
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                RobotParams.LeftArm.Gripper(true);
                            }
                        }
                    }
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                {
                    String out = format.format(RobotParams.Sensors[0]);
                    Sn1.setText(out);
                    out = format.format(RobotParams.Sensors[1]);
                    Sn2.setText(out);
                    out = format.format(RobotParams.Sensors[2]);
                    Sn3.setText(out);
                    out = format.format(RobotParams.Sensors[3]);
                    Sn4.setText(out);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                System.out.println("EXIT");
                ImgProcessor.ExitAllThreads();
                HSV_Options_Window.closeAllWindows();
                grabber.ExitThread();
                if(RobotParams.port!=null && RobotParams.port.isOpen())
                    RobotParams.port.closePort();
                RobotParams.PortOpen=false;
                RobotParams.ServoRefresh=false;
            }
        });
        setActionListeners();
        //(new HSV_Options_Window(ImgProcessor.Frame,imv_1,Img_params)).show();

    }
    void Lower_Body_SetXZ(double X, double Z)
    {
        final double a=70;
        final double b=70;

        double h2=Z*Z+X*X;

        double BAC=acos((a*a + h2 - b*b)/(2*a*sqrt(h2)));
        double ACB= asin((a*sin(BAC))/b);
        double B=PI-ACB-BAC;

        double A1=atan(X/Z)+BAC;
        double A3;
        if(X>=0)
            A3=atan(Z/X)+ACB;
        else
            A3=PI-(-atan(Z/X)-ACB);


        T3.setText(format.format(A1*180/PI));
        T4.setText(format.format(A3*180/PI));
        T5.setText(format.format(B*180/PI));

        //RobotParams.Set_Servo_rad(PI/2-A1,2);
        //RobotParams.Set_Servo_rad(B,3);
        //RobotParams.Set_Servo_rad(A3,4);

        RobotParams.ServoAngles[2]=(PI/2-A1)*180.0/PI;
        RobotParams.ServoAngles[3]=(B)*180.0/PI;
        RobotParams.ServoAngles[4]=(A3)*180.0/PI;
    }

    void setActionListeners()
    {
        con.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String sel=Com_sel.getValue().toString();
                System.out.println(sel);
                int index=0;
                while (index<Ports.length)
                {
                    if(Ports[index].getSystemPortName().equals(sel))
                        break;
                    // else
                    ++index;
                }
                if(RobotParams.port!=null && RobotParams.port.isOpen())
                    RobotParams.port.closePort();
                RobotParams.port=Ports[index];
                RobotParams.port.openPort();
                RobotParams.port.setBaudRate(115200);
                System.out.println(RobotParams.port.getSystemPortName());
                RobotParams.SerialInputStream=RobotParams.port.getInputStream();
                RobotParams.SerialOutputStream = RobotParams.port.getOutputStream();
                RobotParams.SerialPrintStream= new PrintStream(RobotParams.SerialOutputStream);
                RobotParams.PortOpen=true;
                RobotParams.ServoRefresh=true;
                RobotParams.ServoUpdate();
                RobotParams.port_Thread();
                if(RobotParams.SerialOutputStream==null)
                    System.out.println("Output stream null");
            }
        });
        discon.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(RobotParams.port.isOpen()) {
                    RobotParams.port.closePort();
                    System.out.println("Com port Disconnected");
                    RobotParams.PortOpen=false;
                    RobotParams.ServoRefresh=false;
                }

            }
        });
        follow.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FollowObject=!FollowObject;
                TargetReached=false;
                CVcorrectionDone=false;
                IRCorrectionDone=false;
                RobotParams.LeftArm.Gripper(false);
                System.out.println("Follow object: "+FollowObject);
            }
        });
        cameraTrack.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Processor1.TrackObject=!Processor1.TrackObject;
                System.out.println("Camera track: "+Processor1.TrackObject);
            }
        });
        B1.setOnAction((new HSV_Options_Window(ImgProcessor.Frame,imv_1,Img_params)));
        B2.setOnAction(new HSV_Options_Window(ImgProcessor.Frame,imv_2,red));
        B4.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(RobotParams.SerialPrintStream==null)
                    System.out.println("Print stream null");
                new Manual_Sliders_Window().show();
            }
        });

        B5.setOnAction(new Arms_Controller_Window());

        S1.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                String s=String.valueOf(S1.getValue());
                T1.setText(format.format(S1.getValue()));
                Lower_Body_SetXZ(S1.getValue(),S2.getValue());
            }
        });
        S2.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                T2.setText(format.format(S2.getValue()));
                Lower_Body_SetXZ(S1.getValue(),S2.getValue());
            }
        });
        S3.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                T6.setText(format.format(S3.getValue()));
                RobotParams.ServoUpdateDelay=(int)S3.getValue();
            }
        });
    }
    void Setup_UI()
    {
        //root.setMinSize(200,200);
        root.setGridLinesVisible(false);
        root.gridLinesVisibleProperty();

        txt_fps = new Text();
        con = new Button();

        discon = new Button();
        follow=new Button("Track");
        cameraTrack=new Button("CamTrack");
        imv_1 = new ImageView();
        imv_1a = new ImageView();
        imv_1b = new ImageView();
        imv_1c = new ImageView();
        imv_2 = new ImageView();
        Com_sel=new ComboBox();
        S1=new Slider();
        S2=new Slider();
        S3=new Slider();
        T1=new Text();
        T2=new Text();
        T4=new Text();
        T3=new Text();
        T5=new Text();
        T6=new Text();

        Sn1=new Text();
        Sn2=new Text();
        Sn3=new Text();
        Sn4=new Text();

        S1.setMax(40); S1.setMin(-40);
        S2.setMax(140); S2.setMin(0);
        S3.setMax(1000); S2.setMin(0);
        S3.setValue(RobotParams.ServoUpdateDelay);
        S2.setValue(80);

        S1.setMinSize(200,25);
        S2.setMinSize(200,25);

        S1.setBlockIncrement(0.5);
        S2.setBlockIncrement(0.5);

        B1=new Button("HSV1");
        B2=new Button("HSV2");
        B3=new Button("HSV3");
        B4=new Button("Absolute\nManual");
        B5=new Button("Arm\nXYZ");
        B6=new Button("HSV6");
        B7=new Button("HSV7");
        B8=new Button("HSV8");

        ObservableList<String> list= FXCollections.observableArrayList();
        if(Ports.length!=0)
        {
            for(int i=0;i<Ports.length;i++)
                list.add(i,Ports[i].getSystemPortName());
            Com_sel.setItems(list);
            Com_sel.setValue(list.get(0));
        }

        txt_fps.setFill(Color.GRAY);

        txt_fps.setText("TIME");

        Com_sel.setPrefWidth(70);
        Com_sel.setPrefHeight(20);
        root.add(Com_sel,2,0);

        imv_1.setFitHeight(300);
        imv_1.setFitWidth(400);
        //root.setConstraints(iv, 0, 2);

        imv_1a.setFitHeight(90);
        imv_1a.setFitWidth(120);


        imv_1b.setFitHeight(90);
        imv_1b.setFitWidth(120);

        imv_1c.setFitHeight(90);
        imv_1c.setFitWidth(120);

        imv_2.setFitHeight(300);
        imv_2.setFitWidth(400);


        root.add(follow,3,0);
        root.add(cameraTrack,5,0);
        root.add(B1,0,4);
        root.add(B2,1,4);
        root.add(B3,2,4);
        root.add(B4,3,4);
        root.add(B5,4,4);
        //root.add(B6,5,5);

        root.add(B7,5,4);
        root.add(B8,6,4);

        root.add(S1,0,5,4,1);
        root.add(S2,0,6,4,1);
        root.add(S3,0,7,4,1);

        root.add(T1,4,5);   T1.setText("0");
        root.add(T2,4,6);   T2.setText("0");
        root.add(T3,5,5);   T3.setText("0");
        root.add(T4,5,6);   T4.setText("0");
        root.add(T5,6,6);   T5.setText("0");
        root.add(T6,7,7);   T6.setText("500");

        root.add(Sn1,7,5);   Sn1.setText("s1");
        root.add(Sn2,7,6);   Sn2.setText("s2");
        root.add(Sn3,8,5);   Sn3.setText("s3");
        root.add(Sn4,8,6);   Sn4.setText("s4");

        root.setMinSize(200,100);

        root.add(imv_1, 0, 1, 5, 3);//col, row, : col, row occupancy

        root.add(imv_1a, 5, 1, 2, 1);
        root.add(imv_1b, 5, 2, 2, 1);
        root.add(imv_1c, 5, 3, 2, 1);

        root.add(imv_2, 7, 1, 2, 3);
        root.add(txt_fps, 0, 1);

        Image image = new Image("java_.jpg");
        imv_1.setImage(image);
        imv_1a.setImage(image);
        imv_1b.setImage(image);
        imv_1c.setImage(image);
        imv_2.setImage(image);



        con.setText("Connect");
        root.add(con, 0, 0);

        //Thread.sleep(1000);

        discon.setAlignment(Pos.CENTER);
        discon.setText("Disconnect");
        root.add(discon, 1, 0);

        B1.setOnMouseReleased(new GUI_Event_Handler());
    }

    Image MatToImgage(Mat im) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", im, buffer);
        Image Im = new Image(new ByteArrayInputStream(buffer.toArray()));
        return Im;
    }

}