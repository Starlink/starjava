package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Slow;

/**
 * DataStoreFactory implementation that reads columns and caches them
 * for later use.
 * The actual storage mechanism is provided by an externally supplied
 * {@link CachedColumnFactory}.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2013
 */
public class CachedDataStoreFactory implements DataStoreFactory {

    private final CachedColumnFactory colFact_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Constructor.
     *
     * @param   colFact  object which provides the storage for caching
     *                   arrays of typed data
     */
    public CachedDataStoreFactory( CachedColumnFactory colFact ) {
        colFact_ = colFact;
    }

    // how about weak links for all known columns, new methods
    // getExistingDataStore(DataSpec[]) and createDataStore(DataSpec[]).
    // Each plot just hangs on to its own data store to prevent it
    // getting gc'd if it might want to benefit from it in the future,
    // rather than presenting it to the factory next time around.
    // I don't know - gc'ing is a bit questionable since these will
    // tie up non-heap resources.  Same applies to disk-type StoragePolicies.
    // Is this a problem?  Should out of disk errors give a message
    // about changing the tmpdir?

    public DataStore readDataStore( DataSpec[] dataSpecs, DataStore prevStore )
            throws IOException, InterruptedException {
        CacheSpec needSpec = createCacheSpec( dataSpecs );
        CacheData gotData = prevStore instanceof CacheData
                          ? ((CacheData) prevStore)
                          : new CacheData();
        CacheSpec makeSpec = needSpec.subtract( gotData.getSpec() );
        if ( makeSpec.isEmpty() ) {
            return prevStore;
        }
        else {
            CacheData oldData = gotData.retain( needSpec );
            CacheData makeData = makeSpec.readData( colFact_ );
            CacheData useData = makeData.add( oldData );
            return useData;
        }
    }

    /**
     * Extracts a CacheSpec from an array of DataSpecs.
     *
     * @param  dataSpecs  data specifications, some may be null
     * @return   new cache specification
     */
    private static CacheSpec createCacheSpec( DataSpec[] dataSpecs ) {
        Set<MaskSpec> mSet = new HashSet<MaskSpec>();
        Set<CoordSpec> cSet = new HashSet<CoordSpec>();
        for ( int is = 0; is < dataSpecs.length; is++ ) {
            DataSpec dspec = dataSpecs[ is ];
            if ( dspec != null ) {
                mSet.add( new MaskSpec( dspec ) );
                int nc = dspec.getCoordCount();
                for ( int ic = 0; ic < nc; ic++ ) {
                    cSet.add( new CoordSpec( dspec, ic ) );
                }
            }
        }
        return new CacheSpec( mSet, cSet );
    }

    /**
     * Reads data from a table according to a set of mask and column
     * specifications.
     *
     * @param   table  table
     * @param   maskSet  required masks
     * @param   coordSet  required coordinates
     * @param   colFact   supplies data storage objects
     * @return   data object containing required data
     */
    @Slow
    private static CacheData readCacheData( StarTable table,
                                            Set<MaskSpec> maskSet,
                                            Set<CoordSpec> coordSet,
                                            CachedColumnFactory colFact )
            throws IOException, InterruptedException {
        MaskSpec[] masks = maskSet.toArray( new MaskSpec[ 0 ] );
        CoordSpec[] coords = coordSet.toArray( new CoordSpec[ 0 ] );
        int nm = masks.length;
        int nc = coords.length;
        long nrow = table.getRowCount();
        CachedColumn[] maskCols = new CachedColumn[ nm ];
        CachedColumn[] coordCols = new CachedColumn[ nc ];
        for ( int im = 0; im < nm; im++ ) {
            maskCols[ im ] =
                colFact.createColumn( StorageType.BOOLEAN, nrow );
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            coordCols[ ic ] =
                colFact.createColumn( coords[ ic ].getStorageType(), nrow );
        }
        RowSequence rseq = table.getRowSequence();
        try {
            for ( long irow = 0; rseq.next(); irow++ ) {
                if ( Thread.currentThread().isInterrupted() ) {
                    throw new InterruptedException();
                }
                for ( int im = 0; im < nm; im++ ) {
                    boolean include = masks[ im ].readFlag( rseq, irow );
                    maskCols[ im ].add( Boolean.valueOf( include ) );
                }
                for ( int ic = 0; ic < nc; ic++ ) {
                    Object value = coords[ ic ].readValue( rseq, irow );
                    coordCols[ ic ].add( value );
                }
            }
        }
        finally {
            rseq.close();
        }
        for ( int im = 0; im < nm; im++ ) {
            maskCols[ im ].endAdd();
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            coordCols[ ic ].endAdd();
        }
        Map<MaskSpec,CachedColumn> mMap =
            new HashMap<MaskSpec,CachedColumn>();
        Map<CoordSpec,CachedColumn> cMap =
            new HashMap<CoordSpec,CachedColumn>();
        for ( int im = 0; im < nm; im++ ) {
            mMap.put( masks[ im ], maskCols[ im ] );
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            cMap.put( coords[ ic ], coordCols[ ic ] );
        }
        return new CacheData( mMap, cMap );
    }

