package uk.ac.starlink.ttools.votlint;

import junit.framework.TestCase;
import uk.ac.starlink.votable.VOTableVersion;

public class VersionDetailTest extends TestCase {
    private static final VOTableVersion VOTLINT_UNSUPPORTED_VERSION = null;
    public void testVersions() {
        VOTableVersion[] versions =
            VOTableVersion.getKnownVersions().values()
                          .toArray( new VOTableVersion[ 0 ] );
        for ( int i = 0; i < versions.length; i++ ) {
            if ( versions[ i ] != VOTLINT_UNSUPPORTED_VERSION ) {
                VersionDetail detail = getDetail( versions[ i ] );
                assertNotNull( detail );
                assertNotNull( detail.getAttributeCheckers( "PARAM" )
                                     .get( "ref" ) );
                assertNotNull( detail.createElementHandler( "PARAM" ) );
                assertEquals( 0, detail.getAttributeCheckers( "NotAnElement" )
                                       .size() );
                assertNull( detail.createElementHandler( "NotAnElement" ) );
            }
        }
        assertNull( getDetail( VOTableVersion.V12 )
                   .createElementHandler( "BINARY2" ) );
        assertNotNull( getDetail( VOTableVersion.V13 )
                      .createElementHandler( "BINARY2" ) );
    }

    private static VersionDetail getDetail( VOTableVersion version ) {
        VotLintContext context =
            new VotLintContext( version, true,
                                new PrintSaxMessager( System.out, false, 1 ) );
        return VersionDetail.getInstance( context );
    }
}
