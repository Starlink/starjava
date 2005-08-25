package uk.ac.starlink.ttools.task;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.ttools.TableTestCase;

public class VotLintTest extends TableTestCase {

    public VotLintTest( String name ) {
        super( name );
    }

    private String[] getOutputLines( Map map ) throws Exception {
       MapEnvironment env = new MapEnvironment( map );
       new VotLint().createExecutable( env ).execute();
       return env.getOutputLines();
    }

    public void testSilent() throws Exception {
        Map map = new HashMap();
        map.put( "votable",
                 getClass().getResource( "no-errors.vot.gz" ).toString() );
        assertEquals( 0, getOutputLines( map ).length );
    }

    public void testErrors() throws Exception {

        String[] errors = new String[] {
            "INFO (l.4): No arraysize for character, "
                + "FIELD implies single character",
            "ERROR (l.7): Element \"TABLE\" does not allow "
                + "\"DESCRIPTION\" here.",
            "WARNING (l.11): Characters after first in char scalar ignored "
                + "(missing arraysize?)",
            "WARNING (l.15): Wrong number of TDs in row (expecting 3 found 4)",
            "ERROR (l.18): Row count (1) not equal to nrows attribute (2)",
        };

        Map map = new HashMap();
        map.put( "votable",
                 getClass().getResource( "with-errors.vot" ).toString() );
        assertArrayEquals( errors, getOutputLines( map ) );

        map.put( "validate", "false" );
        assertArrayEquals( new String[] { errors[ 0 ], errors[ 2 ],
                                          errors[ 3 ], errors[ 4 ] },
                           getOutputLines( map ) );

        map.put( "validate", "true" );
        assertArrayEquals( errors, getOutputLines( map ) );

        map.put( "version", "1.1" );
        assertArrayEquals( errors, getOutputLines( map ) );

        map.put( "version", "1.0" );
        assertEquals( "WARNING (l.1): Declared version (1.1) " +
                      "differs from version specified to linter (1.0)",
                      getOutputLines( map )[ 0 ] );

        map.put( "version", "9.9" );
        try {
            getOutputLines( map );
            fail();
        }
        catch ( ParameterValueException e ) {
            // OK
        }
    }
}
