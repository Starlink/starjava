package uk.ac.starlink.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parameter whose legal value must be one of a disjunction of given values.
 * Matching is case-insensitive against the stringified value of the option.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class ChoiceParameter<T> extends Parameter<T> {

    private final Map<T,String> optionMap_;
    private boolean usageSet_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     * @param   clazz  type for values of this parameter
     * @param   options  initial array of legal values of this parameter
     */
    public ChoiceParameter( String name, Class<T> clazz, T[] options ) {
        super( name, clazz, true );
        optionMap_ = new LinkedHashMap<T,String>();
        if ( options != null ) {
            for ( int iopt = 0; iopt < options.length; iopt++ ) {
                addOption( options[ iopt ] );
            }
        }
    }

    /**
     * Constructs a choice parameter with no initially set options.
     *
     * @param   name  parameter name
     * @param   clazz  type for values of this parameter
     */
    public ChoiceParameter( String name, Class<T> clazz ) {
        this( name, clazz, null );
    }

    /**
     * Constructs a choice parameter with an initial option set.
     * The data type is taken from the supplied array type.
     *
     * @param   name  parameter name
     * @param   options  initial array of legal values of this parameter
     */
    @SuppressWarnings("unchecked")
    public ChoiceParameter( String name, T[] options ) {
        this( name, (Class<T>) options.getClass().getComponentType(), options );
    }

    public T stringToObject( Environment env, String sval )
            throws TaskException {
        for ( T option : optionMap_.keySet() ) {
            if ( sval.equalsIgnoreCase( getName( option ) ) ) {
                return option;
            }
        }
        StringBuffer sbuf = new StringBuffer()
            .append( "Unknown value " )
            .append( sval )
            .append( " - must be one of " );
        for ( Iterator<T> it = optionMap_.keySet().iterator(); it.hasNext(); ) {
            sbuf.append( getName( it.next() ) );
            if ( it.hasNext() ) {
                sbuf.append( ", " );
            }
        }
        throw new ParameterValueException( this, sbuf.toString() );
    }

    @Override
    public String objectToString( Environment env, T objVal ) {
        return optionMap_.containsKey( objVal )
             ? optionMap_.get( objVal )
             : stringifyOption( objVal );
    }

    /**
     * Adds an option value to this parameter with a given name.
     * The name is the parameter value string used to identify this option.
     *
     * @param   option  option object
     * @param   name   label for option
     */
    public void addOption( T option, String name ) {
        optionMap_.put( option, name );
    }

    /**
     * Adds an option value to this parameter. 
     * The option's name will be determined by this object's
     * {@link #stringifyOption} method.
     *
     * @param  option  option object
     */
    public void addOption( T option ) {
        addOption( option, stringifyOption( option ) );
    }

    /**
     * Clears the list of known options.
     */
    public void clearOptions() {
        optionMap_.clear();
    }

    /**
     * Returns a usage message.  Unless it has been overriden by an earlier
     * call to {@link #setUsage}, this will return a usage message based on
     * the list of known options.
     *
     * @return   usage message
     */
    @Override
    public String getUsage() {
        if ( usageSet_ ) {
            return super.getUsage();
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            for ( Iterator<T> it = optionMap_.keySet().iterator();
                  it.hasNext(); ) {
                sbuf.append( getName( it.next() ) );
                if ( it.hasNext() ) {
                    sbuf.append( '|' );
                }
            }
            return sbuf.toString();
        }
    }

    @Override
    public void setUsage( String usage ) {
        usageSet_ = true;
        super.setUsage( usage );
    }

    /**
     * Sets the default value for this parameter to one of the previously
     * added options.
     *
     * @param  option  default option
     */
    public void setDefaultOption( T option ) {
        if ( option == null ) {
            if ( isNullPermitted() ) {
                setStringDefault( null );
            }
            else {
                throw new IllegalArgumentException( "null value not allowed" );
            }
        }
        else {
            if ( optionMap_.containsKey( option ) ) {
                setStringDefault( getName( option ) );
            }
            else {
                throw new IllegalArgumentException( "No such option: "
                                                  + option );
            }
        }
    }

    /**
     * Returns an array of the string values of options accepted by this
     * parameter.
     *
     * @return  permitted options, stringified
     */
    public String[] getOptionNames() {
        List<String> optNameList = new ArrayList<String>();
        for ( T option : optionMap_.keySet() ) {
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
    public T[] getOptions() {
        return toArray( getOptionValueList() );
    }

    /**
     * Returns a collection of the option objects which may form the
     * values of this parameter.
     *
     * @return  permitted options
     */
    public Collection<T> getOptionValueList() {
        return optionMap_.keySet();
    }

    /**
     * Converts an option value object to a string which is used to identify
     * it as a string value of this parameter.
     *
     * @param  option   option value
     * @return  string representation
     */
    public String getName( T option ) {
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
    public String stringifyOption( T option ) {
        return String.valueOf( option );
    }

    /**
     * Returns the option value associated with a given string by this
     * parameter.  Null is returned if none of the options added so far
     * has a name as supplied.  Name matching is case-insensitive.
     *
     * @param  name   name of option which has been added
     * @return  correspondig option object
     */
    public T getOption( String name ) {
        for ( T option : optionMap_.keySet() ) {
            if ( name.equalsIgnoreCase( getName( option ) ) ) {
                return option;
            }
        }
        return null;
    }
}
