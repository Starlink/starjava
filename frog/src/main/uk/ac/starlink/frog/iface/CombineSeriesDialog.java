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
import uk.ac.starlink.frog.data.MEMTimeSeriesImpl;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.Ephemeris;
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
public class CombineSeriesDialog extends JInternalFrame
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
    public CombineSeriesDialog( )
    {
        super( "Combine Time Series", false, true, false, false );
            
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
          firstSeries.addItem( (String)timeSeriesList[i] );
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
              
              boolean status = doCombine( );
              
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
     * Method that actualy does the work to combine the TimeSeries 
     * and creates a new TimeSeriesComp and associated PlotControlFrame.
     *
     * @param frame PlotControlFrame containing the TimeSeries which
     *              we want to fold around the ephemeris.
     */
     protected boolean doCombine( )
     {
         debugManager.print( "    Calling doCombine()..." );

         // Grab selected series values
         firstKey = (String)firstSeries.getSelectedItem();
         secondKey = (String)secondSeries.getSelectedItem();
         
         // check we're not trying to add the same series together
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
         debugManager.print( "      Calling threadCombTimeSeries()..." ); 
         threadCombTimeSeries();
          
         // doCombine() seems to have completed sucessfully.                  
         return true;
    }
    
    /**
     * Combine the time series inside its own thread so we don't block the 
     * GUI or event threads. Called from doCombine().
     */
    protected void threadCombTimeSeries()
    {
        debugManager.print( "        threadCombTimeSeries()" );
        setWaitCursor();

        //  Now create the thread that reads the spectra.
        Thread loadThread = new Thread( "Combineing series" ) {
                    public void run() {
                        try {
                            debugManager.print("        Spawned thread...");
                            combSeries();
     
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
     * The method that carried out the required combine on the TimeSeries
     * data. Called as a new thread from threadFoldTimeSeries().
     */ 
    protected void combSeries() 
    {
         debugManager.print( "          combSeries()" );
         
         // Grab the two TimeSeries using the keys provided by the user
         TimeSeriesComp firstComp = seriesManager.getSeries( firstKey );
         TimeSeriesComp secondComp = seriesManager.getSeries( secondKey );
         
         TimeSeries first = firstComp.get(firstComp.getCurrentSeries());
         TimeSeries second = secondComp.get(secondComp.getCurrentSeries());

         // grab data
         
         double xRefFirst[] = first.getXData();
         double yRefFirst[] = first.getYData();
         // grab the error if the exist
         double errRefFirst[] = null;
         if( first.haveYDataErrors() ) {
            errRefFirst = first.getYDataErrors();
         }
        
         double xRefSecond[] = second.getXData();
         double yRefSecond[] = second.getYData();
         // grab the error if the exist
         double errRefSecond[] = null;
         if( second.haveYDataErrors() ) {
            errRefSecond = second.getYDataErrors();
         }   
               
         // create the target arrays
         int newSize = xRefFirst.length + xRefSecond.length;
         debugManager.print( "first = " + xRefFirst.length );
         debugManager.print( "second = " + xRefSecond.length );
         debugManager.print( "new = " + newSize );
         
         double xData[] = new double[ newSize ];
         double yData[] = new double[ newSize ];
         double errors[] = null;
         if( first.haveYDataErrors() && second.haveYDataErrors() ) {
            errors = new double[ newSize ];
         } 
         
         // loop and copy the values
         for ( int i = 0; i < xRefFirst.length; i++ ) {
         
            debugManager.print( "i = " + i );
            
            xData[i] = xRefFirst[i];
            yData[i] = yRefFirst[i];
            if( first.haveYDataErrors() && second.haveYDataErrors() ) {
               errors[i] = errRefFirst[i];
            }
         }
         for ( int j = 0; j < xRefSecond.length; j++ ) {

            debugManager.print( "j = " + j );
        
            xData[xRefFirst.length+j] = xRefSecond[j];
            yData[xRefFirst.length+j] = yRefSecond[j];
            if( first.haveYDataErrors() && second.haveYDataErrors() ) {
               errors[xRefFirst.length+j] = errRefSecond[j];
            }
         }
         
         // verbose for debugging    
         for ( int k = 0; k < xData.length; k++ ) {
             if ( errors != null ) {
                 debugManager.print( "              " + k + ": " + 
                    xData[k] + "    " + yData[k] + "    " + errors[k] );
             } else {
                 debugManager.print( "              " + k + ": " + 
                    xData[k] + "    " + yData[k]  );  
             }                         
         } 
                   
         // Create a new TimeSeriesComp object
         MEMTimeSeriesImpl memImpl = null;
         
         // Create a MEMTimeSeriesImpl  
         memImpl = new MEMTimeSeriesImpl( firstKey + " and " + secondKey );
         
         // Add data to the MEMTimeSeriesImpl 
         if ( first.haveYDataErrors() && second.haveYDataErrors() ) {
             memImpl.setData( yData, xData, errors );
         } else {
             memImpl.setData( yData, xData );
         }
          
         // create a real timeseries
         try {
           combinedSeries = new TimeSeries( memImpl );  
           combinedSeries.setType( TimeSeries.TIMESERIES );
         } catch ( FrogException e ) {
           debugManager.print("          FrogException creating TimeSeries...");
           e.printStackTrace();
           return;
         }        
          
         // Load the new series into a PlotControlFrame
         debugManager.print( "            Calling threadLoadNewSeries()..." );
         threadLoadNewSeries(); 
    }
    
    /**
     * Load the time series stored in the combinedSeries object. Use a new
     * Thread so that we do not block the GUI or event threads. 
     */
    protected void threadLoadNewSeries()
    {
        debugManager.print( "              threadLoadNewSeries()" );
        
        if ( combinedSeries != null ) {

            setWaitCursor();

            //  Now create the thread that reads the spectra.
            Thread loadThread = new Thread( "Combiend loader" ) {
                    public void run() {
                        try {
                            debugManager.print(
                                       "                Spawned thread...");
                            addCombinedSeries();
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
        } else {
        
           // Something has gone very wrong, oh dear!
           debugManager.print( "                combinedSeries == null");   
           debugManager.print( "                Aborting process...");        
 
       }

    }             

    
    /**
     * Add the new combinde series to the main desktop inside a new
     * PlotControlFrame
     */ 
    protected void addCombinedSeries() 
    {
          // Debugging is slow, tedious and annoying
          debugManager.print( "                  addCombinedSeries()" );

          debugManager.print( "                    Full Name    = " + 
                          combinedSeries.getFullName() );
          debugManager.print( "                    Short Name   = " +  
                          combinedSeries.getShortName() ); 
          debugManager.print( "                    Series Type  = " +  
                          combinedSeries.getType() );
          debugManager.print( "                    Has Errors?  = " +  
                          combinedSeries.haveYDataErrors() );
          debugManager.print( "                    Draw Errors? = " +  
                          combinedSeries.isDrawErrorBars() );
          debugManager.print( "                    Data Points  = " +  
                          combinedSeries.size() );
          debugManager.print( "                    Data Format  = " +  
                          combinedSeries.getDataFormat() );
          debugManager.print( "                    Plot Style   = " +  
                          combinedSeries.getPlotStyle() );
          debugManager.print( "                    Mark Style   = " +  
                          combinedSeries.getMarkStyle() );
          debugManager.print( "                    Mark Size    = " +  
                          combinedSeries.getMarkSize() ); 
    
          // check if we're in magnitude space, grab the data limits object
          // from the orginal DivaPlot object, contained in the PlotControl
          // object, embedded in a PlotControlFrame (Eeek!) and set the
          // relevant flag in the TimeSeries object so that when its associated
          // with a PlotControlFrame the yFlipped flag will be toggled.
          
          PlotControlFrame firstFrame = seriesManager.getFrame( firstKey );
          PlotControlFrame secondFrame = seriesManager.getFrame( secondKey );
          
          if( firstFrame.getPlot().getPlot().getDataLimits().isPlotInMags() &&
            secondFrame.getPlot().getPlot().getDataLimits().isPlotInMags()) {
            
             debugManager.print( "                    yFlipped     = true");
             combinedSeries.setYFlipped( true );
          } else {
             debugManager.print( "                    yFlipped     = false");
          }    
          
          // add the series to the MainWindow by calling addSeries() in
          // the main Frog Object, by grabbing the reference to the main
          // frog object stored in the TimeSeriesManager. This is an unGodly
          // hack and I should be shot for doing this...
          seriesManager.getFrog().addSeries( combinedSeries );
   
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
