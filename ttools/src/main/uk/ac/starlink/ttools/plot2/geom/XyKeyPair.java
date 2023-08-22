package uk.ac.starlink.ttools.plot2.geom;

import java.util.function.Function;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;

/**
 * Represents a pair of configuration keys that apply to the X and Y
 * axes of a plane plot.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2023
 */
public class XyKeyPair<T> {

    private final Function<String,ConfigKey<T>> keyFactory_;
    private final ConfigKey<T> xKey_;
    private final ConfigKey<T> yKey_;

    /**
     * Constructor.
     *
     * @param   keyFactory  maps an axis name (such as "X" or "Y")
     *                      to a configuration key
     */
    public XyKeyPair( Function<String,ConfigKey<T>> keyFactory ) {
        keyFactory_ = keyFactory;
        xKey_ = keyFactory.apply( "X" );
        yKey_ = keyFactory.apply( "Y" );
    }

    /**
     * Returns the config key for the X axis.
     *
     * @return  X config key
     */
    public ConfigKey<T> getKeyX() {
        return xKey_;
    }

    /**
     * Returns the config key for the Y axis.
     *
     * @return  Y config key
     */
    public ConfigKey<T> getKeyY() {
        return yKey_;
    }

    /**
     * Creates a new key of this type with a named axis.
     *
     * @param  axName  axis name
     * @return   config key for named axis
     */
    public ConfigKey<T> createKey( String axName ) {
        return keyFactory_.apply( axName );
    }
}
