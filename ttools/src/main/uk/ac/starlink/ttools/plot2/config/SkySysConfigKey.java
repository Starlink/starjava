package uk.ac.starlink.ttools.plot2.config;

import uk.ac.starlink.ttools.plot2.geom.SkySys;

/**
 * ConfigKey for selecting sky coordinate systems.
 *
 * @author   Mark Taylor
 * @since    10 Sep 2014
 */
public class SkySysConfigKey extends OptionConfigKey<SkySys> {

    /**
     * Constructor.
     *
     * @param  meta  config key metadata
     * @param  includeNull  true iff null is a permitted option
     */
    public SkySysConfigKey( ConfigMeta meta, boolean includeNull ) {
        super( meta, SkySys.class, SkySys.getKnownSystems( includeNull ) );
    }

    public String valueToString( SkySys sys ) {
        return sys == null ? "generic" : super.valueToString( sys );
    }

    public SkySys stringToValue( String str ) {
        return "generic".equals( str ) ? null
                                       : super.stringToValue( str );
    }

    /**
     * Returns XML text describing the available non-null options.
     *
     * @return   options description
     */
    public static String getDescribedOptionsXml() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>" )
            .append( "Available options are:\n" )
            .append( "<ul>\n" );
        for ( SkySys sys : SkySys.getKnownSystems( false ) ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( sys.getSysName() )
                .append( "</code>: " )
                .append( sys.getSysDescription() )
                .append( "</li>\n" );
        }
        sbuf.append( "</ul>\n" )
            .append( "</p>" );
        return sbuf.toString();
    }
}
