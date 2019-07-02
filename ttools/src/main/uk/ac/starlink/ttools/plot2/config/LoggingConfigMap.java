package uk.ac.starlink.ttools.plot2.config;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * ConfigMap wrapper that issues a logging message for each item that
 * is queried.  Only the first query for each key is logged.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2013
 */
public class LoggingConfigMap extends ConfigMap {

    private final ConfigMap base_;
    private final Level level_;
    private final Logger logger_;
    private final Set<ConfigKey<?>> logged_;

    /**
     * Constructor.
     *
     * @param   base   base configmap
     * @param   level  level at which logging messages will be issued
     */
    public LoggingConfigMap( ConfigMap base, Level level ) {
        base_ = base;
        level_ = level;
        logger_ = Logger.getLogger( getClass().getName() );
        logged_ = new HashSet<ConfigKey<?>>();
    }

    @Override
    public <T> T get( ConfigKey<T> key ) {
        T value = super.get( key );
        if ( logger_.isLoggable( level_ ) && logged_.add( key ) ) {
            logger_.log( level_,
                         new StringBuffer()
                        .append( "Config: " )
                        .append( key.getMeta().getShortName() )
                        .append( "=" )
                        .append( value )
                        .toString() );
        }
        return value;
    }
}
