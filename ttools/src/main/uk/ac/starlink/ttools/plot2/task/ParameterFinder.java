package uk.ac.starlink.ttools.plot2.task;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;

/**
 * Used for obtaining a parameter qualified by an optional suffix.
 *
 * <p>In some cases, it is desirable to allow parameters which are
 * logically associated with a layer suffix to be specified without
 * the suffix, for instance if you have three layers using the
 * same table <code>x</code>, it's nice to write "<code>in=x</code>"
 * as a shorthand for "<code>in1=x in2=x in3=x</code>".
 * But if <code>in2=x</code> is present in the environment, that will
 * be used in preference.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2014
 */ 
public abstract class ParameterFinder<P extends Parameter> { 
    
    /**
     * Concrete subclasses must implement this method to create a
     * parameter of the right type with an arbitrary suffix.
     *                                       
     * @param  suffix  arbitrary suffix
     * @return  parameter of with the given suffix
     */ 
    public abstract P createParameter( String suffix );

    /**
     * Calls {@link #findParameter}, but if the result is null,
     * a parameter with the full suffix is returned.
     * In that case, the environment doesn't already have a
     * value for the parameter, but it may take steps to obtain one
     * (like asking the user).
     *
     * @param  env  execution environment, possibly populated with values
     * @param  fullSuffix  suffix associated with the layer for which
     *                     the value is required
     * @return  parameter for obtaining a value associated with the
     *          layer suffix, not null
     */
    public P getParameter( Environment env, String fullSuffix ) {
        P param = findParameter( env, fullSuffix );
        return param != null ? param : createParameter( fullSuffix );
    }

    /**
     * Returns an existing parameter to use for obtaining a value associated
     * with the given layer suffix from the given environment.
     * If the environment contains a value for the parameter
     * with the given suffix, or of any shortened form of that suffix
     * (including the empty string), that parameter is returned.
     * Otherwise, null is returned.
     *
     * @param  env  execution environment, possibly populated with values
     * @param  fullSuffix  suffix associated with the layer for which
     *                     the value is required
     * @return  parameter for obtaining a value associated with the
     *          layer suffix, or null
     */
    public P findParameter( Environment env, String fullSuffix ) {
        Collection<String> names =
            new HashSet<String>( Arrays.asList( env.getNames() ) );
        for ( int i = fullSuffix.length(); i >= 0; i-- ) {
            P param = createParameter( fullSuffix.substring( 0, i ) );
            if ( names.contains( param.getName() ) ) {
                return param;
            }
        }
        return null;
    }

    /**
     * Attempts to locate a parameter known by this finder with the given name.
     * If it can't be found, return null.
     *
     * @param  target  required parameter name (not case-sensitive)
     * @param  fullSuffix  suffix associated with the layer for which
     *                     the value is required
     * @return   parameter with name <code>target</code>, or null
     */
    public P findParameterByName( String target, String fullSuffix ) {
        for ( int i = fullSuffix.length(); i >= 0; i-- ) {
            P param = createParameter( fullSuffix.substring( 0, i ) );
            if ( target.equalsIgnoreCase( param.getName() ) ) {
                return param;
            }
        }
        return null;
    }
}
