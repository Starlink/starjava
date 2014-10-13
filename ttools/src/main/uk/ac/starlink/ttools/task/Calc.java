package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;

/**
 * Task to do static calculations.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2005
 */
public class Calc implements Task {

    private final StringParameter exprParam_;
    private final InputTableParameter tableParam_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    public Calc() {
        exprParam_ = new StringParameter( "expression" );
        exprParam_.setPosition( 1 );
        exprParam_.setUsage( "<expr>" );
        exprParam_.setPrompt( "Expression to evaluate" );
        exprParam_.setDescription( new String[] {
            "<p>An expression to evaluate.",
            "The functions in <ref id='staticMethods'/> can be used.",
            "</p>",
        } );

        tableParam_ = new InputTableParameter( "table" );
        tableParam_.setNullPermitted( true );
        tableParam_.setUsage( "<table>" );
        tableParam_.setPrompt( "Table providing context for expression" );
        tableParam_.setDescription( new String[] {
            "<p>A table which provides the context within which",
            "<code>" + exprParam_.getName() + "</code> is evaluated.",
            "This parameter is optional, and will usually not be required;",
            "its only purpose is to allow use of constant expressions",
            "(table parameters) associated with the table.",
            "These can be referenced using identifiers of the form",
            "<code>" + StarTableJELRowReader.PARAM_PREFIX + "*</code>,",
            "<code>" + StarTableJELRowReader.UCD_PREFIX + "*</code> or",
            "<code>" + StarTableJELRowReader.UTYPE_PREFIX + "*</code> -",
            "see <ref id='jel-paramref'/> for more detail.",
            "</p>",
        } );
    }

    public String getPurpose() {
        return "Evaluates expressions";
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            exprParam_,
            tableParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {

        /* Get a row reader.  This doesn't have any data, but if a table
         * was given on the command line then that table's parameters
         * may be used for expression evaluation. */
        String tname = tableParam_.stringValue( env );
        StarTable table = tname == null || tname.trim().length() == 0
                        ? null
                        : tableParam_.tableValue( env );
        final JELRowReader rdr = JELUtils.createDatalessRowReader( table );

        /* Compile the expression. */
        Library lib = JELUtils.getLibrary( rdr );
        String expr = exprParam_.stringValue( env );
        final PrintStream out = env.getOutputStream();
        final CompiledExpression compEx;
        try { 
            compEx = Evaluator.compile( expr, lib );
        }
        catch ( CompilationException e ) {
            throw new TaskException( "Bad expression \"" + expr + "\": " + 
                                     e.getMessage(), e );
        }

        /* Create and return the executable. */
        return new Executable() {

            public void execute() throws TaskException {
                out.println( "   " + evaluate() );
            }

            private Object evaluate() throws TaskException {
                Object result;
                try {
                    return rdr.evaluate( compEx );
                }
                catch ( NullPointerException e ) {
                    return null;
                }
                catch ( Throwable e ) {
                    String msg = e.getMessage();
                    if ( msg == null ) {
                        msg = "Execution error: " + e.toString();
                    }
                    else {
                        msg = "Execution error: " + msg;
                    }
                    throw new TaskException( msg, e );
                }
            }
        };
    }
}
