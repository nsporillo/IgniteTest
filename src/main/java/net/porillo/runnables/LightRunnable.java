package net.porillo.runnables;

import net.porillo.Utility;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.transactions.Transaction;

public class LightRunnable implements Runnable {

    private final Ignite ignite;
    private final int numKeys, workLoad;
    private IgniteCache<AffinityKey<String>, String> cache;

    public LightRunnable(Ignite ignite, int numKeys, int workLoad) {
        this.ignite = ignite;
        this.numKeys = numKeys;
        this.workLoad = workLoad;
        this.cache = ignite.cache("test1");
    }

    @Override
    public void run() {
        while (true) {
            Utility.hashToKillTime(workLoad);
            try (Transaction tx = ignite.transactions().txStart()) {
                for (int i = 0; i < numKeys; i++) {
                    String key = String.valueOf(i);
                    AffinityKey<String> affinityKey = new AffinityKey<>(key, key);
                    if (cache.get(affinityKey) == null) {
                        cache.put(affinityKey, "");
                    } else {
                        String value = cache.get(affinityKey);
                        cache.put(affinityKey, value + "a");
                    }
                }

                tx.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Simulate some work in between the double transaction
            Utility.hashToKillTime(workLoad);

            IgniteAtomicLong test = ignite.atomicLong("test", 0, true);
            test.incrementAndGet();
        }
    }
}
