package uk.ac.starlink.treeview;

import java.io.InputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Provides specialised XML entity resolution for Treeview.
 * Treeview keeps local copies of a few resources in {@link #TEXT_PATH}
 * so that it doesn't need to make external connections to resolve these.
 */
public class TreeviewEntityResolver implements EntityResolver {

    public static final String TEXT_PATH = "uk/ac/starlink/treeview/text/";

    private static EntityResolver instance = new TreeviewEntityResolver();

    private ClassLoader loader = getClass().getClassLoader();

    /**
     * Returns the sole instance of this singleton class.
     */
    public static EntityResolver getInstance() {
        return instance;
    }

    private TreeviewEntityResolver() {
    }

    public InputSource resolveEntity( String publicId, String systemId ) {
        String resourcePath;
        if ( systemId.endsWith( "VOTable.dtd" ) ) {
            resourcePath = TEXT_PATH + "VOTable.dtd";
        }
        else if ( systemId.endsWith( "astrores.dtd" ) ) {
            resourcePath = TEXT_PATH + "astrores.dtd";
        }
        else {
            return null;
        }
        InputStream istrm = loader.getResourceAsStream( resourcePath );
        InputSource isrc = new InputSource( istrm );
        isrc.setPublicId( publicId );
        isrc.setSystemId( systemId );
        return isrc;
    }
    
}
