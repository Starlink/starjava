package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
     * @param   tupleRunner  tuple runner dispensed with DataStores
     * @return   data object containing required data
     */
    @Slow
    private static CacheData readCacheData( StarTable table,
                                            Set<MaskSpec> maskSet,
                                            Set<CoordSpec> coordSet,
                                            CachedColumnFactory colFact,
                                            TupleRunner tupleRunner )
            throws IOException, InterruptedException {
        MaskSpec[] masks = maskSet.toArray( new MaskSpec[ 0 ] );
        CoordSpec[] coords = coordSet.toArray( new CoordSpec[ 0 ] );
        TableCachedData tcd =
            TableCachedData.readData( table, masks, coords, colFact );
        long nrow = tcd.getRowCount();
        List<Supplier<CachedReader>> maskCols = tcd.getMaskColumns();
        List<Supplier<CachedReader>> coordCols = tcd.getCoordColumns();
        Map<StarTable,Long> nMap = new HashMap<>();
        Map<MaskSpec,Supplier<CachedReader>> mMap = new HashMap<>();
        Map<CoordSpec,Supplier<CachedReader>> cMap = new HashMap<>();
        nMap.put( table, Long.valueOf( nrow ) );
        for ( int im = 0; im < masks.length; im++ ) {
            mMap.put( masks[ im ], maskCols.get( im ) );
        }
        for ( int ic = 0; ic < coords.length; ic++ ) {
            cMap.put( coords[ ic ], coordCols.get( ic ) );
        }
        return new CacheData( tupleRunner, mMap, cMap, nMap );
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
        private final TupleRunner runner_;
        private final Map<MaskSpec,Supplier<CachedReader>> mMap_;
        private final Map<CoordSpec,Supplier<CachedReader>> cMap_;
        private final Map<StarTable,Long> nMap_;
 
        /**
         * Constructs a CacheData from data maps.
         *
         * @param   runner  tuple runner
         * @param   mMap  map of mask data, keyed by mask spec
         * @param   cMap  map of coordinate data, keyed by coord spec
         * @param   nMap  map of table row count, keyed by table
         */
        CacheData( TupleRunner runner,
                   Map<MaskSpec,Supplier<CachedReader>> mMap,
                   Map<CoordSpec,Supplier<CachedReader>> cMap,
                   Map<StarTable,Long> nMap ) {
            runner_ = runner;
            mMap_ = new HashMap<MaskSpec,Supplier<CachedReader>>( mMap );
            cMap_ = new HashMap<CoordSpec,Supplier<CachedReader>>( cMap );
            nMap_ = new HashMap<StarTable,Long>( nMap );
        }

        /**
         * Clone constructor.
         *
         * @param   runner  tuple runner
         * @param  cloned   object whose data is to be copied (by reference)
         */
        CacheData( TupleRunner runner, CacheData cloned ) {
            this( cloned.runner_, cloned.mMap_, cloned.cMap_, cloned.nMap_ );
        }

        /**
         * Constructs a CacheData with no data.
         *
         * @param   runner  tuple runner
         */
        CacheData( TupleRunner runner ) {
            this( runner, new HashMap<MaskSpec,Supplier<CachedReader>>(),
                          new HashMap<CoordSpec,Supplier<CachedReader>>(),
                          new HashMap<StarTable,Long>() );
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
            CacheData result =
                new CacheData( runner_, this.mMap_, this.cMap_, this.nMap_ );
            result.mMap_.putAll( other.mMap_ );
            result.cMap_.putAll( other.cMap_ );
            result.nMap_.putAll( other.nMap_ );
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
            Set<StarTable> tSet = new HashSet<StarTable>();
            for ( MaskSpec mSpec : mMap_.keySet() ) {
                tSet.add( mSpec.getTable() );
            }
            for ( CoordSpec cSpec : cMap_.keySet() ) {
                tSet.add( cSpec.getTable() );
            }
            CacheData result = new CacheData( runner_, mMap_, cMap_, nMap_ );
            result.mMap_.keySet().retainAll( spec.mSet_ );
            result.cMap_.keySet().retainAll( spec.cSet_ );
            result.nMap_.keySet().retainAll( tSet );
            return result;
        }

        /**
         * Returns a mask data object.
         *
         * @param  dataSpec  data spec
         * @return   mask column
         */
        Supplier<CachedReader> getMask( DataSpec dataSpec ) {
            return mMap_.get( new MaskSpec( dataSpec ) );
        }

        /**
         * Returns a coordinate column data object.
         *
         * @param  dataSpec  data spec
         * @param  icoord  coordinate index within dataSpec
         * @return  data column
         */
        Supplier<CachedReader> getColumn( DataSpec dataSpec, int icoord ) {
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
        List<Supplier<CachedReader>> getColumns( DataSpec dataSpec ) {
            int ncol = dataSpec.getCoordCount();
            List<Supplier<CachedReader>> cols = new ArrayList<>();
            for ( int ic = 0; ic < ncol; ic++ ) {
                Supplier<CachedReader> col = getColumn( dataSpec, ic );
                if ( col == null ) {
                    return null;
                }
                else {
                    cols.add( col );
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
            Long nRow = nMap_.get( spec.getSourceTable() );
            long nrow = nRow == null ? -1 :  nRow.longValue();
            Supplier<CachedReader> maskSupplier = getMask( spec );
            final List<Supplier<CachedReader>> cols = getColumns( spec );
            final int ncol = cols.size();
            Supplier<CachedReader[]> colsSupplier = () -> {
                CachedReader[] rdrs = new CachedReader[ ncol ];
                for ( int ic = 0; ic < ncol; ic++ ) {
                    rdrs[ ic ] = cols.get( ic ).get();
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
