package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.UwsJob;
import uk.ac.starlink.vo.UwsJobInfo;
import uk.ac.starlink.vo.UwsStage;

/**
 * Aggregates parameters used for recovering and delivering the result
 * of a TAP query.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2011
 */
public class TapResultReader {

    private final IntegerParameter pollParam_;
    private final BooleanParameter progressParam_;
    private final ChoiceParameter<DeleteMode> deleteParam_;
    private final Parameter<?>[] parameters_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    public TapResultReader() {
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

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
        pollParam_.setUsage( "<millisec>" );
        pollParam_.setIntDefault( 5000 );
        paramList.add( pollParam_ );

        progressParam_ = new BooleanParameter( "progress" );
        progressParam_.setPrompt( "Report on query progress" );
        progressParam_.setDescription( new String[] {
            "<p>If this parameter is set true, progress of the job is",
            "reported to standard output as it happens.",
            "</p>",
        } );
        progressParam_.setBooleanDefault( true );
        paramList.add( progressParam_ );

        deleteParam_ = new ChoiceParameter<DeleteMode>( "delete",
                                                        DeleteMode.values() );
        deleteParam_.setPrompt( "Delete job on exit?" );
        deleteParam_.setDescription( new String[] {
            "<p>Determines under what circumstances the UWS job is to be",
            "deleted from the server when its data is no longer required.",
            "If it is not deleted, then the job is left on the TAP server",
            "and it can be accessed via the normal UWS REST endpoints",
            "or using <code>tapresume</code>",
            "until it is destroyed by the server.",
            "</p>",
            "<p>Possible values:",
            "<ul>",
            DeleteMode.getListItems(),
            "</ul>",
            "</p>",
        } );
        deleteParam_.setDefaultOption( DeleteMode.finished );
        paramList.add( deleteParam_ );

        parameters_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    /**
     * Returns the parameters associated with this object.
     *
     * @return   parameters
     */
    public Parameter<?>[] getParameters() {
        return parameters_;
    }

    /**
     * Returns the parameter which indicates whether progress should be
     * logged to the user.
     *
     * @return  progress parameter
     */
    public BooleanParameter getProgressParameter() {
        return progressParam_;
    }

    /**
     * Returns the parameter used to acquire the deletion mode for
     * async queries.
     *
     * @return  deletion mode parameter
     */
    public Parameter<DeleteMode> getDeleteParameter() {
        return deleteParam_;
    }

    /**
     * Returns an object which can acquire a table from a TAP query object.
     *
     * @param  env  execution environment
     * @param  coding  configures HTTP compression
     * @return   TAP table producer
     */
    public TapResultProducer createResultProducer( Environment env,
                                                   final ContentCoding coding )
            throws TaskException {
        final int pollMillis = pollParam_.intValue( env );
        final boolean progress = progressParam_.booleanValue( env );
        final PrintStream errStream = env.getErrorStream();
        final DeleteMode delete = deleteParam_.objectValue( env );
        final StarTableFactory tfact =
            LineTableEnvironment.getTableFactory( env );

        /* Most of the complication here is to do with if/when the UWS job
         * corresponding to the query should be deleted.  It's deleted
         * if/when BOTH any non-random table resulting from the query is 
         * no longer in use AND the deletion mode says it's OK.
         * The former condition arises from the fact that the table URL,
         * which lives on the TAP server, may need to get read at any time
         * in the future to stream the data if the data has not been cached
         * locally.  No-longer-in-use-ness of such a table will happen
         * either on table finalization or on JVM exit. */
        return new TapResultProducer() {
            private Thread deleteThread;
            private String lastPhase;

            public StarTable waitForResult( final UwsJob tapJob )
                    throws IOException {
                StoragePolicy storage = tfact.getStoragePolicy();

                /* For the special case deletion mode == now,
                 * delete the job immediately. */
                if ( ! delete.canWait() ) {
                    try {
                        return TapQuery.getResult( tapJob, coding, storage );
                    }
                    catch ( IOException e ) {
                        throw new IOException( "Job not completed, no table" );
                    }
                    finally {
                        considerDeletion( tapJob );
                    }
                }

                /* Otherwise, follow the job's progress. */
                UwsJob.JobWatcher progger = null;
                if ( progress ) {
                    progger = new UwsJob.JobWatcher() {
                        public void jobUpdated( UwsJob job, UwsJobInfo info ) {
                            logPhase( info.getPhase() );
                        }
                    };
                    tapJob.addJobWatcher( progger );
                }
                final StarTable table;
                if ( delete.isDeletionPossible() ) {
                    deleteThread = new Thread( "UWS job deleter" ) {
                        public void run() {
                            considerDeletionOnShutdown( tapJob );
                        }
                    };
                    Runtime.getRuntime().addShutdownHook( deleteThread );
                }
                try {
                    table = TapQuery.waitForResult( tapJob, coding,
                                                    storage, pollMillis );
                }
                catch ( InterruptedException e ) {
                    considerDeletionEarly( tapJob );
                    throw (IOException)
                          new InterruptedIOException( "Interrupted" )
                         .initCause( e );
                }
                catch ( IOException e ) {
                    considerDeletionEarly( tapJob );
                    throw e;
                }
                assert "COMPLETED".equals( tapJob.getLastInfo().getPhase() );
                if ( ! delete.isDeletionPossible() ) {
                    return table;
                }
                else if ( table.isRandom() ) {
                    considerDeletionEarly( tapJob );
                    return table;
                }
                else {
                    return new WrapperStarTable( table ) {
                        boolean isClosed_;
                        @Override
                        public void close() throws IOException {
                            try {
                                if ( !isClosed_ ) {
                                    isClosed_ = true;
                                    considerDeletionEarly( tapJob );
                                }
                            }
                            finally {
                                super.close();
                            }
                        }
                    };
                }
            }

            /**
             * Examines a UWS job, and if suitable for deletion, delete it.
             * Should be called from a non-shutdown hook thread.
             *
             * @param  uwsJob   job to delete
             */
            private void considerDeletionEarly( UwsJob uwsJob ) {
                if ( deleteThread != null ) {
                    Runtime.getRuntime().removeShutdownHook( deleteThread );
                }
                considerDeletion( uwsJob );
            }

            /**
             * Examines a UWS job on JVM shutdown, and if suitable for
             * deletion, deletes it.  Otherwise, informs the user that
             * it's still running.
             * Should be called from a shutdown hook thread.
             *
             * @param  uwsJob   job to delete
             */
            private void considerDeletionOnShutdown( UwsJob uwsJob ) {
                String phase = uwsJob.getLastInfo().getPhase();
                UwsStage stage = UwsStage.forPhase( phase );
                if ( delete.shouldDelete( stage ) ) {
                    uwsJob.attemptDelete();
                    if ( progress ) {
                        errStream.println( "DELETED" );
                        errStream.flush();
                    }
                }
                else if ( stage == UwsStage.RUNNING ||
                          stage == UwsStage.UNSTARTED ) {
                    URL joburl = uwsJob.getJobUrl();
                    errStream.println( "Job still " + phase
                                     + " on server at " + joburl );
                    errStream.println( "Consider aborting it: "
                                     + "stilts tapresume"
                                     + " delete=" + DeleteMode.now
                                     + " joburl='" + joburl + "'" );
                    errStream.flush();
                }
            }

            /**
             * Examine a UWS job, and if it is suitable for deletion,
             * delete it.  May be called from any thread.
             *
             * @param  uwsJob  job to delete
             */
            private void considerDeletion( UwsJob uwsJob ) {
                UwsStage stage =
                    UwsStage.forPhase( uwsJob.getLastInfo().getPhase() );
                if ( delete.shouldDelete( stage ) ) {
                    uwsJob.attemptDelete();
                    if ( progress ) {
                        errStream.println( "DELETED" );
                        errStream.flush();
                    }
                }
            }

            /**
             * Logs the current job phase to standard error in some compact way.
             *
             * @param  phase  UWS job phase
             */
            private void logPhase( String phase ) {
                if ( ! phase.equals( lastPhase ) ) {
                    String txt = phase;
                    if ( UwsStage.forPhase( phase ) != UwsStage.FINISHED ) {
                        txt += " ...";
                    }
                    errStream.println( txt );
                    errStream.flush();
                }
                lastPhase = phase;
            }
        };
    }

    /**
     * Enumeration of UWS job deletion modes.
     */
    public static enum DeleteMode {

        finished( "delete only if the job finished, successfully or not",
                  true, true ) {
            boolean shouldDelete( UwsStage stage ) {
                return stage == UwsStage.FINISHED;
            }
        },
        never( "do not delete", false, true ) {
            boolean shouldDelete( UwsStage stage ) {
                return false;
            }
        },
        always( "delete on command exit", true, true ) {
            boolean shouldDelete( UwsStage stage ) {
                return true;
            }
        },
        now( "delete and return immediately", true, false ) {
            boolean shouldDelete( UwsStage stage ) {
                return true;
            }
        };

        private final String description_;
        private final boolean isDeletionPossible_;
        private final boolean canWait_;

        /**
         * Constructor.
         *
         * @param  description   short XML description
         * @param  isDeletionPossible  whether shouldDelete can ever return true
         * @param  canWait   whether waiting for a result is permissible
         */
        DeleteMode( String description, boolean isDeletionPossible,
                    boolean canWait ) {
            description_ = description;
            isDeletionPossible_ = isDeletionPossible;
            canWait_ = canWait;
        }

        /**
         * Indicates whether a job with the given UWS stage should be deleted.
         *
         * @param   stage  UWS stage
         */
        abstract boolean shouldDelete( UwsStage stage );

        /**
         * Whether this this mode ever recommends deletion.
         *
         * @return  true iff {@link #shouldDelete} can ever return true
         */
        boolean isDeletionPossible() {
            return isDeletionPossible_;
        }

        /**
         * Whether this mode permits waiting for the job to finish.
         * If false, deletion should proceed immediately.
         *
         * @return   true if waiting is permitted
         */
        boolean canWait() {
            return canWait_;
        }

        /**
         * Returns an XML string containing &lt;li&gt; items describing
         * all the items in this enumeration.
         *
         * @return  XML documentation string
         */
        static String getListItems() {
            StringBuffer sbuf = new StringBuffer();
            for ( DeleteMode dMode : Arrays.asList( values() ) ) {
                sbuf.append( "<li>" )
                    .append( "<code>" )
                    .append( dMode.toString() )
                    .append( "</code>" )
                    .append( ": " )
                    .append( dMode.description_ )
                    .append( "</li>" )
                    .append( '\n' );
            }
            return sbuf.toString();
        }
    }
}
