package uk.ac.starlink.ttools.task;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.taplint.JsonOutputReporter;
import uk.ac.starlink.ttools.taplint.OutputReporter;
import uk.ac.starlink.ttools.taplint.ReportType;
import uk.ac.starlink.ttools.taplint.TextOutputReporter;

/**
 * Parameter for selecting an OutputReporter for use with taplint.
 *
 * @author   Mark Taylor
 * @since    23 Oct 2016
 */
public class OutputReporterParameter extends Parameter<OutputReporter> {

    private final StringParameter rtypeParam_;
    private final IntegerParameter repeatParam_;
    private final BooleanParameter debugParam_;
    private final IntegerParameter truncParam_;
    private final Parameter<?>[] reporterParams_;
    private final OutputReporterFactory<?>[] orFactories_ = {
        new TextOutputReporterFactory( "text" ),
        new JsonOutputReporterFactory( "json" ),
    };

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    @SuppressWarnings("this-escape")
    public OutputReporterParameter( String name ) {
        super( name, OutputReporter.class, true );
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

        StringBuffer ubuf = new StringBuffer();
        StringBuffer lbuf = new StringBuffer();
        for ( OutputReporterFactory<?> f : orFactories_ ) {
            if ( ubuf.length() > 0 ) {
                ubuf.append( "|" );
                lbuf.append( ", " );
            }
            ubuf.append( f.name_ );
            lbuf.append( "<code>" )
                .append( f.name_ )
                .append( "</code>" );
        }
        setUsage( ubuf.toString() );
        setPrompt( "Report output format" );
        setDescription( new String[] {
            "<p>Determines the format of the output.",
            "Possible values are",
            lbuf.toString() + ".",
            "</p>",
            "<p>Note not all of the other parameters may be applicable",
            "to all output formats.",
            "</p>",
        } );
        setStringDefault( orFactories_[ 0 ].name_ );

        rtypeParam_ = new StringParameter( "report" );
        rtypeParam_.setPrompt( "Message types to report" );
        ReportType[] types = ReportType.values();
        StringBuilder dbuf = new StringBuilder();
        StringBuilder cbuf = new StringBuilder();
        dbuf.append( "Each character of the string is one of the letters " );
        for ( int it = 0; it < types.length; it++ ) {
            char tchr = types[ it ].getChar();
            cbuf.append( tchr );
            if ( it > 0 ) {
                dbuf.append( ", " );
            }
            dbuf.append( "<code>" )
                .append( tchr )
                .append( "</code>" );
        }
        String tchrs = cbuf.toString();
        dbuf.append( " with the following meanings:\n" )
            .append( "<ul>\n" );
        for ( int it = 0; it < types.length; it++ ) {
            ReportType type = types[ it ];
            dbuf.append( "<li><code>" )
                .append( type.getChar() )
                .append( "</code>: " )
                .append( type.getDescription() )
                .append( "</li>\n" );
        }
        dbuf.append( "</ul>" );
        rtypeParam_.setUsage( "[" + tchrs + "]+" );
        rtypeParam_.setDescription( new String[] {
            "<p>Letters indicating which message types should be listed.",
            dbuf.toString(),
            "</p>",
        } );
        rtypeParam_.setStringDefault( tchrs );
        paramList.add( rtypeParam_ );

        repeatParam_ = new IntegerParameter( "maxrepeat" );
        repeatParam_.setPrompt( "Maximum repeat count per message" );
        repeatParam_.setDescription( new String[] {
            "<p>Puts a limit on the number of times that a single message",
            "will be repeated.",
            "By setting this to some reasonably small number, you can ensure",
            "that the output does not get cluttered up by millions of",
            "repetitions of essentially the same error.",
            "</p>",
        } );
        repeatParam_.setIntDefault( 9 );
        paramList.add( repeatParam_ );

        truncParam_ = new IntegerParameter( "truncate" );
        truncParam_.setPrompt( "Maximum number of characters per line" );
        truncParam_.setDescription( new String[] {
            "<p>Limits the line length written to the output.",
            "</p>",
        } );
        truncParam_.setIntDefault( 640 );
        paramList.add( truncParam_ );

        debugParam_ = new BooleanParameter( "debug" );
        debugParam_.setPrompt( "Emit debugging output?" );
        debugParam_.setDescription( new String[] {
            "<p>If true, debugging output including stack traces will be",
            "output along with the normal validation messages.",
            "</p>",
        } );
        debugParam_.setBooleanDefault( false );
        paramList.add( debugParam_ );

        reporterParams_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    /**
     * Returns an array of parameters associated with this one.
     * Their values are used in conjunction with the value of this parameter
     * to define the selected OutputReporter instance.
     * Note that not all of these paramters may be applicable to
     * every value for this parameter.  But there is probably a fair
     * amount of overlap.
     *
     * @return  list of associated parameters
     */
    public Parameter<?>[] getReporterParameters() {
        return reporterParams_;
    }

    @Override
    public String objectToString( Environment env, OutputReporter orval ) {
        for ( OutputReporterFactory<?> f : orFactories_ ) {
            if ( f.clazz_.isAssignableFrom( orval.getClass() ) ) {
                return f.name_;
            }
        }
        return orval.getClass().getName();
    }

    @Override
    public OutputReporter stringToObject( Environment env, String sval )
            throws TaskException {
        for ( OutputReporterFactory<?> f : orFactories_ ) {
            if ( f.name_.equalsIgnoreCase( sval ) ) {
                return f.createReporter( env );
            }
        }
        throw new ParameterValueException( this, "Unknown reporter type" );
    }

    /**
     * Factory for OutputReporter instances.
     * Abstraction used internally in the OutputReporterParameter class.
     */
    private abstract class OutputReporterFactory<T extends OutputReporter> {
        private final String name_;
        private final Class<T> clazz_;

        /**
         * Constructor.
         *
         * @param  name  format name, used for selection by user
         * @param  clazz   class type of OutputReporter created
         */
        OutputReporterFactory( String name, Class<T> clazz ) {
            name_ = name;
            clazz_ = clazz;
        }

        /**
         * Returns a new reporter with characteristics defined by the
         * given execution environment.
         *
         * @param   env   execution environment
         * @return   new OutputReporter
         */
        public abstract T createReporter( Environment env )
                throws TaskException;

        /**
         * Utility method to acquire a list of the required report types
         * from the environment.
         * Uses the rtypeParam_ parameter.
         *
         * @param   env   execution environment
         * @return   report type list
         */
        ReportType[] getReportTypes( Environment env ) throws TaskException {
            String typeStr = rtypeParam_.stringValue( env );
            List<ReportType> typeList = new ArrayList<ReportType>();
            for ( int ic = 0; ic < typeStr.length(); ic++ ) {
                char c = typeStr.charAt( ic );
                ReportType type = ReportType.forChar( c );
                if ( type == null ) {
                    String msg = "Bad message type character '" + c + "'";
                    throw new ParameterValueException( rtypeParam_, msg );
                }
                typeList.add( type );
            }
            return typeList.toArray( new ReportType[ 0 ] );
        }
    }

    /**
     * OutputReporterFactory instance for plain text output.
     */
    private class TextOutputReporterFactory
            extends OutputReporterFactory<TextOutputReporter> {

        /**
         * Constructor.
         *
         * @param  name  type name for user
         */
        public TextOutputReporterFactory( String name ) {
            super( name, TextOutputReporter.class );
        }

        public TextOutputReporter createReporter( Environment env )
                throws TaskException {
            PrintStream out = env.getOutputStream();
            ReportType[] types = getReportTypes( env );
            int maxRepeat = repeatParam_.intValue( env );
            boolean debug = debugParam_.booleanValue( env );
            int maxChar = truncParam_.intValue( env );
            return new TextOutputReporter( out, types, maxRepeat, debug,
                                           maxChar );
        }
    }

    /**
     * OutputReporterFactory instance for JSON output.
     */
    private class JsonOutputReporterFactory
            extends OutputReporterFactory<JsonOutputReporter> {

        /**
         * Constructor.
         *
         * @param  name  type name for user
         */
        public JsonOutputReporterFactory( String name ) {
            super( name, JsonOutputReporter.class );
        }

        public JsonOutputReporter createReporter( Environment env )
                throws TaskException {
            PrintStream out = env.getOutputStream();
            ReportType[] types = getReportTypes( env );
            int maxRepeat = repeatParam_.intValue( env );
            boolean debug = debugParam_.booleanValue( env );
            int maxChar = truncParam_.intValue( env );
            return new JsonOutputReporter( out, types, maxRepeat, debug,
                                           maxChar );
        }
    }
}
