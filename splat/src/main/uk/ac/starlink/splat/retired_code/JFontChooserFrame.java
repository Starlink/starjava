package uk.ac.starlink.splat.iface;

import java.awt.Font;

import javax.swing.JFrame;
import uk.ac.starlink.util.gui.JFontChooser;

public class JFontChooserFrame extends JFrame 
{
    boolean packFrame = false;
    
    public JFontChooserFrame() 
    {
        pack();
        setVisible( true );

        JFontChooser frame = new JFontChooser( this, "Font Selector", true );
        frame.validate();
        frame.show();
        if ( frame.accepted() ) {
            System.out.println( "Getting selected Font" );
            Font newFont = frame.getSelectedFont();
            System.out.println( newFont.getName() );
        } else {
            System.out.println( "No selection" );
        }
        frame = null;
        System.exit( 1 );
    }
    
    // Main method
    public static void main(String[] args) 
    {
        new JFontChooserFrame();
    }
}
