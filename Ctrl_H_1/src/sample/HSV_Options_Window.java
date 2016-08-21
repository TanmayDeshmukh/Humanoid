package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.opencv.core.Mat;

/**
 * Created by TANMAY on 10-01-2016.
 */
public class HSV_Options_Window implements EventHandler{ //calls handle() on button action
    //objs of this, called by objs of ImgProcessor
    static Stage window;
    ImageView op_iv;
    Mat Img;
    ObjImgParams params;
    Slider sliders[];
    Label labels[];
    Text txt[];

    final String Labels[]={"Hue_Min","Hue_Max","Sat_Min","Sat_Max","Val_Min","Val_Max"};
    volatile double Vals[]=new double[6];

    @Override
    public void handle(Event event) {
        show();
    }

    HSV_Options_Window(Mat input_img,ImageView op_iv,ObjImgParams params)
    {
        this.op_iv=op_iv;
        Img=input_img;
        this.params=params;
        sliders=new Slider[6];
        labels=new Label[6];
        txt=new Text[6];


        this.Vals[0]=params.minValues.val[0];
        this.Vals[1]=params.maxValues.val[0];

        this.Vals[2]=params.minValues.val[1];
        this.Vals[3]=params.maxValues.val[1];

        this.Vals[4]=params.minValues.val[2];
        this.Vals[5]=params.maxValues.val[2];

    }

    void show() {

        GridPane layout= new GridPane();
        Scene scene =new Scene(layout);
        window =new Stage();
        window.setTitle("Adjust HSV range");
        window.setScene(scene);

        layout.setVgap(8);
        layout.setHgap(10);
        layout.setPadding(new Insets(10,10,10,10));
        layout.setMinSize(320,240);



        for(int i=0;i<6;i++)
        {
            sliders[i]=new Slider();
            labels[i]=new Label();
            txt[i]=new Text();
            sliders[i].setMax(255);
            if (i<2)
                sliders[i].setMax(179);
            sliders[i].setMinorTickCount(5);
            sliders[i].setMajorTickUnit(10);
            sliders[i].setShowTickMarks(true);
            sliders[i].setBlockIncrement(5);
            sliders[i].setSnapToTicks(true);
            sliders[i].setMinSize(400,50);



            final int finalI = i;

            /*sliders[i].valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                    int value=(int)sliders[finalI].getValue();
                    txt[finalI].setText(String.valueOf(value));
                    //double v[]={value,params.minValues.val[1],params.minValues.val[2]};
                    //params.minValues.set(v);
                    Vals[finalI]=value;

                    System.out.println(Vals[finalI]);
                }
            });*/

            labels[i].setText(Labels[i]);
            txt[i].setText("0");

            layout.add(sliders[i],0,i);
            layout.add(labels[i],1,i);
            layout.add(txt[i],2,i);

            sliders[i].setValue(Vals[i]);
            txt[i].setText(String.valueOf(Vals[i]));
        }

        sliders[0].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                int value=(int)sliders[0].getValue();
                txt[0].setText(String.valueOf(value));
                params.minValues.val[0]=value;
                System.out.println(params.minValues.val[0]);
            }
        });
        sliders[1].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                int value=(int)sliders[1].getValue();
                txt[1].setText(String.valueOf(value));
                params.maxValues.val[0]=value;
            }
        });

        sliders[2].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                int value=(int)sliders[2].getValue();
                txt[2].setText(String.valueOf(value));
                params.minValues.val[1]=value;
            }
        });
        sliders[3].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                int value=(int)sliders[3].getValue();
                txt[3].setText(String.valueOf(value));
                params.maxValues.val[1]=value;
            }
        });

        sliders[4].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                int value=(int)sliders[4].getValue();
                txt[4].setText(String.valueOf(value));
                params.minValues.val[2]=value;
            }
        });
        sliders[5].valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                int value=(int)sliders[5].getValue();
                txt[5].setText(String.valueOf(value));
                params.maxValues.val[2]=value;
            }
        });


        window.show();


    }
    static void closeAllWindows()
    {
        if(window!=null)
        window.close();
    }
}
