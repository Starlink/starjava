/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import uk.ac.starlink.help.HelpFrame;

/**
 * Simple class for testing {@link HelpFrame}.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class TestHelpFrame
    extends JFrame
{
    public TestHelpFrame()
    {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        URL helpSet = TestHelpFrame.class.getResource( "/HelpSet.hs" );
        if ( helpSet != null ) {
            System.out.println( "helpSet = " + helpSet );
            try {
                HelpFrame.addHelpSet( helpSet );
                HelpFrame.setHelpTitle( "Example Help" );
                HelpFrame.createHelpMenu( "sub-help", "Help on window",
                                          "main-help", "Help on application",
                                          menuBar, null );
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println( "Error: couldn't find /HelpSet.hs" );
            System.exit( 1 );
        }
    }

    public static void main( String[] args )
    {
        TestHelpFrame frame = new TestHelpFrame();
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.setSize( new Dimension( 100, 100 ) );
        frame.setVisible( true );

        // Exit when window is closed.
        WindowListener closer = new WindowAdapter() {   
                public void windowClosing( WindowEvent e ) {
                    System.exit( 1 );
                }
            };
        frame.addWindowListener( closer );
    }
}
