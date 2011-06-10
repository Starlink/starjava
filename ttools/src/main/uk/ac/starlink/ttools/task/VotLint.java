package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.InputStreamParameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.votlint.DoctypeInterpolator;
import uk.ac.starlink.ttools.votlint.VotLintContext;
import uk.ac.starlink.ttools.votlint.VotLinter;
import uk.ac.starlink.ttools.votlint.VotableVersion;
import uk.ac.starlink.util.DataSource;

/**
 * Task which performs VOTable checking.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class VotLint implements Task {

    private final InputStreamParameter inParam_;
    private final BooleanParameter validParam_;
    private final ChoiceParameter versionParam_;
    private final OutputStreamParameter outParam_;

    public VotLint() {
        inParam_ = new InputStreamParameter( "votable" );
        inParam_.setDefault( "-" );
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
        validParam_.setDefault( true );
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

        String[] versions = new String[ VotableVersion.KNOWN_VERSIONS.length ];
        for ( int i = 0; i < VotableVersion.KNOWN_VERSIONS.length; i++ ) {
            versions[ i ] = VotableVersion.KNOWN_VERSIONS[ i ].getNumber();
        }
        versionParam_ =
            new ChoiceParameter( "version", VotableVersion.KNOWN_VERSIONS );
        versionParam_.setNullPermitted( true );
        versionParam_.setPrompt( "VOTable standard version" );
        versionParam_.setDescription( new String[] {
            "<p>Selects the version of the VOTable standard which the input",
            "table is supposed to exemplify.",
            "Currently the version can be 1.0, 1.1 or 1.2.",
            "The version may also be specified within the document",
            "using the \"version\" attribute of the document's VOTABLE",
            "element; if it is and it conflicts with the value specified",
            "by this flag, a warning is issued.",
            "</p>",
        } );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPrompt( "File for output messages" );
        outParam_.setUsage( "<location>" );
        outParam_.setDefault( "-" );
        outParam_.setDescription( new String[] {
            "<p>Destination file for output messages.",
            "May be a filename or \"-\" to indicate standard output.",
            "</p>",
        } );
    }

    public String getPurpose() {
        return "Validates VOTable documents";
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            inParam_,
            validParam_,
            versionParam_,
            outParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        VotableVersion version =
            (VotableVersion) versionParam_.objectValue( env );

        /* Create a lint context. */
        boolean validate = validParam_.booleanValue( env );
        final VotLintContext context = new VotLintContext( version );
        context.setValidating( validate );
        if ( env instanceof TableEnvironment ) {
            context.setDebug( ((TableEnvironment) env).isDebug() );
        }

        /* Get basic input stream. */
        String sysid = inParam_.stringValue( env );
        InputStream in = inParam_.inputStreamValue( env );

        /* Get output stream. */
        PrintStream out;
        try {
            out = new PrintStream( outParam_.destinationValue( env )
                                            .createStream() );
        }
        catch ( IOException e ) {
            throw new UsageException( "Can't open \""
                                     + outParam_.stringValue( env )
                                     + "\" for output: " + e.getMessage(), e );
        }
        context.setOutput( out );

        return new VotLintExecutable( in, validate, context, sysid );
    }

    private class VotLintExecutable implements Executable {

        final InputStream baseIn_;
        final boolean validate_;
        final VotLintContext context_;
        final String sysid_;

        VotLintExecutable( InputStream in, boolean validate, 
                           VotLintContext context, String sysid ) {
            baseIn_ = in;
            validate_ = validate;
            context_ = context;
            sysid_ = sysid;
        }

        public void execute() throws IOException, ExecutionException {

            /* Buffer the stream for efficiency. */
            InputStream in = new BufferedInputStream( baseIn_ );

            /* Interpolate the VOTable DOCTYPE declaration if required. */
            if ( validate_ ) {
                DoctypeInterpolator interp = new DoctypeInterpolator() {
                    public void message( String msg ) {
                        context_.info( msg );
                    }
                };
                in = interp.getStreamWithDoctype( (BufferedInputStream) in );
                String foundVers = interp.getVotableVersion();
                if ( foundVers != null ) {
                    VotableVersion fvers =
                        VotableVersion.getVersionByNumber( foundVers );
                    if ( fvers == null ) {
                        context_.warning( "Unknown VOTABLE version "
                                        + foundVers + " declared" );
                    }
                    if ( context_.getVersion() == null && fvers != null ) {
                        context_.setVersion( fvers );
                    }
                }
            }

            /* Turn the stream into a SAX source. */
            InputSource sax = new InputSource( in );
            sax.setSystemId( sysid_ );

            /* Perform the parse. */
            try {
                new VotLinter( context_ )
                   .createParser( context_.isValidating() )
                   .parse( sax );
            }
            catch ( SAXException e ) {
                throw new ExecutionException( e.getMessage(), e );
            }
        }
    }
}
