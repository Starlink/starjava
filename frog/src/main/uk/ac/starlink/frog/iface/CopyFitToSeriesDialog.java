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
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.MEMTimeSeriesImpl;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.Ephemeris;
import uk.ac.starlink.frog.iface.MetaDataPopup;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.FrogException;

/**
 * Class that displays a dialog window to allow the user to combine
 * two time series objects into a single time series object
 *
 * @since 20-MAR-2003
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeries
 */
public class CopyFitToSeriesDialog extends JInternalFrame
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
     * JComboBox to hold the First Series 
     */   
     JComboBox firstSeries = new JComboBox();
     
    /**
     * JComboBox to hold the Second Series 
     */   
     JComboBox secondSeries = new JComboBox();
        
    /**
      * Key in seriesManager for the first series
      */
      String firstKey;

    /**
      * Key in seriesManager for the second series
      */
      String secondKey;          
            
    /**
     * Frog object that this is being created from...
     */
     Frog frame = null;
        
    /**
     * Newly constructed (combined) TimeSeries object
     */
     TimeSeries combinedSeries = null;
    
    /**
     * Create an instance of the dialog and proceed to combine the time series
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     */
    public CopyFitToSeriesDialog( )
    {
        super( "Copy Fit to Time Series", false, true, false, false );
            
        //enableEvents( AWTEvent.WINDOW_EVENT_MASK ); 
        try {
            // Build the User Interface
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
       frame = seriesManager.getFrog();
             
       // grab location and position window
       Dimension combSize = getSize();
       Dimension frameSize = frame.getSize();
       setLocation( ( frameSize.width - combSize.width ) / 2,
                     ( frameSize.height - combSize.height ) / 2  );
                      
       // create the three main panels
       JPanel mainPanel = new JPanel( new GridLayout( 2, 2 ) );
       JPanel buttonPanel = new JPanel( new BorderLayout() );
       JPanel buttons = new JPanel( new BorderLayout() );
       
       // create the left panel
       JLabel firstLabel = new JLabel( "<html>&nbsp;First Series&nbsp<html>" );
       firstLabel.setBorder( BorderFactory.createEtchedBorder() );
       
       JLabel secondLabel = new JLabel("<html>&nbsp;Second Series&nbsp;<html>");
       secondLabel.setBorder( BorderFactory.createEtchedBorder() );
       
       // grab the list of TimeSeriesComp objects from the manager
       Object [] timeSeriesList = seriesManager.getSeriesKeys();
       Arrays.sort(timeSeriesList);
       
       // add series to combo boxes
       for ( int i=0; i < timeSeriesList.length; i++ ) {

          TimeSeriesComp seriesComp = seriesManager.getSeries( 
                                                  (String)timeSeriesList[i] );
          boolean fitFlag = false;
          for( int k = 0; k <= (seriesComp.count()-1); k++ ) {
              debugManager.print("             Looking for fits..." );
              TimeSeries thisSeries = seriesComp.get(k);
              if ( thisSeries.getType() == TimeSeries.SINCOSFIT ) {
                 fitFlag = true;
                 break;
              }   
          }
          if ( fitFlag ) {
             firstSeries.addItem( (String)timeSeriesList[i] );
          }   
          secondSeries.addItem( (String)timeSeriesList[i] );
       }     
       
       // set it so that by default different time series are selected
       firstSeries.setSelectedIndex(0);  
       secondSeries.setSelectedIndex(1);  
       
       // create the main panel
         
       mainPanel.add( firstLabel );
       mainPanel.add( firstSeries );
       mainPanel.add( secondLabel );
       mainPanel.add( secondSeries );       
       
       // create the button panel
       JLabel buttonLabel = new JLabel ( "          " );
       JButton okButton = new JButton( "Ok" );
       okButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "  Combining series..." );
              
              boolean status = doCopy( );
              
              // If we have bad status recursively call ourselves 
              // and get better numbers.
              if ( status != true ) {
                 
                 // dispose of the current incarnation
                 dispose();
                 
                 // create a new version
                 debugManager.print(  "    Respawning the dialog..." );
                 CombineSeriesDialog comb = new CombineSeriesDialog( );
                 seriesManager.getFrog().getDesktop().add(comb);
                 comb.show();     
              }  
              
              // we must have sucessfully folded the series, or at least
              // handled all the errors, or we won't be here. Get rid of
              // the dialog here because currently its only hidden.
              dispose();   
           }
        }); 
               
       JButton cancelButton = new JButton( "Cancel" );
       cancelButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              dispose();
           }
        }); 
        
       buttons.add( okButton, BorderLayout.CENTER );
       buttons.add( cancelButton, BorderLayout.EAST );
 
       buttonPanel.add( buttonLabel, BorderLayout.CENTER );
       buttonPanel.add( buttons, BorderLayout.EAST );
    
       // display everything
       JPanel contentPane = (JPanel) this.getContentPane();
       contentPane.setLayout( new BorderLayout() );
       
       contentPane.add(mainPanel, BorderLayout.CENTER );
       contentPane.add(buttonPanel, BorderLayout.SOUTH );
       
       pack();

    }

    /**
     * Method that actualy does the work to copy the fit to the TimeSeries 
     * and creates a new TimeSeriesComp and associated PlotControlFrame.
     *
     * @param frame PlotControlFrame containing the TimeSeries which
     *              we want to fold around the ephemeris.
     */
     protected boolean doCopy( )
     {
         debugManager.print( "    Calling doCopy()..." );

         // Grab selected series values
         firstKey = (String)firstSeries.getSelectedItem();
         secondKey = (String)secondSeries.getSelectedItem();
         
         // check we're not trying to copy the fit to the same series
         if ( firstKey == secondKey ) {
            debugManager.print( "    Series identical..." );
            return false;
         }   
         
         // check both series are defined
         if ( firstKey == null || secondKey == null ) {
            debugManager.print( "    One of the series is undefined..." );
            return false;
         }   
         
         // We have valid entries, at least in theory
         debugManager.print( "      1st Series = " + firstKey );
         debugManager.print( "      2nd Series = " + secondKey );
        
         // Hide the dialog, we'll dispose of it later...
         hide();
             
         // Call the folding algorithim
         debugManager.print( "      Calling threadCopyFit()..." ); 
         threadCopyFit();
          
         // doCopy() seems to have completed sucessfully.                  
         return true;
    }
    
    /**
     * Copy the fit inside its own thread so we don't block the 
     * GUI or event threads. Called from doCopy().
     */
    protected void threadCopyFit()
    {
        debugManager.print( "        threadCopyFit()" );
        setWaitCursor();

        //  Now create the thread that reads the spectra.
        Thread loadThread = new Thread( "Copying fit to series" ) {
                    public void run() {
                        try {
                            debugManager.print("        Spawned thread...");
                            copyFitToSeries();
     
                        }
                        catch (Exception error) {
                            error.printStackTrace();
                        }
                        finally {

                            //  Always tidy up and rewaken interface
                            //  when complete (including if an error
                            //  is thrown).
                            resetWaitCursor();
                        }
                    }
         };

         //  Start loading the time series files.
         loadThread.start();
    }

       
    /**
     * The method that carried out the required copy on the TimeSeries
     * data. Called as a new thread from threadCopyFit().
     */ 
    protected void copyFitToSeries() 
    {
         debugManager.print( "          copyFitToSeries()" );
         
         // Grab the two TimeSeriesComp using the keys provided by the user
         TimeSeriesComp firstComp = seriesManager.getSeries( firstKey );
         TimeSeriesComp secondComp = seriesManager.getSeries( secondKey );
         
         TimeSeries first = firstComp.get(firstComp.getCurrentSeries());
         TimeSeries second = secondComp.get(secondComp.getCurrentSeries());

         // Copy the fit from the first to the second
         for ( int k=0; k < firstComp.count(); k++ ) {
             
           debugManager.print( "            Series " + k + " of " +
                               firstComp.count() ); 
                 
           TimeSeries thisSeries = firstComp.get(k);
           debugManager.print( "                 Series " + k +
                               " is of type " + thisSeries.getType() ); 
                               
           if ( thisSeries.getType() == TimeSeries.SINCOSFIT ) {
              secondComp.add( thisSeries );
              
              debugManager.print( "                 Updating plot..." );
              seriesManager.getFrame(secondComp).getPlot().updatePlot();
            
              debugManager.print( "                 Updating menus..." );
              debugManager.getFrog().toggleMenuItemsOnSeriesUpdate();
             
              // display some meta data
              if ( seriesManager.getAuto()) {
                MetaDataPopup meta = new MetaDataPopup(
                                      seriesManager.getFrame(secondComp) );
                JDesktopPane desktop = debugManager.getFrog().getDesktop();
                desktop.add(meta);
                meta.show();          
                debugManager.print("             Displaying popup..." );        
              }
              break;
           }  
         }
         
    }
    
  
    /**
     * Set the main cursor to indicate waiting for some action to
     * complete and lock the interface by trapping all mouse events.
     */
    protected void setWaitCursor()
    {
        //debugManager.print("        setWaitCursor()");
        Cursor wait = Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR );
        Component glassPane = seriesManager.getFrog().getGlassPane();
        glassPane.setCursor( wait );
        glassPane.setVisible( true );
        glassPane.addMouseListener( new MouseAdapter() {} );
    }

    /**
     * Undo the action of the setWaitCursor method.
     */
    protected void resetWaitCursor()
    {
        //debugManager.print("        resetWaitCursor()");
        seriesManager.getFrog().getGlassPane().setCursor( null );
        seriesManager.getFrog().getGlassPane().setVisible( false );
    }
   
}
