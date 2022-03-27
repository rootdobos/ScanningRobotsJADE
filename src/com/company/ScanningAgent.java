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
import org.opencv.core.Point;
public class ScanningAgent extends Agent
{
    private AID _DataManager;
    private Point _Position;
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

}
