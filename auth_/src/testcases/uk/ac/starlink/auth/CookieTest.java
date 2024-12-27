package uk.ac.starlink.auth;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import junit.framework.TestCase;

public class CookieTest extends TestCase {

    public void testCookieScope() throws Exception {
        CookieManager cman = new CookieManager();
        String url = "https://www.star.bristol.ac.uk/mbt";
        Map<String,List<String>> headers = new LinkedHashMap<>();
        String cookieValue = "JSESSIONID=A8C339BFEF1FEE9D; "
                           + "Path=/tap-server; Secure; HttpOnly";
        headers.put( "Set-Cookie", Collections.singletonList( cookieValue ) );
        cman.put( new URI( url ), headers );
        CookieStore cstore = cman.getCookieStore();
        ToIntFunction<String> cookieCount = uri -> {
            try {
                return cstore.get( new URI( uri ) ).size();
            }
            catch ( URISyntaxException e ) {
                return -1;
            }
        };

        assertEquals( 1, cstore.getCookies().size() );
        assertEquals( 1, cookieCount.applyAsInt( url ) );
        assertEquals( 1, cookieCount.applyAsInt( url + "/sub" ) );
        assertEquals( 0, cookieCount
                        .applyAsInt( url.replaceFirst( "bris", "bros" ) ) );
        assertEquals( 0, cookieCount.applyAsInt( "http://evil.com/" ) );
    }
}
