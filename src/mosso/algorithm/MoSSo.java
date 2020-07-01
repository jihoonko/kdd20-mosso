package mosso.algorithm;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import jdk.jshell.spi.ExecutionControl;
import mosso.SupernodeHelper;
import static java.lang.Long.min;

public class MoSSo extends SupernodeHelper {
    private final int INF = 0x7FFFFFFF;
    private int iteration = 0;
    private int escape;
    private int n_hash;
    private int sampleNumber;
    private long start;

    private IntArrayList[] minHash;
    private IntArrayList[] hf;

    private long costCounter = 0;
    private int ecnt = 0;
    private int interval;

    public MoSSo(boolean directed, final int _escape, final int _sample, final int _interval){
        super(directed);
        if(directed){
            try {
                throw new ExecutionControl.NotImplementedException("Directed version is NOT_IMPLEMENTED");
            } catch (ExecutionControl.NotImplementedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        escape = _escape;
        n_hash = 4;
        sampleNumber = _sample;
        interval = _interval;
        start = System.currentTimeMillis();
        hash_initialization();
    }

    private void hash_initialization(){
        minHash = new IntArrayList[n_hash];
        hf = new IntArrayList[n_hash];
        for(int i = 0; i< n_hash; i++){
            minHash[i] = new IntArrayList();
            hf[i] = new IntArrayList();
        }
    }

    @Override
    public void addVertex(int idx) {
        super.addVertex(idx);
        for (int i = 0; i< n_hash; i++){
            minHash[i].add(INF);
            hf[i].add(randInt(1, 0x7FFFFFFE));
        }
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

    private long getDelta(final int R, final int S, IntArrayList Nv, Int2IntOpenHashMap edgeDelta){
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
        for(Int2IntMap.Entry U: extendedNeighbors.int2IntEntrySet()){
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
        // update edge information
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
        for(int U: getSupernodeNeighbors(R)) {
            extendedNbrs_S.addTo(U, 0);
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

    private boolean tryNodalUpdate(final int v, int _S){
        final int R = V.getInt(v);
        int S = _S;
        if (R == _S) {
            return false;
        }

        IntArrayList Nv = getNeighbors(v);
        Int2IntOpenHashMap edgeDelta = new Int2IntOpenHashMap();

        // nodes in Nv are affected by the update
        for(int u: Nv){
            int U = V.getInt(u);
            edgeDelta.addTo(U, 1);
        }

        long delta = getDelta(R, S, Nv, edgeDelta);
        double acceptanceRatio;

        if (delta <= 0) acceptanceRatio = 1.;
        else acceptanceRatio = 0.;

        if(randDouble() <= acceptanceRatio){
            costCounter += delta;
            doNodalUpdate(v, R, S, Nv);
            return true;
        } else {
            return false;
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

    private void updateHash(final int src, final int dst, final boolean add) {
        if(add){
            for (int i = 0; i< n_hash; i++){
                int sh = hf[i].getInt(src), dh = hf[i].getInt(dst);
                // update minhash
                if (minHash[i].getInt(src) > dh) minHash[i].set(src, dh);
                if (minHash[i].getInt(dst) > sh) minHash[i].set(dst, sh);
            }
        }else{
            for(int i = 0; i< n_hash; i++){
                long sh = hf[i].getInt(src), dh = hf[i].getInt(dst);
                if(minHash[i].getInt(src) == dh){
                    // update minhash using neighbors of src
                    minHash[i].set(src, INF);
                    for(int nbd: getNeighbors(src)){
                        int nh = hf[i].getInt(nbd);
                        if(minHash[i].getInt(src) > nh) minHash[i].set(src, nh);
                    }
                }
                if(minHash[i].getInt(dst) == sh){
                    // update minhash using neighbors of dst
                    minHash[i].set(dst, INF);
                    for(int nbd: getNeighbors(dst)){
                        int nh = hf[i].getInt(nbd);
                        if(minHash[i].getInt(dst) > nh) minHash[i].set(dst, nh);
                    }
                }
            }
        }
    }


    private void _processEdge(final int dst, IntArrayList srcnbd, final int which) {
        Long2ObjectOpenHashMap<IntArrayList> srcGrp = new Long2ObjectOpenHashMap<>();
        if(getDegree(dst) > 0) srcnbd.set(0, dst);
        // coarse clustering using minhash
        for (int v : srcnbd) {
            long target = minHash[which].getInt(v);
            if (!srcGrp.containsKey(target)) srcGrp.put(target, new IntArrayList());
            srcGrp.get(target).add(v);
        }
        for (int i = 0; i < sampleNumber; i++) {
            int nbd = srcnbd.getInt(i);
            if (randInt(1, getDegree(nbd)) <= 1) {
                long mh = minHash[which].getInt(nbd);
                int sz = srcGrp.get(mh).size();
                // choose random node in the cluster containing nbd
                int target = srcGrp.get(mh).getInt(randInt(0, sz - 1));
                if (randInt(1, 10) > escape || iteration < 1000) {
                    tryNodalUpdate(nbd, V.getInt(target));
                } else {
                    // only if the supernode containing nbd is not singleton
                    if(getSize(V.getInt(nbd)) > 1) tryNodalUpdate(nbd, newSupernode());
                }
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
        updateHash(src, dst, add);
        int which = randInt(0, n_hash-1);
        if(getDegree(src) > 0){
            IntArrayList srcnbd = getRandomNeighbors(src, sampleNumber);
            _processEdge(dst, srcnbd, which);
        }else{
            // since node src is an isolated node
            deactivateNode(src);
        }
        if(getDegree(dst) > 0){
            IntArrayList dstnbd = getRandomNeighbors(dst, sampleNumber);
            _processEdge(src, dstnbd, which);
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
