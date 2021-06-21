package uk.ac.starlink.vo;

import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

public class UserAgentTest extends TestCase {

    public UserAgentTest() {
        Logger.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
    }

    public static void testTokens() {
        assertEquals( "(IVOA-test)", UserAgentUtil.COMMENT_TEST );
        assertEquals( "(IVOA-copy)", UserAgentUtil.COMMENT_COPY );

        assertEquals( "(IVOA-tat xxx)",
                      UserAgentUtil.createOpPurposeComment( "tat", "xxx" ) );
    }

    public void testSysprop() {
        String agent0 = System.getProperty( "http.agent" );
        checkSysprop();
        System.setProperty( "http.agent", "(Dummy)" );
        checkSysprop();
        if ( agent0 == null ) {
            System.clearProperty( "http.agent" );
        }
        else {
            System.setProperty( "http.agent", agent0 );
        }
    }

    private void checkSysprop() {
        String agent0 = System.getProperty( "http.agent" );
        UserAgentUtil.pushUserAgentToken( "(zzz)" );
        String agent1 = System.getProperty( "http.agent" );
        if ( agent0 == null || agent0.trim().length() == 0 ) {
            assertEquals( "(zzz)", agent1 );
        }
        else {
            assertEquals( agent0 + " (zzz)", agent1 );
        }
        UserAgentUtil.popUserAgentToken( "(zzz)" );
        assertEquals( agent0, System.getProperty( "http.agent" ) );
    }
}
