package uk.ac.starlink.array;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Like a ChunkStepper, but each chunk is of a random size, and the chunks
 * are not dispensed in any particular order (i.e. they are not expected
 * to progress in sequence from the start to the end of the array).
 */
class RandomChunkDispenser {

    private final Iterator chunkIt;
    private long base;
    private int size;

    public RandomChunkDispenser( long leng ) {
        List chunks = new ArrayList();
        for ( RandomChunkStepper cit = new RandomChunkStepper( leng );
              cit.hasNext(); cit.next() ) {
            chunks.add( new long[] { cit.getBase(), (long) cit.getSize() } );
        }
        Random rand = new Random( 99 * (int) leng + 5 );
        Collections.shuffle( chunks, rand );
        chunkIt = chunks.iterator();
        next();
    }

    public void next() {
        long[] item = (long[]) chunkIt.next();
        base = item[ 0 ];
        size = (int) item[ 1 ];
    }

    public long getBase() {
        return base;
    }

    public int getSize() {
        return size;
    }

    public boolean hasNext()  {
        return chunkIt.hasNext();
    }
    
}
