package uk.ac.starlink.task;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract superclass for parameters that permit choices from a given
 * list of typed options.
 *
 * @param   <C>   option type
 * @param   <T>   parameter result type
 * @author   Mark Taylor
 * @since    17 Dec 2021
 */
public abstract class AbstractChoiceParameter<T,C> extends Parameter<T> {

    private final Map<C,String> optionMap_;
    private final Class<C> optClazz_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     * @param   paramClazz   class of result type for this parameter
     * @param   optClazz   class of typed option
     * @param   options   initial list of typed options available
     */
    @SuppressWarnings("this-escape")
    protected AbstractChoiceParameter( String name, Class<T> paramClazz,
                                       Class<C> optClazz, C[] options ) {
        super( name, paramClazz, true );
        optClazz_ = optClazz;
        optionMap_ = new LinkedHashMap<C,String>();
        if ( options != null ) {
            for ( C opt : options ) {
                addOption( opt );
            }
        }
    }

    /**
     * Adds an option value to this parameter with a given name.
     * The name is the parameter value string used to identify this option.
     *
     * @param   option  option object
     * @param   name   label for option
     */
    public void addOption( C option, String name ) {
        optionMap_.put( option, name );
    }

    /**
     * Adds an option value to this parameter.
     * The option's name will be determined by this object's
     * {@link #stringifyOption} method.
     *
     * @param  option  option object
     */
    public void addOption( C option ) {
        addOption( option, stringifyOption( option ) );
    }

    /**
     * Clears the list of known options.
     */
    public void clearOptions() {
        optionMap_.clear();
    }

    /**
     * Returns an array of the string values of options accepted by this
     * parameter.
     *
     * @return  permitted options, stringified
     */
    public String[] getOptionNames() {
        List<String> optNameList = new ArrayList<String>();
        for ( C option : optionMap_.keySet() ) {
            optNameList.add( getName( option ) );
        }
        return optNameList.toArray( new String[ 0 ] );
    }

    /**
     * Returns an array of the option objects which may form the
     * values of this parameter.
     *
     * @return   permitted options
     */
    public C[] getOptions() {
        Collection<C> list = getOptionValueList();
        @SuppressWarnings("unchecked")
        C[] array = (C[]) Array.newInstance( optClazz_, list.size() );
        return list.toArray( array );
    }

    /**
     * Returns a collection of the option objects which may form the
     * values of this parameter.
     *
     * @return  permitted options
     */
    public Collection<C> getOptionValueList() {
        return optionMap_.keySet();
    }

    /**
     * Converts an option value object to a string which is used to identify
     * it as a string value of this parameter.
     *
     * @param  option   option value
     * @return  string representation
     */
    public String getName( C option ) {
        String name = optionMap_.get( option );
        return name == null ? stringifyOption( option ) : name;
    }

    /**
     * Determines how an option will be represented as a string value of
     * this parameter if no name has explicitly been supplied.
     * The default implementation is <code>String.valueOf(option)</code>,
     * but this may be overrridden.
     *
     * @param  option  option value
     * @return   string representation of option
     */
    public String stringifyOption( C option ) {
        return String.valueOf( option );
    }

    /**
     * Returns the option value associated with a given string by this
     * parameter.  Null is returned if none of the options added so far
     * has a name as supplied.  Name matching is case-insensitive.
     *
     * @param  name   name of option which has been added
     * @return  corresponding option object
     */
    public C getOption( String name ) {
        for ( C option : optionMap_.keySet() ) {
            if ( name.equalsIgnoreCase( getName( option ) ) ) {
                return option;
            }
        }
        return null;
    }

    /**
     * Converts a supplied string to an option value for this parameter,
     * or throws a ParameterValueException if it is not suitable.
     *
     * @param  sval  option name
     * @return   option value, not null
     * @throws  ParameterValueException   if no such option
     */
    protected C stringToChoice( String sval ) throws ParameterValueException {
        for ( C option : optionMap_.keySet() ) {
            if ( sval.equalsIgnoreCase( getName( option ) ) ) {
                return option;
            }
        }
        String msg = new StringBuffer()
           .append( "Unknown value \"" )
           .append( sval )
           .append( "\" - must be one of " )
           .append( optionMap_.keySet().stream()
                              .map( this::getName )
                              .collect( Collectors.joining( ", " ) ) )
           .toString();
        throw new ParameterValueException( this, msg );
    }

    /**
     * Provides a string representation of a given typed value
     * for this parameter.
     *
     * @param  objVal  typed option
     * @return   string representation
     */
    protected String choiceToString( C objVal ) {
        return optionMap_.containsKey( objVal )
             ? optionMap_.get( objVal )
             : stringifyOption( objVal );
    }
}
