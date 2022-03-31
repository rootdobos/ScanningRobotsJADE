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

import javax.swing.*;
import java.util.*;

public class ScanningAgent extends Agent {
    private AID _DataManager;
    private Point _Position;
    private Imgcodecs _ImageCodecs;
    private Mat _Environment;
    private Queue<Point> _SuggestedCoordinates = new LinkedList<Point>();
    private ArrayList<Point> _EdgePoints= new ArrayList<>() ;
    private Point _CluePoint= null;

    protected void setup() {
        System.out.println("Agent: " + getLocalName() + " searching for manager");
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("image-scanning");
            template.addServices(templateSd);

            SearchConstraints sc = new SearchConstraints();
            DFAgentDescription[] results = DFService.search(this, template);
            if (results.length > 0) {
                System.out.println("Agent " + getLocalName() + " found the manager:");
                for (int i = 0; i < results.length; ++i) {
                    DFAgentDescription dfd = results[i];
                    AID provider = dfd.getName();
                    _DataManager = provider;
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
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                msg.setContent("new_scanner_checkin");
                msg.addReceiver(_DataManager);
                send(msg);
                System.out.println(getLocalName() + " SENT CHECKIN MESSAGE  TO " + _DataManager.getName());
            } else {
                System.out.println("Agent " + getLocalName() + " did not find the Manager");
            }
            addBehaviour(new CyclicBehaviour(this) {
                public void action() {
                    // listen if a greetings message arrives
                    ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    if (msg != null) {
                        String msgContent = msg.getContent();
                        String[] msgContentParts = msgContent.split("@");
                        if (msgContentParts[0].equalsIgnoreCase("environment_path")) {
                            // if a greetings message is arrived then send an ANSWER
                            System.out.println(myAgent.getLocalName() + " RECEIVED THE ENVIRONMENT PATH FROM " + msg.getSender().getLocalName());
                            _ImageCodecs = new Imgcodecs();
                            _Environment = _ImageCodecs.imread(msgContentParts[1], Imgcodecs.IMREAD_GRAYSCALE);
                            Random rand = new Random();
                            int isWidth = rand.nextInt(2);
                            int isMax = rand.nextInt(2);
                            if (isWidth == 1) {
                                double y = 0;
                                if (isMax == 1) {
                                    y = _Environment.size().height - 1;
                                }
                                //_Position= new Point(x,(double)rand.nextInt((int)_Environment.size().width));
                                _Position = new Point((double) rand.nextInt((int) _Environment.size().width), y);
                            } else {
                                double x = 0;
                                if (isMax == 1) {
                                    x = _Environment.size().width - 1;
                                }
                                //_Position= new Point((double)rand.nextInt((int)_Environment.size().height),y);
                                _Position = new Point(x, (double) rand.nextInt((int) _Environment.size().height));
                            }
                            SendPositionToManager(true);
                            addBehaviour(new SteppingBehaviour((ScanningAgent) myAgent, 20));
                        }else if(msgContentParts[0].equalsIgnoreCase("new_edge_found")) {
                            int x= Integer.parseInt(msgContentParts[1]);
                            int y= Integer.parseInt(msgContentParts[2]);
                            Point edgePoint= new Point((double)x,(double)y);
                            if(!ContainsCoordinates(_EdgePoints,edgePoint))
                            {
                                _CluePoint=edgePoint;
                            }
                        }
                        else {
                            System.out.println(myAgent.getLocalName() + " Unexpected message received from " + msg.getSender().getLocalName());
                        }
                    } else {
                        // if no message is arrived, block the behaviour
                        block();
                    }
                }
            });
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public void SendPositionToManager(boolean starter) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        if (starter) {
            msg.setContent("my_starter_position@" + _Position.x + "@" + _Position.y);
        } else {
            msg.setContent("my_position@" + _Position.x + "@" + _Position.y);
        }
        msg.addReceiver(_DataManager);
        send(msg);
    }

    public void NextStep() {
        if(_SuggestedCoordinates.size()==0) {
            if(_CluePoint!=null)
            {
                GoForClue();
            }
            else{
            RandomEdgeSearching();}

        }
        else
        {
            Point nextSuggested= _SuggestedCoordinates.remove();
            double[] pixel = _Environment.get((int)nextSuggested.y,(int)nextSuggested.x);
            if (pixel[0] < 127  ) {
                _CluePoint=null;
                if(!ContainsCoordinates(_EdgePoints,nextSuggested)) {
                    RegisterEdgePoint((int) nextSuggested.x, (int) nextSuggested.y);
                }
                else
                {
                    _SuggestedCoordinates= new LinkedList<Point>();
                    RandomEdgeSearching();
                }
            }
            else
            {
                _Position=nextSuggested;
            }
        }
    }
    private Point NewPositionWithClue()
    {
        double[] pixelValue;
        if(_Position.x<_CluePoint.x)
        {
            pixelValue = _Environment.get((int)_Position.y,(int)_Position.x+1);
            if(pixelValue[0] > 127) {
                _Position.x++;
                return _Position;
            }
        }
        else if(_Position.x>_CluePoint.x)
        {
            pixelValue = _Environment.get((int)_Position.y,(int)_Position.x-1);
            if(pixelValue[0] > 127) {
                _Position.x--;
                return _Position;
            }
        }
        if(_Position.y<_CluePoint.y)
        {
            pixelValue = _Environment.get((int)_Position.y+1,(int)_Position.x);
            if(pixelValue[0] > 127) {
                _Position.y++;
                return _Position;
            }
        }
        else if(_Position.y>_CluePoint.y)
        {
            pixelValue = _Environment.get((int)_Position.y-1,(int)_Position.x);
            if(pixelValue[0] > 127) {
                _Position.y--;
                return _Position;
            }
        }
        return _Position;
    }
    private void GoForClue()
    {

        _Position= NewPositionWithClue();
        LookNeighbourhoud();
    }
    private boolean ContainsCoordinates(ArrayList<Point> container,Point point)
    {
        for (Point p: container
             ) {
            if(point.x==p.x && point.y==p.y)
            {return true;}
        }
        return  false;
    }
    private void RandomEdgeSearching()
    {
        LookNeighbourhoud();
        int[][] neighbours=Neighbours();
        int index = 0;
        double[] pixelValue;
        do {
        do {
            Random rand = new Random();
            index = rand.nextInt(4);
        } while ((neighbours[index][0] < 0 || neighbours[index][1] < 0
                || neighbours[index][0] >= _Environment.size().width || neighbours[index][1] >= _Environment.size().height));
        //pixelValue=_Environment.get(neighbours[index][0],neighbours[index][1]);
        pixelValue = _Environment.get(neighbours[index][1], neighbours[index][0]);
    } while (pixelValue[0] < 127);
        _Position = new Point((double) neighbours[index][0], (double) neighbours[index][1]);
    }
    private int[][] Neighbours()
    { int x = (int) _Position.x;
        int y = (int) _Position.y;
        int[][] neighbours = new int[8][2];
        neighbours[0][0] = x;
        neighbours[0][1] = y - 1;
        neighbours[1][0] = x + 1;
        neighbours[1][1] = y;
        neighbours[2][0] = x - 1;
        neighbours[2][1] = y;
        neighbours[3][0] = x;
        neighbours[3][1] = y + 1;
        neighbours[4][0] = x - 1;
        neighbours[4][1] = y - 1;
        neighbours[5][0] = x + 1;
        neighbours[5][1] = y - 1;
        neighbours[6][0] = x + 1;
        neighbours[6][1] = y + 1;
        neighbours[7][0] = x - 1;
        neighbours[7][1] = y + 1;
    return neighbours;}
    private void LookNeighbourhoud()
    {
        int x = (int) _Position.x;
        int y = (int) _Position.y;
        int[][] neighbours = Neighbours();
        for (int i = 0; i < 8; i++) {
            if ((neighbours[i][0] < 0 || neighbours[i][1] < 0 || neighbours[i][0] >= _Environment.size().width || neighbours[i][1] >= _Environment.size().height)) {
                continue;
            }
            //double[] pixel=_Environment.get(neighbours[i][0],neighbours[i][1]);
            double[] pixel = _Environment.get(neighbours[i][1], neighbours[i][0]);
            if (pixel == null) {
                System.out.println("Null pixel value: Position: " + neighbours[i][0] + ":" + neighbours[i][1]);
            }
            if (pixel[0] < 127 ) {
                _CluePoint=null;
                if(!ContainsCoordinates(_EdgePoints,new Point((double)neighbours[i][0],(double)neighbours[i][1]))){
                RegisterEdgePoint(neighbours[i][0],neighbours[i][1]);}
            }
        }
    }
    private void RegisterEdgePoint(int x,int y)
    {
        _EdgePoints.add(new Point((double)x,(double)y));
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("edge_position@" + x + "@" +y);
        msg.addReceiver(_DataManager);
        send(msg);
        _SuggestedCoordinates= new LinkedList<Point>();
        if(_Position.x==(double)x-1 &&_Position.y==(double)y-1)
        {
            _SuggestedCoordinates.add(new Point((double)x,(double)y-1));
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y-1));
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y));
        }
        if(_Position.x==(double)x &&_Position.y==(double)y-1)
        {
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y-1));
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y));
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y+1));
        }
        if(_Position.x==(double)x+1 &&_Position.y==(double)y-1)
        {
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y));
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y+1));
            _SuggestedCoordinates.add(new Point((double)x,(double)y+1));
        }
        if(_Position.x==(double)x+1 &&_Position.y==(double)y)
        {
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y+1));
            _SuggestedCoordinates.add(new Point((double)x,(double)y+1));
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y+1));
        }
        if(_Position.x==(double)x+1 &&_Position.y==(double)y+1)
        {
            _SuggestedCoordinates.add(new Point((double)x,(double)y+1));
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y+1));
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y));
        }
        if(_Position.x==(double)x &&_Position.y==(double)y+1)
        {
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y+1));
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y));
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y-1));
        }
        if(_Position.x==(double)x-1 &&_Position.y==(double)y+1)
        {
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y));
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y-1));
            _SuggestedCoordinates.add(new Point((double)x,(double)y-1));
        }
        if(_Position.x==(double)x-1 &&_Position.y==(double)y)
        {
            _SuggestedCoordinates.add(new Point((double)x-1,(double)y-1));
            _SuggestedCoordinates.add(new Point((double)x,(double)y-1));
            _SuggestedCoordinates.add(new Point((double)x+1,(double)y-1));
        }
    }
}
