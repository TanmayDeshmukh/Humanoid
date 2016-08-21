package sample;

import com.fazecast.jSerialComm.SerialPort;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static java.lang.Double.NaN;
import static java.lang.Double.isInfinite;
import static java.lang.Math.*;

/**
 * Created by TANMAY on 25-01-2016.
 */
public class RobotParams {
    static SerialPort port;
    static boolean PortOpen,ServoRefresh;
    static InputStream SerialInputStream;
    static OutputStream SerialOutputStream;
    static PrintStream SerialPrintStream;
    static Arm RightArm,LeftArm;
    static int ServoUpdateDelay=100;

    volatile static double ServoAngles[]=new double[17];
    static boolean InvertAngles[]={false,false,        //head
                                    false,false,false, //lower body

                                    true, false, false,//right arm
                                    false,false,false,

                                    false,true,true, //left arm
                                    true,true,true};

    static double UTS_val=500;//ultrasonic sensor
    static int Sensors[]=new int[4];
    static final int num_Sensor_vals=2;
    static int Sensors_avg[][]=new int[4][num_Sensor_vals];
    static double max_UTS_diff=300;
    static final int num_UTS_vals=5;
    static double UTS_vals[]=new double[num_UTS_vals];

    public class Shoulder
    {
        double Pitch,Roll,Yaw;
        FV FrontView;
        TV TopView;
        final int ID1,ID2,ID3;
        Shoulder(int ID1,int ID2,int ID3)
        {
            FrontView=new FV();
            TopView=new TV();
            this.ID1=ID1;
            this.ID2=ID2;
            this.ID3=ID3;
        }
        public class FV
        {
            final double m,l,n,k,EAD,ADE,CDB,BD2,AD2;
            double h;
            double BDI;
            double DBA;
            FV() {
                m = 28.5;
                l = 27.0;
                n = 28.36;
                k = 15.09;

                EAD = atan(l / m);
                ADE = atan(m / l);
                CDB = atan(n / k);

                BD2 = n * n + k * k;
                AD2 = m * m + l * l;
            }
        }
        public class TV
        {
            final double m , n , l , r ;
            double h;
            TV() {
                m = 12.25;
                n = 15;
                l = 45;
                r = 21;
            }
        }
        void SetShoulder_deg(double Pitch,double Roll,double Yaw)
        {
            System.out.println("Piitch:"+Pitch+"\tRoll:"+Roll+"\tYaw:"+Yaw);
            SetShoulder_rad( Pitch*PI/180, Roll*PI/180,Yaw*PI/180);
        }
        void SetShoulder_rad(double Pitch,double Roll,double Yaw)
        {
            //Roll operation:
            //System.out.println("Piitch:"+Pitch+"\tRoll:"+Roll+"\tYaw:"+Yaw);
            FrontView.BDI=atan(FrontView.n/FrontView.k)+Roll;
            double ADB=PI-FrontView.ADE-FrontView.BDI;
            FrontView.h=sqrt(FrontView.AD2+FrontView.BD2-2*sqrt(FrontView.AD2*FrontView.BD2)*cos(ADB));
            FrontView.DBA=acos((pow(FrontView.h,2)+FrontView.AD2-FrontView.BD2)/(2*FrontView.h*sqrt(FrontView.AD2)));
            double EAB=FrontView.DBA+atan(FrontView.l/FrontView.m);
            TopView.h=FrontView.h*cos(PI/2-EAB);
            /*System.out.println();
            System.out.println("Hfv="+FrontView.h);
            System.out.println("Htv="+TopView.h);
            System.out.println("EAB="+EAB*180/PI);
            System.out.println("ADB="+ADB*180/PI);
            System.out.println();*/
            //Yaw operation
            double Yawop=(YawOperation(Yaw)+PI/2)*180/PI;
            if(Yawop!=NaN)
            ServoAngles[ID1]=Yawop;
            //Set_Servo_rad(YawOperation(-Yaw)+PI/2,ID2);
            Yawop=(YawOperation(-Yaw)+PI/2)*180/PI;
            if(Yawop!=NaN)
            ServoAngles[ID2]=Yawop;

            double PitchOut=(Pitch*2+PI)/3;
            //Set_Servo_rad(PitchOut,ID3);
            ServoAngles[ID3]=(PitchOut)*180/PI;
        }
        double YawOperation(double angle)
        {
            double GH2=pow(TopView.h-TopView.n*sin(angle),2)+pow(TopView.m-TopView.n*cos(angle),2);

            double HIG=acos((pow(TopView.r,2)+pow(TopView.l,2)-GH2)/(2*TopView.r*TopView.l));

            double IHG=PI-asin((TopView.r*sin(HIG))/sqrt(GH2))-HIG;

            double gj=TopView.h-TopView.n*sin(angle);
            double hj=abs(TopView.m-TopView.n*cos(angle));

            double GHJ=atan(gj/hj);
            /*System.out.println("GH2="+GH2*180/PI);
            System.out.println("HIG="+HIG*180/PI);
            System.out.println("IHG="+IHG*180/PI);
            System.out.println("GHJ="+GHJ*180/PI);
            System.out.println();*/
            return (IHG-PI+GHJ);
        }
    }
    public class Arm
    {
        Shoulder shoulder;
        int ID[]=new int[6];
        final double x,d;
        final double arm_x_offset;
        final double arm_y_offset;
        final double arm_z_offset;
        double XYZ[]=new double[3];
        boolean UpdateXYZ_continuous;
        public Arm(int ID[]) {
            x=79;
            this.ID=ID;
            d=138;
            arm_x_offset=9.62;
            arm_y_offset=93.98;
            arm_z_offset=74.5;
            XYZ=new double[]{100,arm_y_offset,arm_z_offset+d};
            shoulder=new Shoulder(ID[0],ID[1],ID[2]);//IDs S1,S2,Pitch
        }

