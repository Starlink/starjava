package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.Comparator;
import uk.ac.starlink.table.ColumnInfo;
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
            "<p>Note that the <ref id='regquery'><code>regquery</code></ref>",
            "command can be used to locate the service URL for cone search",
            "services.",
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
            return csearch_.performSearch( ra, dec, sr, verb_, tfact_ );
        }

        public int getRaIndex( StarTable result ) {
            for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
                if ( "POS_EQ_RA_MAIN".equals( result.getColumnInfo( icol )
                                                    .getUCD() ) ) {
                    return icol;
                }
            }
            return -1;
        }

        public int getDecIndex( StarTable result ) {
            for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
                if ( "POS_EQ_DEC_MAIN".equals( result.getColumnInfo( icol )
                                                     .getUCD() ) ) {
                    return icol;
                }
            }
            return -1;
        }
    }
}
