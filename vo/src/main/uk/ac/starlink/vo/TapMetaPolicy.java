package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.IOFunction;

/**
 * Defines the policy for acquiring TAP metadata from a remote service.
 * This is a factory for TapMetaReader objects.
 *
 * @author   Mark Taylor
 * @since    25 Mar 2015
 */
public abstract class TapMetaPolicy {

    private final String name_;
    private final String description_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** Tries its best to do something sensible. */
    public static final TapMetaPolicy AUTO;

    /** Uses the VOSI 1.0 /tables endpoint. */
    public static final TapMetaPolicy VOSI10;

    /** Uses the TAP_SCHEMA tables, with columns on demand. */
    public static final TapMetaPolicy TAPSCHEMA_C;

    /** Uses the TAP_SCHEMA tables, with columns and foreign keys on demand. */
    public static final TapMetaPolicy TAPSCHEMA_CF;

    /** Uses the TAP_SCHEMA tables, all data loaded at once. */
    public static final TapMetaPolicy TAPSCHEMA;

    /** Uses the non-standard VizieR two-level tables endpoint. */
    public static final TapMetaPolicy VIZIER;

    /** Uses the VOSI 1.1 one-stage (detail=max) /tables endpoint. */
    public static final TapMetaPolicy VOSI11_MAX;

    /** Uses the VOSI 1.1 two-stage (detail=min) /tables endpoint. */
    public static final TapMetaPolicy VOSI11_MIN;

    /** Uses the VOSI 1.1 /tables endpoint (backward compatible). */
    public static final TapMetaPolicy VOSI11_NULL;

    private static final TapMetaPolicy[] KNOWN_VALUES = {
        AUTO = new TapMetaPolicy( "Auto",
                                  "Chooses a suitable place to get table "
                                + "metadata depending on the service" ) {
            public TapMetaReader createMetaReader( TapService service,
                                                   ContentCoding coding ) {
                return createAutoMetaReader( service, coding, 20_000 );
            }
        },
        TAPSCHEMA_C = createTapSchemaPolicy( "TAP_SCHEMA-C", false, true ),
        TAPSCHEMA_CF =
            createTapSchemaPolicy( "TAP_SCHEMA-CF", false, false ),
        TAPSCHEMA =
            createTapSchemaPolicy( "TAP_SCHEMA", true, false ),
        VOSI11_NULL = createVosi11Policy( "TableSet-VOSI1.1",
                                          Vosi11TapMetaReader.DetailMode.NULL ),
        VOSI11_MAX = createVosi11Policy( "TableSet-VOSI1.1-1step",
                                         Vosi11TapMetaReader.DetailMode.MAX ),
        VOSI11_MIN = createVosi11Policy( "TableSet-VOSI1.1-2step",
                                         Vosi11TapMetaReader.DetailMode.MIN ),
        VOSI10 = new TapMetaPolicy( "TableSet-VOSI1.0",
                                    "Reads all metadata in one go from the "
                                  + "VOSI-1.0 /tables endpoint" ) {
            public TapMetaReader createMetaReader( TapService service,
                                                   ContentCoding coding ) {
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                URL tablesUrl = service.getTablesEndpoint();
                return new TableSetTapMetaReader( tablesUrl, fixer, coding );
            }
        },
        VIZIER = new TapMetaPolicy( "VizieR",
                                    "Uses TAPVizieR's non-standard two-stage "
                                  + "VOSI tables endpoint" ) {
            public TapMetaReader createMetaReader( TapService service,
                                                   ContentCoding coding ) {
                URL tablesetUrl = service.getTablesEndpoint();
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new VizierTapMetaReader( tablesetUrl, fixer, coding );
            }
        },
    };

