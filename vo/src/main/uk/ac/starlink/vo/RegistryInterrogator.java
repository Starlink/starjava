package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;
import javax.xml.rpc.ServiceException;
import org.us_vo.www.Registry;
import org.us_vo.www.RegistryLocator;
import org.us_vo.www.RegistrySoap;
import org.us_vo.www.SimpleResource;

/**
 * Simple class which encapsulates use of an NVO-like registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class RegistryInterrogator {

    private RegistrySoap registry_;
    private URL url_;

    private static Boolean available_;
    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** Default registry URL. */
    public static final URL DEFAULT_URL; 
    static {
        try {
            DEFAULT_URL = 
                new URL( "http://voservices.net/registry/registry.asmx" );
        }
        catch ( MalformedURLException e ) {
            throw (AssertionError) new AssertionError( "Bad URL???" )
                                  .initCause( e );
        }
    }

    /**
     * Constructs a new registry interrogator with a given URL.
     *
     * @param  url  registry location
     */
    public RegistryInterrogator( URL url ) {
        url_ = url;
    }

    /**
     * Constructs a new registry interrogator with a default URL.
     */
    public RegistryInterrogator() {
        this( DEFAULT_URL );
    }

    /**
     * Returns the SOAP service associated with this registry.
     *
     * @return  SOAP registry service
     */
    public RegistrySoap getRegistry() throws ServiceException, RemoteException {
        if ( registry_ == null ) {
            Registry rserv = new RegistryLocator();
            registry_ = rserv.getRegistrySoap( url_ );
            registry_.revisions();  // does this check interface versions?
        }
        return registry_;
    }

    /**
     * Executes a query on this registry.  If the specified query string
     * is null, every record in the registry will be returned.
     *
     * @param  query  query text or <tt>null</tt>
     */
    public SimpleResource[] getResources( String query )
            throws RemoteException, ServiceException {
        return query == null
             ? getRegistry().dumpRegistry().getSimpleResource()
             : getRegistry().queryRegistry( query ).getSimpleResource();
    }

    /**
     * Example/convenience method which queries the registry for all cone 
     * search services and return them as {@link ConeSearch} objects.
     *
     * @return   cone search array
     */
    public ConeSearch[] getConeSearches() throws IOException {
        try {
            SimpleResource[] resources = 
                getResources( "ServiceType like 'CONE'" );
            int ncone = resources.length;
            ConeSearch[] cs = new ConeSearch[ ncone ];
            for ( int i = 0; i < ncone; i++ ) {
                cs[ i ] = new ConeSearch( resources[ i ] );
            }
            return cs;
        }
        catch ( ServiceException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Returns true if the classes required for operation of the registry
     * are present and correct.
     *
     * @return  usability status of registry-related classes
     */
    public static boolean isAvailable() {
        if ( available_ == null ) {
            try {
                Class c = RegistryInterrogator.class;
                c.forName( "net.ivoa.www.xml.VORegistry.v0_3.Registry" );
                c.forName( "org.us_vo.www.Registry" );
                available_ = Boolean.TRUE;
            }
            catch ( Throwable th ) {
                logger_.info( "WSDL classes unavailable" + " (" + th + ")" );
                available_ = Boolean.FALSE;
            }
        }
        return available_.booleanValue();
    }

}
