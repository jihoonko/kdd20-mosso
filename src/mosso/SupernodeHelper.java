package mosso;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import jdk.jshell.spi.ExecutionControl;
import mosso.SummaryGraphModule;
import mosso.AdjacencyList;
import mosso.util.*;

import static java.lang.Double.min;

public class SupernodeHelper extends SummaryGraphModule {
    protected ObjectArrayList<IntHashSet> invV = new ObjectArrayList<>();
    private IntArrayList deg = new IntArrayList();

    private IntArrayList emptyNodeStack = new IntArrayList();

    // Helper variables for counting
    private boolean useEdgeCounter = true;
    private AdjacencyList E = new AdjacencyList();
    private IntArrayList EdgeCnt = new IntArrayList();

    public SupernodeHelper(boolean directed) {
        super(directed);
        if(directed){
            try {
                throw new ExecutionControl.NotImplementedException("Directed version is NOT_IMPLEMENTED");
            } catch (ExecutionControl.NotImplementedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    protected int getSize(int Sv){
        return invV.get(Sv).size();
    }

    protected int getDegree(int v){
        return deg.getInt(v);
    }


    protected int getNumberOfSupernodes(){
        return n - emptyNodeStack.size();
    }


    protected int newSupernode(){
        return emptyNodeStack.topInt();
    }


    protected IntSet getMembers(int Sv){
        return invV.get(Sv);
    }

    protected IntArrayList getNeighbors(int v){
        // Based on algorithms in SWeG
        IntOpenHashSet nbrs = new IntOpenHashSet();
        int Sv = V.getInt(v);
        for(int nbr: P.getNeighbors(Sv)){
            nbrs.addAll(invV.get(nbr));
        }
        for(int nbr: Cm.getNeighbors(v)){
            nbrs.remove(nbr);
        }
        nbrs.remove(v);
        IntArrayList Nv = new IntArrayList(nbrs);
        for(int nbr: Cp.getNeighbors(v)){
            Nv.add(nbr);
        }
        return Nv;
    }

    protected int getNeighborCost(int v){
        return getDegree(v) + P.getAdjList().get(V.getInt(v)).size() + 2 * Cm.getAdjList().get(v).size();
    }

    protected int getSupernodeDegree(int Sv){
        return E.getAdjList().get(Sv).size();
    }

    protected IntArrayList getRandomNeighbors(int v, int k) {
        int Sv = V.getInt(v);
        IntArrayList randomNeighbors = new IntArrayList(k);
        Int2IntHashMap Cp_v = Cp.getAdjList().get(v);
        Int2IntHashMap Cm_v = Cm.getAdjList().get(v);
        Int2IntHashMap P_v = P.getAdjList().get(Sv);

        int psz = Cp.getAdjList().get(v).size();
        int now = -1, nowsz = 0;

        for (int i = 0; i < k; i++) {
            int idx = randInt(0, deg.getInt(v) - 1);
            if (idx < psz) {
                randomNeighbors.add(Cp_v.getRandomElement());
            } else {
                while (true) {
                    int candidate = P_v.getRandomElement(), candisz = P_v.get(candidate);
                    // MCMC for sampling neighbors
                    if (now == -1 || randDouble() < min(1.0, candisz / (double) nowsz)) {
                        now = candidate;
                        nowsz = candisz;
                    }
                    int nbd = invV.get(now).getRandomElement();
                    if (!Cm_v.containsKey(nbd) && (nbd != v)) {
                        randomNeighbors.add(nbd);
                        break;
                    }
                }
            }
        }
        return randomNeighbors;
    }

    protected int getEdgeCount(int Su, int Sv){
        return E.getEdgeCount(Su, Sv);
    }

    protected Int2IntOpenHashMap getEdgeCountAll(int Su){
        return E.getAdjList().get(Su).clone();
    }

    protected void updateEdgeCount(int Su, int Sv, int delta){
        E.updateEdge(Su, Sv, delta);
        EdgeCnt.set(Su, EdgeCnt.getInt(Su) + delta);
    }

    protected int getTotalEdgeCount(int Sv){
        return EdgeCnt.getInt(Sv);
    }

    protected IntSet getSupernodeNeighbors(int Sv){
        if(useEdgeCounter) return E.getNeighbors(Sv);
        else return null;
    }

    protected Int2IntMap.FastEntrySet getSupernodeNeighborsAndWeights(int Sv){
        if(useEdgeCounter) return E.getNeighborsAndWeights(Sv);
        else return null;
    }

    protected Int2IntOpenHashMap getRawNeighbors(int Sv){
        if(useEdgeCounter) return E.getAdjList().get(Sv);
        else return null;
    }

    protected void moveNode(int v, int R, int S){
        if(R > -1) invV.get(R).remove(v);
        if(S > -1) invV.get(S).add(v);
        V.set(v, S);
        
        // Do not change ordering! (it is crucial)
        if(S > -1 && getSize(S) == 1) emptyNodeStack.popInt();
        if(R > -1 && getSize(R) == 0) emptyNodeStack.push(R);
    }

    @Override
    public void addVertex(int idx) {
        super.addVertex(idx);
        if(useEdgeCounter){
            E.expand();
            EdgeCnt.add(0);
        }
        deg.add(0);
        int[] newSingleton = {n-1};
        invV.add(new IntHashSet(newSingleton));
    }

    @Override
    public void processEdge(final int src, final int dst, final boolean add) {
        //System.out.println(src + " -> " + dst + " : " + add);
        final int SRC = V.getInt(src), DST = V.getInt(dst);
        if (useEdgeCounter) {
            if (add) {
                E.updateEdge(SRC, DST, 1);
                EdgeCnt.set(SRC, EdgeCnt.getInt(SRC) + 1);
                if (!directed) {
                    E.updateEdge(DST, SRC, 1);
                    EdgeCnt.set(DST, EdgeCnt.getInt(DST) + 1);
                }
            } else {
                E.updateEdge(SRC, DST, -1);
                EdgeCnt.set(SRC, EdgeCnt.getInt(SRC) - 1);
                if (!directed) {
                    E.updateEdge(DST, SRC, -1);
                    EdgeCnt.set(DST, EdgeCnt.getInt(DST) - 1);
                }
            }
        }
        deg.set(src, deg.getInt(src) + (add ? 1 : -1));
        deg.set(dst, deg.getInt(dst) + (add ? 1 : -1));
    }

    @Override
    public void processBatch(){
    }

    protected long printForDebug(){
        return 0;
    }
}
