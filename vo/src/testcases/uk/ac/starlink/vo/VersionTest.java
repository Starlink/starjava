package uk.ac.starlink.vo;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

    public void testVersions() {
        assertFalse( TapVersion.V10.is11() );
        assertTrue( TapVersion.V11.is11() );

        assertFalse( DatalinkVersion.V10.is11() );
        assertTrue( DatalinkVersion.V11.is11() );
        assertTrue( DatalinkVersion.V10.getStandardId().isValid() );
        assertTrue( DatalinkVersion.V11.getStandardId().isValid() );

        assertTrue( SiaVersion.V20.compareTo( SiaVersion.V10 ) > 0 );
    }
}
