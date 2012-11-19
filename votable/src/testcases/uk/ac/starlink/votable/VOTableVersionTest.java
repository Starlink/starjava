package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.Arrays;
import junit.framework.TestCase;

public class VOTableVersionTest extends TestCase {

    public void testSchema() {
        // Check schema documents actually exist on the classpath.
        for ( VOTableVersion version :
              VOTableVersion.getKnownVersions().values() ) {
            assertTrue( version == VOTableVersion.V10 ||
                        version.getSchema() != null );
        }
    }

    public void testVersions() {
        assertEquals( Arrays.asList( new VOTableVersion[] {
            VOTableVersion.V10,
            VOTableVersion.V11,
            VOTableVersion.V12,
            VOTableVersion.V13,
        } ), new ArrayList( VOTableVersion.getKnownVersions().values() ) );
    }
}
