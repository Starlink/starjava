package uk.ac.starlink.ttools.plot2.layer;

import junit.framework.TestCase;
import uk.ac.starlink.ttools.plot2.Scale;

public class LayerTest extends TestCase {

    public void testMapper() {
        BinMapper mapper =
            new BinMapper( Scale.LINEAR, .002, 0.0, 76316552.13232906 );
    }
}
