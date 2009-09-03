package uk.ac.starlink.ttools.lint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

public class LintTest extends TestCase {

    public void testVersions() {
        VotableVersion[] versions = VotableVersion.KNOWN_VERSIONS;
        LintContext context = new LintContext();
        Set vSet = new HashSet();
        Set nSet = new HashSet();
        for ( int i = 0; i < versions.length; i++ ) {
            VotableVersion version = versions[ i ];
            assertNotNull( version.getDTD( context ) );
            String number = version.getNumber();
            assertEquals( number, version.toString() );
            assertNotNull( number );
            assertEquals( version, VotableVersion.getVersion( number ) );
            vSet.add( version );
            nSet.add( number );
        }
        assertEquals( versions.length, vSet.size() );
        assertEquals( versions.length, nSet.size() );
        List vList = new ArrayList( vSet );
        Collections.sort( vList );

        // This only works if the KNOWN_VERSIONS array is arranged in ascending
        // order.  It probably is.
        assertEquals( vList, Arrays.asList( versions ) );
    }
}