    /**
     * Formats a count of typed items for logging messages.
     *
     * @param  collection   collection to count
     * @param  word   word for a single item
     * @return   string like "103 widgets"
     */
    private static String itemCount( Collection collection, String word ) {
        int count = collection.size();
        StringBuilder sbuf = new StringBuilder()
            .append( count )
            .append( ' ' )
            .append( word );
        if ( count != 1 ) {
            sbuf.append( 's' );
        }
        return sbuf.toString();
    }

    /**
     * Specifies the mask and column data held in a cache data object.
     * Its state is simply an aggregation of a set of MaskSpecs and a set
     * of CoordSpecs.  It also provides some manipulation methods.
     */
    private static class CacheSpec {
        private final Set<MaskSpec> mSet_;
        private final Set<CoordSpec> cSet_;

        /**
         * Constructor.
         *
         * @param   mSet  set of mask specifications
         * @param   cSet  set of coordinate specifications
         */
        CacheSpec( Set<MaskSpec> mSet, Set<CoordSpec> cSet ) {
            mSet_ = new HashSet<MaskSpec>( mSet );
            cSet_ = new HashSet<CoordSpec>( cSet );
        }

        /**
         * Returns a new CacheSpec which is the result of removing the
         * items in another one from this one.
         * 
         * @param  other  second cache spec
         * @return  new cache spec containing this one's content minus
         *          the other one's content
         */
        CacheSpec subtract( CacheSpec other ) {
            CacheSpec result = new CacheSpec( this.mSet_, this.cSet_ );
            result.mSet_.removeAll( other.mSet_ );
            result.cSet_.removeAll( other.cSet_ );
            return result;
        }

        /**
         * Indicates whether this spec is empty.
         *
         * @return   true iff this object specifies no masks and no columns
         */
        boolean isEmpty() {
            return mSet_.isEmpty() && cSet_.isEmpty();
        }

        /**
         * Reads the data specified by this object and returns a corresponding
         * CacheData.
         *
         * @param   colFact  factory supplying actual column data storage
         * @return  data object containing all data specified by this object
         */
        @Slow
        CacheData readData( CachedColumnFactory colFact )
                throws IOException, InterruptedException {
            Level level = Level.INFO;
            if ( logger_.isLoggable( level ) ) {
                String msg = new StringBuilder()
                    .append( "Caching plot data: " )
                    .append( itemCount( getTables(), "table" ) )
                    .append( ", " )
                    .append( itemCount( mSet_, "mask" ) )
                    .append( ", " )
                    .append( itemCount( cSet_, "coord" ) )
                    .toString();
                logger_.log( level, msg );
            }
            CacheData data = new CacheData();
            for ( StarTable table : getTables() ) {
                CacheData tData =
                    readCacheData( table, getMasks( table ), getCoords( table ),
                                   colFact );
                data = data.add( tData );
            }
            return data;
        }

        /**
         * Returns the set of tables used by any of the masks or columns
         * specified by this object.
         *
         * @return   used tables
         */
        private Set<StarTable> getTables() {
            Set<StarTable> tSet = new HashSet<StarTable>();
            for ( MaskSpec mask : mSet_ ) {
                tSet.add( mask.table_ );
            }
            for ( CoordSpec coord : cSet_ ) {
                tSet.add( coord.table_ );
            }
            return tSet;
        }

        /**
         * Returns a set of all the masks required by this specification
         * from a given table.
         *
         * @param   table  table
         * @return  masks required from table
         */
        private Set<MaskSpec> getMasks( StarTable table ) {
            Set<MaskSpec> tmSet = new HashSet<MaskSpec>();
            for ( MaskSpec mask : mSet_ ) {
                if ( mask.table_.equals( table ) ) {
                    tmSet.add( mask );
                }
            }
            return tmSet;
        }

