package uk.ac.starlink.votable;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Element;
import uk.ac.starlink.util.Loader;

/**
 * Determines how namespaces are handled in VOTable documents.
 * The static {@link #getInstance} method provides an object which
 * encapsulates the default namespacing policy.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2009
 */
public abstract class Namespacing {

    private final String name_;
    private static Namespacing instance_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Name of system property which determines namespacing policy used.
     * Property name is {@value}.
     * Possible values are
     * "<code>none</code>", "<code>lax</code>", "<code>strict</code>"
     * or the classname of a <code>Namespacing<code> implementation
     * which has a no-arg constructor.
     */
    public static final String POLICY_PROP = "votable.namespacing";

    /** No namespace awareness; any namespacing will probably confuse parser. */
    public static final Namespacing NONE;

    /** Interpret likely-looking elements in any namespace as VOTable ones. */
    public static final Namespacing LAX;

    /** Only elements in namespaces with VOTable URIs are significant. */
    public static final Namespacing STRICT;

    /** All known Namespacing values. */
    private static final Namespacing[] KNOWN_POLICIES = new Namespacing[] {
        NONE = new NoNamespacing(),
        LAX = new LaxNamespacing(),
        STRICT = new StrictNamespacing(),
    };

    /** Default policy name. */
    private static final String POLICY_DEFAULT = LAX.toString();

    /**
     * Constructor.
     *
     * @param  name  concise human-readable name describing this policy
     */
    public Namespacing( String name ) {
        name_ = name;
    }

    /**
     * Performs any necessary configuration of the namespacing capabilities
     * of a given parser factory for use with this namespacing policy.
     *
     * @param  spfact   factory to configure
     */
    public abstract void configureSAXParserFactory( SAXParserFactory spfact );

    /**
     * Returns the VOTable tagname for an XML element as encountered by a
     * SAX parser.
     * The return value is a bare string which may be compared to one of
     * the VOTable element names ignoring namespaces; for instance
     * a return value of "TABLE" indicates that this is a VOTable TABLE
     * element.
     *
     * <p>The parameters have the same meanings, and may or may not be 
     * present as for, those in 
     * {@link org.xml.sax.ContentHandler#startElement}.
     *
     * @param   namespaceURI  namespace URI
     * @param   localName     local name
     * @param   qName         qualified name
     * @return   element name in VOTable namespace
     */
    public abstract String getVOTagName( String namespaceURI, String localName,
                                         String qName );

    /**
     * Returns the VOTable tagname for an XML element as present in a DOM.
     * The return value is a bare string which may be compared to one of
     * the VOTable element names ignoring namespaces; for instance
     * a return value of "TABLE" indicates that this is a VOTable TABLE
     * element.
     *
     * @param   el  element
     * @return   element name in VOTable namespace
     */
    public String getVOTagName( Element el ) {
        return getVOTagName( el.getNamespaceURI(), el.getLocalName(),
                             el.getTagName() );
    }

    /**
     * Returns the name of this namespacing policy.
     *
     * @return  name
     */
    public String toString() {
        return name_;
    }

    /**
     * Returns the default Namespacing instance.
     * The default value is determined by examining the {@link #POLICY_PROP}
     * system property.
     * If not otherwise set the default is currently {@link #LAX}.
     *
     * @return   namespacing instance
     */
    public static Namespacing getInstance() {
        if ( instance_ == null ) {
            String policy;
            try {
                policy = System.getProperty( POLICY_PROP, POLICY_DEFAULT );
            }
            catch ( SecurityException e ) {
                policy = POLICY_DEFAULT;
            }
            Namespacing ns = null;
            for ( int i = 0; i < KNOWN_POLICIES.length; i++ ) {
                if ( KNOWN_POLICIES[ i ].toString()
                                        .equalsIgnoreCase( policy ) ) {
                    ns = KNOWN_POLICIES[ i ];
                }
            }
            if ( ns == null ) {
                ns = (Namespacing)
                     Loader.getClassInstance( policy, Namespacing.class );
            }
            if ( ns == null ) {
                StringBuffer sbuf = new StringBuffer()
                    .append( "Unknown value \"" )
                    .append( policy )
                    .append( "\" for system property " )
                    .append( POLICY_PROP )
                    .append( ".  Known values: " );
                for ( int i = 0; i < KNOWN_POLICIES.length; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( KNOWN_POLICIES[ i ] );
                }
                sbuf.append( "." );
                throw new RuntimeException( sbuf.toString() );
            }
            instance_ = ns;
            logger_.config( "VOTable namespacing mode is " + instance_ );
        }
        return instance_;
    }

    /**
     * Sets the default instance.
     *
     * @param  instance  new default instance
     */
    public void setInstance( Namespacing instance ) {
        instance_ = instance;
    }

    /**
     * Implementation ignoring namespaces.
     */
    private static class NoNamespacing extends Namespacing {
        NoNamespacing() {
            super( "none" );
        }

        public void configureSAXParserFactory( SAXParserFactory spfact ) {
            spfact.setNamespaceAware( false );
        }

        public String getVOTagName( String namespaceURI, String localName,
                                    String qName ) {
            if ( qName != null ) {
                return qName;
            }
            else {
                assert false;
                return localName;
            }
        }
    }

    /**
     * Implementation which tries to make sense of anything that looks like
     * it might be a VOTable element.
     */
    private static class LaxNamespacing extends Namespacing {
        LaxNamespacing() {
            super( "lax" );
        }

        public void configureSAXParserFactory( SAXParserFactory fact ) {
            fact.setNamespaceAware( false );
        }

        public String getVOTagName( String namespaceURI, String localName,
                                    String qName ) {
            if ( qName != null ) {
                int iColon = qName.indexOf( ':' );
                return iColon >= 0 ? qName.substring( iColon + 1 )
                                   : qName;
            }
            else {
                return localName;
            }
        }
    }

    /**
     * Strict implementation; requires that namespacing is done correctly.
     */
    private static class StrictNamespacing extends Namespacing {
        private final Pattern votUriRegex_;

        StrictNamespacing() {
            super( "strict" );
            votUriRegex_ = Pattern.compile( "\\Q"
                                          + "http://www.ivoa.net/xml/VOTable/v"
                                          + "\\E"
                                          + "[0-9\\.\\-]+" );
        }

        public void configureSAXParserFactory( SAXParserFactory spfact ) {
            spfact.setNamespaceAware( true );
        }

        public String getVOTagName( String namespaceURI, String localName,
                                    String qName ) {
            if ( localName != null && localName.trim().length() > 0 ) {
                if ( isVotableUri( namespaceURI ) ) {
                    return localName;
                }
                else {
                    if ( qName.endsWith( ":VOTABLE" ) ||
                         qName.equals( "VOTABLE" ) ||
                         localName.equals( "VOTABLE" ) ) {
                        logger_.warning( "<VOTABLE> element in non-VOTable"
                                       + " namespace " + namespaceURI
                                       + " not treated as VOTable" );
                    }
                    if ( qName != null ) {
                        return qName.indexOf( ':' ) >= 0 ? qName
                                                         : ( ":" + qName );
                    }
                    else {
                        return localName;
                    }
                }
            }
            else {
                return qName;
            }
        }

        /**
         * Indicates whether a given URI is a correct VOTable one.
         *
         * @param  uri  namespace URI
         * @return   true iff <code>uri</code> is for VOTable elements
         */
        private boolean isVotableUri( String uri ) {
            return votUriRegex_.matcher( uri ).matches();
        }
    };
}
