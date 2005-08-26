package uk.ac.starlink.ttools.task;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Table execution environment which gets parameter variables from
 * command-line arguments.
 * Currently the arguments are of the form "param-name=param-value".
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class LineEnvironment extends TableEnvironment {

    private Argument[] arguments_;
    private int narg_;
    private int numberedArgs_;

    /**
     * Initializes the input data for this environment from a list of
     * words presumably got from a command line of some description.
     *
     * @param  args  array of words from the command line
     */
    public void setArgs( String[] args ) {
        if ( arguments_ != null ) {
            throw new IllegalStateException( "Arguments already set" );
        }
        arguments_ = new Argument[ args.length ];
        for ( int i = 0; i < args.length; i++ ) {
            arguments_[ i ] = new Argument( args[ i ] );
        }
    }

    /**
     * From the command line arguments finds the string value of a selected
     * parameter.
     *
     * @param   param  parameter to locate
     * @return  string value for <tt>param</tt>
     */
    private String findValue( Parameter param ) {

        /* If it's a multiparameter, concatenate all the appearances
         * on the command line. */
        if ( param instanceof MultiParameter ) {
            StringBuffer val = new StringBuffer();
            int igot = 0;
            for ( int i = 0; i < arguments_.length; i++ ) {
                Argument arg = arguments_[ i ];
                if ( param.getName().equals( arg.name_ ) ||
                     ( arg.pos_ > 0 && param.getPosition() == arg.pos_ ) ) {
                    arg.used_ = true;
                    if ( igot++ > 0 ) {
                        val.append( ';' );
                    }
                    val.append( arg.value_ );
                }
            }
            if ( igot > 0 ) {
                return val.toString();
            }
        }

        /* Otherwise, just take the first one. */
        else {
            for ( int i = 0; i < arguments_.length; i++ ) {
                Argument arg = arguments_[ i ];
                if ( param.getName().equals( arg.name_ ) ||
                     ( arg.pos_ > 0 && param.getPosition() == arg.pos_ ) ) {
                    arg.used_ = true;
                    return arg.value_;
                }
            }
        }

        /* If no return so far, use the default value. */
        return param.getDefault();
    }

    /**
     * Clears the value of a given parameter.  Since this environment is
     * not currently interactive, this is not supported.
     *
     * @throws   UnsupportedOperationException
     */
    public void clearValue( Parameter par ) {
        throw new UnsupportedOperationException();
    }

    public PrintStream getPrintStream() {
        return System.out;
    }

    public void acquireValue( Parameter param ) throws TaskException {
        String stringVal = findValue( param );
        if ( stringVal == null && ! param.isNullPermitted() ) {
            throw new ParameterValueException( param,
                                               "null value not allowed" );
        }
        else {
            param.setValueFromString( this, stringVal );
        }
    }

    /**
     * Returns a string containing any words of the input argument list 
     * which were never queried by the application to find their 
     * value.  Such unused words probably merit a warning, since they 
     * may for instance be misspelled versions of real parameters.
     *
     * @return   array of unused words
     */
    public String[] getUnused() {
        List unusedList = new ArrayList();
        for ( int i = 0; i < arguments_.length; i++ ) {
            Argument arg = arguments_[ i ];
            if ( ! arg.used_ ) {
                unusedList.add( arg.orig_ );
            }
        }
        return (String[]) unusedList.toArray( new String[ 0 ] );
    }

    /**
     * Turns a string value from the input word into a string value suitable
     * for presentation to the parameter system.  Null value handling may
     * be done.
     *
     * @param   sval  raw string value
     * @return  processed string value
     */
    private static String readValue( String sval ) {
        if ( sval.length() == 0 || sval.equals( "null" ) ) {
            return null;
        }
        else {
            return sval;
        }
    }

    /**
     * Helper class to encapsulate a word found on the command line.
     */
    private class Argument {

        final String orig_;
        final String name_;
        final int pos_;
        final String value_;
        boolean used_;

        /**
         * Constructor.
         *
         * @param  arg  name=value word
         */
        Argument( String arg ) {
            orig_ = arg;
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                int epos = arg.indexOf( '=' );
                if ( epos < 0 ) {
                    throw new IllegalArgumentException( "Bad flag " + arg );
                }
                name_ = arg.substring( 1, epos );
                pos_ = -1;
                value_ = readValue( arg.substring( epos + 1 ) );
            }
            else {
                pos_ = ++numberedArgs_;
                name_ = null;
                value_ = readValue( arg );
            }
        }
    }

}
