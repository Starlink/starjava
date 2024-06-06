package uk.ac.starlink.vo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.AuthStatus;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.IOSupplier;

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
public class TapServiceKit {

    private final TapService service_;
    private final String ivoid_;
    private final TapMetaPolicy metaPolicy_;
    private final ContentCoding coding_;
    private final int queueLimit_;
    private final Map<Populator<?>,Collection<Runnable>> runningMap_;
    private ExecutorService metaExecutor_;
    private volatile FutureTask<TapMetaReader> rdrFuture_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   service     TAP service description
     * @param   ivoid       IVORN of TAP service, if known (may be null)
     * @param   metaPolicy  implementation for reading table metadata
     * @param   coding      configures HTTP compression
     * @param   queueLimit  maximum number of table metadata requests queued
     *                      to service; more than that and older ones
     *                      will be dropped
     */
    public TapServiceKit( TapService service, String ivoid,
                          TapMetaPolicy metaPolicy, ContentCoding coding,
                          int queueLimit ) {
        service_ = service;
        ivoid_ = ivoid;
        metaPolicy_ = metaPolicy;
        coding_ = coding;
        queueLimit_ = queueLimit;
        runningMap_ = new HashMap<Populator<?>,Collection<Runnable>>();
    }

    /**
     * Returns the TAP service used by this kit.
     *
     * @return   service description
     */
    public TapService getTapService() {
        return service_;
    }

