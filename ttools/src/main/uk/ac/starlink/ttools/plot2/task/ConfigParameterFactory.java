package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;

/**
 * Provides a mapping from ConfigKeys to Parameters.
 * Although the implementation of simply adapting from ConfigKey to
 * Parameter is provided by the {@link ConfigParameter} class,
 * implementations of this class are often required to do considerably
 * more work in the context of the supplied execution
 * {@link uk.ac.starlink.task.Environment}, including working out
 * a parameter name based on variants of the key name including
 * relevant layer/zone suffixes, configuring the parameters with
 * suitable defaults, etc.
 *
 * <p>This interface is used extensively by {@link AbstractPlot2Task}.
 * 
 * <p>This looks like it should be marked as a {@link FunctionalInterface},
 * and the compiler does permit that, but I can't figure out how to write
 * a lambda expression corresponding to a Single Abstract Method
 * like the one here which is parameterised by a type T, so there
 * doesn't seem much point.  I don't <em>think</em> it's possible;
 * the Java Language Specification (JLS SE8, Sec 15.27) says
 * "Lambda expressions cannot declare type parameters".
 *
 * @author   Mark Taylor
 * @since    23 Aug 2023
 */
public interface ConfigParameterFactory {

    /**
     * Produces a parameter to find the value for a given config key.
     *
     * @param   env  execution environment
     * @param   key  config key
     * @return   parameter that can get a value for <code>key</code>
     */
    <T> ConfigParameter<T> getParameter( Environment env, ConfigKey<T> key )
             throws TaskException;
}
