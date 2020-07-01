package mosso.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.concurrent.ThreadLocalRandom;

public class Int2IntHashMap extends Int2IntOpenHashMap {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Int2IntHashMap() {
        super(0);
    }
    public Int2IntHashMap(final int capacity) {
        super(capacity);
    }

    public int getRandomElement(){
        while(true) {
            int target = random.nextInt(mask + 2);
            if (target > mask && containsNullKey) return 0;
            else if (key[target] > 0) return key[target];
        }
    }

    @Override
    public int remove(final int k){
        final int ret = super.remove(k);
        if(n > minN && size < maxFill / 4) rehash(n / 2);
        return ret;
    }
}
