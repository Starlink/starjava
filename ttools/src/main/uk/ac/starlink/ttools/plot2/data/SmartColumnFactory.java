package uk.ac.starlink.ttools.plot2.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * CachedColumnFactory implementation that is able to spot non-varying
 * columns and represent them efficiently.
 * It delegates to a supplied CachedColumnFactory instance that actually
 * performs the storage.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2013
 */
public class SmartColumnFactory implements CachedColumnFactory {

    private final CachedColumnFactory bulkColumnFactory_;
    private final CachedColumnFactory singleColumnFactory_;
    private static final Map<StorageType,ValueComparison> comparisonMap_ =
        createComparisonMap();

    /**
     * Constructor.
     *
     * @param  bulkColumnFactory   column factory that does the work of
     *         storing varying columns
     */
    public SmartColumnFactory( CachedColumnFactory bulkColumnFactory ) {
        bulkColumnFactory_ = bulkColumnFactory;
        singleColumnFactory_ = new MemoryColumnFactory();
    }

    public CachedColumn createColumn( StorageType type, long nrow ) {
        return new SmartCachedColumn( type, nrow );
    }

    /**
     * Creates a map by StorageType of ValueComparison objects.
     *
     * @return   value comparison object map
     */
    private static Map<StorageType,ValueComparison> createComparisonMap() {
        Map<StorageType,ValueComparison> map =
            new HashMap<StorageType,ValueComparison>();

        /* Set up a comparison object that works for immutable objects.
         * Float and double NaN values should compare as equal.
         * This implementation does that, because the Float/Double wrapper
         * objects compare NaNs equal, unlike the primitives. */
        ValueComparison immutableComparison = new ValueComparison() {
            public boolean equalValues( Object v1, Object v2 ) {

                /* v1 and v2 are known not to be null. */
                return v1.equals( v2 );
            }
            public Object copyValue( Object v ) {
                return v;
            }
        };
        map.put( StorageType.BOOLEAN, immutableComparison );
        map.put( StorageType.DOUBLE, immutableComparison );
        map.put( StorageType.FLOAT, immutableComparison );
        map.put( StorageType.INT, immutableComparison );
        map.put( StorageType.SHORT, immutableComparison );
        map.put( StorageType.BYTE, immutableComparison );
        map.put( StorageType.STRING, immutableComparison );
        map.put( StorageType.INT3, new ValueComparison() {
            public boolean equalValues( Object v1, Object v2 ) {
                return Arrays.equals( (int[]) v1, (int[]) v2 );
            }
            public Object copyValue( Object v ) {
                return ((int[]) v).clone();
            }
        } );
        map.put( StorageType.DOUBLE3, new ValueComparison() {
            public boolean equalValues( Object v1, Object v2 ) {
                return Arrays.equals( (double[]) v1, (double[]) v2 );
            }
            public Object copyValue( Object v ) {
                return ((double[]) v).clone();
            }
        } );
        map.put( StorageType.FLOAT3, new ValueComparison() {
            public boolean equalValues( Object v1, Object v2 ) {
                return Arrays.equals( (float[]) v1, (float[]) v2 );
            }
            public Object copyValue( Object v ) {
                return ((float[]) v).clone();
            }
        } );
        map.put( StorageType.DOUBLE_ARRAY, new ValueComparison() {
            public boolean equalValues( Object v1, Object v2 ) {
                return Arrays.equals( (double[]) v1, (double[]) v2 );
            }
            public Object copyValue( Object v ) {
                return ((double[]) v).clone();
            }
        } );
        map.put( StorageType.FLOAT_ARRAY, new ValueComparison() {
            public boolean equalValues( Object v1, Object v2 ) {
                return Arrays.equals( (float[]) v1, (float[]) v2 );
            }
            public Object copyValue( Object v ) {
                return ((float[]) v).clone();
            }
        } );
        assert map.keySet()
                  .containsAll( Arrays.asList( StorageType.values() ) );
        return Collections.unmodifiableMap( map );
    }

    /**
     * CachedColumn instance that watches added values, and as long as they
     * keep being the same, just stores a single value.
     * As soon as at least one different value in the column is spotted,
     * it acquires a column from the bulk factory and uses that instead.
     */
    private class SmartCachedColumn implements CachedColumn {
        private final StorageType type_;
        private final long nrow_;
        private final ValueComparison comparison_;
        private CachedColumn constCol_;
        private CachedColumn bulkCol_;
        private Object value1_;
        private long constCount_;

