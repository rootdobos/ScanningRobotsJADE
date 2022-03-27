package com.company;
import jade.core.Agent;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;
import jade.util.leap.Iterator;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Point;

import java.util.Random;

public class ScanningAgent extends Agent
{
    private AID _DataManager;
    private Point _Position;
    private Imgcodecs _ImageCodecs;
    private Mat _Environment;
protected void setup(){
    System.out.println("Agent: "+getLocalName()+" searching for manager");
    try{
        DFAgentDescription template= new DFAgentDescription();
        ServiceDescription templateSd= new ServiceDescription();
        templateSd.setType("image-scanning");
        template.addServices(templateSd);

        SearchConstraints sc = new SearchConstraints();
        DFAgentDescription[] results = DFService.search(this, template);
        if (results.length > 0) {
            System.out.println("Agent "+getLocalName()+" found the manager:");
            for (int i = 0; i < results.length; ++i) {
                DFAgentDescription dfd = results[i];
                AID provider = dfd.getName();
                _DataManager= provider;
                // The same agent may provide several services; we are only interested
                // in the weather-forcast one
//                Iterator it = dfd.getAllServices();
//                while (it.hasNext()) {
//                    ServiceDescription sd = (ServiceDescription) it.next();
//                    if (sd.getType().equals("weather-forecast")) {
//                        System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getName());
//                    }
//                }
            }
            ACLMessage msg= new ACLMessage(ACLMessage.INFORM);

            msg.setContent("new_scanner_checkin");
            msg.addReceiver(_DataManager);
            send(msg);
            System.out.println(getLocalName()+" SENT CHECKIN MESSAGE  TO "+_DataManager.getName());
        }
        else {
            System.out.println("Agent "+getLocalName()+" did not find the Manager");
        }
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // listen if a greetings message arrives
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) {
                    String msgContent= msg.getContent();
                    String[] msgContentParts= msgContent.split("@");
                    if (msgContentParts[0].equalsIgnoreCase("environment_path")) {
                        // if a greetings message is arrived then send an ANSWER
                        System.out.println(myAgent.getLocalName()+" RECEIVED THE ENVIRONMENT PATH FROM "+msg.getSender().getLocalName());
                        _ImageCodecs= new Imgcodecs();
                        _Environment= _ImageCodecs.imread(msgContentParts[1],Imgcodecs.IMREAD_GRAYSCALE);
                        Random rand= new Random();
                        int isWidth= rand.nextInt(2);
                        if(isWidth==1)
                        {
                            _Position= new Point((double)rand.nextInt((int)_Environment.size().width),0.0);
                        }
                        else
                        {
                            _Position= new Point(0.0,(double)rand.nextInt((int)_Environment.size().height));
                        }
                        SendPositionToManager();
                        addBehaviour(new SteppingBehaviour((ScanningAgent)myAgent ,1000));
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
    catch (FIPAException fe) {
        fe.printStackTrace();
    }
}
    public void SendPositionToManager()
    {
        ACLMessage msg= new ACLMessage(ACLMessage.INFORM);

        msg.setContent("my_position@"+_Position.x+"@"+_Position.y);
        msg.addReceiver(_DataManager);
        send(msg);
    }
}
