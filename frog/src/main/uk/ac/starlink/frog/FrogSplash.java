package uk.ac.starlink.frog;

// GUI stuff
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

// FROG stuff
import uk.ac.starlink.frog.iface.images.ImageHolder;

/**
 * Class which spawns the FROG Splash Screen
 *
 * @see Frog, FrogMain
 * @since $Date$
 * @author Alasdair Allan (Starlink, University of Exeter)
 * @version $Id$
 */
public class FrogSplash extends JWindow
{

   /**
     * Image for action icons.
     */ 
    protected static ImageIcon splashImage = 
        new ImageIcon( ImageHolder.class.getResource( "splash_frog.jpg" ) );

    /**
     * Create the splash screen
     */
    public FrogSplash( Frame frame ) 
    {
       
       super(frame);
       
       // Create splash screen
       JLabel splash = new JLabel( splashImage );
       getContentPane().add(splash, BorderLayout.CENTER);
       pack();
       
       // Position the splash screen
       Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
       Dimension labelSize = splash.getPreferredSize();
       setLocation( screenSize.width/2 - (labelSize.width/2),
                    screenSize.height/2 - (labelSize.height/2));
       
       // Add listener, splash screen will dispose() if clicked
       addMouseListener(new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
             setVisible(false);
             dispose();
          }
       });
       
       // Setup the splash screen to automatically disappear in 4000ms
       final int pause = 4000;
       
       final Runnable closerRunner = new Runnable() {
          public void run() {
             setVisible(false);
             dispose();
          }
       };
       
       Runnable waitRunner = new Runnable() {
          public void run() {
             try {
                Thread.sleep(pause);
                SwingUtilities.invokeAndWait(closerRunner);
             } catch(Exception e) {
                e.printStackTrace();
             }
          }
       };
       
       // Make it visible
       setVisible(true);
       
       // Start the thread to make it disappear
       Thread splashThread = new Thread(waitRunner, "SplashThread");
       splashThread.start();
    }       
 
}
