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
    private final boolean allowCdsImpl_;

    /** Maximum MOC order for BitSet use in adaptive modes. */
    public static final int BITSET_MAXORDER = 12;  // -> size = 24 Mb

    /** Size in bits corresponding to BITSET_MAXORDER. */
    private static final int BITSET_MAXSIZE =
        (int) ( 12L << 2 * BITSET_MAXORDER );

    /** Instance based on CDS MOC library. */
    public static final MocImpl CDS =
            new MocImpl( "cds", "Uses cds.moc classes", true ) {
        final int batchSize = 1;
        public MocBuilder createMocBuilder( int mocOrder ) {
            return CdsMocBuilder.createCdsSMocBuilder( mocOrder, batchSize );
        }
    };

    /** Instance based on CDS MOC library with batched adds. */
    public static final MocImpl CDS_BATCH =
            new MocImpl( "cds-batch", "Uses cds.moc classes with batching",
                         true ) {
        final int batchSize = 100_000;
        public MocBuilder createMocBuilder( int mocOrder ) {
            return CdsMocBuilder.createCdsSMocBuilder( mocOrder, batchSize );
        }
    };

    /** Instance based on IndexBags and BitSets. */
    public static final MocImpl BITSET =
            new MocImpl( "bits", "Uses BitSets", false ) {
        public MocBuilder createMocBuilder( int mocOrder ) {
            return new BagMocBuilder( mocOrder,
                                      s -> s <= BITSET_MAXSIZE
                                                ? new BitSetBag( (int) s )
                                                : new MultiBitSetBag( s ) );
        }
    };

    /** Instance based on IndexBags, BitSets and Int/LongBags. */
    public static final MocImpl LIST =
            new MocImpl( "lists", "Uses BitSets and lists", false ) {
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
            new MocImpl( "auto", "Chooses a suitable implementation", true ) {
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
     * @param  allowCdsImpl  iff true, use of CDS classes rather than methods
     *                       provided by this class is permitted
     *                       for building MOCs
     */
    protected MocImpl( String name, String description, boolean allowCdsImpl ) {
        name_ = name;
        description_ = description;
        allowCdsImpl_ = allowCdsImpl;
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

    /**
     * Indicates whether this will allow use of CDS classes.
     *
     * @return   if true, then using CDS classes to build a MOC
     *           with this choice of MocImpl is permitted;
     *           otherwise, such attempts should fail
     */
    public boolean allowCdsImplementation() {
        return allowCdsImpl_;
    }

    @Override
    public String toString() {
        return name_;
    }
}
