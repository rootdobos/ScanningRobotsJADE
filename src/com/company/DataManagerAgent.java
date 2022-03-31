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
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class DataManagerAgent extends Agent
{
    private String _EnvironmentPath;
    private Imgcodecs _ImageCodecs;
    private Mat _ScanningInformation;
    private List<AID>  _Scanners= new ArrayList<AID>();
    private ArrayList<Point> _EdgePoints= new ArrayList<>() ;

    private HashMap<AID,Point> _ScannerPositions= new HashMap<AID,Point>();
    protected void setup() {
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        System.out.println(getLocalName()+" STARTED");
        Object[] arguments= getArguments();
        int numberOfScanners=0;
        if (arguments != null && arguments.length > 0) {
            String[] args= ((String)arguments[0]).split("%");
            _EnvironmentPath=args[0];
            numberOfScanners=Integer.parseInt(args[1]);
        }
        _ImageCodecs= new Imgcodecs();
        Mat environment= _ImageCodecs.imread(_EnvironmentPath,Imgcodecs.IMREAD_GRAYSCALE);
        System.out.println("Environment size: " +environment.size().height +":"+environment.size().width+ ":" +environment.channels());
        _ScanningInformation= new Mat(environment.size(), CvType.CV_8UC3);
        _ScanningInformation.setTo(new Scalar(255,255,255));
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
                    else if(msgContentParts[0].equalsIgnoreCase("my_starter_position") )
                    {
                        Point scannerPosition= new Point(Double.parseDouble( msgContentParts[1]),Double.parseDouble(msgContentParts[2]));
                        _ScannerPositions.put(msg.getSender(),scannerPosition);
                    }
                    else if(msgContentParts[0].equalsIgnoreCase("my_position") )
                    {
                        Point position=_ScannerPositions.get(msg.getSender());
                        double[] gray= new double[]{150,150,150};
                        //_ScanningInformation.put((int)position.x,(int)position.y,gray);
                        _ScanningInformation.put((int)position.y,(int)position.x,gray);
                        Point scannerPosition= new Point(Double.parseDouble( msgContentParts[1]),Double.parseDouble(msgContentParts[2]));
                        _ScannerPositions.put(msg.getSender(),scannerPosition);
                        DrawAgentPositions();
                    }
                    else if(msgContentParts[0].equalsIgnoreCase("edge_position") )
                    {
                        Point newEdge= new Point(Double.parseDouble(msgContentParts[1]),Double.parseDouble(msgContentParts[2]));
                        if(!Utilities.ContainsCoordinates(_EdgePoints,newEdge)) {
                            _EdgePoints.add(newEdge);
                            double[] red = new double[]{0, 0, 254};
                            //_ScanningInformation.put(Integer.parseInt(msgContentParts[1]),Integer.parseInt(msgContentParts[2]),red);
                            _ScanningInformation.put(Integer.parseInt(msgContentParts[2]), Integer.parseInt(msgContentParts[1]), red);
                            for (AID scanner : _Scanners) {
                                ACLMessage egdeMsg = new ACLMessage(ACLMessage.INFORM);

                                egdeMsg.setContent("new_edge_found@" + Integer.parseInt(msgContentParts[1]) + "@" + Integer.parseInt(msgContentParts[2]));
                                egdeMsg.addReceiver(scanner);
                                send(egdeMsg);
                            }
                        }
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
        try {
            for(int i=0;i<numberOfScanners;i++) {
                AgentContainer container = (AgentContainer) getContainerController(); // get a container controller for creating new agents
                String nickName = "Scanner"+i;
                AgentController t1 = container.createNewAgent(nickName, "com.company.ScanningAgent", null);
                t1.start();
            }
        } catch (Exception any) {
            any.printStackTrace();
        }
    }
    private int guiCounter=0;
    private void RefreshGUI()
    {
        guiCounter++;
        if(guiCounter==100)
        {
            Mat resizeimage = new Mat();
            Size sz = new Size(_ScanningInformation.width() * 5, _ScanningInformation.height() * 5);
            Imgproc.resize(_ScanningInformation, resizeimage, sz);
            HighGui.imshow("ScannedData", resizeimage);
            // HighGui.imshow("ScannedData",_ScanningInformation);
            HighGui.waitKey(1);
            guiCounter=0;
        }
    }
    private void DrawAgentPositions()
    {
        for (Map.Entry<AID, Point> entry: _ScannerPositions.entrySet())
        {
            double[] green= new double[]{0,255,0};
           //_ScanningInformation.put((int)entry.getValue().x,(int)entry.getValue().y,green);
            _ScanningInformation.put((int)entry.getValue().y,(int)entry.getValue().x,green);
        }
        RefreshGUI();
    }
}
