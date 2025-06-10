package uk.ac.starlink.vo;

import junit.framework.TestCase;

public class CodeTest extends TestCase {

    public void testUcdStatus() {
        for ( String ucd : new String[] {
                  "pos.eq.ra;meta.main",
                  "stat.likelihood",
                  "TIME.PERIOD;STAT.MAX",
                  "pos.eq.ra;meta.main",
                  "phys.voltage",
              } ) {
            UcdStatus status = UcdStatus.getStatus( ucd );
            UcdStatus.Code code = status.getCode();
            assertEquals( UcdStatus.Code.OK, code );
            assertFalse( code.isError() );
            assertFalse( code.isWarning() );
            assertNull( status.getMessage(), status.getMessage() );
        }
        for ( String ucd : new String[] {
                  "pos.earth;pos.bodyrc.lat",
                  "pos.bodyrc",
                  "pump.m.up",
                  "POS_EQ_RA_MAIN;META_MAIN",
              } ) {
            UcdStatus status = UcdStatus.getStatus( ucd );
            UcdStatus.Code code = status.getCode();
            assertTrue( code.isError() );
            assertFalse( code.isWarning() );
            assertTrue( status.getMessage().length() > 0 );
        }
        UcdStatus ucd1Status = UcdStatus.getStatus( "POS_EQ_DEC_MAIN" );
        assertEquals( UcdStatus.Code.UCD1, ucd1Status.getCode() );
        assertTrue( ucd1Status.getMessage().indexOf( "UCD1" ) >= 0 );

        UcdStatus deprecStatus = UcdStatus.getStatus( "time.expo" );
        UcdStatus.Code deprecCode = deprecStatus.getCode();
        assertEquals( UcdStatus.Code.DEPRECATED, deprecCode );
        assertTrue( deprecStatus.getMessage().indexOf( "DEPRECATED" ) >= 0 );
        assertFalse( deprecCode.isError() );
        assertTrue( deprecCode.isWarning() );
    }

    public void testUnitStatus() {
        for ( String unit : new String[] {
                  "m",
                  "m**2",
                  "m**-2/s",
                  "m.N",
                  "mN",
              } ) {
            UnitStatus status = UnitStatus.getStatus( unit );
            UnitStatus.Code code = status.getCode();
            assertEquals( UnitStatus.Code.OK, code );
            assertFalse( code.isError() );
            assertNull( status.getMessage() );
        }
        for ( String unit : new String[] {
                  "m^2",
                  "m**-2 / s",
              } ) {
            UnitStatus status = UnitStatus.getStatus( unit );
            UnitStatus.Code code = status.getCode();
            assertTrue( code.isError() );
            assertTrue( status.getMessage().length() > 0 );
        }
        for ( String unit : new String[] {
                  "'electron'.pix**-1",
                  "sizeOfWales",
              } ) {
            UnitStatus status = UnitStatus.getStatus( unit );
            UnitStatus.Code code = status.getCode();
            assertFalse( code.isError() );
            assertTrue( code.isWarning() );
            assertTrue( status.getMessage().length() > 0 );
        }
        assertEquals( UnitStatus.Code.WHITESPACE,
                      UnitStatus.getStatus( "m / s" ).getCode() );
        assertEquals( UnitStatus.Code.DEPRECATED,
                      UnitStatus.getStatus( "Angstrom" ).getCode() );
    }
}
