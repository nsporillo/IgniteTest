package net.porillo.runnables;

import net.porillo.Utility;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.transactions.TransactionMetrics;

public class ReportRunnable implements Runnable {

    private final Ignite ignite;
    private final long startTime;
    private final int workLoad;
    private IgniteCache<AffinityKey<String>, String> cache;

    public ReportRunnable(Ignite ignite, long startTime, int workLoad) {
        this.ignite = ignite;
        this.startTime = startTime;
        this.workLoad = workLoad;
        this.cache = ignite.cache("test1");
    }

    @Override
    public void run() {
        TransactionMetrics txMetrics = ignite.transactions().metrics();
        CacheMetrics cacheMetrics = cache.metrics();

        double secondsAlive = (System.currentTimeMillis() - startTime) / 1000.0;
        double tps = cacheMetrics.getCacheTxCommits() / secondsAlive;
        double txtime = cacheMetrics.getAverageTxCommitTime() / 1000.0;

        System.out.println("--------------------------------------");
        System.out.println("Running for " + (int) secondsAlive + " seconds.");
        System.out.println("TPS: " + String.format("%.3f", tps));
        System.out.println("Avg Tx Time: " + String.format("%.3f", txtime) + " ms");
        System.out.println("Tx Commited: " + txMetrics.getTransactionsCommittedNumber());
        System.out.println("Holding locks for: " + txMetrics.getTransactionsHoldingLockNumber()
                + " tx. Total locked keys: " + txMetrics.getLockedKeysNumber());
        Utility.measureTime(workLoad); // print out roughly how long its taking for the workload
        // the test value should roughly be 0 since we typically have equal number of ++ and --
        System.out.println("Atomic Value: " + ignite.atomicLong("test", 0, true).get());
        System.out.println("--------------------------------------");
    }
}
