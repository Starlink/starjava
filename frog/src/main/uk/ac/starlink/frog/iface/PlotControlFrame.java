package uk.ac.starlink.frog.iface;

import java.io.File;
import java.lang.Double;
import java.lang.Integer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.gui.AstFigureStore;
import uk.ac.starlink.ast.gui.AstPlotSource;
import uk.ac.starlink.ast.gui.PlotConfigurator;
import uk.ac.starlink.ast.gui.GraphicsHintsControls;
import uk.ac.starlink.ast.gui.GraphicsEdgesControls;
import uk.ac.starlink.ast.gui.ComponentColourControls;
import uk.ac.starlink.ast.grf.DefaultGrfMarker;

import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;

import uk.ac.starlink.topcat.ControlWindow;

import uk.ac.starlink.frog.Frog;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.TimeSeriesFactory;
import uk.ac.starlink.frog.data.GramManager;
import uk.ac.starlink.frog.data.SinFit;
import uk.ac.starlink.frog.data.MEMTimeSeriesImpl;
import uk.ac.starlink.frog.iface.MetaDataPopup;
import uk.ac.starlink.frog.iface.images.ImageHolder;
import uk.ac.starlink.frog.iface.SimpleDataLimitMenu;
import uk.ac.starlink.frog.iface.FoldSeriesDialog;
import uk.ac.starlink.frog.iface.GramCreationDialog;
import uk.ac.starlink.frog.iface.PlotControlFrameListener;
import uk.ac.starlink.frog.plot.PlotControl;
import uk.ac.starlink.frog.plot.DivaPlot;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.Utilities;
import uk.ac.starlink.frog.util.JPEGUtility;
import uk.ac.starlink.frog.fit.LeastSquaresFitSin;

import uk.ac.starlink.diva.*;

