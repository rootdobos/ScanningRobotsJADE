package com.company;
import jade.core.Agent;
public class ScanningAgent extends Agent
{
    protected void setup()
    {
        System.out.println("Hello World!");
        System.out.println("My name is: "+ getLocalName());
    }
}
