package mosso.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.concurrent.ThreadLocalRandom;

public class IntHashSet extends IntOpenHashSet {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public IntHashSet(){
        super(0);
    }

    public IntHashSet(int[] list){
        super(list);
    }

    public int getRandomElement(){
        while(true) {
            int target = random.nextInt(mask + 2);
            if (target > mask && containsNull) return 0;
            else if (key[target] > 0) return key[target];
        }
    }

    @Override
    public boolean remove(final int k){
        final boolean ret = super.remove(k);
        if(n > minN && size < maxFill / 4) rehash(n / 2);
        return ret;
    }
}
