package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.vo.TapQuery;

/**
 * Aggregates parameters used for recovering and delivering the result
 * of a TAP query.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2011
 */
public class TapResultReader {

    private final IntegerParameter pollParam_;
    private final BooleanParameter deleteParam_;
    private final Parameter[] parameters_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    public TapResultReader() {
        List<Parameter> paramList = new ArrayList<Parameter>();
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

        parameters_ = paramList.toArray( new Parameter[ 0 ] );
    }

    /**
     * Returns the parameters associated with this object.
     *
     * @return   parameters
     */
    public Parameter[] getParameters() {
        return parameters_;
    }

    /**
     * Returns an object which can acquire a table from a TAP query object.
     *
     * @param  env  execution environment
     * @return   TAP table producer
     */
    public TapResultProducer createResultProducer( Environment env )
            throws TaskException {
        final int pollMillis = pollParam_.intValue( env );
        final boolean delete = deleteParam_.booleanValue( env );
        final StarTableFactory tfact =
            LineTableEnvironment.getTableFactory( env );
        return new TapResultProducer() {
            public StarTable waitForResult( TapQuery query )
                    throws IOException {
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
