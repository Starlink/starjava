package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.taplint.OutputReporter;
import uk.ac.starlink.ttools.taplint.Stage;
import uk.ac.starlink.ttools.taplint.TextOutputReporter;
import uk.ac.starlink.ttools.taplint.TapLinter;
import uk.ac.starlink.vo.EndpointSet;

/**
 * TAP Validator task.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 */
public class TapLint implements Task {

    private final TapLinter tapLinter_;
    private final OutputReporterParameter reporterParam_;
    private final TapEndpointParams endpointParams_;
    private final StringMultiParameter stagesParam_;
    private final IntegerParameter maxtableParam_;
    private final Parameter[] params_;

    /**
     * Constructor.
     */
    public TapLint() {
        List<Parameter> paramList = new ArrayList<Parameter>();

        endpointParams_ = new TapEndpointParams( "tapurl" );
        Parameter urlParam = endpointParams_.getBaseParameter();
        urlParam.setPosition( 1 );
        paramList.add( urlParam );

        stagesParam_ = new StringMultiParameter( "stages", ' ' );
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
        stagesParam_.setStringDefault( sdbuf.toString() );
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

        maxtableParam_ = new IntegerParameter( "maxtable" );
        maxtableParam_.setPrompt( "Maximum number of tables "
                                + "tested individually" );
        maxtableParam_.setDescription( new String[] {
            "<p>Limits the number of tables from the service",
            "that will be tested.",
            "Currently, this only affects",
            "stage <code>" + TapLinter.MDQ_NAME + "</code>.",
            "If the value is left blank (the default),",
            "or if it is larger than the number of tables actually",
            "present in the service, it will have no effect.",
            "</p>",
        } );
        maxtableParam_.setMinimum( 1 );
        maxtableParam_.setNullPermitted( true );
        paramList.add( maxtableParam_ );

        reporterParam_ = new OutputReporterParameter( "format" );

        paramList.add( reporterParam_ );
        paramList.addAll( Arrays.asList( reporterParam_
                                        .getReporterParameters() ) );
        paramList.addAll( Arrays.asList( endpointParams_
                                        .getOtherParameters() ) );

        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    public String getPurpose() {
        return "Tests TAP services";
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        EndpointSet endpointSet = endpointParams_.getEndpointSet( env );

        Integer maxTablesObj = maxtableParam_.objectValue( env );
        int maxTestTables = maxTablesObj == null ? -1 : maxTablesObj.intValue();
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
        OutputReporter reporter = reporterParam_.objectValue( env );
        return tapLinter_.createExecutable( reporter, endpointSet, stageSet,
                                            maxTestTables );
    }
}
