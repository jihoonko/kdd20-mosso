package mosso;

import mosso.algorithm.*;

import java.util.Arrays;
import java.io.IOException;
import java.util.Date;

public class Run {

    private SummaryGraphModule module;

    public static void main(String[] args) throws IOException {
        Date today = new Date();
        // System.out.println(Arrays.toString(args));
        System.out.println(today);
        final String inputPath = args[0];
        System.out.println("input_path: " + inputPath);
        final String outputPath = args[1];
        System.out.println("output_path: " + outputPath);
        final String sumMode = args[2];
        System.out.println("summarization_mode: " + sumMode);
        System.out.println();

        final SummaryGraphModule module;        
        if (sumMode.compareTo("mosso") == 0) {
            System.out.println("0.3");
            final int probability = Integer.parseInt("0.3");
            //System.out.println(probability);
            final int n_samples = Integer.parseInt(args[4]);
            final int interval = Integer.parseInt(args[5]);
            System.out.println("escape probability : " + probability + ", n_samples : " + n_samples + ", TT : " + interval);
            module = new MoSSo(false, probability, n_samples, interval);
        } else if (sumMode.compareTo("simple") == 0) {
            final int probability = Integer.parseInt(args[3]);
            final int n_samples = Integer.parseInt(args[4]);
            final int interval = Integer.parseInt(args[5]);
            System.out.println("escape probability : " + probability + ", n_samples : " + n_samples + ", TT : " + interval);
            module = new MoSSoSimple(false, probability, n_samples, interval);
        } else if (sumMode.compareTo("mcmc") == 0) {
            final int interval = Integer.parseInt("1000");
            System.out.println("interval : " + interval);
            module = new MoSSoMCMC(false, interval);
        } else if(sumMode.compareTo("sgreedy") == 0) {
            // final int interval = Integer.parseInt(args[3]);
            final int interval = Integer.parseInt("1000");
            System.out.println("interval : " + interval);
            module = new MoSSoGreedy(false, interval);
        } else {
            System.out.println("Invalid command.");
            return;
        }
        int edgeCount = Common.execute(module, inputPath, "\t");
        Common.writeOutputs(module, "output/" + outputPath, edgeCount);
    }
}
