package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.PrintStream;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.JELRowReader;
import uk.ac.starlink.ttools.JELUtils;
import uk.ac.starlink.ttools.filter.DummyJELRowReader;

/**
 * Task to do static calculations.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2005
 */
public class Calc implements Task {

    private final Parameter exprParam_;

    public Calc() {
        exprParam_ = new Parameter( "expression" );
        exprParam_.setPosition( 1 );
        exprParam_.setUsage( "<expr>" );
        exprParam_.setPrompt( "Expression to evaluate" );
        exprParam_.setDescription( new String[] {
            "An expression to evaluate.",
            "The functions in <ref id='staticMethods'/> can be used.",
        } );
    }

    public Parameter[] getParameters() {
        return new Parameter[] { exprParam_ };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        StarTable dummyTable = ColumnStarTable.makeTableWithRows( 0 );
        final JELRowReader rdr = new DummyJELRowReader( dummyTable );
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
        return new Executable() {

            public void execute() throws TaskException {
                out.println( "   " + evaluate() );
            }

            private Object evaluate() throws TaskException {
                Object result;
                try {
                    return compEx.evaluate( new Object[] { rdr } );
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
