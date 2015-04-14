package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;

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

    /** Uses the /tables endpoint. */
    public static final TapMetaPolicy TABLESET;

    /** Uses the TAP_SCHEMA tables. */
    public static final TapMetaPolicy TAPSCHEMA;

    /** Tries its best to do something sensible. */
    public static final TapMetaPolicy ADAPTIVE;
 
    private static final TapMetaPolicy[] KNOWN_VALUES = {
        TABLESET = new TapMetaPolicy( "TableSet",
                                      "Reads all metadata in one go from the "
                                    + "vs:TableSet document at the /tables "
                                    + "endpoint of the TAP service" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                return new TableSetTapMetaReader( serviceUrl + "/tables" );
            }
        },
        TAPSCHEMA = new TapMetaPolicy( "TAP_SCHEMA",
                                       "Reads metadata as required by making "
                                     + "synchronous queries on the TAP_SCHEMA "
                                     + "tables in the TAP service" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                return new TapSchemaTapMetaReader( serviceUrl.toString(), 99999,
                                                   true, false );
            }
        },
        ADAPTIVE = new TapMetaPolicy( "ADAPTIVE",
                                      "Uses the /tables endpoint if there are "
                                    + "a moderate number of tables, or "
                                    + "TAP_SCHEMA queries if there are many" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                return createAdaptiveMetaReader( serviceUrl, 5000 );
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
        return ADAPTIVE;
    }

    /**
     * Returns a TapMetaReader instance for a given service with policy
     * determined by the apparent size of the metadata set.
     *
     * @param  serviceUrl   TAP service URL
     * @param  maxrow     maximum number of records to tolerate from a single
     *                    TAP metadata query 
     */
    private static TapMetaReader createAdaptiveMetaReader( URL serviceUrl,
                                                           int maxrow ) {
 
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
                                                   maxrec, true, false );
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
        return new TableSetTapMetaReader( serviceUrl + "/tables" );
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
        TapQuery tq = new TapQuery( serviceUrl, adql, null );
        StarTable result = tq.executeSync( StoragePolicy.PREFER_MEMORY );
        result = Tables.randomTable( result );
        if ( result.getRowCount() == 1 && result.getColumnCount() == 1 ) {
            Object cell = result.getCell( 0, 0 );
            if ( cell instanceof Number ) {
                return ((Number) cell).longValue();
            }
            else {
                throw new IOException( "Count result not numeric" );
            }
        }
        else {
            throw new IOException( "Count result not unique ("
                                 + result.getRowCount() + "x"
                                 + result.getColumnCount() );
        }
    }
}
