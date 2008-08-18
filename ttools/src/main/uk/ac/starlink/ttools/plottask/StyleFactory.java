package uk.ac.starlink.ttools.plottask;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.Style;

/**
 * Defines an object which can obtain a {@link uk.ac.starlink.ttools.plot.Style}
 * object from the environment.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2008
 */
public abstract class StyleFactory {

    private final String prefix_;
    private final List suffixList_;

    /**
     * Constructor.
     *
     * @param  prefix  prefix to be prepended to all parameters used by this
     *                 factory
     */
    protected StyleFactory( String prefix ) {
        prefix_ = prefix;
        suffixList_ = new ArrayList();
    }

    /**
     * Returns the parameters used by this factory.
     *
     * @param  stSuffix  label identifying the data set for which the style
     *                   will be required
     */
    public abstract Parameter[] getParameters( String stSuffix );

    /**
     * Obtains a Style object from the environment by examining parameters.
     *
     * @param  env  execution environment
     * @param  stSuffix  label identifying the data set for which the style
     *                   is required
     * @return   plotting style
     */
    public abstract Style getStyle( Environment env, String stSuffix )
            throws TaskException;

    /**
     * Assembles a parameter name from a base name and a dataset suffix.
     *
     * @param  baseName  parameter base name
     * @param  stSuffix  label identifying dataset
     * @return  parameter name
     */
    public String paramName( String baseName, String stSuffix ) {
        return prefix_ + baseName + stSuffix;
    }

    /**
     * Returns a zero-based index associated with a given suffix for this
     * factory.  The same suffix will always give the same result.
     *
     * @param  suffix  identifier
     * @return   identifier index
     */
    public int getStyleIndex( String suffix ) {
        if ( ! suffixList_.contains( suffix ) ) {
            suffixList_.add( suffix );
        }
        return suffixList_.indexOf( suffix );
    }
}
