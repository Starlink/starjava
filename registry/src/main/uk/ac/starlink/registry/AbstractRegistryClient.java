package uk.ac.starlink.registry;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Can submit ADQL queries to a registry and return the result as a
 * list of resources.
 * This class uses custom SAX parsing of the SOAP response 
 * to ensure that even a large response (not uncommon)
 * can be processed without large client-side resource usage.
 * For this reason, no use is made of the maximum record limit
 * options provided by RI 1.0; there's no client-side need for it.
 *
 * @author   Mark Taylor
 * @see   <a href="http://www.ivoa.net/Documents/RegistryInterface/"
 *           >IVOA Registry Interfaces</a>
 */
public abstract class AbstractRegistryClient<R> {

    private static final String RI_NS = 
        "http://www.ivoa.net/xml/RegistryInterface/v1.0";
    private static final String XSI_NS =
        "http://www.w3.org/2001/XMLSchema-instance";

    private final SoapClient soapClient_;

    /**
     * Constructs a AbstractRegistryClient given a SOAP client.
     *
     * @param  soapClient  SOAP client which talks to an RI-compliant
     *         registry service
     */
    public AbstractRegistryClient( SoapClient soapClient ) {
        soapClient_ = soapClient;
    }

    /**
     * Returns a SAX ContentHandler which can parse a VOResources
     * element and feed items of some type R to a ResourceSink object.
     * R presumably represents a resource.
     *
     * @param  sink  receiver of resource objects
     * @return  new resource handler
     */
    protected abstract ContentHandler
            createResourceHandler( ResourceSink<R> sink );

    /**
     * Returns the SOAP endpoint this client talks to.
     *
     * @return  registry endpoint
     */
    public URL getEndpoint() {
        return soapClient_.getEndpoint();
    }

    /**
     * Returns a list of resources corresponding to a given SOAP request.
     * The list is the result of feeding the SOAP response to the 
     * handler created by {@link #createResourceHandler}.
     *
     * @param   request  SOAP request
     * @return  list of resources
     */
    public List<R> getResourceList( SoapRequest request )
            throws IOException {
        final List<R> resList = new ArrayList<R>();
        ResourceSink<R> sink = new ResourceSink<R>() {
            public void addResource( R resource ) {
                resList.add( resource );
            }
        };
        try {
            soapClient_.execute( request, createResourceHandler( sink ) );
        }
        catch ( SAXException e ) {
            String msg = e.getMessage();
            if ( msg == null ) {
                msg = "SAX parse error";
            }
            throw (IOException) new IOException( msg ).initCause( e );
        }
        return resList;
    }

    /**
     * Returns an iterator over resources corresponding to a given SOAP request.
     * The resources are the result of feeding the SOAP response to the
     * handler created by {@link #createResourceHandler}.
     *
     * <p>The iterator's <code>next</code> or <code>hasNext</code> method
     * may throw a {@link RegistryQueryException}.
     *
     * <p>The writing thread will only read resources from the SAX response
     * as they are read out by the thread iterating over this iterator,
     * so memory used by this class remains very small regardless of
     * the number of records processed.
     *
     * @param  request  SOAP request
     * @return  iterator over resources; its <code>next</code> method may 
     *          throw a <code>RegistryQueryException</code>
     */
    public Iterator<R> getResourceIterator( final SoapRequest request )
            throws IOException {
        final IteratorResourceSink<R> sink = new IteratorResourceSink<R>();
        final ContentHandler resourceHandler = createResourceHandler( sink );
        new Thread( "Registry" + request.getAction() ) {
            public void run() {
                try {
                    soapClient_.execute( request, resourceHandler );
                }
                catch ( Throwable e ) {
                    sink.setError( e );
                }
                finally {
                    sink.close();
                }
            }
        }.start();
        return sink;
    }

    /**
     * ResourceSink implementation which also implements an Iterator
     * over the received resources.  Feeding the sink and iterating
     * over the results will generally be done in different threads.
     */
    private class IteratorResourceSink<R>
            implements ResourceSink<R>, Iterator<R> {
        private final List<Object> queue_ = new LinkedList<Object>();
        private volatile Throwable error_;
        private volatile boolean done_;

        /**
         * Adds a resource to the the queue.
         *
         * @param  resource  item to add
         */
        public synchronized void addResource( R resource ) {
            queue_.add( resource );
            notifyAll();
        }

        /**
         * Arrange for an error to be thrown from a subsequent iterator
         * method at a later date.
         *
         * @param  error  error to signal
         */
        synchronized void setError( Throwable error ) {
            queue_.add( error );
            notifyAll();
        }

        /**
         * Notify that no more resources will be added.
         */
        synchronized void close() {
            done_ = true;
            notifyAll();
        }

        public synchronized boolean hasNext() {
            while ( ! done_ && queue_.isEmpty() ) {
                try {
                    wait();
                }
                catch ( InterruptedException e ) {
                    setError( e );
                }
            }
            return ! queue_.isEmpty();
        }

        @SuppressWarnings("unchecked")
        public synchronized R next() {
            if ( hasNext() ) {
                Object item = queue_.remove( 0 );
                if ( item instanceof Throwable ) {
                    throw new RegistryQueryException( (Throwable) item );
                }
                else { // it's an R
                    return (R) item;
                }
            }
            else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
