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
import uk.ac.starlink.frog.iface.GramControlFrame;
import uk.ac.starlink.frog.iface.PlotControlFrame;
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.GramManager;
import uk.ac.starlink.frog.data.GramFactory;
import uk.ac.starlink.frog.data.MEMGramImpl;
import uk.ac.starlink.frog.data.GramComp;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.Gram;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.gram.FourierTransform;

/**
 * Class that displays a dialog window to allow the user to search for
 * periodicities in a Time Series.
 *
 * @since 16-FEB-2003
 * @author Alasdair Allan
 * @version $Id$
 * @see Gram, TimeSeries
 */
public class GramCreationDialog extends JInternalFrame
{
    /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

    /**
     *  Manager class for Periodograms
     */
    protected GramManager gramManager = GramManager.getReference();


    /**
     *  Manager class for TimeSeries
     */
    protected TimeSeriesManager 
         seriesManager = TimeSeriesManager.getReference();
       
    /**
     * TextEntryField for the minimum frequency
     */   
     JTextField minEntry = new JTextField();
     
    /**
     * TextEntry field for the maximum frequesncy
     */ 
     JTextField maxEntry = new JTextField();     
         
    /**
     * TextEntry field for the frequency interval
     */ 
     JTextField intervalEntry = new JTextField();     
    
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
     * JComboBox to hold the different types of periodogram
     */
     JComboBox gramType = new JComboBox();
     
    /**
     * Array of Strings containing the list of periodograms
     */
     String [] gramItems = { "Fourier Power Spectrum", 
                             "Chi-squared Periodogram" }; 
     
    /**
     * Seleced periodogram type
     */
     String selectedGram = null; 
     
    /**
     * Minimum Frequency
     */
     double minFreq;
         
    /**
     * Maximum Frequency
     */
     double maxFreq;
 
    /**
     * Frequency Interval
     */ 
     double freqInterval;
     
    /**
     * PlotControlFrame holding the TimeSeries we're going to inspect
     * for periodicities.
     */
     PlotControlFrame frame = null;
      
    /**
     * Generated periodogram
     */
     Gram periodogram = null;  
        
