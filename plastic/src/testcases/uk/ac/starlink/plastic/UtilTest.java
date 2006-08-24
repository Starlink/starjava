package uk.ac.starlink.plastic;

import junit.framework.TestCase;

public class UtilTest extends TestCase {

    public UtilTest( String name ) {
        super( name );
    }

    public void testImages() {
        assertEquals( 24, PlasticUtils.getSendIcon().getIconWidth() );
        assertEquals( 24, PlasticUtils.getSendIcon().getIconHeight() );
        assertEquals( 24, PlasticUtils.getBroadcastIcon().getIconWidth() );
        assertEquals( 24, PlasticUtils.getBroadcastIcon().getIconHeight() );
        assertEquals( PlasticUtils.getSendIcon(), PlasticUtils.getSendIcon() );
        assertTrue( ! PlasticUtils.getSendIcon()
                     .equals( PlasticUtils.getBroadcastIcon() ) );
    }
}