        /**
         * Returns a set of all the coordinates required by this specification
         * from a given table.
         *
         * @param   table  table
         * @return  coords required from table
         */
        private Set<CoordSpec> getCoords( StarTable table ) {
            Set<CoordSpec> tcSet = new HashSet<CoordSpec>();
            for ( CoordSpec coord : cSet_ ) {
                if ( coord.table_.equals( table ) ) {
                    tcSet.add( coord );
                }
            }
            return tcSet;
        }

        @Override
        public String toString() {
            return "Masks: " + mSet_ + "; Columns: " + cSet_;
        }
    }

    /**
     * Holds cached column and mask data for a number of masks and coords.
     * It also implements DataStore.
     */
    private static class CacheData implements DataStore {
        private final Map<MaskSpec,CachedColumn> mMap_;
        private final Map<CoordSpec,CachedColumn> cMap_;
 
        /**
         * Constructs a CacheData from data maps.
         *
         * @param   mMap  map of mask data, keyed by mask spec
         * @param   cMap  map of coordinate data, keyed by coord spec
         */
        CacheData( Map<MaskSpec,CachedColumn> mMap,
                   Map<CoordSpec,CachedColumn> cMap ) {
            mMap_ = new HashMap<MaskSpec,CachedColumn>( mMap );
            cMap_ = new HashMap<CoordSpec,CachedColumn>( cMap );
        }

        /**
         * Clone constructor.
         *
         * @param  cloned   object whos data is to be copied (by reference)
         */
        CacheData( CacheData cloned ) {
            this( cloned.mMap_, cloned.cMap_ );
        }

        /**
         * Constructs a CacheData with no data.
         */
        CacheData() {
            this( new HashMap<MaskSpec,CachedColumn>(),
                  new HashMap<CoordSpec,CachedColumn>() );
        }

        /**
         * Returns an object which specifies the data held by this data store.
         *
         * @return   cached data specification
         */
        CacheSpec getSpec() {
            return new CacheSpec( mMap_.keySet(), cMap_.keySet() );
        }

        /**
         * Returns a new CacheData which is the union of this and another.
         *
         * @param   other  other data object
         * @return   new data object containing union
         */
        CacheData add( CacheData other ) {
            CacheData result = new CacheData( this.mMap_, this.cMap_ );
            result.mMap_.putAll( other.mMap_ );
            result.cMap_.putAll( other.cMap_ );
            return result;
        }

        /**
         * Returns a new CacheData which contains only those items from
         * this one which are specified in a supplied spec.
         *
         * @param   spec   specification of data items to retain
         * @return  new intersection data object
         */
        CacheData retain( CacheSpec spec ) {
            CacheData result = new CacheData( this.mMap_, this.cMap_ );
            result.mMap_.keySet().retainAll( spec.mSet_ );
            result.cMap_.keySet().retainAll( spec.cSet_ );
            return result;
        }

        /**
         * Returns a mask data object.
         *
         * @param  dataSpec  data spec
         * @return   mask column
         */
        CachedColumn getMask( DataSpec dataSpec ) {
            return mMap_.get( new MaskSpec( dataSpec ) );
        }

        /**
         * Returns a coordinate column data object.
         *
         * @param  dataSpec  data spec
         * @param  icoord  coordinate index within dataSpec
         * @return  data column
         */
        CachedColumn getColumn( DataSpec dataSpec, int icoord ) {
            return cMap_.get( new CoordSpec( dataSpec, icoord ) );
        }

        /**
         * Returns the coordinate data for a given data spec as an array of
         * cached columns.  If any of the coordinates is not available,
         * null is returned.
         *
         * @param   dataSpec  specification of required columns
         * @return   all column data, or null
         */
        CachedColumn[] getColumns( DataSpec dataSpec ) {
            int ncol = dataSpec.getCoordCount();
            CachedColumn[] cols = new CachedColumn[ ncol ];
            for ( int ic = 0; ic < ncol; ic++ ) {
                cols[ ic ] = getColumn( dataSpec, ic );
                if ( cols[ ic ] == null ) {
                    return null;
                }
            }
            return cols;
        }

        // DataStore implementation.
        public boolean hasData( DataSpec spec ) {
            return getMask( spec ) != null
                && getColumns( spec ) != null;
        }

        public TupleSequence getTupleSequence( DataSpec spec ) {
            return new CachedTupleSequence( getMask( spec ),
                                            getColumns( spec ) );
        }
    }

    /**
     * Characterises information about a data inclusion mask.
     * It aggregates a table and a maskId, and provides the capability of
     * reading the corresponding inclusion data from a RowSequence.
     */
    @Equality
    private static class MaskSpec {
        final UserDataReader dataReader_;
        final StarTable table_;
        final Object maskId_;

