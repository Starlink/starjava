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
import uk.ac.starlink.frog.iface.MetaDataPopup;
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.MEMTimeSeriesImpl;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.Ephemeris;
import uk.ac.starlink.frog.data.SinFit;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.fit.LeastSquaresFitSin;


/**
 * Class that displays a dialog window to allow the user to input an
 * ephemeris, around which a TimeSeries object can be folded.
 *
 * @since 09-FEB-2003
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeries
 */
public class FoldSeriesDialog extends JInternalFrame
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
     * TextEntryField for the zero point
     */   
     JTextField zeroEntry = new JTextField();
     
    /**
     * TextEntry field for the period
     */ 
     JTextField periodEntry = new JTextField();     
         
    /**
     * TextEntry field for the number of phase bins used in the fold.
     */ 
     JTextField binEntry = new JTextField();     
    
    /**
     * Zero Point
     */
     double zeroPoint;
         
    /**
     * Period
     */
     double period;
 
    /**
     * Are we binning the data?
     */ 
     boolean binData;
     
 
    /**
     * Are we fitting a + b*sin() + c*cos()
     */ 
     boolean fitData;     
                  
    /**
     * Number of phase bins
     */
     int phaseBins;
     
    /**
     * Array to put the binned phase value
     */ 
     double[] binnedPhase = null;
     
    /**
     * Array to put the binned data value
     */      
     double[] binnedYData = null;
     
    /**
     * Array to put the binned error estimate
     */     
     double[] binnedError = null;
     
    /**
     * Phase value
     */ 
     double[] phase = null;
     
    /**
     * Raw y data
     */      
     double[] yData = null;
     
    /**
     * Raw y errors
     */     
     double[] errors = null;   
      
    /**
     * PlotControlFrame object containing the TimeSeries of interest.
     */
     PlotControlFrame frame = null;
        
    /**
     * Newly constructed (folded) TimeSeries object
     */
     TimeSeries foldedSeries = null;
        
    /**
     * Fit to the new TimeSeries object
     */
     TimeSeries fittedSeries = null;   
       
    /**
     * Ephemeris object, TimeSeries will be folded around this ephemeris
     */
     Ephemeris ephem = null; 
     
        
    /**
     * Create an instance of the dialog and proceed to fold the time series
     * around the user provided ephemeris. The data will not be binned.
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     */
    public FoldSeriesDialog( PlotControlFrame f )
    {
        // call the other constructor
        this( f, false, false );
    }
    
    /**
     * Create an instance of the dialog and proceed to fold the time series
     * around the user provided ephemeris. 
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     * @param b Bool, if true we'll bin the data into a user set number of bins.
     */
    public FoldSeriesDialog( PlotControlFrame f, boolean b )
    {
        // call the other constructor
        this( f, b, false );
    }     
     
    /**
     * Create an instance of the dialog and proceed to fold the time series
     * around the user provided ephemeris 
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     * @param b Bool, if true we'll bin the data into a user set number of bins.
     * @param s Bool, if true then we want to fit a sin() + cos() function
     */
    public FoldSeriesDialog( PlotControlFrame f, boolean b, boolean s)
    {
        super( "Fold Time Series", false, true, false, false );

        frame = f;           // grab the interesting frame
        binData = b;         // we want to bin the data up
        fitData = s;         // we want to fit the data
     
        try {
            // Build the User Interface
            initUI( );
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }
    
    /**
     * Create an instance of the dialog and proceed to fold the time series
     * around the user provided ephemeris 
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     * @param b Bool, if true we'll bin the data into a user set number of bins.
     * @param s Bool, if true then we want to fit a sin() + cos() function
     * @param z Default value for the zero point
     * @param p Default value for the period
     * @param n Default value for the number of phase bins
     */
    public FoldSeriesDialog( PlotControlFrame f, boolean b, boolean s, 
                             double z, double p, int n )
    {
        super( "Fold Time Series", false, true, false, false );

        frame = f;           // grab the interesting frame
        binData = b;         // we want to bin the data up
        fitData = s;         // we want to fit the data
        zeroPoint = z;       
        period = p;
        phaseBins = n;
        
        zeroEntry.setText( new Double(zeroPoint).toString() );
        periodEntry.setText( new Double(period).toString() );
        binEntry.setText( new Integer(phaseBins).toString() );
       
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

       // set size of entry widgets
       zeroEntry.setColumns(20);
       periodEntry.setColumns(20);
       binEntry.setColumns(20);    
               
       // grab location
       Dimension foldSize = getSize();
       Dimension frameSize = frame.getSize();
       Point location = frame.getLocation();
       setLocation( ( frameSize.width - foldSize.width ) / 2 + location.x,
                     ( frameSize.height - foldSize.height ) / 2 + location.y );
                      
       // create the three main panels
       JPanel leftPanel = new JPanel( new BorderLayout() );
       JPanel rightPanel = new JPanel( new BorderLayout() );
       JPanel buttonPanel = new JPanel( new BorderLayout() );
       JPanel buttons = new JPanel( new BorderLayout() );
       
       // create the left panel
       JLabel zeroLabel = new JLabel( "<html>&nbsp;Zero Point&nbsp;<html>" );
       JLabel periodLabel = new JLabel( "<html>&nbsp;Period<html>" );
       JLabel binLabel = new JLabel( "<html>&nbsp;Number of bins&nbsp;<html>");
       zeroLabel.setBorder( BorderFactory.createEtchedBorder() );
       periodLabel.setBorder( BorderFactory.createEtchedBorder() );
       binLabel.setBorder( BorderFactory.createEtchedBorder() );
       leftPanel.add( zeroLabel, BorderLayout.NORTH );
       leftPanel.add( periodLabel, BorderLayout.CENTER );
       if( binData ) {
          leftPanel.add( binLabel, BorderLayout.SOUTH );
       }
       
       // create the right panel
       rightPanel.add( zeroEntry, BorderLayout.NORTH );
       rightPanel.add( periodEntry, BorderLayout.CENTER );       
       if( binData ) {
          rightPanel.add( binEntry, BorderLayout.SOUTH );       
       }
       
       // create the button panel
       JLabel buttonLabel = new JLabel ( "          " );
       JButton okButton = new JButton( "Ok" );
       okButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "  Folding series..." );
              
              boolean status = doFold( );
              //debugManager.print( "    doFold() returned: " + status );
              
              // If we have bad status recursively call ourselves 
              // and get better numbers.
              if ( status != true ) {
                 
                 // dispose of the current incarnation
                 dispose();
                 
                 // create a new version
                 debugManager.print(  "    Respawning the dialog..." );
                 FoldSeriesDialog fold = new FoldSeriesDialog( frame, binData);
                 seriesManager.getFrog().getDesktop().add(fold);
                 fold.show();     
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
       
       contentPane.add(leftPanel, BorderLayout.WEST );
       contentPane.add(rightPanel, BorderLayout.CENTER );
       contentPane.add(buttonPanel, BorderLayout.SOUTH );
       
       pack();

    }

    /**
     * Method that actualy does the work to fold the TimeSeries 
     * around the user provided ephemeris and creates a new
     * TimeSeriesComp and associated PlotControlFrame.
     */
     protected boolean doFold( )
     {
         debugManager.print( "    Calling doFold()..." );

         // Grab JTextField values
         String zeroPointString = zeroEntry.getText();
         String periodString = periodEntry.getText();
         String binString = binEntry.getText();
         
         // Define these otherwise Java complains, depsite the fact 
         // we know the debug statements won't be reached unless they
         // actually have valid values (we hope!)
         zeroPoint = 0.0;
         period = 0.0;
         phaseBins = 0;
         
         // Convert to primitive types      
         try {
             zeroPoint = (new Double(zeroPointString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid zero point..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid zero point entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }

         try {
             period = (new Double(periodString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid period..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid period entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }
         
         if( binData ) {
            try {
                phaseBins = (new Integer(binString)).intValue();
            } catch ( Exception e ) {
                debugManager.print( "      Invalid number of phase bins..." );
                dispose();      
                JOptionPane message = new JOptionPane();
                message.setLocation( this.getLocation() );
                message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid number of phase bins entered",
                                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
         }
         
         // We have valid entries, at least in theory
         debugManager.print( "      Zero Point = " + 
                        zeroPointString + "( " + zeroPoint + " )" );
         debugManager.print( "      Period     = " + 
                        periodString + "( " + period + " )" );
                        
         // Only if we're binning the data               
         if( binData ) {               
            debugManager.print( "      Phase bins = " + 
                           binString + "( " + phaseBins + " )" );
         }
         
         // Hide the dialog, we'll dispose of it later...
         hide();
         
         // Create an ephemeris object
         ephem = new Ephemeris();
         ephem.setPeriod( period );
         ephem.setZeroPoint( zeroPoint );               
             
         // Call the folding algorithim
         debugManager.print( "      Calling threadFoldTimeSeries()..." ); 
         threadFoldTimeSeries();
          
         // doFold() seems to have completed sucessfully.                  
         return true;
    }
    
    /**
     * Fold the time series around the user provided ephemeris inside its
     * own thread so we don't block the GUI or event threads. Called from
     * doFold().
     */
    protected void threadFoldTimeSeries()
    {
        debugManager.print( "        threadFoldTimeSeries()" );
        setWaitCursor();

        //  Now create the thread that reads the spectra.
        Thread loadThread = new Thread( "Series folding" ) {
                    public void run() {
                        try {
                            debugManager.print("        Spawned thread...");
                            foldSeries();
     
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
     * The method that carried out the required fold on the TimeSeries
     * data. Called as a new thread from threadFoldTimeSeries().
     */ 
    protected void foldSeries() 
    {
         debugManager.print( "          foldSeries()" );
         
         // Grab (current) TimeSeries from the PlotControlFrame
         TimeSeriesComp inital = seriesManager.getSeries( frame );
         TimeSeries currentSeries = inital.get( inital.getCurrentSeries() );

         frame.getPlot().setStatusTextTwo( "Folding: " + 
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
         yData = (double[]) yRef.clone();

         if( currentSeries.haveYDataErrors() ) {
            errors = (double[]) errRef.clone();
         }   
          
         // verbose for debugging    
         for ( int i = 0; i < xData.length; i++ ) {
             if ( errors != null ) {
                 debugManager.print( "              " + i + ": " + 
                    xData[i] + "    " + yData[i] + "    " + errors[i] );
             } else {
                 debugManager.print( "              " + i + ": " + 
                    xData[i] + "    " + yData[i]  );  
             }                         
         }  
                          
         // define new xData array to hold folded x values
         phase = new double[xData.length];
               
         // Loop round the data points
         for ( int i = 0; i < xData.length; i++ ) {
                phase[i] = ( xData[i] - zeroPoint )/period;  
                phase[i] = phase[i] - (int)phase[i];
                while( phase[i] < 0.0 ) {
                   phase[i] = phase[i] + 1.0;
                }   
         } 
         
         //------------------------------------------------------------------
         // This is a _serious_ kludge there has to be a better way to do it!
         // -----------------------------------------------------------------
         
         // sort the yData and errors using phase as the keys. This is a 
         // pretty sucky thing to actually have to do, but AST is going to
         // seriously object if we don't.
         //
         // Since we need to maintain the mapping between the phase and data
         // values, we have to use a TreeMap rather than doing a straght
         // numerical sort. This is not good for overheads.
         
         // create the TreeMap(s)
         TreeMap sortData = new TreeMap();
         TreeMap sortError = new TreeMap();
         for ( int i = 0; i < xData.length; i++ ) {
            sortData.put( new Double(phase[i]), new Double(yData[i]) );
            if ( currentSeries.haveYDataErrors() ) {
               sortError.put( new Double(phase[i]), new Double(errors[i]) );
            }
         }
         
         // grab the newly sorted phase, yData and error arrays
         Object [] phaseObj = sortData.keySet().toArray();
         Object [] yDataObj = sortData.values().toArray();
         Object [] errorsObj = null;
         if ( currentSeries.haveYDataErrors() ) {
            errorsObj = sortError.values().toArray();
         }
         
         // Convert them back to primitives...
         // I think we have to do it this way? Surely not?
         for ( int i = 0; i < phaseObj.length; i++ ) {
         
            phase[i] = ((Double)phaseObj[i]).doubleValue();
            yData[i] = ((Double)yDataObj[i]).doubleValue();
            if ( currentSeries.haveYDataErrors() ) {
               errors[i] = ((Double)errorsObj[i]).doubleValue();
            }

            // debugging
            if ( currentSeries.haveYDataErrors() ) {
                 debugManager.print( "              " + i + ": " + 
                    phase[i] + "    " + yData[i] + "    " + errors[i] );
            } else {
                 debugManager.print( "              " + i + ": " + 
                    phase[i] + "    " + yData[i]  );  
            }             
            
         }
         
         //------------------------------------------------------------------
         
         MEMTimeSeriesImpl memImpl = null;
         if( binData ) {
           // We're binning the data into "phaseBins" number of bins,
           // so we're not finished yet.
           
           debugManager.print("              Binning Data..." );

           // allocate the binned arrays
           binnedPhase = new double [phaseBins];
           binnedYData = new double [phaseBins];
           binnedError = new double [phaseBins];
  
           // create a local array to count the number of points in a bin
           int[] count = new int[phaseBins];
  
           // Loop round the data points
           for ( int i = 0; i < xData.length; i++ ) { 
           
                // Work out the bin number NINT()
                int binNum = (int)( phase[i] * (double)phaseBins );
                
                debugManager.print( 
                   "                " + i + ": assigned to bin " + binNum );
                   
                // bin up the phase and data values
                binnedPhase[binNum] =  binnedPhase[binNum]+ phase[i];
                binnedYData[binNum] =  binnedYData[binNum]+ yData[i];
               
                // do the errors
                if ( currentSeries.haveYDataErrors() ) {
                   binnedError[binNum] =  binnedError[binNum]+ 
                                          Math.pow( errors[i], 2.0 );
                } 
                
                count[binNum] = count[binNum] + 1;                         
           }
           
           // Loop round the bins, work out mean phase and data value
           for ( int j =  0; j < phaseBins; j++ ) {
           
                // check we have points in that bin, allocate the BAD
                // value to that bin if there aren't any points in there
                if( count[j] > 0 ) {
                   binnedPhase[j] = binnedPhase[j]/( (double)count[j] );
                   binnedYData[j] = binnedYData[j]/( (double)count[j] );
                } else {
                   binnedPhase[j] = ((double)j/phaseBins); 
                   binnedYData[j] = TimeSeries.BAD;
                }
           }
           
           // Error estimates are slightly more complex
           // -----------------------------------------
           
           // if we have errors we want and error estimate from them
           if ( currentSeries.haveYDataErrors() ) {
 
              // work out the final error estimate
              for ( int m = 0; m < phaseBins; m++ ) {
                 if( count[m] == 1 ) {
                    binnedError[m] = Math.sqrt(binnedError[m]);
                 } else if ( count[m] == 0 ) {
                    binnedError[m] = TimeSeries.BAD;
                 } else {
                    binnedError[m] = Math.sqrt(binnedError[m])/(double)count[m];
                 }
              }          
           
           // otherwise we we'll take possion noise 
           } else {
              double sum[] = new double[phaseBins];
              for ( int m = 0; m < phaseBins; m++ ) {
                 for ( int n = 0; n < xData.length; n++ ) {
                 
                    if( count[m] > 0 ) {
                       int binNum = (int)(phase[n]*(double)phaseBins);
                       if( binNum == m ) {
                          sum[m] = sum[m] + 
                                  Math.pow( (yData[n]-binnedYData[m]), 2.0 );
                       }
                     }             
                 }
                 
                 // work out the final error estimate
                 if( count[m] == 1 ) {
                    binnedError[m] = 0.0;
                 } else if ( count[m] == 0 ) {
                    binnedError[m] = TimeSeries.BAD;
                 } else {
                    binnedError[m] = Math.sqrt( 
                      (1.0/(count[m]*(count[m]-1.0)))*sum[m]);
                 }          
              }               
           
           }

/* ------------------------------------------------------------------------
           
           // Get rid of TimeSeries.BAD data points in the fold
           // -------------------------------------------------
           //
           // This isn't necessary as the Grf object drops them
           // from the plot, maybe I should add this as an option
           // during the save file routines?
           //
           //  1. Transit the primitive arrays to ArrayList objects
           //  2. Run through the ArrayList object and check to BAD data
           //  3. Remove data point from all three ArrayList objects
           //  4. Move the data back into primitive arrays

           ArrayList listData = new ArrayList();
           ArrayList listPhase = new ArrayList();
           ArrayList listError = new ArrayList();
           
           for( int j = 0; j < binnedPhase.length; j++ ) {
              listData.add( j, new Double( binnedYData[j] ) );
              listPhase.add( j, new Double( binnedPhase[j] ) );
              listError.add( j, new Double( binnedError[j] ) );
           }
           
           for( int i = listPhase.size()-1; i >= 0; i-- ) {
              
               if( ((Double)listData.get( i )).doubleValue() 
                    == TimeSeries.BAD ) {
                    
                   debugManager.print(
                        "              Removing data point " + i + " as bad");
                   listData.remove( i );
                   listPhase.remove( i );
                   listError.remove( i );
                }
           }
           
           // annul the old arrays and create some new ones
           binnedYData = new double[listData.size()];
           binnedPhase = new double[listData.size()];
           binnedError = new double[listData.size()];
           
           for ( int k = 0; k < listData.size(); k++ ) {
         
               binnedYData[k] = ((Double)listData.get(k)).doubleValue();
               binnedPhase[k] = ((Double)listPhase.get(k)).doubleValue();
               binnedError[k] = ((Double)listError.get(k)).doubleValue();
           }
           
------------------------------------------------------------------------ */
 
           frame.getPlot().setStatusTextTwo( "Registering: " + 
                             currentSeries.getShortName() );  
                                     
           // build a TimeSeries object from the BINNED phases
           // ------------------------------------------------
         
           // Create a MEMTimeSeriesImpl  
           memImpl = new MEMTimeSeriesImpl(
              currentSeries.getShortName() + " folded on " + period + 
              " with " + phaseBins + " bins " );
         
           // Add data to the MEMTimeSeriesImpl 
           memImpl.setData( binnedYData, binnedPhase, binnedError ); 
                  
         } else {

 
            frame.getPlot().setStatusTextTwo( "Registering: " + 
                             currentSeries.getShortName() );  
         
            // Build a TimeSeries object from the UNBINNED phases
            // --------------------------------------------------
         
            // Create a MEMTimeSeriesImpl  
            memImpl = new MEMTimeSeriesImpl(
                      currentSeries.getShortName() + " folded on " + period );
         
            // Add data to the MEMTimeSeriesImpl 
            if ( currentSeries.haveYDataErrors() ) {
               memImpl.setData( yData, phase, errors );
            } else {
               memImpl.setData( yData, phase );
            }
         }

         // Create a real TimeSeries object
         // -------------------------------
         try {
           foldedSeries = new TimeSeries( memImpl );  
           
           // toggle the detrended flag if the previous series had been
           if ( currentSeries.getDetrend() ) {
              foldedSeries.setDetrend( true );
           }
           
           // grab the origin
           if( currentSeries.getOrigin().equals( "a File" ) ||
               currentSeries.getOrigin().equals( "a SOAP message" ) ) {
              String key = seriesManager.getKey( inital );
              foldedSeries.setOrigin( key );
              debugManager.print("            setOrigin( " + key + ")");
           } else {
              foldedSeries.setOrigin( currentSeries.getOrigin() );
           }
              
           // setEphemeris()
           foldedSeries.setEphemeris( ephem );
           
           // setType()
           if( binData ) {
               foldedSeries.setType( TimeSeries.BINFOLDED );
               debugManager.print("            setType( TimeSeries.BINFOLDED)");
           } else {
               foldedSeries.setType( TimeSeries.FOLDED );
               debugManager.print("            setType( TimeSeries.FOLDED)");
           }  
                      
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
     * Load the time series stored in the foldedSeries object. Use a new
     * Thread so that we do not block the GUI or event threads. 
     *
     * If fitting is required then fit and add this series also
     */
    protected void threadLoadNewSeries()
    {
        debugManager.print( "              threadLoadNewSeries()" );
        
        if ( foldedSeries != null ) {

            setWaitCursor();

            //  Now create the thread that reads the spectra.
            Thread loadThread = new Thread( "Folded loader" ) {
                    public void run() {
                        try {
                            debugManager.print(
                                       "                Spawned thread...");
                            addFoldedSeries();
                            if ( fitData ) {
                               fitFoldedSeries();
                            } 
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
           debugManager.print( "                foldedSeries == null");        
           debugManager.print( "                Aborting process...");        
 
           frame.getPlot().setStatusTextTwo( 
                 "Error registering folded data" );  
       }

    }             

    
    /**
     * Add the new folded series to the main desktop inside a new
     * PlotControlFrame
     */ 
    protected void addFoldedSeries() 
    {
          // Debugging is slow, tedious and annoying
          debugManager.print( "                  addFoldedSeries()" );

          debugManager.print( "                    Full Name    = " + 
                          foldedSeries.getFullName() );
          debugManager.print( "                    Short Name   = " +  
                          foldedSeries.getShortName() ); 
          debugManager.print( "                    Series Type  = " +  
                          foldedSeries.getType() );
          debugManager.print( "                    Has Errors?  = " +  
                          foldedSeries.haveYDataErrors() );
          debugManager.print( "                    Draw Errors? = " +  
                          foldedSeries.isDrawErrorBars() );
          debugManager.print( "                    Data Points  = " +  
                          foldedSeries.size() );
          debugManager.print( "                    Data Format  = " +  
                          foldedSeries.getDataFormat() );
          debugManager.print( "                    Plot Style   = " +  
                          foldedSeries.getPlotStyle() );
          debugManager.print( "                    Mark Style   = " +  
                          foldedSeries.getMarkStyle() );
          debugManager.print( "                    Mark Size    = " +  
                          foldedSeries.getMarkSize() ); 
    
          // check if we're in magnitude space, grab the data limits object
          // from the orginal DivaPlot object, contained in the PlotControl
          // object, embedded in a PlotControlFrame (Eeek!) and set the
          // relevant flag in the TimeSeries object so that when its assocaited
          // with a PlotControlFrame the yFlipped flag will be toggled.
          if( frame.getPlot().getPlot().getDataLimits().isPlotInMags() ) {
              debugManager.print( "                    yFlipped     = true");
              foldedSeries.setYFlipped( true );
          } else {
              debugManager.print( "                    yFlipped     = false");
          }    
          
          // add the series to the MainWindow by calling addSeries() in
          // the main Frog Object, by grabbing the reference to the main
          // frog object stored in the TimeSeriesManager. This is an unGodly
          // hack and I should be shot for doing this...
          seriesManager.getFrog().addSeries( foldedSeries );
          frame.getPlot().setStatusTextTwo( "" ); 
   
    }
    
   /**
     * Fit the folded series with a + b*sin() + c*cos()
     */ 
    protected void fitFoldedSeries() 
    {
        // create arrays to hold the fit
        double[] fitX;
        double[] fitY; 
        
        // create a fitting object
        LeastSquaresFitSin sinFit = null;

        // do the fit to a + b*sin() + c*cos()
        if( binData ) {
          sinFit = new LeastSquaresFitSin( binnedPhase, binnedYData, 
                                           binnedError, 1.0 );
                                    
          fitX = new double[ binnedPhase.length ];
          fitY = new double[ binnedPhase.length ];
          
          debugManager.print("            Fitted " + sinFit.getEquation());
          for ( int i = 0; i < binnedPhase.length; i++ ) {
            
             // fill arrays
             fitX[i] = binnedPhase[i];
             fitY[i] = sinFit.getValue( fitX[i] );
            
             debugManager.print( "            Yd = " + 
               fitX[i] + " Yf = " + sinFit.getValue( fitX[i] ) );
          }         

        } else {
          sinFit = new LeastSquaresFitSin( phase, yData, errors, 1.0 );
                                    
          fitX = new double[ phase.length ];
          fitY = new double[ phase.length ];   
                  
          debugManager.print("            Fitted " + sinFit.getEquation());
          for ( int i = 0; i < phase.length; i++ ) {
            
             // fill arrays
             fitX[i] = phase[i];
             fitY[i] = sinFit.getValue( fitX[i] );
            
             debugManager.print( "            Yd = " + 
                     fitX[i] + " Yf = " + sinFit.getValue( fitX[i] ) );
          }                     
            
        }
        
        // Build a TimeSeries object for the fit
        MEMTimeSeriesImpl fitImpl = null;
        
        fitImpl = new MEMTimeSeriesImpl( 
              "sin() + cos() fit to " + foldedSeries.getShortName() );
             
             
        // create the Impl
        fitImpl.setData( fitY, fitX );
         
        // build a real TimeSeries object 
        try {
           fittedSeries = new TimeSeries( fitImpl );  
           fittedSeries.setType( TimeSeries.SINCOSFIT );
           debugManager.print("            setType( TimeSeries.SINCOSFIT)");
        } catch ( FrogException e ) {
           debugManager.print(
                  "          FrogException creating TimeSeries...");
           e.printStackTrace();
           return;
        }
        
            
        // set the plotstyle to polyline and colour to blue
        fittedSeries.setPlotStyle( TimeSeries.POLYLINE );
        fittedSeries.setLineColour( Color.blue.getRGB() );
        fittedSeries.setLineThickness( 1.5 );
        fittedSeries.setLineStyle( 1.0 );  
          
        // toggle the detrended flag if the previous series had been
        if ( foldedSeries.getDetrend() ) {
              fittedSeries.setDetrend( true );
        }
        
        // grab the origin of the folded series
        fittedSeries.setOrigin( foldedSeries.getOrigin() ); 
                
        // associate a SinFit object
        SinFit fit = sinFit.getFit();
        fittedSeries.setSinFit( fit );
       
        // grab the PlotComtrolFrame associate with teh foldedSeries
        int seriesID = seriesManager.getCurrentID();
        String frameKey = "Time Series " + seriesID;
        
        TimeSeriesComp currComp = seriesManager.getSeries( frameKey );
        currComp.add( fittedSeries );
                
        PlotControlFrame currFrame = seriesManager.getFrame( frameKey );
        currFrame.getPlot().updatePlot();

        // associate the TimeSeriesComp with the SinFit
        fit.setTimeSeriesComp( currComp );
        
        debugManager.print("           Fitted: " + fit.toString() );
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
        
   // protected void processWindowEvent(WindowEvent e) 
   // {
   //     if ( e.getID() == WindowEvent.WINDOW_CLOSING ) {
   //         dispose();
   //     }
   //     super.processWindowEvent( e );
   // }

}
