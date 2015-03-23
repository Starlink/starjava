package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Handles asynchronous population of the TAP metadata hierarchy.
 * It owns a TapMetaReader and invokes its methods as required to
 * populate the metadata hiearchy objects, providing methods that
 * allow callbacks to be invoked when the relevant items are available.
 *
 * @author   Mark Taylor
 * @since    23 Mar 2015
 */
public class TapMetaManager {

    private final TapMetaReader rdr_;
    private final Map<Object,List<Runnable>> callbackMap_;
    private ExecutorService executor_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   rdr   object that knows how to acquire metadata
     */
    public TapMetaManager( TapMetaReader rdr ) {
        rdr_ = rdr;
        callbackMap_ = new HashMap<Object,List<Runnable>>();
    }

    /**
     * Returns the object that performs the actual data reads.
     *
     * @return  metadata reader
     */
    public TapMetaReader getReader() {
        return rdr_;
    }

    /**
     * Invokes a runnable on the Event Dispatch Thread when the
     * table metadata is available for a given schema metadata object.
     * This method, and the supplied callback, are invoked on the EDT.
     *
     * @param  smeta  schema metadata item
     * @param  callback  runnable to be invoked on the EDT when
     *                   smeta.getTables() has a non-null return
     * @return  true iff callback has been called synchronously
     */
    public boolean onTables( final SchemaMeta smeta, Runnable callback ) {
        if ( smeta.getTables() != null ) {
            callback.run();
            return true;
        }
        else {
            schedule( smeta, new Runnable() {
                public void run() {
                    TableMeta[] tmetas;
                    try {
                        tmetas = rdr_.readTables( smeta );
                    }
                    catch ( IOException e ) {
                        logger_.log( Level.WARNING,
                                     "Failed to read tables for schema "
                                   + smeta.getName()
                                   + " (" + rdr_.getSource() + ")", e );
                        tmetas = new TableMeta[ 0 ];
                    }
                    final TableMeta[] tmetas0 = tmetas;

                    /* Update the metadata on the EDT as well, otherwise
                     * the change is not guaranteed to be visible from that
                     * thread (Java Memory Model). */
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            smeta.setTables( tmetas0 );
                        }
                    } );
                }
            }, callback );
            return false;
        }
    }

    /**
     * Invokes a runnable on the Event Dispatch Thread when the
     * column metadata is available for a given table metadata object.
     * This method, and the supplied callback, are invoked on the EDT.
     *
     * @param  tmeta  table metadata item
     * @param  callback  runnable to be invoked on the EDT when
     *                   tmeta.getColumns() has a non-null return
     * @return  true iff callback has been called synchronously
     */
    public boolean onColumns( final TableMeta tmeta, Runnable callback ) {
        if ( tmeta.getColumns() != null ) {
            callback.run();
            return true;
        }
        else {
            schedule( new ColKey( tmeta ), new Runnable() {
                public void run() {
                    ColumnMeta[] cmetas;
                    try {
                        cmetas = rdr_.readColumns( tmeta );
                    }
                    catch ( IOException e ) {
                        logger_.log( Level.WARNING,
                                     "Failed to read columns for table "
                                    + tmeta.getName()
                                    + " (" + rdr_.getSource() + ")", e );
                        cmetas = new ColumnMeta[ 0 ];
                    }
                    final ColumnMeta[] cmetas0 = cmetas;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            tmeta.setColumns( cmetas0 );
                        }
                    } );
                }
            }, callback );
            return false;
        }
    }

    /**
     * Invokes a runnable on the Event Dispatch Thread when the
     * foreign key metadata is available for a given table metadata object.
     * This method, and the supplied callback, are invoked on the EDT.
     *
     * @param  tmeta  table metadata item
     * @param  callback  runnable to be invoked on the EDT when
     *                   tmeta.getForeignKeys() has a non-null return
     * @return  true iff callback has been called synchronously
     */
    public boolean onForeignKeys( final TableMeta tmeta, Runnable callback ) {
        if ( tmeta.getForeignKeys() != null ) {
            callback.run();
            return true;
        }
        else {
            schedule( new ForKey( tmeta ), new Runnable() {
                public void run() {
                    ForeignMeta[] fmetas;
                    try {
                        fmetas = rdr_.readForeignKeys( tmeta );
                    }
                    catch ( IOException e ) {
                        logger_.log( Level.WARNING,
                                     "Failed to read foreign keys for table "
                                   + tmeta.getName()
                                   + " (" + rdr_.getSource() + ")", e );
                        fmetas = new ForeignMeta[ 0 ];
                    }
                    final ForeignMeta[] fmetas0 = fmetas;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            tmeta.setForeignKeys( fmetas0 );
                        }
                    } );
                }
            }, callback );
            return false;
        }
    }

    /**
     * Releases resources and terminates any currently running asynchronous
     * metadata reads.  Calling this method does not prevent future use
     * of this object.
     */
    public void shutdown() {
        if ( executor_ != null ) {
            executor_.shutdownNow();
            executor_ = null;
        }
    }

    /**
     * Schedules a callback to run on the Event Dispatch Thread
     * following completion of a supplied runnable.
     * This method, and the callback, are called on the EDT.
     *
     * @param   key   object identifying the populator;
     *                populators with the same key do the same thing,
     *                so it's not necessary to run more than one
     * @param   populator  runnable to be completed before callback invocation;
     *                     will be run asynchronously
     * @param   callback  callback to be run on populator completion
     */
    private void schedule( final Object key, final Runnable populator,
                           Runnable callback ) {

        /* See if we're already running a populator that does the job in
         * question.  If so, just add the callback to the list that will
         * be run on poplulator completion. */
        List<Runnable> callbacks = callbackMap_.get( key );
        if ( callbacks != null ) {
            callbacks.add( callback );
        }

        /* Otherwise, schedule the populator to run asynchronously. */
        else {
            callbacks = new ArrayList<Runnable>();
            callbacks.add( callback );
            callbackMap_.put( key, callbacks );
            getExecutor().submit( new Runnable() {
                public void run() {
                    populator.run();

                    /* On completion, run any associated callbacks. */
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            for ( Runnable callback :
                                  callbackMap_.remove( key ) ) {
                                callback.run();
                            }
                        }
                    } );
                }
            } );
        }
    }

    /**
     * Lazily create and return an executor instance.
     *
     * @return  executor used for asynchronous queries by this object
     */
    public ExecutorService getExecutor() {
        if ( executor_ == null ) {
            executor_ = Executors.newSingleThreadExecutor( new ThreadFactory() {
                public Thread newThread( Runnable r ) {
                    Thread th = new Thread( r, "TAP Metadata Query" );
                    th.setDaemon( true );
                    return th;
                }
            } );
        }
        return executor_;
    }

    /**
     * Characterises a column request job.
     */
    private static class ColKey {
        private final TableMeta tmeta_;

        /**
         * Constructor.
         *
         * @param  tmeta   table for which columns are requested
         */
        ColKey( TableMeta tmeta ) {
            tmeta_ = tmeta;
        }
        @Override
        public int hashCode() {
            return 99 * tmeta_.hashCode();
        }
        @Override
        public boolean equals( Object other ) {
            return other instanceof ColKey
                && this.tmeta_ == ((ColKey) other).tmeta_;
        }
    }

    /**
     * Characterises a foreign key request job.
     */
    private static class ForKey {
        private final TableMeta tmeta_;

        /**
         * Constructor.
         *
         * @param  tmeta   table for which foreign keys are requested
         */
        ForKey( TableMeta tmeta ) {
            tmeta_ = tmeta;
        }
        @Override
        public int hashCode() {
            return 77 * tmeta_.hashCode();
        }
        @Override
        public boolean equals( Object other ) {
            return other instanceof ForKey
                && this.tmeta_ == ((ForKey) other).tmeta_;
        }
    }
}
