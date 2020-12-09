package uk.ac.starlink.ttools.plot2.data;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.task.ConsumerTask;
import uk.ac.starlink.util.URLUtils;

/**
 * DataStoreFactory implementation that will store cached columns
 * in the file system.  These files are not cleared up, so will persist
 * between JVMs.
 *
 * <p>Use with caution, since this may leave large files in a cache directory.
 * To mitigate this, a note about persistent files that were written
 * is written to stderr on JVM shutdown.
 *
 * <p>An instance of this class is safe for use from concurrent threads.
 * It is also safe to use multiple PersistentDataStoreFactory instances
 * using the same cache directory
 * (since {@link ColumnStorage} uses {@link MoveFileByteStore}s to ensure
 * that cached columns only appear in the cache when they are fully populated).
 * However, multiple PersistentDataStoreFactory instances may end up
 * doing work to cache the same input data at the same time,
 * which is not maximally efficient.
 *
 * @author   Mark Taylor
 * @since    7 Jan 2020
 */
public class PersistentDataStoreFactory implements DataStoreFactory {

    private final DiskCache cache_;
    private final TupleRunner tupleRunner_;
    private final DataStore dataStore_;
    private final Set<CacheEntry> inProgress_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.data" );

    /**
     * Constructor.
     *
     * @param  cache   persistent storage cache;
     *                 if null, a default instance will be used
     * @param  tupleRunner  tuple runner;
     *                      if null, a default instance will be used
     */
    public PersistentDataStoreFactory( DiskCache cache,
                                       TupleRunner tupleRunner ) {
        cache_ = cache == null
               ? new DiskCache( toCacheDir( (File) null ), 0 )
               : cache;
        tupleRunner_ = tupleRunner == null ? TupleRunner.DEFAULT : tupleRunner;
        dataStore_ = new PersistentDataStore();
        inProgress_ = new HashSet<CacheEntry>();
    }

    /**
     * Default constructor.
     */
    public PersistentDataStoreFactory() {
        this( (DiskCache) null, (TupleRunner) null );
    }

