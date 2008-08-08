package uk.ac.starlink.tptask;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.tplot.Style;

/**
 * Defines an object which can obtain a {@link uk.ac.starlink.tplot.Style}
 * object from the environment.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2008
 */
public interface StyleFactory {

    /**
     * Returns the parameters used by this factory.
     *
     * @param  stSuffix  label identifying the data set for which the style
     *                   will be required
     */
    Parameter[] getParameters( String stSuffix );

    /**
     * Obtains a Style object from the environment by examining parameters.
     *
     * @param  env  execution environment
     * @param  stSuffix  label identifying the data set for which the style
     *                   is required
     * @return   plotting style
     */
    Style getStyle( Environment env, String stSuffix ) throws TaskException;
}