        //elbow , gripper
        void Start_Update()
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(UpdateXYZ_continuous)
                    {
                        Set_End_Effector_XYZ_abs(XYZ);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        void Set_End_Effector_XYZ_abs(double[] vals)
        {
            Set_End_Effector_XYZ_abs(vals[0],vals[1],vals[2]);
        }
        void Set_End_Effector_XYZ_abs(double X,double Y,double Z)
        {
            Set_End_Effector_XYZ_relative(X+arm_x_offset,arm_y_offset-Y,Z-arm_z_offset);
        }
        void Set_End_Effector_XYZ_relative(double X,double Y,double Z)
        {

            if(sqrt(X*X+Y*Y+Z*Z)<=x+d && Y<=d) {
                double ds = sqrt(d * d - Y * Y);
                double y = sqrt(X * X + Z * Z);
                double phi = acos((x * x + y * y - ds * ds) / (2 * x * y));
                double Shoulder_Pitch = asin(X / y) - phi;
                double Elbow = PI - asin(x * sin(phi) / ds) - phi;
                double X_ = x * sin(Shoulder_Pitch);
                double Shoulder_Yaw = atan(Y / (X - X_));

                shoulder.SetShoulder_rad(Shoulder_Pitch, 0, Shoulder_Yaw);
                //Set_Servo_rad(Elbow, ID[3]);
                if (Elbow!=NaN)
                ServoAngles[ID[3]]=Elbow*180/PI;
            }

        }
        void Gripper(boolean grab)
        {
            ServoAngles[ID[5]]=(grab)?60:120;
        }
    }
    void avg_out()
    {
        //for(int i=0;i<10;i++)
    }
    RobotParams()
    {
        RightArm=new Arm(new int[]{5,6,7,8,9,10});
        LeftArm=new Arm(new int[]{11,12,13,14,15,16});
        ServoAngles= new double[]{90,90,90, 90,90,  90,90,60,180,90,120,   90,90,60,180,90,120};
    }
    synchronized static void Set_Servo_raw(double angle, int servo_ID)
    {
        //if(servo_ID==0)
        //    System.out.println("A: "+angle);
        if (angle>180)angle=180;
        else if (angle<0)angle=0;
        //ServoAngles[servo_ID]=angle;
        if(angle!=NaN) {
            try {

                NumberFormat format = new DecimalFormat("#000.0");

                if (InvertAngles[servo_ID])
                    angle = 180 - angle;
                servo_ID++;
                if (servo_ID < 1) servo_ID = 1;
                else if (servo_ID > 17) servo_ID = 17;
                String out = format.format(angle);
                //System.out.println();
                //System.out.println(out);
                out = out.substring(0, 3) + out.substring(4, 5);
                //System.out.println(out);
                if (servo_ID < 10)
                    out += "0";
                out += String.valueOf(servo_ID);
                //System.out.println(out);

                SerialPrintStream.println(out);
                //System.out.println("Sending: "+out);
            } catch (NullPointerException e) {
                System.out.println("Null pointer: OutputStream");

            } catch (StringIndexOutOfBoundsException SE) {
                //System.out.println("Angle undefined");
            }
        }

    }
    synchronized static void Set_Servo_rad(double angle, int servo_ID)
    {
        angle=angle*180/PI;
        Set_Servo_raw(angle, servo_ID);
    }
    static void port_Thread()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Port thread running");
                BufferedReader br=new BufferedReader(new InputStreamReader(SerialInputStream));
                String input= "";

