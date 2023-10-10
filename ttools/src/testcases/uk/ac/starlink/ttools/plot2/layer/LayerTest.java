package uk.ac.starlink.ttools.plot2.layer;

import junit.framework.TestCase;

public class LayerTest extends TestCase {

    public void testMapper() {
        BinMapper mapper =
            BinMapper.createMapper( false, 0.002, 0.0, 76316552.13232906 );
    }
}
