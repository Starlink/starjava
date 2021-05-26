package uk.ac.starlink.ttools.votlint;

import java.io.IOException;
import java.io.InputStream;
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
public class VotLintEntityResolver implements EntityResolver, LexicalHandler {

    private final VotLintContext context_;
    private String dtdSystemId_;
    private String dtdPublicId_;

    /** 
     * Constructor.
     *
     * @param  context   lint context
     */
    public VotLintEntityResolver( VotLintContext context ) {
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
            InputSource saxsrc = getVOTableDTD();
            if ( saxsrc != null ) {
                return saxsrc;
            }
        }
        return null;
    }

    /**
     * Returns a SAX input source containing the VOTable DTD.
     *
     * @return   SAX input source containing the DTD
     */
    private InputSource getVOTableDTD() {
        URL dtdUrl = context_.getVersion().getDtdUrl();
        if ( dtdUrl == null ) {
            return null;
        }
        else {
            InputStream in;
            try {
                in = dtdUrl.openStream();
            }
            catch ( IOException e ) {
                if ( context_.isValidating() ) {
                    context_.warning( new VotLintCode( "DTD" ),
                                      "Trouble opening DTD - "
                                    + "validation may not be done" );
                }
                return null;
            }
            InputSource saxsrc = new InputSource( in );
            saxsrc.setSystemId( dtdUrl.toString() );
            return saxsrc;
        }
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