        /**
         * Constructor.
         *
         * @param  type  storage type
         * @param   nrow  number of rows, negative means unknown
         */
        SmartCachedColumn( StorageType type, long nrow ) {
            type_ = type;
            nrow_ = nrow;
            comparison_ = comparisonMap_.get( type );
            constCol_ = singleColumnFactory_.createColumn( type, 1 );
        }

        public void add( Object value ) {

            /* Have already had varying values.  Delegate to a column capable
             * of storing varying values. */
            if ( bulkCol_ != null ) {
                bulkCol_.add( value );
            }

            /* Haven't had any values yet.  Initialise a single-entry column
             * with the first value. */
            else if ( constCount_ == 0 ) {
                assert value1_ == null;
                value1_ = comparison_.copyValue( value );
                constCol_.add( value );
                constCount_++;
            }

            /* Have had values all equal to constant. */
            else {

                /* It's another equal to constant.  Just bump the number of
                 * constant values seen. */
                if ( comparison_.equalValues( value1_, value ) ) {
                    constCount_++;
                }

                /* This one's different - revert to non-constant behaviour.
                 * Acquire a normal varying column, populate its first few
                 * entries with the initial value, and prepare for use with
                 * varying values. */
                else {
                    constCol_ = null;
                    bulkCol_ = bulkColumnFactory_.createColumn( type_, nrow_ );
                    for ( int i = 0; i < constCount_; i++ ) {
                        bulkCol_.add( value1_ );
                    }
                    bulkCol_.add( value );
                }
            }
        }

        public void endAdd() {
            ( bulkCol_ != null ? bulkCol_ : constCol_ ).endAdd();
            assert constCol_ == null || nrow_ < 0 || constCount_ == nrow_;
        }

        public CachedSequence createSequence() {

            /* Return a sequence based on a varying or non-varying underlying
             * column as appropriate. */
            return bulkCol_ != null
                 ? bulkCol_.createSequence()
                 : new ConstantSequence( constCol_, constCount_ );
        }
    }

    /**
     * Presents a single-entry sequence as a multi-entry sequence in which
     * all the values are the same.
     */
    private static class ConstantSequence implements CachedSequence {
        private final boolean booleanValue_;
        private final int intValue_;
        private final double doubleValue_;
        private final Object objectValue_;
        private final long nrow_;
        private long irow_;

        /**
         * Constructor.
         *
         * @param  col1  single-entry column
         * @param  nrow  number of entries in output sequence (&gt;=0)
         */
        ConstantSequence( CachedColumn col1, long nrow ) {
            nrow_ = nrow;
            CachedSequence constSeq = col1.createSequence();
            boolean hasValue = constSeq.next();
            booleanValue_ = hasValue ? constSeq.getBooleanValue() : false;
            intValue_ = hasValue ? constSeq.getIntValue() : Integer.MIN_VALUE;
            doubleValue_ = hasValue ? constSeq.getDoubleValue() : Double.NaN;
            objectValue_ = hasValue ? constSeq.getObjectValue() : null;
        }

        public boolean getBooleanValue() {
            return booleanValue_;
        }

        public int getIntValue() {
            return intValue_;
        }

        public double getDoubleValue() {
            return doubleValue_;
        }

        public Object getObjectValue() {
            return objectValue_;
        }

        public boolean next() {
            return irow_++ < nrow_;
        }
    }

    /**
     * Compares values.
     */
    private static interface ValueComparison {

        /**
         * Indicates whether two entries are equivalent.
         * If true, then calling {@link #add} on either of the two would have
         * the same effect.
         *
         * @param   v1  first value, correct type, not null
         * @param   v2  second value, correct type, not null
         * @return   true if the arguments are equivalent in terms of adding
         *           them to a column
         */
        boolean equalValues( Object v1, Object v2 );

        /**
         * Creates a deep copy of an entry.
         * The result is not a reference to other objects which may change,
         * but calling {@link #add} on it would have the same result as calling
         * it on the supplied object <code>v</code>.
         *
         * @param   v   value, correct type, not null
         * @return  clone of <code>v</code>
         */
        Object copyValue( Object v );
    }
}
