package uk.ac.starlink.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Environment which accepts an initial command line,
 * and communicates with the user using standard input and standard output.
 *
 * <p>This environment is somewhat deprecated in favour of the more capable
 * {@link LineEnvironment}.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class TerminalEnvironment implements Environment {

    private Map<Parameter<?>,String> valueMap;
    private Set<Parameter<?>> paramSet;

    /** The number of goes you get to put in an invalid parameter. */
    public static int NUM_TRIES = 5;
 
    /**
     * Constructs a new Environment based on a String array containing
     * supplied arguments, and a list of parameters which may be encountered.
     * The initial arguments may have the form <code>value</code> or 
     * <code>name=value</code>; in the former case they must correspond
     * to a parameter with a non-zero position attribute indicating
     * where it is expected on the command line, and in the latter case
     * the <code>name</code> must correspond to the name of one of the 
     * parameters in <code>params</code>.
     *
     * @param  args  an array of words found on the command line
     * @param  params  an array of Parameter objects which this Environment
     *                 may be asked to get values for
     */
    public TerminalEnvironment( String[] args, Parameter<?>[] params )
            throws UsageException {
        paramSet = new HashSet<Parameter<?>>( params.length );
        for ( int i = 0; i < params.length; i++ ) {
            paramSet.add( params[ i ] );
        }
        valueMap = new LinkedHashMap<Parameter<?>,String>();
        for ( int i = 0; i < args.length; i++ ) {
            boolean found = false;
            String[] pp = args[ i ].split( "=" );
            if ( pp.length == 2 ) {
                String name = pp[ 0 ];
                String value = pp[ 1 ];
                for ( int j = 0; j < params.length; j++ ) {
                    if ( params[ j ].getName().equals( name ) ) {
                        valueMap.put( params[ j ], value );
                        found = true;
                    }
                }
                if ( ! found ) {
                    throw new UsageException( "Unknown parameter " + name );
                }
            }
            else {
                for ( int j = 0; j < params.length; j++ ) {
                    if ( params[ j ].getPosition() == i + 1 ) {
                        valueMap.put( params[ j ], args[ i ] );
                        found = true;
                    }
                }
            }
            if ( ! found ) {
                throw new UsageException( "Unknown word: " + args[ i ] );
            }
        }
    }

    public void clear( Parameter<?> par ) {
        valueMap.remove( par );
    }


    /**
     * Sets the value of a parameter.  If it a value has been specified 
     * on the command line then that is used, otherwise any known default 
     * is used, otherwise the user is prompted on standard output and 
     * response got from standard input.
     * <p>
     * A more configurable order (cf ADAM PPATH/VPATH) could be implemented
     * by adding some methods to Parameter and getting this method to
     * query them.
     *
     * @param   par  the parameter whose value is to be set
     */
    public void acquireValue( Parameter<?> par ) throws TaskException {
        acquireValue( par, NUM_TRIES );
    }

    private void acquireValue( Parameter<?> par, int ntries )
            throws TaskException {

        /* If we've run out of attempts, bail out. */
        if ( ntries <= 0 ) {
            throw new ParameterValueException( par,
                "No valid value in " + NUM_TRIES + " attempts" );
        }

        if ( ! paramSet.contains( par ) ) {
            throw new IllegalArgumentException( 
                "Unknown parameter " + par.getName() + " (programming error)" );
        }
          
        if ( ! valueMap.containsKey( par ) ) {
            if ( par.getStringDefault() != null ) {
                valueMap.put( par, par.getStringDefault() );
            }
            else if ( par.getPreferExplicit() || ! par.isNullPermitted() ) {
                String prompt = par.getPrompt();
                System.out.print( par.getName() 
                                + ( ( prompt == null ) ? ""
                                                       : (": " + prompt ) ) 
                                + " > " );
                BufferedReader rdr = 
                    new BufferedReader( new InputStreamReader( System.in ) );
                String value; 
                try {
                    value = rdr.readLine();
                }
                catch ( IOException e ) {
                    throw new AbortException( "Error getting parameter " 
                                            + par, e );
                }
                if ( value == null || value.equals( "!!" ) ) {
                    throw new AbortException();
                }
                if ( value.equals( "!" ) ) {
                    value = "";
                }
                if ( value.length() == 0 ) {
                    String def = par.getStringDefault();
                    if ( par.getStringDefault() != null ) {
                        value = def;
                    }
                    else {  // recurse
                        acquireValue( par, ntries - 1 );
                        return;
                    }
                }
                valueMap.put( par, value );
            }
        }
        try {
            par.setValueFromString( this, valueMap.get( par ) );
        }
        catch ( ParameterValueException e ) {
            System.out.println( e.getMessage() );
            valueMap.remove( par );
            if ( ntries > 1 ) {
                acquireValue( par, ntries - 1 );  // recurse
            }
            else {
                throw e;
            }
        }
    }

    public void clearValue( Parameter<?> par ) {
        valueMap.remove( par );
    }

    public String[] getNames() {
        List<String> nameList = new ArrayList<String>();
        for ( Parameter<?> param : paramSet ) {
            nameList.add( param.getName() );
        }
        return nameList.toArray( new String[ 0 ] );
    }

    /**
     * Returns <code>System.out</code>.
     *
     * @return   System.out
     */
    public PrintStream getOutputStream() {
        return System.out;
    }

    /**
     * Returns <code>System.err</code>.
     *
     * @return   System.err
     */
    public PrintStream getErrorStream() {
        return System.err;
    }
}
