package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.taplint.IvoaSchemaResolver;

public class XsdValidateTest extends TestCase {

    public void testValidate() throws Exception {
        String goodJob = getLocation( "goodjob.xml" );
        String funnyJob = getLocation( "funnyjob.xml" );
        String funnySchema = getLocation( "funnyUws.xsd" );
        String uwsUri = IvoaSchemaResolver.UWS_URI;
        String funnySchemaLoc = uwsUri + "=" + funnySchema;

        assertValid( true,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", goodJob ) );
        assertValid( false,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", funnyJob ) );

        assertValid( false,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", goodJob )
                    .setValue( "schemaloc", funnySchemaLoc ) );
        assertValid( true,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", funnyJob )
                    .setValue( "schemaloc", funnySchemaLoc ) );

        assertValid( true,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", goodJob )
                    .setValue( "topel", "job" ) );
        assertValid( true,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", goodJob )
                    .setValue( "topel", "{" + uwsUri + "}job" ) );
        assertValid( true,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", goodJob )
                    .setValue( "topel", "{" + uwsUri + "}job" ) );
        assertValid( false,
                     new MapEnvironment()
                    .setValue( "uselocals", Boolean.TRUE )
                    .setValue( "doc", goodJob )
                    .setValue( "topel", "{" + uwsUri + "}jub" ) );

        assertValid( true,
                     new MapEnvironment()
                    .setValue( "doc", funnySchema )
                    .setValue( "topel",
                               "{http://www.w3.org/2001/XMLSchema}schema" ) );
    }

    private void assertValid( boolean isValid, MapEnvironment env )
            throws IOException, TaskException {
        Executable exec = new XsdValidate().createExecutable( env );
        boolean success;
        try {
            exec.execute();
            success = true;
        }
        catch ( ExecutionException e ) {
            success = false;
        }
        assertTrue( isValid == success );
    }

    private static String getLocation( String name ) {
        return XsdValidateTest.class.getResource( name ).toString();
    }

}
