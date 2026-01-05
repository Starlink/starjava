package uk.ac.starlink.vo;

import java.util.logging.Level;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class UserAgentTest extends TestCase {

    public UserAgentTest() {
        LogUtils.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
    }

    public static void testTokens() {
        assertEquals( "(IVOA-test)",
                      UserAgentUtil
                     .createOpPurposeComment( UserAgentUtil.PURPOSE_TEST,
                                              null ) );
        assertEquals( "(IVOA-copy)",
                      UserAgentUtil
                     .createOpPurposeComment( UserAgentUtil.PURPOSE_COPY,
                                              null ) );
        assertEquals( "(IVOA-tat xxx)",
                      UserAgentUtil.createOpPurposeComment( "tat", "xxx" ) );
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
}
