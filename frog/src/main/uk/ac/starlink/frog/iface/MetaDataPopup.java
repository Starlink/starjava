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
     * TextPane for information
     */   
     JTextPane textPane =  new JTextPane();
    
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
       JScrollPane scrollPane = new JScrollPane( textPane,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       scrollPane.setPreferredSize( new Dimension(500, 250) );
       scrollPane.setMinimumSize( new Dimension(500, 250) );
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
       
       doc = doc + "<h2><u>Time Series</u></h2>\n";
          
       doc = doc + "<ul>\n";
       
       // data errors
       if ( popupSeries.haveYDataErrors() ) {
          doc = doc + "<li>The data has errors";
          if ( popupSeries.isDrawErrorBars() ) {
             doc = doc + " (drawn)\n";
          } else {
             doc = doc + " (hidden)\n";
          } 
       } else {
          doc = doc + "<li>The data has no errors";
       }  
       
       // magnitudes
       if ( popupFrame.getPlot().getPlot().getDataLimits().isPlotInMags() ) {
          doc = doc + "<li>The Y data values are in magnitudes\n";
       } else {
           doc = doc + "<li>The Y data values are in counts\n";
       }
       
       // detrend
       if ( popupSeries.getDetrend() ) {
          doc = doc + "<li>The data has been detrended\n";
       }
       
       // By TYPE
       // -------
       if ( popupSeries.getType() ==  TimeSeries.UNCLASSIFIED ) {
          doc = doc + "<li>The data type is unknown</ul>\n";
  
       } else if ( popupSeries.getType() == TimeSeries.TIMESERIES ) { 
          doc = doc + "<li>The data is a standard time series</ul>\n";



       } else if ( popupSeries.getType() == TimeSeries.FOLDED || 
                   popupSeries.getType() == TimeSeries.BINFOLDED) {     
          doc = doc + "<li>The data has been folded\n";
          if ( popupSeries.getType() == TimeSeries.BINFOLDED) {
             doc = doc + "<li>The data has been binned\n";
          }

          
         debugManager.print("             Appending folding info..." );
         Ephemeris ephem = popupSeries.getEphemeris();
         
         String doc2 = "<h2><u>Ephemeris</u></h2>\n"; 
         doc2 = doc2 + "Time = " + ephem.getZeroPoint() +  " + " + 
                           ephem.getPeriod() + " &times; E\n";
          
          for( int k = 0; k <= (popupComp.count()-1); k++ ) {
             debugManager.print("             Looking for fits..." );          
          
             TimeSeries thisSeries = popupComp.get(k);
             debugManager.print( "               Series " + k +
                                 " is of type " + thisSeries.getType() ); 
          
             if ( thisSeries.getType() == TimeSeries.SINCOSFIT ) {
                SinFit sinFit = thisSeries.getSinFit(); 
                doc = doc + 
                  "<li>The series has been fitted with a sin(&nbsp;)" +
                  " + cos(&nbsp;) function\n";
                
                doc2 = doc2 + "<h2><u>Fit sin(&nbsp;) + cos(&nbsp;)</u></h2>\n";
                doc2 = doc2 + "Fitting to:<br>\n";
                doc2 = doc2 + "<em><center>" +
                  "Y = A + B*sin(2pi/period*X) + C*cos(2pi/period*X)<br>" +
                  "</center></em>\n";
                                
                doc2 = doc2 + "Results are:<br>\n";
                               
                doc2 = doc2 + "<center><table border=1>\n";
                doc2 = doc2 + "<tr><th>Parameter</th> <th>Value</th></tr>\n";
                doc2 = doc2 + 
                   "<tr><td>A</td><td>" + sinFit.getA() + "</td></tr>\n";
                doc2 = doc2 + 
                   "<tr><td>B</td><td>" + sinFit.getB() + "</td></tr>\n";
                doc2 = doc2 + 
                   "<tr><td>C</td><td>" + sinFit.getC() + "</td></tr>\n";
                doc2 = doc2 + "</table></center>\n";
            }
          }
          
          doc = doc + "</ul>\n" + doc2;

       } else if ( popupSeries.getType() == TimeSeries.DETRENDED ) { 


       } else if ( popupSeries.getType() == TimeSeries.FAKEDATA  ) { 
          doc = doc + "<li>The data is artificially generated (fake)</ul>\n";



       }
       
       // drop document into textArea
       doc = doc + "</body></html>";
       debugManager.print("             Passing HTML Document to textPane...");
       debugManager.print("\n" + doc + "\n");
       textPane.setText( doc );

/*          
       // General Information    
     
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
            

       }
        
       // Fitting information
       //
       //   thisSeries.getType() == TimeSeries.SINCOSFIT
       
       // SinFit sinFit = thisSeries.getSinFit();
       debugManager.print( "             TimeSeriesComp object has " + 
                           popupComp.count() + " TimeSeries objects" ); 
       
*/
       
      
        
       // Pack now avoiding problems with the scrollbar?
       pack();

    }
   
}
