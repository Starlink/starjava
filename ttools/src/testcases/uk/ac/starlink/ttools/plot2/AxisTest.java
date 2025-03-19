package uk.ac.starlink.ttools.plot2;

import junit.framework.TestCase;

public class AxisTest extends TestCase {

    public void testAxes() {
        Scale[] scales = {
            ScaleTest.createAsinhScale( 0.01 ),
            ScaleTest.createAsinhScale( Math.E ),
            ScaleTest.createAsinhScale( 6.6 ),
            ScaleTest.createAsinhScale( 50 ),
            ScaleTest.createSymlogScale( 1, 1 ),
            ScaleTest.createSymlogScale( .01, 5 ),
            ScaleTest.createSymlogScale( 1e6, 6 ),
        };
        for ( int ig = 0; ig < 10; ig++ ) {
            for ( int id = 0; id < 10; id++ ) {
                int glo = 50;
                int ghi = 60 + 35 * ig;
                double dlo = 1.1e3;
                double dhi = dlo + 10 + 500 * id;
                for ( boolean flip : new boolean[] { false, true } ) {
                    for ( Scale scale : scales ) {
                        Axis ax1 = new Axis( glo, ghi, dlo, dhi, scale, flip );
                        exerciseAxis( ax1, flip );
                    }
                }
            }
        }
    }

    private void exerciseAxis( Axis axis, boolean isFlip ) {
        int[] gbounds = axis.getGraphicsLimits();
        double[] dbounds = axis.getDataLimits();
        assertEquals( dbounds[ isFlip ? 1 : 0 ],
                      axis.graphicsToData( gbounds[ 0 ] ),
                      1e-5 );
        assertEquals( dbounds[ isFlip ? 0 : 1 ],
                      axis.graphicsToData( gbounds[ 1 ] ),
                      1e-5 );
        assertEquals( (double) gbounds[ 0 ],
                      axis.dataToGraphics( dbounds[ isFlip ? 1 : 0 ] ),
                      1e-5 );
        assertEquals( (double) gbounds[ 1 ],
                      axis.dataToGraphics( dbounds[ isFlip ? 0 : 1 ] ),
                      1e-5 );
    }
}