    /**
     * Returns the resource identifier for this kit if known.
     *
     * @return  IVORN for TAP service resource, or null
     */
    public String getIvoid() {
        return ivoid_;
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
     * Asynchronously acquires TAP database schema list.
     *
     * @param  handler  receiver for schema information
     */
    public void acquireSchemas( final ResultHandler<SchemaMeta[]> handler ) {
        acquireData( handler, () -> {
            TapMetaReader rdr = acquireMetaReader();
            try {
                return rdr.readSchemas();
            }
            catch ( IOException e ) {
                throw new IOException( rdr.getSource() + " error: " + e, e );
            }
        } );
    }

    /**
     * Asynchronously acquires TAP capability information.
     *
     * @param  handler   receiver for TAP capability object
     */
    public void acquireCapability( final
                                   ResultHandler<TapCapability> handler ) {
        acquireData( handler, () -> {
            URL curl = service_.getCapabilitiesEndpoint();
            if ( curl == null ) {
                throw new IOException( "No capabilities endpoint" );
            }
            logger_.info( "Reading capability metadata from " + curl );
            try {
                return TapCapabilitiesDoc.readCapabilities( curl )
                      .getTapCapability();
            }
            catch ( SAXException e ) {
                throw new IOException( "Capability parse error: " + e, e );
            }
        } );
    }

    /**
     * Asynchronously acquires information about the registry resource
     * corresponding to this service.
     * The result is a map of standard RegTAP resource column names
     * to their values.
     *
     * @param  handler  receiver for resource metadata map
     */
    public void acquireResource( final
                                 ResultHandler<Map<String,String>> handler ) {
        acquireData( handler,
                     () -> readResourceInfo( getRegTapService(), ivoid_ ) );
    }

    /**
     * Asynchronously acquires information from the RegTAP rr.res_role table
     * corresponding to this service.
     *
     * @param  handler  receiver for role list
     */
    public void acquireRoles( final ResultHandler<RegRole[]> handler ) {
        acquireData( handler,
                     () -> RegRole.readRoles( getRegTapService(),
                                              ivoid_, coding_ ) );
    }

    /**
     * Asynchronously acquires a list of service-specific query examples,
     * if available.
     *
     * @param  handler  receiver for example list
     */
    public void acquireExamples( ResultHandler<List<Tree<DaliExample>>>
                                 handler ) {
        final URL examplesUrl = service_.getExamplesEndpoint();
        if ( examplesUrl != null ) {
            acquireData( handler,
                         () -> new DaliExampleReader()
                              .readExamples( examplesUrl ) );
        }
        else {
            logger_.warning( "No examples endpoint" );
        }
    }

    /**
     * Asynchronously attempts to contact the authcheck endpoint of the
     * TAP service to determine authentication information.
     * This acquires the authentication status, but also sets up
     * the authenticated connection, including acquiring credentials
     * from the user, so that the user does not get asked
     * on subsequent occasions.
     *
     * <p>On completion, the supplied handler is messaged with either
     * the authentication status (which may represent anonymous usage)
     * meaning that subsequent interactions with this service via
     * the AuthManager may be successful, or with an IOException,
     * meaning that authorization was denied and further interaction
     * with the service is likely to fail.
     *
     * @param  handler  receives authentication information;
     *                  either an authentication status
     *                  or an authentication error
     * @param  forceLogin  if true, the user will be asked to (re-)login if
     *                     authentication is available; otherwise
     *                     existing or anonymous interaction is preferred
     */
    public void acquireAuthStatus( final ResultHandler<AuthStatus> handler,
                                   boolean forceLogin ) {
        FutureTask<TapMetaReader> rdrFuture = rdrFuture_;
        if ( rdrFuture != null ) {
            rdrFuture.cancel( true );
        }
        rdrFuture_ = null;
        runningMap_.clear();

        /* These should be defined for TAP by SSO. */
        final boolean isHead = true;
        final URL authcheckUrl = service_.getCapabilitiesEndpoint();

        /* Asynchronously enquire status with logging. */
        ResultHandler<AuthStatus> loggingHandler =
                new ResultHandler<AuthStatus>() {
            public boolean isActive() {
                return handler.isActive();
            }
            public void showWaiting() {
                handler.showWaiting();
            }
            public void showError( IOException error ) {
                logger_.log( Level.WARNING,
                             "Failed to acquire authentication status at "
                           + authcheckUrl + ": " + error, error );
                handler.showError( error );
            }
            public void showResult( AuthStatus status ) {
                logger_.info( "Authentication status: " + status
                            + " (" + authcheckUrl + ")" );
                handler.showResult( status );
            }
        };
        acquireData( loggingHandler,
                     () -> AuthManager.getInstance()
                          .authcheck( authcheckUrl, isHead, forceLogin ) );
    }

    /**
     * Returns a RegTAP service that can be queried
     * for information about this service's registry record.
     *
     * <p>The default implementation returns the default value from
     * {@link TapServices#getRegTapService}.
     *
     * @return  RegTAP service description
     */
    public TapService getRegTapService() {
        return TapServices.getRegTapService();
    }

    /**
     * Releases resources and terminates any currently running asynchronous
     * metadata reads.  Calling this method does not prevent future use
     * of this object.
     */
    public void shutdown() {
        if ( metaExecutor_ != null ) {
            metaExecutor_.shutdownNow();
            metaExecutor_ = null;
        }
    }

    /**
     * Returns the TapMetaReader in use by this kit.
     * This method will not block, but may return null if the reader to use
     * has not yet been determined.
     *
     * @return   metaReader in use, or null
     */
    public TapMetaReader getMetaReader() {
        if ( rdrFuture_ != null && rdrFuture_.isDone() ) {
            try {
                return rdrFuture_.get( 0, TimeUnit.SECONDS );
            }
            catch ( Exception e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns a TapMetaReader for use by this kit.
     * Thread safe, but should be called on a thread which is not the EDT.
     * May be time consuming.
     *
     * @return  a tap meta reader for use by this object, not null
     */
    private TapMetaReader acquireMetaReader() {
        final boolean runNow;

        /* Prepare to initiate acquisition only if no other process has
         * got there first. */
        synchronized ( this ) {
            if ( rdrFuture_ == null ) {
                runNow = true;
                rdrFuture_ = new FutureTask<TapMetaReader>(
                                 new Callable<TapMetaReader>() {
                    public TapMetaReader call() {
                        return metaPolicy_
                              .createMetaReader( service_, coding_ );
                    }
                } );
            }
            else {
                runNow = false;
            }
        }

        /* If this invocation is responsible for obtaining the value,
         * do it synchronously. */
        if ( runNow ) {
            rdrFuture_.run();
        }

        /* Obtain the value, which may entail waiting for another thread. */
        try {
            return rdrFuture_.get();
        }
        catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            return new ErrorMetaReader( "interrupted", e );
        }
        catch ( ExecutionException e ) {
            return new ErrorMetaReader( e.getCause().getMessage(), e );
        }
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
                getMetaExecutor().submit( new Runnable() {
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
        populator.populate( acquireMetaReader() );
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
     * Asynchronously acquires a result and passes it to a supplied handler
     * whose methods are invoked on the event dispatch thread.
     *
     * @param  handler  receiver for acquired information
     * @param  supplier   supplier for information
     */
    private <T> void acquireData( final ResultHandler<T> handler,
                                  final IOSupplier<T> supplier ) {
        if ( ! handler.isActive() ) {
            return;
        }
        handler.showWaiting();
        getMetaExecutor().submit( new Runnable() {
            public void run() {
                if ( ! handler.isActive() ) {
                    return;
                }
                T data;
                try {
                    data = supplier.get();
                }
                catch ( final Throwable error ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( handler.isActive() ) {
                                handler.showError( TapTableLoadDialog
                                                  .asIOException( error ) );
                            }
                        }
                    } );
                    return;
                }
                final T data0 = data;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( handler.isActive() ) {
                            handler.showResult( data0 );
                        }
                    }
                } );
            }
        } );
    }

