package uk.ac.starlink.table;

import java.io.IOException;

public class MetaCopyStarTableTest extends TableCase {

    public void testMeta() throws IOException {
        ColumnInfo infoA = new ColumnInfo( "A", Integer.class, null );
        ColumnInfo infoB = new ColumnInfo( "B", String.class, "Description" );
        Object[] row = new Object[] { Integer.valueOf( 23 ), "Twenty-three" };
        ValueInfo xInfo = new DefaultValueInfo( "X", String.class, null );
        StarTable t1 =
            new ConstantStarTable( new ColumnInfo[] { infoA, infoB }, row, 5 );
        t1.setParameter( new DescribedValue( xInfo, "Ant" ) );
        t1.setName( "Insects" );
        StarTable t2 = new MetaCopyStarTable( t1 );
        assertTableEquals( t1, t2 );
        checkStarTable( t1 );
        checkStarTable( t2 );
        assertEquals( "Ant",
                      t2.getParameterByName( xInfo.getName() ).getValue() );
        t1.setParameter( new DescribedValue( xInfo, "Bee" ) );
        assertEquals( "Bee",
                      t1.getParameterByName( xInfo.getName() ).getValue() );
        assertEquals( "Ant",
                      t2.getParameterByName( xInfo.getName() ).getValue() );

        assertEquals( "A", t1.getColumnInfo( 0 ).getName() );
        assertEquals( "A", t2.getColumnInfo( 0 ).getName() );
        t1.getColumnInfo( 0 ).setName( "AA" );
        assertEquals( "AA", t1.getColumnInfo( 0 ).getName() );
        assertEquals( "A", t2.getColumnInfo( 0 ).getName() );

        assertEquals( "Insects", t1.getName() );
        assertEquals( "Insects", t2.getName() );
        t1.setName( "Arthropods" );
        assertEquals( "Arthropods", t1.getName() );
        assertEquals( "Insects", t2.getName() );
        t2.setName( "Outsects" );
        assertEquals( "Outsects", t2.getName() );
    }
}
