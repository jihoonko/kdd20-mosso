package mosso.algorithm;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mosso.SupernodeHelper;

import static java.lang.Long.min;

public class MoSSoGreedy extends SupernodeHelper {
    private int iteration = 0;
    private IntOpenHashSet sn = new IntOpenHashSet();

    private int ecnt = 0, interval;
    private long costCounter = 0;
    private long start;

    public MoSSoGreedy(boolean directed, int _interval){
        super(directed);
        interval = _interval;
        start = System.currentTimeMillis();
    }

    @Override
    public void addVertex(int idx){
        super.addVertex(idx);
        sn.add(n-1);
    }

    private long getPi(final int Su, final int Sv){
        long pi = getSize(Su); pi *= getSize(Sv);
        if(Su == Sv){
            pi -= getSize(Sv);
            pi /= 2;
        }
        return pi;
    }

    private void updateSuperedge(int Su, int Sv, boolean add) {
        if (add) {
            if (P.getEdgeCount(Su, Sv) == 1) return;
            P.addEdge(Su, Sv);
            if (Su != Sv) P.addEdge(Sv, Su);
            for (int _u : getMembers(Su)) {
                for (int _v : getMembers(Sv)) {
                    if (_u == _v) continue;
                    if (Su == Sv && _u > _v) continue;
                    int e = Cp.getEdgeCount(_u, _v);
                    if (e == 0) {
                        // (_u, _v) is not in Cp
                        Cm.addEdge(_u, _v);
                        Cm.addEdge(_v, _u);
                    } else {
                        // (_u, _v) is in Cp
                        Cp.deleteEdge(_u, _v);
                        Cp.deleteEdge(_v, _u);
                    }
                }
            }
        } else {
            if (P.getEdgeCount(Su, Sv) == 0) return;
            P.deleteEdge(Su, Sv);
            if (Su != Sv) P.deleteEdge(Sv, Su);
            for (int _u : getMembers(Su)) {
                for (int _v : getMembers(Sv)) {
                    if (_u == _v) continue;
                    if (Su == Sv && _u > _v) continue;
                    int e = Cm.getEdgeCount(_u, _v);
                    if (e == 0) {
                        // (_u, _v) is not in Cm
                        Cp.addEdge(_u, _v);
                        Cp.addEdge(_v, _u);
                    } else {
                        // (_u, _v) is in Cm
                        Cm.deleteEdge(_u, _v);
                        Cm.deleteEdge(_v, _u);
                    }
                }
            }
        }
    }

    private long getDelta(final int R, final int S, Int2IntOpenHashMap edgeDelta){
        int rDelta = edgeDelta.get(R), sDelta = edgeDelta.get(S);

        // compute Delta of |P| + |Cp| + |Cm|
        long before = 0, after = 0;
        long edgeCount, newEdgeCount, pi, npi, cost, newCost;

        Int2IntOpenHashMap extendedNeighbors = new Int2IntOpenHashMap(getRawNeighbors(S));
        for(Int2IntMap.Entry U: getSupernodeNeighborsAndWeights(R)){
            extendedNeighbors.addTo(U.getIntKey(), 0);
            if(U.getIntKey() == S) continue;
            // compute delta between supernode U and supernode R
            edgeCount = U.getIntValue() / (U.getIntKey() == R ? 2 : 1);
            newEdgeCount = edgeCount - edgeDelta.getOrDefault(U.getIntKey(), 0);
            pi = getPi(R, U.getIntKey());
            npi = pi - getSize(U.getIntKey()) + (U.getIntKey() == R ? 1 : 0);
            cost = min(edgeCount, pi - edgeCount + 1);
            newCost = min(newEdgeCount, npi - newEdgeCount + 1);
            before += cost; after += newCost;
        }
        for(Int2IntMap.Entry U: extendedNeighbors.int2IntEntrySet()/*getSupernodeNeighborsAndWeights(S)*/){
            if(U.getIntKey() == R) continue;
            // compute delta between supernode U and supernode S
            edgeCount = U.getIntValue() / (U.getIntKey() == S ? 2 : 1);
            newEdgeCount = edgeCount + edgeDelta.getOrDefault(U.getIntKey(), 0);
            pi = getPi(S, U.getIntKey());
            npi = pi + getSize(U.getIntKey());
            cost = min(edgeCount, pi - edgeCount + 1);
            newCost = min(newEdgeCount, npi - newEdgeCount + 1);
            before += cost; after += newCost;
        }

        // compute delta between supernode R and supernode S
        edgeCount = getEdgeCount(R, S);
        newEdgeCount = edgeCount + (rDelta - sDelta);
        pi = getPi(R, S);
        npi = pi + getSize(R) - getSize(S) - 1;
        cost = min(edgeCount, pi - edgeCount + 1);
        newCost = min(newEdgeCount, npi - newEdgeCount + 1);
        before += cost; after += newCost;

        long delta = (after - before);
        return delta;
    }

