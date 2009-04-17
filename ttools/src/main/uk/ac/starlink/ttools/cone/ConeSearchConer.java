package uk.ac.starlink.ttools.cone;

import uk.ac.starlink.table.StarTableFactory;
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
        final boolean believeEmpty = believeemptyParam_.booleanValue( env );
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
        return new ServiceConeSearcher( new ConeSearch( url ), verb,
                                        believeEmpty, tfact ) {
            protected String getInconsistentResultsWarning() {
                String msg = "Different queries to the same cone search"
                           + " return incompatible tables";
                if ( believeEmpty ) {
                    msg += " - try " + BELIEVE_EMPTY_NAME + "=false";
                }
                return msg;
            }
        };
    }
}
