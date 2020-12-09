package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
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
    private final TupleRunner runner_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Constructor.
     *
     * @param   colFact  object which provides the storage for caching
     *                   arrays of typed data
     * @param   runner  tuple runner dispensed with DataStores
     */
    public CachedDataStoreFactory( CachedColumnFactory colFact,
                                   TupleRunner runner ) {
        colFact_ = colFact;
        runner_ = runner;
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
                          : new CacheData( runner_ );
        CacheSpec makeSpec = needSpec.subtract( gotData.getSpec() );
        if ( makeSpec.isEmpty() ) {
            return prevStore;
        }
        else {
            CacheData oldData = gotData.retain( needSpec );
            CacheData makeData = makeSpec.readData( colFact_, runner_ );
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
     * @param   runner  tuple runner dispensed with DataStores
     * @return   data object containing required data
     */
    @Slow
    private static CacheData readCacheData( StarTable table,
                                            Set<MaskSpec> maskSet,
                                            Set<CoordSpec> coordSet,
                                            CachedColumnFactory colFact,
                                            TupleRunner runner )
            throws IOException, InterruptedException {
        MaskSpec[] masks = maskSet.toArray( new MaskSpec[ 0 ] );
        CoordSpec[] coords = coordSet.toArray( new CoordSpec[ 0 ] );
        int nm = masks.length;
        int nc = coords.length;
        long nrow = table.getRowCount();
        CachedColumn[] maskCols = new CachedColumn[ nm ];
        CachedColumn[] coordCols = new CachedColumn[ nc ];
        MaskSpec.Reader[] maskRdrs = new MaskSpec.Reader[ nm ];
        CoordSpec.Reader[] coordRdrs = new CoordSpec.Reader[ nc ];
        RowSequence rseq = table.getRowSequence();
        for ( int im = 0; im < nm; im++ ) {
            maskRdrs[ im ] = masks[ im ].flagReader( rseq );
            maskCols[ im ] =
                colFact.createColumn( StorageType.BOOLEAN, nrow );
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            coordRdrs[ ic ] = coords[ ic ].valueReader( rseq );
            coordCols[ ic ] =
                colFact.createColumn( coords[ ic ].getStorageType(), nrow );
        }
        try {
            for ( long irow = 0; rseq.next(); irow++ ) {
                if ( Thread.currentThread().isInterrupted() ) {
                    throw new InterruptedException();
                }
                for ( int im = 0; im < nm; im++ ) {
                    boolean include = maskRdrs[ im ].readFlag( irow );
                    maskCols[ im ].add( Boolean.valueOf( include ) );
                }
                for ( int ic = 0; ic < nc; ic++ ) {
                    Object value = coordRdrs[ ic ].readValue( irow );
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
        return new CacheData( runner, mMap, cMap );
    }

    /**
     * Formats a count of typed items for logging messages.
     *
     * @param  collection   collection to count
     * @param  word   word for a single item
     * @return   string like "103 widgets"
     */
    private static String itemCount( Collection<?> collection, String word ) {
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
         * @param   runner  tuple runner dispensed with DataStores
         * @return  data object containing all data specified by this object
         */
        @Slow
        CacheData readData( CachedColumnFactory colFact, TupleRunner runner )
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
            CacheData data = new CacheData( runner );
            for ( StarTable table : getTables() ) {
                CacheData tData =
                    readCacheData( table, getMasks( table ), getCoords( table ),
                                   colFact, runner );
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
                tSet.add( mask.getTable() );
            }
            for ( CoordSpec coord : cSet_ ) {
                tSet.add( coord.getTable() );
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
                if ( mask.getTable().equals( table ) ) {
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
                if ( coord.getTable().equals( table ) ) {
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
        private final TupleRunner runner_;
 
        /**
         * Constructs a CacheData from data maps.
         *
         * @param   runner  tuple runner
         * @param   mMap  map of mask data, keyed by mask spec
         * @param   cMap  map of coordinate data, keyed by coord spec
         */
        CacheData( TupleRunner runner,
                   Map<MaskSpec,CachedColumn> mMap,
                   Map<CoordSpec,CachedColumn> cMap ) {
            runner_ = runner;
            mMap_ = new HashMap<MaskSpec,CachedColumn>( mMap );
            cMap_ = new HashMap<CoordSpec,CachedColumn>( cMap );
        }

        /**
         * Clone constructor.
         *
         * @param   runner  tuple runner
         * @param  cloned   object whos data is to be copied (by reference)
         */
        CacheData( TupleRunner runner, CacheData cloned ) {
            this( cloned.runner_, cloned.mMap_, cloned.cMap_ );
        }

        /**
         * Constructs a CacheData with no data.
         *
         * @param   runner  tuple runner
         */
        CacheData( TupleRunner runner ) {
            this( runner, new HashMap<MaskSpec,CachedColumn>(),
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
            CacheData result = new CacheData( runner_, this.mMap_, this.cMap_ );
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
            CacheData result = new CacheData( runner_, this.mMap_, this.cMap_ );
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
            final CachedColumn mask = getMask( spec );
            long nrow = mask.getRowCount();
            Supplier<CachedReader> maskSupplier = mask::createReader;
            final CachedColumn[] cols = getColumns( spec );
            final int ncol = cols.length;
            Supplier<CachedReader[]> colsSupplier = () -> {
                CachedReader[] rdrs = new CachedReader[ ncol ];
                for ( int ic = 0; ic < ncol; ic++ ) {
                    rdrs[ ic ] = cols[ ic ].createReader();
                }
                return rdrs;
            };
            return new CachedTupleSequence( maskSupplier, colsSupplier, nrow );
        }

        public TupleRunner getTupleRunner() {
            return runner_;
        }
    }
}
