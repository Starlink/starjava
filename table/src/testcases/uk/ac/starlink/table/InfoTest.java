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

        DefaultValueInfo info = new DefaultValueInfo( "ABV", Double.class,
                                                      "Strength" );
        assertNull( info.getUtype() );
        info.setUtype( "meta.weird" );
        assertEquals( "meta.weird", info.getUtype() );
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
