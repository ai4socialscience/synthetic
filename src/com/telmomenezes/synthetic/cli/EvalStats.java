package com.telmomenezes.synthetic.cli;

import com.telmomenezes.synthetic.Generator;
import com.telmomenezes.synthetic.Net;


public class EvalStats extends Command {
    public boolean run() throws SynCliException {
    	String netfile = getStringParam("inet");
    	String progFile = getStringParam("prg");
    	String outProg = getStringParam("oprg");
    	int trials = getIntegerParam("trials", 50);
        boolean directed = !paramExists("undir");
    	
        Net net = Net.load(netfile, directed);
        
        System.out.println(net);
        
        Generator gen = new Generator(net.getNodeCount(), net.getEdgeCount(), directed, trials);
        gen.load(progFile);
        gen.run();
        
        gen.getProg().write(outProg, true);
    	
        System.out.println("done.");
        
        return true;
    }
}