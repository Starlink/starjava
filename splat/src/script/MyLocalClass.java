/**
 * Demonstration class for loading local classes into SPLAT.
 */

import javax.swing.*;
import java.awt.event.*;
import uk.ac.starlink.splat.iface.SplatBrowser;

public class MyLocalClass
{
    public MyLocalClass( SplatBrowser browser )
    {
        JFrame f = new JFrame( "MyLocalClass JFrame" );
        JButton b = new JButton( "Press me" );
        f.getContentPane().add( b );
        f.pack();
        f.show();
        this.browser = browser;
        b.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    loadImage();
                }
            });
    }

    SplatBrowser browser;
    public void loadImage()
    {
        browser.displaySpectrum
            ("/home2/pdraper/java/top/uk/ac/starlink/splat/testdata/test_stand.sdf");
    }
}
