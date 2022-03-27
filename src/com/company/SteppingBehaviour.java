package com.company;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
public class SteppingBehaviour extends TickerBehaviour {
    SteppingBehaviour(ScanningAgent a, long time)
    {
        super(a,time);
    }

    @Override
    protected void onTick() {
        //System.out.println("Agent "+myAgent.getLocalName()+": tick="+getTickCount());
        ((ScanningAgent)myAgent).NextStep();
        ((ScanningAgent)myAgent).SendPositionToManager(false);

    }
}
