package uk.ac.starlink.votable;

import java.io.InputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Provides specialised XML entity resolution for VOTables.
 * The package keeps local copies of some resources (currently just
 * the VOTable 1.0 DTD) in {@link #TEXT_PATH}.  Any system ID which
 * ends with "VOTable.dtd" is currently assumed to refer to this
 * document.  In this way
 * it doesn't need to make external connections to resolve these.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableEntityResolver implements EntityResolver {

    public static final String TEXT_PATH = "uk/ac/starlink/votable/text/";

    private static EntityResolver instance = new VOTableEntityResolver();

    private ClassLoader loader = getClass().getClassLoader();

    /**
     * Returns the sole instance of this singleton class.
     */
    public static EntityResolver getInstance() {
        return instance;
    }

    private VOTableEntityResolver() {
    }

    public InputSource resolveEntity( String publicId, String systemId ) {
        String resourcePath;
        if ( systemId.endsWith( "VOTable.dtd" ) ) {
            resourcePath = TEXT_PATH + "VOTable.dtd";
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
