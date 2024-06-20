package uk.ac.starlink.table;

import uk.ac.starlink.util.TestCase;

public class TableTest extends TestCase {

    public TableTest( String name ) {
        super( name );
    }

    public void testArrayColumn() {
        final int nrow = 100;

        int[] iData = new int[ nrow ];
        double[] xData = new double[ nrow ];
        double[] yData = new double[ nrow ];

        fillCycle( iData, 1, nrow );
        fillCycle( xData, 0, 10 );
        fillRandom( yData, 0., 1. );

        ColumnInfo iColInfo =
            new ColumnInfo( "Index", Integer.class, "row index" );
        ColumnInfo xColInfo =
            new ColumnInfo( "X", Double.class, "X coordinate" );
        ColumnInfo yColInfo =
            new ColumnInfo( "Y", Double.class, "Y coordinate" );

        ArrayColumn iCol = PrimitiveArrayColumn
                          .makePrimitiveColumn( iColInfo, iData );
        ArrayColumn xCol = PrimitiveArrayColumn
                          .makePrimitiveColumn( xColInfo, xData );
        ArrayColumn yCol = PrimitiveArrayColumn
                          .makePrimitiveColumn( yColInfo, yData );

        ColumnStarTable st = ColumnStarTable.makeTableWithRows( (long) nrow );
        st.setName( "Position data table" );
        st.addColumn( iCol );
        st.addColumn( xCol );
        st.addColumn( yCol );

        ValueInfo fruitInfo = 
            new DefaultValueInfo( "Fruit", String.class,
                                  "Like a vegetable, only sweeter" );
        assertNull( st.getParameterByName( fruitInfo.getName() ) );
        st.setParameter( new DescribedValue( fruitInfo, "Banana" ) );
        assertEquals( "Banana", 
                      st.getParameterByName( fruitInfo.getName() ).getValue() );
        st.setParameter( new DescribedValue( fruitInfo, "Kumquat" ) );
        assertEquals( "Kumquat", 
                      st.getParameterByName( fruitInfo.getName() ).getValue() );
    }

    public void testFormatting() {
        DefaultValueInfo info = new DefaultValueInfo( "test", Double.class );
        assertEquals( "1.2345678",
                      info.formatValue( Double.valueOf( 1.2345678 ), 100 ) );
        assertEquals( "1.234",
                      info.formatValue( Double.valueOf( 1.2345678 ), 5 ) );
        assertEquals( "1.234...", 
                      info.formatValue( Double.valueOf( 1.2345678e10 ), 8 ) );
    }
}
