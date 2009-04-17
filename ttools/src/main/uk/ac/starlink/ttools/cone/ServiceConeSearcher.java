package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.vo.ConeSearch;

/**
 * ConeSearcher implementation using a VO 
 * <a href="http://www.ivoa.net/Documents/latest/ConeSearch.html"
 *    >Cone Search</a> service.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2009
 */
public class ServiceConeSearcher implements ConeSearcher {

    private final ConeSearch csearch_;
    private final int verb_;
    private final boolean believeEmpty_;
    private final StarTableFactory tfact_;
    private Class[] colTypes_;
    private boolean warned_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param   csearch  cone search service specification object
     * @param   verb  verbosity parameter
     * @param   believeEmpty  whether empty tables are considered to
     *          contain correct metadata
     * @param   tfact  table factory
     */
    public ServiceConeSearcher( ConeSearch csearch, int verb,
                                boolean believeEmpty, StarTableFactory tfact ) {
        csearch_ = csearch;
        verb_ = verb;
        believeEmpty_ = believeEmpty;
        tfact_ = tfact;
    }

    public StarTable performSearch( double ra, double dec, double sr )
            throws IOException {
        StarTable table = csearch_.performSearch( ra, dec, sr, verb_, tfact_ );
        table = Tables.randomTable( table );
        StarTable result = ( believeEmpty_ || ! isEmpty( table ) )
                         ? table
                         : null;

        /* Check for consistency of columns between different calls.
         * The main point of this is so that we can suggest adjusting
         * the believeEmpty parameter if appropriate; it is *not* a
         * good idea to throw an error here in response to inconsistencies.
         * That is because (a) it's checked downstream of here and
         * (b) an error here might look like an error in the cone search
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

    public int getRaIndex( StarTable result ) {
        for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
            ColumnInfo info = result.getColumnInfo( icol );
            if ( "POS_EQ_RA_MAIN".equals( info.getUCD() ) ) {
                if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                    return icol;
                }
                else {
                    logger_.warning( "Non-numeric POS_EQ_RA_MAIN column" );
                }
            }
        }

        /* No UCD1-style RA, as mandated in the standard. */
        logger_.warning( "No POS_EQ_RA_MAIN column"
                      + " (service violates SCS 1.02 standard)"
                      + " - clutching at straws" );
        for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
            ColumnInfo info = result.getColumnInfo( icol );
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

    public int getDecIndex( StarTable result ) {
        for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
            ColumnInfo info = result.getColumnInfo( icol );
            if ( "POS_EQ_DEC_MAIN".equals( info.getUCD() ) ) {
                if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                    return icol;
                }
                else {
                    logger_.warning( "Non-numeric POS_EQ_DEC_MAIN column" );
                }
            }
        }

        /* No UCD1-style Dec, as mandated in the standard. */
        logger_.warning( "No POS_EQ_DEC_MAIN column"
                      + " (service violates SCS 1.02 standard)"
                      + " - clutching at straws" );
        for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
            ColumnInfo info = result.getColumnInfo( icol );
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

    public void close() {
    }

    /**
     * Returns the warning message to be output in the case that 
     * subsequent accesses to the same cone search service return
     * incompatible tables (ones with different columns).
     * This most commonly happens when the table has no rows, and
     * the server doesn't bother to send columns either.
     *
     * @return   warning message
     */
    protected String getInconsistentResultsWarning() {
        String msg = "Different queries to the same cone search "
                   + "return incompatible tables";
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
