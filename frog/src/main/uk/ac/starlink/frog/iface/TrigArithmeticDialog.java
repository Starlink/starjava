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
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.FrogException;

/**
 * Class that displays a dialog window to allow the user to do
 * arithmetic to a single time series object
 *
 * @since 21-MAR-2003
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeries
 */
public class TrigArithmeticDialog extends JInternalFrame
{

   /**
     * Image for sin() function
     */ 
    protected static ImageIcon sinImage = 
        new ImageIcon( ImageHolder.class.getResource( "sin_function.gif" ) );

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
     * JComboBox to hold the TimeSeries list
     */   
     JComboBox whichSeries = new JComboBox();

    /**
     * JComboBox to hold the different arithmetic operations
     */   
     JComboBox whichOps = new JComboBox(); 
    
    /**
     * Window Function checkbox, if ture a window function will be
     * generated rather than the perioogram of the data
     */
     JCheckBox windowCheck = new JCheckBox();
    
    /** 
     * Boolean to say whether we're geenrating a window function
     */
     boolean window;
                 
    /**
     * TextEntryField for the gamma vlaue
     */   
     JTextField gammaEntry = new JTextField();
        
    /**
     * Gamma value
     */
     double gamma;

    /**
     * TextEntryField for the amplitude vlaue
     */   
     JTextField amplitudeEntry = new JTextField();
             
    /**
     * Amplitude value
     */
     double amplitude;  

    /**
     * TextEntryField for the period vlaue
     */   
     JTextField periodEntry = new JTextField();
                      
    /**
     * Period value
     */
     double period;      

    /**
     * TextEntryField for the zero point vlaue
     */   
     JTextField zeroPointEntry = new JTextField();
                      
    /**
     * Zero Point value
     */
     double zeroPoint;        
                
    /**
      * Key in seriesManager for the time series in which we're interested
      */
      String seriesKey;     
    
    /**
      * Which operation we're going to perform (Add, Subtract, Multiply, Divide)
      */
      String opsKey;     
            
    /**
     * Frog object that this is being created from...
     */
     Frog frame = null;
        
    /**
     * Newly constructed (combined) TimeSeries object
     */
     TimeSeries newSeries = null;
    
