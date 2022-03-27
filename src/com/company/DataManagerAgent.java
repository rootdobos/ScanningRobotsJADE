package com.company;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.*;

import org.opencv.core.Point;

public class DataManagerAgent extends Agent
{
    private String _EnvironmentPath;
    private Imgcodecs _ImageCodecs;
    private Mat _ScanningInformation;
    private List<AID>  _Scanners= new ArrayList<AID>();
    private HashMap<AID,Point> _ScannerPositions= new HashMap<AID,Point>();
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
        _ScanningInformation= new Mat(environment.size(), CvType.CV_8UC3);
        _ScanningInformation.setTo(new Scalar(255));
        //HighGui.imshow("Sinf",_ScanningInformation);
        //HighGui.waitKey();
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName("ImageScanning");
            sd.setType("image-scanning");
            sd.addOntologies("image-scanning-ontology");
            sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
            sd.addProperties(new Property("agent", "manager"));
            dfd.addServices(sd);
            DFService.register(this, dfd);
        }
        catch (FIPAException fe){
            fe.printStackTrace();
        }
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // listen if a greetings message arrives
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) {
                    String msgContent= msg.getContent();
                    String[] msgContentParts= msgContent.split("@");
                    if (msgContentParts[0].equalsIgnoreCase("new_scanner_checkin")) {
                        // if a greetings message is arrived then send an ANSWER
                        System.out.println(myAgent.getLocalName()+" RECEIVED CHECKIN MESSAGE FROM "+msg.getSender().getLocalName());
                        _Scanners.add(msg.getSender());
                        ACLMessage reply = msg.createReply();
                        String message= "environment_path@"+_EnvironmentPath;
                        reply.setContent(message);
                        myAgent.send(reply);
                        System.out.println(myAgent.getLocalName()+" SENT ENVIRONMENT PATH TO "+msg.getSender().getLocalName());
                    }
                    else if(msgContentParts[0].equalsIgnoreCase("my_position") )
                    {
                        Point scannerPosition= new Point(Double.parseDouble( msgContentParts[1]),Double.parseDouble(msgContentParts[2]));
                        _ScannerPositions.put(msg.getSender(),scannerPosition);
//                        if(_ScannerPositions.containsKey(msg.getSender()))
//                        {
//                            _ScannerPositions[msg.getSender()]=
//                        }
                    }
                    else {
                        System.out.println(myAgent.getLocalName()+" Unexpected message received from "+msg.getSender().getLocalName());
                    }
                }
                else {
                    // if no message is arrived, block the behaviour
                    block();
                }
            }
        });
    }
}