    public DataStore readDataStore( DataSpec[] dataSpecs, DataStore store0 )
            throws IOException {

        /* If the existing instance can handle these requests, just use that. */
        if ( store0 != null ) {
            boolean hasAll = true;
            for ( DataSpec dspec : dataSpecs ) {
                hasAll = hasAll && store0.hasData( dspec );
            }
            if ( hasAll ) {
                return store0;
            }
        }

        /* Make sure a writable directory is available. */
        cache_.ready();

        /* Find out what data items are required. */
        Set<MaskSpec> mSet = new HashSet<>();
        Set<CoordSpec> cSet = new HashSet<>();
        Set<StarTable> tSet = new HashSet<>();
        for ( DataSpec dspec : dataSpecs ) {
            if ( dspec != null ) {
                tSet.add( dspec.getSourceTable() );
                if ( ! dspec.isMaskTrue() ) {
                    mSet.add( new MaskSpec( dspec ) );
                }
                int nc = dspec.getCoordCount();
                for ( int ic = 0; ic < nc; ic++ ) {
                    cSet.add( new CoordSpec( dspec, ic ) );
                }
            }
        }

        /* Go through required items and see what we need to acquire.
         * For items that are currently in the process of being written
         * (following a separate request), make a note that we need to
         * wait until they are done.  For items that are absent, prepare
         * a list indexed by table of the caching that needs to be done.
         * Items that are already present are touched so that the
         * last modified time reflects usage; this is used for LRU cache
         * management. */
        Collection<CacheEntry> waitings = new HashSet<>();
        Map<StarTable,List<CacheEntry>> tMap = new LinkedHashMap<>();
        synchronized ( inProgress_ ) {
            for ( MaskSpec mspec : mSet ) {
                CacheEntry mEntry = createMaskCacheEntry( mspec );
                if ( inProgress_.contains( mEntry ) ) {
                    waitings.add( mEntry );
                }
                else if ( mEntry.dataExists() ) {
                    mEntry.touch();
                }
                else {
                    inProgress_.add( mEntry );
                    tMap.computeIfAbsent( mspec.getTable(),
                                          k -> new ArrayList<CacheEntry>() )
                        .add( mEntry );
                }
            }
            for ( CoordSpec cspec : cSet ) {
                CacheEntry cEntry = createCoordCacheEntry( cspec );
                if ( inProgress_.contains( cEntry ) ) {
                    waitings.add( cEntry );
                }
                else if ( cEntry.dataExists() ) {
                    cEntry.touch();
                }
                else {
                    inProgress_.add( cEntry );
                    tMap.computeIfAbsent( cspec.getTable(),
                                          k -> new ArrayList<CacheEntry>() )
                        .add( cEntry );
                }
            }
        }

        /* For each table, iterate over rows and write the required data
         * columns to file. */
        for ( StarTable table : tMap.keySet() ) {
            RowSequence rseq = table.getRowSequence();
            List<CacheEntry> tEntries = tMap.get( table );
            int nent = tEntries.size();
            CoordSpec.Reader[] objRdrs = new CoordSpec.Reader[ nent ];
            CachedColumn[] wcols = new CachedColumn[ nent ];
            List<File> files = new ArrayList<File>();
            for ( int ient = 0; ient < nent; ient++ ) {
                CacheEntry entry = tEntries.get( ient );
                objRdrs[ ient ] = entry.createObjectReader( rseq );
                wcols[ ient ] = entry.createWriter();
                files.addAll( Arrays.asList( entry.files_ ) );
            }
            assert nent > 0;
            try {
                for ( long irow = 0; rseq.next(); irow++ ) {
                    for ( int ient = 0; ient < nent; ient++ ) {
                        wcols[ ient ].add( objRdrs[ ient ].readValue( irow ) );
                    }
                }
                for ( int ient = 0; ient < nent; ient++ ) {
                    wcols[ ient ].endAdd();
                }
            }
            catch ( IOException e ) {
                cache_.log( "Plot cache data write failure; "
                          + "deleting files " + files );
                for ( File f : files ) {
                    f.delete();
                }
                throw e;
            }
            finally {
                rseq.close();
                synchronized ( inProgress_ ) {
                    inProgress_.removeAll( tEntries );
                    inProgress_.notifyAll();
                }
            }
            for ( File f : files ) {
                cache_.fileAdded( f );
            }
        }

        /* If any writes have been made, ensure that the cache directory
         * is not overfull. */
        if ( tMap.size() > 0 ) {
            cache_.tidy();
        }

        /* Finally wait for any required items being written by
         * other threads. */
        try {
            synchronized ( inProgress_ ) {
                while ( waitings.size() > 0 ) {
                    inProgress_.wait();
                    waitings.retainAll( inProgress_ );
                }
            }
        }
        catch ( InterruptedException e ) {
            throw (IOException) new InterruptedIOException()
                               .initCause( e );
        }

        /* All the required data should now be cached on disk;
         * either it was already there when this method was invoked,
         * or we have just written it.  There may be a possibility that
         * it's fallen out of the cache in the mean time, but for now
         * we are not addressing that possibility.
         * So return a DataStore instance based on the cache directory,
         * and it should be able to service all the data requests. */
        return dataStore_;
    }

    /**
     * Returns a CacheEntry based on a MaskSpec.
     *
     * @param   mspec  mask specification
     * @return  cache entry
     */
    private CacheEntry createMaskCacheEntry( final MaskSpec mspec ) {
        String id = new StringBuffer()
           .append( "T-" )
           .append( DiskCache.hashText( getTableId( mspec.getTable() ) ) )
           .append( "-M-" )
           .append( DiskCache.hashText( getMaskId( mspec ) ) )
           .toString();
        return new CacheEntry( id, StorageType.BOOLEAN ) {
            CoordSpec.Reader createObjectReader( final RowData rdata ) {
                final MaskSpec.Reader maskRdr = mspec.flagReader( rdata );
                return ir -> Boolean.valueOf( maskRdr.readFlag( ir ) );
            }
        };
    }

    /**
     * Returns a CacheEntry based on a CoordSpec.
     *
     * @param  cspec  coordinate specification
     * @return  cache entry
     */
    private CacheEntry createCoordCacheEntry( final CoordSpec cspec ) {
        String id = new StringBuffer()
           .append( "T-" )
           .append( DiskCache.hashText( getTableId( cspec.getTable() ) ) )
           .append( "-C-" )
           .append( DiskCache.hashText( getCoordId( cspec ) ) )
           .toString();
        return new CacheEntry( id, cspec.getStorageType() ) {
            CoordSpec.Reader createObjectReader( final RowData rdata ) {
                return cspec.valueReader( rdata );
            }
        };
    }

