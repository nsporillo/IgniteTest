package net.porillo.runnables;

import net.porillo.Utility;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.transactions.Transaction;

public class HeavyRunnable implements Runnable{

    private final Ignite ignite;
    private final int numKeys, workLoad;
    private IgniteCache<AffinityKey<String>, String> cache;

    public HeavyRunnable(Ignite ignite, int numKeys, int workLoad) {
        this.ignite = ignite;
        this.numKeys = numKeys;
        this.workLoad = workLoad;
        this.cache = ignite.cache("test1");
    }

    @Override
    public void run() {
        while (true) {
            // Simulate some work before we get into the transaction
            Utility.hashToKillTime(workLoad);

            try (Transaction tx = ignite.transactions().txStart()) {
                for (int b = 0; b < numKeys; b++) {
                    String key = String.valueOf(b);
                    AffinityKey<String> affinityKey = new AffinityKey<>(key, key);
                    String test = cache.get(affinityKey);
                    if (test != null) {
                        // Simulating long running transaction is KEY!
                        test = Utility.hashToKillTime(workLoad);
                        cache.put(affinityKey, test);
                    }

                }

                tx.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Utility.hashToKillTime(workLoad);

            // tx under the hood
            IgniteAtomicLong test = ignite.atomicLong("test", 0, true);
            test.decrementAndGet();
        }
    }


}