    private void doNodalUpdate(final int v, final int R, final int S, IntArrayList Nv){
        // update edge info
        // remove every edge containing node v
        for(int u: Nv){
            updateEdge(v, u, false, false);
        }
        for(int U: P.getNeighbors(R)){
            for(int _u: getMembers(U)){
                if(v == _u) continue;
                Cm.deleteEdge(v, _u); Cm.deleteEdge(_u, v);
            }
        }
        // move subnode v from supernode R to supernode S
        moveNode(v, R, S);
        // restore every edge containing node v
        if (getSize(R) == 0 && sn.contains(R)) sn.remove(R);
        if (getSize(S) > 0) sn.add(S);

        for(int U: P.getNeighbors(S)){
            for(int _u: getMembers(U)){
                if(v == _u) continue;
                Cm.addEdge(v, _u); Cm.addEdge(_u, v);
            }
        }
        for(int u: Nv){
            updateEdge(v, u, true, false);
        }

        // update compressedData
        Int2IntOpenHashMap extendedNbrs_R = new Int2IntOpenHashMap(getRawNeighbors(R));
        Int2IntOpenHashMap extendedNbrs_S = new Int2IntOpenHashMap(getRawNeighbors(S));
        for(Int2IntMap.Entry U: getSupernodeNeighborsAndWeights(R)) {
            extendedNbrs_S.addTo(U.getIntKey(), 0);
        }
        for(int U: P.getNeighbors(R)) {
            extendedNbrs_R.addTo(U, 0);
        }

        long edgeCount, pi;
        for(Int2IntMap.Entry U: extendedNbrs_R.int2IntEntrySet()) {
            if(U.getIntKey() == S) continue;
            // update connection between supernode R and supernode U
            edgeCount = U.getIntValue() / (U.getIntKey() == R ? 2 : 1);
            pi = getPi(R, U.getIntKey());
            if(P.getEdgeCount(R, U.getIntKey()) > 0){
                if(edgeCount < pi - edgeCount + 1L) updateSuperedge(R, U.getIntKey(), false);
            } else {
                if(edgeCount > pi - edgeCount + 1L) updateSuperedge(R, U.getIntKey(), true);
            }
        }
        for(Int2IntMap.Entry U: extendedNbrs_S.int2IntEntrySet()){
            if(U.getIntKey() == R) continue;
            // update connection between supernode S and supernode U
            edgeCount = U.getIntValue() / (U.getIntKey() == S ? 2 : 1);
            pi = getPi(S, U.getIntKey());
            if(P.getEdgeCount(S, U.getIntKey()) > 0){
                if(edgeCount < pi - edgeCount + 1L) updateSuperedge(S, U.getIntKey(), false);
            } else {
                if(edgeCount > pi - edgeCount + 1L) updateSuperedge(S, U.getIntKey(), true);
            }
        }
        // update connection between supernode R and supernode S
        edgeCount = getEdgeCount(R, S);
        pi = getPi(R, S);
        if(P.getEdgeCount(R, S) > 0){
            if(edgeCount < pi - edgeCount + 1L) updateSuperedge(R, S, false);
        } else {
            if(edgeCount > pi - edgeCount + 1L) updateSuperedge(S, R, true);
        }
    }

    private long estimateSaving(final int v, Int2IntOpenHashMap edgeDelta, int S) {
        final int R = V.getInt(v);
        if (R == S) return 0;

        return getDelta(R, S, edgeDelta);
    }

    private void tryNodalUpdate(final int v){
        long save = 0;
        int bestSv = V.getInt(v);
        IntArrayList nbd = getNeighbors(v);
        Int2IntOpenHashMap edgeDelta = new Int2IntOpenHashMap();

        // nodes in nbd are affected by the update
        for(int u: nbd){
            int U = V.getInt(u);
            edgeDelta.addTo(U, 1);
        }

        // choose the target supernode greedily
        for (int S : sn) {
            if (S == -1) continue;
            long estimate = estimateSaving(v, edgeDelta, S);
            if (save > estimate) {
                bestSv = S;
                save = estimate;
            }
        }

        if (save > 0 || bestSv == V.getInt(v)) return;
        if (save <= 0) {
            // update only if we can save the cost
            costCounter += save;
            doNodalUpdate(v, V.getInt(v), bestSv, nbd);
        }
    }

