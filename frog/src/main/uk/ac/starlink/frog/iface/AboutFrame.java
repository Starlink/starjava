package uk.ac.starlink.frog.iface;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import uk.ac.starlink.frog.iface.images.ImageHolder;
import uk.ac.starlink.frog.util.Utilities;

/**
 * Class that displays a dialog window with the "about" information
 * for the application.
 *
 * @since 09-FEB-2003
 * @author Alasdair Allan
 * @version $Id$
 */
public class AboutFrame extends JFrame
{

   /**
     * Image for action icons.
     */ 
    protected static ImageIcon aboutImage = 
        new ImageIcon( ImageHolder.class.getResource( "about_frog.gif" ) );

   /**
     * String decribing the application
     */
    protected String description =
        "<html><font color=green>"+ Utilities.getFullDescription()+ "</font>" +
        "<br> Version: " + Utilities.getReleaseVersion() +
        "<br><br> <font color=blue>"+ Utilities.getOperatingSystem() + 
        "<br> JDK Version " + Utilities.getJavaVersion() +
        "<br> Copyright (C) 2002-2004 CLRC" +
        "<br> Author: Alasdair Allan (aa@astro.ex.ac.uk)&nbsp;</font></html>";  
   
    /**
     * String detailing the GNU public license
     */
    protected String licenseTerms = 
     "<html><center>" +
     "This program is free software; you can redistribute it<br>" +
     "and/or modify it under the terms of the GNU General<br>" +
     "Public License published by the Free Software Foundation;<br>" +
     "either version 2 of the License, or (at your option) any<br>" +
     "later version." +
     "<br><br>" +
     "This program is distributed in the hope that it will be<br>" +
     "useful, but WITHOUT ANY WARRANTY; without even the<br>" +
     "implied warranty of MERCHANTABILITY or FITNESS FOR A<br>" +
     "PARTICULAR PURPOSE. See the GNU General Public License<br>" +
     "for more details.You should have received a copy of<br>" +
     "the GNU General Public License along with this program;<br>" +
     "if not, write to the Free Software Foundation, Inc.,<br>" +
     "59 Temple Place, Suite 330, Boston, MA  02111-1307, USA" +
     "</center></html>";   
       
    /**
     * Create an instance. 
     */
    public AboutFrame( String s)
    {
        super( s );
        enableEvents( AWTEvent.WINDOW_EVENT_MASK );
        try {
            initUI();
            setSize( new Dimension( 420, 450 ) );
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the user interface components.
     */
    protected void initUI() throws Exception
    {
       // create the three main panels
       JPanel aboutPanel = new JPanel( new BorderLayout() );
       JPanel licensePanel = new JPanel( new BorderLayout() );
       JPanel buttonPanel = new JPanel( new BorderLayout() );
       
       // create the about panel
       JLabel imageLabel = new JLabel( aboutImage );
       JLabel authorLabel = new JLabel( description);
       aboutPanel.add( imageLabel, BorderLayout.WEST );
       aboutPanel.add( authorLabel, BorderLayout.CENTER );
   
       // create the license panel
       JLabel licenseLabel = new JLabel( licenseTerms );
       licenseLabel.setHorizontalAlignment(SwingConstants.CENTER);
       licensePanel.add( licenseLabel );
       
       // create the button panel
       JLabel buttonLabel = new JLabel ( "        " );
       JButton okButton = new JButton( "Ok" );
       okButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              dispose();
           }
        }); 
        
       buttonPanel.add( buttonLabel, BorderLayout.WEST );
       buttonPanel.add( okButton, BorderLayout.EAST );
    
       // display everything
       JPanel contentPane = (JPanel) this.getContentPane();
       contentPane.setLayout( new BorderLayout() );
       
       contentPane.add(aboutPanel, BorderLayout.NORTH );
       contentPane.add(licensePanel, BorderLayout.CENTER );
       contentPane.add(buttonPanel, BorderLayout.SOUTH );
       
       
       
 
    }

    /**
     * Looks out for window closing events.
     */
    protected void processWindowEvent(WindowEvent e) 
    {
        if ( e.getID() == WindowEvent.WINDOW_CLOSING ) {
            dispose();
        }
        super.processWindowEvent( e );
    }

}
