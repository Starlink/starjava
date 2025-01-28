package uk.ac.starlink.ttools.moc;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import uk.ac.starlink.util.LongList;

/**
 * MocBuilder implementation based on IndexBags.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2025
 */
public class BagMocBuilder implements MocBuilder {

    private final int maxOrder_;
    private final LongFunction<IndexBag> bagFactory_;
    private final IndexBag[] bags_;

    /**
     * Constructor with default IndexBag creation policy.
     *
     * @param  maxOrder  maximum order stored by this MOC
     */
    public BagMocBuilder( int maxOrder ) {
        this( maxOrder, BagMocBuilder::createDefaultBag );
    }

    /**
     * Constructor with custom IndexBag creation policy.
     *
     * @param  maxOrder  maximum order stored by this MOC
     * @param  bagFactory  creates an IndexBag capable of storing non-negative
     *                     integers less than the supplied long value
     */
    public BagMocBuilder( int maxOrder, LongFunction<IndexBag> bagFactory ) {
        maxOrder_ = maxOrder;
        bagFactory_ = bagFactory;
        bags_ = new IndexBag[ maxOrder_ + 1 ];
    }

    public void addTile( int order, long ipix ) {
        if ( order > maxOrder_ ) {
            ipix = ipix >> 2 * ( order - maxOrder_ );
            order = maxOrder_;
        }
        if ( ipix >= 0 && ipix < 12L << 2 * order ) {
            getBag( order ).addIndex( ipix );
        }
        else {
            throw new IllegalArgumentException( "Order: " + order
                                              + ", pixel: " + ipix );
        }
    }

    public void endTiles() {
        consolidate();
    }

