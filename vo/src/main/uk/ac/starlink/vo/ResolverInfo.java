package uk.ac.starlink.vo;

import Sesame_pkg.Sesame;
import Sesame_pkg.SesameServiceLocator;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * Information about an astronomical object obrtained from some name
 * resolution service.
 * The current implementation is in terms of CDS's Sesame name resolver
 * web service.
 * 
 * @author  Mark Taylor (Starlink)
 * @since   4 Feb 2005
 * @see  <http://cdsweb.u-strasbg.fr/cdsws/name_resolver.gml>
 */
public class ResolverInfo {

    private static SesameServiceLocator locator_;
    private double raDegrees_;
    private double decDegrees_;

    /**
     * Constructs a new resolver from the &lt;Resolver&gt; XML element 
     * containing the result of a successful CDS resolver service query.
     *
     * @param   resolverEl  Resolver element
     */
    private ResolverInfo( Element resolverEl ) throws ResolverException {
        checkTagname( resolverEl, "Resolver" );
        raDegrees_ = Double.NaN;
        decDegrees_ = Double.NaN;
        for ( Node node = resolverEl.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            String tag = getTagname( node );
            if ( "jradeg".equals( tag ) ) {
                raDegrees_ = getDoubleContent( (Element) node );
            }
            else if ( "jdedeg".equals( tag ) ) {
                decDegrees_ = getDoubleContent( (Element) node );
            }
        }
        if ( Double.isNaN( raDegrees_ ) || Double.isNaN( decDegrees_ ) ) {
            throw new ResolverException( "No position information" );
        }
    }

    /**
     * Returns object right ascension in degrees.
     *
     * @return  RA
     */
    public double getRaDegrees() {
        return raDegrees_;
    }

    /**
     * Returns object declination in degrees.
     *
     * @return   declination
     */
    public double getDecDegrees() {
        return decDegrees_;
    }

    /**
     * Attempts to resolve an object by name using a name resover service,
     * and either returns an object containing information about it or
     * throws an exception.
     *
     * @param   name  object name
     * @return  resolver info object
     * @throws  ResolverException  if resolution failed for some reason
     */
    public static ResolverInfo resolve( String name ) throws ResolverException {
        try {
            String result = getService().sesame( name, "x" );
            Element el = new SourceReader()
                        .getElement( 
                             new StreamSource(
                                 new ByteArrayInputStream(
                                     result.getBytes() ) ) );
            String tag = getTagname( el );
            if ( "Sesame".equals( tag ) ) {
                return interpretSesame( el );
            }
            else {
                throw new ResolverException( "Bad element type <" + tag + 
                                             "> from Sesame" );
            }
        }
        catch ( RemoteException e ) {
            throw new ResolverException( e.getMessage(), e );
        }
        catch ( ServiceException e ) {
            throw new ResolverException( e.getMessage(), e );
        }
        catch ( TransformerException e ) {
            throw new ResolverException( e.getMessage(), e );
        }
    }

    private static ResolverInfo interpretSesame( Element sesameEl )
            throws ResolverException {
        checkTagname( sesameEl, "Sesame" );
        checkForErrors( sesameEl );
        for ( Node node = sesameEl.getFirstChild(); node != null; 
              node = node.getNextSibling() ) {
            if ( "Resolver".equals( getTagname( node ) ) ) {
                Element resolverEl = (Element) node;
                checkForErrors( resolverEl );
                return new ResolverInfo( resolverEl );
            }
        }
        throw new ResolverException( "Name not resolved" );
    }

    private static void checkForErrors( Element el ) throws ResolverException {
        for ( Node node = el.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            if ( "ERROR".equals( getTagname( node ) ) ) {
                throw new ResolverException( DOMUtils
                                            .getTextContent( (Element) node ) );
            }
        }
    }

    private static void checkTagname( Node el, String tagname ) {
        if ( ! tagname.equals( getTagname( el ) ) ) {
            throw new IllegalArgumentException( "Node is not a <" +
                                                tagname + ">" );
        }
    }

    private static String getTagname( Node node ) {
        return node instanceof Element 
             ? ((Element) node).getTagName()
             : null;
    }

    private static double getDoubleContent( Element el ) {
        try {
            return Double.parseDouble( DOMUtils.getTextContent( el ) );
        }
        catch ( NumberFormatException e ) {
            return Double.NaN;
        }
    }

    private static String getStringContent( Element el ) {
        String s = DOMUtils.getTextContent( el );
        if ( s == null || s.trim().length() == 0 ) {
            return null;
        }
        else {
            return s.trim();
        }
    }

    private static synchronized Sesame getService() throws ServiceException {
        if ( locator_ == null ) {
            locator_ = new SesameServiceLocator();
        }
        return locator_.getSesame();
    }

    public static void main( String[] args ) throws ResolverException {
        for ( int i = 0; i < args.length; i++ ) {
            String name = args[ i ];
            ResolverInfo info = resolve( name );
            System.out.println( "\t" + name + ":\t" + 
                                (float) info.getRaDegrees() + ",\t" +
                                (float) info.getDecDegrees() );
        }
    }
}
