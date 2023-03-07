package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.taplint.FixedCode;
import uk.ac.starlink.ttools.taplint.OutputReporter;
import uk.ac.starlink.ttools.taplint.Stage;
import uk.ac.starlink.ttools.taplint.TextOutputReporter;
import uk.ac.starlink.ttools.taplint.TapLinter;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapService;

/**
 * TAP Validator task.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 */
public class TapLint implements Task {

    private final TapLinter tapLinter_;
    private final OutputReporterParameter reporterParam_;
    private final TapServiceParams tapserviceParams_;
    private final StringMultiParameter stagesParam_;
    private final IntegerParameter maxtableParam_;
    private final StringParameter tablesParam_;
    private final Parameter<?>[] params_;

    /**
     * Constructor.
     */
    public TapLint() {
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

        tapserviceParams_ = new TapServiceParams( "tapurl", true );
        Parameter<?> urlParam = tapserviceParams_.getBaseParameter();
        urlParam.setPosition( 1 );
        paramList.add( urlParam );

        stagesParam_ = new StringMultiParameter( "stages", ' ' );
        stagesParam_.setPrompt( "Codes for validation stages to run" );
        tapLinter_ = new TapLinter();
        Map<String,Stage> stageMap = tapLinter_.getKnownStages();
        StringBuffer sbuf = new StringBuffer();
        for ( String code : stageMap.keySet() ) {
            Stage stage = stageMap.get( code );
            boolean on = tapLinter_.isDefault( code );
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( code )
                .append( "</code>" )
                .append( ": " )
                .append( stage.getDescription() )
                .append( on ? "" : " (off)" )
                .append( "</li>" )
                .append( "\n" );
        }
        stagesParam_.setUsage( "[+/-]XXX ..." );
        stagesParam_.setNullPermitted( true );
        stagesParam_.setDescription( new String[] {
            "<p>Determines the validation stages which the validator",
            "will peform.",
            "Each stage is represented by a short code, as follows:",
            "<ul>",
            sbuf.toString(),
            "</ul>",
            "</p>",
            "<p>This parameter can specify what stages to run",
            "in the following ways:",
            "<ul>",
            "<li>if left blank, the default list of stages",
            "    (which is most or all of them) will be run</li>",
            "<li>if the value is a space-separated list of three-letter codes,",
            "    it lists the stages that will be run</li>",
            "<li>if the value is a space separated list of three-letter codes",
            "    preceded by a \"+\" or \"-\" character, the named stages",
            "    will be removed or added to the default list</li>",
            "</ul>",
            "So \"<code>TME CAP</code>\" will run only Table Metadata and",
            "Capability stages,",
            "while \"<code>-EXA -UPL</code>\" will run all the default stages",
            "apart from Examples and Upload.",
            "The order in which stages are listed is not significant.",
            "</p>",
            "<p>Note that removing some stages may affect the operation",
            "of others;",
            "for instance table metadata is acquired from the metadata stages,",
            "and avoiding those will mean that later stages which use",
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

        tablesParam_ = new StringParameter( "tables" );
        tablesParam_.setPrompt( "Selected table names" );
        tablesParam_.setUsage( "<name-list>" );
        tablesParam_.setDescription( new String[] {
            "<p>If supplied, this specifies a list of tables to test.",
            "It may be set to a space- or comma-separated list",
            "of table names for consideration;",
            "any tables not covered by this list are mostly ignored",
            "for the purposes of reporting.",
            "Matching against table names is case-insensitive,",
            "and the asterisk character \"<code>*</code>\"",
            "may be used as a wildcard to match any sequence of characters.",
            "Note that matching is against the declared table name",
            "which may or may not include a schema name prefix",
            "depending on service behaviour.",
            "</p>",
            "<p>By default this parameter is not set,",
            "which means that all tables are considered.",
            "</p>",
        } );
        tablesParam_.setNullPermitted( true );
        paramList.add( tablesParam_ );

        reporterParam_ = new OutputReporterParameter( "format" );

        paramList.add( reporterParam_ );
        paramList.addAll( Arrays.asList( reporterParam_
                                        .getReporterParameters() ) );
        paramList.addAll( tapserviceParams_.getInterfaceParameters() );
        paramList.addAll( tapserviceParams_.getOtherParameters() );

        params_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    public String getPurpose() {
        return "Tests TAP services";
    }

    public Parameter<?>[] getParameters() {
        return params_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        OutputReporter reporter = reporterParam_.objectValue( env );
        Integer maxTablesObj = maxtableParam_.objectValue( env );
        int maxTestTables = maxTablesObj == null ? -1 : maxTablesObj.intValue();
        Set<String> stageSet = getStageSet( stagesParam_.stringsValue( env ) );
        Predicate<TableMeta> tableFilter =
            createTableNameFilter( tablesParam_.objectValue( env ) );

        /* Acquire the service, which may (depending on chosen interface type)
         * involve reading the capabilities document. */
        TapService tapService;
        try {
            tapService = tapserviceParams_.getTapService( env );
        }

        /* The service acquisition may fail if the service just doesn't exist.
         * Treat that eventuality as a special case and report it through
         * the reporter in a special ad hoc stage PRE.
         * This is a bit hacky but it's hard to do it better since the rest
         * of the linting hasn't been set up yet, and it's better than
         * the alternative which is to fail with an exception, which a
         * validator ought not to do as a result of service deficiencies. */
        catch ( ExecutionException e ) {
            return new Executable() {
                public void execute() {
                    reporter.start( TapLinter.getAnnouncements( false ) );
                    reporter.startSection( "PRE", "Preparation" );
                    reporter.report( FixedCode.E_TAP0,
                                     "TAP service not present", e );
                    reporter.endSection();
                    reporter.end();
                }
            };
        }

        /* If we have a service, set up validation in the usual way. */
        return tapLinter_.createExecutable( reporter, tapService, stageSet,
                                            maxTestTables, tableFilter );
    }

    /**
     * Returns a list of stage identifiers selected by the value
     * of the stagesParameter.
     *
     * @param  stageStrings  a list of stage tokens as supplied by the
     *                       values of the stagesParameter
     * @return   a set of stage three-character codes to be executed
     */
    private Set<String> getStageSet( String[] stageStrings )
            throws ParameterValueException {
        Collection<String> knownStages = tapLinter_.getKnownStages().keySet();
        Collection<String> dfltStages =
            tapLinter_.getKnownStages().keySet().stream()
                      .filter( tapLinter_::isDefault )
                      .collect( Collectors.toSet() );

        /* Ensure that the stages are named as expected. */
        for ( String s : knownStages ) {
            assert s.length() == 3 && s.equals( s.toUpperCase() );
        }

        /* If input is blank, just use the default list. */
        if ( stageStrings == null || stageStrings.length == 0 ) {
            return new HashSet<String>( dfltStages );
        }
        else {

            /* Parse all the tokens as stage add, remove or select items. */
            Pattern stageRegex = Pattern.compile( "([+-]?)([A-Za-z]{3})" );
            Set<String> adds = new HashSet<>();
            Set<String> removes = new HashSet<>();
            Set<String> selects = new HashSet<>();
            for ( String stageString : stageStrings ) {
                Matcher matcher = stageRegex.matcher( stageString );
                if ( matcher.matches() ) {
                    String flagChar = matcher.group( 1 );
                    String sname = matcher.group( 2 ).toUpperCase();
                    if ( knownStages.contains( sname ) ) {
                        if ( "+".equals( flagChar ) ) {
                            adds.add( sname );
                        }
                        else if ( "-".equals( flagChar ) ) {
                            removes.add( sname );
                        }
                        else {
                            assert flagChar == null || flagChar.length() == 0;
                            selects.add( sname );
                        }
                    }
                    else {
                        String msg = new StringBuffer()
                           .append( "Unknown stage \"" )
                           .append( sname )
                           .append( "\": stages are " )
                           .append( String.join( ", ", knownStages ) )
                           .toString();
                        throw new ParameterValueException( stagesParam_, msg );
                    }
                }
                else {
                    throw new ParameterValueException(
                        stagesParam_,
                        "Bad stage specification \"" + stageString + "\"" );
                }
            }

            /* If all are add/remove, modify and return the defaults list. */
            if ( selects.size() == 0 ) {
                Set<String> stageSet = new HashSet<String>( dfltStages );
                stageSet.addAll( adds );
                stageSet.removeAll( removes );
                return stageSet;
            }

            /* If all are selects, return the list specified. */
            else if ( adds.size() == 0 && removes.size() == 0 ) {
                return new HashSet<String>( selects );
            }

            /* Otherwise signal error. */
            else {
                throw new ParameterValueException(
                    stagesParam_, "Can't mix +XXX/-XXX and XXX items" );
            }
        }
    }

    /**
     * Converts a text specification for table names of interest to
     * a TableMeta filter.
     * The input format may have multiple space- or comma-separated entries,
     * each one case insensitive, and the "*" wildcard is recognised.
     *
     * @param  txt  table name pattern
     * @return   TableMeta filter, or null for no filtering
     */
    private static Predicate<TableMeta> createTableNameFilter( String txt ) {
        if ( txt == null || txt.trim().length() == 0 ) {
            return null;
        }
        final List<Pattern> patterns =
            Arrays.stream( txt.split( "[\\s,]+", 0 ) )
                  .map( TapLint::globToRegex )
                  .collect( Collectors.toList() );
        return tmeta -> {
            String tname = tmeta == null ? null : tmeta.getName();
            return tname != null
                && patterns
                  .stream()
                  .anyMatch( p -> p.matcher( tname ).matches() );
        };
    }

    /**
     * Converts a simple glob-style pattern to a regular expression.
     *
     * @param   glob  case-insensitive string that may contain "*" wildcards
     * @return  regular expression pattern
     */
    private static Pattern globToRegex( String glob ) {
        final String regex;
        if ( glob == null || glob.trim().length() == 0 ) {
            regex = "";
        }
        else {
            regex = "\\Q" + glob.replaceAll( "\\*", "\\\\E.*\\\\Q" ) + "\\E";
        }
        return Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
    }
}
