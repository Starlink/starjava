package uk.ac.starlink.ttools.plot2.config;

/**
 * ConfigMeta implementation that conforms minimally to the requirements.
 * This is only intended for prototyping and test usage, not production code;
 * config keys using instances of this class will not be properly documented
 * in the user-facing documentation.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2018
 */
public class MinimalConfigMeta extends ConfigMeta {

    /**
     * Constructor.
     *
     * @param  name  basic name for this item
     */
    @SuppressWarnings("this-escape")
    public MinimalConfigMeta( String name ) {
        super( name, name );
        setXmlDescription( "<p>(no description)</p>" );
    }
}
