package uk.ac.starlink.vo;

import adql.parser.ADQLParser;

/**
 * Version of the ADQL language.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2019
 */
public enum AdqlVersion {

    /** ADQL version 2.0. */
    V20( "2.0", "2.0", "ivo://ivoa.net/std/ADQL#v2.0",
         ADQLParser.ADQLVersion.V2_0,
         new String[] {
             "features-udf",
             "features-adqlgeo",
         } ),
 
    /**
     * ADQL version 2.1.
     * This corresponds to PR-ADQL-2.1-20180112, except that the feature
     * "<code>ivo://ivoa.net/std/TAPRegExt#features-adql-geo</code>"
     * defined there is not included; I believe that's a typo for
     * "<code>ivo://ivoa.net/std/TAPRegExt#features-adqlgeo</code>"
     * that applies to ADQL 2.0.
     */
    V21( "2.1-PR", "2.1", "ivo://ivoa.net/std/ADQL#v2.1",
         ADQLParser.ADQLVersion.V2_1,
         new String[] {
             "features-udf",
             "features-adqlgeo",
             "features-adql-bitwise",
             "features-adql-common-table",
             "features-adql-offset",
             "features-adql-sets",
             "features-adql-string",
             "features-adql-type",
             "features-adql-unit",
         } );

    private final String name_;
    private final String number_;
    private final String ivoid_;
    private final ADQLParser.ADQLVersion volltVersion_;
    private final String[] featureUris_;

    /**
     * Constructor.
     *
     * @param  name   version name
     * @param  number  X.Y format version number
     * @param  ivoid   IVO identifier
     * @param  volltVersion  corresponding version object from VOLLT library
     * @param  tapregextFeatureFragments  list of ADQL language features
     *                                    for this version
     */
    AdqlVersion( String name, String number, String ivoid,
                 ADQLParser.ADQLVersion volltVersion,
                 String[] tapregextFeatureFragments ) {
        name_ = name;
        number_ = number;
        ivoid_ = ivoid;
        volltVersion_ = volltVersion;
        int nfeat = tapregextFeatureFragments.length;
        featureUris_ = new String[ nfeat ];
        for ( int ifeat = 0; ifeat < nfeat; ifeat++ ) {
            featureUris_[ ifeat ] = TapCapability.TAPREGEXT_STD_URI + "#"
                                  + tapregextFeatureFragments[ ifeat ];
        }
    }

    /**
     * Returns the informal name of this version.
     *
     * @return  version name, suitable for presentation to user
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the version number as a string.
     *
     * @return  X.Y format version number
     */
    public String getNumber() {
        return number_;
    }

    /**
     * Returns the version identifier string.
     *
     * @return  URI, currently of the form ivo://ivoa.net/std/ADQL#vX.Y
     */
    public String getIvoid() {
        return ivoid_;
    }

    /**
     * Returns an array of all the language feature URIs defined by this
     * version.  These currently all have the form
     * "<code>ivo://ivoa.net/std/TAPRegExt#features-*</code>".
     * These are defined in the TAPRegExt and ADQL standards.
     *
     * @return  language feature URIs
     */
    public String[] getFeatureUris() {
        return featureUris_.clone();
    }

    /**
     * Returns the corresponding version identifier from the VOLLT
     * ADQL parser library.
     *
     * @return  version identifier, not null
     */
    ADQLParser.ADQLVersion getVolltVersion() {
        return volltVersion_;
    }

    @Override
    public String toString() {
        return "V" + name_;
    }

    /**
     * Returns the AdqlVersion instance corresponding to a version number.
     *
     * @param  number  version number of the form X.Y
     * @return  corresponding version instance, or null
     */
    public static AdqlVersion byNumber( String number ) {
        for ( AdqlVersion v : values() ) {
            if ( v.number_.equals( number ) ) {
                return v;
            }
        }
        return null;
    }

    /**
     * Returns the AdqlVersion instance corresponding to an IVOID.
     *
     * @param  ivoid  URI, currently of the form ivo://ivoa.net/std/ADQL#vX.Y
     */
    public static AdqlVersion byIvoid( String ivoid ) {
        for ( AdqlVersion v : values() ) {
            if ( v.ivoid_.equalsIgnoreCase( ivoid ) ) {
                return v;
            }
        }
        return null;
    }
}
