package uk.ac.starlink.ttools.votlint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.InputSource;

/**
 * Encapsulates VOTable version-specific aspects of validation process.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2009
 */
public abstract class VotableVersion implements Comparable {

    private final String number_;
    private final URL dtdUrl_;
    private final String namespaceUri_;
    private final int sequence_;
    private final Map checkersMap_;

    /** Version object for VOTable 1.0. */
    public static final VotableVersion V10;

    /** Version object for VOTable 1.1. */
    public static final VotableVersion V11;

    /** Version object for VOTable 1.2. */
    public static final VotableVersion V12;

    /** List of known versions. */
    public static final VotableVersion[] KNOWN_VERSIONS = new VotableVersion[] {
        V10 = new VotableVersion10(),
        V11 = new VotableVersion11(),
        V12 = new VotableVersion12(),
    };

    /**
     * Constructor.
     */
    protected VotableVersion( String number, URL dtdUrl, String namespaceUri ) {
        number_ = number;
        dtdUrl_ = dtdUrl;
        namespaceUri_ = namespaceUri;
        sequence_ = Math.round( Float.parseFloat( number ) * 10f );
        checkersMap_ = new HashMap();
    }

    /**
     * Returns the version number as a string.
     *
     * @return  version number string
     */
    public String getNumber() {
        return number_;
    }

