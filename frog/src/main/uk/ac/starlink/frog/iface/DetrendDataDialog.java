package uk.ac.starlink.frog.iface;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.TreeMap;
import java.util.ArrayList;
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
import uk.ac.starlink.frog.fit.LeastSquaresFitPoly;

/**
 * Class that displays a dialog window to allow the user to detrend
 * a TimeSeries object. There are two options, the user may either
 * subtract the mean and divide by the stnadard deviation, or subtract
 * a low order polynomial.
 *
 * @since 09-FEB-2003
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeries
 */
public class DetrendDataDialog extends JInternalFrame
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
     * JComboBox to hold the type of detrends available list
     */   
     JComboBox whichRemove = new JComboBox();
  
    /**
      * What type of detrend are we doing?
      */
      String typeRemove;    
       
    /**
     * TextEntryField for the order of the polynomial (if needed)
     */   
     JTextField orderEntry = new JTextField();

    /**
     * Label for the order of the polynomial
     */
     JLabel orderLabel = null;
    
    /**
     * order of detrend polyynomial
     */
     int order;
    
    /**
     * PlotControlFrame object containing the TimeSeries of interest.
     */
     PlotControlFrame frame = null;
        
    /**
     * Newly constructed (detrended) TimeSeries object
     */
     TimeSeries newSeries = null;
     
    /**
     * TimeSeries object constaining the polynomial fit (if done)
     */
     TimeSeries fitSeries = null;
        
    /**
     * Create an instance of the dialog and proceed to detrend the time series
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     */
    public DetrendDataDialog( PlotControlFrame f )
    {
        super( "Detrend Time Series", false, true, false, false );

        frame = f;           // grab the interesting frame
            
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

       // grab location
       Dimension detSize = getSize();
       Dimension frameSize = frame.getSize();
       Point location = frame.getLocation();
       setLocation( ( frameSize.width - detSize.width ) / 2 + location.x,
                     ( frameSize.height - detSize.height ) / 2 + location.y );
                      
       // create the main panel
       JLabel removeLabel = new JLabel( "<html>&nbsp;Subtract&nbsp;<html>" );
       removeLabel.setBorder( BorderFactory.createEtchedBorder() );
       whichRemove.addItem( "Low Order Polynomial" );
       whichRemove.addItem( "Mean Value and divide by Std. Dev." );
       
       // add an action listener
       whichRemove.addItemListener( new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
             if (e.getStateChange() == ItemEvent.SELECTED) {
           
               if( whichRemove.getSelectedIndex() == 0 ) {
                  orderLabel.setVisible( true );
                  orderEntry.setVisible( true );
               } else {
                  orderLabel.setVisible( false );
                  orderEntry.setVisible( false );              
               }
             } 

           }       

       });
       
       // polynomial order stuff
       orderLabel = new JLabel( "<html>&nbsp;Order&nbsp;<html>" );
       orderLabel.setBorder( BorderFactory.createEtchedBorder() );
       orderEntry.setColumns(14);
       orderEntry.setText("1");
       
       // By default we're subtracting the mean, disable the order stuff
       // inital. We need to re-enable it if polynomial fit is selected
 
       
       // main panel

       GridBagConstraints constraints = new GridBagConstraints();
       constraints.weightx = 1.0;
       constraints.weighty = 1.0;
       constraints.fill = GridBagConstraints.BOTH;
              
       JPanel mainPanel = new JPanel( );
       mainPanel.setLayout( new GridBagLayout() );

       constraints.gridx = 0;  
       constraints.gridy = 0; 
       mainPanel.add( removeLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 0;
       mainPanel.add( whichRemove, constraints );
       
       constraints.gridx = 0;  
       constraints.gridy = 1;         
       mainPanel.add( orderLabel, constraints );

       constraints.gridx = 1;  
       constraints.gridy = 1;  
       mainPanel.add( orderEntry, constraints );       
     
       // create the button panel
       JPanel buttonPanel = new JPanel( new BorderLayout() );
       JPanel buttons = new JPanel( new BorderLayout() );
       
       JLabel buttonLabel = new JLabel ( "          " );
       JButton okButton = new JButton( "Ok" );
       okButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "  Detrending series..." );
              
              boolean status = doDetrend( );
              
              // If we have bad status recursively call ourselves 
              // and get better numbers.
              if ( status != true ) {
                 
                 // dispose of the current incarnation
                 dispose();
                 
                 // create a new version
                 debugManager.print(  "    Respawning the dialog..." );
                 DetrendDataDialog det = new DetrendDataDialog( frame );
                 seriesManager.getFrog().getDesktop().add(det);
                 det.show();     
              }  
              
              // we must have sucessfully detrended the series, or at least
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
       
       contentPane.add(mainPanel, BorderLayout.NORTH );
       contentPane.add(buttonPanel, BorderLayout.SOUTH );
       
       pack();
       //orderLabel.setVisible( false );
       //orderEntry.setVisible( false );
    }

    /**
     * Method that actualy does the work to detrend the TimeSeries 
     * around the user provided ephemeris and creates a new
     * TimeSeriesComp and associated PlotControlFrame.
     */
     protected boolean doDetrend( )
     {
         debugManager.print( "    Calling doDetrend()..." );

         // Grab JTextField values
         typeRemove = (String)whichRemove.getSelectedItem();

         String orderString;
         if ( typeRemove == "Low Order Polynomial" ) {
            orderString = orderEntry.getText();
            try {
                order = (new Integer(orderString)).intValue();
            } catch ( Exception e ) {
                debugManager.print( "      Invalid polynomial order..." );
                dispose();      
                JOptionPane message = new JOptionPane();
                message.setLocation( this.getLocation() );
                message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid polynomial order entered",
                                        JOptionPane.ERROR_MESSAGE);
                return false;
            }         
         }
             
         // Call the folding algorithim
         debugManager.print( "      Calling threadDetrendTimeSeries()..." ); 
         threadDetrendTimeSeries();
          
         // doDetrend() seems to have completed sucessfully.                  
         return true;
    }
    
    /**
     * Detrend the time series around the user provided ephemeris inside its
     * own thread so we don't block the GUI or event threads. Called from
     * doDetrend().
     */
    protected void threadDetrendTimeSeries()
    {
        debugManager.print( "        threadDetrendTimeSeries()" );
        setWaitCursor();

        //  Now create the thread that reads the spectra.
        Thread loadThread = new Thread( "Series detrending" ) {
                    public void run() {
                        try {
                            debugManager.print("        Spawned thread...");
                            detrendSeries();
     
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
     * The method that carried out the required detrend on the TimeSeries
     * data. Called as a new thread from threadFoldTimeSeries().
     */ 
    protected void detrendSeries() 
    {
         debugManager.print( "          detrendSeries()" );
         
         // Grab (current) TimeSeries from the PlotControlFrame
         TimeSeriesComp inital = seriesManager.getSeries( frame );
         TimeSeries currentSeries = inital.get( inital.getCurrentSeries() );

         frame.getPlot().setStatusTextTwo( "Detrending: " + 
                             currentSeries.getShortName() );
                          
         // I actually get to code about four lines of algorithim, oh
         // joy, what happiness is this, science at last...           
                    
         // grab data
         double xRef[] = currentSeries.getXData();
         double yRef[] = currentSeries.getYData();
         
         // grab the error if the exist
         double errRef[] = null;
         if( currentSeries.haveYDataErrors() ) {
            errRef = currentSeries.getYDataErrors();
         }   
         
         // copy the arrays, this sucks as it double the memory requirement
         // or the application at a stroke, but we currently have only
         // references to the data held in the currentSeries object.
         double xData[] = (double[]) xRef.clone();
         double yData[] = (double[]) yRef.clone();
         double errors[] = null;
         if( currentSeries.haveYDataErrors() ) {
            errors = (double[]) errRef.clone();
         } 
          
         // verbose for debugging    
         for ( int i = 0; i < xData.length; i++ ) {
             if ( currentSeries.haveYDataErrors() ) {
                 debugManager.print( "              " + i + ": " + 
                    xData[i] + "    " + yData[i] + "    " + errors[i] );
             } else {
                 debugManager.print( "              " + i + ": " + 
                    xData[i] + "    " + yData[i]  );  
             }                         
         }  
                          
         // *********************************
         //  DO ACTUAL DETRENDING STUFF HERE 
         // *********************************
        
        
         if ( typeRemove == "Low Order Polynomial" ) {
         
            // check that we have more points than coefficients
            if ( xData.length == 0 || order > yData.length ) {
              debugManager.print(
              "          Number of points less than number of coefficients...");
              return;
            } 
            
            LeastSquaresFitPoly polyFit = 
              new LeastSquaresFitPoly( xData, yData, errors, order );
           
            debugManager.print("            Fitted " + polyFit.getEquation());
            for ( int i = 0; i < xData.length; i++ ) {
               
               // verbose for debugging
               debugManager.print( "            Yd = " + 
                     yData[i] + " Yf = " + polyFit.getValue( xData[i] ) );

               // detrend the data
               yData[i] = yData[i]-polyFit.getValue( xData[i] );
            }             

            // Build a TimeSeries object for the fit
            MEMTimeSeriesImpl fitImpl = null;
        
            frame.getPlot().setStatusTextTwo( "Registering: " + 
                   "Fit to " + currentSeries.getShortName() );           
        
            fitImpl = new MEMTimeSeriesImpl( 
                           "Detrending fit of order " + order );
         
            double yFit [] = new double[ yData.length ];
         
            for ( int i = 0; i < xData.length; i++ ) {
                yFit[i] = polyFit.getValue( xData[i] );
            }
             
            // create the Impl
            fitImpl.setData( yFit, xData );
         
            // build a real TimeSeries object 
            try {
               fitSeries = new TimeSeries( fitImpl );  
               fitSeries.setType( TimeSeries.POLYNOMIAL );
               debugManager.print("            setType( TimeSeries.PLOYNOMIAL");
            } catch ( FrogException e ) {
               debugManager.print(
                    "          FrogException creating TimeSeries...");
               e.printStackTrace();
               return;
            } 
            
            // set the plotstyle to polyline and colour to blue
            fitSeries.setPlotStyle( TimeSeries.POLYLINE );
            fitSeries.setLineColour( Color.red.getRGB() );
            fitSeries.setLineThickness( 1.5 );
            fitSeries.setLineStyle( 2.0 );
            
            // grab the inital TimeSeriesComp object
            TimeSeriesComp initalComp = frame.getPlot().getTimeSeriesComp();  
         
            // add this new fit to the TimeSeriesComp object
            initalComp.add( fitSeries );
            frame.getPlot().updatePlot();
         
         } else if ( typeRemove == "Mean Value and divide by Std. Dev." ) {
         
            // calculate the mean and the first (absolute) and second//
            // moments of deviation from the mean.
            double mean = 0.0;
            for ( int i = 0; i < yData.length; i++ ) {
               mean += yData[i];
            }  
            mean = mean/(double)yData.length;
            
            double stdDeviation = 0.0;
            double absDeviation = 0.0;
            double variance = 0.0;
            for ( int i = 0; i < yData.length; i++ ) {
                double dataMinusMean = yData[i] - mean;
                absDeviation += Math.abs(dataMinusMean);
                variance += dataMinusMean*dataMinusMean;
            } 
            
            absDeviation = absDeviation/(double)yData.length;   
            variance = variance/(double)(yData.length-1);
            
            stdDeviation = Math.sqrt(variance);
            if( stdDeviation == 0.0 ) {
              debugManager.print("          Std. Deviation is zero...");
              return;
            }   
            
            // detrend the data
            for ( int i = 0; i < yData.length; i++ ) {
               
                yData[i] = yData[i]-mean;
                yData[i] = yData[i]/stdDeviation;
                if ( currentSeries.haveYDataErrors() ) {
                   errors[i] = 
                     Math.sqrt( 
                        Math.pow(errors[i], 2.0)/ Math.pow(stdDeviation, 2.0) );
                }
            }  
            
                     
         } else {
            // Shouldn't happen
            return;
         }   

                
         // Build a TimeSeries object for the new dataset
         // ---------------------------------------------
         MEMTimeSeriesImpl memImpl = null;
        
         frame.getPlot().setStatusTextTwo( "Registering: " + 
                             currentSeries.getShortName() );  
      
         memImpl = new MEMTimeSeriesImpl(
              "Detrended " + currentSeries.getShortName());
         
         if ( currentSeries.haveYDataErrors() ) {
            memImpl.setData( yData, xData, errors );
         } else {
            memImpl.setData( yData, xData );
         }

         // Create a real TimeSeries object
         // -------------------------------
         try {
           newSeries = new TimeSeries( memImpl );  
           newSeries.setType( TimeSeries.DETRENDED );
           newSeries.setDetrend( true );
           debugManager.print("            setType( TimeSeries.DETRENDED");
         } catch ( FrogException e ) {
           debugManager.print("          FrogException creating TimeSeries...");
           e.printStackTrace();
           return;
         }
         
         // grab the origin
         if( currentSeries.getOrigin().equals( "a File" ) ||
             currentSeries.getOrigin().equals( "a SOAP message" )  ) {
              String key = seriesManager.getKey( inital );
              newSeries.setOrigin( key );
              debugManager.print("            setOrigin( " + key + ")");
         } else {
              newSeries.setOrigin( currentSeries.getOrigin() );
         }
                           
         // Load the new series into a PlotControlFrame
         debugManager.print( "            Calling threadLoadNewSeries()..." );
         threadLoadNewSeries(); 
    }
    
    /**
     * Load the time series stored in the foldedSeries object. Use a new
     * Thread so that we do not block the GUI or event threads. 
     */
    protected void threadLoadNewSeries()
    {
        debugManager.print( "              threadLoadNewSeries()" );
        
        if ( newSeries != null ) {

            setWaitCursor();

            //  Now create the thread that reads the spectra.
            Thread loadThread = new Thread( "Detrended loader" ) {
                    public void run() {
                        try {
                            debugManager.print(
                                       "                Spawned thread...");
                            addDetrendedSeries();
                            if ( seriesManager.getAuto() ) {  
                               displayMetaData();
                            }   
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
           debugManager.print( "                newSeries == null");        
           debugManager.print( "                Aborting process...");        
 
           frame.getPlot().setStatusTextTwo( 
                 "Error registering detrended data" );  
       }

    }             

    
    /**
     * Add the new folded series to the main desktop inside a new
     * PlotControlFrame
     */ 
    protected void addDetrendedSeries() 
    {
          // Debugging is slow, tedious and annoying
          debugManager.print( "                  addFoldedSeries()" );

          debugManager.print( "                    Full Name    = " + 
                          newSeries.getFullName() );
          debugManager.print( "                    Short Name   = " +  
                          newSeries.getShortName() ); 
          debugManager.print( "                    Series Type  = " +  
                          newSeries.getType() );
          debugManager.print( "                    Has Errors?  = " +  
                          newSeries.haveYDataErrors() );
          debugManager.print( "                    Draw Errors? = " +  
                          newSeries.isDrawErrorBars() );
          debugManager.print( "                    Data Points  = " +  
                          newSeries.size() );
          debugManager.print( "                    Data Format  = " +  
                          newSeries.getDataFormat() );
          debugManager.print( "                    Plot Style   = " +  
                          newSeries.getPlotStyle() );
          debugManager.print( "                    Mark Style   = " +  
                          newSeries.getMarkStyle() );
          debugManager.print( "                    Mark Size    = " +  
                          newSeries.getMarkSize() ); 
    
          // check if we're in magnitude space, grab the data limits object
          // from the orginal DivaPlot object, contained in the PlotControl
          // object, embedded in a PlotControlFrame (Eeek!) and set the
          // relevant flag in the TimeSeries object so that when its assocaited
          // with a PlotControlFrame the yFlipped flag will be toggled.
          if( frame.getPlot().getPlot().getDataLimits().isPlotInMags() ) {
              debugManager.print( "                    yFlipped     = true");
              newSeries.setYFlipped( true );
          } else {
              debugManager.print( "                    yFlipped     = false");
          }    
          
          // add the series to the MainWindow by calling addSeries() in
          // the main Frog Object, by grabbing the reference to the main
          // frog object stored in the TimeSeriesManager. This is an unGodly
          // hack and I should be shot for doing this...
          seriesManager.getFrog().addSeries( newSeries );
          frame.getPlot().setStatusTextTwo( "" ); 
   
    }

   
   /**
     * Display the meta-data popup abotu the series
     */ 
    protected void displayMetaData( ) 
    {    
        debugManager.print( "           void displayMetaData( )" );
        
        int seriesID = seriesManager.getCurrentID();
        String frameKey = "Time Series " + seriesID;
        debugManager.print("             Time Series " + seriesID );
    
        PlotControlFrame currFrame = seriesManager.getFrame( frameKey );   
         
        // display some meta data
        MetaDataPopup meta = new MetaDataPopup( currFrame );
        Frog frame = debugManager.getFrog();
        JDesktopPane desktop = frame.getDesktop();
        desktop.add(meta);
        meta.show();          
        debugManager.print("             Displaying popup..." );
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
