package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Utility class to aid with implementation of ConeSearcher classes
 * based on contact with remote Data Access Layer-type services.
 * A new instance of this class should be used for each set of related
 * calls to a given service.
 *
 * @author   Mark Taylor
 * @since    23 Sep 2009
 */
public class DalConeSearcher {

    private final boolean believeEmpty_;
    private final String stdName_;
    private final String stdVers_;
    private Class[] colTypes_;
    private Boolean typesFromEmpty_;
    private boolean warned_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param   stdName  name of the DAL standard (used in user messages)
     * @param   stdVers  version of the DAL standard (used in user messages)
     * @param   believeEmpty  whether empty tables are considered to
     *          contain correct metadata
     */
    public DalConeSearcher( String stdName, String stdVers,
                            boolean believeEmpty ) {
        believeEmpty_ = believeEmpty;
        stdName_ = stdName;
        stdVers_ = stdVers;
    }

    /**
     * Takes a table which is the result of a query to the service handled
     * by this searcher, and returns a table which has compatible column
     * structure to any tables returned by previous calls to this method.
     * If the given table looks inconsistent (different number or type of
     * columns), an exception may be thrown or null returned instead.
     *
     * @param  table  candidate table
     * @return   same table if it's consistent, or possibly null
     */
    protected StarTable getConsistentTable( StarTable table )
            throws IOException {

        /* Check if the table has zero rows. */
        table = Tables.randomTable( table );
        boolean isEmpty = isEmpty( table );

        /* If it does, and if zero-row tables are not trusted, return null
         * (since there's no data, and the metadata might be wrong). */
        if ( isEmpty && ! believeEmpty_ ) {
            return null;
        }

        /* Get the array of column data types from the result table. */
        Class[] ctypes = getColumnTypes( table );

        /* If we haven't encountered reliable column metadata before now,
         * set them from the values we have. */
        if ( colTypes_ == null ) {
            colTypes_ = ctypes;
            typesFromEmpty_ = Boolean.valueOf( isEmpty );
        }
        assert colTypes_ != null;
        assert typesFromEmpty_ != null;

        /* Find out if the columns for this table are consistent with those
         * previously obtained. */
        boolean isConsistent = Arrays.equals( colTypes_, ctypes );

        /* If consistent, just return the table. */
        if ( isConsistent ) {
            return table;
        }

        /* If not, throw an exception and possibly issue a one-time warning.
         * The point of the warning is that the exception may get caught
         * and ignored (erract param). */
        else {
            if ( typesFromEmpty_ && ! isEmpty ) {
                String message = "Non-empty and empty queries to the same "
                               + "service seem to return incompatible tables"
                               + " - " + getInconsistentEmptyAdvice();
                if ( ! warned_ ) {
                    logger_.warning( message );
                }
                throw new IOException( message );
            }
            else {
                throw new IOException( "Different queries to the same service "
                                     + "return incompatible tables" );
            }
        }
    }

    /**
     * Returns the column index for a column in a given table identified 
     * with the UCD POS_EQ_RA_MAIN.  If it can't be done, some attempt is made
     * to warn and guess an alternative.
     *
     * @param   table  table
     * @return   index of RA column, or -1 if it can't be found
     */
    public int getUcd1RaIndex( StarTable table ) {
        String raUcd1 = "POS_EQ_RA_MAIN";
        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            if ( raUcd1.equals( info.getUCD() ) ) {
                if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                    return icol;
                }
                else {
                    logger_.warning( "Non-numeric " + raUcd1 + " column" );
                }
            }
        }

        /* No UCD1-style RA. */
        logger_.warning( "No " + raUcd1 + " column"
                      + " (service violates " + stdName_ + " " + stdVers_ 
                      + " standard) - clutching at straws" );
        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                String ucd = info.getUCD();
                String name = info.getName();
                if ( ucd != null && ucd.startsWith( "pos.eq.ra" ) ) {
                    return icol;
                }
                else if ( name != null &&
                          ( name.equalsIgnoreCase( "ra" ) ||
                            name.equalsIgnoreCase( "ra2000" ) ) ) {
                    return icol;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the column index for a column in a given table identified
     * with the UCD POS_EQ_DEC_MAIN.  If it can't be done, some attempt is made
     * to warn and guess an alternative.
     *
     * @param   table  table
     * @return  index of Dec column, or -1 if it can't be found
     */
    public int getUcd1DecIndex( StarTable table ) {
        String decUcd1 = "POS_EQ_DEC_MAIN";
        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            if ( decUcd1.equals( info.getUCD() ) ) {
                if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                    return icol;
                }
                else {
                    logger_.warning( "Non-numeric " + decUcd1 + " column" );
                }
            }
        }

        /* No UCD1-style Dec. */
        logger_.warning( "No " + decUcd1 + " column"
                      + " (service violates " + stdName_ + " " + stdVers_
                      + " standard) - clutching at straws" );
        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                String ucd = info.getUCD();
                String name = info.getName();
                if ( ucd != null && ucd.startsWith( "pos.eq.dec" ) ) {
                    return icol;
                }
                else if ( name != null &&
                          ( name.equalsIgnoreCase( "dec" ) ||
                            name.equalsIgnoreCase( "dec2000" ) ) ) {
                    return icol;
                }
            }
        }
        return -1;
    }

    /**
     * Returns implementation-specific advice to the user about how to
     * swich off trusting the metadata of zero-row tables.
     * This is issued to the user in the event that zero-row tables are
     * trusted (<code>believeEmpty==true</code>), but subsequent results
     * make it look like they shouldn't be.
     *
     * @return   warning message
     */
    protected String getInconsistentEmptyAdvice() {
        return "try believeEmpty=false";
    }

    /**
     * Determines whether a table is empty (has no rows).
     *
     * @param  table  table to test
     * @return  true iff table has no rows
     */
    private static boolean isEmpty( StarTable table ) throws IOException {
        long nr = table.getRowCount();
        if ( nr >= 0 ) {
            return nr == 0;
        }
        else {
            RowSequence rseq = table.getRowSequence();
            try {
                return rseq.next();
            }
            finally {
                rseq.close();
            }
        }
    }

    /**
     * Assembles and returns an array of the content type for each column
     * in a table.
     *
     * @param  table  table
     * @return   ncol-element array of column content classes
     */
    private static Class[] getColumnTypes( StarTable table ) {
        int ncol = table.getColumnCount();
        Class[] types = new Class[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            types[ icol ] = table.getColumnInfo( icol ).getContentClass();
        }
        return types;
    }
}
