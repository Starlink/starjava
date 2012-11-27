package uk.ac.starlink.ttools.votlint;

import junit.framework.TestCase;
import uk.ac.starlink.votable.VOTableVersion;

public class VersionDetailTest extends TestCase {
    public void testVersions() {
        VOTableVersion[] versions =
            VOTableVersion.getKnownVersions().values()
                          .toArray( new VOTableVersion[ 0 ] );
        for ( int i = 0; i < versions.length; i++ ) {
            VersionDetail detail = getDetail( versions[ i ] );
            assertNotNull( detail );
            assertNotNull( detail.getAttributeCheckers( "PARAM" )
                                 .get( "ref" ) );
            assertNotNull( detail.createElementHandler( "PARAM" ) );
            assertEquals( 0, detail.getAttributeCheckers( "NotAnElement" )
                                   .size() );
            assertNull( detail.createElementHandler( "NotAnElement" ) );
        }
        assertNull( getDetail( VOTableVersion.V12 )
                   .createElementHandler( "BINARY2" ) );
        assertNotNull( getDetail( VOTableVersion.V13 )
                      .createElementHandler( "BINARY2" ) );
    }

    private static VersionDetail getDetail( VOTableVersion version ) {
        return VersionDetail
              .getInstance( new VotLintContext( version, true, false, null ) );
    }
}
