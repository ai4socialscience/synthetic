/*
 * This class computes the Earth Mover's Distance, using the EMD-HAT algorithm
 * created by Ofir Pele and Michael Werman.
 * 
 * This implementation is strongly based on the C++ code by the same authors,
 * that can be found here:
 * http://www.cs.huji.ac.il/~ofirpele/FastEMD/code/
 * 
 * Some of the author's comments on the original were kept or edited for 
 * this context.
 */



package com.telmomenezes.synthetic.emd;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

class Edge {
    Edge(int to, long cost) {
        _to = to;
        _cost = cost;
    }

    int _to;
    long _cost;
}

class Edge0 {
    Edge0(int to, long cost, long flow) {
        _to = to;
        _cost = cost;
        _flow = flow;
    }

    int _to;
    long _cost;
    long _flow;
}

class Edge1 {
    Edge1(int to, long reduced_cost) {
        _to = to;
        _reduced_cost = reduced_cost;
    }

    int _to;
    long _reduced_cost;
}

class Edge2 {
    Edge2(int to, long reduced_cost, long residual_capacity) {
        _to = to;
        _reduced_cost = reduced_cost;
        _residual_capacity = residual_capacity;
    }

    int _to;
    long _reduced_cost;
    long _residual_capacity;
}

class Edge3 {
    Edge3() {
        _to = 0;
        _dist = 0;
    }

    int _to;
    long _dist;
}

class MinCostFlow {

    private int numNodes;
    private Vector<Integer> nodesToQ;

    // e - supply(positive) and demand(negative).
    // c[i] - edges that goes from node i. first is the second nod
    // x - the flow is returned in it
    long compute(Vector<Long> e, Vector<List<Edge>> c, Vector<List<Edge0>> x) {
        assert (e.size() == c.size());
        assert (x.size() == c.size());

        numNodes = e.size();
        nodesToQ = new Vector<Integer>();
        for (int i = 0; i < numNodes; i++) {
            nodesToQ.add(0);
        }

        // init flow
        for (int from = 0; from < numNodes; ++from) {
            for (Edge it : c.get(from)) {
                x.get(from).add(new Edge0(it._to, it._cost, 0));
                x.get(it._to).add(new Edge0(from, -it._cost, 0));
            }
        }

        // reduced costs for forward edges (c[i,j]-pi[i]+pi[j])
        // Note that for forward edges the residual capacity is infinity
        Vector<List<Edge1>> rCostForward = new Vector<List<Edge1>>();
        for (int i = 0; i < numNodes; i++) {
            rCostForward.add(new LinkedList<Edge1>());
        }
        for (int from = 0; from < numNodes; ++from) {
            for (Edge it : c.get(from)) {
                rCostForward.get(from).add(new Edge1(it._to, it._cost));
            }
        }

        // reduced costs and capacity for backward edges
        // (c[j,i]-pi[j]+pi[i])
        // Since the flow at the beginning is 0, the residual capacity is
        // also zero
        Vector<List<Edge2>> rCostCapBackward = new Vector<List<Edge2>>();
        for (int i = 0; i < numNodes; i++) {
            rCostCapBackward.add(new LinkedList<Edge2>());
        }
        for (int from = 0; from < numNodes; ++from) {
            for (Edge it : c.get(from)) {
                rCostCapBackward.get(it._to).add(
                        new Edge2(from, -it._cost, 0));
            }
        }

        // Max supply TODO:demand?, given U?, optimization-> min out of
        // demand,supply
        long U = 0;
        for (int i = 0; i < numNodes; i++) {
            if (e.get(i) > U)
                U = e.get(i);
        }
        long delta;

        Vector<Long> d = new Vector<Long>();
        Vector<Integer> prev = new Vector<Integer>();
        for (int i = 0; i < numNodes; i++) {
            d.add(0L);
            prev.add(0);
        }
        while (true) { // until we break when S or T is empty
            long maxSupply = 0;
            int k = 0;
            for (int i = 0; i < numNodes; i++) {
                if (e.get(i) > 0) {
                    if (maxSupply < e.get(i)) {
                        maxSupply = e.get(i);
                        k = i;
                    }
                }
            }
            if (maxSupply == 0)
                break;
            delta = maxSupply;

            int[] l = new int[1];
            computeShortestPath(d, prev, k, rCostForward, rCostCapBackward,
                    e, l);

            // find delta (minimum on the path from k to l)
            // delta= e[k];
            // if (-e[l]<delta) delta= e[k];
            int to = l[0];
            do {
                int from = prev.get(to);
                assert (from != to);

                // residual
                int itccb = 0;
                while ((itccb < rCostCapBackward.get(from).size())
                        && (rCostCapBackward.get(from).get(itccb)._to != to)) {
                    itccb++;
                }
                if (itccb < rCostCapBackward.get(from).size()) {
                    if (rCostCapBackward.get(from).get(itccb)._residual_capacity < delta)
                        delta = rCostCapBackward.get(from).get(itccb)._residual_capacity;
                }

                to = from;
            } while (to != k);

            // augment delta flow from k to l (backwards actually...)
            to = l[0];
            do {
                int from = prev.get(to);
                assert (from != to);

                // TODO - might do here O(n) can be done in O(1)
                int itx = 0;
                while (x.get(from).get(itx)._to != to) {
                    itx++;
                }
                x.get(from).get(itx)._flow += delta;

                // update residual for backward edges
                int itccb = 0;
                while ((itccb < rCostCapBackward.get(to).size())
                        && (rCostCapBackward.get(to).get(itccb)._to != from)) {
                    itccb++;
                }
                if (itccb < rCostCapBackward.get(to).size()) {
                    rCostCapBackward.get(to).get(itccb)._residual_capacity += delta;
                }
                itccb = 0;
                while ((itccb < rCostCapBackward.get(from).size())
                        && (rCostCapBackward.get(from).get(itccb)._to != to)) {
                    itccb++;
                }
                if (itccb < rCostCapBackward.get(from).size()) {
                    rCostCapBackward.get(from).get(itccb)._residual_capacity -= delta;
                }

                // update e
                e.set(to, e.get(to) + delta);
                e.set(from, e.get(from) - delta);

                to = from;
            } while (to != k);
        }

        // compute distance from x
        long dist = 0;
        for (int from = 0; from < numNodes; from++) {
            for (Edge0 it : x.get(from)) {
                dist += (it._cost * it._flow);
            }
        }
        return dist;
    }

