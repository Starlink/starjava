package uk.ac.starlink.ttools.task;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.vo.RegistryInterrogator;
import uk.ac.starlink.vo.RegistryQuery;
import uk.ac.starlink.vo.RegistryStarTable;

/**
 * Performs a registry query.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2006
 */
public class RegQuery extends ConsumerTask {

    private final Parameter queryParam_;
    private final Parameter urlParam_;
    private final static String ALL_RECORDS = "ALL";

    /**
     * Constructor.
     */
    public RegQuery() {
        super( "Queries the VO registry", new ChoiceMode(), true );
        List paramList = new ArrayList();

        queryParam_ = new Parameter( "query" );
        queryParam_.setPrompt( "Text of registry query" );
        queryParam_.setDescription( new String[] {
            "Text of an SQL WHERE clause defining which resource records",
            "you wish to retrieve from the registry.",
            "Some examples are:",
            "<ul>",
            "<li><code>serviceType='CONE'</code></li>",
            "<li><code>title like '%2MASS%'</code></li>",
            "<li><code>publisher like 'CDS%' and title like "
                    + "'%galax%'</code></li>",
            "</ul>",
            "The special value \"ALL\" will attempt to retrieve all the",
            "records in the registry",
            "(though this is not necessarily a sensible thing to do).",
            "A full description of SQL syntax is beyond the scope of this",
            "documentation, but in general you want to use",
            "<code>&lt;field-name&gt; like '&lt;value&gt;</code>",
            "where '<code>%</code>' is a wildcard character.",
            "Logical operators <code>and</code> and <code>or</code> and",
            "parentheses can be used to group and combine expressions.",
            "You can find the various <code>&lt;field-name&gt;</code>s",
            "by executing one of the queries above and looking at the",
            "column names in the returned table.",
        } );
        paramList.add( queryParam_ );

        urlParam_ = new Parameter( "regurl" );
        urlParam_.setPrompt( "URL of registry service" );
        urlParam_.setDefault( RegistryInterrogator.DEFAULT_URL.toString() );
        urlParam_.setDescription( new String[] {
            "The URL of a SOAP endpoint which provides suitable",
            "registry query services.",
        } );
        paramList.add( urlParam_ );

        getParameterList().addAll( 0, paramList );
    }

    protected TableProducer createProducer( Environment env )
            throws TaskException {
                String queryText = queryParam_.stringValue( env );
        if ( ALL_RECORDS.toUpperCase()
            .equals( queryText.trim().toUpperCase() ) ) {
            queryText = null;
        }
        String urlText = urlParam_.stringValue( env );
        URL regURL;
        try {
            regURL = new URL( urlText );
        }
        catch ( MalformedURLException e ) {
            throw new ParameterValueException( urlParam_, "Bad URL: " + urlText,
                                               e );
        }
        final RegistryQuery query = new RegistryQuery( regURL, queryText );
        return new TableProducer() {
            public StarTable getTable() throws TaskException {
                try {
                    return new RegistryStarTable( query );
                }
                catch ( Exception e ) {
                    throw new ExecutionException( "Query failed: "
                                                + e.getMessage(), e );
                }
            }
        };
    }
};
