package uk.ac.starlink.frog.iface;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import uk.ac.starlink.frog.Frog;
import uk.ac.starlink.frog.iface.PlotControlFrame;
import uk.ac.starlink.frog.iface.images.ImageHolder;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.FrogException;

/**
 * Class that displays a text box window to allow the user to see the 
 * debugging ouput produced by the FROG progam. This is a single
 * instance class, we really don't want multiple copies of the debugger.
 *
 * @since 10-DEC-2003
 * @author Alasdair Allan
 * @version $Id$
 * @see FrogDebug
 * @see "The Singleton Design Pattern"
 */
public class DebugConsole extends JInternalFrame
{
   /**
     *  Create the single class instance.
     */
    private static final DebugConsole instance = new DebugConsole();
    
    /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();     
            
    /**
     * Frog object that this is being created from...
     */
     Frog frame = null;
                      
    /**
     * TextArea for debugging information
     */   
     JTextArea debugArea =  new JTextArea();
    
    /**
     * Create an instance of the popup 
     */
    public DebugConsole( )
    {
        super( "Debugging Window", true, false, true, true );
            
        //enableEvents( AWTEvent.WINDOW_EVENT_MASK ); 
        try {
            // Build the User Interface
            initUI( );
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }
   
    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference reference to the instance of this class.
     */
    public static DebugConsole getReference()
    {
        return instance;
    }
    
    /**
     * Initialize the user interface components.
     */
    protected void initUI( ) throws Exception
    {

       // Setup the JInternalFrame
       setDefaultCloseOperation( JInternalFrame.HIDE_ON_CLOSE );
       addInternalFrameListener( new DialogListener() );  
         
       // grab Frog object  
       frame = debugManager.getFrog();
          
       // setup the scroll pane
       debugArea.setColumns(40);
       debugArea.setRows(25);
       JScrollPane scrollPane = new JScrollPane(debugArea,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       debugArea.setEditable(false);
       
       // Stuff them into the main panel
       // ------------------------------

       GridBagConstraints constraints = new GridBagConstraints();
       constraints.weightx = 1.0;
       constraints.weighty = 1.0;
       constraints.fill = GridBagConstraints.BOTH;
       
       JPanel mainPanel = new JPanel();
           
       // create the main panel
       mainPanel.setLayout( new GridBagLayout() );
       
       constraints.gridx = 0;  
       constraints.gridy = 0;        
       mainPanel.add( scrollPane, constraints );
                                    
       // display everything
       JPanel contentPane = (JPanel) this.getContentPane();
       contentPane.setLayout( new BorderLayout() );
       contentPane.add(mainPanel, BorderLayout.CENTER );
       
       // pack the window
       pack();
       
       // position window, really need to do this relative to window size
       this.setLocation( 300, 25 ); 
       
       // display the window             
       JDesktopPane desktop = frame.getDesktop();
       desktop.add( this );

    }
    
   /**
     * Open the debugging window
     */
    public void openDebug( )
    { 
        debugManager.print( "Debugging directed to window...");
        debugManager.setConsoleFlag( false );
        this.setVisible( true );
        debugManager.print( "Debugging directed to window...");
    }    
        
   /**
     * Close the debugging window
     */
    public void closeDebug( )
    { 
        debugManager.print( "Debugging directed to console...");
        debugManager.setConsoleFlag( true );
        this.setVisible( false );
        debugManager.print( "Debugging directed to console...");
    }    
    
      
   /**
     * Print a debugging message
     *
     * @param string String will be printed only if debugging is on.
     */
     public void write( String s )
     {
         debugArea.append( s );
     }    
  
}
