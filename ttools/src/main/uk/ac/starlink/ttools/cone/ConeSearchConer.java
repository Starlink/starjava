package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.Comparator;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
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
    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

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
        return new ServiceSearcher( new ConeSearch( url ), verb, tfact );
    }

    /**
     * ConeSearcher implementation which accesses a remote cone search service.
     */
    private static class ServiceSearcher implements ConeSearcher {
        private final ConeSearch csearch_;
        private final int verb_;
        private final StarTableFactory tfact_;

        /**
         * Constructor.
         *
         * @param   csearch  cone search service specification object
         * @param   verb  verbosity parameter
         * @param   tfact  table factory
         */
        ServiceSearcher( ConeSearch csearch, int verb,
                         StarTableFactory tfact ) {
            csearch_ = csearch;
            verb_ = verb;
            tfact_ = tfact;
        }

        public StarTable performSearch( double ra, double dec, double sr ) 
                throws IOException {
            StarTable table = 
                csearch_.performSearch( ra, dec, sr, verb_, tfact_ );

            /* If the table has no rows, return null.  This is slightly 
             * annoying, since it would be better to return a row-less table
             * (see performSearch contract), but some cone search services
             * tend to return tables with different numbers of columns when
             * there are no rows.  This is against the spirit of the Cone
             * Search standard, but not against the letter. */
            return isEmpty( table ) ? null : table;
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
    }
}
