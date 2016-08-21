package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by TANMAY on 27-01-2016.
 */
public class Arms_Controller_Window implements EventHandler{
    static Stage window;
    static GridPane layout;
    static Scene scene;

    static Slider sliders[];
    static Label labels[];
    static Text txt[];
    static final String S_Labels[]={"R_X","R_Y","R_Z","L_X","L_Y","L_Z"};
    static boolean BlockListeners,windowOpen;

    final NumberFormat format = new DecimalFormat("#000.0");

    Arms_Controller_Window()
    {
        sliders=new Slider[6];
        labels=new Label[6];
        txt=new Text[6];
        windowOpen=true;
    }
    void show() {
        layout = new GridPane();
        scene = new Scene(layout);
        window = new Stage();
        window.setTitle("Arm coordinates");
        window.setScene(scene);

        //layout.setGridLinesVisible(true);

        layout.setVgap(8);
        layout.setHgap(10);
        layout.setPadding(new Insets(10, 10, 10, 10));
        layout.setMinSize(550, 170);

        for(int i=0;i<6;i++) {
            sliders[i] = new Slider();
            labels[i] = new Label();
            txt[i] = new Text();

            sliders[i].setMinorTickCount(5);
            sliders[i].setMajorTickUnit(10);
            sliders[i].setShowTickMarks(true);
            sliders[i].setBlockIncrement(0.5);
            sliders[i].setSnapToTicks(true);
            sliders[i].setMinSize(200,25);

            labels[i].setText(S_Labels[i]);
            txt[i].setText("000.0");
        }
        sliders[0].setMin(-100);sliders[0].setMax(200);
        sliders[1].setMin(-100);sliders[1].setMax(200+RobotParams.RightArm.arm_y_offset);
        sliders[2].setMin(-100);sliders[2].setMax(RobotParams.RightArm.d+RobotParams.RightArm.x+RobotParams.RightArm.arm_z_offset);

        sliders[3].setMin(-100);sliders[3].setMax(200);
        sliders[4].setMin(-100);sliders[4].setMax(200+RobotParams.RightArm.arm_y_offset);
        sliders[5].setMin(-100);sliders[5].setMax(RobotParams.RightArm.d+RobotParams.RightArm.x+RobotParams.RightArm.arm_z_offset);

        layout.add(new Label("Relative(to shoulder) XYZ"),0,0);

        final NumberFormat format = new DecimalFormat("#000.0");
        ChangeListener action_right=new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object o, Object t1) {
                if(!BlockListeners) {
                    //RobotParams.RightArm.Set_End_Effector_XYZ_abs(sliders[0].getValue(), sliders[1].getValue(), sliders[2].getValue());
                    RobotParams.RightArm.XYZ[0]=sliders[0].getValue();
                    RobotParams.RightArm.XYZ[1]=sliders[1].getValue();
                    RobotParams.RightArm.XYZ[2]=sliders[2].getValue();

                    String out = format.format(sliders[0].getValue());
                    txt[0].setText(out);
                    out = format.format(sliders[1].getValue());
                    txt[1].setText(out);
                    out = format.format(sliders[2].getValue());
                    txt[2].setText(out);
                }
            }
        };
        ChangeListener action_left=new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object o, Object t1) {
                if(!BlockListeners) {
                    //RobotParams.LeftArm.Set_End_Effector_XYZ_abs(sliders[3].getValue(), sliders[4].getValue(), sliders[5].getValue());
                    //RobotParams.LeftArm.shoulder.SetShoulder_deg(sliders[3].getValue(),sliders[4].getValue(),sliders[5].getValue());
                    RobotParams.LeftArm.XYZ[0]=sliders[3].getValue();
                    RobotParams.LeftArm.XYZ[1]=sliders[4].getValue();
                    RobotParams.LeftArm.XYZ[2]=sliders[5].getValue();

                    String out = format.format(sliders[3].getValue());
                    txt[3].setText(out);
                    out = format.format(sliders[4].getValue());
                    txt[4].setText(out);
                    out = format.format(sliders[5].getValue());
                    txt[5].setText(out);
                }
            }
        };
        for(int i=0;i<3;i++) {
            layout.add(sliders[i], 0, i + 1);
            layout.add(txt[i],1,i+1);
            sliders[i].valueProperty().addListener(action_right);

            layout.add(sliders[i + 3], 2, i + 1);
            layout.add(txt[i+3],3,i+1);
            sliders[i + 3].valueProperty().addListener(action_left);
        }
        window.show();
        window.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                windowOpen=false;
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (windowOpen) {
                    RefreshSliders();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }
    @Override
    public void handle(Event event) {
        show();
    }
    void RefreshSliders()
    {
        BlockListeners=true;
        for(int i=0;i<3;i++) {
            sliders[i].setValue(RobotParams.RightArm.XYZ[i]);
            String out = format.format(sliders[i].getValue());
            txt[i].setText(out);

            sliders[i+3].setValue(RobotParams.LeftArm.XYZ[i]);
            out = format.format(sliders[i+3].getValue());
            txt[i+3].setText(out);
        }
        BlockListeners=false;


    }
}
