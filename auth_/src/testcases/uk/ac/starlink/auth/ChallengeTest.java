package uk.ac.starlink.auth;

import java.util.LinkedHashMap;
import uk.ac.starlink.util.TestCase;

public class ChallengeTest extends TestCase {

    public void testChallenges() {
        String txt1 =
            "Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\"";
        String txt2 =
            "Basic realm=\"simple\"";
        String txt3 =
            "Basic realm=\"Authentication needed\"";
        String cookieScheme = "ivo://ivoa.net/std/SSO#cookie";   // not legal
        cookieScheme = "vo-sso-cookie";
        String txt4 =
            cookieScheme + " realm=\"Authentication needed\", " +
            "accessURL=\"https://geadev.esac.esa.int/tap-server/login\", " +
            "userParameter=\"username\", passwordParameter=\"password\", " +
            "responseCookie=\"JSESSIONID\"";
        Challenge ch1 =
            new Challenge( "Newauth", "apps",
                           new ParamMap()
                          .addParam( "type", "1" )
                          .addParam( "title", "Login to \"apps\"" ) );
        Challenge ch2 =
            new Challenge( "Basic", "simple", new ParamMap() );
        Challenge ch3 =
            new Challenge( "basic", "Authentication needed", new ParamMap() );
        Challenge ch4 =
            new Challenge( cookieScheme, "Authentication needed",
                           new ParamMap()
                          .addParam(
                               "accessURL",
                               "https://geadev.esac.esa.int/tap-server/login" )
                          .addParam( "userParameter", "username" )
                          .addParam( "passwordParameter", "password" )
                          .addParam( "responseCookie", "JSESSIONID" ) );

        assertChallengesEqual( txt1, ch1 );
        assertChallengesEqual( txt2, ch2 );
        assertChallengesEqual( txt1 + ", " + txt2, ch1, ch2 );
        assertChallengesEqual( txt3, ch3 );
        assertChallengesEqual( txt4, ch4 );
        assertChallengesEqual( txt1 + " , " + txt2 + "," + txt3 + ", " + txt4,
                               ch1, ch2, ch3, ch4 );
    }

    private void assertChallengesEqual( String txt, Challenge... challenges ) {
        assertArrayEquals( challenges,
                           Challenge.parseChallenges( txt )
                                    .toArray( new Challenge[ 0 ] ) );
    }

    private static class ParamMap extends LinkedHashMap<String,String> {
        public ParamMap addParam( String key, String value ) {
            put( key, value );
            return this;
        }
    }
}
