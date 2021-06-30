package net.porillo;

import org.junit.jupiter.api.Test;

// Use to determine what value for workload to use for the testing
// is entirely system dependent (processor, L3 cache, memory speed, etc)
//
public class UtilityTest {

    @Test
    public void testWork() {
        Utility.hashToKillTime(10000); //warmup
        int[] workloads = new int[]{1, 10, 100, 500, 600, 650, 700, 750, 1000, 1500, 2000, 5000, 10000};
        for (int w : workloads) {
            measureTime(w);
        }
    }

    public void measureTime(int workload) {
        long start = -System.nanoTime();
        Utility.hashToKillTime(workload);
        long delta = (start + System.nanoTime());
        double deltaMillis = delta / 1_000_000.0;
        double hashsPerMilli = workload / deltaMillis;
        System.out.println("Took " + deltaMillis
                + " ms to do " + workload + " hashes. (" + hashsPerMilli + " h/ms)");
    }
}
