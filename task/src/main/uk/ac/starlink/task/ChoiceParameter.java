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
            name = option.toString();
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
     * The object's <code>toString</code> method supplies the name.
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
     * Returns an array of the string values of options accepted by this
     * parameter.
     *
     * @return  permitted options, stringified
     */
    public String[] getOptionNames() {
        return (String[]) nameList_.toArray( new String[ 0 ] );
    }
}
