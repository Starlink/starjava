package uk.ac.starlink.table;

import junit.framework.TestCase;

public class InfoTest extends TestCase {

    public InfoTest( String name ) {
        super( name );
    }

    public void testUtype() {
        ValueInfo colInfo = new ColumnInfo( "ABV", Double.class, "Strength" );
        assertNull( Tables.getUtype( colInfo ) );
        Tables.setUtype( colInfo, "meta.weird" );
        assertEquals( "meta.weird", Tables.getUtype( colInfo ) );

        /* Currently utypes not supported for non-column ValueInfos.
         * If they become supported like UCDs are, modify this test. */
        ValueInfo info = new DefaultValueInfo( "ABV", Double.class,
                                               "Strength" );
        assertNull( Tables.getUtype( info ) );
        Tables.setUtype( info, "meta.weird" );
        assertNull( Tables.getUtype( info ) );
    }

    public void testUCD() {
        ColumnInfo colInfo = new ColumnInfo( "ABV", Double.class, "Strength" );
        assertNull( colInfo.getUCD() );
        colInfo.setUCD( "meta.weird" );
        assertEquals( "meta.weird", colInfo.getUCD() );
        DefaultValueInfo info = new DefaultValueInfo( "ABV", Double.class,
                                                      "Strength" );
        assertNull( info.getUCD() );
        info.setUCD( "meta.weird" );
        assertEquals( "meta.weird", info.getUCD() );
    }
}
