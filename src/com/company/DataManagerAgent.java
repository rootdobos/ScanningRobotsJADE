package com.company;
import jade.core.Agent;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;



public class DataManagerAgent extends Agent
{
    private String _EnvironmentPath;
    private Imgcodecs _ImageCodecs;
    private Mat _ScanningInformation;
    protected void setup() {
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        System.out.println(getLocalName()+" STARTED");
        Object[] args= getArguments();
        if (args != null && args.length > 0) {
            _EnvironmentPath=(String) args[0];
        }
        _ImageCodecs= new Imgcodecs();
        Mat environment= _ImageCodecs.imread(_EnvironmentPath,Imgcodecs.IMREAD_GRAYSCALE);
        System.out.println("Environment size: " +environment.size().height +":"+environment.size().width+ ":" +environment.channels());
        _ScanningInformation= new Mat(environment.size(), CvType.CV_8UC1);
        _ScanningInformation.setTo(new Scalar(255));
        System.out.println("Sinf size: " +_ScanningInformation.size().height +":"+_ScanningInformation.size().width+ ":" +_ScanningInformation.channels());
        //HighGui.imshow("Sinf",_ScanningInformation);
        //HighGui.waitKey();
    }
}
