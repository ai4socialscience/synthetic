package com.telmomenezes.synthetic;

import java.util.Vector;

import com.telmomenezes.synthetic.random.RandomGenerator;


public class Node implements Cloneable {
	private int id;
    private Vector<Edge> inEdges;
    private Vector<Edge> outEdges;
    private int inDegree;
    private int outDegree;

    public int value;
    
    // pageranks
    private double prD;
    private double prDLast;
    private double prU;
    private double prULast;

    // Auxiliary flag for algorithms that need to know if this node was already
    // visited
    private boolean flag;
    
    
    public Node(int id, int value) {
        this.id = id;
        inDegree = 0;
        outDegree = 0;
        inEdges = new Vector<Edge>();
        outEdges = new Vector<Edge>();
        this.value = value;
    }
    
    
    public Node(int id) {
    	this(id, 0);
    }
    
    @Override
    public Node clone()
    {
        return new Node(id, value);
    }

    public int getId() {
        return id;
    }

    public Vector<Edge> getInEdges() {
        return inEdges;
    }
    
    public Vector<Edge> getOutEdges() {
        return outEdges;
    }

    public int getInDegree() {
        return inDegree;
    }

    public int getOutDegree() {
        return outDegree;
    }
    
    public int getDegree() {
        return inDegree + outDegree;
    }

    double getPrD() {
        return prD;
    }

    void setPrD(double prD) {
        this.prD = prD;
    }

    double getPrDLast() {
        return prDLast;
    }

    void setPrDLast(double prDLast) {
        this.prDLast = prDLast;
    }

    double getPrU() {
        return prU;
    }

    void setPrU(double prU) {
        this.prU = prU;
    }

    double getPrULast() {
        return prULast;
    }

    void setPrULast(double prULast) {
        this.prULast = prULast;
    }

    public boolean getFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
    
    void addInEdge(Edge edge) {
        inEdges.add(edge);
        inDegree++;
    }
    
    void addOutEdge(Edge edge) {
        outEdges.add(edge);
        outDegree++;
    }
    
    
    public Node getRandomOutputNode() {
    	int size = outEdges.size();
    	
    	if (size == 0) {
    		return null;
    	}
    	
    	int index = RandomGenerator.random.nextInt(size);
    	return outEdges.get(index).getTarget();
    }
    
    
    public Node getRandomInputNode() {
    	int size = inEdges.size();
    	
    	if (size == 0) {
    		return null;
    	}
    	
    	int index = RandomGenerator.random.nextInt(size);
    	return inEdges.get(index).getOrigin();
    }
    
    
    public Node getRandomNeighbour() {
    	double ins = inDegree;
    	double outs = outDegree;
    	double total = ins + outs;
    	
    	if (total <= 0) {
    		return null;
    	}
    	
    	double probIn = ins / total;
    	
    	if (RandomGenerator.random.nextDouble() < probIn) {
    		return getRandomInputNode();
    	}
    	else {
    		return getRandomOutputNode();
    	}
    }
    
    
    @Override
    public String toString() {
    	return "" + id;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		return id == other.id;
	}
}