/** Coco.java
 *
 * @author Roy Platon
 * @version 1.00 25 October 2002
 * Provides front end to run Coco as an application.
 **/

package uk.ac.starlink.coco;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import uk.ac.starlink.coco.*;
    
/**
 * Creates a dialogue box to enter Details.
 */
public class Coco extends JPanel {
    static CocoPanel pane;
    static String task = null;
    static Container contentPane;

/**
 *  This is the entry point for an Applet
 */
    public void init( ) {
        pane = new CocoPanel( this );
        add( pane );
        pane.setup( task );
    }

/**
 *  This main routine will run as an application
 */    
    static public void main( String[] args ) {
        if ( args.length > 0 ) task = args[0];
        if ( task == null ) task = "Radial Velocities";
        new CocoFrame( new Coco(), 680, 400 );
    }
}

/** 
 * This class sets properties for a Pane to run coco as a GUI by 
 * entering at the applet init method
 */
class CocoFrame extends JFrame {
    CocoFrame( Coco c, int x, int y ) {
        setTitle( c.getClass().getName() );
        setSize( x, y );
        addWindowListener( new WindowAdapter()  {
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        } );
        Container contentPane = getContentPane();
        contentPane.add( c );
        c.init();
        setVisible( true );
    }
}