    private void computeShortestPath(Vector<Long> d, Vector<Integer> prev,
            int from, Vector<List<Edge1>> costForward,
            Vector<List<Edge2>> costBackward, Vector<Long> e, int[] l) {
        // Making heap (all inf except 0, so we are saving comparisons...)
        Vector<Edge3> Q = new Vector<Edge3>();
        for (int i = 0; i < numNodes; i++) {
            Q.add(new Edge3());
        }

        Q.get(0)._to = from;
        nodesToQ.set(from, 0);
        Q.get(0)._dist = 0;

        int j = 1;
        // TODO: both of these into a function?
        for (int i = 0; i < from; ++i) {
            Q.get(j)._to = i;
            nodesToQ.set(i, j);
            Q.get(j)._dist = Long.MAX_VALUE;
            j++;
        }

        for (int i = from + 1; i < numNodes; i++) {
            Q.get(j)._to = i;
            nodesToQ.set(i, j);
            Q.get(j)._dist = Long.MAX_VALUE;
            j++;
        }

        Vector<Boolean> finalNodesFlg = new Vector<Boolean>();
        for (int i = 0; i < numNodes; i++) {
            finalNodesFlg.add(false);
        }
        do {
            int u = Q.get(0)._to;

            d.set(u, Q.get(0)._dist); // final distance
            finalNodesFlg.set(u, true);
            if (e.get(u) < 0) {
                l[0] = u;
                break;
            }

            heapRemoveFirst(Q, nodesToQ);

            // neighbors of u
            for (Edge1 it : costForward.get(u)) {
                assert (it._reduced_cost >= 0);
                long alt = d.get(u) + it._reduced_cost;
                int v = it._to;
                if ((nodesToQ.get(v) < Q.size())
                        && (alt < Q.get(nodesToQ.get(v))._dist)) {
                    heapDecreaseKey(Q, nodesToQ, v, alt);
                    prev.set(v, u);
                }
            }
            for (Edge2 it : costBackward.get(u)) {
                if (it._residual_capacity > 0) {
                    assert (it._reduced_cost >= 0);
                    long alt = d.get(u) + it._reduced_cost;
                    int v = it._to;
                    if ((nodesToQ.get(v) < Q.size())
                            && (alt < Q.get(nodesToQ.get(v))._dist)) {
                        heapDecreaseKey(Q, nodesToQ, v, alt);
                        prev.set(v, u);
                    }
                }
            }

        } while (Q.size() > 0);

        for (int _from = 0; _from < numNodes; ++_from) {
            for (Edge1 it : costForward.get(_from)) {
                if (finalNodesFlg.get(_from)) {
                    it._reduced_cost += d.get(_from) - d.get(l[0]);
                }
                if (finalNodesFlg.get(it._to)) {
                    it._reduced_cost -= d.get(it._to) - d.get(l[0]);
                }
            }
        }

        // reduced costs and capacity for backward edges
        // (c[j,i]-pi[j]+pi[i])
        for (int _from = 0; _from < numNodes; ++_from) {
            for (Edge2 it : costBackward.get(_from)) {
                if (finalNodesFlg.get(_from)) {
                    it._reduced_cost += d.get(_from) - d.get(l[0]);
                }
                if (finalNodesFlg.get(it._to)) {
                    it._reduced_cost -= d.get(it._to) - d.get(l[0]);
                }
            }
        }
    }

