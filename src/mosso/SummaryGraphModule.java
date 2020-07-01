package mosso;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jdk.jshell.spi.ExecutionControl;

import java.util.concurrent.ThreadLocalRandom;

public class SummaryGraphModule implements IncrementalInterface{
    // Core data structure
    protected IntArrayList V = new IntArrayList(0);
    protected AdjacencyList P = new AdjacencyList();
    protected AdjacencyList Cp = new AdjacencyList();
    protected AdjacencyList Cm = new AdjacencyList();
    protected int n = 0;

    // helper
    private final ThreadLocalRandom random;
    private IntArrayList idxs = new IntArrayList(0);
    private Int2IntOpenHashMap vmap = new Int2IntOpenHashMap(0);

    // argument
    protected boolean directed;

    public SummaryGraphModule(boolean directed){
        this.random = ThreadLocalRandom.current();
        this.directed = directed;
    }

    public void addVertex(int idx) {
        vmap.addTo(idx, n);
        idxs.add(idx);
        V.add(n);
        n += 1;
        P.expand(); Cp.expand(); Cm.expand();
    }

    @Override
    public void processAddition(final int src, final int dst){
        if(!vmap.containsKey(src)) addVertex(src);
        if(!vmap.containsKey(dst)) addVertex(dst);
        int[] vIdx = {vmap.getOrDefault(src, -1), vmap.getOrDefault(dst, -1)};
        processEdge(vIdx[0], vIdx[1], true);
    }

    @Override
    public void processDeletion(final int src, final int dst){
        if(!vmap.containsKey(src)) addVertex(src);
        if(!vmap.containsKey(dst)) addVertex(dst);
        int[] vIdx = {vmap.getOrDefault(src, -1), vmap.getOrDefault(dst, -1)};
        processEdge(vIdx[0], vIdx[1], false);
    }

    public void processBatch(){
        try {
            throw new ExecutionControl.NotImplementedException("processBatch: NOT_IMPLEMENTED");
        } catch (ExecutionControl.NotImplementedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    protected double randDouble(){
        return random.nextDouble();
    }

    protected int randInt(final int from, final int to){
        // return generated random number in [from, to] (close interval)
        return from + random.nextInt(to - from + 1);
    }

    protected long randLong(final long from, final long to){
        // return generated random number in [from, to] (close interval)
        return from + random.nextLong(to - from + 1);
    }

    public void processEdge(final int src, final int dst, final boolean add) {
        try {
            throw new ExecutionControl.NotImplementedException("processEdge: NOT_IMPLEMENTED");
        } catch (ExecutionControl.NotImplementedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public Tuple<IntArrayList, AdjacencyList[], IntArrayList> getCompressedResult(){
        return new Tuple<IntArrayList, AdjacencyList[], IntArrayList>(V, new AdjacencyList[]{P, Cp, Cm}, idxs);
    }
}
