package uk.ac.starlink.vo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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
 * <p>This class is intended for use with Swing; some methods must be
 * invoked from the Event Dispatch Thread as documented.
 *                      
 * @author   Mark Taylor
 * @since    23 Mar 2015
 */
public class TapMetaManager {

    private final TapMetaReader rdr_;
    private final Map<Populator,Collection<Runnable>> runningMap_;
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
        runningMap_ = new HashMap<Populator,Collection<Runnable>>();
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
        return onData( callback,
                       new Populator<TableMeta>( TableMeta.class, "tables",
                                                 smeta ) {
            public boolean hasData() {
                return smeta.getTables() != null;
            }
            public TableMeta[] readData( TapMetaReader rdr )
                    throws IOException {
                return rdr.readTables( smeta );
            }
            public void updateData( TableMeta[] tmetas ) {
                smeta.setTables( tmetas );
            }
        } );
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
        return onData( callback,
                       new Populator<ColumnMeta>( ColumnMeta.class, "columns",
                                                  tmeta ) {
            public boolean hasData() {
                return tmeta.getColumns() != null;
            }
            public ColumnMeta[] readData( TapMetaReader rdr )
                    throws IOException {
                return rdr.readColumns( tmeta );
            }
            public void updateData( ColumnMeta[] cmetas ) {
                tmeta.setColumns( cmetas );
            }
        } );
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
        return onData( callback,
                       new Populator<ForeignMeta>( ForeignMeta.class,
                                                   "foreign keys", tmeta ) {
            public boolean hasData() {
                return tmeta.getForeignKeys() != null;
            }
            public ForeignMeta[] readData( TapMetaReader rdr )
                    throws IOException {
                return rdr.readForeignKeys( tmeta );
            }
            public void updateData( ForeignMeta[] fmetas ) {
                tmeta.setForeignKeys( fmetas );
            }
        } );
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
     * Lazily create and return an executor instance.
     *
     * @return  executor used for asynchronous queries by this object
     */
    public ExecutorService getExecutor() {
        if ( executor_ == null ) {
            executor_ = createExecutorService();
        }
        return executor_;
    }

    /**
     * Executes a given callback when data is available from a given populator.
     * This method, and the supplied callback, are called on the
     * Event Dispatch Thread.
     *
     * @param  callback  runnable to execute when data is available
     * @param  populator   defines data acquisition
     */
    private boolean onData( final Runnable callback,
                            final Populator<?> populator ) {
        if ( populator.hasData() ) {
            callback.run();
            return true;
        }
        else {
            try {
                getExecutor().submit( new Runnable() {
                    public void run() {
                        if ( populator.populateCompleted_ ) {
                            SwingUtilities.invokeLater( callback );
                        }
                        else {
                            populateAndCallback( populator, callback );
                        }
                    }
                } );
            }
            catch ( RejectedExecutionException e ) {
                logger_.log( Level.INFO,
                             "Will not read TAP metadata this time"
                            + " (" + populator.getDataDescription() + ")", e );
            }
            return false;
        }
    }

    /**
     * Causes the data in a populator to be updated as required,
     * and then calls a supplied callback.
     * Manages the possibility of other equivalent populators being
     * queried concurrently, so as to avoid the same work being done
     * multiple times.
     *
     * <p>This method may be called from any thread.
     *
     * @param  populator   defines data acquisition
     * @param  callback  runnable to execute on the EDT when data is available
     */
    private void populateAndCallback( final Populator<?> populator,
                                      Runnable callback ) {
        synchronized ( runningMap_ ) {

            /* If an equivalent populator is already running, piggyback on it
             * and just ensure that our callback gets run on completion. */
            if ( runningMap_.containsKey( populator ) ) {
                runningMap_.get( populator ).add( callback );
                return;
            }

            /* Otherwise initiate a list of callbacks, with the requested one
             * as the initial sole member. */
            else {
                List<Runnable> callbacks = new ArrayList<Runnable>();
                callbacks.add( callback );
                runningMap_.put( populator, callbacks );
            }
        }

        /* Acquire the data synchronously. */
        populator.populate( rdr_ );
        populator.populateCompleted_ = true;

        /* Get the list of callbacks dependent on the data.
         * This will certainly contain the one requested  by this invocation,
         * but may contain more added by other threads during the data
         * acquisition. */
        final Collection<Runnable> callbacks;
        synchronized ( runningMap_ ) {
            callbacks = runningMap_.remove( populator );
        }

        /* Invoke all the callbacks on the EDT. */
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                assert populator.hasData();
                for ( Runnable cb : callbacks ) {
                    cb.run();
                }
            }
        } );
    }

    /**
     * Creates an executor service suitable for use by this object.
     *
     * @return   new executor service
     */
    private static ExecutorService createExecutorService() {
        ThreadFactory thFact = new ThreadFactory() {
            public Thread newThread( Runnable r ) {
                Thread th = new Thread( r, "TAP metadata query" );
                th.setDaemon( true );
                return th;
            }
        };
        return Executors.newSingleThreadExecutor( thFact );
    }

    /**
     * Defines data to be acquired.
     * Instances of this class with the same behaviour are equal according
     * to the <code>equals</code> method.
     */
    private static abstract class Populator<T> {

        private final Class<T> clazz_;
        private final String metasType_;
        private final Object id_;
        volatile boolean populateCompleted_;

        /**
         * Constructor.
         *
         * @param   clazz  element type of acquired data
         * @param   metasType  string (for user messages) describing acquired
         *                     data items, plural
         * @param   id  object defining the identity (equality) for this object
         */
        Populator( Class<T> clazz, String metasType, Object id ) {
            clazz_ = clazz;
            metasType_ = metasType;
            id_ = id;
        }

        /**
         * Indicates whether the data for this populator is already avaialable.
         * Execute on the Event Dispatch Thread.
         *
         * @return   true iff data has been acquired
         */
        abstract boolean hasData();

        /**
         * Does the work of reading the data for this populator.
         * Runs from any thread (not EDT).  May be time consuming.
         *
         * @param  rdr   reader object
         * @return   acquired data
         * @throws  IOException   in case of read error
         */
        abstract T[] readData( TapMetaReader rdr ) throws IOException;

        /**
         * Sets the data for this populator.
         * Execute on the Event Dispatch Thread.
         *
         * @param  data   acquired data
         */
        abstract void updateData( T[] data );

        /**
         * Causes this populator's data to be populated.
         * May be run from any thread, updates are performed on EDT.
         *
         * @param  rdr   reader object
         */
        void populate( TapMetaReader rdr ) {
            final T[] data = getData( rdr );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    updateData( data );
                }
            } );
        }

        /**
         * Reads data, returning an empty array in case of error.
         *
         * @param  rdr   reader object
         * @return   acquired data or empty array, not null
         */
        private T[] getData( TapMetaReader rdr ) {
            try {
                return readData( rdr );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING,
                             "Failed to read TAP metadata "
                           + getDataDescription(), e );
                @SuppressWarnings("unchecked")
                T[] emptyArray = (T[]) Array.newInstance( clazz_, 0 );
                return emptyArray;
            }
        }

        /**
         * Returns a user-directed description of whatever it is this
         * populator is supposed to be reading.
         */
        public String getDataDescription() {
            return metasType_ + " for " + id_;
        }

        @Override
        public int hashCode() {
            return id_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            return getClass().equals( o.getClass() )
                && this.id_.equals( ((Populator) o).id_ );
        }
    }
}
