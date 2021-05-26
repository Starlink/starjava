package uk.ac.starlink.ttools.taplint;

import java.util.Arrays;
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
}