    private void heapDecreaseKey(Vector<Edge3> Q, Vector<Integer> nodes_to_Q,
            int v, long alt) {
        int i = nodes_to_Q.get(v);
        Q.get(i)._dist = alt;
        while (i > 0 && Q.get(PARENT(i))._dist > Q.get(i)._dist) {
            swapHeap(Q, nodes_to_Q, i, PARENT(i));
            i = PARENT(i);
        }
    }

    private void heapRemoveFirst(Vector<Edge3> Q, Vector<Integer> nodes_to_Q) {
        swapHeap(Q, nodes_to_Q, 0, Q.size() - 1);
        Q.remove(Q.size() - 1);
        heapify(Q, nodes_to_Q);
    }

    private void heapify(Vector<Edge3> Q, Vector<Integer> nodes_to_Q) {
        int i = 0;
        do {
            int l = LEFT(i);
            int r = RIGHT(i);
            int smallest;
            if ((l < Q.size()) && (Q.get(l)._dist < Q.get(i)._dist)) {
                smallest = l;
            } else {
                smallest = i;
            }
            if ((r < Q.size()) && (Q.get(r)._dist < Q.get(smallest)._dist)) {
                smallest = r;
            }

            if (smallest == i)
                return;

            swapHeap(Q, nodes_to_Q, i, smallest);
            i = smallest;

        } while (true);
    }

    private void swapHeap(Vector<Edge3> Q, Vector<Integer> nodesToQ, int i, int j) {
        Edge3 tmp = Q.get(i);
        Q.set(i, Q.get(j));
        Q.set(j, tmp);
        nodesToQ.set(Q.get(j)._to, j);
        nodesToQ.set(Q.get(i)._to, i);
    }

    private int LEFT(int i) {
        return 2 * (i + 1) - 1;
    }

    private int RIGHT(int i) {
        return 2 * (i + 1); // 2 * (i + 1) + 1 - 1
    }

    private int PARENT(int i) {
        return (i - 1) / 2;
    }
}

/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 * @author Ofir Pele
 *
 */
