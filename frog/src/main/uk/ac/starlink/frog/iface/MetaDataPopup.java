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
       Frog frame = seriesManager.getFrog();
             
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
       
       // Frame Name
       JLabel seriesLabel = new JLabel( "<html>&nbsp;Time Series&nbsp<html>" );
       seriesLabel.setBorder( BorderFactory.createEtchedBorder() );
       
       JLabel seriesValue = new JLabel( "<html>&nbsp;" + 
                       seriesManager.getKey(popupFrame)  +"&nbsp<html>" );
       seriesValue.setBorder( BorderFactory.createEtchedBorder() );
          
          
       // Series Name        
       JLabel nameLabel = new JLabel( "<html>&nbsp;Name&nbsp<html>" );
       nameLabel.setBorder( BorderFactory.createEtchedBorder() );
       
       JLabel nameValue = new JLabel( "<html>&nbsp;" + 
                       popupSeries.getFullName()  +"&nbsp<html>" );
       nameValue.setBorder( BorderFactory.createEtchedBorder() );               
               
       // if folded
       JLabel ephemLabel = null;
       JLabel ephemValue = null;
       
       if( popupSeries.getType() == TimeSeries.FOLDED ||
           popupSeries.getType() == TimeSeries.BINFOLDED ) {
              Ephemeris ephem = popupSeries.getEphemeris();
              ephemLabel = new JLabel( "<html>&nbsp;Ephemeris&nbsp<html>" );
              ephemLabel.setBorder( BorderFactory.createEtchedBorder() );
              ephemValue = new JLabel( "<html>&nbsp;" + ephem.getZeroPoint() + 
                " + " + ephem.getPeriod() + " x E&nbsp<html>" );
              ephemValue.setBorder( BorderFactory.createEtchedBorder() ); 
       }
       
       // if fitted
       JLabel fitLabel = null;
       JLabel fitValue = null;
       boolean fitted = false;
       
       for( int k = 0; k < popupComp.count(); k++ ) {
       
          if ( popupComp.get(k).getType() == TimeSeries.SINCOSFIT ) {
          
             SinFit sinFit = popupComp.get(k).getSinFit();
             
             fitLabel = 
                 new JLabel( 
                    "<html>&nbsp;sin(&nbsp;) + cos(&nbsp;) fit&nbsp<html>" );
             fitLabel.setBorder( BorderFactory.createEtchedBorder() );
             
             fitValue = new JLabel( 
               "<html>&nbsp;" + sinFit.toString() +"&nbsp<html>" );
             fitValue.setBorder( BorderFactory.createEtchedBorder() ); 
          
             // flag it
             fitted = true;
             
          }              
       
       }                     

       // Stuff them into the main panel
       // ------------------------------

       GridBagConstraints constraints = new GridBagConstraints();
       constraints.weightx = 1.0;
       constraints.weighty = 1.0;
       constraints.fill = GridBagConstraints.BOTH;
       
       JPanel mainPanel = new JPanel();
           
       // create the main panel
       mainPanel.setLayout( new GridBagLayout() );
       
       // frame name
       constraints.gridx = 0;  
       constraints.gridy = 0;  
       mainPanel.add( seriesLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 0;        
       mainPanel.add( seriesValue, constraints );
       
       // series name
       constraints.gridx = 0;  
       constraints.gridy = 1;  
       mainPanel.add( nameLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 1;        
       mainPanel.add( nameValue, constraints );  

       // if folded       
       if( popupSeries.getType() == TimeSeries.FOLDED ||
           popupSeries.getType() == TimeSeries.BINFOLDED ) { 

          constraints.gridx = 0;  
          constraints.gridy = 2;  
          mainPanel.add( ephemLabel, constraints );
       
          constraints.gridx = 1;  
          constraints.gridy = 2;        
          mainPanel.add( ephemValue, constraints );  
  
       }
       
       // if fitted      
       if( fitted ) { 

          constraints.gridx = 0;  
          constraints.gridy = 3;  
          mainPanel.add( fitLabel, constraints );
       
          constraints.gridx = 1;  
          constraints.gridy = 3;        
          mainPanel.add( fitValue, constraints );  
  
       }      
                      
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
       
       pack();

    }

   
}
