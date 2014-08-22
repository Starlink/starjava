package uk.ac.starlink.task;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

public class ArithmeticTest extends TestCase {

    public ArithmeticTest( String name ) {
        super( name );
    }

    public void testArithmetic() throws TaskException, IOException {
        Map<String,String> paramMap = new HashMap<String,String>();
        paramMap.put( "first", "101" );
        paramMap.put( "second", "707" );
        Task task = new AddTask();
        assertEquals( "first [second] [comment]", 
                      TerminalInvoker.getTaskUsage( task ) );
        MapEnvironment env = new MapEnvironment( paramMap );
        task.createExecutable( env ).execute();
        assertEquals( "101 + 707 = 808.0", 
                      env.getOutputText().trim() );
    }

    public static void main( String[] args ) throws TaskException, IOException {
        Task task = new AddTask();
        Parameter[] params = task.getParameters();
        Environment env = new TerminalEnvironment( args, params );
        task.createExecutable( env ).execute();
    }

    private static class AddTask implements Task {
        private DoubleParameter p1;
        private IntegerParameter p2;
        private StringParameter p3;

        public AddTask() {
            p1 = new DoubleParameter( "first" );
            p1.setPosition( 1 );
            p1.setPrompt( "First number" );

            p2 = new IntegerParameter( "second" );
            p2.setPosition( 2 );
            p2.setPrompt( "Second number" );
            p2.setIntDefault( 0 );
            assertEquals( "0", p2.getStringDefault() );
            p2.setStringDefault( "0" );
            assertEquals( "0", p2.getStringDefault() );

            p3 = new StringParameter( "comment" );
            p3.setPrompt( "Comment" );
            p3.setNullPermitted( true );
        }

        public Parameter[] getParameters() {
            return new Parameter[] { p1, p2, p3 };
        }

        public Executable createExecutable( Environment env )
                throws TaskException {
            final String sval1 = p1.stringValue( env );
            final String sval2 = p2.stringValue( env );
            final double val1 = p1.doubleValue( env );
            final int val2 = p2.intValue( env );
            final PrintStream out = env.getOutputStream();
            return new Executable() {
                public void execute() {
                    out.println( "   " + sval1 + " + " + sval2 + " = "
                                 + ( val1 + val2 ) );
                }
            };
        }

        public String getPurpose() {
            return "Adds two numbers";
        }
    }
}
