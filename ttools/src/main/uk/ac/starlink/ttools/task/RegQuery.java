package uk.ac.starlink.ttools.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.registry.SoapClient;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.util.Destination;
import uk.ac.starlink.vo.Ri1RegistryQuery;
import uk.ac.starlink.vo.RegistryStarTable;

/**
 * Performs a registry query.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2006
 */
public class RegQuery extends ConsumerTask {

    private final StringParameter queryParam_;
    private final URLParameter urlParam_;
    private final OutputStreamParameter soapoutParam_;
    private final static String ALL_RECORDS = "ALL";

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public RegQuery() {
        super( "Queries the VO registry", new ChoiceMode(), true );
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

        queryParam_ = new StringParameter( "query" );
        queryParam_.setPrompt( "Text of registry query" );
        queryParam_.setDescription( new String[] {
            "<p>Text of an ADQL WHERE clause targeted at the",
            "<webref url='http://www.ivoa.net/Documents/cover/"
                       + "VOResource-20080222.html'>VOResource 1.0</webref>",
            "schema defining which resource records",
            "you wish to retrieve from the registry.",
            "Some examples are:",
            "<ul>",
            "<li><code>@xsi:type like '%Organisation%'</code></li>",
            "<li><code>capability/@standardID = 'ivo://ivoa.net/std/ConeSearch'"
                    + " and title like '%SDSS%'</code></li>",
            "<li><code>curation/publisher like 'CDS%'"
                    + " and title like '%galax%'</code></li>",
            "</ul>",
            "</p>",
            "<p>A full description of ADQL syntax and of the VOResource schema",
            "is well beyond the scope of this",
            "documentation, but in general you want to use",
            "<code>&lt;field-name&gt; like '&lt;value&gt;'</code>",
            "where '<code>%</code>' is a wildcard character.",
            "Logical operators <code>and</code> and <code>or</code> and",
            "parentheses can be used to group and combine expressions.",
            "To work out the various <code>&lt;field-name&gt;</code>s",
            "you need to look at the VOResource 1.0 schema.",
            "</p>",
        } );
        paramList.add( queryParam_ );

        urlParam_ = new URLParameter( "regurl" );
        urlParam_.setPrompt( "URL of registry service" );
        urlParam_.setStringDefault( Ri1RegistryQuery.AG_REG );
        urlParam_.setDescription( new String[] {
            "<p>The URL of a SOAP endpoint which provides",
            "a VOResource1.0 IVOA registry service.",
            "Some known suitable registry endpoints at time of writing are",
            "<ul>",
            "<li><code>" + Ri1RegistryQuery.AG_REG + "</code></li>",
            "<li><code>" + Ri1RegistryQuery.EUROVO_REG + "</code></li>",
            "<li><code>" + Ri1RegistryQuery.VAO_REG + "</code></li>",
            "</ul>",
            "</p>",
        } );
        paramList.add( urlParam_ );

        soapoutParam_ = new OutputStreamParameter( "soapout" );
        soapoutParam_.setNullPermitted( true );
        soapoutParam_.setStringDefault( null );
        soapoutParam_.setPrompt( "SOAP message destination stream" );
        soapoutParam_.setDescription( new String[] {
            "<p>If set to a non-null value, this gives the destination",
            "for the text of the request and response SOAP messages.",
            "The special value \"-\" indicates standard output.",
            "</p>",
        } );
        paramList.add( soapoutParam_ );

        getParameterList().addAll( 0, paramList );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        String queryText = queryParam_.stringValue( env );
        if ( ALL_RECORDS.toUpperCase()
            .equals( queryText.trim().toUpperCase() ) ) {
            queryText = null;
        }
        final String qText = queryText;
        final URL regURL = urlParam_.objectValue( env );
        final Destination soapdest = soapoutParam_.objectValue( env );
        return new TableProducer() {
            public StarTable getTable() throws TaskException {
                try {
                    SoapClient soapClient = new SoapClient( regURL );
                    if ( soapdest != null ) {
                        soapClient.setEchoStream( soapdest.createStream() );
                    }
                    Ri1RegistryQuery query =
                        new Ri1RegistryQuery( soapClient, qText );
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
