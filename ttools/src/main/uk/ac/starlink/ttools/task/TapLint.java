package uk.ac.starlink.ttools.task;

import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.ttools.taplint.ReportType;
import uk.ac.starlink.ttools.taplint.Reporter;
import uk.ac.starlink.ttools.taplint.Stage;
import uk.ac.starlink.ttools.taplint.TapLinter;

/**
 * TAP Validator task.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 */
public class TapLint implements Task {

    private final TapLinter tapLinter_;
    private final URLParameter urlParam_;
    private final DefaultMultiParameter stagesParam_;
    private final IntegerParameter repeatParam_;
    private final IntegerParameter truncParam_;
    private final BooleanParameter debugParam_;
    private final Parameter reportParam_;
    private final Parameter[] params_;

    /**
     * Constructor.
     */
    public TapLint() {
        List<Parameter> paramList = new ArrayList<Parameter>();

        urlParam_ = new URLParameter( "tapurl" );
        urlParam_.setPosition( 1 );
        urlParam_.setPrompt( "Base URL of TAP service" );
        urlParam_.setDescription( new String[] {
            "<p>The base URL of a Table Access Protocol service.",
            "This is the bare URL without a trailing \"/[a]sync\".",
            "</p>",
        } );
        paramList.add( urlParam_ );

        stagesParam_ = new DefaultMultiParameter( "stages", ' ' );
        stagesParam_.setPrompt( "Codes for validation stages to run" );
        tapLinter_ = new TapLinter();
        Map<String,Stage> stageMap = tapLinter_.getKnownStages();
        StringBuffer slbuf = new StringBuffer();
        StringBuffer subuf = new StringBuffer();
        StringBuffer sdbuf = new StringBuffer();
        for ( String code : stageMap.keySet() ) {
            Stage stage = stageMap.get( code );
            boolean on = tapLinter_.isDefault( code );
            slbuf.append( "<li>" )
                 .append( "<code>" )
                 .append( code )
                 .append( "</code>" )
                 .append( ": " )
                 .append( stage.getDescription() )
                 .append( on ? " (on)" : "" )
                 .append( "</li>" )
                 .append( "\n" );
            if ( subuf.length() > 0 ) {
                subuf.append( '|' );
            }
            subuf.append( code );
            if ( on ) {
                if ( sdbuf.length() > 0 ) {
                    sdbuf.append( ' ' );
                }
                sdbuf.append( code );
            }
        }
        stagesParam_.setUsage( subuf.toString() + "[ ...]" );
        stagesParam_.setDefault( sdbuf.toString() );
        stagesParam_.setDescription( new String[] {
            "<p>Lists the validation stages which the validator will perform.",
            "Each stage is represented by a short code, as follows:",
            "<ul>",
            slbuf.toString(),
            "</ul>",
            "You can specify a list of stage codes, separated by spaces.",
            "Order is not significant.",
            "</p>",
            "<p>Note that removing some stages may affect the operation",
            "of others;",
            "for instance table metadata is acquired from the metadata stages,",
            "and avoiding those will mean that later stages that use",
            "the table metadata to pose queries will not be able to do so",
            "with knowledge of the database schema.",
            "</p>",
        } );
        paramList.add( stagesParam_ );

        reportParam_ = new Parameter( "report" );
        reportParam_.setPrompt( "Message types to report" );
        ReportType[] types = ReportType.values();
        StringBuilder dbuf = new StringBuilder();
        StringBuilder cbuf = new StringBuilder();
        dbuf.append( "Each character of the string is one of the letters " );
        for ( int it = 0; it < types.length; it++ ) {
            char tchr = types[ it ].getChar();
            if ( it > 0 ) {
                dbuf.append( ", " );
            }
            cbuf.append( tchr );
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
        reportParam_.setUsage( "[" + tchrs + "]+" );
        reportParam_.setDescription( new String[] {
            "<p>Letters indicating which message types should be listed.",
            dbuf.toString(),
            "</p>",
        } );
        reportParam_.setDefault( tchrs );
        paramList.add( reportParam_ );

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
        repeatParam_.setDefault( "9" );
        paramList.add( repeatParam_ );

        truncParam_ = new IntegerParameter( "truncate" );
        truncParam_.setPrompt( "Maximum number of characters per line" );
        truncParam_.setDescription( new String[] {
            "<p>Limits the line length written to the output.",
            "</p>",
        } );
        truncParam_.setDefault( "640" );
        paramList.add( truncParam_ );

        debugParam_ = new BooleanParameter( "debug" );
        debugParam_.setPrompt( "Emit debugging output?" );
        debugParam_.setDescription( new String[] {
            "<p>If true, debugging output including stack traces will be",
            "output along with the normal validation messages.",
            "</p>",
        } );
        debugParam_.setDefault( "false" );
        paramList.add( debugParam_ );

        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    public String getPurpose() {
        return "Tests TAP services";
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        URL serviceUrl = urlParam_.urlValue( env );
        PrintStream out = env.getOutputStream();
        String typeStr = reportParam_.stringValue( env );
        List<ReportType> typeList = new ArrayList<ReportType>();
        for ( int ic = 0; ic < typeStr.length(); ic++ ) {
            char c = typeStr.charAt( ic );
            ReportType type = ReportType.forChar( c );
            if ( type == null ) {
                throw new ParameterValueException( reportParam_,
                                                   "Bad message type character"
                                                 + " '" + c + "'" );
            }
            typeList.add( type );
        }
        ReportType[] types = typeList.toArray( new ReportType[ 0 ] );
        int maxRepeat = repeatParam_.intValue( env );;
        boolean debug = debugParam_.booleanValue( env );
        int maxChar = truncParam_.intValue( env );
        Set<String> stageSet = new HashSet<String>();
        Collection<String> knownStages = tapLinter_.getKnownStages().keySet();
        for ( String s : knownStages ) {
            assert s.equals( s.toUpperCase() );
        }
        String[] stages = stagesParam_.stringsValue( env );
        for ( int is = 0; is < stages.length; is++ ) {
            String sc = stages[ is ];
            if ( ! knownStages.contains( sc.toUpperCase() ) ) {
                throw new ParameterValueException( stagesParam_,
                                                   "Unknown stage " + sc );
            }
            stageSet.add( sc );
        }
        Reporter reporter =
            new Reporter( out, types, maxRepeat, debug, maxChar );
        return tapLinter_.createExecutable( reporter, serviceUrl, stageSet );
    }
}
