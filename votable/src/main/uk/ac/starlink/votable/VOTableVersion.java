package uk.ac.starlink.votable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides characteristics for a given version of the VOTable standard.
 * An instance of this class is passed to a {@link VOTableWriter} to 
 * indicate what version of the standard should be followed when generating
 * VOTable output.
 *
 * @author   Mark Taylor
 * @since    15 Nov 2012
 */
public abstract class VOTableVersion {

    /** VOTable 1.0. */
    public static final VOTableVersion V10;

    /** VOTable 1.1. */
    public static final VOTableVersion V11;

    /** VOTable 1.2. */
    public static final VOTableVersion V12;

    /** VOTable 1.3. */
    public static final VOTableVersion V13;

    private static final Map<String,VOTableVersion> VERSION_MAP =
        Collections.unmodifiableMap( createMap( new VOTableVersion[] {
            V10 = new VersionLike10( "1.0" ),
            V11 = new VersionLike11( "1.1" ),
            V12 = new VersionLike12( "1.2" ),
            V13 = new VersionLike13( "1.3" ),
        } ) );

    /** 
     * Default VOTable version number which output will conform to
     * if not otherwise specified ({@value}).
     */
    public static final String DEFAULT_VERSION_STRING = "1.2";

    /**
     * System property name whose value gives the default VOTable version
     * written by instances of this class if no version is given explicitly.
     * The property is named {@value} and if it is not supplied the
     * version defaults to the value of {@link #DEFAULT_VERSION_STRING}
     * (={@value #DEFAULT_VERSION_STRING}).
     */
    public static final String VOTABLE_VERSION_PROP = "votable.version";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    private final String versionNumber_;

    /**
     * Constructor.
     *
     * @param   versionNumber  the number (like "1.1") identifying this version
     */
    protected VOTableVersion( String versionNumber ) {
        versionNumber_ = versionNumber;
    }

    /**
     * Returns the version number for this version.
     *
     * @return  version number (like "1.1")
     */
    public String getVersionNumber() {
        return versionNumber_;
    }

    /**
     * Returns the XML namespace in which the VOTable elements reside.
     *
     * @return  VOTable XML namespace, or null
     */
    public abstract String getXmlNamespace();

    /**
     * Returns the URL of the VOTable schema corresponding to this version.
     *
     * @return  VOTable schema, or null
     */
    public abstract String getSchemaLocation();

    /**
     * Returns the text of the DOCTYPE XML declaration for this version.
     *
     * @return  doctype declaration, or null
     */
    public abstract String getDoctypeDeclaration();

    /**
     * Indicates whether this version permits an empty TD element to represent
     * a null value for <em>all</em> data types.
     *
     * @return  true iff empty TD elements are always permitted
     */
    public abstract boolean allowEmptyTd();

    /**
     * Indicates whether the BINARY2 serialization format is defined by
     * this version.
     *
     * @return  true iff BINARY2 is allowed
     */
    public abstract boolean allowBinary2();

    /**
     * Indicates whether the xtype attribute is permitted on FIELD elements
     * etc in this version.
     *
     * @return  true iff xtype attribute is allowed
     */
    public abstract boolean allowXtype();

    /**
     * Returns version number.
     */
    @Override
    public String toString() {
        return versionNumber_;
    }

    /**
     * Returns a number->version map for all known versions.
     * The map keys are version number strings like "1.1".
     *
     * @return   version map
     */
    public static Map<String,VOTableVersion> getKnownVersions() {
        return VERSION_MAP;
    }

    /**
     * Returns the version instance used by default for output in this JVM.
     * By default this is determined by the value of the
     * {@link #DEFAULT_VERSION_STRING} constant, but it can be
     * overridden by use of the {@link #VOTABLE_VERSION_PROP}
     * ({@value #VOTABLE_VERSION_PROP}) system property.
     *
     * @return  default VOTable version for output
     */
    public static VOTableVersion getDefaultVersion() {
        String vnum = DEFAULT_VERSION_STRING;
        try {
            vnum = System.getProperty( VOTABLE_VERSION_PROP, vnum );
            if ( ! VERSION_MAP.containsKey( vnum ) ) {
                logger_.warning( "Unknown VOTable version \"" + vnum
                               + "\" - use default " + DEFAULT_VERSION_STRING );
                vnum = DEFAULT_VERSION_STRING;
            }
        }
        catch ( SecurityException e ) {
        }
        VOTableVersion version = VERSION_MAP.get( vnum );
        assert version != null;
        return version;
    }

    /**
     * Creates a map of names to versions from a list of versions.
     *
     * @param  versions  array of versions
     * @return  name->version map
     */
    private static Map<String,VOTableVersion>
            createMap( VOTableVersion[] versions ) {
        Map<String,VOTableVersion> map =
            new LinkedHashMap<String,VOTableVersion>();
        for ( int i = 0; i < versions.length; i++ ) {
            VOTableVersion vers = versions[ i ];
            map.put( vers.getVersionNumber(), vers );
        }
        return map;
    }

    /**
     * VOTable 1.0-like version instance.
     */
    private static class VersionLike10 extends VOTableVersion {

        /**
         * Constructor.
         *
         * @param  version   version number
         */
        VersionLike10( String version ) {
            super( version );
        }

        public String getXmlNamespace() {
            return "http://vizier.u-strasbg.fr/VOTable";
        }
        public String getSchemaLocation() {
            return null;
        }
        public String getDoctypeDeclaration() {
            return "<!DOCTYPE VOTABLE SYSTEM "
                 + "\"http://us-vo.org/xml/VOTable.dtd\">";
        }
        public boolean allowXtype() {
            return false;
        }
        public boolean allowEmptyTd() {
            return false;
        }
        public boolean allowBinary2() {
            return false;
        }
    }

    /**
     * VOTable 1.1-like version instance.
     */
    private static class VersionLike11 extends VersionLike10 {

        /**
         * Constructor.
         *
         * @param  version   version number
         */
        VersionLike11( String version ) {
            super( version );
        }

        @Override
        public String getXmlNamespace() {
            return "http://www.ivoa.net/xml/VOTable/v" + getVersionNumber();
        }
        @Override
        public String getSchemaLocation() {
            return getXmlNamespace();
        }
        @Override
        public String getDoctypeDeclaration() {
            return null;
        }
    }

    /**
     * VOTable 1.2-like version instance.
     */
    private static class VersionLike12 extends VersionLike11 {

        /**
         * Constructor.
         *
         * @param  version   version number
         */
        VersionLike12( String version ) {
            super( version );
        }

        @Override 
        public boolean allowXtype() {
            return true;
        }
    }

    /**
     * VOTable 1.3-like version instance.
     */
    private static class VersionLike13 extends VersionLike12 {

        /**
         * Constructor.
         *
         * @param  version   version number
         */
        VersionLike13( String version ) {
            super( version );
        }

        @Override
        public boolean allowEmptyTd() {
            return true;
        }
        @Override
        public boolean allowBinary2() {
            return true;
        }
    }
}