    /**
     * Constructor.
     *
     * @param  name  short name for this instance
     * @param  description   plain text description of this instance
     */
    protected TapMetaPolicy( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Returns the name of this object.
     *
     * @return  short name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a plain text description of this object.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Creates an object capable of acquiring TAP metadata for a
     * given TAP service description.
     *
     * @param    service  TAP service description
     * @param    coding  configures HTTP compression;
     *                   implementations may honour this hint but are not
     *                   required to
     * @return   new metadata reader
     */
    public abstract TapMetaReader createMetaReader( TapService service,
                                                    ContentCoding coding );

    /**
     * Returns a list of some general-purpose concrete implementations
     * of this class.
     *
     * @return   list of instances
     */
    public static TapMetaPolicy[] getStandardInstances() {
        return KNOWN_VALUES.clone();
    }

    /**
     * Returns an instance of this class suitable for general use.
     *
     * @return   default instance
     */
    public static TapMetaPolicy getDefaultInstance() {
        return AUTO;
    }

    /**
     * Sorts an array of schemas in place by schema name.
     *
     * @param  smetas  schema array
     */
    static void sortSchemas( SchemaMeta[] smetas ) {
        Arrays.sort( smetas, new Comparator<SchemaMeta>() {
            public int compare( SchemaMeta s1, SchemaMeta s2 ) {
                return getSchemaName( s1 ).compareTo( getSchemaName( s2 ) );
            }
            private String getSchemaName( SchemaMeta smeta ) {
                String name = smeta.getName();
                return name == null ? "" : name;
            }
        } );
    }

    /**
     * Sorts an array of tables in place by table name.
     *
     * @param  tmetas  table array
     */
    static void sortTables( TableMeta[] tmetas ) {
        Arrays.sort( tmetas, new Comparator<TableMeta>() {
            public int compare( TableMeta t1, TableMeta t2 ) {
                return getTableName( t1 ).compareTo( getTableName( t2 ) );
            }
            private String getTableName( TableMeta tmeta ) {
                String name = tmeta.getName();
                return name == null ? "" : name;
            }
        } );
    }


    /**
     * Create a policy instance that uses the VOSI-1.1 variable-detail option.
     *
     * @param  name  policy name
     * @param  dmode   requested detail mode for metadata queries
     */
    private static TapMetaPolicy
            createVosi11Policy( String name,
                                final Vosi11TapMetaReader.DetailMode dmode ) {
        String descrip =
             new StringBuffer()
            .append( "Reads metadata from the VOSI-1.1 /tables endpoint;\n" )
            .append( dmode.getDescription() )
            .toString();
        return new TapMetaPolicy( name, descrip ) {
            public TapMetaReader createMetaReader( TapService service,
                                                   ContentCoding coding ) {
                URL tablesUrl = service.getTablesEndpoint();
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new Vosi11TapMetaReader( tablesUrl, fixer, coding,
                                                dmode );
            }
        };
    }

    /**
     * Returns a policy instance that uses TAP_SCHEMA metadata.
     *
     * @param  name  policy name
     * @param  popTables  if true tables will be populated when read with
     *                    both columns and foreign keys; if false with neither
     * @param  preloadKeys  true to load all foreign keys in advance,
     *                      false to load them on demand;
     *                      must be false if <code>popTables</code> is true
     */
    private static TapMetaPolicy
            createTapSchemaPolicy( String name, final boolean popTables,
                                   final boolean preloadKeys ) {
        StringBuffer sbuf = new StringBuffer()
            .append( "Reads metadata by making synchronous queries " )
            .append( "on the TAP_SCHEMA tables; " );
        if ( popTables ) {
            assert ! preloadKeys;
            sbuf.append( "metadata for all tables is read at once" );
        }
        else if ( preloadKeys ) {
            sbuf.append( "foreign keys are all read at once, " )
                .append( "columns are read as required" );
        }
        else {
            sbuf.append( "columns and foreign keys are read as required" );
        }
        return new TapMetaPolicy( name, sbuf.toString() ) {
            public TapMetaReader createMetaReader( TapService service,
                                                   ContentCoding coding ) {
                int maxrec = 99999;
                boolean popSchemas = true;
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new TapSchemaTapMetaReader( service, maxrec, coding,
                                                   popSchemas, popTables, fixer,
                                                   preloadKeys );
            }
        };
    }

    /**
     * Returns a TapMetaReader instance for a given service with policy
     * determined by the apparent size of the metadata set.
     *
     * @param  service   TAP service description
     * @param    coding  configures HTTP compression
     * @param  maxrow     maximum number of records to tolerate from a single
     *                    TAP metadata query 
     */
    private static TapMetaReader createAutoMetaReader( TapService service,
                                                       ContentCoding coding,
                                                       int maxrow ) {
        MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();

        /* Find out how many columns there are in total.
         * The columns table is almost certainly the longest one we would
         * have to cope with.  In principle it could be the foreign key table,
         * but that seems very unlikely. */
        long ncol = readRowCount( service,
                                  TapSchemaInterrogator.COLUMN_QUERIER
                                                       .getTableName() );

        /* Make decisions about how to populate the metadata based on this.
         * For a large service, just get the table list first and get
         * individual column lists as and when they are required;
         * for a small service, grab all the metadata now. */
        boolean manyCols = ncol >= 0 && ncol > maxrow;
        String prefMsg =
            manyCols ? ( "Many columns in TAP service"
                       + " (" + ncol + " > " + maxrow + ");"
                       + " prefer reading tables up front"
                       + " and columns as required" )
                     : ( "Not many columns in TAP service"
                       + " (" + ncol + " <= " + maxrow + "); "
                       + "prefer reading table and column metadata up front" );
        logger_.info( prefMsg );

        /* Attempt to read metadata using the VOSI tables endpoint.
         * This tends to be faster, and in some cases is more reliable,
         * than TAP_SCHEMA queries.  Note however that if the service
         * does not honour the DetailMode flag, which many services do not,
         * we are pulling down the whole metadata document here,
         * which may be quite large (at time of writing up to about 20Mb). */
        Vosi11TapMetaReader.DetailMode detailMode =
            manyCols ? Vosi11TapMetaReader.DetailMode.MIN
                     : Vosi11TapMetaReader.DetailMode.MAX;
        TapMetaReader preferredReader =
            new Vosi11TapMetaReader( service.getTablesEndpoint(), fixer,
                                     coding, detailMode );

        /* However, the VOSI tables endpoint is optional (though present
         * on most/all respectable TAP services?), so prepare to fall back
         * to querying the mandatory TAP_SCHEMA metadata if necessary. */
        Supplier<TapMetaReader> fallbackReaderSupplier = () -> {
            String linkTableName =
                TapSchemaInterrogator.LINK_QUERIER.getTableName();
            long nlink = readRowCount( service, linkTableName );
            boolean preloadFkeys = nlink < 0 || nlink <= maxrow;
            if ( nlink >= 0 ) {
                String msg = new StringBuffer()
                    .append( preloadFkeys ? "Not many" : "Many" )
                    .append( " rows in " )
                    .append( linkTableName )
                    .append( " (" )
                    .append( nlink )
                    .append( preloadFkeys ? " <= " : " > " )
                    .append( maxrow )
                    .append( ");" )
                    .append( preloadFkeys ? " preload all foreign keys"
                                          : " no preload" )
                    .toString();
                logger_.info( msg );
            }
            int maxrec = (int) Math.min( Integer.MAX_VALUE,
                                         Math.max( ncol + 1, nlink + 1 ) );
            boolean popSchema = true;
            boolean popTable = ! manyCols;
            return new TapSchemaTapMetaReader( service, maxrec, coding,
                                               popSchema, popTable, fixer,
                                               preloadFkeys );
        };
        return new FallbackTapMetaReader( preferredReader,
                                          fallbackReaderSupplier );
    }

    /**
     * Return the number of rows in a named TAP table.
     * In case of failure, a logging message is issued and -1 is returned.
     *
     * @param   service  TAP service description
     * @param   tableName  fully qualified ADQL table name
     * @return   number of rows in table, or -1
     */
    private static long readRowCount( TapService service, String tableName ) {
        String adql = "SELECT COUNT(*) AS nrow FROM " + tableName;
        Number nrow;
        try {
            nrow = TapQuery.scalarQuery( service, adql, Number.class );
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING,
                         "Row count for " + tableName + " failed: " + e, e );
            return -1;
        }
        if ( nrow == null ) {
            logger_.log( Level.WARNING,
                         "No row count result for " + tableName );
            return -1;
        }
        return nrow.longValue();
    }