        /**
         * Constructor.
         *
         * @param   dataSpec   specification from which the mask information
         *                     is taken
         */
        MaskSpec( DataSpec dataSpec ) {
            dataReader_ = dataSpec.createUserDataReader();
            table_ = dataSpec.getSourceTable();
            maskId_ = dataSpec.getMaskId();
        }

        /**
         * Reads inclusion flag from a row sequence.
         *
         * @param   rseq   row sequence of this data spec's table
         * @param   irow   row index
         * @return  inclusion mask for current row
         */
        boolean readFlag( RowSequence rseq, long irow ) throws IOException {
            return dataReader_.getMaskFlag( rseq, irow );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MaskSpec ) {
                MaskSpec other = (MaskSpec) o;
                return other.table_.equals( this.table_ )
                    && other.maskId_.equals( this.maskId_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.table_.hashCode() * 23 + maskId_.hashCode();
        }

        @Override
        public String toString() {
            return String.valueOf( maskId_ );
        }
    }

    /**
     * Characterises information about a coordinate value.
     * It aggregates a table and a coordId, and provides the capability of
     * reading the corresponding value data from a row sequence.
     */
    @Equality
    private static class CoordSpec {
        final UserDataReader dataReader_;
        final StarTable table_;
        final int icoord_;
        final Coord coord_;
        final Object coordId_;
        final DomainMapper[] mappers_;

        /**
         * Constructor.
         *
         * @param  dataSpec  data specification
         * @param  icoord  coordinate index within dataSpec
         */
        CoordSpec( DataSpec dataSpec, int icoord ) {
            dataReader_ = dataSpec.createUserDataReader();
            icoord_ = icoord;
            table_ = dataSpec.getSourceTable();
            coordId_ = dataSpec.getCoordId( icoord );
            coord_ = dataSpec.getCoord( icoord );
            mappers_ = SimpleDataStoreFactory
                      .getUserCoordMappers( dataSpec, icoord );
        }

        /**
         * Returns the storage type for this column.
         *
         * @return  storage type
         */
        StorageType getStorageType() {
            return coord_.getStorageType();
        }

        /**
         * Reads the user for this coordinate from a row sequence.
         *
         * @param   rseq   row sequence of this data spec's table
         * @param   irow   row index
         * @param   coordinate stored value for this column at current row
         */
        Object readValue( RowSequence rseq, long irow ) throws IOException {
            Object[] userCoords =
                dataReader_.getUserCoordValues( rseq, irow, icoord_ );
            Object value = coord_.inputToStorage( userCoords, mappers_ );
            assert value != null;
            return value;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CoordSpec ) {
                CoordSpec other = (CoordSpec) o;
                return other.table_.equals( this.table_ )
                    && other.coordId_.equals( this.coordId_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.table_.hashCode() * 37 + coordId_.hashCode();
        }

        @Override
        public String toString() {
            return String.valueOf( coordId_ );
        }
    }

    /**
     * TupleSequence implementation based on CachedColumns.
     */
    private static class CachedTupleSequence implements TupleSequence {

        private final int ncol_;
        private final CachedSequence maskSeq_;
        private final CachedSequence[] colSeqs_;
        private long irow_ = -1;

        /**
         * Constructor.
         *
         * @param  mask  boolean-typed column providing inclusion flags per row
         * @param  cols  array of columns providing data cells per row
         */
        CachedTupleSequence( CachedColumn mask, CachedColumn[] cols ) {
            ncol_ = cols.length;
            maskSeq_ = mask.createSequence();
            colSeqs_ = new CachedSequence[ ncol_ ];
            for ( int ic = 0; ic < ncol_; ic++ ) {
                colSeqs_[ ic ] = cols[ ic ].createSequence();
            }
        }

        public boolean next() {
            while ( maskSeq_.next() ) {
                irow_++;
                for ( int ic = 0; ic < ncol_; ic++ ) {
                    colSeqs_[ ic ].next();
                }
                if ( maskSeq_.getBooleanValue() ) {
                    return true;
                }
            }
            return false;
        }

        public long getRowIndex() {
            return irow_;
        }

        public Object getObjectValue( int icol ) {
            return colSeqs_[ icol ].getObjectValue();
        }

        public double getDoubleValue( int icol ) {
            return colSeqs_[ icol ].getDoubleValue();
        }

        public int getIntValue( int icol ) {
            return colSeqs_[ icol ].getIntValue();
        }

        public boolean getBooleanValue( int icol ) {
            return colSeqs_[ icol ].getBooleanValue();
        }
    }
}
