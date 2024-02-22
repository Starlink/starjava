package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import junit.framework.TestCase;

public class VOTableVersionTest extends TestCase {

    public void testSchema() {
        // Check schema documents actually exist on the classpath.
        for ( VOTableVersion version :
              VOTableVersion.getKnownVersions().values() ) {
            boolean hasDtd = ( version.getDoctypeDeclaration() != null );
            assertEquals( hasDtd, version == VOTableVersion.V10 );
            assertEquals( hasDtd, version.getDtdUrl() != null );
            assertEquals( hasDtd, version.getSchemaLocation() == null );
            assertEquals( hasDtd, version.getSchema() == null );
        }
    }

    public void testVersions() {
        Collection<VOTableVersion> versions =
            VOTableVersion.getKnownVersions().values();
        assertEquals( Arrays.asList( new VOTableVersion[] {
            VOTableVersion.V10,
            VOTableVersion.V11,
            VOTableVersion.V12,
            VOTableVersion.V13,
            VOTableVersion.V14,
            VOTableVersion.V15,
        } ), new ArrayList<VOTableVersion>( versions ) );
        SortedSet<VOTableVersion> sortedVersions =
            new TreeSet<VOTableVersion>( versions );
        assertEquals( new ArrayList<VOTableVersion>( sortedVersions ),
                      new ArrayList<VOTableVersion>( versions ) );

        assertNull( VOTableVersion.V14.getDraftIdentifier() );
        assertNull( VOTableVersion.V15.getDraftIdentifier() );
    }

    /**
     * Check known versions map contains version in ascending order,
     * as per contract.
     */
    public void testSequence() {
        List<VOTableVersion> list =
            new ArrayList( VOTableVersion.getKnownVersions().values() );
        List<VOTableVersion> list1 = new ArrayList<VOTableVersion>( list );

        /* This comparator might need to be rewritten if the VOTable version
         * numbers contain some versions that don't follow alphabetic
         * sequence. */
        Collections.sort( list1, new Comparator<VOTableVersion>() {
            public int compare( VOTableVersion v1, VOTableVersion v2 ) {
                return v1.getVersionNumber().compareTo( v1.getVersionNumber() );
            }
        } );
        assertEquals( list, list1 );
    }
}
