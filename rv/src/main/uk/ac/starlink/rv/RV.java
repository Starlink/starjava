/** RV.java
 *
 * @author Roy Platon
 * @version 1.00 26 MArch 2002
 * Provides front end to run RV as an Application.
 **/

package uk.ac.starlink.rv;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import uk.ac.starlink.rv.*;
    
/**
 * The main application for running RV as an Application or an Applet.
 */
public class RV extends JPanel {
    private static RVPanel pane;
    private static String task = null;
    private static Container contentPane;

/**
 * Initialise the GUI and create the Panel
 */
    public void init( ) {
        pane = new RVPanel( this );
        add( pane );
        pane.setup( task );
    }
    
/**
 * Run RV as an Application
 */
    static public void main( String[] args ) {
        if ( args.length > 0 ) task = args[0];
        if ( task == null ) task = "Radial Velocities";
        new RVFrame( new RV( ), 800, 600 );
    }
}

/**
 * Run RV as an Application
 */
class RVFrame extends JFrame {
    RVFrame( RV a, int x, int y ) {
        setTitle( a.getClass().getName() );
        setSize( x, y );
        addWindowListener( new WindowAdapter()  {
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        } );
        Container contentPane = getContentPane();
        contentPane.add( a );
        a.init();
        setVisible( true );
    }
}
