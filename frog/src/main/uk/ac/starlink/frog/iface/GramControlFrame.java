package uk.ac.starlink.frog.iface;

import java.io.File;

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

import uk.ac.starlink.frog.data.Gram;
import uk.ac.starlink.frog.data.GramComp;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.GramManager;
import uk.ac.starlink.frog.data.GramFactory;
import uk.ac.starlink.frog.data.DrawSimpleLineOnGramImpl;
import uk.ac.starlink.frog.iface.PlotControlFrame;
import uk.ac.starlink.frog.iface.images.ImageHolder;
import uk.ac.starlink.frog.iface.GramControlFrameListener;
import uk.ac.starlink.frog.iface.FoldSeriesDialog;
import uk.ac.starlink.frog.plot.GramControl;
import uk.ac.starlink.frog.plot.DivaPlot;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.Utilities;
import uk.ac.starlink.frog.util.JPEGUtility;
import uk.ac.starlink.frog.gram.BestPeriod;

import uk.ac.starlink.diva.*;
/**
 * GramControlFrame provides a top-level wrapper for a GramControl
 * object.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since $Date$
 * @since 29-SEP-2000, original version
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class GramControlFrame extends JInternalFrame 
{
  /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

   /**
     *  Manager Class for Gram
     */
    protected GramManager 
         gramManager = GramManager.getReference();

   /**
     *  Manager Class for TimeSeries
     */
    protected TimeSeriesManager 
         seriesManager = TimeSeriesManager.getReference();

                   
    /**
     *  GramControl object for displaying the gram.
     */
    protected GramControl plot;

    /**
     *  GramComp object that contains all the displayed gram.
     */
    protected GramComp timeGramComp;

    /**
     *  Save file chooser.
     */
    protected JFileChooser fileChooser = null;

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
      * the "fold on best period" menu item
      */
    protected JMenu foldMenu;

    /**
     * Magnitude Checkbox Menu Item
     */
    protected JCheckBoxMenuItem mCheck = new JCheckBoxMenuItem();

    /**
     *  Plot a gram.
     *
     *  @param timeGramComp Active GramComp reference.
     *
     */
    public GramControlFrame( String title, GramComp timeGramComp )
        throws FrogException
    {
    
        super(title, true, true, true, true );

        debugManager.print(
              "            Creating GramControlFrame()...");
        if ( timeGramComp == null ) {
            plot = new GramControl();
        } else {
            plot = new GramControl( timeGramComp );
        }
        this.timeGramComp = plot.getGramComp();
        initUI( );
    }

  
    /**
     *  Return a reference to the GramControl object.
     */
    public GramControl getPlot()
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
        addInternalFrameListener( new GramControlFrameListener() );         
       
        // add the GramControl object
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
       
        //  Add the Graphics menu
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

        // Save Gram to disk
        JMenuItem saveItem = new JMenuItem("Save File");
        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator( 
           KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
        saveItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "Save Gram...");
              saveFile();
           }
        }); 
        fileMenu.add(saveItem); 
            
        // Print Window Contents
        JMenuItem printItem = new JMenuItem("Print Periodogram");
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
     * Spawn a Gram Meta Data popup
     *
     */
     protected void doMetaData( ) 
     {
        // Create a new Fold Frame
        GramMetaDataPopup meta = new GramMetaDataPopup( this );
        gramManager.getFrog().getDesktop().add(meta);
        meta.show();
     } 
 
    /**
     * Close the JInternalFrame
     */
    public void closeFrame()
    {
       debugManager.print("   Closing frame...");
       removeGram();
    }   
 
    /**
     * Remove the gram from the gramManager 
     */
     public void removeGram()
     {
         gramManager.remove( this );
     }
 
    /**
     * Enable the save file chooser. The currently selected gram
     * is saved to a file with the chosen name and data format.
     */
    protected void saveFile()
    {
        debugManager.print( "  saveFile()");

        // grab current gram
        int currentGram = timeGramComp.getCurrentGram();
        
        // open File chooser
        initFileChooser( );
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File destFile = fileChooser.getSelectedFile();
            if ( destFile != null ) {
               threadSaveGram( currentGram, destFile.getPath() );
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
     * Save a given gram as a file. Use a thread so that we do not
     * block the GUI or event threads.
     *
     * @param index the index of the gram that should be saved
     * @param target the file to write the spectrum into.
     */
    protected void threadSaveGram( int currentGram, String target )
    {

        debugManager.print( "    threadSaveGram()" );
        setWaitCursor();

        // inner class kludge, I'm not sure I know what I'm doing here
        final int index = currentGram;
        final String fileName = target;

        //  Now create the thread that saves the spectrum.
        Thread saveThread = new Thread( "Gram saver" ) {
                public void run() {
                    try {
                       debugManager.print("      Spawned thread...");
                       saveGram( index, fileName );
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
     * Save a gram to disk file.
     *
     * @param index the index of the gram to save
     * @param file the file to write the gram into.
     */
    public void saveGram( int currentGram, String file )
    { 
         Gram selectedGram = timeGramComp.get( currentGram );
         debugManager.print( "        saveGram(" + 
                             selectedGram.getShortName() +")");
                             
        GramFactory factory = GramFactory.getReference();
        try {
            Gram target = factory.getClone( selectedGram, file );
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
            GramFileFilter textFileFilter =
                new GramFileFilter( textExtensions, "TEXT files" );
            fileChooser.addChoosableFileFilter( textFileFilter );

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
                         plot.getGramComp().isDrawErrorBars() );
        
        eCheck.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              plot.getGramComp().setDrawErrorBars( eCheck.getState() );
              plot.updatePlot();
          } 
        });

        displayMenu.add(eCheck);       
       
        // Fit to width
        JMenuItem widthItem = new JMenuItem("Fit to Width");
        widthItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             debugManager.print( "Fitting Plot to width of GramControlFrame");
             plot.fitToWidth();
             plot.updatePlot();
           } 
        });                  
        displayMenu.add(widthItem);
        
        // Fit to height
        JMenuItem heightItem = new JMenuItem("Fit to Height");
        heightItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             debugManager.print( "Fitting Plot to height of GramControlFrame");
             plot.fitToHeight();
             plot.updatePlot();
           } 
        });          
        displayMenu.add(heightItem);
        
        // Line Style Sub-Menu
        // -------------------
        JMenu styleDisplaySubMenu = new JMenu("Line Style");
        
        JMenuItem pointsItem = new JMenuItem("Points");
        JMenuItem polylineItem = new JMenuItem("Polyline");
        JMenuItem histogramItem = new JMenuItem("Histogram");
   
        // Gram.POINTS style            
        pointsItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {

              debugManager.print( "Setting line style to Gram.POINTS");
         
              // set the plot style of the curretn gram to POINTS
              GramComp tmp = plot.getGramComp();
              tmp.setPlotStyle( Gram.POINTS );              
              plot.updatePlot();
           } 
        });
        
        // Gram.POLYLINE style
        polylineItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {

              debugManager.print( "Setting line style to Gram.POLYLINE");
         
              // set the plot style of the curretn gram to POLYLINE
              GramComp tmp = plot.getGramComp();
              tmp.setPlotStyle( Gram.POLYLINE );                
              plot.updatePlot();
           } 
        });               

        // Gram.HISTOGRAM style
        histogramItem.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {

              debugManager.print( "Setting line style to Gram.HISTOGRAM");
         
              // set the plot style of the curretn gram to HISTOGRAM
              GramComp tmp = plot.getGramComp();
              tmp.setPlotStyle( Gram.HISTOGRAM );                
              plot.updatePlot();
           } 
        });        


        styleDisplaySubMenu.add(pointsItem);
        styleDisplaySubMenu.add(polylineItem);
        styleDisplaySubMenu.add(histogramItem);
        displayMenu.add(styleDisplaySubMenu);

    }

   
   /**
    * Create the Operations menu and populate it with appropriate actions.
    */
    protected void setupOperationsMenu() 
    {
       opsMenu.setText( "Operations" ); 
       menuBar.add( opsMenu );
       
       // Find Best Period
       JMenuItem bestItem;
       bestItem = new JMenuItem("Find Best Period");
       bestItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Finding best period...");
               doBestPeriod( );     
           }
        }); 
        opsMenu.add(bestItem);               
        
        foldMenu = new JMenu( "Fold around Best Period" );
        foldMenu.addMenuListener( new MenuListener() {
           public void menuSelected(MenuEvent e) {   
           
              // Fold series
              JMenuItem foldSeriesItem = new JMenuItem("Fold Only");
              foldSeriesItem.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) { 
                      debugManager.print( "Creating Fold Series Dialog...");
                      doBestFold( false, false );     
                  }
              }); 
              foldMenu.add(foldSeriesItem);         
        
              // Fold and binseries
              JMenuItem binSeriesItem = new JMenuItem("Fold and Bin Data");
              binSeriesItem.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) { 
                      debugManager.print( "Creating Fold Series Dialog...");
                      doBestFold( true, false );     
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
        foldMenu.setEnabled( false );
        
        
    
    }
  
   /**
     * Find the best period
     *
     */
     protected void doBestPeriod( ) 
     {
        // Find the best period (highest peak or lowest trough)
        debugManager.print( "  doBestPeriod()");
        
        Gram gram = gramManager.getGram(this).getGram();
        double range[] = gram.getRange();        
        double bestPeriod = BestPeriod.find ( gram, range[0], range[1] );
        
        gram.setBestPeriod( bestPeriod );
        foldMenu.setEnabled(true); 
        this.getPlot().setStatusTextOne( " Best Period: " + bestPeriod ); 
        
        if( gramManager.getAuto() ) {
           doMetaData( ); 
        }
        
        // create a line
        // -------------
        double[] xLine = new double[2];
        double[] yLine = new double[2];
        
        xLine[0] = 1.0/bestPeriod;
        xLine[1] = 1.0/bestPeriod;
        yLine[0] = 0.8*range[3];
        yLine[1] = 0.99*range[3];
        
        
        // draw a line on the plot to denote the best period
        // -------------------------------------------------
        
        DrawSimpleLineOnGramImpl lineImpl = 
            new DrawSimpleLineOnGramImpl( "Best Period at " + bestPeriod );
            lineImpl.setData( yLine, xLine );
       
        Gram lineGram = null;
        debugManager.print("    Adding marker to plot...");
        
        try {
           lineGram = new Gram( lineImpl );
         } catch ( FrogException e ) {
           debugManager.print("    FrogException creating Maker Line...");
           e.printStackTrace();
           return;
         } 
         
         lineGram.setType( Gram.LINE );
         lineGram.setPlotStyle( Gram.POLYLINE );
         lineGram.setLineColour( Color.red.getRGB() );
         lineGram.setLineThickness( 1.5 );
         
         GramComp thisComp = plot.getGramComp();
         thisComp.add( lineGram );
         this.getPlot().updatePlot();
                
       
     }
     
   /**
     * fold around the best period, spawn a FoldSeriesDialog popup
     *
     * @param bin If true the series will be binned after folding
     * @param fit If true the series will be fitted after folding
     * @see FoldSeriesDialog  
     */
     protected void doBestFold( boolean b, boolean f ) 
     {
        // Find the best period (highest peak or lowest trough)
        debugManager.print( "  doBestFold( b, f)");
        Gram gram = gramManager.getGram(this).getGram();
        double bestPeriod = gram.getBestPeriod( );
        
        // grab the PlotControlFrame associated with the time series
        // of which this is a periodogram
        TimeSeriesComp comp = gram.getTimeSeriesComp();
        PlotControlFrame frame = seriesManager.getFrame( comp );
        
        // spawn the plot control frame
        FoldSeriesDialog fold = new FoldSeriesDialog( 
                                   frame, b, f , 0.0, bestPeriod, 25 );
        debugManager.getFrog().getDesktop().add(fold);
        fold.show();        
     }
     
    /**
     * Spawn a TableViewer popup
     *
     */
     protected void doTableViewer( ) 
     {
     
        Gram gram = gramManager.getGram(this).getGram();
     
        ColumnInfo iColInfo = null;
        ColumnInfo xColInfo = null;
        ColumnInfo yColInfo = null;
        ColumnInfo eColInfo = null;
        
        iColInfo = new ColumnInfo( "Index", Integer.class, "Row index" );
        xColInfo = new ColumnInfo( "Frequency", Double.class, "Frequency" );
        yColInfo = new ColumnInfo( "Power", Double.class, "Power / arbitary" );
    
        // grab data
        double xRef[] = gram.getXData();
        double yRef[] = gram.getYData();
         
        // copy the arrays, this sucks as it double the memory requirement
        // or the application at a stroke, but we currently have only
        // references to the data held in the currentGram object.
        double xData[] = (double[]) xRef.clone();
        double yData[] = (double[]) yRef.clone();

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

        final int nRows = xData.length;
        ColumnStarTable sTable = new ColumnStarTable() {
            public long getRowCount() {
                return (long) nRows;
            }
        };

        sTable.setName( gramManager.getKey( this ) );

        // you can add some more table metadata here by calling 
        // AbstractStarTable methods on st if you've got more to say

        sTable.addColumn( iCol );
        sTable.addColumn( xCol );
        sTable.addColumn( yCol );
        
        // spawn the table viewer
        ControlWindow.getInstance().addTable( sTable, "time series", true ); 
  
     }      

        
}