public class JFastEMD {
    /**
     * This interface is similar to Rubner's interface. See:
     * http://www.cs.duke.edu/~tomasi/software/emd.htm
     *
     * To get the same results as Rubner's code you should set extra_mass_penalty to 0,
     * and divide by the minimum of the sum of the two signature's weights. However, I
     * suggest not to do this as you lose the metric property and more importantly, in my
     * experience the performance is better with emd_hat. for more on the difference
     * between emd and emd_hat, see the paper:
     * A Linear Time Histogram Metric for Improved SIFT Matching
     * Ofir Pele, Michael Werman
     * ECCV 2008
     *
     * To get shorter running time, set the ground distance function to
     * be a thresholded distance. For example: min(L2, T). Where T is some threshold.
     * Note that the running time is shorter with smaller T values. Note also that
     * thresholding the distance will probably increase accuracy. Finally, a thresholded
     * metric is also a metric. See paper:
     * Fast and Robust Earth Mover's Distances
     * Ofir Pele, Michael Werman
     * ICCV 2009
     *
     * If you use this code, please cite the papers.
     */
    static public double distance(Signature signature1, Signature signature2, double extraMassPenalty) {

        Vector<Double> P = new Vector<Double>();
        Vector<Double> Q = new Vector<Double>();
        for (int i = 0; i < signature1.getNumberOfFeatures() + signature2.getNumberOfFeatures(); i++) {
            P.add(0.0);
            Q.add(0.0);
        }
        for (int i = 0; i < signature1.getNumberOfFeatures(); i++) {
            P.set(i, signature1.getWeights()[i]);
        }
        for (int j = 0; j < signature2.getNumberOfFeatures(); j++) {
            Q.set(j + signature1.getNumberOfFeatures(), signature2.getWeights()[j]);
        }

        Vector<Vector<Double>> C = new Vector<Vector<Double>>();
        for (int i = 0; i < P.size(); i++) {
            Vector<Double> vec = new Vector<Double>();
            for (int j = 0; j < P.size(); j++) {
                vec.add(0.0);
            }
            C.add(vec);
        }
        for (int i = 0; i < signature1.getNumberOfFeatures(); i++) {
            for (int j = 0; j < signature2.getNumberOfFeatures(); j++) {
                double dist = signature1.getFeatures()[i]
                        .groundDist(signature2.getFeatures()[j]);
                assert (dist >= 0);
                C.get(i).set(j + signature1.getNumberOfFeatures(), dist);
                C.get(j + signature1.getNumberOfFeatures()).set(i, dist);
            }
        }

        return emdHat(P, Q, C, extraMassPenalty);
    }
    

