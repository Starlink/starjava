package uk.ac.starlink.ttools.task;

import junit.framework.TestCase;
import uk.ac.starlink.task.TaskException;

public class CalcTest extends TestCase {

    public CalcTest( String name ) {
        super( name );
    }

    public void testCalc() throws Exception {
        assertEquals( "3", eval( "1+2" ) );
        assertEquals( "53729.0", eval( "isoToMjd(\"2005-12-25T00:00:00\")" ) );
    }

    public void testError() throws Exception {
        try {
            eval( "do what?" );
            fail();
        }
        catch ( TaskException e ) {
        }
    }

    private Object eval( String expr ) throws Exception {
        MapEnvironment env = new MapEnvironment()
                      .setValue( "expression", expr );
        new Calc().createExecutable( env ).execute();
        return env.getOutputText().trim();
    }
}
