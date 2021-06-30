package net.porillo;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import net.porillo.affinity.CustomAffinity;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.configuration.*;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.nio.charset.StandardCharsets;

public class Utility {

    private final static String SEED = "43e2293a-3db0-4cdc-88b9-87a687f46262";

    public static String hashToKillTime(int workload) {
        String hashedString = SEED;
        for (int i = 0; i < workload; i++) {
            HashCode hashCode = Hashing.murmur3_128().hashString(hashedString, StandardCharsets.UTF_8);
            hashedString = new String(hashCode.asBytes());
        }
        return hashedString;
    }

    public static void measureTime(int workload) {
        long start = -System.nanoTime();
        Utility.hashToKillTime(workload);
        long delta = (start + System.nanoTime());
        double deltaMillis = delta / 1_000_000.0;
        double hashsPerMilli = workload / deltaMillis;
        System.out.println("Took " + deltaMillis
                + " ms to do " + workload + " hashes. (" + String.format("%.3f", hashsPerMilli) + " h/ms)");
    }

    public static IgniteConfiguration getIgniteConfig(int txTimeout,
                                                      TransactionConcurrency concurrency,
                                                      TransactionIsolation isolation) {
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        AffinityFunction affinityFunction = new CustomAffinity();
        igniteConfiguration.setAtomicConfiguration(new AtomicConfiguration()
                .setCacheMode(CacheMode.PARTITIONED)
                .setBackups(1)
        );
        igniteConfiguration.setDataStorageConfiguration(new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(new DataRegionConfiguration()
                        .setName("test")
                        .setPersistenceEnabled(false)
                        .setMetricsEnabled(true)
                        .setInitialSize(256 * 1024L * 1024L)
                        .setMaxSize(1024 * 1024 * 1024L))
        );

        igniteConfiguration.setCacheConfiguration(new CacheConfiguration()
                .setName("test1")
                .setDataRegionName("test")
                .setStatisticsEnabled(true)
                .setWriteSynchronizationMode(CacheWriteSynchronizationMode.PRIMARY_SYNC)
                .setBackups(1)
                .setAffinity(affinityFunction)
                .setRebalanceMode(CacheRebalanceMode.SYNC)
                .setCacheMode(CacheMode.PARTITIONED)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
        );

        // Since Atomic Data structures leverage transactions under the hood, we know
        // that if we loosen consistency requirements for any cache, it can impact
        // the atomic long that we're assuming should be fully consistent!
        igniteConfiguration.setTransactionConfiguration(new TransactionConfiguration()
                .setDefaultTxConcurrency(concurrency)
                .setDefaultTxIsolation(isolation)
                .setDefaultTxTimeout(txTimeout)
                .setTxTimeoutOnPartitionMapExchange(txTimeout));

        return igniteConfiguration;
    }
}
