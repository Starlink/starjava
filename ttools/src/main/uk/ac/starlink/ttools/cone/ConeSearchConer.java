package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.vo.ConeSearch;

/**
 * Coner implementation which uses remote 
 * <a href="http://www.ivoa.net/Documents/latest/ConeSearch.html"
 *    >Cone Search</a> services.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2007
 */
public class ConeSearchConer implements Coner {

    private final Parameter urlParam_;
    private final ChoiceParameter verbParam_;
    private final BooleanParameter believeemptyParam_;
    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );
    private static final String BELIEVE_EMPTY_NAME = "emptyok";

    /**
     * Constructor.
     */
    public ConeSearchConer() {
        urlParam_ = new Parameter( "serviceurl" );
        urlParam_.setPrompt( "Base URL for query returning VOTable" );
        urlParam_.setDescription( new String[] {
            "<p>The base part of a URL which defines the queries to be made.",
            "Additional parameters will be appended to this using CGI syntax",
            "(\"<code>name=value</code>\", separated by '&amp;' characters).",
            "If this value does not end in either a '?' or a '&amp;',",
            "one will be added as appropriate.",
            "</p>",
            "<p>See <ref id='coneService'/> for discussion of how to locate",
            "service URLs corresponding to given datasets.",
            "</p>",
        } );

        verbParam_ = new ChoiceParameter( "verb",
                                          new String[] { "1", "2", "3", } );
        verbParam_.setNullPermitted( true );
        verbParam_.setPrompt( "Verbosity level of search responses (1..3)" );
        verbParam_.setDescription( new String[] {
            "<p>Verbosity level of the tables returned by the query service.",
            "A value of 1 indicates the bare minimum and",
            "3 indicates all available information.",
            "</p>",
        } );

        believeemptyParam_ = new BooleanParameter( BELIEVE_EMPTY_NAME );
        believeemptyParam_.setDefault( "true" );
        believeemptyParam_.setPrompt( "Believe metadata from empty results?" );
        believeemptyParam_.setDescription( new String[] {
            "<p>Whether the table metadata which is returned from a search",
            "result with zero rows is to be believed.",
            "According to the spirit, though not the letter, of the",
            "cone search standard, a cone search service which returns no data",
            "ought nevertheless to return the correct column headings.",
            "Unfortunately this is not always the case.",
            "If this parameter is set <code>true</code>, it is assumed",
            "that the service behaves properly in this respect; if it does not",
            "an error may result.  In that case, set this parameter",
            "<code>false</code>.  A consequence of setting it false is that",
            "in the event of no results being returned, the task will",
            "return no table at all, rather than an empty one.",
            "</p>",
        } );
    }

    /**
     * Returns "ICRS", which is the system defined to be used by the
     * Cone Search specification.
     */
    public String getSkySystem() {
        return "ICRS";
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            urlParam_,
            verbParam_,
            believeemptyParam_,
        };
    }

    public ConeSearcher createSearcher( Environment env, boolean bestOnly )
            throws TaskException {
        String url;
        try {
            url = urlParam_.stringValue( env );
        }
        catch ( IllegalArgumentException e ) {
            throw new ParameterValueException( urlParam_, e.getMessage(), e );
        }

        String sverb = verbParam_.stringValue( env );
        boolean believeEmpty = believeemptyParam_.booleanValue( env );
        int verb;
        if ( sverb == null ) {
            verb = -1;
        }
        else {
            try {
                verb = Integer.parseInt( sverb );
            }
            catch ( NumberFormatException e ) {
                assert false;
                throw new ParameterValueException( verbParam_,
                                                   e.getMessage(), e );
            }
        }
        StarTableFactory tfact = LineTableEnvironment.getTableFactory( env );
        return new ServiceSearcher( new ConeSearch( url ), verb, believeEmpty,
                                    tfact );
    }

    /**
     * ConeSearcher implementation which accesses a remote cone search service.
     */
    private static class ServiceSearcher implements ConeSearcher {
        private final ConeSearch csearch_;
        private final int verb_;
        private final boolean believeEmpty_;
        private final StarTableFactory tfact_;
        private Class[] colTypes_;
        private boolean warned_;

        /**
         * Constructor.
         *
         * @param   csearch  cone search service specification object
         * @param   verb  verbosity parameter
         * @param   believeEmpty  whether empty tables are considered to
         *          contain correct metadata
         * @param   tfact  table factory
         */
        ServiceSearcher( ConeSearch csearch, int verb, boolean believeEmpty,
                         StarTableFactory tfact ) {
            csearch_ = csearch;
            verb_ = verb;
            believeEmpty_ = believeEmpty;
            tfact_ = tfact;
        }

        public StarTable performSearch( double ra, double dec, double sr ) 
                throws IOException {
            StarTable table = 
                csearch_.performSearch( ra, dec, sr, verb_, tfact_ );
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
                    if ( ! Arrays.equals( colTypes_,
                                          getColumnTypes( result ) ) ) {
                        String msg = "Different queries to the same cone search"
                                   + " return incompatible tables";
                        if ( believeEmpty_ ) {
                            msg += " - try " + BELIEVE_EMPTY_NAME + "=false";
                        }
                        logger.warning( msg );
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
                    if ( Number.class
                               .isAssignableFrom( info.getContentClass() ) ) {
                        return icol;
                    }
                    else {
                        logger.warning( "Non-numeric POS_EQ_RA_MAIN column" );
                    }
                }
            }

            /* No UCD1-style RA, as mandated in the standard. */
            logger.warning( "No POS_EQ_RA_MAIN column"
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
                    if ( Number.class
                               .isAssignableFrom( info.getContentClass() ) ) {
                        return icol;
                    }
                    else {
                        logger.warning( "Non-numeric POS_EQ_DEC_MAIN column" );
                    }
                }
            }

            /* No UCD1-style Dec, as mandated in the standard. */
            logger.warning( "No POS_EQ_DEC_MAIN column"
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
}
