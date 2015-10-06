package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.ContentCoding;

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

    /** Uses the /tables endpoint. */
    public static final TapMetaPolicy TABLESET;

    /** Uses the TAP_SCHEMA tables, with columns on demand. */
    public static final TapMetaPolicy TAPSCHEMA_C;

    /** Uses the TAP_SCHEMA tables, with columns and foreign keys on demand. */
    public static final TapMetaPolicy TAPSCHEMA_CF;

    /** Uses the TAP_SCHEMA tables, all data loaded at once. */
    public static final TapMetaPolicy TAPSCHEMA;

    /** Uses the non-standard VizieR two-level /tables endpoint. */
    public static final TapMetaPolicy VIZIER;

    /** Uses the non-standard proposed VOSI 1.1 two-level /tables endpoint. */
    public static final TapMetaPolicy CADC;

    private static final TapMetaPolicy[] KNOWN_VALUES = {
        AUTO = new TapMetaPolicy( "Auto",
                                  "Chooses a suitable place to get table "
                                + "metadata. "
                                + "Some services may have custom protocols. "
                                + "Otherwise, use the /tables endpoint "
                                + "when there are a moderate number of tables, "
                                + "or TAP_SCHEMA queries if there are many" ) {
            public TapMetaReader createMetaReader( URL serviceUrl,
                                                   ContentCoding coding ) {
                return createAutoMetaReader( serviceUrl, coding, 5000 );
            }
        },
        TABLESET = new TapMetaPolicy( "TableSet",
                                      "Reads all metadata in one go from the "
                                    + "vs:TableSet document at the /tables "
                                    + "endpoint of the TAP service" ) {
            public TapMetaReader createMetaReader( URL serviceUrl,
                                                   ContentCoding coding ) {
                String tablesetUrl = serviceUrl + "/tables";
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new TableSetTapMetaReader( tablesetUrl, fixer, coding );
            }
        },
        TAPSCHEMA_C = createTapSchemaPolicy( "TAP_SCHEMA_C", false, true ),
        TAPSCHEMA_CF =
            createTapSchemaPolicy( "TAP_SCHEMA_CF", false, false ),
        TAPSCHEMA =
            createTapSchemaPolicy( "TAP_SCHEMA", true, false ),
        VIZIER = new TapMetaPolicy( "VizieR",
                                    "Uses TAPVizieR's non-standard two-stage "
                                  + "/tables endpoint" ) {
            public TapMetaReader createMetaReader( URL serviceUrl,
                                                   ContentCoding coding ) {
                String tablesetUrl = serviceUrl + "/tables";
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new VizierTapMetaReader( tablesetUrl, fixer, coding );
            }
        },
        CADC = new TapMetaPolicy( "CADC",
                                  "Uses CADC's non-standard multi-stage "
                                + "/tables endpoint" ) {
            public TapMetaReader createMetaReader( URL serviceUrl,
                                                   ContentCoding coding ) {
                String tablesetUrl = serviceUrl + "/tables";
                CadcTapMetaReader.Config config =
                    CadcTapMetaReader.Config.POPULATE_SCHEMAS;
                return new CadcTapMetaReader( tablesetUrl, config, coding );
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
     * Creates an object capable of acquiring TAP metadata for a TAP service
     * at a given URL.
     *
     * @param    serviceUrl   base URL of the TAP service
     * @param    coding  configures HTTP compression;
     *                   implementations may honour this hint but are not
     *                   required to
     * @return   new metadata reader
     */
    public abstract TapMetaReader createMetaReader( URL serviceUrl,
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
        String descrip = sbuf.toString();
        return new TapMetaPolicy( name, sbuf.toString() ) {
            public TapMetaReader createMetaReader( URL serviceUrl,
                                                   ContentCoding coding ) {
                int maxrec = 99999;
                boolean popSchemas = true;
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new TapSchemaTapMetaReader( serviceUrl.toString(),
                                                   maxrec, coding, popSchemas,
                                                   popTables, fixer,
                                                   preloadKeys );
            }
        };
    }

    /**
     * Returns a TapMetaReader instance for a given service with policy
     * determined by the apparent size of the metadata set.
     *
     * @param  serviceUrl   TAP service URL
     * @param    coding  configures HTTP compression
     * @param  maxrow     maximum number of records to tolerate from a single
     *                    TAP metadata query 
     */
    private static TapMetaReader createAutoMetaReader( URL serviceUrl,
                                                       ContentCoding coding,
                                                       int maxrow ) {
        MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
 
        /* Find out how many columns there are in total.
         * The columns table is almost certainly the longest one we would
         * have to cope with.  In principle it could be the foreign key table,
         * but that seems very unlikely. */
        long ncol = readRowCount( serviceUrl,
                                  TapSchemaInterrogator.COLUMN_QUERIER
                                                       .getTableName() );

        /* If there are more columns than the threshold, read the tables
         * now and defer column reads until later. */
        if ( ncol >= 0 && ncol > maxrow ) {
            logger_.info( "Many columns in TAP service ("
                        + ncol + " > " + maxrow + "); "
                        + "use TAP_SCHEMA queries for " + serviceUrl );
            String linkTableName =
                TapSchemaInterrogator.LINK_QUERIER.getTableName();
            long nlink = readRowCount( serviceUrl, linkTableName );
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
            boolean popTable = false;
            return new TapSchemaTapMetaReader( serviceUrl.toString(), maxrec,
                                               coding, popSchema, popTable,
                                               fixer, preloadFkeys );
        }
        else {
            logger_.info( "Not excessive column count for TAP service ("
                        + ncol + " <= " + maxrow + ")" );
        }

        /* If there are fewer columns than the threshold, or the column
         * count failed, use the VOSI /tables endpoint instead. */
        logger_.info( "Use /tables endpoint for " + serviceUrl );
        return new TableSetTapMetaReader( serviceUrl + "/tables",
                                          fixer, coding );
    }

    /**
     * Return the number of rows in a named TAP table.
     * In case of failure, a logging message is issued and -1 is returned.
     *
     * @param   serviceUrl  TAP service URL
     * @param   tableName  fully qualified ADQL table name
     * @return   number of rows in table, or -1
     */
    private static long readRowCount( URL serviceUrl, String tableName ) {
        String adql = "SELECT COUNT(*) AS nrow FROM " + tableName;
        Number nrow;
        try {
            nrow = TapQuery.scalarQuery( serviceUrl, adql, Number.class );
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
}
