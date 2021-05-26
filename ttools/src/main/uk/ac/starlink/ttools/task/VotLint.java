package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.InputStreamParameter;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.votlint.DoctypeInterpolator;
import uk.ac.starlink.ttools.votlint.PrintSaxMessager;
import uk.ac.starlink.ttools.votlint.SaxMessager;
import uk.ac.starlink.ttools.votlint.VersionDetector;
import uk.ac.starlink.ttools.votlint.VotLintCode;
import uk.ac.starlink.ttools.votlint.VotLintContext;
import uk.ac.starlink.ttools.votlint.VotLinter;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.VOTableVersion;

/**
 * Task which performs VOTable checking.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class VotLint implements Task {

    private final InputStreamParameter inParam_;
    private final BooleanParameter validParam_;
    private final IntegerParameter maxrepeatParam_;
    private final ChoiceParameter<VOTableVersion> versionParam_;
    private final OutputStreamParameter outParam_;

    public VotLint() {
        inParam_ = new InputStreamParameter( "votable" );
        inParam_.setPosition( 1 );
        inParam_.setPrompt( "VOTable location" );
        inParam_.setDescription( new String[] {
            "<p>Location of the VOTable to be checked.",
            "This may be a filename, URL or \"-\" (the default),", 
            "to indicate standard input.",
            "The input may be compressed using one of the known",
            "compression formats (Unix compress, gzip or bzip2).",
            "</p>",
        } );

        validParam_ = new BooleanParameter( "validate" );
        validParam_.setBooleanDefault( true );
        validParam_.setPrompt( "Validate against VOTable DTD?" );
        validParam_.setDescription( new String[] {
            "<p>Whether to validate the input document aganist",
            "the VOTable DTD.",
            "If true (the default), then as well as",
            "<code>votlint</code>'s own checks,",
            "it is validated against an appropriate version of the VOTable",
            "DTD which picks up such things as the presence of",
            "unknown elements and attributes, elements in the wrong place,",
            "and so on.",
            "Sometimes however, particularly when XML namespaces are",
            "involved, the validator can get confused and may produce",
            "a lot of spurious errors.  Setting this flag false prevents",
            "this validation step so that only <code>votlint</code>'s",
            "own checks are performed.",
            "In this case many violations of the VOTable standard",
            "concerning document structure will go unnoticed.",
            "</p>",
        } );

        maxrepeatParam_ = new IntegerParameter( "maxrepeat" );
        maxrepeatParam_.setIntDefault( 4 );
        maxrepeatParam_.setPrompt( "Maximum repeats of similar message" );
        maxrepeatParam_.setDescription( new String[] {
            "<p>Puts a limit on the number of times that the same issue",
            "will be reported.",
            "With this set to a relatively small number,",
            "the output is not cluttered with many repetitions of",
            "the same problem.",
            "</p>",
        } );

        versionParam_ =
            new ChoiceParameter<VOTableVersion>( "version",
                                VOTableVersion.getKnownVersions().values()
                               .toArray( new VOTableVersion[ 0 ] ) );
        versionParam_.setNullPermitted( true );
        versionParam_.setPrompt( "VOTable standard version" );
        versionParam_.setDescription( new String[] {
            "<p>Selects the version of the VOTable standard which the input",
            "table is supposed to exemplify.",
            "The version may also be specified within the document",
            "using the \"version\" attribute of the document's VOTABLE",
            "element; if it is and it conflicts with the value specified",
            "by this flag, a warning is issued.",
            "</p>",
            "<p>If no value is provided for this parameter (the default),",
            "the version will be determined from the VOTable itself.",
            "</p>",
        } );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPrompt( "File for output messages" );
        outParam_.setUsage( "<location>" );
        outParam_.setStringDefault( "-" );
        outParam_.setDescription( new String[] {
            "<p>Destination file for output messages.",
            "May be a filename or \"-\" to indicate standard output.",
            "</p>",
        } );
    }

    public String getPurpose() {
        return "Validates VOTable documents";
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            inParam_,
            maxrepeatParam_,
            validParam_,
            versionParam_,
            outParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        VOTableVersion version = versionParam_.objectValue( env );
        boolean validate = validParam_.booleanValue( env );
        int maxRepeat = maxrepeatParam_.intValue( env );
        boolean debug = env instanceof TableEnvironment
                     && ((TableEnvironment) env).isDebug();
        String sysid = inParam_.stringValue( env );
        InputStream in = inParam_.objectValue( env );
        PrintStream out;
        try {
            out = new PrintStream( outParam_.objectValue( env )
                                            .createStream() );
        }
        catch ( IOException e ) {
            throw new UsageException( "Can't open \""
                                     + outParam_.stringValue( env )
                                     + "\" for output: " + e.getMessage(), e );
        }
        SaxMessager messager = new PrintSaxMessager( out, debug, maxRepeat );
        return new VotLintExecutable( in, version, validate, sysid, messager );
    }

    private class VotLintExecutable implements Executable {

        final InputStream baseIn_;
        final VOTableVersion forceVersion_;
        final boolean validate_;
        final String sysid_;
        final SaxMessager messager_;

        VotLintExecutable( InputStream in, VOTableVersion forceVersion,
                           boolean validate, String sysid,
                           SaxMessager messager ) {
            baseIn_ = in;
            forceVersion_ = forceVersion;
            validate_ = validate;
            sysid_ = sysid;
            messager_ = messager;
        }

        public void execute() throws IOException, ExecutionException {

            /* Buffer the stream for efficiency and mark/reset capability. */
            BufferedInputStream bufIn = new BufferedInputStream( baseIn_ );

            /* Determine the VOTable version against which to check. */
            final VOTableVersion version;
            if ( forceVersion_ != null ) {
                version = forceVersion_;
            }
            else {

                /* If not specified by the command, determine the version from
                 * the document itself.  It's not necessary to report
                 * mismatches or bad values for the declared version here,
                 * since the linter will report such issues later. */
                String foundVersStr = VersionDetector.getVersionString( bufIn );
                VOTableVersion foundVersion = 
                    VOTableVersion.getKnownVersions().get( foundVersStr );
                if ( foundVersion != null ) {
                    version = foundVersion;
                }
                else {
                    Locator noloc = null;
                    messager_.reportMessage( SaxMessager.Level.INFO,
                                             "Unable to determine VOTable "
                                           + "version from document", noloc );
                    version = VOTableVersion.getDefaultVersion();
                    messager_.reportMessage( SaxMessager.Level.INFO,
                                             "Assuming VOTable v" + version
                                           + " by default", noloc );
                }
            }

            /* Create a context. */
            assert version != null;
            final VotLintContext context =
                new VotLintContext( version, validate_, messager_ );
               
            /* Interpolate the VOTable DOCTYPE declaration if required. */
            final InputStream in;
            if ( validate_ && version.getDoctypeDeclaration() != null ) {
                in = new DoctypeInterpolator() {
                    public void message( String msg ) {
                        context.info( new VotLintCode( "DOC" ), msg );
                    }
                }.getStreamWithDoctype( bufIn );
            }
            else {
                in = bufIn;
            }

            /* Turn the stream into a SAX source. */
            InputSource sax = new InputSource( in );
            sax.setSystemId( sysid_ );

            /* Perform the parse. */
            try {
                new VotLinter( context )
                   .createParser( null )
                   .parse( sax );
            }
            catch ( SAXException e ) {
                throw new ExecutionException( e.getMessage(), e );
            }
        }
    }
}
