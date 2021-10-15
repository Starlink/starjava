package uk.ac.starlink.ttools.taplint;

import java.util.Arrays;
import java.util.regex.Pattern;
import junit.framework.TestCase;

public class CodeTest extends TestCase {

    public void testFixedCodes() {
        for ( FixedCode code : Arrays.asList( FixedCode.values() ) ) {
            String name = code.name();
            assertTrue( code.name().matches( "^[EWSIF]_[A-Z0-9]{4}$" ) );
            assertNotNull( code.getType() );
            assertEquals( 4, code.getLabel().length() );
            assertTrue( "Votlint error reporting Namespace clash: " + code,
                        code.getLabel().charAt( 0 ) !=
                        ReporterSaxMessager.VOTLINT_PREFIX_CHAR );
            assertNotNull( code.getDescription() );
        }
    }

    public void testIso8601() {
        Pattern timestampRegex = EpnTapStage.DALI_TIMESTAMP_REGEX;
        String[] goodTimestamps = new String[] {
            "2017-03-10T10:30:51.407564",
            "1999-12-31T23:59:59.999999Z",
            "2000-01-01T00:00:00",
            "2017-03-10T10:30:51.40",
            "2017-03-10T10:30:51Z",
            "2007-10-07",
        };
        
        for ( String timestamp : goodTimestamps ) {
            assertTrue( timestamp, timestampRegex.matcher( timestamp ).matches() );
        }
        String[] badTimestamps = new String[] {
            "2017-03-10T10:30:51+3",
            "2017-03-10T10:30",
            "2012-03-13 10:30",
            "2017-03-10 10:30:51.407564",
            "2017-03-10 10:30:51",
            "9999-99-99T99:99:99",
        };
        for ( String timestamp : badTimestamps ) {
            assertFalse( timestampRegex.matcher( timestamp ).matches() );
        }
    }
}
