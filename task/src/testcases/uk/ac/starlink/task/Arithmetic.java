package uk.ac.starlink.task;

public class Arithmetic {

    public static void main( String[] args ) throws TaskException, Exception {
        Task task = new AddTask();
        Parameter[] params = task.getParameters();
        Environment env = new TerminalEnvironment( args, params );
        for ( int i = 0; i < params.length; i++ ) {
            params[ i ].setEnvironment( env );
        }
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

        public void invoke( Environment env )
                throws ExecutionException, ParameterValueException, AbortException {
            double val1 = p1.doubleValue();
            double val2 = p2.doubleValue();
            env.getPrintStream().println( "   " +
                                          p1.stringValue() + " + " +
                                          p2.stringValue() + " = " +
                                          ( val1 + val2 ) );
        }
    }
}