    /**
     * Returns a suitable cache directory for use with this class,
     * given a base directory.
     *
     * @param   baseDir  base directory; if null, java.io.tmpdir is used
     * @return  directory to which cache files can be written
     */
    public static File toCacheDir( File baseDir ) {
        return DiskCache.toCacheDir( baseDir, "plot2-data" );
    }

    /**
     * Returns a unique identifier for a table,
     * for use when generating persistent file names.
     * This should be consistent between JVM invocations.
     *
     * @param  table  table
     * @return   table identifying string
     */
    private static String getTableId( StarTable table ) {

        /* The identity includes both the persisent URL of the original file
         * and any filter commands that have affected it. */
        String identity = ConsumerTask.getIdentity( table );
        assert identity != null;

        /* Add also the modification file of the underlying file, if it's
         * a file: URL, so that if the content changes, the cache entry
         * will no longer be used.  Note this doesn't currently work
         * for (e.g.) http URLs, so once a remote table has been accessed,
         * its data will never be updated in the cache unless
         * the entry expires. */
        File file = getUnderlyingFile( table );
        long modTime = file == null ? 0 : file.lastModified();
        identity += "-@" + Long.toString( modTime );
        return identity;
    }

    /**
     * Attempts to return the original file on which a table is based.
     * Note that different tables can have the same underlying table,
     * since e.g. different wrappers (filtering operations) may be applied.
     *
     * @param  table  table
     * @return   file on which table is based
     */
    private static File getUnderlyingFile( StarTable table ) {
        if ( table == null ) {
            return null;
        }
        URL url = table.getURL();
        if ( url != null ) {
            return URLUtils.urlToFile( url.toString() );
        }
        return table instanceof WrapperStarTable
             ? getUnderlyingFile( ((WrapperStarTable) table).getBaseTable() )
             : null;
    }

    /**
     * Returns an identifier for a mask, unique per table,
     * for use when generating persistent file names.
     * This should be consistent between JVM invocations.
     *
     * @param  mspec  mask specification
     * @return   identifying string
     */
    private static String getMaskId( MaskSpec mspec ) {
        return mspec.getMaskId();
    }

    /**
     * Returns an identifier for a coordinate, unique per table,
     * for use when generating persistent file names.
     * This should be consistent between JVM invocations.
     *
     * @param  cspec  coordinate specification
     * @return   identifying string
     */
    private static String getCoordId( CoordSpec cspec ) {
        return cspec.getCoordId();
    }

    /**
     * Returns a CachedReader for which the boolean result is always true.
     *
     * @return  reader
     */
    private static CachedReader createTrueReader() {
        return new CachedReader() {
            public boolean getBooleanValue( long ix ) {
                return true;
            }
            public double getDoubleValue( long ix ) {
                return -1;
            }
            public int getIntValue( long ix ) {
                return -1;
            }
            public long getLongValue( long ix ) {
                return -1L;
            }
            public Object getObjectValue( long ix ) {
                return null;
            }
        };
    }

    /**
     * Returns a CachedReader for which the results are generally blank.
     *
     * @return  reader
     */
    private static CachedReader createBlankReader() {
        return new CachedReader() {
            public boolean getBooleanValue( long ix ) {
                return false;
            }
            public double getDoubleValue( long ix ) {
                return Double.NaN;
            }
            public int getIntValue( long ix ) {
                return 0;
            }
            public long getLongValue( long ix ) {
                return 0;
            }
            public Object getObjectValue( long ix ) {
                return null;
            }
        };
    }

    /**
     * DataStore implementation.  One instance per factory.
     */
    private class PersistentDataStore implements DataStore {

        private final CachedReader TRUE_READER = createTrueReader();
        private final CachedReader BLANK_READER = createBlankReader();

        public TupleRunner getTupleRunner() {
            return tupleRunner_;
        }

        public boolean hasData( DataSpec dspec ) {
            if ( ! createMaskCacheEntry( new MaskSpec( dspec ) )
                  .dataExists() ) {
                return false;
            }
            for ( int ic = 0; ic < dspec.getCoordCount(); ic++ ) {
                if ( ! createCoordCacheEntry( new CoordSpec( dspec, ic ) )
                      .dataExists() ) {
                    return false;
                }
            }
            return true;
        }

