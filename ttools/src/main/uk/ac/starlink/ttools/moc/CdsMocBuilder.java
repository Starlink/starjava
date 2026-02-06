package uk.ac.starlink.ttools.moc;

import cds.moc.Moc;
import cds.moc.MocCell;
import cds.moc.Range;
import cds.moc.SMoc;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import uk.ac.starlink.ttools.func.Coverage;

/**
 * MocBuilder implementation based on the CDS SMoc class.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2025
 */
public abstract class CdsMocBuilder implements MocBuilder {

    private final int maxOrder_;
    final SMoc1 smoc_;

    /**
     * Constructor.
     *
     * @param   maxOrder  maximum order stored by this MOC
     */
    protected CdsMocBuilder( int maxOrder ) {
        maxOrder_ = maxOrder;
        smoc_ = new SMoc1( maxOrder );
    }

    public PrimitiveIterator.OfLong createOrderedUniqIterator() {
        Iterator<MocCell> cellIt = smoc_.iterator();
        return new PrimitiveIterator.OfLong() {
            public boolean hasNext() {
                return cellIt.hasNext();
            }
            public long nextLong() {
                MocCell cell = cellIt.next();
                return Moc.hpix2uniq( cell.order, cell.start );
            }
        };
    }

    public long[] getOrderCounts() {
        long[] counts = new long[ maxOrder_ + 1 ];
        int maxOrd = 0;
        for ( PrimitiveIterator.OfLong uniqIt = createOrderedUniqIterator();
              uniqIt.hasNext(); ) {
            long uniq = uniqIt.nextLong();
            int order = Coverage.uniqToOrder( uniq );
            maxOrd = Math.max( maxOrd, order );
            counts[ order ]++;
        }
        long[] counts1 = new long[ maxOrd + 1 ];
        System.arraycopy( counts, 0, counts1, 0, maxOrd + 1 );
        return counts1;
    }

    /**
     * Returns an instance of this class that may or may not use
     * tile batching to affect performance.
     *
     * @param  maxOrder  maximum HEALPix order of the MOC
     * @param  batchSize  if &gt;1, pixels are added in batches per order,
     *                    which may result in improved performance
     */
    public static MocBuilder createCdsMocBuilder( int maxOrder,
                                                  int batchSize ) {
        if ( batchSize == 1 ) {
            return new CdsMocBuilder( maxOrder ) {
                /* Constructor. */ {
                    // In (some) tests this value seems about optimal.
                    smoc_.bufferOn( 500_000 );
                }
                public void addTile( int order, long ipix ) {
                    try {
                        smoc_.add( order, ipix );
                    }
                    catch ( Exception e ) {
                        throw new RuntimeException( "CDS MOC error", e );
                    }
                }
                public void endTiles() {
                    smoc_.bufferOff();
                }
            };
        }
        else {
            return new CdsMocBuilder( maxOrder ) {
                final int bufsiz_;
                final long[][] bufs_;
                final int[] ibs_;
                /* Constructor. */ {
                    bufsiz_ = batchSize;
                    bufs_ = new long[ maxOrder + 1 ][];
                    ibs_ = new int[ maxOrder + 1 ];
                    // I don't know if buffering helps here or not,
                    // but it's probably harmless.
                    smoc_.bufferOn( 500_000 );
                }
                public void addTile( int order, long ipix ) {
                    if ( bufs_[ order ] == null ) {
                        bufs_[ order ] = new long[ bufsiz_ ];
                    }
                    long[] buf = bufs_[ order ];
                    buf[ ibs_[ order ]++ ] = ipix;
                    if ( ibs_[ order ] >= bufsiz_ ) {
                        smoc_.add( order, buf, ibs_[ order ] );
                        ibs_[ order ] = 0;
                    }
                }
                public void endTiles() {
                    for ( int io = 0; io <= maxOrder; io++ ) {
                        long[] buf = bufs_[ io ];
                        if ( buf != null ) {
                            smoc_.add( io, buf, ibs_[ io ] );
                        }
                    }
                    smoc_.bufferOff();
                }
            };
        }
    }

    /**
     * Extends SMoc with a fast multiple addition method
     * supplied by Pierre by email 23 Jan 2025.
     */
    private static class SMoc1 extends SMoc {

        /**
         * Constructor.
         *
         * @param  order  maximum order of MOC
         */
        SMoc1( int order ) {
            super( order );
        }

        /**
         * Fast addition of a list of singletons expressed
         * at the specified order
         * These singletons do not need to be sorted, nor to be unique.
         *
         * @param order singleton order
         * @param singletons list of singletons
         * @param size number of singletons
         */
        public void add( int order, long [] singletons, int size) {
            Arrays.sort(singletons,0,size);
            int shift = (maxOrder()-order) * shiftOrder();
            Range r = new Range(size);
            int j;
            for( int i=0; i<size; i++ ) {
                for( j=i; j<size-1 && (singletons[j+1]-singletons[j])<=1; j++ );
                long start = (singletons[i]) << shift;
                long end = (singletons[j]+1L) << shift;
                r.append(start,end);
            }
            range = range.union(r);
            resetCache();
        }
    }
}
