package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides specialised XML entity resolution.
 * This resolver knows about some entities which starjava applications
 * are likely to want to retrieve; it keeps copies of them so that
 * no network connection is required for them.
 * <p>
 * Use {@link #getInstance} to obtain an instance of this class without
 * a parent.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarEntityResolver implements EntityResolver {

    private static StarEntityResolver instance = new StarEntityResolver();
    private EntityResolver parent;
    private static final String VOT_URI_BASE =
        "http://www.ivoa.net/xml/VOTable/";

    /**
     * Private no-arg constructor.
     */
    private StarEntityResolver() {
    }

    /**
     * Constructs a resolver which will resolve entities this class knows
     * about, and for those it doesn't it will defer resolution to a supplied
     * parent resolver.
     *
     * @param   parent   fallback resolver (may be <code>null</code>)
     */
    public StarEntityResolver( EntityResolver parent ) {
        this.parent = parent;
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   StarEntityResolver instance
     */
    public static StarEntityResolver getInstance() {
        return instance;
    }

    /**
     * Resolves an entity if it is one of the ones that we keep on hand.
     */
    public InputSource resolveEntity( String publicId, String systemId ) 
            throws SAXException, IOException {
        String local = getLocalResource( publicId, systemId );
        if ( local != null ) {
            InputStream istrm = getClass().getResourceAsStream( local );
            InputSource isrc = new InputSource( istrm );
            isrc.setPublicId( publicId );
            isrc.setSystemId( systemId );
            return isrc;
        }
        else if ( parent != null ) {
            return parent.resolveEntity( publicId, systemId );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a resource name locating the entity referenced by a given
     * public/system ID pair.  If there is a local copy of this entity,
     * its path relative to this class is returned, otherwise <code>null</code>
     * is returned.
     *
     * @param  systemId  the entity's system ID
     * @param  publicId  the entity's public ID
     * @return  path to the resource, or <code>null</code> if it is unknown
     */
    protected String getLocalResource( String publicId, String systemId ) {

        /* Avoid null pointer exceptions. */
        if ( publicId == null ) {
            publicId = "";
        }
        if ( systemId == null ) {
            systemId = "";
        }

        /* VOTable DTD. */
        if ( systemId.endsWith( "VOTable.dtd" ) ) { 
            return "text/VOTable.dtd";
        }

        /* VOTable 1.1 schema. */
        if ( systemId.equals( VOT_URI_BASE + "v1.1" ) ||
             systemId.equals( VOT_URI_BASE + "v1.1/" ) ||
             systemId.startsWith( VOT_URI_BASE + "v1.1" )
             && systemId.endsWith( ".xsd" ) ||
             systemId.equals( VOT_URI_BASE + "votable-1.1.xsd" ) ) {
            return "text/VOTable1.1.xsd";
        }

        /* VOTable 1.2 schema. */
        if ( systemId.equals( VOT_URI_BASE + "v1.2" ) ||
             systemId.equals( VOT_URI_BASE + "votable-1.2.xsd" ) ) {
            return "text/VOTable1.2.xsd";
        }

        /* VOTable 1.3 schema. */
        if ( systemId.equals( VOT_URI_BASE + "votable-1.3.xsd" ) ) {
            return "text/VOTable1.3.xsd";
        }

        /* VOTable 1.4 schema. */
        if ( systemId.equals( VOT_URI_BASE + "votable-1.4.xsd" ) ) {
            return "text/VOTable1.4.xsd";
        }

        /* Latest version of 1.* series VOTable.
         * See the IVOA Endorsed Note "XML Schema Versioning Policies"
         * (http://www.ivoa.net/documents/Notes/XMLVers/) and the commentary
         * in VOTable 1.4 section 3 to explain why the "v1.3" URI is used
         * for the latest, not necessarily v1.3, schema for versions 1.3+. */
        if ( systemId.equals( VOT_URI_BASE + "v1.3" ) ) {
            return "text/VOTable1.4.xsd";
        }

        /* VOTable 1.0 schema. */
        if ( systemId.endsWith( "VOTable.xsd" ) ) {
            return "text/VOTable.xsd";
        }

        /* Astrores DTD. */
        if ( systemId.endsWith( "astrores.dtd" ) ) {
            return "text/astrores.dtd";
        }

        /* JavaHelp DTDs. */
        String javaHelpId = "-//Sun Microsystems Inc.//DTD JavaHelp ";
        if ( publicId.equals( javaHelpId + "HelpSet Version 1.0//EN" ) ) {
            return "text/helpset_1_0.dtd";
        }
        if ( publicId.equals( javaHelpId + "TOC Version 1.0//EN" ) ) {
            return "text/toc_1_0.dtd";
        }
        if ( publicId.equals( javaHelpId + "Map Version 1.0//EN" ) ) {
            return "text/map_1_0.dtd";
        }

        /* Don't know. */
        return null;
    }
}
