package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.InputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.lint.DoctypeInterpolator;
import uk.ac.starlink.ttools.lint.LintContext;
import uk.ac.starlink.ttools.lint.Linter;
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
    private final Parameter versionParam_;

    public VotLint() {
        inParam_ = new InputStreamParameter( "votable" );
        inParam_.setDefault( "-" );
        inParam_.setPosition( 1 );

        validParam_ = new BooleanParameter( "validate" );
        validParam_.setDefault( true );

        versionParam_ = new ChoiceParameter( "version",
                                             LintContext.VOT_VERSIONS );
        versionParam_.setNullPermitted( true );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            inParam_,
            validParam_,
            versionParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        String version = versionParam_.stringValue( env );

        /* Create a lint context. */
        boolean validate = validParam_.booleanValue( env );
        final LintContext context = new LintContext( version );
        context.setValidating( validate );
        context.setOutput( env.getPrintStream() );
        if ( env instanceof TableEnvironment ) {
            context.setDebug( ((TableEnvironment) env).isDebug() );
        }

        /* Get basic input stream. */
        String sysid = inParam_.stringValue( env );
        InputStream in = inParam_.inputStreamValue( env );

        return new VotLintExecutable( in, validate, context, sysid );
    }

    private class VotLintExecutable implements Executable {

        final InputStream in_;
        final boolean validate_;
        final LintContext context_;
        final String sysid_;

        VotLintExecutable( InputStream in, boolean validate, 
                          LintContext context, String sysid ) {
            in_ = in;
            validate_ = validate;
            context_ = context;
            sysid_ = sysid;
        }

        public void execute() throws IOException, ExecutionException {

            /* Buffer the stream for efficiency. */
            InputStream in = new BufferedInputStream( in_ );

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
                    if ( context_.getVersion() == null ) {
                        context_.setVersion( foundVers );
                    }
                }
            }

            /* Turn the stream into a SAX source. */
            InputSource sax = new InputSource( in_ );
            sax.setSystemId( sysid_ );

            /* Perform the parse. */
            try {
                new Linter( context_ ).createParser( context_.isValidating() )
                                      .parse( sax );
            }
            catch ( SAXException e ) {
                throw new ExecutionException( e.getMessage(), e );
            }
        }
    }
}