    public PrimitiveIterator.OfLong createOrderedUniqIterator() {
        int io = 0;
        return new PrimitiveIterator.OfLong() {
            int io_;
            boolean done_;
            PrimitiveIterator.OfLong oIt_;
            long nextUniq_;
            /* Constructor. */ {
                io_ = -1;
                oIt_ = LongStream.empty().iterator();
                calculateNext();
            }
            public boolean hasNext() {
                return !done_;
            }
            public long nextLong() {
                if ( hasNext() ) {
                    long nextUniq = nextUniq_;
                    calculateNext();
                    return nextUniq;
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            private void calculateNext() {
                while ( ! oIt_.hasNext() ) {
                    if ( io_ + 1 < bags_.length ) {
                        IndexBag bag = bags_[ ++io_ ];
                        oIt_ = bag == null ? LongStream.empty().iterator()
                                           : bag.sortedLongIterator();
                    }
                    else {
                        done_ = true;
                        return;
                    }
                }
                nextUniq_ = uniq( io_, oIt_.nextLong() );
            }
        };
    }

    public long[] getOrderCounts() {
        LongList countList = new LongList();
        for ( int io = maxOrder_; io >= 0; io-- ) {
            long count = bags_[ io ] == null ? 0 : bags_[ io ].getCount();
            if ( countList.size() > 0 || count > 0 ) {
                countList.add( count );
            }
        }
        int nc = countList.size();
        long[] counts = new long[ nc ];
        for ( int io = 0; io < nc; io++ ) {
            counts[ io ] = countList.get( nc - 1 - io );
        }
        return counts;
    }

    /**
     * Following a call to this method, the MOC is in normal form;
     * no sets of 4 tiles in the same quad, no part of the sphere
     * represented by more than one tile at different orders.
     * There is no change in the semantic content.
     *
     * <p>Calling this method during a run of tile additions may
     * result in a smaller memory footprint or better performance.
     * Or it may not.
     */
    public void consolidate() {
        IndexBag[] bags1 = new IndexBag[ maxOrder_ + 1 ];
        long[] quadMembers = new long[ 4 ];

        /* Work from highest to lowest order. */
        for ( int io = maxOrder_; io >= 0; io-- ) {
            IndexBag bag0 = bags_[ io ];
            if ( bag0 != null ) {
                IndexBag bag1 = createOrderBag( io );
                long iquad = -1;
                int quadCount = 0;

                /* Go through all pixels present at this order. */
                for ( PrimitiveIterator.OfLong ixIt =
                      bag0.sortedLongIterator(); ixIt.hasNext(); ) {
                    long index = ixIt.nextLong();
                    long iq = index >> 2;

                    /* Wait until we have had the opportunity to encounter all
                     * members of a quad, which corresponds to a single pixel
                     * at the next order down. */
                    if ( iq != iquad ) {

                        /* If the whole quad is already present at a lower
                         * order, do nothing. */
                        if ( quadCount > 0 && ! isCovered( io - 1, iquad ) ) {

                            /* Otherwise, if all 4 members are present,
                             * add the pixel to the next order down. */
                            if ( quadCount == 4 && io > 0 ) {
                                getBag( io - 1 ).addIndex( iquad );
                            }

                            /* If fewer than 4 members are present, retain
                             * them at this order. */
                            else {
                                for ( int i = 0; i < quadCount; i++ ) {
                                    bag1.addIndex( quadMembers[ i ] );
                                }
                            }
                        }
                        iquad = iq;
                        quadCount = 0;
                    }
                    quadMembers[ quadCount++ ] = index;
                }

                /* Make sure we deal with any members of the last quad
                 * encountered. */
                if ( quadCount > 0 && ! isCovered( io - 1, iquad ) ) {
                    if ( quadCount == 4 && io > 0 ) {
                        getBag( io - 1 ).addIndex( iquad );
                    }
                    else {
                        for ( int i = 0; i < quadCount; i++ ) {
                            bag1.addIndex( quadMembers[ i ] );
                        }
                    }
                }
                bags1[ io ] = bag1.getCount() == 0 ? null : bag1;
            }
        }
        System.arraycopy( bags1, 0, bags_, 0, maxOrder_ + 1 );
    }

    /**
     * Encodes an order and a tile index to a UNIQ value.
     * Input values are assumed to be legal.
     *
     * @param  order  order
     * @param  lindex   tile index within order
     * @return  uniq value
     */
    public static long uniq( int order, long lindex ) {
        return ( 4L << ( 2 * order ) ) + lindex;
    }

    /**
     * Factory method for IndexBag instances used to store pixels
     * at each order.
     *
     * @param  order  HEALPix order
     * @return  new IndexBag
     */
    private IndexBag createOrderBag( int order ) {
        assert order >= 0 && order <= maxOrder_;
        long size = 12L << 2 * order;
        return bagFactory_.apply( size );
    }

    /**
     * Returns a non-null bag for the given order.
     *
     * @param  order  HEALPix order, &lt;= maxOrder
     * @return   index bag, lazily constructed if necessary
     */
    private IndexBag getBag( int order ) {
        if ( bags_[ order ] == null ) {
            bags_[ order ] = createOrderBag( order );
        }
        return bags_[ order ];
    }

    /**
     * Indicates whether this MOC includes the area represented by a
     * given tile.
     *
     * @param  order  HEALPix order &lt;= maxOrder
     * @param  lindex  tile index within order
     */
    private boolean isCovered( int order, long lindex ) {
        for ( int io = order; io >= 0; io-- ) {
            if ( bags_[ io ] != null && bags_[ io ].hasIndex( lindex ) ) {
                return true;
            }
            lindex = lindex >> 2;
        }
        return false;
    }

    /**
     * Default bag creation policy.
     *
     * @param  size  bag size
     * @return   new IndexBag
     */
    private static IndexBag createDefaultBag( long size ) {
        int isize = (int) size;
        int bOrder = 12;
        if ( size <= 12L << 2 * bOrder ) {  // 24 Mb
            assert isize == size;
            return new BitSetBag( isize );
        }
        else {
            return new MultiBitSetBag( size );
        }
    }
}
