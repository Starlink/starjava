package uk.ac.starlink.topcat.plot;

import junit.framework.TestCase;

public class ColumnTest extends TestCase {

    public ColumnTest( String name ) {
        super( name );
    }

    public void testConstantColumnData() {
        assertEquals( ConstantColumnData.NAN, ConstantColumnData.NAN );
        assertEquals( ConstantColumnData.ZERO, ConstantColumnData.ZERO );
        assertEquals( ConstantColumnData.ONE, ConstantColumnData.ONE );
        assertEquals( ConstantColumnData.NAN,
                      new ConstantColumnData( "null", Double.NaN ) );
        assertEquals( ConstantColumnData.ZERO,
                      new ConstantColumnData( "zero", 0. ) );
        assertEquals( ConstantColumnData.ONE,
                      new ConstantColumnData( "one", 1. ) );
        assertFalse( ConstantColumnData.NAN
                    .equals( new ConstantColumnData( "Unity", 1.0 ) ) );

        assertEquals( "Two", 
                      new ConstantColumnData( "Two", 2.0 ).getColumnInfo()
                                                          .getName() );
    }

}
