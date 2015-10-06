package uk.ac.starlink.votable;

import java.util.regex.Pattern;
import junit.framework.TestCase;

public class VOTableDomainMappersTest extends TestCase {

    public void testTime() {
        Pattern ucdTimeRegex = VOTableDomainMappers.ISO8601_UCD_PATTERN;
        String[] yesTimeUcds = {
            "time", "time.start", "time.end", "time.epoch",
            "time;obs.exposure", "time.start;obs.exposure",
            "TIME_DATE", "TIME_DATE_OBS", "time_date",
        };
        String[] noTimeUcds = {
            "time.duration", "time.equinox", "meta.id;time", "pos.eq.ra", "",
            "TIME_DOTE", "TIME_DURATION", "time_duration",
        };
        for ( String ucd : yesTimeUcds ) {
            assertTrue( ucdTimeRegex.matcher( ucd ).matches() );
        }
        for ( String ucd : noTimeUcds ) {
            assertFalse( ucdTimeRegex.matcher( ucd ).matches() );
        }

        Pattern unitTimeRegex = VOTableDomainMappers.ISO8601_UNIT_PATTERN;
        String[] yesTimeUnits = {
            "ISO8601", "iso-8601", "iso 8601", "iso8601", "ISO 8601",
        };
        String[] noTimeUnits = {
            "ISO8602", "iso", "m.s^-1", "",
        };
        for ( String unit : yesTimeUnits ) {
            assertTrue( unitTimeRegex.matcher( unit ).matches() );
        }
        for ( String unit : noTimeUnits ) {
            assertFalse( unitTimeRegex.matcher( unit ).matches() );
        }
    }
}
