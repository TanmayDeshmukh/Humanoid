package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by TANMAY on 18-01-2016.
 */
public class Manual_Sliders_Window implements EventHandler {
    static Stage window;
    static Slider UTS;
    static Text UTS_val;
    static Slider sliders[];
    static Label labels[];
    static Text txt[];
    boolean WindowOpen,BlockListeners;

    static double ServoVals[]=new double[17];

    static final String S_Labels[]={"Pan","TLT",//head
            "RS1","RS2","RPT","REL","RGW","RGp",//right arm
            "LS1","LS2","LPT","LEL","LGW","LGp",//left
            "Hip","KNE","AKL"};//lower body
    final NumberFormat format = new DecimalFormat("#000.0");

    GridPane layout;
    Scene scene;
    Manual_Sliders_Window()
    {
        sliders=new Slider[17];
        labels=new Label[17];
        txt=new Text[17];
        WindowOpen=true;
    }
    @Override
    public void handle(Event event)
    {
        if(RobotParams.SerialPrintStream==null)
            System.out.println("Print stream null");
        show();
    }
    void show()
    {
        Button refresh=new Button("Refresh");
        UTS= new Slider();
        UTS_val=new Text();
        UTS.setMinSize(200,25);
        //UTS.setDisable(true);
        UTS.setMax(500);

        layout=new GridPane();
        scene=new Scene(layout);
        window=new Stage();
        window.setTitle("Absolute Manual Control");
        window.setScene(scene);

        //layout.setGridLinesVisible(true);

        layout.setVgap(8);
        layout.setHgap(10);
        layout.setPadding(new Insets(10,10,10,10));
        //layout.setMinSize(600,200);

        for(int i=0;i<17;i++) {
            sliders[i] = new Slider();
            labels[i] = new Label();
            txt[i] = new Text();

            if(i==4 || i==10)   //Right/left arm pitch
                sliders[i].setMax(270);
            else
                sliders[i].setMax(180);

            if(i==5 || i==11)
                sliders[i].setMin(45);

            sliders[i].setMinorTickCount(5);
            sliders[i].setMajorTickUnit(10);
            sliders[i].setShowTickMarks(true);
            sliders[i].setBlockIncrement(0.5);
            sliders[i].setSnapToTicks(true);
            sliders[i].setMinSize(200,25);

            labels[i].setText(S_Labels[i]);
            txt[i].setText("0");


        }

        layout.add(refresh,1,0);layout.add(UTS,2,0,2,1);layout.add(UTS_val,4,0);
        refresh.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                RefreshSliders();
            }
        });

        layout.add(new Label("Head"),0,0);
        layout.add(new Label("Arms"),0,3);
        layout.add(new Label("Lower Body"),0,10);

        layout.add(sliders[0],0,1);
        layout.add(labels[0],1,1);
        layout.add(txt[0],2,1);
        sliders[0].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if(!BlockListeners) {
                    //RobotParams.Set_Servo_raw(sliders[0].getValue(), 0);
                    RobotParams.ServoAngles[0]=sliders[0].getValue();
                    String out = format.format(sliders[0].getValue());
                    txt[0].setText(out);
                }
            }
        });

        layout.add(sliders[1],0,2);
        layout.add(labels[1],1,2);
        layout.add(txt[1],2,2);
        sliders[1].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if(!BlockListeners) {
                    //RobotParams.Set_Servo_raw(sliders[1].getValue(), 1);
                    RobotParams.ServoAngles[1]=sliders[1].getValue();
                    String out = format.format(sliders[1].getValue());
                    txt[1].setText(out);
                }
            }
        });
        //Right arm

        for(int i=2;i<8;i++) {
            layout.add(sliders[i],0,i+2);
            layout.add(labels[i],1,i+2);
            layout.add(txt[i],2,i+2);
            final  int finalI=i;
            sliders[finalI].valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                    if(!BlockListeners) {
                        //RobotParams.Set_Servo_raw(sliders[finalI].getValue(), finalI + 3);
                        RobotParams.ServoAngles[finalI+3]=sliders[finalI].getValue();
                        String out = format.format(sliders[finalI].getValue());
                        txt[finalI].setText(out);
                    }
                }
            });
        }
        //Left Arm
        for(int i=8;i<14;i++) {
            layout.add(sliders[i],3,i+2-6,4,1);
            layout.add(labels[i],7,i+2-6);
            layout.add(txt[i],8,i+2-6);
            final  int finalI=i;
            sliders[finalI].valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                    if(!BlockListeners) {
                        //RobotParams.Set_Servo_raw(sliders[finalI].getValue(), finalI + 3);
                        RobotParams.ServoAngles[finalI+3]=sliders[finalI].getValue();
                        String out = format.format(sliders[finalI].getValue());
                        txt[finalI].setText(out);
                    }
                }
            });
        }
        //Lower body
        for(int i=14;i<17;i++) {
            layout.add(sliders[i], 0, i + 2-5);
            layout.add(labels[i], 1, i + 2-5);
            layout.add(txt[i], 2, i + 2-5);
            final  int finalI=i;
            sliders[finalI].valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                    if(!BlockListeners) {
                        //RobotParams.Set_Servo_raw(sliders[finalI].getValue(), finalI - 12);
                        RobotParams.ServoAngles[finalI-12]=sliders[finalI].getValue();
                        String out = format.format(sliders[finalI].getValue());
                        txt[finalI].setText(out);
                    }
                }
            });
        }
        window.show();
        txt[2].setText("180.0");
        window.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                WindowOpen=false;
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(WindowOpen)
                {
                    RefreshSliders();
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    void RefreshSliders()
    {
        BlockListeners=true;
        sliders[0].setValue(RobotParams.ServoAngles[0]);
        String out = format.format(sliders[0].getValue());
        txt[0].setText(out);
        sliders[1].setValue(RobotParams.ServoAngles[1]);
        out = format.format(sliders[1].getValue());
        txt[1].setText(out);
        for(int i=2;i<8;i++)
        {
            sliders[i].setValue(RobotParams.ServoAngles[i+3]);
            out = format.format(sliders[i].getValue());
            txt[i].setText(out);
        }
        for(int i=8;i<14;i++)
        {
            sliders[i].setValue(RobotParams.ServoAngles[i+3]);
            out = format.format(sliders[i].getValue());
            txt[i].setText(out);
        }
        for(int i=14;i<17;i++)
        {
            sliders[i].setValue(RobotParams.ServoAngles[i-12]);
            out = format.format(sliders[i].getValue());
            txt[i].setText(out);
        }
        UTS.setValue(RobotParams.UTS_val);
        UTS_val.setText(String.valueOf((int)RobotParams.UTS_val));
        BlockListeners=false;
    }
}
