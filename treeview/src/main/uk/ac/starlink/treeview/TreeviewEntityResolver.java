package uk.ac.starlink.treeview;

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

/**
 * Provides specialised XML entity resolution for Treeview.
 * Treeview keeps local copies of a few resources in {@link #TEXT_PATH}
 * so that it doesn't need to make external connections to resolve these.
 * Additionally, any entity which is retrieved using a URL by this 
 * entity resolver is cached for future use for the case in which,
 * for instance, multiple files in the same directory reference the
 * same external entity.  Done this way, you don't have to keep making
 * network connections, you keep a copy of the items you retrieve.
 * Obviously, there is a down side, you can fill up memory with things
 * that you're not going to need again.  Try it like this for now.
 */
public class TreeviewEntityResolver implements EntityResolver {

    public static final String TEXT_PATH = "uk/ac/starlink/treeview/text/";

    private static EntityResolver instance = new TreeviewEntityResolver();

    private ClassLoader loader = getClass().getClassLoader();
    private Map cache = new HashMap();

    /**
     * Returns the sole instance of this singleton class.
     */
    public static EntityResolver getInstance() {
        return instance;
    }

    private TreeviewEntityResolver() {
    }

    public InputSource resolveEntity( String publicId, String systemId ) {

        /* See if it is one of the items we keep on hand. */
        String resourcePath = null;
        if ( systemId.endsWith( "VOTable.dtd" ) ) {
            resourcePath = TEXT_PATH + "VOTable.dtd";
        }
        else if ( systemId.endsWith( "astrores.dtd" ) ) {
            resourcePath = TEXT_PATH + "astrores.dtd";
        }
        if ( resourcePath != null ) {
            InputStream istrm = loader.getResourceAsStream( resourcePath );
            InputSource isrc = new InputSource( istrm );
            isrc.setPublicId( publicId );
            isrc.setSystemId( systemId );
            return isrc;
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