                while(PortOpen)
                {
                    try {

                        input=br.readLine();
                    //if(port.bytesAvailable()>0)
                    if(input!=null)

                    {
                        //System.out.println("Bytes avail: "+port.bytesAvailable());

                        try {
                            byte b[]=new byte[50];
                           // SerialInputStream.read(b);
                            //input=new String(b);


                            //System.out.print(input);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        processInput(input);
                    }
                    /*try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                  }
                    catch (Exception e){}
                }
            }
        }).start();
    }
    static void ServoUpdate()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(ServoRefresh && PortOpen)
                {
                    //SerialPrintStream.println("RD");
                    for(int i=0;i<17;i++)
                        Set_Servo_raw( ServoAngles[i],i);
                    try {
                        Thread.sleep(ServoUpdateDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    static void processInput(String s)
    {
        try {
            //System.out.println("Processing: "+s);
            //String val = s.substring(2,s.length()-2);
            String val = s.substring(2);
            switch (s.charAt(0)) {
                case 'U':
                    double v=Double.parseDouble(val);

                    if(v<500 &&v>50) {
                        UTS_val = v;
                        //UTS_vals[num_UTS_vals - 1] = v;
                       // System.out.println();
                        //for (int i = 0; i < num_UTS_vals - 1; i++) {
                            //System.out.print("\t" + i + ": " + UTS_vals[i]);
                            //UTS_val += UTS_vals[i];
                            //UTS_vals[i] = UTS_vals[i + 1];

                        //}
                        //UTS_val /= (num_UTS_vals);
                        //if(abs(v-UTS_val)<max_UTS_diff)

                        //System.out.println("Set: " + UTS_vals[0]);
                    }
                    break;
                case 'S':
                    //System.out.println("s :"+s);
                    //String ID=s.substring(1,2);
                    int IDi=(int)s.charAt(1)-(int)'0'-1;
                    val = s.substring(2);
                    //System.out.println(IDi+" val: "+val);
                    Sensors[IDi]=Integer.parseInt(val);

                    /*for(int i=0;i<num_Sensor_vals-1;i++)
                        Sensors_avg[IDi][i]=Sensors_avg[IDi][i+1];
                    Sensors_avg[IDi][num_Sensor_vals-1]=Sensors[IDi];

                    long tot=0;
                    for(int i=0;i<num_Sensor_vals;i++)
                        tot+=Sensors_avg[IDi][i];
                    Sensors[IDi]= (int) (tot/num_Sensor_vals);*/
                    //if(IDi==3)
                     //   Sensors[IDi-1]= (int) ((Sensors[IDi-1]/800.0)*500.0);
                   // System.out.println("S"+IDi+": "+Sensors[IDi-1]);
            }
        }
        catch (NumberFormatException ne)
        {
            System.out.println("number format exception");
        }
    }
}
