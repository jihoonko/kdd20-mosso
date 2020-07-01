package mosso;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mosso.util.Int2IntHashMap;

public class AdjacencyList {
    private int n = 0;
    private ObjectArrayList<Int2IntHashMap> adjList = new ObjectArrayList<Int2IntHashMap>(0);

    public AdjacencyList(){}

    public ObjectArrayList<Int2IntHashMap> getAdjList(){
        return adjList;
    }

    public void expand(){
        n += 1;
        Int2IntHashMap newMap = new Int2IntHashMap(0);
        newMap.defaultReturnValue(0);
        adjList.add(newMap);
    }

    public void updateEdge(final int src, final int dst, final int incr){
        assert(src < n && dst < n && src > -1 && dst > -1);
        int prevValue = adjList.get(src).addTo(dst, incr);
        if(prevValue == -incr) adjList.get(src).remove(dst);
    }

    public void addEdge(final int src, final int dst) {
        updateEdge(src, dst, 1);
    }

    public void deleteEdge(final int src, final int dst){
        updateEdge(src, dst, -1);
    }

    public int getEdgeCount(final int src, final int dst){
        return adjList.get(src).getOrDefault(dst, 0);
    }

    public IntSet getNeighbors(final int src){
        return adjList.get(src).keySet();
    }

    public Int2IntMap.FastEntrySet getNeighborsAndWeights(final int src){
        return adjList.get(src).int2IntEntrySet();
    }

    public int getDegree(final int src) { return adjList.get(src).size(); }
}
