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
     * If the given table looks different (different number or type of
     * columns), null is returned instead.
     * A one-time warning is issued throgh the logging system if an
     * inconsistent table is encountered.
     *
     * @param  table  candidate table
     * @return   same table if it's consistent, else null
     */
    protected StarTable getConsistentTable( StarTable table )
            throws IOException {
        table = Tables.randomTable( table );
        StarTable result = ( believeEmpty_ || ! isEmpty( table ) )
                         ? table
                         : null;

        /* Check for consistency of columns between different calls.
         * The main point of this is so that we can suggest adjusting
         * the believeEmpty parameter if appropriate; it is *not* a
         * good idea to throw an error here in response to inconsistencies.
         * That is because (a) it's checked downstream of here and
         * (b) an error here might look like an error in the service 
         * resolution itself and be ignored accordingly (erract param). */
        if ( result != null && ! warned_ ) {
            if ( colTypes_ == null ) {
                colTypes_ = getColumnTypes( result );
            }
            else {
                if ( ! Arrays.equals( colTypes_, getColumnTypes( result ) ) ) {
                    logger_.warning( getInconsistentResultsWarning() );
                    warned_ = true;
                }
            }
        }
        return result;
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
     * Returns the warning message to be output in the case that
     * subsequent accesses to the same service return
     * incompatible tables (ones with different columns).
     * This most commonly happens when the table has no rows, and
     * the server doesn't bother to send columns either.
     *
     * @return   warning message
     */
    protected String getInconsistentResultsWarning() {
        String msg = "Different queries to the same " + stdName_
                   + " service return incompatible tables";
        if ( believeEmpty_ ) {
            msg += " - try believeEmpty=false";
        }
        return msg;
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