    private void updateEdge(final int src, final int dst, final boolean add, final boolean check){
        super.processEdge(src, dst, add);
        final int SRC = V.getInt(src), DST = V.getInt(dst);
        if(add) {
            if (P.getNeighbors(SRC).contains(DST)) {
                if(check) costCounter -= 1;
                // remove the information from Cm
                Cm.deleteEdge(src, dst); Cm.deleteEdge(dst, src);
            } else {
                if(check) costCounter += 1;
                // add the information to Cp
                Cp.addEdge(src, dst); Cp.addEdge(dst, src);
                int edgeCount = getEdgeCount(SRC, DST);
                if(check && getPi(SRC, DST) + 1L < ((SRC != DST) ? 2L: 1L) * edgeCount){
                    // add new superedge (from SRC to DST) to P
                    costCounter -= (edgeCount / ((SRC == DST) ? 2 : 1));
                    costCounter += getPi(SRC, DST) + 1L - (edgeCount / ((SRC == DST) ? 2 : 1));
                    updateSuperedge(SRC, DST, true);
                }
            }
        } else {
            if (P.getNeighbors(SRC).contains(DST)) {
                if(check) costCounter += 1;
                // add the information to Cm
                Cm.addEdge(src, dst); Cm.addEdge(dst, src);
                int edgeCount = getEdgeCount(SRC, DST);
                if(check && getPi(SRC, DST) + 1L > ((SRC != DST) ? 2L: 1L) * edgeCount){
                    // remove superedge (from SRC to DST) from P
                    costCounter += edgeCount / ((SRC == DST) ? 2 : 1);
                    costCounter -= getPi(SRC, DST) + 1L - (edgeCount / ((SRC == DST) ? 2 : 1));
                    updateSuperedge(SRC, DST, false);
                }
            } else {
                if(check) costCounter -= 1;
                // remove the information from Cp
                Cp.deleteEdge(src, dst); Cp.deleteEdge(dst, src);
            }
        }
    }

    private void deactivateNode(final int v){
        int R = V.getInt(v);
        for(int U: P.getNeighbors(R)){
            for(int _u: getMembers(U)){
                if(v == _u) continue;
                Cm.deleteEdge(v, _u); Cm.deleteEdge(_u, v);
            }
        }
        moveNode(v, R, -1);

        // update compressedData
        Int2IntOpenHashMap extendedNbrs_R = new Int2IntOpenHashMap(getRawNeighbors(R));
        for(int U: P.getNeighbors(R)) {
            extendedNbrs_R.addTo(U, 0);
        }

        long edgeCount, pi;
        // update summary
        for(Int2IntMap.Entry U: extendedNbrs_R.int2IntEntrySet()) {
            edgeCount = U.getIntValue() / (U.getIntKey() == R ? 2 : 1);
            pi = getPi(R, U.getIntKey());
            costCounter -= min((pi + getSize(U.getIntKey())) - edgeCount + 1, edgeCount);
            costCounter += min(pi - edgeCount + 1, edgeCount);
            if(P.getEdgeCount(R, U.getIntKey()) > 0){
                if(edgeCount < pi - edgeCount + 1L){
                    updateSuperedge(R, U.getIntKey(), false);
                }
            } else {
                if(edgeCount > pi - edgeCount + 1L){
                    updateSuperedge(R, U.getIntKey(), true);
                }
            }
        }
    }

    @Override
    public void processEdge(final int src, final int dst, final boolean add) {
        iteration += 1;
        if(add){
            ecnt += 1;
        }else{
            ecnt -= 1;
        }
        // add node src in graph and create singleton supernode
        if(V.getInt(src) < 0){
            int to = newSupernode();
            moveNode(src,-1, to);
        }
        // add node dst in graph and create singleton supernode
        if(V.getInt(dst) < 0){
            int to = newSupernode();
            moveNode(dst,-1, to);
        }
        updateEdge(src, dst, add, true);

        if(getDegree(src) > 0){
            tryNodalUpdate(src);
        }else{
            // since node src is an isolated node
            deactivateNode(src);
        }
        if(getDegree(dst) > 0){
            tryNodalUpdate(dst);
        }else{
            // since node dst is an isolated node
            deactivateNode(dst);
        }
        if (iteration % interval == 0) {
            System.out.print(iteration);
            System.out.print(" : Elapsed time : " + (System.currentTimeMillis() - start) / 1000.);
            System.out.println(" : ratio : " + (costCounter / ((double)ecnt)));
        }
    }

    @Override
    public void processBatch(){
        System.out.println("Expected Compression Ratio: " + (costCounter / (double)ecnt));
    }
}
