package uk.ac.starlink.ttools.moc;

/**
 * Characterises an implementation of MOC building.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2025
 */
public abstract class MocImpl {

    private final String name_;
    private final String description_;

    /** Maximum MOC order for BitSet use in adaptive modes. */
    public static final int BITSET_MAXORDER = 12;  // -> size = 24 Mb

    /** Size in bits corresponding to BITSET_MAXORDER. */
    private static final int BITSET_MAXSIZE =
        (int) ( 12L << 2 * BITSET_MAXORDER );

    /** Instance based on CDS MOC library. */
    public static final MocImpl CDS =
            new MocImpl( "cds", "Uses CDS SMoc class" ) {
        final int batchSize = 1; // 100_000;
        public MocBuilder createMocBuilder( int mocOrder ) {
            return CdsMocBuilder.createCdsMocBuilder( mocOrder, batchSize );
        }
    };

    /** Instance based on IndexBags and BitSets. */
    public static final MocImpl BITSET =
            new MocImpl( "bits", "Uses BitSets" ) {
        public MocBuilder createMocBuilder( int mocOrder ) {
            return new BagMocBuilder( mocOrder,
                                      s -> s <= BITSET_MAXSIZE
                                                ? new BitSetBag( (int) s )
                                                : new MultiBitSetBag( s ) );
        }
    };

    /** Instance based on IndexBags, BitSets and Int/LongBags. */
    public static final MocImpl LIST =
            new MocImpl( "lists", "Uses BitSets and lists" ) {
        public MocBuilder createMocBuilder( int mocOrder ) {
            return new BagMocBuilder( mocOrder,
                                      s -> {
                                          if ( s < BITSET_MAXSIZE ) {
                                              return new BitSetBag( (int) s );
                                          }
                                          else if ( s < Integer.MAX_VALUE ) {
                                              return new IntegerBag();
                                          }
                                          else {
                                              return new LongBag();
                                          }
                                      } );
        }
    };

    /** Instance that picks an implementation based on order. */
    public static final MocImpl AUTO =
            new MocImpl( "auto", "Chooses implementation based on order" ) {
        public MocBuilder createMocBuilder( int mocOrder ) {
            return ( mocOrder <= BITSET_MAXORDER ? BITSET : CDS )
                  .createMocBuilder( mocOrder );
        }
    };

    /**
     * Constructor.
     *
     * @param  name  implementation name
     * @param  description  implementation description
     */
    protected MocImpl( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Returns a new MocBuilder for a given maximum order.
     *
     * @param  mocOrder  maximum order of resulting MOCs
     */
    public abstract MocBuilder createMocBuilder( int mocOrder );

    /**
     * Returns this instance's name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short description of this instance.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }
}
