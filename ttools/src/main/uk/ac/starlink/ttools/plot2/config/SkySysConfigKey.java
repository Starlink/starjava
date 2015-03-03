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

    public SkySys stringToValue( String str ) throws ConfigException {
        return "generic".equals( str ) ? null
                                       : super.stringToValue( str );
    }

    public String getXmlDescription( SkySys sys ) {
        return sys == null ? null :  sys.getSysDescription();
    }
}
