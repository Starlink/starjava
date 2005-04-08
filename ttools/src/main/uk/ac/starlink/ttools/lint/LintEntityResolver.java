package uk.ac.starlink.ttools.lint;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.ext.LexicalHandler;

/**
 * Provides entity resolution for the VOTable linter.
 * In particular, when registered as a LexicalHandler during the VOTable
 * parse, this resolver will make sure that whatever entity is declared
 * as the external DTD in fact resolves to the (locally held) text of
 * one of the VOTable DTDs (according to the context's version).
 * This both ensures that we're using a known, correct copy of the DTD,
 * and prevents the necessity for making external network connections.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Apr 2005
 */
public class LintEntityResolver implements EntityResolver, LexicalHandler {

    private final LintContext context_;
    private String dtdSystemId_;
    private String dtdPublicId_;

    /** 
     * Constructor.
     *
     * @param  context   lint context
     */
    public LintEntityResolver( LintContext context ) {
        context_ = context;
    }

    public void startDTD( String name, String publicId, String systemId ) {

        /* Invoked when the <!DOCTYPE> declaration is encountered (if at all).
         * Store the public and system IDs associated with the declaration
         * entity. */
        dtdPublicId_ = publicId;
        dtdSystemId_ = systemId;
    }

    public InputSource resolveEntity( String publicId, String systemId ) {

        /* If the entity has the same (non-blank) system or public ID as
         * the one which was declared in the DOCTYPE declaration, 
         * use the local copy. */
        if ( ( publicId != null && publicId.trim().length() > 0 && 
               publicId.equals( dtdPublicId_ ) ) ||
             ( systemId != null && systemId.trim().length() > 0 &&
               systemId.equals( dtdSystemId_ ) ) ) {
            InputSource saxsrc = getVOTableDTD( context_.getVersion() );
            if ( saxsrc != null ) {
                return saxsrc;
            }
        }
        return null;
    }

    /**
     * Returns a SAX input source containing the VOTable DTD.
     *
     * @param   version  VOTable version for which the DTD is required
     * @return   SAX input source containing the DTD
     */
    private InputSource getVOTableDTD( String version ) {

        /* Work out the filename for the requested version. */
        String filename;
        if ( LintContext.V10.equals( version ) ) {
            filename = "votable-1.0.dtd";
            // systemId = "http://us-vo.org/xml/VOTable.dtd";
        }
        else {
            if ( null == version ) {
                context_.info( "Unspecified VOTable version - " +
                               "validating for V1.1" );
            }
            else if ( LintContext.V11.equals( version ) ) {
                // no action
            }
            else {
                context_.info( "Unknown VOTable version " + version + " - " +
                               "validating for V1.1" );
            }
            filename = "votable-1.1.dtd";
        }

        /* Construct and return a SAX source based on the filename we have. */
        URL url = getClass().getResource( filename );
        try {
            if ( url != null ) {
                InputSource saxsrc = new InputSource( url.openStream() );
                saxsrc.setSystemId( url.toString() );
                return saxsrc;
            }
        }
        catch ( IOException e ) {
            context_.warning( "Trouble opening DTD - " + 
                              "validation may not be done" );
        }
        return null;
    }

    /*
     * Dummy implementations of other LexicalHandler methods.
     */
    public void endDTD() {}
    public void startCDATA() {}
    public void endCDATA() {}
    public void startEntity( String name ) {}
    public void endEntity( String name ) {}
    public void comment( char[] ch, int start, int length ) {}
}
