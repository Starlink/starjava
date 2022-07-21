package uk.ac.starlink.vo;

import java.util.logging.Level;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class UserAgentTest extends TestCase {

    public UserAgentTest() {
        LogUtils.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
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

    public void testParse() {
        assertArrayEquals(
            new String[] { "DaCHS/2.3.2", "twistedWeb/18.9.0", },
            UserAgentUtil.parseProducts( "DaCHS/2.3.2 twistedWeb/18.9.0" ) );
        assertArrayEquals(
            new String[] { "Apache/2.4.29", "(Ubuntu)", "mod_jk/1.2.43", },
            UserAgentUtil
           .parseProducts( "Apache/2.4.29 (Ubuntu) mod_jk/1.2.43" ) );
        assertArrayEquals(
            new String[] { "Aaa", "(yyy-) zzz)", "bbb", },
            UserAgentUtil.parseProducts( "Aaa (yyy-\\) z\\zz) bbb" ) );
        checkBadProducts( "aaa (bbb)b ccc" );
        checkBadProducts( "<word>" );
    }

    private void checkBadProducts( String txt ) {
        try {
            UserAgentUtil.parseProducts( txt );
            fail( txt );
        }
        catch ( RuntimeException e ) {
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
