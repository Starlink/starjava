package uk.ac.starlink.array;

import java.util.Random;

/**
 * Like a ChunkStepper, but each chunk is of a random size.
 */
class RandomChunkStepper {

    private static int defaultSize = 2048;
    private final long leng;
    private final int maxsize;
    private final Random rand; 

    private long off = 0L;
    private int size = 0;

    public RandomChunkStepper( long leng ) {
        this.leng = leng;
        this.rand = new Random( (int) size * 23 + 1 );
        maxsize = (int) Math.min( (long) defaultSize, leng / 5 + 2 );
        next();
    }

    public void next() {
        off += size;
        size = (int) Math.min( (long) rand.nextInt( maxsize ), leng - off );
    }

    public long getBase() {
        return off;
    }

    public int getSize() {
        return size;
    }

    public boolean hasNext()  {
        return off < leng;
    }
    
}
