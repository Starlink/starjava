package uk.ac.starlink.ttools.plot2;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class that represents a call to a function with numeric
 * arguments and its textual representation.
 * The object modelled is of the form <code>funcName(arg1,arg2,...)</code>.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2025
 */
public class ParsedFunctionCall {

    private final String funcName_;
    private final double[] args_;

    private static final Pattern FUNC_REGEX =
        Pattern.compile( "([a-zA-Z0-9]+)(?: *\\(([ ,0-9eE.+-]*)\\))?" );

    /**
     * Constructor.
     *
     * @param  funcName  function name
     * @param  args   list of arguments, may be null
     */
    public ParsedFunctionCall( String funcName, double[] args ) {
        funcName_ = funcName;
        args_ = args;
    }

    /**
     * Returns the function name.
     *
     * @return  name
     */
    public String getFunctionName() {
        return funcName_;
    }

    /**
     * Returns the array of arguments.
     *
     * @return  args array, may be null
     */
    public double[] getArguments() {
        return args_;
    }

    /**
     * Serializes this object in the form <code>name(arg1,arg2,...)</code>.
     * If the argument array is null, there will be no brackets.
     *
     * @return  string representation
     */
    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer( funcName_ );
        if ( args_ != null ) {
            sbuf.append( "(" )
                .append( Arrays.stream( args_ )
                        .mapToObj( n -> n == (int) n
                                            ? Integer.toString( (int) n )
                                            : Double.toString( n ) )
                        .collect( Collectors.joining( "," ) ) )
                .append( ")" );
        }
        return sbuf.toString();
    }

    /**
     * Constructs a ParsedFunctionCall instance from a string.
     * This does this opposite job to {@link #toString}.
     *
     * @param  txt  string representation
     * @return   ParsedFunctionCall object, or null if it doesn't look like one
     */
    public static ParsedFunctionCall fromString( String txt ) {
        Matcher matcher = FUNC_REGEX.matcher( txt );
        if ( matcher.matches() ) {
            String name = matcher.group( 1 );
            String argsTxt = matcher.group( 2 );
            final double[] args;
            if ( argsTxt == null ) {
                args = null;
            }
            else if ( argsTxt.trim().length() == 0 ) {
                args = new double[ 0 ];
            }
            else {
                try {
                    args = Arrays.stream( argsTxt.split( " *, *" ) )
                                 .mapToDouble( s -> Double.parseDouble( s ) )
                                 .toArray();
                }
                catch ( NumberFormatException e ) {
                    return null;
                }
            }
            return new ParsedFunctionCall( name, args );
        }
        return null;
    }
}
