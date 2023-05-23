package uk.ac.starlink.ttools.plot2.geom;

import java.util.BitSet;
import junit.framework.TestCase;

public class MatrixTest extends TestCase {

    public void testShapes() {

        /* I think these tests are pretty comprehensive. */
        for ( int i = 1; i < 10; i++ ) {
            for ( int k = 0; k < 8; k++ ) {
                boolean hasDiag = ( k & 1 ) != 0;
                boolean hasLower = ( k & 2 ) != 0;
                boolean hasUpper = ( k & 4 ) != 0;
                MatrixShape shape =
                    new MatrixShape( i, hasDiag, hasLower, hasUpper );
                MatrixShape shape2 =
                    new MatrixShape( i, hasDiag, hasLower, hasUpper );
                assertEquals( shape, shape2 );
                assertEquals( shape.hashCode(), shape2.hashCode() );
                exerciseShape( shape );
            }
        }
    }

    private void exerciseShape( MatrixShape shape ) {
        int nx = shape.getWidth();
        int nc = shape.getCellCount();
        BitSet present = new BitSet();
        for ( int ix = 0; ix < nx; ix++ ) {
            for ( int iy = 0; iy < nx; iy++ ) {
                int index = shape.getIndex( ix, iy );
                boolean isPresent = index >= 0;
                if ( isPresent ) {
                    assertFalse( present.get( index ) );
                    present.set( index );
                }
                if ( ix == iy ) {
                    assertEquals( isPresent, shape.hasDiagonal() );
                }
                if ( ix < iy ) {
                    assertEquals( isPresent, shape.hasUpper() );
                }
                if ( ix > iy ) {
                    assertEquals( isPresent, shape.hasLower() );
                }
            }
        }
        assertEquals( nc, present.nextClearBit( 0 ) );
        for ( int ic = 0; ic < nc; ic++ ) {
            MatrixShape.Cell cell = shape.getCell( ic );
            int x = cell.getX();
            int y = cell.getY();
            assertTrue( x >= 0 && x < nx );
            assertTrue( y >= 0 && y < nx );
            assertEquals( ic, shape.getIndex( cell ) );
        }
    }

    public void printShape( MatrixShape shape ) {
        for ( int ic = 0; ic < shape.getCellCount(); ic++ ) {
            MatrixShape.Cell cell = shape.getCell( ic );
            System.out.println( ic + ": " + cell );
        }
    }
}
