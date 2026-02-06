package uk.ac.starlink.ttools.moc;

import cds.moc.Moc;
import cds.moc.Moc1D;
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
    final SMoc smoc_;

    /**
     * Constructor.
     *
     * @param   maxOrder  maximum order stored by this MOC
     */
    protected CdsMocBuilder( int maxOrder ) {
        maxOrder_ = maxOrder;
        smoc_ = new SMoc( maxOrder );
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
                        addArrayAtOrder( smoc_, order, buf, ibs_[ order ] );
                        ibs_[ order ] = 0;
                    }
                }
                public void endTiles() {
                    for ( int io = 0; io <= maxOrder; io++ ) {
                        long[] buf = bufs_[ io ];
                        if ( buf != null ) {
                            addArrayAtOrder( smoc_, io, buf, ibs_[ io ] );
                        }
                    }
                    smoc_.bufferOff();
                }
            };
        }
    }

    /**
     * Fast addition of multiple ids at a single order to a MOC.
     *
     * <p>This is adapted from some code supplied by Pierre Fernique
     * that was in the form of an additional method on Moc1D,
     * by email on 23 Jan 2025.  Written with reference to the source
     * code of the cds.moc lib version 6.31.
     *
     * @param   moc   moc to modify
     * @param   order  order of all supplied ids
     * @param   values   array of ids to add; this may be overwritten
     * @param   nval   number of ids present in values array
     */
    private static void addArrayAtOrder( Moc1D moc, int order, long[] values,
                                         int nval ) {
        if ( nval < values.length ) {
            long[] values1 = new long[ nval ];
            System.arraycopy( values, 0, values1, 0, nval );
            values = values1;
        }
        Arrays.sort( values );
        int shift = ( moc.maxOrder() - order ) * moc.shiftOrder();
        Range r = new Range( nval );
        for ( int i = 0; i < nval; i++ ) {
            int j = i;
            while ( j < nval - 1 && values[ j + 1 ] - values[ j ] <= 1 ) {
                j++;
            }
            long start = values[ i ] << shift;
            long end = ( values[ j ] + 1L ) << shift;
            r.append( start, end );
        }
        moc.setRangeList( moc.seeRangeList().union( r ) );
    }
}