    /**
     * Reads resource metadata corresponding to a given IVORN.
     * The result is a map of standard RegTAP resource column names
     * to their values.
     *
     * @param  regtapService   RegTAP service description
     * @param  ivoid    ivorn for resource of interest
     * @return  map from resource column name to value for selected resource
     *          metadata items
     */
    private static Map<String,String>
            readResourceInfo( TapService regtapService, String ivoid )
            throws IOException {
        String[] items = new String[] {
            "short_name",
            "res_title",
            "res_description",
            "reference_url",
        };
        StringBuffer adql = new StringBuffer()
           .append( "SELECT" );
        for ( int i = 0; i < items.length; i++ ) {
            adql.append( i == 0 ? " " : ", " )
                .append( items[ i ] );
        }
        adql.append( " FROM rr.resource" )
            .append( " WHERE ivoid='" )
            .append( ivoid )
            .append( "'" );
        TapQuery tq = new TapQuery( regtapService, adql.toString(), null );
        StarTable result = tq.executeSync( StoragePolicy.PREFER_MEMORY,
                                           ContentCoding.NONE );
        result = Tables.randomTable( result );
        Map<String,String> resultMap = new LinkedHashMap<String,String>();
        int ncol = result.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            Object value = result.getCell( 0, icol );
            if ( value instanceof String ) {
                resultMap.put( items[ icol ], (String) value );
            }
        }
        return resultMap;
    }

    /**
     * Returns a lazily constructed ExecutorService for use in acquiring
     * metadata items.
     *
     * @return  executor
     */
    private ExecutorService getMetaExecutor() {
        if ( metaExecutor_ == null ) {
            metaExecutor_ = createExecutorService( queueLimit_ );
        }
        return metaExecutor_;
    }

    /**
     * Creates an executor service suitable for use by this object.
     *
     * @param   queueLimit  maximum number of metadata requests queued
     *                      to service; more than that and older ones
     *                      will be dropped
     * @return   new executor service
     */
    private static ExecutorService createExecutorService( int queueLimit ) {

        /* Use a custom queue implementation that has LIFO rather than FIFO
         * semantics.  For our purposes (driving requests from a GUI),
         * it is likely that the most recently received request is the
         * most pressing one.  Older ones may relate to actions that are
         * no longer relevant (e.g. selection of items that have since
         * been superceded).  So prioritise the most recent requests, and
         * possibly discard older ones in their favour. */
        BlockingQueue<Runnable> queue =
            new BoundedBlockingStack<Runnable>( queueLimit );

        /* This handler deals with a full queue by discarding the runnable
         * at the tail of the executorService's queue.  Since we are using
         * a LIFO "queue", that is the oldest one
         * (note that ThreadPoolExecutor.DiscardOldestPolicy discards the
         * item at the head, which in this case is the newest one).
         * So in case of a full queue, the most recent requests are honoured,
         * and the old ones get forgotten. */
        RejectedExecutionHandler rejectHandler =
                new RejectedExecutionHandler() {
            private final RejectedExecutionHandler dfltHandler =
                new ThreadPoolExecutor.AbortPolicy();
            public void rejectedExecution( Runnable r,
                                           ThreadPoolExecutor e ) {
                BlockingQueue<Runnable> q = e.getQueue();
                if ( q instanceof BoundedBlockingStack ) {

                    /* This implementation code is adapted from
                     * ThreadPoolExecutor.DiscardOldestPolicy. */
                    if ( ! e.isShutdown() ) {
                        ((BoundedBlockingStack) q).removeTail();
                        logger_.log( Level.INFO,
                                     "Discard metadata request" );
                        e.execute( r );
                    }
                }

                /* Fall back to default behaviour if we have the wrong queue
                 * type for some reason. */
                else {
                    assert false;
                    dfltHandler.rejectedExecution( r, e );
                }
            }
        };

        /* Construct and return an ExecutorService based on these custom
         * characteristics. */
        int corePoolSize = 1;
        int maxPoolSize = 1;
        ThreadFactory thFact = new ThreadFactory() {
            public Thread newThread( Runnable r ) {
                Thread th = new Thread( r, "TAP metadata query" );
                th.setDaemon( true );
                return th;
            }
        };
        return new ThreadPoolExecutor( corePoolSize, maxPoolSize,
                                       30, TimeUnit.SECONDS,
                                       queue, thFact, rejectHandler );
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
                && this.id_.equals( ((Populator<?>) o).id_ );
        }
    }

    /**
     * Dummy TapMetaReader implementation that throws errors for everything.
     */
    private static class ErrorMetaReader implements TapMetaReader {
        final Exception error_;
        final String msg_;

        /**
         * Constructor.
         *
         * @param  msg  short message about what went wrong
         * @param  error  cause of problem
         */
        ErrorMetaReader( String msg, Exception error ) {
            error_ = error;
            msg_ = msg;
        }
        public String getSource() {
            return "No source (" + error_ + ")";
        }
        public String getMeans() {
            return "No method (" + error_ + ")";
        }
        public SchemaMeta[] readSchemas() throws IOException {
            throw rethrown();
        }
        public TableMeta[] readTables( SchemaMeta schema ) throws IOException {
            throw rethrown();
        }
        public ColumnMeta[] readColumns( TableMeta table ) throws IOException {
            throw rethrown();
        }
        public ForeignMeta[] readForeignKeys( TableMeta table )
                throws IOException {
            throw rethrown();
        }

        /**
         * Returns a new IOException whose cause is the one on which this
         * reader was constructed.
         *
         * @return   IOException
         */
        private IOException rethrown() {
            return (IOException)
                   new IOException( "No metadata reader: " + msg_ )
                  .initCause( error_ );
        }
    }

    /**
     * Bounded LIFO BlockingQueue implementation.
     * This subclasses Doug Lea's (unbounded) LinkedBlockingStack
     * implementation to provide a limit on queue size.
     * It doesn't do it correctly, since it lies about the capacity -
     * adding items will always work without blocking, even when this
     * implementation says otherwise.  But it works well enough to fool
     * an ExecutorService into thinking it's full and rejecting new runnables.
     * In any case I don't think the contract violations of this class
     * are dangerous or even discoverable by clients.
     *
     * <p>An alternative would be to base this implementation on
     * (JSE6) java.util.concurrent.LinkedBlockingDeque, which has bounds.
     * But it's fiddly to present the deque as a BlockingQueue, since
     * all the FIFO methods need to be wired instead to work from the
     * other end of the deque.
     */
    private static class BoundedBlockingStack<E>
            extends LinkedBlockingStack<E> {
        private final int capacity_;
        private final Lock lock_;

        /**
         * Constructor.
         *
         * @param  capacity  maximum nominal queue capacity
         */
        BoundedBlockingStack( int capacity ) {
            capacity_ = capacity;
            lock_ = getLock();
        }

        @Override
        public boolean add( E o ) {
            lock_.lock();
            try {
                if ( remainingCapacity() > 0 ) {
                    return super.add( o );
                }
                else {
                    throw new IllegalStateException( "Queue full" );
                }
            }
            finally {
                lock_.unlock();
            }
        }

        @Override
        public boolean offer( E o ) {
            lock_.lock();
            try {
                return remainingCapacity() > 0 && super.offer( o );
            }
            finally {
                lock_.unlock();
            }
        }

        @Override
        public int remainingCapacity() {
            return Math.max( 0, capacity_ - size() );
        }

        /**
         * Deletes the item at the tail of this queue.
         *
         * @return   tail item, or null if empty
         */
        public E removeTail() {
            lock_.lock();
            try {
                for ( Iterator<E> it = iterator(); it.hasNext(); ) {
                    E item = it.next();
                    if ( ! it.hasNext() ) {
                        it.remove();
                        return item;
                    }
                }
                return null;
            }
            finally {
                lock_.unlock();
            }
        }
    }
}
