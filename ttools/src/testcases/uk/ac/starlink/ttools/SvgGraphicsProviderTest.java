package uk.ac.starlink.ttools;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.util.LogUtils;

public class SvgGraphicsProviderTest extends TestCase {

    public SvgGraphicsProviderTest() {
        LogUtils.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
    }

    public void testProvider() throws IOException {
        SvgGraphicsProvider svgp = SvgGraphicsProvider.getInstance();
        Graphics2D g2 = svgp.createGraphics2D( 500, 100 );
        String txt = "I wanna be a wallaby";
        g2.drawString( txt, 50, 50 );
        String svgel = svgp.getSVGElement( g2 );
        assertTrue( svgel.startsWith( "<svg " ) );
        assertTrue( svgel.indexOf( txt ) > 0 );
    }
}
