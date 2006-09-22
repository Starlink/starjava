package uk.ac.starlink.plastic;

import java.io.IOException;
import java.util.Arrays;
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

    public void testCheckArgs() throws IOException {
        Class[] reqs =
            new Class[] { Integer.class, Number.class, String.class };
        assertTrue( HubManager.checkArgs(
            Arrays.asList( new Object[] { new Integer( 1 ), new Float( 2f ), 
                                          "3", } ),
            reqs ) );
        assertTrue( HubManager.checkArgs(
            Arrays.asList( new Object[] { new Integer( 1 ),
                                          new Short( (short) 2 ), "3",
                                          new Object() } ),
            reqs ) );
        try {
            HubManager.checkArgs(
                Arrays.asList( new Object[] { new Integer( 1 ),
                                              new Short( (short) 2 ), } ),
                reqs );
            fail();
        }
        catch ( IOException e ) {
        }
        try {
            HubManager.checkArgs(
                Arrays.asList( new Object[] { new Integer( 1 ), "2", "3" } ),
                reqs );
            fail();
        }
        catch ( IOException e ) {
        }
    }
}