/**
 * PlotControlFrame provides a top-level wrapper for a PlotControl
 * object.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since $Date$
 * @since 29-SEP-2000, original version
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class PlotControlFrame extends JInternalFrame 
{
  /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


   /**
     *  Manager Class for TimeSeries
     */
    protected TimeSeriesManager 
         seriesManager = TimeSeriesManager.getReference();
         
   /**
     *  Manager Class for Grams
     */
    protected GramManager gramManager = GramManager.getReference();
         
    /**
     *  PlotControl object for displaying the series.
     */
    protected PlotControl plot;

    /**
     *  TimeSeriesComp object that contains all the displayed series.
     */
    protected TimeSeriesComp timeSeriesComp;

    /**
     *  Save file chooser.
     */
    protected JFileChooser fileChooser = null;
    
    /**
     * Menu item which allows the user to fit the rawe data. This item
     * is located ni the fitMeny sub-menu of the opsMenu (Operations Menu).
     * We define this as class wide so we can toggle it on and off when we 
     * have already fitted the data.
     */
    JMenuItem rawSeriesItem; 
    
    /**
     * Timer for used for event queue actions.
     */
    private Timer waitTimer;    
 
    /**
     *  Main menubar and various menus.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenu displayMenu = new JMenu();
    protected JMenu opsMenu = new JMenu();

    /**
     * Magnitude Checkbox Menu Item
     */
    protected JCheckBoxMenuItem mCheck = new JCheckBoxMenuItem();

    /**
     *  Plot a series.
     *
     *  @param timeSeriesComp Active TimeSeriesComp reference.
     *
     */
    public PlotControlFrame( String title, TimeSeriesComp timeSeriesComp )
        throws FrogException
    {
    
        super(title, true, true, true, true );

        debugManager.print(
              "            Creating PlotControlFrame()...");
        if ( timeSeriesComp == null ) {
            plot = new PlotControl();
        } else {
            plot = new PlotControl( timeSeriesComp );
        }
        this.timeSeriesComp = plot.getTimeSeriesComp();
        initUI( );
    }

  
    /**
     *  Return a reference to the PlotControl object.
     */
    public PlotControl getPlot()
    {
        return plot;
    }

    /**
     *  Make the frame visible and set the default action for when we
     *  are closed.
     */
    protected void initUI( )
    {
        
        // Setup the JInternalFrame
        setDefaultCloseOperation( JInternalFrame.DISPOSE_ON_CLOSE );
        addInternalFrameListener( new PlotControlFrameListener() );         
       
        // add the PlotControl object
        getContentPane().add( plot, BorderLayout.CENTER );
                
        // Configure menus and display
        configureMenus();
        pack();
    }

    /**
     *  Configure the menu and toolbar.
     */
    protected void configureMenus()
    {
        //  Add the menuBar.
        this.setJMenuBar( menuBar );

        //  Add the File menu.
        setupFileMenu();
        
        //  Add the Display menu
        setupDisplayMenu();

        //  Set up the Graphics menu.
        setupGraphicsMenu();
               
        // Add the Operations menu 
        setupOperationsMenu();
    }

    /**
     * Configure the Graphics menu.
     */
    protected void setupGraphicsMenu()
    {
        DrawActions drawActions = plot.getPlot().getDrawActions();
        AstFigureStore store = 
            new AstFigureStore( (AstPlotSource) plot.getPlot(),
                                Utilities.getApplicationName(),
                                "FigureStore.xml",
                                "drawnfigures" );
        drawActions.setFigureStore( store );
        menuBar.add( new DrawGraphicsMenu( drawActions ) );
    }
    
    /**
     *  Configure the File menu.
     */
    protected void setupFileMenu()
    {
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        // Save TimeSeries to disk
        JMenuItem saveItem = new JMenuItem("Save File");
        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator( 
           KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
        saveItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "Save Series...");
              saveFile();
           }
        }); 
        fileMenu.add(saveItem); 
            
        // Print Window Contents
        JMenuItem printItem = new JMenuItem("Print Series");
        printItem.setMnemonic(KeyEvent.VK_P);
        printItem.setAccelerator( 
           KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
        printItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              plot.print();
           }
        }); 
        fileMenu.add(printItem);   
      
        // Export to JPEG
        JMenuItem jpegItem = new JMenuItem("Export to JPEG");
        jpegItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
           
              // Create popup to generate JPEG
              JPEGUtility jpegPopup = new JPEGUtility();
              jpegPopup.showJPEGChooser( plot.getPlot() );
           }
        }); 
        fileMenu.add(jpegItem); 

        // separator
        fileMenu.addSeparator();
        
        // Meta Data
        JMenuItem dataItem = new JMenuItem("Meta Data");
        dataItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Meta-Data Popup...");
               doMetaData( );     
           }
        }); 
        fileMenu.add(dataItem);          

        JMenuItem tableItem = new JMenuItem("View Data");
        tableItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating TableViewer Popup...");
               doTableViewer( );     
           }
        }); 
        fileMenu.add(tableItem);       


        // separator
        fileMenu.addSeparator();
                        
        // Close Window
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.setMnemonic(KeyEvent.VK_C);
        closeItem.setAccelerator( 
           KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK));
        closeItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              
              // close the frame
              doDefaultCloseAction();
           }
        }); 
        fileMenu.add(closeItem);         
      
    }
 
 
    /**
     * Close the JInternalFrame
     */
    public void closeFrame()
    {
       debugManager.print("   Closing frame...");
       removeSeries();
    }   
 
    /**
     * Remove the series from the seriesManager 
     */
     public void removeSeries()
     {
         seriesManager.remove( this );
     }
 
    /**
     * Enable the save file chooser. The currently selected series
     * is saved to a file with the chosen name and data format.
     */
    protected void saveFile()
    {
        debugManager.print( "  saveFile()");

        // grab current series
        int currentSeries = timeSeriesComp.getCurrentSeries();
        
        // open File chooser
        initFileChooser( );
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File destFile = fileChooser.getSelectedFile();
            if ( destFile != null ) {
               threadSaveSeries( currentSeries, destFile.getPath() );
            }
            else {
                //  This occasionally happens (1.4), not sure why...
                JOptionPane.showMessageDialog( this, 
                    "Intermittent error: file not correctly selected.",
                    "No write",  JOptionPane.WARNING_MESSAGE );
            }
        }
     }

    /**
     * Save a given series as a file. Use a thread so that we do not
     * block the GUI or event threads.
     *
     * @param index the index of the series that should be saved
     * @param target the file to write the spectrum into.
     */
    protected void threadSaveSeries( int currentSeries, String target )
    {

        debugManager.print( "    threadSaveSeries()" );
        setWaitCursor();

        // inner class kludge, I'm not sure I know what I'm doing here
        final int index = currentSeries;
        final String fileName = target;

        //  Now create the thread that saves the spectrum.
        Thread saveThread = new Thread( "Series saver" ) {
                public void run() {
                    try {
                       debugManager.print("      Spawned thread...");
                       saveSeries( index, fileName );
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {

                        //  Always tidy up and rewaken interface
                        //  when complete (including if an error
                        //  is thrown).
                        resetWaitCursor();
                    }
                }
            };

        //  Start saving spectrum.
        saveThread.start();
    }

    /**
     * Save a series to disk file.
     *
     * @param index the index of the series to save
     * @param file the file to write the series into.
     */
    public void saveSeries( int currentSeries, String file )
    { 
         TimeSeries selectedSeries = timeSeriesComp.get( currentSeries );
         debugManager.print( "        saveSeries(" + 
                             selectedSeries.getShortName() +")");
                             
        TimeSeriesFactory factory = TimeSeriesFactory.getReference();
        try {
            TimeSeries target = factory.getClone( selectedSeries, file );
            plot.setStatusTextTwo( "Saving: " + file );
            target.save();
            plot.setStatusTextTwo( "" );
        }
        catch ( FrogException e ) {
            e.printStackTrace();
        }
    } 
    
    /**
     * Set the main cursor to indicate waiting for some action to
     * complete and lock the interface by trapping all mouse events.
     */
    protected void setWaitCursor()
    {
        debugManager.print("      setWaitCursor()");
        Cursor wait = Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR );
        Component glassPane = getGlassPane();
        glassPane.setCursor( wait );
        glassPane.setVisible( true );
        glassPane.addMouseListener( new MouseAdapter() {} );
    }

    /**
     * Undo the action of the setWaitCursor method.
     */
    protected void resetWaitCursor()
    {
        debugManager.print("      resetWaitCursor()");
        getGlassPane().setCursor( null );
        getGlassPane().setVisible( false );
    }
 
     /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser( )
    {
        debugManager.print( "    initFileChooser()");
        
        if (fileChooser == null ) {
            fileChooser = new JFileChooser( System.getProperty( "user.dir" ) );
            fileChooser.setMultiSelectionEnabled( true );

            // TEXT Files
            debugManager.print( "      Text files...");
            String[] textExtensions = { "txt", "lis", "cat", "dat" };
            SeriesFileFilter textFileFilter =
                new SeriesFileFilter( textExtensions, "TEXT files" );
            fileChooser.addChoosableFileFilter( textFileFilter );

            // FITS Files
            debugManager.print( "      FITS files...");
            String[] fitsExtensions = { "fits", "fit", "fts" };
            SeriesFileFilter fitsFileFilter =
                new SeriesFileFilter( fitsExtensions, "FITS files" );
            fileChooser.addChoosableFileFilter( fitsFileFilter );
 
            // HDS Container Files
            debugManager.print( "      HDS files...");
            SeriesFileFilter hdsFileFilter =
                new SeriesFileFilter ( "sdf", "HDS container files" );
            fileChooser.addChoosableFileFilter( hdsFileFilter );

            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
    }
 
    /**
     *  Configure the Display menu.
     */
    protected void setupDisplayMenu()
    {
        displayMenu.setText( "Display" );
        menuBar.add( displayMenu );
     
        // Toggle the Vertical Hair    
        final JCheckBoxMenuItem vCheck = new JCheckBoxMenuItem("V-hair",
                                            plot.getPlot().isShowVHair() );
        vCheck.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              plot.getPlot().setShowVHair( vCheck.getState() );
           } 
        });
        
        displayMenu.add(vCheck);
        
        // Toggle display of Error Bars   
        final JCheckBoxMenuItem eCheck = new JCheckBoxMenuItem("Error Bars",
                         plot.getTimeSeriesComp().isDrawErrorBars() );
        
        eCheck.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              plot.getTimeSeriesComp().setDrawErrorBars( eCheck.getState() );
              plot.updatePlot();
          } 
        });

        displayMenu.add(eCheck);       
        
         // Toggle magnitude vs. counts on y axis
        mCheck.setLabel( "Magnitudes" );
        mCheck.setState( plot.getPlot().getDataLimits().isYFlipped() );
        
        mCheck.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
           
             // Call the routine which handles this...
             setInMags( mCheck.getState() );
             
             // update the plot
             plot.updatePlot();
          } 
        });
        
        displayMenu.add(mCheck);       
 

        // Fit to width
        JMenuItem widthItem = new JMenuItem("Fit to Width");
        widthItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             debugManager.print( "Fitting Plot to width of PlotControlFrame");
             plot.fitToWidth();
             plot.updatePlot();
           } 
        });                  
        displayMenu.add(widthItem);
        
        // Fit to height
        JMenuItem heightItem = new JMenuItem("Fit to Height");
        heightItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             debugManager.print( "Fitting Plot to height of PlotControlFrame");
             plot.fitToHeight();
             plot.updatePlot();
           } 
        });          
        displayMenu.add(heightItem);
        
        // Scaling Sub-Menu
        // ----------------
        SimpleDataLimitMenu scalingMenu = 
                     new SimpleDataLimitMenu( plot, displayMenu );
         
        
        // Line Style Sub-Menu
        // -------------------
        JMenu styleDisplaySubMenu = new JMenu("Line Style");
        
        JMenuItem pointsItem = new JMenuItem("Points");
        JMenuItem polylineItem = new JMenuItem("Polyline");
        JMenuItem histogramItem = new JMenuItem("Histogram");
   
        // TimeSeries.POINTS style            
        pointsItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {

              debugManager.print( "Setting line style to TimeSeries.POINTS");
         
              // set the plot style of the curretn series to POINTS
              TimeSeriesComp tmp = plot.getTimeSeriesComp();
              tmp.setPlotStyle( TimeSeries.POINTS );              
              plot.updatePlot();
           } 
        });
        
        // TimeSeries.POLYLINE style
        polylineItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {

              debugManager.print( "Setting line style to TimeSeries.POLYLINE");
         
              // set the plot style of the curretn series to POLYLINE
              TimeSeriesComp tmp = plot.getTimeSeriesComp();
              tmp.setPlotStyle( TimeSeries.POLYLINE );                
              plot.updatePlot();
           } 
        });               

        // TimeSeries.HISTOGRAM style
        histogramItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {

              debugManager.print( "Setting line style to TimeSeries.HISTOGRAM");
         
              // set the plot style of the curretn series to HISTOGRAM
              TimeSeriesComp tmp = plot.getTimeSeriesComp();
              tmp.setPlotStyle( TimeSeries.HISTOGRAM );                
              plot.updatePlot();
           } 
        });        


        styleDisplaySubMenu.add(pointsItem);
        styleDisplaySubMenu.add(polylineItem);
        styleDisplaySubMenu.add(histogramItem);
        displayMenu.add(styleDisplaySubMenu);

    }

    /**
     * Set the value of the mCheck JCheckBoxMenuItem
     *
     * @param value Boolean value for the checkbox
     */
     public void setInMags( boolean b ) 
     {
      
        // Set the checkbox state
        mCheck.setState( b );
                  
        // Set the toggle in DataLimits
        plot.getPlot().getDataLimits().setYFlipped( b );
        
        // Set it in the current timeseries object
        TimeSeriesComp tmpComp = plot.getTimeSeriesComp();
        tmpComp.get(tmpComp.getCurrentSeries()).setYFlipped(mCheck.getState());
             
     }

   
   /**
    * Create the Operations menu and populate it with appropriate actions.
    */
    protected void setupOperationsMenu() 
    {
       opsMenu.setText( "Operations" ); 
       menuBar.add( opsMenu );

       // Detrend Data
       JMenuItem detItem = new JMenuItem("Detrend Data");
       detItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Detrending Dialog...");
               doDetrend( );     
           }
        }); 
        opsMenu.add(detItem);       
     
       // Generate periodogram
       JMenuItem periodItem = new JMenuItem("Find Periodicities");
       periodItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Periodogram Dialog...");
               doGram( );     
           }
        }); 
        opsMenu.add(periodItem);      

        // fit series
        rawSeriesItem = new JMenuItem("Fit Data");
        rawSeriesItem.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { 
                debugManager.print( "Fitting time series...");
                doFit( );     
            }
        }); 
        opsMenu.add(rawSeriesItem);

        // Folding menu
        final JMenu foldMenu = new JMenu( "Fold Data" );
        foldMenu.addMenuListener( new MenuListener() {
           public void menuSelected(MenuEvent e) {   
           
              // Fold series
              JMenuItem foldSeriesItem = new JMenuItem("Fold Only");
              foldSeriesItem.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) { 
                      debugManager.print( "Creating Fold Series Dialog...");
                      doFold( false, false );     
                  }
              }); 
              foldMenu.add(foldSeriesItem);         
        
              // Fold and binseries
              JMenuItem binSeriesItem = new JMenuItem("Fold and Bin Data");
              binSeriesItem.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) { 
                      debugManager.print( "Creating Fold Series Dialog...");
                      doFold( true, false );     
                  }
              }); 
              foldMenu.add(binSeriesItem);        
                   
           }
             
           public void menuDeselected(MenuEvent e) {
              foldMenu.removeAll();
           }
           
           public void menuCanceled(MenuEvent e) { 
              foldMenu.removeAll();
           }      
        
        });
        opsMenu.add(foldMenu);

