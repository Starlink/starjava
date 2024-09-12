package uk.ac.starlink.topcat.plot2;

import java.util.Arrays;
import junit.framework.TestCase;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;

public class PlotTypeTest extends TestCase {

    public void testSideways() {
        int np = 0;
        for ( Plotter<?> plotter :
              HistogramPlotWindow.createHistogramPlotters() ) {
            if ( ! ( plotter instanceof FunctionPlotter ) ) {
                np++;
                assertTrue( Arrays.asList( plotter.getStyleKeys() )
                                  .contains( StyleKeys.SIDEWAYS ) );
            }
        }
        assertTrue( np > 3 );
    }
}
