/**
 * Test ProxySetupFrame by creating an instance for interaction.
 */

import javax.swing.JFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.ProxySetupFrame;

public class TestProxySetupFrame 
{
    /**
     * Create a test instance.
     */
    public static void main( String args[] )
    {
        ProxySetup.getInstance().restore();
        ProxySetupFrame setupFrame = new ProxySetupFrame();
        setupFrame.setVisible( true );
        
        // Exit when window is closed.
        WindowListener closer = new WindowAdapter() {	
                public void windowClosing( WindowEvent e ) {
                    System.exit( 1 );
                }
            };
        setupFrame.addWindowListener( closer );
    }
}
