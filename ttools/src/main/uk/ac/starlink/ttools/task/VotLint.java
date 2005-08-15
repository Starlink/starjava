package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
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

    private final Parameter sysidParam_;
    private final BooleanParameter validParam_;
    private final Parameter versionParam_;

    public VotLint() {
        sysidParam_ = new Parameter( "votable" );
        sysidParam_.setDefault( "-" );
        sysidParam_.setPosition( 1 );

        validParam_ = new BooleanParameter( "validate" );
        validParam_.setDefault( true );

        versionParam_ = new ChoiceParameter( "version",
                                             LintContext.VOT_VERSIONS );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            sysidParam_,
            validParam_,
            versionParam_,
        };
    }

    public String getUsage() {
        return null;
    }

    public void invoke( Environment env ) throws TaskException {
        try {
            doInvoke( env );
        }
        catch ( IOException e ) {
            throw new ExecutionException( e.getMessage(), e );
        }
        catch ( SAXException e ) {
            throw new ExecutionException( e.getMessage(), e );
        }
    }

    private void doInvoke( Environment env )
            throws TaskException, IOException, SAXException {

        /* Create a lint context. */
        String version = versionParam_.stringValue( env );
        boolean validate = validParam_.booleanValue( env );
        final LintContext context = new LintContext( version );
        context.setValidating( validate );
        context.setOutput( env.getPrintStream() );
        if ( env instanceof TableEnvironment ) {
            context.setDebug( ((TableEnvironment) env).isDebug() );
        }

        /* Get basic input stream. */
        InputStream in;
        String sysid = sysidParam_.stringValue( env );
        if ( sysid == null || sysid.equals( "-" ) ) {
            in = System.in;
        }
        else {
            in = DataSource.getInputStream( sysid );
        }

        /* Buffer the stream for efficiency. */
        in = new BufferedInputStream( in );

        /* Interpolate the VOTable DOCTYPE declaration if required. */
        if ( validate ) {
            DoctypeInterpolator interp = new DoctypeInterpolator() {
                public void message( String msg ) {
                    context.info( msg );
                }
            };
            in = interp.getStreamWithDoctype( (BufferedInputStream) in );
            String foundVers = interp.getVotableVersion();
            if ( foundVers != null ) {
                if ( context.getVersion() == null ) {
                    context.setVersion( foundVers );
                }
            }
        }

        /* Turn the stream into a SAX source. */
        InputSource sax = new InputSource( in );
        sax.setSystemId( sysid );

        /* Perform the parse. */
        new Linter( context ).createParser( context.isValidating() )
                             .parse( sax );
    }
}
