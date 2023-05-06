package uk.ac.starlink.ttools.plot2.config;

import uk.ac.starlink.ttools.plot2.geom.SkySys;

/**
 * ConfigKey for selecting sky coordinate systems.
 *
 * @author   Mark Taylor
 * @since    10 Sep 2014
 */
public class SkySysConfigKey extends OptionConfigKey<SkySys> {

    private final boolean isViewComparison_;

    /**
     * Constructor.
     *
     * <p>The <code>isViewComparison</code> parameter does not affect
     * the behaviour of this key itself, but may be used by external code
     * that needs to know whether values configured here are resolved
     * with reference to the sky system applying to the plot as a whole.
     *
     * @param  meta  config key metadata
     * @param  includeNull  true iff null is a permitted option
     * @param  isViewComparison  true iff this key is used to configure
     *                           a system that will be compared with
     *                           the plot view system
     */
    public SkySysConfigKey( ConfigMeta meta, boolean includeNull,
                            boolean isViewComparison ) {
        super( meta, SkySys.class, SkySys.getKnownSystems( includeNull ) );
        isViewComparison_ = isViewComparison;
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

    /**
     * Indicates whether this key is used to select a sky system that
     * will be compared with the view system of the plot itself.
     *
     * @return  true for view comparison keys
     */
    public boolean isViewComparison() {
        return isViewComparison_;
    }
}
