package net.porillo.affinity;

import com.google.common.hash.Hashing;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;

import java.nio.charset.StandardCharsets;

public class CustomAffinity extends RendezvousAffinityFunction {

    public CustomAffinity() {
        super(true, 2047);
    }

    @Override
    public int partition(Object key) {
        int keyHash = Hashing.murmur3_32().hashString(key.toString(), StandardCharsets.UTF_8).asInt();
        return Math.abs(keyHash % super.getPartitions());
    }
}
