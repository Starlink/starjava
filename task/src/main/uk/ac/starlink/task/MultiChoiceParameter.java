package uk.ac.starlink.task;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * MultiParameter that returns an array of values from a given
 * typed list of options.
 * Matching is case-insensitive against the stringified value of the option.
 *
 * @param  <C>  type for single element of parameter output array
 *
 * @author   Mark Taylor
 * @since    17 Dec 2021
 */
public class MultiChoiceParameter<C> extends AbstractChoiceParameter<C[],C>
                                     implements MultiParameter {

    private final Class<C> optClazz_;
    private final char valueSep_;
    private final String allToken_;
    private final C[] allOpts_;
    private boolean usageSet_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     * @param   optClazz  type for element value of this parameter
     * @param   valueSep   separator character for multiple values in a string
     * @param   options  initial array of legal element values
     *                   for this parameter
     * @param   allToken  if non-blank, supplying this value will select
     *                     all the available options
     */
    @SuppressWarnings("unchecked")
    public MultiChoiceParameter( String name, Class<C> optClazz, char valueSep,
                                 C[] options, String allToken ) {
        super( name, (Class<C[]>) Array.newInstance( optClazz, 0 ).getClass(),
               optClazz, options );
        optClazz_ = optClazz;
        valueSep_ = valueSep;
        allToken_ = allToken;
        allOpts_ = allToken != null ? options.clone() : null;
    }

    public char getValueSeparator() {
        return valueSep_;
    }

    public C[] stringToObject( Environment env, String sval )
            throws TaskException {
        if ( allToken_ != null && allToken_.equalsIgnoreCase( sval ) ) {
            return allOpts_.clone();
        }
        String[] words = sval.split( Character.toString( valueSep_ ) );
        int nobj = words.length;
        @SuppressWarnings("unchecked")
        C[] objs = (C[]) Array.newInstance( optClazz_, nobj );
        for ( int iobj = 0; iobj < nobj; iobj++ ) {
            objs[ iobj ] = stringToChoice( words[ iobj ] );
        }
        return objs;
    }

    @Override
    public String objectToString( Environment env, C[] objVal ) {
        return choicesToString( objVal );
    }

    @Override
    public void setUsage( String usage ) {
        usageSet_ = true;
        super.setUsage( usage );
    }

    @Override
    public String getUsage() {
        if ( usageSet_ ) {
            return super.getUsage();
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "[" )
                .append( getOptionValueList()
                        .stream()
                        .map( this::getName )
                        .collect( Collectors.joining( "|" ) ) )
                .append( ", ..." )
                .append( "]" );
            if ( allToken_ != null ) {
                sbuf.append( " | " )
                    .append( allToken_ );
            }
            return sbuf.toString();
        }
    }

    /**
     * Sets the default value for this parameter to an array containing
     * previously added options.
     *
     * @param  options  default parameter value
     */
    public void setDefaultOptions( C[] options ) {
        if ( options == null ) {
            if ( isNullPermitted() ) {
                setStringDefault( null );
            }
            else {
                throw new IllegalArgumentException( "null value not allowed" );
            }
        }
        else {
            setStringDefault( choicesToString( options ) );
        }
    }

    /**
     * Maps an array of known options to a textual representation as used
     * by this parameter.
     *
     * @param  options   option array
     * @return   string representatino
     */
    private String choicesToString( C[] options ) {
        return Arrays.asList( options )
              .stream()
              .map( this::choiceToString )
              .collect( Collectors.joining( Character.toString( valueSep_ ) ) );
    }
}