/*        
        // fitting menu
        final JMenu fitMenu = new JMenu( "Fit Data" );
        fitMenu.addMenuListener( new MenuListener() {
           public void menuSelected(MenuEvent e) {   
 
              // fit series
              rawSeriesItem = new JMenuItem("Fit Data");
              rawSeriesItem.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) { 
                      debugManager.print( "Fitting time series...");
                      doFit( );     
                  }
              }); 
              fitMenu.add(rawSeriesItem);
                                 
              // Fold and fit series
              JMenuItem fitSeriesItem = new JMenuItem("Fit Folded Data");
              fitSeriesItem.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) { 
                      debugManager.print( "Creating Fold Series Dialog...");
                      doFold( false, true );     
                  }
              }); 
              fitMenu.add(fitSeriesItem); 
        
              // Fold and fit binned series
              JMenuItem fbSeriesItem = 
                  new JMenuItem("Fit Folded and Binned Data");
              fbSeriesItem.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) { 
                      debugManager.print( "Creating Fold Series Dialog...");
                      doFold( true, true );     
                  }
              }); 
              fitMenu.add(fbSeriesItem);               
        
           }
             
           public void menuDeselected(MenuEvent e) {
              fitMenu.removeAll();
           }
           
           public void menuCanceled(MenuEvent e) { 
              fitMenu.removeAll();
           }      
        
        });
        opsMenu.add(fitMenu);        
*/
   
    }

    /**
     * Spawn a Meta Data popup
     *
     */
     protected void doMetaData( ) 
     {
        // Create a new Fold Frame
        MetaDataPopup meta = new MetaDataPopup( this );
        seriesManager.getFrog().getDesktop().add(meta);
        meta.show();
     }

    /**
     * Spawn a TableViewer popup
     *
     */
     protected void doTableViewer( ) 
     {
     
        TimeSeries series = seriesManager.getSeries(this).getSeries();
     
        ColumnInfo iColInfo = null;
        ColumnInfo xColInfo = null;
        ColumnInfo yColInfo = null;
        ColumnInfo eColInfo = null;
        
        iColInfo = new ColumnInfo( "Index", Integer.class, "Row index" );
        xColInfo = new ColumnInfo( "Time", Double.class, "Time Axis" );
        yColInfo = new ColumnInfo( "Data", Double.class, "Data Value" );
        
        if( series.isInMags() ) {
           yColInfo.setUnitString( "magnitudes" );
        } else {
           yColInfo.setUnitString( "flux" );
        }

        // grab data
        double xRef[] = series.getXData();
        double yRef[] = series.getYData();
         
        // grab the error if the exist
        double errRef[] = null;
        if( series.haveYDataErrors() ) {
           errRef = series.getYDataErrors();
           eColInfo = new ColumnInfo( "Error", Double.class, "Data Error" );
        }   
         
        // copy the arrays, this sucks as it double the memory requirement
        // or the application at a stroke, but we currently have only
        // references to the data held in the currentSeries object.
        double xData[] = (double[]) xRef.clone();
        double yData[] = (double[]) yRef.clone();

        double errors[] = null;
        if( series.haveYDataErrors() ) {
           errors = (double[]) errRef.clone();
        }   

        // make row index
        int iData[] = new int[xData.length];
        for ( int i = 0; i < xData.length; i++ ) {
            iData[i] = i;
        }
              
        // you can add some more column metadata here by calling ColumnInfo
        // methods if you've got more to say about these columns

        ArrayColumn iCol = ArrayColumn.makeColumn( iColInfo, iData );
        ArrayColumn xCol = ArrayColumn.makeColumn( xColInfo, xData );
        ArrayColumn yCol = ArrayColumn.makeColumn( yColInfo, yData );
 
        ArrayColumn eCol = null;
        if( series.haveYDataErrors() ) {
           eCol = ArrayColumn.makeColumn( eColInfo, errors );
        }   

        final int nRows = xData.length;
        ColumnStarTable sTable = new ColumnStarTable() {
            public long getRowCount() {
                return (long) nRows;
            }
        };

        sTable.setName( seriesManager.getKey( this ) );

        // you can add some more table metadata here by calling 
        // AbstractStarTable methods on st if you've got more to say

        sTable.addColumn( iCol );
        sTable.addColumn( xCol );
        sTable.addColumn( yCol );
        if( series.haveYDataErrors() ) {
           sTable.addColumn( eCol );
        }
        
        // spawn the table viewer
        ControlWindow.getInstance().addTable( sTable, "time series", true );
  
     }  
       
    /**
     * Spawn a FoldSeriesDialog popup
     *
     * @param bin If true the series will be binned after folding
     * @param fit If true the series will be fitted after folding
     * @see FoldSeriesDialog
     */
     protected void doFold(boolean b, boolean f) 
     {
        // Create a new Fold Frame
        FoldSeriesDialog fold = new FoldSeriesDialog( this, b, f );
        seriesManager.getFrog().getDesktop().add(fold);
        fold.show();
        
     }
     
    /**
     * Fit a time series
     *
     * @also FoldSeriesDialog
     */
     protected void doFit( ) 
     {
        debugManager.print("         doFit()" );

        // create arrays to hold the data
        double[] fitX;
        double[] fitY;   
          
        // create arrays to hold the fit
        double[] xData = null;
        double[] yData = null;            
        double[] errors = null;            
            
        // grab the current TimeSeries
        TimeSeries currentSeries = timeSeriesComp.getSeries();
        
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
        xData = (double[]) xRef.clone();
        yData = (double[]) yRef.clone();

        if( currentSeries.haveYDataErrors() ) {
           errors = (double[]) errRef.clone();
        } else { 
           for ( int i = 0; i < xData.length; i++ ) {
              errors[i] = 1.0;
           }
        }
        
        // create a fitting object
        LeastSquaresFitSin sinFit = null;
        if( currentSeries.haveYDataErrors() ) {
           sinFit = new LeastSquaresFitSin( xData, yData, errors, 1.0 );
        }
                                    
        fitX = new double[ xData.length ];
        fitY = new double[ yData.length ];   
                  
        debugManager.print("            Fitted " + sinFit.getEquation());
        for ( int i = 0; i < xData.length; i++ ) {
            
           // fill arrays
           fitX[i] = xData[i];
           fitY[i] = sinFit.getValue( fitX[i] );
            
           debugManager.print( "            Yd = " + 
                     fitX[i] + " Yf = " + sinFit.getValue( fitX[i] ) );
        }                                   
         
        // Build a TimeSeries object for the fit
        MEMTimeSeriesImpl fitImpl = null;
        
        fitImpl = new MEMTimeSeriesImpl( 
              "sin() + cos() fit to " + currentSeries.getShortName() );
             
             
        // create the Impl
        fitImpl.setData( fitY, fitX );
         
        // build a real TimeSeries object 
        TimeSeries fittedSeries = null;
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
        if ( currentSeries.getDetrend() ) {
              fittedSeries.setDetrend( true );
        } 
        
        // grab the origin
        String key = seriesManager.getKey( timeSeriesComp );
        fittedSeries.setOrigin( key );
        debugManager.print("            setOrigin( " + key + ")");

        // associate a SinFit object
        SinFit fit = sinFit.getFit();
        fittedSeries.setSinFit( fit );
        
        // associated the TimeSeriesComp object with the SinFit
        fit.setTimeSeriesComp( timeSeriesComp );
        fittedSeries.setSinFit( fit );
        
        // add the fit to the current frame 
        timeSeriesComp.add( fittedSeries );
        debugManager.print("           Updating plot and toggling menu..." );
        plot.updatePlot();
        debugManager.getFrog().toggleMenuItemsOnSeriesUpdate();
        debugManager.print("           Fitted: " + fit.toString() );      
        rawSeriesItem.setEnabled(false);
               
        if ( seriesManager.getAuto()) {
           // display some meta data
           MetaDataPopup meta = new MetaDataPopup( this );
           Frog frame = debugManager.getFrog();
           JDesktopPane desktop = frame.getDesktop();
           desktop.add(meta);
           meta.show();          
           debugManager.print("             Displaying popup..." );        
        }
        
     }     
       

   /**
     * Spawn a GramCreationDialog popup
     *
     * @see GramCreationDialog
     */
     protected void doGram() 
     {
        // Create a new Gram Frame
        GramCreationDialog gram = new GramCreationDialog( this );
        gramManager.getFrog().getDesktop().add(gram);
        gram.show();         
        
     }   

   /**
     * Spawn a DetrendDataDialog popup
     *
     * @see DetrendDataDialog
     */
     protected void doDetrend() 
     {
        // Create a new Detrend Frame
        DetrendDataDialog det = new DetrendDataDialog( this );
        seriesManager.getFrog().getDesktop().add(det);
        det.show();         
        
     }   
             
}