    /**
     * Create an instance of the dialog and proceed to generate a
     * periodogram
     *
     * @param f The PlotControlFrame holding the TimeSeries of interest
     */
    public GramCreationDialog( PlotControlFrame f )
    {
        super( "Periodogram Analysis", false, true, false, false );

        frame = f;           // grab the interesting frame
        
        this.setSize( new Dimension( 400, 190 ) );
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
       Dimension gramSize = getSize();
       Dimension frameSize = frame.getSize();
       Point location = frame.getLocation();
       setLocation( ( frameSize.width - gramSize.width ) / 2 + location.x,
                     ( frameSize.height - gramSize.height ) / 2 + location.y );
                      
       // create the four main panels
       JPanel mainPanel = new JPanel( new GridLayout( 4, 2 ) );
       JPanel checkPanel = new JPanel( new BorderLayout() );
       JPanel buttonPanel = new JPanel( new BorderLayout() );
       JPanel buttons = new JPanel( new BorderLayout() );
       
       // create the main panel
       JLabel gramLabel = new JLabel( "<html>&nbsp;Type&nbsp;<html>" );       
       gramLabel.setBorder( BorderFactory.createEtchedBorder() );
          
       for ( int i=0; i < gramItems.length; i++ ) {
          gramType.addItem( gramItems[i] );
       }
       
       JLabel minLabel = new JLabel( 
          "<html>&nbsp;Minimum Frequency&nbsp;<html>" );
          
       JLabel maxLabel = new JLabel( 
          "<html>&nbsp;Maximum Frequency<html>" );
          
       JLabel intLabel = new JLabel(  
          "<html>&nbsp;Frequency Interval&nbsp;<html>");
          
       minLabel.setBorder( BorderFactory.createEtchedBorder() );
       maxLabel.setBorder( BorderFactory.createEtchedBorder() );
       intLabel.setBorder( BorderFactory.createEtchedBorder() );

       mainPanel.add( gramLabel );
       mainPanel.add( gramType );
       mainPanel.add( minLabel );
       mainPanel.add( minEntry );
       mainPanel.add( maxLabel );
       mainPanel.add( maxEntry );       
       mainPanel.add( intLabel );
       mainPanel.add( intervalEntry );       
       
       // create the check panel
       windowCheck.setText("Generate Window Function");
       windowCheck.addItemListener( new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            if( e.getStateChange() == ItemEvent.SELECTED ) {
                 window = true;
                 gramType.setSelectedIndex( 0 );
                 gramType.setEnabled( false );
            } else {
                 window = false;
                 gramType.setEnabled( true );
            }
         }
       });
       
       checkPanel.add( windowCheck, BorderLayout.EAST );                     
       
       // create the button panel
       JLabel buttonLabel = new JLabel ( "          " );
       JButton okButton = new JButton( "Ok" );
       okButton.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "  Creating periodogram..." );
              
              boolean status = doGram( );
    
              // If we have bad status recursively call ourselves 
              // and get better numbers.
              if ( status != true ) {

                 // dispose of the current incarnation
                 dispose();

                 // create a new version
                 debugManager.print(  "    Respawning the dialog..." );
                 GramCreationDialog gram = new GramCreationDialog( frame );
                 gramManager.getFrog().getDesktop().add(gram);
                 gram.show();     
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
       
       // drop the default values into the popup
        
       TimeSeriesComp inital = seriesManager.getSeries( frame );
       TimeSeries currentSeries = inital.get( inital.getCurrentSeries() );
       double[] range = currentSeries.getRange();
       double totalTime = range[1] - range[0];
       
       debugManager.print( 
          "  xmin = " + range[0] + " xmax = " + range[1] + 
          " ymin = " + range[2] + " ymax = " + range[3] );
       debugManager.print( "  nyquist = " + currentSeries.getNyquist() );
       
       // maximum frquency to search
       maxFreq = currentSeries.getNyquist(); 
       if( maxFreq < 0 ) maxFreq = - maxFreq;
        
       // minimum frequency 
       minFreq = 0.0;
       
       // find the number of points, try not to go above 1000
       freqInterval = 1.0 / ( 4.0 * totalTime );
       if( maxFreq/freqInterval > 1000.0 ) {
          freqInterval = (maxFreq - minFreq)/1000.0;
       }  
         
       // drop them into the JText, type to float so we don't put
       // hude numbers of useless decimal places in the dialogs
       minEntry.setText( (new Float(minFreq)).toString());
       maxEntry.setText( (new Float(maxFreq)).toString());
       intervalEntry.setText( (new Float(freqInterval)).toString());

    }

    /**
     * Method that actualy does the work to generate the periodogram
     */
     protected boolean doGram( )
     {
         debugManager.print( "    Calling doGram()..." );

         // Grab JTextField values
         String minString = minEntry.getText();
         String maxString = maxEntry.getText();
         String intString = intervalEntry.getText();
       
         
         // Convert to primitive types      
         try {
             minFreq = (new Double(minString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid minimum frequency..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid minimum frequency entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }

         try {
             maxFreq = (new Double(maxString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid maximum frequency..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                        "Invalid entry: " + e.getMessage(),
                                        "Invalid maximum frequency entered",
                                        JOptionPane.ERROR_MESSAGE);
             return false;
         }
         
         try {
             freqInterval = (new Double(intString)).doubleValue();
         } catch ( Exception e ) {
             debugManager.print( "      Invalid frequency interval..." );
             dispose();      
             JOptionPane message = new JOptionPane();
             message.setLocation( this.getLocation() );
             message.showMessageDialog( this,
                                     "Invalid entry: " + e.getMessage(),
                                     "Invalid frequency interval entered",
                                     JOptionPane.ERROR_MESSAGE);
             return false;
         }
         
         // Grab the selected type of periodogram
         String selected = (String)gramType.getSelectedItem();
         if( selected == "Fourier Power Spectrum" ) {
             selectedGram = "FOURIER";
         }
         else if( selected == "Chi-squared Periodogram" ) {
             selectedGram = "CHISQ";
         }
            
         
         // We have valid entries, at least in theory
         debugManager.print( "      Min Freq      = " + 
                        minString + "( " + minFreq + " )" );
         debugManager.print( "      Max Freq      = " + 
                        maxString + "( " + maxFreq + " )" );
         debugManager.print( "      Freq Interval = " + 
                        intString + "( " + freqInterval + " )" );
                        
         debugManager.print( "      Periodogram   = " +
                        (String)gramType.getSelectedItem() );              
       
         // Hide the dialog, we'll dispose of it later...
         hide();
        
         // Call the periodogram algorithim
         debugManager.print( "      Calling threadMakeGram()..." ); 
         threadMakeGram();
          
         // doGram() seems to have completed sucessfully.                  
         return true;
    }
    
    /**
     * Make the periodogram, do this in a seperate thread
     */
    protected void threadMakeGram()
    {
        debugManager.print( "        threadMakeGram()" );
        setWaitCursor();

        //  Now create the thread that reads the spectra.
        Thread loadThread = new Thread( "Period Search" ) {
                    public void run() {
                        try {
                            debugManager.print("        Spawned thread...");
                            gramSeries();
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
     * The method that carries out the periodogram
     */ 
    protected void gramSeries() 
    {
         debugManager.print( "          gramSeries()" );
         
         // Grab (current) TimeSeries from the PlotControlFrame
         TimeSeriesComp inital = seriesManager.getSeries( frame );
         TimeSeries series = inital.get( inital.getCurrentSeries() );

         frame.getPlot().setStatusTextTwo( "Searching: " + 
                             series.getShortName() );
         
         // Periodogram Factory
         GramFactory gramFactory = GramFactory.getReference();
         
         // make the periodogram
         // --------------------
         debugManager.print( "          Building gramFactory..." );
         try {
            periodogram = gramFactory.get( 
                 series, window, minFreq, maxFreq, freqInterval, selectedGram );
         } catch ( Exception e ) {         
           // do nothing
           e.printStackTrace();
         }
         
         // verbose for debugging  
         double[] xData = periodogram.getXData();
         double[] yData = periodogram.getYData();
           
         for ( int i = 0; i < xData.length; i++ ) {
            debugManager.print( "            " + i + ": " + 
                                xData[i] + "    " + yData[i]  );  
         }  
         
         // registetr the periodogram
         // -------------------------
         frame.getPlot().setStatusTextTwo( "Registering: " + 
                             series.getShortName() );          
         // set the type
         if( selectedGram == "FOURIER" ) {
            periodogram.setType( Gram.FOURIER );
            debugManager.print("            setType( Gram.FOURIER");
         } else if (selectedGram == "CHISQ" ) {
            periodogram.setType( Gram.CHISQ );
            debugManager.print("            setType( Gram.CHISQ");
         }
         
         // assocaite a TimeSeriesComp object with this Gram
         periodogram.setTimeSeriesComp( inital );
         
         // Load the new series into a PlotControlFrame
         debugManager.print( "            Calling threadLoadNewGram()..." );
         threadLoadNewGram();   
       
    }
   
   
    /**
     * Load the periodogram stored in the periodogram object. Use a new
     * Thread so that we do not block the GUI or event threads. 
     */
    protected void threadLoadNewGram()
    {
        debugManager.print( "              threadLoadNewGram()" );
        
        if ( periodogram != null ) {

            setWaitCursor();

            //  Now create the thread that reads the spectra.
            Thread loadThread = new Thread( "Gram loader" ) {
                    public void run() {
                        try {
                            debugManager.print(
                                       "                Spawned thread...");
                            addPeriodogram();
                            if ( gramManager.getAuto() ) {  
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
           debugManager.print( "                periodogram == null");        
           debugManager.print( "                Aborting process...");        
 
           frame.getPlot().setStatusTextTwo( 
                 "Error registering periodogram" );  
       }

    }             

   
    /**
     * Add the new periodogram to the main desktop inside a new
     * GramControlFrame
     */ 
    protected void addPeriodogram() 
    {
          // Debugging is slow, tedious and annoying
          debugManager.print( "                  addPeriodogram()" );

          debugManager.print( "                    Full Name    = " + 
                          periodogram.getFullName() );
          debugManager.print( "                    Short Name   = " +  
                          periodogram.getShortName() ); 
          debugManager.print( "                    Series Type  = " +  
                          periodogram.getType() );
          debugManager.print( "                    Has Errors?  = " +  
                          periodogram.haveYDataErrors() );
          debugManager.print( "                    Draw Errors? = " +  
                          periodogram.isDrawErrorBars() );
          debugManager.print( "                    Data Points  = " +  
                          periodogram.size() );
          debugManager.print( "                    Data Format  = " +  
                          periodogram.getDataFormat() );
          debugManager.print( "                    Plot Style   = " +  
                          periodogram.getPlotStyle() );
          debugManager.print( "                    Mark Style   = " +  
                          periodogram.getMarkStyle() );
          debugManager.print( "                    Mark Size    = " +  
                          periodogram.getMarkSize() ); 
    
          
          // add the series to the MainWindow by calling addGram() in
          // the main Frog Object, by grabbing the reference to the main
          // frog object stored in the GramManager. This is an unGodly
          // hack and I should be shot for doing this...
          gramManager.getFrog().addGram( periodogram );
          frame.getPlot().setStatusTextTwo( "" ); 
   
    }

   
   /**
     * Display the meta-data popup abotu the series
     */ 
    protected void displayMetaData( ) 
    {    
        debugManager.print( "           void displayMetaData( )" );
        
        int seriesID = gramManager.getCurrentID();
        String frameKey = "Periodogram " + seriesID;
        debugManager.print("             Periodogram " + seriesID );
    
        GramControlFrame currFrame = gramManager.getFrame( frameKey );   
         
        // display some meta data
        GramMetaDataPopup meta = new GramMetaDataPopup( currFrame );
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
        Component glassPane = gramManager.getFrog().getGlassPane();
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
        gramManager.getFrog().getGlassPane().setCursor( null );
        gramManager.getFrog().getGlassPane().setVisible( false );
    }
   

}
