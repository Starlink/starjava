package uk.ac.starlink.task;

import java.util.ArrayList;
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
public class ChoiceParameter<T> extends Parameter {

    private final Map<T,String> optionMap_;
    private T objectValue_;
    private boolean usageSet_;

    /**
     * Constructs a ChoiceParameter with a given list of options.
     *
     * @param   name  parameter name
     * @param   options  legal values of this parameter
     */
    public ChoiceParameter( String name, T[] options ) {
        super( name );
        optionMap_ = new LinkedHashMap<T,String>();
        if ( options != null ) {
            for ( int iopt = 0; iopt < options.length; iopt++ ) {
                addOption( options[ iopt ] );
            }
        }
    }

    /**
     * Constructs a ChoiceParameter with no options.
     *
     * @param   name  parameter name
     */
    public ChoiceParameter( String name ) {
        this( name, null );
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
        addOption( option, null );
    }

    /**
     * Returns a usage message.  Unless it has been overriden by an earlier
     * call to {@link #setUsage}, this will return a usage message based on
     * the list of known options.
     *
     * @return   usage message
     */
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

    public void setUsage( String usage ) {
        usageSet_ = true;
        super.setUsage( usage );
    }

    public void setValueFromString( Environment env, String value )
            throws TaskException {
        if ( value == null ) {
            super.setValueFromString( env, value );
            return;
        }
        for ( T option : optionMap_.keySet() ) {
            if ( value.equalsIgnoreCase( getName( option ) ) ) {
                objectValue_ = option;
                super.setValueFromString( env, value );
                return;
            }
        }
        StringBuffer sbuf = new StringBuffer()
            .append( "Unknown value " )
            .append( value )
            .append( " - must be one of " );
        for ( Iterator<T> it = optionMap_.keySet().iterator(); it.hasNext(); ) {
            sbuf.append( getName( it.next() ) );
            if ( it.hasNext() ) {
                sbuf.append( ", " );
            }
        }
        throw new ParameterValueException( this, sbuf.toString() );
    }

    /**
     * Returns the value as an object.  It will be identical to one of
     * the options of this parameter.
     *
     * @param  env  execution environment
     * @return  selected object
     */
    public T objectValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return objectValue_;
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
                setDefault( null );
            }
            else {
                throw new IllegalArgumentException( "null value not allowed" );
            }
        }
        else {
            if ( optionMap_.containsKey( option ) ) {
                setDefault( getName( option ) );
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
    public Object[] getOptions() {
        return optionMap_.keySet().toArray( new Object[ 0 ] );
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
    public Object getOption( String name ) {
        for ( T option : optionMap_.keySet() ) {
            if ( name.equalsIgnoreCase( getName( option ) ) ) {
                return option;
            }
        }
        return null;
    }
}
