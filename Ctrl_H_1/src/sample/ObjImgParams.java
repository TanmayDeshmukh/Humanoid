package sample;

import org.opencv.core.Scalar;
import org.opencv.core.Size;

/**
 * Created by TANMAY on 10-01-2016.
 */
public class ObjImgParams {

    Scalar minValues = new Scalar(96, 130, 130);//Default blue obj
    Scalar maxValues = new Scalar(120, 550, 255);

    Size dilateElementSize=new Size(24, 24);
    Size erodeElementSize= new Size(12,12);

}

