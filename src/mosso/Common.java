package mosso;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class Common {

    public Common(){}

    private static int[] parseEdge(String line, String delim){
        String[] tokens = line.split(delim);
        try {
            int src = Integer.valueOf(tokens[0]);
            int dst = Integer.valueOf(tokens[1]);
            int add = Integer.valueOf(tokens[2]);
            return new int[]{src, dst, add};
        }catch(Exception e){
            return null;
        }
    }

    public static int execute(final SummaryGraphModule module, final String inputPath, final String delim) throws IOException{
        int count = 0;
        long start = System.currentTimeMillis();
        BufferedReader br = new BufferedReader(new FileReader(inputPath));

        while(true){
            final String line = br.readLine();
            if(line == null) break;
            final int[] edge = parseEdge(line, delim);
            if(edge == null) break;
            count += edge[2];
            if(edge[2] > 0) module.processAddition(edge[0], edge[1]);
            else module.processDeletion(edge[0], edge[1]);
        }
        module.processBatch();

        br.close();
        long end = System.currentTimeMillis();
        System.out.println("Execution time: " + (end - start) / 1000.0 + "s.");
        return count;
    }

    public static void writeOutputs(final SummaryGraphModule module, final String outputPath, final int edgeCount) throws IOException{
        /*
        Output:
        line 1: |V|, |E|
        line 2 ~ |V|+1: (real_index, membership(block) info)
        line |V|+2 ~ |V|+|P|+1: superedges in P
        line |V|+|P|+2: -1 -1
        line |V|+|P|+3 ~ |V|+|P|+|Cp|+2: edges in Cp
        line |V|+|P|+|Cp|+3: -1 -1
        line |V|+|P|+|Cp|+4 ~ |V|+|P|+|Cp|+|Cm|+3: edges in Cm
        line |V|+|P|+|Cp|+|Cm|+4: -1 -1
        */
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
        Tuple<IntArrayList, AdjacencyList[], IntArrayList> compressedResult = module.getCompressedResult();
        int n = compressedResult.third.size();
        int[] summaryCount = {0, 0, 0};
        List<String> listNames = Arrays.asList("P", "C_plus", "C_minus");
        ListIterator<String> it = listNames.listIterator();
        IntArrayList realIdxs = compressedResult.third;

        // line 1
        bw.write(n + "\t" + edgeCount + "\n");

        // line 2 ~ |V|+1
        for(int i=0;i<n;i++){
            bw.write(realIdxs.getInt(i) + "\t" + compressedResult.first.getInt(i) + "\n");
        }

        while(it.hasNext()){
            int idx = it.nextIndex();
            AdjacencyList target = compressedResult.second[idx];
            it.next();
            for(int i=0;i<n;i++){
                for(int v: target.getNeighbors(i)){
                    if(i <= v){
                        bw.write(i + "\t" + v + "\n");
                        summaryCount[idx]++;
                    }
                }
            }
            bw.write("-1\t-1\n");
        }
        System.out.println("SUMMARY:");
        for(int i=0;i<3;i++){
            System.out.println("|" + listNames.get(i) + "|: " + summaryCount[i]);
        }
        int totalCount = (summaryCount[0] + summaryCount[1] + summaryCount[2]);
        System.out.println("total: " + totalCount);
        System.out.println("compression_rate: " + totalCount / (double) edgeCount);
        System.out.println();
        bw.close();
    }
}
