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

        ColumnInfo iColInfo = new ColumnInfo( "Index", Integer.class, "row index" );
        ColumnInfo xColInfo = new ColumnInfo( "X", Double.class, "X coordinate" );
        ColumnInfo yColInfo = new ColumnInfo( "Y", Double.class, "Y coordinate" );

        ArrayColumn iCol = ArrayColumn.makeColumn( iColInfo, iData );
        ArrayColumn xCol = ArrayColumn.makeColumn( xColInfo, xData );
        ArrayColumn yCol = ArrayColumn.makeColumn( yColInfo, yData );

        ColumnStarTable st = ColumnStarTable.makeTableWithRows( (long) nrow );
        st.setName( "Position data table" );
        st.addColumn( iCol );
        st.addColumn( xCol );
        st.addColumn( yCol );

    }
}
