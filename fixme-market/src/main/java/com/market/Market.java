package com.market;

import com.core.Client;

public class Market extends Client{

    public static void main (String []args) throws Exception {
        Market market = new Market("localhost", 5001);
        market.run();

    }

    public Market(String host, int port) throws Exception {
        super(host, port);
    }
}