    /**
     * TapMetaReader implementation that uses a preferred method if it can,
     * but if that fails on the first attempt to use it, falls back
     * to a secondary one.
     */
    private static class FallbackTapMetaReader implements TapMetaReader {
        private TapMetaReader reader_;
        private Boolean isWorking_;
        final Supplier<TapMetaReader> fallbackReaderSupplier_;

        /**
         * Constructor.
         *
         * @param  preferredReader   preferred reader instance
         * @param  fallbackReaderSupplier  supplier for backup instance
         */
        FallbackTapMetaReader( TapMetaReader preferredReader,
                               Supplier<TapMetaReader> fallbackReaderSupplier ){
            reader_ = preferredReader;
            fallbackReaderSupplier_ = fallbackReaderSupplier;
        }

        public String getMeans() {
            return reader_.getMeans();
        }
        public String getSource() {
            return reader_.getSource();
        }
        public SchemaMeta[] readSchemas() throws IOException {
            return fallbackRead( rdr -> rdr.readSchemas() );
        }
        public TableMeta[] readTables( SchemaMeta schema ) throws IOException {
            return fallbackRead( rdr -> rdr.readTables( schema ) );
        }
        public ColumnMeta[] readColumns( TableMeta table ) throws IOException {
            return fallbackRead( rdr -> rdr.readColumns( table ) );
        }
        public ForeignMeta[] readForeignKeys( TableMeta table )
                throws IOException {
            return fallbackRead( rdr -> rdr.readForeignKeys( table ) );
        }

        /**
         * Does the work of reading and if necessary falling back to the
         * backup implementation.
         */
        private <T> T[] fallbackRead( IOFunction<TapMetaReader,T[]> readFunc )
                throws IOException {
            TapMetaReader rdr;
            String msg;

            /* On the first occasion only when a read is attempted,
             * prepare to fall back.  If it's already worked once,
             * the issue is probably something wrong that falling back
             * may not fix.  This has to be syncronized to guaranteed that
             * two first calls don't overlap, but in expected usage
             * scenarios concurrent calls wouldn't occur. */
            synchronized ( this ) {
                if ( isWorking_ == null ) {
                    try {
                        T[] result = readFunc.apply( reader_ );
                        isWorking_ = Boolean.TRUE;
                        msg = null;
                        return result;
                    }
                    catch ( IOException e ) {
                        String means1 = reader_.getMeans();
                        reader_ = fallbackReaderSupplier_.get();
                        String means2 = reader_.getMeans();
                        msg = "TAP metadata read failure from " + means1 
                            + "; fallback to " + means2;
                        isWorking_ = Boolean.FALSE;
                    }
                }
                else {
                    msg = null;
                }
                rdr = reader_;
            }
            if ( msg != null ) {
                logger_.warning( msg );
            }

            /* If we get here, either the first call failed and we fell back,
             * or it's not the first call.  Either way, run it and succeed
             * or fail as usual. */
            return readFunc.apply( rdr );
        }
    }
}
