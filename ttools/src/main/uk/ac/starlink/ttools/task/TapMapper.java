package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.vo.TapQuery;

/**
 * Mapper that does the work for {@link TapQuerier}.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2011
 */
public class TapMapper implements TableMapper {

    private final Parameter urlParam_;
    private final Parameter adqlParam_;
    private final IntegerParameter pollParam_;
    private final BooleanParameter deleteParam_;
    private final Parameter[] params_;
    private final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    public TapMapper() {
        List<Parameter> paramList = new ArrayList<Parameter>();

        urlParam_ = new Parameter( "tapurl" );
        urlParam_.setPrompt( "Base URL of TAP service" );
        urlParam_.setDescription( new String[] {
            "<p>The base URL of a Table Access Protocol service.",
            "This is the bare URL without a trailing \"/async\".",
            "</p>",
        } );
        paramList.add( urlParam_ );

        adqlParam_ = new Parameter( "adql" );
        adqlParam_.setPrompt( "ADQL query text" );
        adqlParam_.setDescription( new String[] {
            "<p>Astronomical Data Query Language string specifying the",
            "TAP query to execute.",
            "ADQL/S resembles SQL, so this string will likely start with",
            "\"SELECT\".",
            "</p>",
        } );
        paramList.add( adqlParam_ );

        pollParam_ = new IntegerParameter( "poll" );
        pollParam_.setPrompt( "Polling interval in milliseconds" );
        int minPoll = 50;
        pollParam_.setMinimum( minPoll );
        pollParam_.setDescription( new String[] {
            "<p>Interval to wait between polling attempts, in milliseconds.",
            "Asynchronous TAP queries can only find out when they are",
            "complete by repeatedly polling the server to find out the",
            "job's status.  This parameter allows you to set how often",
            "that happens.",
            "Attempts to set it too low (&lt;" + minPoll + ")",
            "will be rejected on the assumption that you're thinking in",
            "seconds.",
            "</p>",
        } );
        pollParam_.setMinimum( 50 );
        pollParam_.setDefault( "5000" );
        paramList.add( pollParam_ );

        deleteParam_ = new BooleanParameter( "delete" );
        deleteParam_.setPrompt( "Delete job when complete?" );
        deleteParam_.setDescription( new String[] {
            "<p>If true, the UWS job is deleted when complete.",
            "If false, the job is left on the server, and it can be",
            "access via the normal UWS REST endpoints after the completion",
            "of this command.",
            "</p>",
        } );
        deleteParam_.setDefault( "true" );
        paramList.add( deleteParam_ );

        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {
        String urlText = urlParam_.stringValue( env );
        final URL url;
        try {
            url = new URL( urlText );
        }
        catch ( MalformedURLException e ) {
            throw new ParameterValueException( urlParam_, "Bad URL: " + urlText,
                                               e );
        }
        final String adql = adqlParam_.stringValue( env );
        final int pollMillis = pollParam_.intValue( env );
        final boolean delete = deleteParam_.booleanValue( env );
        final StarTableFactory tfact =
            LineTableEnvironment.getTableFactory( env );
        return new TableMapping() {
            public StarTable mapTables( InputTableSpec[] inSpecs )
                    throws TaskException, IOException {
                Map<String,StarTable> uploadMap =
                    new LinkedHashMap<String,StarTable>();
                int nup = inSpecs.length;
                for ( int iu = 0; iu < nup ;iu++ ) {
                    uploadMap.put( "up" + ( iu + 1 ),
                                   inSpecs[ iu ].getWrappedTable() );
                }
                TapQuery query =
                    TapQuery.createAdqlQuery( url, adql, uploadMap );
                query.start();
                try {
                    return query.waitForResult( tfact, pollMillis, delete );
                }
                catch ( InterruptedException e ) {
                    throw (IOException)
                          new InterruptedIOException( "Interrupted" )
                         .initCause( e );
                }
                finally {
                    if ( ! delete ) {
                        logger_.warning( "UWS job "
                                       + query.getUwsJob().getJobUrl()
                                       + " not deleted" );
                    }
                }
            }
        };
    }
}