    static private long emdHatImplLongLongInt(Vector<Long> Pc, Vector<Long> Qc, Vector<Vector<Long>> C) {
        long extraMassPenalty = 0L;

        int N = Pc.size();
        assert (Qc.size() == N);

        // Ensuring that the supplier - P, have more mass.
        // Note that we assume here that C is symmetric
        Vector<Long> P;
        Vector<Long> Q;
        long absDiffSumPSumQ;
        long sumP = 0;
        long sumQ = 0;
        for (long x: Pc)
            sumP += x;
        for (int i = 0; i < N; i++)
            sumQ += Qc.get(i);
        if (sumQ > sumP) {
            P = Qc;
            Q = Pc;
            absDiffSumPSumQ = sumQ - sumP;
        } else {
            P = Pc;
            Q = Qc;
            absDiffSumPSumQ = sumP - sumQ;
        }

        // creating the b vector that contains all vertexes
        Vector<Long> b = new Vector<Long>();
        for (int i = 0; i < 2 * N + 2; i++) {
            b.add(0L);
        }
        int THRESHOLD_NODE = 2 * N;
        int ARTIFICIAL_NODE = 2 * N + 1; // need to be last !
        for (int i = 0; i < N; i++) {
            b.set(i, P.get(i));
        }
        for (int i = N; i < 2 * N; i++) {
            b.set(i, Q.get(i - N));
        }

        // remark*) I put here a deficit of the extra mass, as mass that flows
        // to the threshold node
        // can be absorbed from all sources with cost zero (this is in reverse
        // order from the paper,
        // where incoming edges to the threshold node had the cost of the
        // threshold and outgoing
        // edges had the cost of zero)
        // This also makes sum of b zero.
        b.set(THRESHOLD_NODE, -absDiffSumPSumQ);
        b.set(ARTIFICIAL_NODE, 0L);

        long maxC = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                assert (C.get(i).get(j) >= 0);
                if (C.get(i).get(j) > maxC)
                    maxC = C.get(i).get(j);
            }
        }

        Set<Integer> sourcesThatFlowNotOnlyToThresh = new HashSet<Integer>();
        Set<Integer> sinksThatGetFlowNotOnlyFromThresh = new HashSet<Integer>();
        long preFlowCost = 0;

        // regular edges between sinks and sources without threshold edges
        Vector<List<Edge>> c = new Vector<List<Edge>>();
        for (int i = 0; i < b.size(); i++) {
            c.add(new LinkedList<Edge>());
        }
        for (int i = 0; i < N; i++) {
            if (b.get(i) == 0)
                continue;
            for (int j = 0; j < N; j++) {
                if (b.get(j + N) == 0)
                    continue;
                if (C.get(i).get(j) == maxC)
                    continue;
                c.get(i).add(new Edge(j + N, C.get(i).get(j)));
            }
        }

        // checking which are not isolated
        for (int i = 0; i < N; i++) {
            if (b.get(i) == 0)
                continue;
            for (int j = 0; j < N; j++) {
                if (b.get(j + N) == 0)
                    continue;
                if (C.get(i).get(j) == maxC)
                    continue;
                sourcesThatFlowNotOnlyToThresh.add(i);
                sinksThatGetFlowNotOnlyFromThresh.add(j + N);
            }
        }

        // converting all sinks to negative
        for (int i = N; i < 2 * N; i++) {
            b.set(i, -b.get(i));
        }

        // add edges from/to threshold node,
        // note that costs are reversed to the paper (see also remark* above)
        // It is important that it will be this way because of remark* above.
        for (int i = 0; i < N; ++i) {
            c.get(i).add(new Edge(THRESHOLD_NODE, 0));
        }
        for (int j = 0; j < N; ++j) {
            c.get(THRESHOLD_NODE).add(new Edge(j + N, maxC));
        }

        // artificial arcs - Note the restriction that only one edge i,j is
        // artificial so I ignore it...
        for (int i = 0; i < ARTIFICIAL_NODE; i++) {
            c.get(i).add(new Edge(ARTIFICIAL_NODE, maxC + 1));
            c.get(ARTIFICIAL_NODE).add(new Edge(i, maxC + 1));
        }

        // remove nodes with supply demand of 0
        // and vertexes that are connected only to the
        // threshold vertex
        int currentNodeName = 0;
        // Note here it should be vector<int> and not vector<int>
        // as I'm using -1 as a special flag !!!
        int REMOVE_NODE_FLAG = -1;
        Vector<Integer> nodesNewNames = new Vector<Integer>();
        for (int i = 0; i < b.size(); i++) {
            nodesNewNames.add(REMOVE_NODE_FLAG);
        }
        for (int i = 0; i < N * 2; i++) {
            if (b.get(i) != 0) {
                if (sourcesThatFlowNotOnlyToThresh.contains(i)
                        || sinksThatGetFlowNotOnlyFromThresh.contains(i)) {
                    nodesNewNames.set(i, currentNodeName);
                    currentNodeName++;
                } else {
                    if (i >= N) {
                        preFlowCost -= (b.get(i) * maxC);
                    }
                    b.set(THRESHOLD_NODE, b.get(THRESHOLD_NODE) + b.get(i)); // add mass(i<N) or deficit (i>=N)
                }
            }
        }
        nodesNewNames.set(THRESHOLD_NODE, currentNodeName);
        currentNodeName++;
        nodesNewNames.set(ARTIFICIAL_NODE, currentNodeName);
        currentNodeName++;

        Vector<Long> bb = new Vector<Long>();
        for (int i = 0; i < currentNodeName; i++) {
            bb.add(0L);
        }
        int j = 0;
        for (int i = 0; i < b.size(); i++) {
            if (nodesNewNames.get(i) != REMOVE_NODE_FLAG) {
                bb.set(j, b.get(i));
                j++;
            }
        }

        Vector<List<Edge>> cc = new Vector<List<Edge>>();
        for (int i = 0; i < bb.size(); i++) {
            cc.add(new LinkedList<Edge>());
        }
        for (int i = 0; i < c.size(); i++) {
            if (nodesNewNames.get(i) == REMOVE_NODE_FLAG)
                continue;
            for (Edge it : c.get(i)) {
                if (nodesNewNames.get(it._to) != REMOVE_NODE_FLAG) {
                    cc.get(nodesNewNames.get(i)).add(
                            new Edge(nodesNewNames.get(it._to), it._cost));
                }
            }
        }

        MinCostFlow mcf = new MinCostFlow();

        long myDist;

        Vector<List<Edge0>> flows = new Vector<List<Edge0>>(bb.size());
        for (int i = 0; i < bb.size(); i++) {
            flows.add(new LinkedList<Edge0>());
        }

        long mcfDist = mcf.compute(bb, cc, flows);

        myDist = preFlowCost + // pre-flowing on cases where it was possible
                mcfDist + // solution of the transportation problem
                (absDiffSumPSumQ * extraMassPenalty); // emd-hat extra mass penalty

        return myDist;
    }

    static private double emdHat(Vector<Double> P, Vector<Double> Q, Vector<Vector<Double>> C,
            double extraMassPenalty) {

        // This condition should hold:
        // ( 2^(sizeof(CONVERT_TO_T*8)) >= ( MULT_FACTOR^2 )
        // Note that it can be problematic to check it because
        // of overflow problems. I simply checked it with Linux calc
        // which has arbitrary precision.
        double MULT_FACTOR = 1000000;

        // Constructing the input
        int N = P.size();
        Vector<Long> iP = new Vector<Long>();
        Vector<Long> iQ = new Vector<Long>();
        Vector<Vector<Long>> iC = new Vector<Vector<Long>>();
        for (int i = 0; i < N; i++) {
            iP.add(0L);
            iQ.add(0L);
            Vector<Long> vec = new Vector<Long>();
            for (int j = 0; j < N; j++) {
                vec.add(0L);
            }
            iC.add(vec);
        }

        // Converting to CONVERT_TO_T
        double sumP = 0.0;
        double sumQ = 0.0;
        double maxC = C.get(0).get(0);
        for (int i = 0; i < N; i++) {
            sumP += P.get(i);
            sumQ += Q.get(i);
            for (int j = 0; j < N; j++) {
                if (C.get(i).get(j) > maxC)
                    maxC = C.get(i).get(j);
            }
        }
        double minSum = Math.min(sumP, sumQ);
        double maxSum = Math.max(sumP, sumQ);
        double PQnormFactor = MULT_FACTOR / maxSum;
        double CnormFactor = MULT_FACTOR / maxC;
        for (int i = 0; i < N; i++) {
            iP.set(i, (long) (Math.floor(P.get(i) * PQnormFactor + 0.5)));
            iQ.set(i, (long) (Math.floor(Q.get(i) * PQnormFactor + 0.5)));
            for (int j = 0; j < N; j++) {
                iC.get(i)
                        .set(j,
                                (long) (Math.floor(C.get(i).get(j)
                                        * CnormFactor + 0.5)));
            }
        }

        // computing distance without extra mass penalty
        double dist = emdHatImplLongLongInt(iP, iQ, iC);
        // unnormalize
        dist = dist / PQnormFactor;
        dist = dist / CnormFactor;

        // adding extra mass penalty
        if (extraMassPenalty == -1)
            extraMassPenalty = maxC;
        dist += (maxSum - minSum) * extraMassPenalty;
        
        return dist;
    }
    
    
    public static void main(String[] args) {
    	int n = 5;
    	
    	double[] w1 = {0, 1, 0, 0, 0};
    	double[] w2 = {1, 0, 0, 0, 0};
    	
    	Feature1D[] f1 = new Feature1D[n];
    	Feature1D[] f2 = new Feature1D[n];
    	for (int i = 0; i < n; i++) {
    		f1[i] = new Feature1D(i);
    		f2[i] = new Feature1D(i);
    	}
    	
    	Signature signature1 = new Signature();
        signature1.setNumberOfFeatures(n);
        signature1.setFeatures(f1);
        signature1.setWeights(w1);
        
        Signature signature2 = new Signature();
        signature2.setNumberOfFeatures(n);
        signature2.setFeatures(f2);
        signature2.setWeights(w2);
        
        System.out.println(distance(signature1, signature2, -1));
    }
}