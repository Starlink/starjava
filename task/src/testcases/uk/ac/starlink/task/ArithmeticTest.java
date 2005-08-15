package uk.ac.starlink.task;

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

public class ArithmeticTest extends TestCase {

    public ArithmeticTest( String name ) {
        super( name );
    }

    public void testArithmetic() throws TaskException {
        Map paramMap = new HashMap();
        paramMap.put( "first", "101" );
        paramMap.put( "second", "707" );
        Task task = new AddTask();
        MapEnvironment env = new MapEnvironment( paramMap );
        task.invoke( env );
        assertEquals( "101 + 707 = 808.0", 
                      env.getOutputText().trim() );
    }

    public static void main( String[] args ) throws TaskException {
        Task task = new AddTask();
        Parameter[] params = task.getParameters();
        Environment env = new TerminalEnvironment( args, params );
        task.invoke( env );
    }

    private static class AddTask implements Task {
        private DoubleParameter p1;
        private DoubleParameter p2;

        public AddTask() {
            p1 = new DoubleParameter( "first" );
            p1.setPosition( 1 );
            p1.setPrompt( "First number" );

            p2 = new DoubleParameter( "second" );
            p2.setPosition( 2 );
            p2.setPrompt( "Second number" );
            p2.setDefault( "0" );
        }

        public Parameter[] getParameters() {
            return new Parameter[] { p1, p2 };
        }

        public String getUsage() {
            return "first second";
        }

        public void invoke( Environment env ) throws TaskException {
            double val1 = p1.doubleValue( env );
            double val2 = p2.doubleValue( env );
            env.getPrintStream().println( "   " +
                                          p1.stringValue( env ) + " + " +
                                          p2.stringValue( env ) + " = " +
                                          ( val1 + val2 ) );
        }
    }
}
