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
import uk.ac.starlink.frog.iface.PlotControlFrame;
import uk.ac.starlink.frog.iface.images.ImageHolder;
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.MEMTimeSeriesImpl;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.Ephemeris;
import uk.ac.starlink.frog.data.SinFit;
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
public class MetaDataPopup extends JInternalFrame
{

    /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

    /**
     *  Manager class for TimeSeries
     */
    protected TimeSeriesManager 
         seriesManager = TimeSeriesManager.getReference();

    
    /**
     * PlotControlFrame object containing the TimeSeries of interest.
     */
     PlotControlFrame popupFrame = null;
                      
    /**
     * TextArea for information
     */   
     JTextArea textArea =  new JTextArea();
                
    /**
     * Create an instance of the dialog and proceed to do arithmetic
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     */
    public MetaDataPopup(  PlotControlFrame f  )
    {
        super( "", false, true, false, false );
        popupFrame = f;           // grab the interesting frame
            
        try {
            // Build the User Interface
            this.setTitle( seriesManager.getKey( popupFrame ) );
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
       TimeSeriesComp popupComp = seriesManager.getSeries( popupFrame );
       TimeSeries popupSeries = popupComp.getSeries();
       
       // create the main panel
       // ---------------------
          
       // setup the scroll pane
       textArea.setColumns(40);
       textArea.setRows(10);
       JScrollPane scrollPane = new JScrollPane( textArea,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       textArea.setEditable(false);
 
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
          
       // General Information    
       textArea.append("Full Name   = " +  popupSeries.getFullName()+ "\n" );
       textArea.append("Short Name   = " + popupSeries.getShortName()+ "\n" );
       textArea.append("Series Type  = " + popupSeries.getType()+ "\n" );
       textArea.append("Has Errors?  = " + popupSeries.haveYDataErrors()+ "\n");
       textArea.append("Draw Errors? = " + popupSeries.isDrawErrorBars()+ "\n"); 
       textArea.append("Data Points  = " + popupSeries.size()+ "\n"  );
       textArea.append("Data Format  = " + popupSeries.getDataFormat()+ "\n" );
       textArea.append("Plot Style   = " + popupSeries.getPlotStyle()+ "\n"  );
       textArea.append("Mark Style   = " + popupSeries.getMarkStyle()+ "\n"  );
       textArea.append("Mark Size    = " + popupSeries.getMarkSize()+ "\n"  ); 
       if( popupFrame.getPlot().getPlot().getDataLimits().isPlotInMags() ) {
          textArea.append( "yFlipped     = true\n");
       } else {
          textArea.append( "yFlipped     = false\n");
       } 
                      
       // Frame Name
       // Time Series  seriesManager.getKey(popupFrame)
 
       // Series Name        
       // Name popupSeries.getFullName()          
       debugManager.print("             Appending name..." );
       textArea.append( popupSeries.getFullName() + "\n" );
          
       // Folded Information
       //
       //   popupSeries.getType() == TimeSeries.FOLDED ||
       //   popupSeries.getType() == TimeSeries.BINFOLDED 
       
       // Ephemeris ephem = popupSeries.getEphemeris();
       
       if ( popupSeries.getType() == TimeSeries.FOLDED ||
            popupSeries.getType() == TimeSeries.BINFOLDED  ) {
            
            debugManager.print("             Appending folding info..." );
            Ephemeris ephem = popupSeries.getEphemeris();
            textArea.append( 
               ephem.getZeroPoint() +  " + " + ephem.getPeriod() + " x E\n" );

       }
        
       // Fitting information
       //
       //   thisSeries.getType() == TimeSeries.SINCOSFIT
       
       // SinFit sinFit = thisSeries.getSinFit();
       debugManager.print( "             TimeSeriesComp object has " + 
                           popupComp.count() + " TimeSeries objects" ); 
                           
       for( int k = 0; k <= (popupComp.count()-1); k++ ) {
          debugManager.print("             Looking for fits..." );          
          
          TimeSeries thisSeries = popupComp.get(k);
          debugManager.print( "               Series " + k +
                              " is of type " + thisSeries.getType() ); 
          
          if ( thisSeries.getType() == TimeSeries.SINCOSFIT ) {
             SinFit sinFit = thisSeries.getSinFit(); 
             String equation = "Fit to data sin( ) + cos( ) = ";
             textArea.append( equation + sinFit.toString() + "\n" );
          }
       }
       
       /*
        * This was in the code, but seemed to give in exception in 
        * all cases except when the TimeSeries was a SINCOSFIT. I
        * don't understand whats going on here, have I modified the
        * code so I don't need to do this anymore? Very odd!
        *
        * for( int k = 0; k < popupComp.count(); k++ ) {
        *   
        *    TimeSeries thisSeries = popupComp.get(k);
        *    if ( thisSeries.getType() == TimeSeries.SINCOSFIT ) {
        *   
        *      SinFit sinFit = thisSeries.getSinFit();
        *      
        *      String equation = "Fit to data sin( ) + cos( ) = ";
        *      textArea.append( equation + sinFit.toString() + "\n" );
        *    }              
        *
        * }
        *                     
        */
        
       // Pack now avoiding problems with the scrollbar?
       pack();

    }

   
}
