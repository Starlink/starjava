package uk.ac.starlink.frog.iface;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Double;
import java.lang.Math;

import uk.ac.starlink.frog.Frog;
import uk.ac.starlink.frog.iface.GramControlFrame;
import uk.ac.starlink.frog.iface.images.ImageHolder;
import uk.ac.starlink.frog.data.GramManager;
import uk.ac.starlink.frog.data.GramComp;
import uk.ac.starlink.frog.data.Gram;
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.FrogException;

/**
 * TimeSeries MetaData Popup Class
 *
 * @since 21-MAR-2003
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeries
 */
public class GramMetaDataPopup extends JInternalFrame
{

    /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

    /**
     *  Manager class for Periodograms
     */
    protected GramManager 
         gramManager = GramManager.getReference();

    /**
     *  Manager class for Time Series
     */
    protected TimeSeriesManager 
         seriesManager = TimeSeriesManager.getReference();
    
    /**
     * GramControlFrame object containing the periodogram of interest.
     */
     GramControlFrame popupFrame = null;
                      
    /**
     * TextPane for information
     */   
     JTextPane textPane =  new JTextPane();
    
    /**
     * Create an instance of the dialog
     *
     * @param f The GramControlFrame holding the periodogram of interest
     */
    public GramMetaDataPopup(  GramControlFrame f  )
    {
        super( "", false, true, false, false );
        popupFrame = f;           // grab the interesting frame
            
        try {
            // Build the User Interface
            this.setTitle( gramManager.getKey( popupFrame ) );
            initUI( );
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the user interface components.
     */
    protected void initUI( ) throws Exception
    {

       // Setup the JInternalFrame
       setDefaultCloseOperation( JInternalFrame.DISPOSE_ON_CLOSE );
       addInternalFrameListener( new DialogListener() );  
         
       // grab Frog object  
       Frog frame = debugManager.getFrog();
             
       // grab location and position window
       Dimension popupSize = getSize();
       Dimension frameSize = frame.getSize();
       setLocation( ( frameSize.width - popupSize.width ) / 4 ,
                    ( frameSize.height - popupSize.height ) / 4  );
       
       // Grab TimeSeriesComp object
       GramComp popupComp = gramManager.getGram( popupFrame );
       Gram popupGram = popupComp.getGram();
       
       // create the main panel
       // ---------------------
        
       // setup the scroll pane
       JScrollPane scrollPane = new JScrollPane( textPane,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       scrollPane.setPreferredSize( new Dimension(500, 450) );
       scrollPane.setMinimumSize( new Dimension(500, 300) );
       textPane.setEditable(false);
       textPane.setContentType("text/html");
           
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
       // create the button panels
       JPanel buttonPanel = new JPanel( new BorderLayout() );
       JPanel buttons = new JPanel( new BorderLayout() );
              
       // create the button panel
       // -----------------------
       JLabel buttonLabel = new JLabel ( "          " );
               
       JButton cancelButton = new JButton( "Cancel" );
       cancelButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              dispose();
           }
        }); 
        
       buttons.add( cancelButton, BorderLayout.EAST );
 
       buttonPanel.add( buttonLabel, BorderLayout.CENTER );
       buttonPanel.add( buttons, BorderLayout.EAST );
    
       // display everything
       JPanel contentPane = (JPanel) this.getContentPane();
       contentPane.setLayout( new BorderLayout() );
       
       contentPane.add(mainPanel, BorderLayout.NORTH );
       contentPane.add(buttonPanel, BorderLayout.SOUTH );
              
       // Add information to textArea
       // ---------------------------
       String doc = "<html><body>";
       
       doc = doc + "<h2><u>Periodogram</u></h2>\n";
          
       doc = doc + "<ul>\n";
       
       TimeSeriesComp popupTimeComp = popupGram.getTimeSeriesComp();
       TimeSeries popupSeries = popupTimeComp.getSeries();
       String key = seriesManager.getKey( popupTimeComp );
       
       doc = doc + "<li>The periodogram is associated with <strong>" 
             + key + "</strong>\n";
       
       if ( popupGram.getType() ==  Gram.UNCLASSIFIED ) {
          doc = doc + "<li>The periodogram is of unknown type\n" +
           "<li><font color=red>Warning: Possible programming error?</font>\n"
           + "</ul>\n";
  
       } else if ( popupGram.getType() == Gram.FOURIER ) { 
          doc = doc + "<li>The periodogram is a Fourier Transform</ul>\n";

       } else if ( popupGram.getType() == Gram.CHISQ ) { 
          doc = doc + "<li>The periodogram is a Chi-Squared Periodogram</ul>\n";


       }
       
       // best fit period?
       String periodDoc = "";
       if ( popupGram.haveBestPeriod() ) {
        
         double range[] = popupGram.getRange();        
       
         periodDoc = "<h2><u>Best Period</u></h2>\n"; 
         periodDoc = periodDoc + "<ul>\n"
                     + " <li> Minimum Frequency is " + range[0] + "\n"
                     + " <li> Maximum Frequency is " + range[1] + "\n"
                     + " <li> Maximum power at frequency of " + 
                         1.0/popupGram.getBestPeriod() + "\n"
                     + " <li> This corresponds to a period of " +
                          popupGram.getBestPeriod() + "\n</ul>\n";  

       }
       
       
       // drop document into textArea
       doc = doc + periodDoc + "</body></html>";
       debugManager.print("             Passing HTML Document to textPane...");
       debugManager.print("\n" + doc + "\n");
       textPane.setText( doc );

        
       // Pack now avoiding problems with the scrollbar?
       pack();

    }
   
}
