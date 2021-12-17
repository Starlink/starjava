package uk.ac.starlink.task;

import java.util.stream.Collectors;

/**
 * Parameter whose legal value must be one of a disjunction of given values.
 * Matching is case-insensitive against the stringified value of the option.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class ChoiceParameter<T> extends AbstractChoiceParameter<T,T> {

    private boolean usageSet_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     * @param   clazz  type for values of this parameter
     * @param   options  initial array of legal values of this parameter
     */
    public ChoiceParameter( String name, Class<T> clazz, T[] options ) {
        super( name, clazz, clazz, options );
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
        return stringToChoice( sval );
    }

    @Override
    public String objectToString( Environment env, T objVal ) {
        return choiceToString( objVal );
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
        return usageSet_
             ? super.getUsage()
             : getOptionValueList().stream()
                                   .map( this::getName )
                                   .collect( Collectors.joining( "|" ) );
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
            if ( getOptionValueList().contains( option ) ) {
                setStringDefault( getName( option ) );
            }
            else {
                throw new IllegalArgumentException( "No such option: "
                                                  + option );
            }
        }
    }
}