    /**
     * Create an instance of the dialog and proceed to do arithmetic
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     */
    public TrigArithmeticDialog( )
    {
        super( "Sin( ) Arithmetic", false, true, false, false );
            
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
       Dimension arithSize = getSize();
       Dimension frameSize = frame.getSize();
       setLocation( ( frameSize.width - arithSize.width ) / 4 ,
                    ( frameSize.height - arithSize.height ) / 4  );
       
       // create the top line
       // -------------------
       JLabel seriesLabel = new JLabel( "<html>&nbsp;Time Series&nbsp<html>" );
       seriesLabel.setBorder( BorderFactory.createEtchedBorder() );
       
       // grab the list of TimeSeriesComp objects from the manager
       Object [] timeSeriesList = seriesManager.getSeriesKeys();
       Arrays.sort(timeSeriesList);
       
       // add series to combo boxes
       for ( int i=0; i < timeSeriesList.length; i++ ) {
          whichSeries.addItem( (String)timeSeriesList[i] );
       }     
       whichSeries.setSelectedIndex(0);  

       // create the next line
       // --------------------
       JLabel opsLabel = new JLabel("<html>&nbsp;Operation&nbsp;<html>");
       opsLabel.setBorder( BorderFactory.createEtchedBorder() );
       
       // add operations
       whichOps.addItem( "Add" );
       whichOps.addItem( "Subtract" );
       whichOps.addItem( "Multiply" );
       whichOps.addItem( "Divide" );
       
       // create the function line
       // ----------------------
       JLabel functionLabel = new JLabel( sinImage );
       functionLabel.setBorder( BorderFactory.createEtchedBorder() );

       // create entry widgets
       // --------------------
       JLabel gammaLabel = new JLabel("<html>&nbsp;Gamma&nbsp;<html>");
       gammaLabel.setBorder( BorderFactory.createEtchedBorder() );
       gammaEntry.setColumns(14);

       JLabel ampLabel = new JLabel("<html>&nbsp;Amplitude&nbsp;<html>");
       ampLabel.setBorder( BorderFactory.createEtchedBorder() );
       amplitudeEntry.setColumns(14);

       JLabel perLabel = new JLabel("<html>&nbsp;Period&nbsp;<html>");
       perLabel.setBorder( BorderFactory.createEtchedBorder() );
       periodEntry.setColumns(14);
       
       JLabel zpLabel = new JLabel("<html>&nbsp;Zero Point&nbsp;<html>");
       zpLabel.setBorder( BorderFactory.createEtchedBorder() );
       zeroPointEntry.setColumns(14);       

       // create the check panel
       windowCheck.setText("Add to Windowed Function");
       windowCheck.addItemListener( new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            if( e.getStateChange() == ItemEvent.SELECTED ) {
                 window = true;
            } else {
                 window = false;
            }
         }
       });
       JPanel checkPanel = new JPanel( new BorderLayout() );      
       checkPanel.add( windowCheck, BorderLayout.EAST );                     

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
       mainPanel.add( seriesLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 0;        
       mainPanel.add( whichSeries, constraints );
       
       constraints.gridx = 0;  
       constraints.gridy = 1;        
       mainPanel.add( opsLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 1;        
       mainPanel.add( whichOps, constraints );
       
       constraints.gridx = 0;  
       constraints.gridy = 2;        
       constraints.gridwidth = 2;       
       mainPanel.add( functionLabel, constraints );
       constraints.gridwidth = 1;       
         
       constraints.gridx = 0;  
       constraints.gridy = 3;        
       mainPanel.add( gammaLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 3;        
       mainPanel.add( gammaEntry, constraints );         
         
       constraints.gridx = 0;  
       constraints.gridy = 4;        
       mainPanel.add( ampLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 4;        
       mainPanel.add( amplitudeEntry, constraints );          
         
       constraints.gridx = 0;  
       constraints.gridy = 5;        
       mainPanel.add( perLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 5;        
       mainPanel.add( periodEntry, constraints ); 
         
       constraints.gridx = 0;  
       constraints.gridy = 6;        
       mainPanel.add( zpLabel, constraints );
       
       constraints.gridx = 1;  
       constraints.gridy = 6;        
       mainPanel.add( zeroPointEntry, constraints );        
       
                                    
       // create the button panels
       JPanel buttonPanel = new JPanel( new BorderLayout() );
       JPanel buttons = new JPanel( new BorderLayout() );
              
       // create the button panel
       // -----------------------
       JLabel buttonLabel = new JLabel ( "          " );
       JButton okButton = new JButton( "Ok" );
       okButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "  Doing arithmetic..." );
              
              boolean status = doArithmetic( );
              
              // If we have bad status recursively call ourselves 
              // and get better numbers.
              if ( status != true ) {
                 
                 // dispose of the current incarnation
                 dispose();
                 
                 // create a new version
                 debugManager.print(  "    Respawning the dialog..." );
                 ArithmeticDialog arith = new ArithmeticDialog( );
                 seriesManager.getFrog().getDesktop().add(arith);
                 arith.show();     
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
       
       contentPane.add(mainPanel, BorderLayout.NORTH );
       contentPane.add(checkPanel, BorderLayout.CENTER );
       contentPane.add(buttonPanel, BorderLayout.SOUTH );
       
       pack();

    }

    /**
     * Method that actualy does the work to arithmetic to the TimeSeries 
     * and creates a new TimeSeriesComp and associated PlotControlFrame.
     */
     protected boolean doArithmetic( )
     {
         debugManager.print( "    Calling doArithmetic()..." );

         // Grab selected series values
         seriesKey = (String)whichSeries.getSelectedItem();
         opsKey = (String)whichOps.getSelectedItem();

         String gammaString = gammaEntry.getText();
         String amplitudeString = amplitudeEntry.getText();
         String periodString = periodEntry.getText();
         String zeroPointString = zeroPointEntry.getText();
         
         // define floating or Java will have a cow
         gamma = 0.0;
         amplitude = 0.0;
         period = 0.0;
         zeroPoint = 0.0;
        
         // convert primitive types
          try {
             gamma = (new Double(gammaString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid Gamma..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid Gamma entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }
         
         try {
             amplitude = (new Double(amplitudeString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid Amplitude..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid Amplitude entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }
         
         try {
             period = (new Double(periodString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid Period..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid Period entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }
         
         try {
             zeroPoint = (new Double(zeroPointString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid Zero Point..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid Zero Point entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }         
        
         // check the series is defined
         if ( seriesKey == null ) {
            debugManager.print( "    The series is undefined..." );
            return false;
         }   
         
         // We have valid entries, at least in theory
         debugManager.print( "      Series     " + seriesKey );
         debugManager.print( "      Operation  " + opsKey );
         debugManager.print( "      Gamma      " + gammaString ); 
         debugManager.print( "      Amplitude  " + amplitudeString );
         debugManager.print( "      Period     " + periodString ); 
         debugManager.print( "      Zero Point " + zeroPointString );
         
         // Hide the dialog, we'll dispose of it later...
         hide();
             
         // Call the algorithim
         debugManager.print( "      Calling threadArithTimeSeries()..." ); 
         threadArithTimeSeries();
          
         // doArithmetic() seems to have completed sucessfully.
         return true;
    }
    
    /**
     * Do arithmetic inside its own thread so we don't block the 
     * GUI or event threads. Called from doCombine().
     */
    protected void threadArithTimeSeries()
    {
        debugManager.print( "        threadArithTimeSeries()" );
        setWaitCursor();

        //  Now create the thread that reads the spectra.
        Thread loadThread = new Thread( "Series Arithmetic" ) {
                    public void run() {
                        try {
                            debugManager.print("        Spawned thread...");
                            arithSeries();
     
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
     * The method that carried out the required arithmetic on the TimeSeries
     * data. Called as a new thread from threadFoldTimeSeries().
     */ 
    protected void arithSeries() 
    {
         debugManager.print( "          arithSeries()" );
         
         // Grab the two TimeSeries using the keys provided by the user
         TimeSeriesComp initial = seriesManager.getSeries( seriesKey );
         TimeSeries currentSeries = initial.get(initial.getCurrentSeries());

         // grab the data
         double xRef[] = currentSeries.getXData();
         double yRef[] = currentSeries.getYData();
         
         // grab the error if the exist
         double errRef[] = null;
         if( currentSeries.haveYDataErrors() ) {
            errRef = currentSeries.getYDataErrors();
         }   
         
         // copy the arrays
         double xData[] = (double[]) xRef.clone();
         double yData[] = (double[]) yRef.clone();
         double errors[] = null;
         if( currentSeries.haveYDataErrors() ) {
            errors = (double[]) errRef.clone();
         }   
        
         // Do arithmetic
         // -------------
         for ( int k = 0; k < yData.length; k++ ) {
             
             // calculate the sin() at x position
             double floating = 
               gamma + ( amplitude * Math.sin( ((2.0*Math.PI)/ period ) *
                         ( xData[k] - zeroPoint )));
             
             // window the data first
             if ( window ) {
               yData[k] = 1.0;
             }  
             
             if( opsKey == "Add" ) {
               yData[k] = yData[k] + floating;
               debugManager.print( "              " + k + ": +" + floating );

             } else if( opsKey == "Subtract" ) {
                yData[k] = yData[k] - floating;
               debugManager.print( "              " + k + ": -" + floating );
             
             } else if( opsKey == "Divide" ) {
                 yData[k] = yData[k] / floating;
                 errors[k] = errors[k] / floating;
               debugManager.print( "              " + k + ": /" + floating );
                 
             } else if( opsKey == "Multiply" ) {
                yData[k] = yData[k] * floating;
                errors[k] = errors[k] * floating;
                debugManager.print( "              " + k + ": *" + floating );
             
             }       
                              
         }         
        
        
                   
         // Create a new TimeSeriesComp object
         MEMTimeSeriesImpl memImpl = null;
         
         // Create a MEMTimeSeriesImpl  
         memImpl = new MEMTimeSeriesImpl( 
                      seriesKey + " "+ opsKey + " sin() function" );
         
         // Add data to the MEMTimeSeriesImpl 
         if ( currentSeries.haveYDataErrors() ) {
             memImpl.setData( yData, xData, errors );
         } else {
             memImpl.setData( yData, xData );
         }
          
         // create a real timeseries
         try {
           newSeries = new TimeSeries( memImpl );  
           newSeries.setType( TimeSeries.TIMESERIES );
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
     * Load the time series stored in the newSeries object. Use a new
     * Thread so that we do not block the GUI or event threads. 
     */
    protected void threadLoadNewSeries()
    {
        debugManager.print( "              threadLoadNewSeries()" );
        
        if ( newSeries != null ) {

            setWaitCursor();

            //  Now create the thread that reads the spectra.
            Thread loadThread = new Thread( "New Series loader" ) {
                    public void run() {
                        try {
                            debugManager.print(
                                       "                Spawned thread...");
                            addNewSeries();
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
 
       }

    }             

    
    /**
     * Add the new series to the main desktop inside a new PlotControlFrame
     */ 
    protected void addNewSeries() 
    {
          // Debugging is slow, tedious and annoying
          debugManager.print( "                  addnewSeries()" );

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
          // relevant flag in the TimeSeries object so that when its associated
          // with a PlotControlFrame the yFlipped flag will be toggled.
          
          PlotControlFrame initalFrame = seriesManager.getFrame( seriesKey );
          
          if(initalFrame.getPlot().getPlot().getDataLimits().isPlotInMags()){
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
