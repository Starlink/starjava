package uk.ac.starlink.splat.iface;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.UIManager;

/**
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) Peter W. Draper<p>
 * Company:      Starlink, Durham University<p>
 * @author Peter W. Draper
 * @version 1.0
 */
public class SelectCharactersWindow 
    implements SelectCharactersListener 
{
    // Construct the application
    public SelectCharactersWindow() {
        SelectCharacters frame = new SelectCharacters( new Font(
            "Lucida Bright", Font.PLAIN, 24 ) );
        frame.validate();
        
        //Center the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
        
        //  Register ourselves to recieve the output.
        frame.addListener( this );

    }

    public void newCharacters( SelectCharactersEvent e ) {
        System.out.println( "Received: " + e.getText() );
        System.exit( 1 );
    }

    // Main method
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        new SelectCharactersWindow();
    }
}
