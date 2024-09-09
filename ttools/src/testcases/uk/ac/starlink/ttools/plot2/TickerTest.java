package uk.ac.starlink.ttools.plot2;

import junit.framework.TestCase ;
import uk.ac.starlink.ttools.plot2.geom.SlaveTicker;

public class TickerTest extends TestCase {

    public void testSlave() {
        double dlo = 0.0;
        double dhi = 1.0;
        int npix = 400;
        int glo = 1000;
        Axis masterAxis =
            Axis.createAxis( glo, glo + npix, dlo, dhi, false, false );
        Ticker basicTicker = BasicTicker.LINEAR;
        SlaveTicker slaveTicker =
            new SlaveTicker( masterAxis, x -> x*x, basicTicker );
        Orientation[] orients = new Orientation[] { Orientation.X };
        TickRun masterTickRun =
            basicTicker.getTicks( dlo, dhi, false, NullCaptioner.INSTANCE,
                                  orients, npix, 1 );
        TickRun slaveTickRun =
            slaveTicker.getTicks( dlo, dhi, false, NullCaptioner.INSTANCE,
                                  orients, npix, 1 );
    }
}
