package uk.ac.starlink.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parameter whose legal value must be one of a disjunction of given values.
 * Matching is case-insensitive against the stringified value of the option.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class ChoiceParameter extends Parameter {

    private final List optionList_;
    private final List nameList_;
    private Object objectValue_;

    /**
     * Constructs a ChoiceParameter with a given list of options.
     *
     * @param   name  parameter name
     * @param   options  legal values of this parameter
     */
    public ChoiceParameter( String name, Object[] options ) {
        super( name );
        optionList_ = new ArrayList();
        nameList_ = new ArrayList();
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
    public void addOption( Object option, String name ) {
        if ( name == null ) {
            name = getName( option );
        }
        if ( nameList_.contains( name ) ) {
            throw new IllegalArgumentException( "Option " + name +
                                                " already exists" );
        }
        optionList_.add( option );
        nameList_.add( name );
    }

    /**
     * Adds an option value to this parameter. 
     * This object's {@link #getName(java.lang.Object)} method is used to
     * supply the option name.
     *
     * @param  option  option object
     */
    public void addOption( Object option ) {
        addOption( option, null );
    }

    public String getUsage() {
        StringBuffer sbuf = new StringBuffer();
        for ( Iterator it = nameList_.iterator(); it.hasNext(); ) {
            sbuf.append( (String) it.next() );
            if ( it.hasNext() ) {
                sbuf.append( '|' );
            }
        }
        return sbuf.toString();
    }

    public void setValueFromString( Environment env, String value )
            throws TaskException {
        if ( value == null ) {
            super.setValueFromString( env, value );
            return;
        }
        int nopt = optionList_.size();
        for ( int i = 0; i < nopt; i++ ) {
            if ( value.equalsIgnoreCase( (String) nameList_.get( i ) ) ) {
                objectValue_ = optionList_.get( i );
                super.setValueFromString( env, value );
                return;
            }
        }
        StringBuffer sbuf = new StringBuffer()
            .append( "Unknown value " )
            .append( value )
            .append( " - must be one of " );
        for ( Iterator it = nameList_.iterator(); it.hasNext(); ) {
            sbuf.append( (String) it.next() );
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
    public Object objectValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return objectValue_;
    }

    /**
     * Sets the default value for this parameter to one of the previously
     * added options.
     *
     * @param  option  default option
     */
    public void setDefaultOption( Object option ) {
        int iopt = optionList_.indexOf( option );
        if ( iopt >= 0 ) {
            setDefault( (String) nameList_.get( iopt ) );
        }
        else {
            throw new IllegalArgumentException( "No such option: " + option );
        }
    }

    /**
     * Returns an array of the string values of options accepted by this
     * parameter.
     *
     * @return  permitted options, stringified
     */
    public String[] getOptionNames() {
        return (String[]) nameList_.toArray( new String[ 0 ] );
    }

    /** 
     * Returns an array of the option objects which may form the
     * values of this parameter.
     *
     * @return   permitted options
     */
    public Object[] getOptions() {
        return optionList_.toArray( new Object[ 0 ] );
    }

    /**
     * Converts an option value object to a string which is used to identify
     * it as a string value of this parameter.
     * The default implementation is <code>String.valueOf(option)</code>,
     * but this may be overrridden.
     *
     * @param  option   option value
     * @return  string representation
     */
    public String getName( Object option ) {
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
        int nopt = optionList_.size();
        for ( int i = 0; i < nopt; i++ ) {
            if ( name.equalsIgnoreCase( (String) nameList_.get( i ) ) ) {
                return optionList_.get( i );
            }
        }
        return null;
    }
}
