package uk.ac.starlink.datanode.nodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Provides specialised XML entity resolution for data nodes.
 * As well as the local copies of some useful entities provided by 
 * the superclass {@link StarEntityResolver}
 * any entity which is retrieved using a URL by this 
 * entity resolver is cached for future use for the case in which,
 * for instance, multiple files in the same directory reference the
 * same external entity.  Done this way, you don't have to keep making
 * network connections, you keep a copy of the items you retrieve.
 * Obviously, there is a down side, you can fill up memory with things
 * that you're not going to need again.  Try it like this for now.
 */
public class NodeEntityResolver implements EntityResolver {

    private static NodeEntityResolver instance = new NodeEntityResolver();

    private StarEntityResolver localResolver = StarEntityResolver.getInstance();
    private Map cache = new HashMap();

    /**
     * Returns the sole instance of this singleton class.
     */
    public static NodeEntityResolver getInstance() {
        return instance;
    }

    /**
     * Private sole constructor.
     */
    private NodeEntityResolver() {
    }

    public InputSource resolveEntity( String publicId, String systemId )
            throws SAXException, IOException {

        /* Give StarEntityResolver a chance to retrieve it from a local copy. */
        InputSource is = localResolver.resolveEntity( publicId, systemId );
        if ( is != null ) {
            return is;
        }

        /* See if it's in the cache. */
        if ( cache.containsKey( systemId ) ) {
            InputStream istrm = 
                new ByteArrayInputStream( (byte[]) cache.get( systemId ) );
            InputSource isrc = new InputSource( istrm );
            isrc.setPublicId( publicId );
            isrc.setSystemId( systemId );
            return isrc;
        }

        /* Otherwise, see if it is a URL that we want to cache. */
        try {
            URL url = new URL( systemId );
            String proto = url.getProtocol();
            if ( ! proto.equals( "jar" ) && ! proto.equals( "file" ) ) {
                ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
                InputStream istrm = url.openStream();
                byte[] buf = new byte[ 512 ];
                for ( int nb = istrm.read( buf, 0, 512 ); nb > 0; 
                          nb = istrm.read( buf, 0, 512 ) ) {
                    ostrm.write( buf, 0, nb );
                }
                istrm.close();
                ostrm.close();
                cache.put( systemId, ostrm.toByteArray() );

                /* Re-invoke this method; since it's now in the cache, it
                 * will get returned from there. */
                return resolveEntity( publicId, systemId );
            }
        }

        /* If there is some error, then just give up and decline to resolve
         * the entity. */
        catch ( MalformedURLException e ) {
            return null;
        }
        catch ( IOException e ) {
            return null;
        }

        /* Signal to whoever called us that we can't help. */
        return null;
    }
    
}