        public TupleSequence getTupleSequence( final DataSpec dspec ) {
            final CacheEntry maskEntry =
                createMaskCacheEntry( new MaskSpec( dspec ) );
            final int nc = dspec.getCoordCount();
            final CacheEntry[] coordEntries = new CacheEntry[ nc ];
            for ( int ic = 0; ic < nc; ic++ ) {
                coordEntries[ ic ] =
                    createCoordCacheEntry( new CoordSpec( dspec, ic ) );
            }
            Supplier<CachedReader> maskSupplier = () -> {
                if ( dspec.isMaskTrue() ) {
                    return TRUE_READER;
                }
                else {
                    try {
                        return maskEntry.createReader();
                    }
                    catch ( IOException e ) {
                        logger_.log( Level.WARNING, "Mask read error", e );
                        return TRUE_READER;
                    }
                }
            };
            Supplier<CachedReader[]> coordsSupplier = () -> {
                CachedReader[] rdrs = new CachedReader[ nc ];
                for ( int ic = 0; ic < nc; ic++ ) {
                    CachedReader rdr;
                    if ( dspec.isCoordBlank( ic ) ) {
                        rdr = BLANK_READER;
                    }
                    else {
                        try {
                            rdr = coordEntries[ ic ].createReader();
                        }
                        catch ( IOException e ) {
                            logger_.log( Level.WARNING, "Coord read error", e );
                            rdr = BLANK_READER;
                        }
                    }
                    rdrs[ ic ] = rdr;
                }
                return rdrs;
            };
            long nrow = dspec.getSourceTable().getRowCount();
            for ( int ic = 0; ic < nc; ic++ ) {
                if ( nrow < 0 ) {
                    nrow = coordEntries[ ic ].getRowCount();
                } 
            }
            return new CachedTupleSequence( maskSupplier, coordsSupplier,
                                            nrow );
        }
    }

    /**
     * Represents a column that is, or that can be, cached to disk.
     */
    @Equality
    private abstract class CacheEntry {

        private final String baseName_;
        private final ColumnStorage colStorage_;
        private final File[] files_;

        /**
         * Constructor.
         * The supplied baseName identifies the data uniquely;
         * instances with the same name are considered to contain the
         * same data.  This name is used as part of the filename(s)
         * for the data on disk.
         *
         * @param  baseName  identifying name for this entry
         * @param  storageType  data type
         */
        CacheEntry( String baseName, StorageType storageType ) {
            baseName_ = baseName;
            colStorage_ = ColumnStorage.getStorage( storageType );
            files_ = colStorage_
                    .getFileNames( new File( cache_.getDir(), baseName ) );
        }

        /**
         * Indicates whether the data for this entry has already been cached
         * to disk.
         *
         * @return  true iff cache contents are available for reading
         */
        boolean dataExists() {
            for ( File file : files_ ) {
                if ( ! file.isFile() ) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Gives a best guess for the number of rows that have been cached.
         *
         * @return   row count approximation, or -1 if not known
         */
        long getRowCount() {
            return colStorage_.getDiskRowCount( files_ );
        }

        /**
         * Creates a disk column to which the data for this
         * entry can be written.
         *
         * @return  column ready for input
         */
        CachedColumn createWriter() throws IOException {
            cache_.log( "Writing plot cache data to "
                       + Arrays.toString( files_ ) );
            return colStorage_.createDiskColumn( files_ );
        }

        /**
         * Returns a reader for the data to be written for this entry,
         * given a RowData obtained from the relevant table.
         *
         * @param  rdata  row data object
         * @return  object reader
         */
        abstract CoordSpec.Reader createObjectReader( RowData rdata );

        /**
         * Returns a reader that will read the data for this entry from disk,
         * assuming it has been written.
         *
         * @return  data reader
         */
        CachedReader createReader() throws IOException {
            return colStorage_.createDiskReader( files_ );
        }

        /**
         * Reset the last modified timestamp of this entry's files
         * to the current time.
         */
        void touch() {
            long now = System.currentTimeMillis();
            for ( File file : files_ ) {
                if ( ! file.setLastModified( now ) ) {
                    logger_.warning( "Touch " + file + " failed" );
                }
            }
        }

        @Override
        public int hashCode() {
            int code = 55413;
            code = 23 * code + colStorage_.hashCode();
            for ( File f : files_ ) {
                code = 23 * code + f.hashCode();
            }
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CacheEntry ) {
                CacheEntry other = (CacheEntry) o;
                return this.colStorage_.equals( other.colStorage_ )
                    && Arrays.equals( this.files_, other.files_ );
            }
            else {
                return false;
            }
        }

        @Override
        public String toString() {
            return baseName_;
        }
    }
}
