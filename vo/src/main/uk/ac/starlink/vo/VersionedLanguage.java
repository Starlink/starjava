package uk.ac.starlink.vo;

import java.util.Arrays;

/**
 * Aggregates a TapLanguage and one of its declared versions.
 *
 * @author   Mark Taylor
 * @since    4 Apr 2023
 */
public class VersionedLanguage {

    private final TapLanguage lang_;
    private final String version_;

    /**
     * Constructor.
     *
     * @param  lang  language
     * @param  version   version string
     * @throws  IllegalArgumentException  if version is not one of language's
     *                                    declared versions
     */
    public VersionedLanguage( TapLanguage lang, String version ) {
        lang_ = lang;
        version_ = version;
        if ( ! Arrays.asList( lang.getVersions() ).contains( version ) ) {
            throw new IllegalArgumentException( "Language \"" + lang 
                                              + "\" does not have a version \""
                                              + version + "\"" );
        }
    }

    /**
     * Returns the language.
     *
     * @return  language
     */
    public TapLanguage getLanguage() {
        return lang_;
    }

    /**
     * Returns the version string.
     *
     * @return  version string
     */
    public String getVersion() {
        return version_;
    }

    /**
     * Returns the version of ADQL represented by this object, if any.
     *
     * @return  ADQL version, or null
     */
    public AdqlVersion getAdqlVersion() {
        if ( lang_ == null || version_ == null ) {
            return null;
        }
        String[] vids = lang_.getVersionIds();
        String[] vnames = lang_.getVersions();
        if ( vids == null || vnames == null || vids.length != vnames.length ) {
            return null;
        }
        int iv = Arrays.asList( vnames ).indexOf( version_ );
        if ( iv < 0 ) {
            return null;
        }
        AdqlVersion idVersion = AdqlVersion.byIvoid( vids[ iv ] );
        if ( idVersion != null ) {
            return idVersion;
        }
        if ( "ADQL".equalsIgnoreCase( lang_.getName() ) ) {
            AdqlVersion numVersion = AdqlVersion.byNumber( vnames[ iv ] );
            if ( numVersion != null ) {
                return numVersion;
            }
        }
        return null;
    }

    /**
     * Returns a representation of this language/version combination in
     * the form &lt;language-name&gt;-&lt;version-name&gt;.
     * This is the form required for the value of the LANG parameter
     * supplied to a TAP service, as defined by section 2.7.1 of TAP 1.1.
     *
     * @return  versioned name
     */
    public String getVersionedName() {
        StringBuffer sbuf = new StringBuffer()
           .append( lang_.getName() );
        if ( version_ != null ) {
            sbuf.append( '-' )
                .append( version_ );
        }
        return sbuf.toString();
    }

    @Override
    public String toString() {
        return getVersionedName();
    }
}
