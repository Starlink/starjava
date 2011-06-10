package uk.ac.starlink.ttools.votlint;

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
        VotLintContext context = new VotLintContext();
        Set vSet = new HashSet();
        Set nSet = new HashSet();
        Set nsSet = new HashSet();
        for ( int i = 0; i < versions.length; i++ ) {
            VotableVersion version = versions[ i ];
            assertNotNull( version.getDTD( context ) );
            String number = version.getNumber();
            String namespace = version.getNamespaceUri();
            assertEquals( number, version.toString() );
            assertNotNull( number );
            assertEquals( version,
                          VotableVersion.getVersionByNumber( number ) );
            assertEquals( version,
                          VotableVersion.getVersionByNamespace( namespace ) );
            vSet.add( version );
            nSet.add( number );
            nsSet.add( namespace );
        }
        assertEquals( versions.length, vSet.size() );
        assertEquals( versions.length, nSet.size() );
        assertEquals( versions.length, nsSet.size() );
        List vList = new ArrayList( vSet );
        Collections.sort( vList );

        // This only works if the KNOWN_VERSIONS array is arranged in ascending
        // order.  It probably is.
        assertEquals( vList, Arrays.asList( versions ) );
    }
}
