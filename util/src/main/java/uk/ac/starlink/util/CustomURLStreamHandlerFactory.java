package uk.ac.starlink.util;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Customisable implementation of <code>URLStreamHandlerFactory</code>.
 * The handlers it can dispense are configured using a map; the keys of
 * the map are protocol names and its values are the names of classes
 * which implement {@link java.net.URLStreamHandler} (and have no-arg
 * constructors).
 *
 * @author   Mark Taylor
 * @since    25 Aug 2006
 * @see  java.net.URL
 */
public class CustomURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private final Map<String,String> classMap_;
    private final Map<String,URLStreamHandler> instanceMap_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );

    /**
     * Constructs a no-op handler factory.
     */
    public CustomURLStreamHandlerFactory() {
        this( new HashMap<String,String>() );
    }

    /**
     * Constructs a handler factory initialised with a given 
     * protocol-&gt;classname map.
     *
     * @param  classMap  handler class map
     */
    public CustomURLStreamHandlerFactory( Map<String,String> classMap ) {
        classMap_ = classMap;
        instanceMap_ = new HashMap<String,URLStreamHandler>();
    }

    /**
     * Returns the protocol-&gt;classname map that describes this factory.
     * It may be altered to modify behaviour.
     *
     * @return   handler class map
     */
    public Map<String,String> getHandlerClassMap() {
        return classMap_;
    }

    public URLStreamHandler createURLStreamHandler( String protocol ) {
        Map<String,String> classMap = getHandlerClassMap();

        /* If we already have a handler for this protocol, return it. */
        if ( instanceMap_.containsKey( protocol ) ) {
            return instanceMap_.get( protocol );
        }

        /* Otherwise if we have a classname which is supposed to reference
         * a handler for this protocol, try to instantiate it. */
        else if ( classMap.containsKey( protocol ) ) {
            String clazzName = classMap.get( protocol );
            URLStreamHandler handler;
            try {
                handler = (URLStreamHandler)
                          Class.forName( clazzName ).getDeclaredConstructor()
                                                    .newInstance();
            }
            catch ( Throwable e ) {
                handler = null;
                logger_.warning( "Cannot instantiate handler for URL protocol "
                               + protocol + ": " + e );
            }

            /* Success or failure, put an entry into the instance map with
             * the result. */
            instanceMap_.put( protocol, handler );
            return handler;
        }

        /* No idea. */
        else {
            return null;
        }
    }
}
