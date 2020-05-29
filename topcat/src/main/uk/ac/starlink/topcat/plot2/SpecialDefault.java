package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Represents a configuration default that is sensitive to the
 * table whose data it is plotting.
 *
 * @author   Mark Taylor
 * @since    29 May 2020
 */
public abstract class SpecialDefault<T> {

    private final ConfigKey<T> key_;

    /** Defaults marker size depending on table row count. */
    public static final SpecialDefault<Integer> SIZE =
            new SpecialDefault<Integer>( StyleKeys.SIZE ) {
        public Integer getDefaultValue( TopcatModel tcModel ) {
            long nrow = tcModel == null
                      ? -1
                      : tcModel.getDataModel().getRowCount();
            if ( nrow <= 0 ) {
                return null;
            }
            if ( nrow < 50 ) {
                return Integer.valueOf( 5 );
            }
            if ( nrow <= 200 ) {
                return Integer.valueOf( 3 );
            }
            if ( nrow <= 2000 ) {
                return Integer.valueOf( 2 );
            }
            return Integer.valueOf( 1 );
        }
    };

    /**
     * Constructor.
     *
     * @param  key  config item for which this object operates
     */
    protected SpecialDefault( ConfigKey<T> key ) {
        key_ = key;
    }

    /**
     * Returns the config item for which this object operates.
     *
     * @return   config key
     */
    public ConfigKey<T> getKey() {
        return key_;
    }

    /**
     * Returns the default value to apply to this object's config item.
     *
     * @param  tcModel   table
     * @return   default value appropriate for the supplied table,
     *           or null if the normal default is appropriate
     */
    public abstract T getDefaultValue( TopcatModel tcModel );
}
