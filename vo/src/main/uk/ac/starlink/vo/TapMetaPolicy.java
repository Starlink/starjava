package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /** Uses the TAP_SCHEMA tables. */
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
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                return createAutoMetaReader( serviceUrl, 5000 );
            }
        },
        TABLESET = new TapMetaPolicy( "TableSet",
                                      "Reads all metadata in one go from the "
                                    + "vs:TableSet document at the /tables "
                                    + "endpoint of the TAP service" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                String tablesetUrl = serviceUrl + "/tables";
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new TableSetTapMetaReader( tablesetUrl, fixer );
            }
        },
        TAPSCHEMA = new TapMetaPolicy( "TAP_SCHEMA",
                                       "Reads metadata as required by making "
                                     + "synchronous queries on the TAP_SCHEMA "
                                     + "tables in the TAP service" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                int maxrec = 99999;
                boolean popSchemas = true;
                boolean popTables = false;
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new TapSchemaTapMetaReader( serviceUrl.toString(),
                                                   maxrec, popSchemas,
                                                   popTables, fixer );
            }
        },
        VIZIER = new TapMetaPolicy( "VizieR",
                                    "Uses TAPVizieR's non-standard two-stage "
                                  + "/tables endpoint" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                String tablesetUrl = serviceUrl + "/tables";
                MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();
                return new VizierTapMetaReader( tablesetUrl, fixer );
            }
        },
        CADC = new TapMetaPolicy( "CADC",
                                  "Uses CADC's non-standard multi-stage "
                                + "/tables endpoint" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                String tablesetUrl = serviceUrl + "/tables";
                return new CadcTapMetaReader( tablesetUrl );
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
     * @return   new metadata reader
     */
    public abstract TapMetaReader createMetaReader( URL serviceUrl );

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
     * Returns a TapMetaReader instance for a given service with policy
     * determined by the apparent size of the metadata set.
     *
     * @param  serviceUrl   TAP service URL
     * @param  maxrow     maximum number of records to tolerate from a single
     *                    TAP metadata query 
     */
    private static TapMetaReader createAutoMetaReader( URL serviceUrl,
                                                       int maxrow ) {
        MetaNameFixer fixer = MetaNameFixer.createDefaultFixer();

        /* Special non-standard protocol for TAPVizieR. */
        if ( VizierTapMetaReader.isVizierTapService( serviceUrl ) ) {
            logger_.info( "Using VizieR-specific metadata acquisition" );
            return new VizierTapMetaReader( serviceUrl + "/tables", fixer ); 
        }

        /* Proposed VOSI 1.1 protocol implemented at CADC. */
        if ( CadcTapMetaReader.isCadcTapService( serviceUrl ) ) {
            logger_.info( "Using CADC-specific metadata acquisition" );
            return new CadcTapMetaReader( serviceUrl + "/tables" );
        }
 
        /* The columns table is almost certainly the longest one we would
         * have to cope with.  In principle it could be the foreign key table,
         * but that seems very unlikely. */
        final String colTableName = "TAP_SCHEMA.columns";

        /* Find out how many columns there are in total. */
        try {
            long ncol = readRowCount( serviceUrl, colTableName );

            /* If there are more columns than the threshold, read the tables
             * now and defer column reads until later. */
            if ( ncol > maxrow ) {
                logger_.info( "Many columns in TAP service ("
                            + ncol + " > " + maxrow + "); "
                            + "use TAP_SCHEMA queries for " + serviceUrl );
                int maxrec = (int) Math.min( Integer.MAX_VALUE, ncol + 1 );
                return new TapSchemaTapMetaReader( serviceUrl.toString(),
                                                   maxrec, true, false, fixer );
            }
            else {
                logger_.info( "Not excessive column count for TAP service ("
                            + ncol + " <= " + maxrow + ")" );
            }
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING,
                         "Row count for " + colTableName + " failed: " + e,
                         e );
        }

        /* If there are fewer columns than the threshold, or the column
         * count failed, use the VOSI /tables endpoint instead. */
        logger_.info( "Use /tables endpoint for " + serviceUrl );
        return new TableSetTapMetaReader( serviceUrl + "/tables", fixer );
    }

    /**
     * Return the number of rows in a named TAP table.
     *
     * @param   serviceUrl  TAP service URL
     * @param   tableName  fully qualified ADQL table name
     * @return   number of rows in table
     * @throws   IOException   if the row count cannot be determined
     *                         for some reason
     */
    private static long readRowCount( URL serviceUrl, String tableName )
            throws IOException {
        String adql = "SELECT COUNT(*) AS nrow FROM " + tableName;
        Number nrow = TapQuery.scalarQuery( serviceUrl, adql, Number.class );
        if ( nrow != null ) {
            return nrow.longValue();
        }
        else {
            throw new IOException( "No count result" );
        }
    }
}
