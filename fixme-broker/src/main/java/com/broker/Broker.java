package com.broker;

import com.core.Client;

public class Broker extends Client{

    public static void main (String []args) throws Exception {
        Broker broker = new Broker("localhost", 5000);
        Thread clientThread = new Thread(broker);
        clientThread.start();
        //broker.run();
    }

    public Broker(String host, int port) throws Exception {
        super(host, port);
    }
}