    /**
     * Returns the DTD for this version as a SAX InputSource.
     *
     * @param  context  processing context (used for error reporting)
     * @return  DTD source; null if there's a problem
     */
    public InputSource getDTD( VotLintContext context ) {
        if ( dtdUrl_ != null ) {
            InputStream in;
            try {
                in = dtdUrl_.openStream();
            }
            catch ( IOException e ) {
                if ( context.isValidating() ) {
                    context.warning( "Trouble opening DTD - "
                                   + "validation may not be done" );
                }
                return null;
            }
            InputSource saxsrc = new InputSource( in );
            saxsrc.setSystemId( dtdUrl_.toString() );
            return saxsrc;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the namespace URI properly associated with this version.
     * It's null in some cases.
     *
     * @return  XML namespace URI for VOTable elements
     */
    public String getNamespaceUri() {
        return namespaceUri_;
    }

    /**
     * Constructs a new ElementHandler for a given local element name.
     *
     * @param   voTagname  unqualified element name in VOTable namespace
     * @param   context   processing context
     * @return   handler to process an element of type <tt>name</tt>
     */
    public ElementHandler createElementHandler( String voTagname,
                                                VotLintContext context ) {
        if ( voTagname == null ) {
            throw new NullPointerException();
        }
        else {
            ElementHandler handler = createElementHandler( voTagname );
            if ( handler == null ) {
                if ( ! context.isValidating() ) {
                    context.error( "Element " + voTagname
                                 + " not known at VOTable " + getNumber() );
                }
                handler = new ElementHandler();
            }
            handler.configure( voTagname, context );
            return handler;
        }
    }

    /**
     * Returns a map of attribute checkers suitable for processing
     * elements of a given name.
     *
     * @param   voTagname  unqualified element name in VOTable namespace
     * @return  String->AttributeChecker map for checking attributes
     */
    public Map getAttributeCheckers( String voTagname ) {
        if ( ! checkersMap_.containsKey( voTagname ) ) {
            checkersMap_.put( voTagname, createAttributeCheckers( voTagname ) );
        }
        return (Map) checkersMap_.get( voTagname );
    }

    /**
     * Constructs a new element handler for an element with the given 
     * unqualified VOTable tag name.
     *
     * @param  voTagname  unqualified element name
     * @return  element handler, or null if the element is unknown
     */
    protected abstract ElementHandler createElementHandler( String voTagname );

    /**
     * Constructs a map of attribute checkers suitable for processing
     * elements of a given name.
     *
     * @param   voTagname  unqualified element name in VOTable namespace
     * @return  String->AttributeChecker map for checking attributes
     */
    protected abstract Map createAttributeCheckers( String voTagname );

    /**
     * Returns the version number.
     */
    public String toString() {
        return getNumber();
    }

    /**
     * Comparison based on version number.
     */
    public int compareTo( Object o ) {
        VotableVersion other = (VotableVersion) o;
        return this.sequence_ - other.sequence_;
    }

    /**
     * Returns the version object for a given version string.
     *
     * @param   number  version string
     * @return  version object, or null if not known
     */
    public static VotableVersion getVersionByNumber( String number ) {
        for ( int i = 0; i < KNOWN_VERSIONS.length; i++ ) {
            VotableVersion version = KNOWN_VERSIONS[ i ];
            if ( version.getNumber().equals( number ) ) {
                return version;
            }
        }
        return null;
    }

    /**
     * Returns the version object for a given XML namespace URI.
     *
     * @param  namespaceUri  XML namespace associated with version
     * @return  version object, or null if not known
     */
    public static VotableVersion getVersionByNamespace( String namespaceUri ) {
        for ( int i = 0; i < KNOWN_VERSIONS.length; i++ ) {
            VotableVersion version = KNOWN_VERSIONS[ i ];
            String vns = version.getNamespaceUri();
            if ( ( vns == null && namespaceUri == null ) ||
                 ( vns != null && vns.equals( namespaceUri ) ) ) {
                return version;
            }
        }
        return null;
    }

    /**
     * Version implementation for VOTable 1.0.
     */
    private static class VotableVersion10 extends VotableVersion {

        /**
         * Constructor.
         */
        VotableVersion10() {
            super( "1.0",
                   VotableVersion.class.getResource( "votable-1.0.dtd" ),
                   null );
        }

        protected ElementHandler createElementHandler( String name ) {
            if ( "TABLE".equals( name ) ) {
                return new TableHandler();
            }
            else if ( "PARAM".equals( name ) ) {
                return new ParamHandler();
            }
            else if ( "FIELD".equals( name ) ) {
                return new FieldHandler();
            }
            else if ( "DATA".equals( name ) ) {
                return new DataHandler();
            }
            else if ( "TR".equals( name ) ) {
                return new TrHandler();
            }
            else if ( "TD".equals( name ) ) {
                return new TdHandler();
            }
            else if ( "STREAM".equals( name ) ) {
                return new StreamHandler();
            }
            else if ( "BINARY".equals( name ) ) {
                return new BinaryHandler();
            }
            else if ( "FITS".equals( name ) ) {
                return new FitsHandler();
            }
            else if ( "VOTABLE".equals( name ) ||
                      "RESOURCE".equals( name ) ||
                      "DESCRIPTION".equals( name ) ||
                      "DEFINITIONS".equals( name ) ||
                      "INFO".equals( name ) ||
                      "VALUES".equals( name ) ||
                      "MIN".equals( name ) ||
                      "MAX".equals( name ) ||
                      "OPTION".equals( name ) ||
                      "LINK".equals( name ) ||
                      "TABLEDATA".equals( name ) ||
                      "COOSYS".equals( name ) ) {
                return new ElementHandler();
            }
            else {
                return null;
            }
        }

        protected Map createAttributeCheckers( String name ) {
            Map map = new HashMap();
            boolean hasID = false;
            boolean hasName = false;
            if ( name == null ) {
                throw new NullPointerException();
            }
            else if ( "BINARY".equals( name ) ) {
            }
            else if ( "COOSYS".equals( name ) ) {
                hasID = true;
            }
            else if ( "DATA".equals( name ) ) {
            }
            else if ( "DEFINITIONS".equals( name ) ) {
            }
            else if ( "DESCRIPTION".equals( name ) ) {
            }
            else if ( "FIELD".equals( name ) ) {
                hasID = true;
                hasName = true;
                map.put( "ref",
                         new RefChecker( new String[] { "COOSYS", "GROUP" } ) );
            }
            else if ( "FITS".equals( name ) ) {
            }
            else if ( "INFO".equals( name ) ) {
                hasID = true;
                hasName = true;
            }
            else if ( "LINK".equals( name ) ) {
                hasID = true;
            }
            else if ( "MAX".equals( name ) ) {
            }
            else if ( "MIN".equals( name ) ) {
            }
            else if ( "OPTION".equals( name ) ) {
                hasName = true;
            }
            else if ( "PARAM".equals( name ) ) {
                hasID = true;
                hasName = true;
                map.put( "value", new ParamHandler.ValueChecker() );
                map.put( "ref",
                         new RefChecker( new String[] { "COOSYS", "GROUP" } ) );
            }
            else if ( "RESOURCE".equals( name ) ) {
                hasID = true;
                hasName = true;
            }
            else if ( "STREAM".equals( name ) ) {
            }
            else if ( "TABLE".equals( name ) ) {
                hasID = true;
                hasName = true;
                map.put( "ref", new RefChecker( "TABLE" ) );
                map.put( "nrows", new TableHandler.NrowsChecker() );
            }
            else if ( "TABLEDATA".equals( name ) ) {
            }
            else if ( "TD".equals( name ) ) {
                map.put( "ref", new RefChecker( new String[ 0 ] ) );
            }
            else if ( "TR".equals( name ) ) {
            }
            else if ( "VALUES".equals( name ) ) {
                hasID = true;
            }
            else if ( "VOTABLE".equals( name ) ) {
                hasID = true;
                map.put( "version", new VersionChecker() );
            }
    
            if ( hasID ) {
                map.put( "ID", new IDChecker() );
            }
            if ( hasName ) {
                map.put( "name", new NameChecker() );
            }
            return map;
        }
    }

    /**
     * Version implementation for VOTable 1.1.
     */
    private static class VotableVersion11 extends VotableVersion {

        /**
         * Constructor.
         */
        VotableVersion11() {
            super( "1.1", 
                   VotableVersion.class.getResource( "votable-1.1.dtd" ),
                   "http://www.ivoa.net/xml/VOTable/v1.1" );
        }

        protected ElementHandler createElementHandler( String name ) {
            ElementHandler handler = V10.createElementHandler( name );
            if ( handler != null ) {
                return handler;
            }
            else if ( "GROUP".equals( name ) ||
                      "FIELDref".equals( name ) ||
                      "PARAMref".equals( name ) ) {
                return new ElementHandler();
            }
            else {
                return null;
            }
        }

        protected Map createAttributeCheckers( String name ) {
            Map map = V10.createAttributeCheckers( name );
            if ( "LINK".equals( name ) ) {
                map.put( "gref", new DeprecatedAttChecker( "gref" ) );
            }
            else if ( "FIELDref".equals( name ) ) {
                map.put( "ref", new RefChecker( "FIELD" ) );
            } 
            else if ( "GROUP".equals( name ) ) {
                map.put( "ref", new RefChecker( new String[] { "GROUP",
                                                               "COOSYS", } ) );
                map.put( "ID", new IDChecker() );
                map.put( "name", new NameChecker() );
            }
            else if ( "PARAMref".equals( name ) ) {
                map.put( "ref", new RefChecker( "PARAM" ) );
            }
            return map;
        }
    }

    /**
     * Version implementation for VOTable 1.2.
     */
    private static class VotableVersion12 extends VotableVersion {

        /**
         * Constructor.
         */
        VotableVersion12() {
            super( "1.2",
                   VotableVersion.class.getResource( "votable-1.2.dtd" ),
                   "http://www.ivoa.net/xml/VOTable/v1.2" );
        }

        protected ElementHandler createElementHandler( String name ) {
            if ( "COOSYS".equals( name ) ) {
                return new ElementHandler() {
                    public void startElement() {
                        super.startElement();
                        info( "COOSYS is deprecated at VOTable 1.2" );
                    }
                };
            }
            else {
                return V11.createElementHandler( name );
            }
        }

        protected Map createAttributeCheckers( String name ) {
            Map map = V11.createAttributeCheckers( name );
            if ( "GROUP".equals( name ) ) {
                map.put( "ref", new RefChecker( new String[] { "GROUP",
                                                               "COOSYS",
                                                               "TABLE", } ) );
            }
            return map;
        }
    }
}
