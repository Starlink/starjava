package uk.ac.starlink.frog;

// General stuff
import java.io.File;
import java.util.ArrayList;
import java.net.URL;

// GUI stuff
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.help.*;

// Interface classes
import uk.ac.starlink.frog.iface.LookAndFeelManager;
import uk.ac.starlink.frog.iface.SeriesFileFilter;
import uk.ac.starlink.frog.iface.PlotControlFrame;
import uk.ac.starlink.frog.iface.GramControlFrame;
import uk.ac.starlink.frog.iface.AboutFrame;

import uk.ac.starlink.frog.iface.CombineSeriesDialog;
import uk.ac.starlink.frog.iface.CopyFitToSeriesDialog;
import uk.ac.starlink.frog.iface.ArithmeticDialog;
import uk.ac.starlink.frog.iface.TrigArithmeticDialog;
import uk.ac.starlink.frog.iface.FakeDataCreationDialog;
import uk.ac.starlink.frog.iface.DebugConsole;

// Utilities Classes
import uk.ac.starlink.frog.util.Utilities;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogSOAPServer;
//import uk.ac.starlink.frog.util.RemoteServer;

// Data Handlers
import uk.ac.starlink.frog.data.TimeSeriesManager;
import uk.ac.starlink.frog.data.TimeSeriesFactory;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.Ephemeris;
import uk.ac.starlink.frog.data.InputNameParser;
import uk.ac.starlink.frog.data.GramManager;
import uk.ac.starlink.frog.data.GramFactory;
import uk.ac.starlink.frog.data.GramComp;
import uk.ac.starlink.frog.data.Gram;

// JavaHelp
import uk.ac.starlink.help.HelpFrame;
import uk.ac.starlink.frog.help.HelpHolder;

/**
 * This class for constructs the FROG user interface. It creates the main 
 * window and handles managing time series using an instance of the
 * TimeSeriesManager class.
 * <p>
 * Application level operations are available from the main window menu
 * where access to multiple time series is required, for instance adding
 * two series together.
 * <p>
 * Instanced by the FrogMain class, where the <CODE>public static void
 * main()</CODE> method lives.
 *
 * @author Alasdair Allan
 * @version $Id$
 * @since $Date$
 * @see FrogMain
 */
public class Frog extends JFrame
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
     *  Manager class for periodograms
     */
    protected GramManager gramManager = GramManager.getReference();

    /**
     *  Content pane of JFrame
     */
    protected JPanel contentPane;
    
    /**
     * Layout manager for the main JPanel contentPane
     */
    protected BorderLayout mainLayout = new BorderLayout();

    /**
     * Main status label
     */
     protected JLabel frogStatus = new JLabel();
                     
    /**
     *  Main menubar
     */
    protected JMenuBar menuBar = new JMenuBar();
    
    /**
     * Look and Feel and Themes menu
     */
    protected JMenu themeMenu = null;
    
    /**
     * Debug menu, will be hidden if the application wide debugging flag
     * is set to false
     */
    protected JMenu debugMenu = new JMenu( "Debug" );

    /**
     * Operations menu, some tasks in the menu will be hidden or 
     * disabled if not enough time series are currently open
     */
     protected JMenu operationsMenu = new JMenu( "Operations" );
     
    /**
     * The combine series operation menu entry, will be disabled if open
     * series number less than 2
     */
     JMenuItem combineSeriesItem = null;
     
    /**
     * The copy fit operations menu entry, will be disabled if open
     * series (with fits) number less than 2
     */
     JMenuItem copyFitItem = null; 
         
    /**
     * The arithmetic operation menu entry, will be disabled if no
     * time series are open
     */
     JMenuItem arithSeriesItem = null;

    /**
     * The function arithmetic operation menu entry, will be disabled 
     * if no time series are open
     */
     JMenuItem trigArithSeriesItem = null;     

    /**
     * The look and feel, plus metal themes manager.
     */
    protected LookAndFeelManager lookAndFeelManager = null;

    /**
     * Main desktop
     */
    protected JDesktopPane mainDesktop = new JDesktopPane();    
    
    /**
     *  Open file chooser.
     */
    protected JFileChooser fileChooser = null;

    /**
     *  Names of files that are passed to other threads for loading.
     */
    protected File[] newFiles = null;    

    /**
     * Number of series currently loaded by the addNewSeries method.
     */
    private int filesDone = 0;

    /**
     * Whether the application is embedded. In this case application
     * exit is assumed controlled by the embedding app.
     */
    protected boolean embedded = false;    
  
    /**
     *  Create a browser with no existing time series.
     */
    public Frog()
    {
        this( null );
    } 
    
    /**
     * Constructor, with list of time series to initialise.
     *
     *  @param timeseries list of time series to add. If null then none
     *                    are added.
     */
    public Frog( String[] timeseries )
    {    
        
        // Committ a reference to this object to the TimeSeries,
        // Gram Manager and DebugManager classes. We need to get
        // rid of all the getFrog() calls to seriesManager and
        // gramManager(), as this should now be handled in 
        // debugManager() only (I think)? Perhaps move this into
        // util.Utilities class instead?
        //
        // Any way, I think this is a bit of a hack, bad design
        // on my part. Oh well...
        seriesManager.setFrog( this );
        gramManager.setFrog( this );
        debugManager.setFrog( this );

        // turn debugging off, this is a release version
        debugManager.setDebugFlag( false );

        debugManager.print( "\n" + Utilities.getReleaseName() + " V" +
                               Utilities.getReleaseVersion() );

        debugManager.print( "Enabling events...");
        enableEvents( AWTEvent.WINDOW_EVENT_MASK ); 
        try {
            debugManager.print( "Calling initComponents()...");
            initComponents();
        }
        catch ( Exception error ) {
            error.printStackTrace();
            return;
        }
        
        // load any files passed on the command line
        if ( timeseries != null ) {
        
            debugManager.print( "  Files passed on command line...");
       
            for ( int i = 0; i < timeseries.length; i++ ) {
               
               debugManager.print( "    File: " + timeseries[i] );
               File newFile = new File( timeseries[i] );

               // we only want to grab usable files
               InputNameParser namer = new InputNameParser();
               namer.setName( timeseries[i] );
               try {
                  if ( namer.exists() ) {
 
                     // allocate the array on first access
                     if ( newFiles == null ) {             
                        debugManager.print( 
                              "    Creating array, adding file to stack..." ); 
                        newFiles = new File[timeseries.length];
                        newFiles[0] = newFile;
                     } else {
                     
                        // add it to the already existing stack
                        debugManager.print( "    Adding file to stack..." );  
                        newFiles[i] = newFile;
                     }                    
                  } else {
                     throw new FrogException("Cannot read: " +  timeseries[i]);
                  }
              
               } catch ( FrogException error ) {
                    JOptionPane.showMessageDialog( this, error.getMessage(),
                        "Error opening time series", JOptionPane.ERROR_MESSAGE);
                    frogStatus.setText( "Error opening: " + timeseries[i] );
               }      
            }   
             
            // Open the chosen files into internal frames
            debugManager.print( "    Calling threadLoadChosenSeries()...");
            
            SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                         threadLoadChosenSeries( "TimeSeries" );
                         threadInitRemoteServices();
                    }
            });
            
        } else {
            // Make sure we start the remote services, but avoid
            // contention with image loading by also doing this as
            // above when there are files to be loaded.
            SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        threadInitRemoteServices();
                    }
            });        
        
        }
    }     

    /**
     *  Initialise all visual components.
     */
    protected void initComponents()
    {
        //  Set up the content pane and window size.
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout( mainLayout );
        this.setSize( new Dimension( 800, 600 ) );
        this.setTitle( Utilities.getFullDescription() );
               
        // Position the application
        this.setLocation( 50, 50 );

        //  Setup menus and toolbar.
        debugManager.print( "  Calling setupMenusAndToolbar()");
        setupMenusAndToolbar();

        // Setup Desktop
        debugManager.print( "  Adding mainDesktop to contentPane");
        contentPane.add( mainDesktop );
        
        // Setup Status Label
        frogStatus.setText( Utilities.getReleaseName() +  " version " +
                            Utilities.getReleaseVersion() );
        contentPane.add( frogStatus, BorderLayout.SOUTH );
    
    }
    
    /**
     *  Setup the menus and toolbar.
     */
    protected void setupMenusAndToolbar()
    {
        //  Add the menuBar.
        this.setJMenuBar( menuBar );

        // Add File menu
        debugManager.print( "    Calling createFileMenu()");
        createFileMenu();
        
        // Add Options menu
        debugManager.print( "    Calling createOptionsMenu()");
        createOptionsMenu();
        
        // Add Operations menu
        debugManager.print( "    Calling createOperationsMenu()");
        createOperationsMenu();
                
        // Add Debug menu
        debugManager.print( "    Calling createDebugMenu()");
        createDebugMenu();

        // Add Help menu
        debugManager.print( "    Calling createHelpMenu()");
        createHelpMenu();
    }
    
    /**
     *  Create the File Menu and populate it with appropriate actions
     */
    protected void createFileMenu()
    {
    
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu ); 
        
        // Open Time Series File
        JMenuItem openItem = new JMenuItem("Open Time Series");
        openItem.setMnemonic(KeyEvent.VK_T);
        openItem.setAccelerator( 
           KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK));
        openItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "Opening Time Series...");
              openTimeSeries();
           }
        }); 
        fileMenu.add(openItem);            

  
        // Open Periodogram
        JMenuItem gramItem = new JMenuItem("Open Periodogram");
        gramItem.setMnemonic(KeyEvent.VK_P);
        gramItem.setAccelerator( 
           KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
        gramItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "Opening Periodogram...");
              openPeriodogram();
           }
        }); 
        fileMenu.add(gramItem);  

        // Exit
        JMenuItem exitItem = new JMenuItem("Quit");
        exitItem.setMnemonic(KeyEvent.VK_Q);
        exitItem.setAccelerator( 
           KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
        exitItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              debugManager.print( "ActionEvent System.exit(0)");
              System.exit(0); 
           }
        }); 
        fileMenu.add(exitItem);    
        
    }
    
   /**
     * Create the Options menu and populate it with appropriate actions.
     */
    protected void createOptionsMenu()
    {

        JMenu optionsMenu = new JMenu( "Options" );
        menuBar.add( optionsMenu );

        //  Add the LookAndFeel selections.
        lookAndFeelManager =
            new LookAndFeelManager( contentPane, optionsMenu );

        final JMenu autoMenu = new JMenu( "Automatic Display" );
        autoMenu.addMenuListener( new MenuListener() {
           public void menuSelected(MenuEvent e) { 
 
 
               // Toggle the AutoDisplay of TimeSeries MetaData Flag     
               JCheckBoxMenuItem seriesCheck =  new JCheckBoxMenuItem(
                       "Time Series Meta Data", seriesManager.getAuto() );
               seriesCheck.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     if ( seriesManager.getAuto() ) {
                        seriesManager.setAuto(false);
                        debugManager.print( 
                           "AutoDisplay of Periodogram meta-data off...");
                     } else {
                        seriesManager.setAuto(true);
                        debugManager.print( 
                           "AutoDisplay of Periodogram meta-data on...");
                     }
                  } 
               });
               autoMenu.add(seriesCheck);  

               // Toggle the AutoDisplay of Periodogram MetaData Flag     
               JCheckBoxMenuItem gramCheck =  new JCheckBoxMenuItem(
                       "Periodogram Meta Data", gramManager.getAuto() );
               gramCheck.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     if ( gramManager.getAuto() ) {
                        gramManager.setAuto(false);
                        debugManager.print( 
                           "AutoDisplay of TimeSeries meta-data off...");
                     } else {
                        gramManager.setAuto(true);
                        debugManager.print( 
                           "AutoDisplay of TimeSeries meta-data on...");
                     }
                  } 
               });
               autoMenu.add(gramCheck);           
              
           }
           
           public void menuDeselected(MenuEvent e) {
              autoMenu.removeAll();
           }
           
           public void menuCanceled(MenuEvent e) { 
              autoMenu.removeAll();
           }
           
        });
        optionsMenu.add(autoMenu);

         
        // Toggle the Debug Flag     
        JCheckBoxMenuItem debugCheck = new JCheckBoxMenuItem("Debug",
                                                 debugManager.getDebugFlag() );
        debugCheck.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              if ( debugManager.getDebugFlag() ) {
                 debugManager.print( "Debugging off...");
                 debugManager.setDebugFlag(false);
                 
                 // hide the debug menu
                 debugMenu.setVisible( false );
                 
                 // hide the debugging console if its around
                 DebugConsole consoleInstance = DebugConsole.getReference();
                 if ( consoleInstance.isVisible() ) {
                    consoleInstance.setVisible(false); 
                 } 
              
              } else {
                 debugManager.setDebugFlag(true);
                 debugManager.print( "Debugging on...");
                 
                 // show the debug menu
                 debugMenu.setVisible( true ); 
                 
                 // show the debugging console if its needed
                 if ( !debugManager.getConsoleFlag() ) {
                    DebugConsole consoleInstance = DebugConsole.getReference();
                    consoleInstance.setVisible(true);           
                    consoleInstance.moveToFront();          
                 } 
              }
           } 
        });
        optionsMenu.add(debugCheck);            
        
        
        
        
    }

   /**
     * Create the Operations menu and populate it with appropriate actions.
     */
    protected void createOperationsMenu()
    {
       menuBar.add( operationsMenu );
 
       // Combine series
       combineSeriesItem = new JMenuItem("Combine Series");
       combineSeriesItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Combine Series Dialog...");
               doCombine( );     
           }
       }); 
       operationsMenu.add(combineSeriesItem); 
       combineSeriesItem.setEnabled( false );
        
       // Constant Arithmetic on TimeSeries
       arithSeriesItem = new JMenuItem("Constant Arithmetic");
       arithSeriesItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Constant Arithmetic Dialog...");
               doArith( );     
           }
       }); 
       operationsMenu.add(arithSeriesItem); 
       arithSeriesItem.setEnabled( false ); 
        
       // Function Arithmetic on TimeSeries
       trigArithSeriesItem = new JMenuItem("Sin( ) Arithmetic");
       trigArithSeriesItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Function Arithmetic Dialog...");
               doTrigArith( );     
           }
       }); 
       operationsMenu.add(trigArithSeriesItem); 
       trigArithSeriesItem.setEnabled( false );         
       
       // Copy Fit
       copyFitItem = new JMenuItem("Copy Fit to Time Series");
       copyFitItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Copy Fit to Series Dialog...");
               doCopyFit( );     
           }
       }); 
       operationsMenu.add(copyFitItem); 
       copyFitItem.setEnabled( false );
        
       // Periodic Fake Data
       JMenuItem fakePeriodicItem = new JMenuItem("Create Fake Periodic Data");
       fakePeriodicItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

               debugManager.print( "Creating Fake Data Dialog...");
               doPeriodicFake( );     
           }
       }); 
       operationsMenu.add(fakePeriodicItem); 

    }
    
    /**
     * Spawn a CombineSeriesDialog popup
     *
     * @see CombineSeriesDialog
     */
     protected void doCombine() 
     {
        // Create a new Combine Frame
        CombineSeriesDialog combine = new CombineSeriesDialog( );
        mainDesktop.add(combine);
        combine.show();
     } 
     
   /**
     * Spawn a CopyFitToSeriesDialog popup
     *
     * @see CopyFitToSeriesDialog
     */
     protected void doCopyFit() 
     {
        // Create a new Copy Fit frame
        CopyFitToSeriesDialog copy = new CopyFitToSeriesDialog( );
        mainDesktop.add(copy);
        copy.show();
     } 
     
    /**
     * Spawn a ArithmeticDialog popup
     *
     * @see ArithmeticDialog
     */
     protected void doArith() 
     {
        // Create a new Arithmetic Frame
        ArithmeticDialog arith = new ArithmeticDialog( );
        mainDesktop.add(arith);
        arith.show();
     } 

    /**
     * Spawn a TrigArithmeticDialog popup
     *
     * @see TrigArithmeticDialog
     */
     protected void doTrigArith() 
     {
        // Create a new function Arithmetic Frame
        TrigArithmeticDialog trigArith = new TrigArithmeticDialog( );
        mainDesktop.add(trigArith);
        trigArith.show();
     } 
 
    /**
     * Spawn a FakeDataCreationDialog popup
     *
     * @see FakeDataCreationDialog
     */
     protected void doPeriodicFake() 
     {
        // Create a new fake data frame
        FakeDataCreationDialog fake = new FakeDataCreationDialog( );
        mainDesktop.add(fake);
        fake.show();
     }      
               
   /**
     * Create the Debug menu and populate it with appropriate actions.
     * if FrogDebug.debug is false the menu will not be displayed.
     *
     * @see FrogDebug
     */
    protected void createDebugMenu()
    {
        menuBar.add( debugMenu );
        if( debugManager.getDebugFlag() ) {
           debugMenu.setVisible( true );
        } else {
           debugMenu.setVisible( false );
        }  
               
        // TimeSeriesManager
        JMenuItem seriesManagerItem = new JMenuItem("Dump TimeSeriesManager");
        seriesManagerItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

              debugManager.print( "\nDumping TimeSeriesManager...");
              debugManager.print( "\nSeriesMap\n---------\n" 
                                  + seriesManager.dumpSeriesMap() );
              //debugManager.print( "\nFrameMap\n--------\n" 
              //                    + seriesManager.dumpFrameMap() );
           }
        }); 
        debugMenu.add(seriesManagerItem);  

        // GramManager
        JMenuItem gramManagerItem = new JMenuItem("Dump GramManager");
        gramManagerItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 

              debugManager.print( "\nDumping GramManager...");
              debugManager.print( "\nGramMap\n-------\n" 
                                  + gramManager.dumpGramMap() );
           }
        }); 
        debugMenu.add(gramManagerItem);  
        
        
        // TimeSeriesComp
        final JMenu compositeMenu = new JMenu( "Dump TimeSeriesComp" );
        compositeMenu.addMenuListener( new MenuListener() {
           public void menuSelected(MenuEvent e) { 
              createCompositeDumpMenu( compositeMenu );
           }
           
           public void menuDeselected(MenuEvent e) {
              compositeMenu.removeAll();
           }
           
           public void menuCanceled(MenuEvent e) { 
              compositeMenu.removeAll();
           }
           
        });
        debugMenu.add(compositeMenu);

        // GramComp
        final JMenu gramCompositeMenu = new JMenu( "Dump GramComp" );
        gramCompositeMenu.addMenuListener( new MenuListener() {
           public void menuSelected(MenuEvent e) { 
              createGramCompositeDumpMenu( gramCompositeMenu );
           }
           
           public void menuDeselected(MenuEvent e) {
              gramCompositeMenu.removeAll();
           }
           
           public void menuCanceled(MenuEvent e) { 
              gramCompositeMenu.removeAll();
           }
           
        });
        debugMenu.add(gramCompositeMenu);
        
        // separator
        debugMenu.addSeparator();  
        
        // Full garbage collection and output memory statistics
        JMenuItem memoryItem = new JMenuItem( "Memory Statistics" );
        memoryItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
           
              debugManager.printMemoryStatistics( true );
           }   
        });
        debugMenu.add(memoryItem);
        
        // separator
        debugMenu.addSeparator();  
        
        // Toggle the Debug Flag     
        JCheckBoxMenuItem consoleCheck = 
         new JCheckBoxMenuItem("Debug to Console",
                               debugManager.getConsoleFlag() );
                               
        consoleCheck.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
              DebugConsole consoleInstance = DebugConsole.getReference();
              if ( debugManager.getConsoleFlag() ) {
                 consoleInstance.openDebug();           
              } else {
                 consoleInstance.closeDebug();           
              }
           } 
        });
        debugMenu.add(consoleCheck);          
             
    }
    
   /**
     * Generate the list of TimeSeriesComp obejctsand build a submenu 
     * for the application's debugMenu.
     *
     * @param menu The debugMenu widget
     */
    protected void createCompositeDumpMenu( JMenu compositeMenu )
    {
    
     if( seriesManager.getCount() > 0) {
       
        Object [] seriesNames = seriesManager.getSeriesKeys();      
        for( int i=0; i < seriesNames.length; i++ ) {
           
              final String thisName = (String)seriesNames[i];
              JMenuItem tmpItem = new JMenuItem( thisName );
              tmpItem.addActionListener( new ActionListener() {
                 public void actionPerformed(ActionEvent e) { 

                    debugManager.print( "\nDumping '" + thisName + "'...\n");
                    
                    // grab the TimeSeriesComp object
                    TimeSeriesComp tmpComp = seriesManager.getSeries(thisName);
                    debugManager.print( 
                       "  Number of TimeSeries =  " + tmpComp.count() + "\n" );
                    
                    // grab the number of TimeSeries objects in the Composite
                    // and loop round grabbing each TimeSeries in turn and
                    // printing interesting things about them
                    for ( int i = 0; i < tmpComp.count(); i++ ) {
                       
                        // grab TimeSeries object
                        TimeSeries tmpSeries = tmpComp.get( i );
                    
                    
                        debugManager.print( "  TimeSeries [" + i + "]" );
                        debugManager.print( "    Full Name    = " +
                                        tmpSeries.getFullName() );
                        debugManager.print( "    Short Name   = " +
                                        tmpSeries.getShortName() ); 
                        debugManager.print( "    Series Type  = " +
                                        tmpSeries.getType() );
                        debugManager.print( "    Has Errors?  = " +
                                        tmpSeries.haveYDataErrors() );
                        debugManager.print( "    Draw Errors? = " +
                                        tmpSeries.isDrawErrorBars() );
                        debugManager.print( "    Data Points  = " +
                                        tmpSeries.size() );
                        debugManager.print( "    Data Format  = " +
                                        tmpSeries.getDataFormat() );                                     debugManager.print( "    Plot Style   = " +
                                        tmpSeries.getPlotStyle() );       
                        debugManager.print( "    Mark Style   = " +
                                        tmpSeries.getMarkStyle() );             
                        debugManager.print( "    Mark Size    = " +
                                        tmpSeries.getMarkSize() ); 
                        debugManager.print( "    Magnitudes   = " +
                                        tmpSeries.isInMags() );
                                                         
                        if( tmpSeries.getType() == TimeSeries.FOLDED ||
                            tmpSeries.getType() == TimeSeries.BINFOLDED ) {
                                Ephemeris ephem = tmpSeries.getEphemeris();
                                debugManager.print( "    Ephemeris    = " +
                                  ephem.getZeroPoint() + " + " +
                                  ephem.getPeriod() + " x E " ); 
                        }          
                        
                        debugManager.print( "\n    Data\n    ----" );                
                        double xData[] = tmpSeries.getXData();
                        double yData[] = tmpSeries.getYData(); 
                        double errors[] = tmpSeries.getYDataErrors();
                        for ( int j = 0; j < xData.length; j++ ) {
                           if ( errors != null ) {
                              debugManager.print( "    " + j + ": " + 
                                    xData[j]+"    "+yData[j]+"    "+errors[j] );
                           } else {
                              debugManager.print( "    " + j + ": " + 
                                    xData[j] + "    " + yData[j]  );  
                           }                         
                        }                                   
                    }
                 }
              });
              compositeMenu.add(tmpItem); 
           }
        }
     }  

   /**
     * Generate the list of GramComp obejctsand build a submenu 
     * for the application's debugMenu.
     *
     * @param menu The debugMenu widget
     */
    protected void createGramCompositeDumpMenu( JMenu gramCompositeMenu )
    {
    
     if( gramManager.getCount() > 0) {
       
        Object [] gramNames = gramManager.getGramKeys();      
        for( int i=0; i < gramNames.length; i++ ) {
           
              final String thisName = (String)gramNames[i];
              JMenuItem tmpItem = new JMenuItem( thisName );
              tmpItem.addActionListener( new ActionListener() {
                 public void actionPerformed(ActionEvent e) { 

                    debugManager.print( "\nDumping '" + thisName + "'...\n");
                    
                    // grab the GramComp object
                    GramComp tmpComp = gramManager.getGram(thisName);
                    debugManager.print( 
                       "  Number of Gram =  " + tmpComp.count() + "\n" );
                    
                    // grab the number of Gram objects in the gramComp
                    // and loop round grabbing each Gram in turn and
                    // printing interesting things about them
                    for ( int i = 0; i < tmpComp.count(); i++ ) {
                       
                        // grab Gram object
                        Gram tmpGram = tmpComp.get( i );
                    
                    
                        debugManager.print( "  Gram [" + i + "]" );
                        debugManager.print( "    Full Name    = " +
                                        tmpGram.getFullName() );
                        debugManager.print( "    Short Name   = " +
                                        tmpGram.getShortName() ); 
                        debugManager.print( "    Gram Type  = " +
                                        tmpGram.getType() );
                        debugManager.print( "    Has Errors?  = " +
                                        tmpGram.haveYDataErrors() );
                        debugManager.print( "    Draw Errors? = " +
                                        tmpGram.isDrawErrorBars() );
                        debugManager.print( "    Data Points  = " +
                                        tmpGram.size() );
                        debugManager.print( "    Data Format  = " +
                                        tmpGram.getDataFormat() );                                     debugManager.print( "    Plot Style   = " +
                                        tmpGram.getPlotStyle() );       
                        debugManager.print( "    Mark Style   = " +
                                        tmpGram.getMarkStyle() );             
                        debugManager.print( "    Mark Size    = " +
                                        tmpGram.getMarkSize() ); 
                        
                        debugManager.print( "\n    Data\n    ----" );                
                        double xData[] = tmpGram.getXData();
                        double yData[] = tmpGram.getYData(); 
                        double errors[] = tmpGram.getYDataErrors();
                        for ( int j = 0; j < xData.length; j++ ) {
                           if ( errors != null ) {
                              debugManager.print( "    " + j + ": " + 
                                    xData[j]+"    "+yData[j]+"    "+errors[j] );
                           } else {
                              debugManager.print( "    " + j + ": " + 
                                    xData[j] + "    " + yData[j]  );  
                           }                         
                        }                                   
                    }
                 }
              });
              gramCompositeMenu.add(tmpItem); 
           }
        }
     }  

    
   /**
     * Create the Help menu and populate it with appropriate actions.
     */
    protected void createHelpMenu()
    {     
        /**        
        JMenu helpMenu = new JMenu( "Help" );
        menuBar.add( Box.createHorizontalGlue() );
        menuBar.add( helpMenu );
        
        //  help dialog
        final String helpString =  Utilities.getReleaseName() + " Help System";
        JMenuItem helpItem = new JMenuItem( helpString );
        helpItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
               
               debugManager.print( "Creating an helpViewer...");
               JHelp helpViewer = null;
               try {
                   
	           //ClassLoader loader = this.getClass().getClassLoader();
	           //URL url = HelpSet.findHelpSet( loader, "FrogHelpSet.hs");
                   
                   URL url = Frog.class.getResource( "/FrogHelpSet.hs" );
                   if ( url == null ) {
                     throw new FrogException( "Failed to locate HelpSet" );
                   }
                   debugManager.print("  HelpSet = " + url.toString() );
                   
                   HelpSet helpSet = new HelpSet( null, url );
                   helpViewer = new JHelp( helpSet );
                   helpViewer.setCurrentID("Frog.SplashPage");
	      } catch (Exception exception) {
	         debugManager.print("  Cannot find FrogHelpSet.hs");
                 exception.printStackTrace();
     	      }
              
              // Create a new frame.
              try {
                 JFrame frame = new JFrame();
                 //frame.setSize(930,860);
                 frame.setSize(800,600);
                 frame.setLocation(100,100);
                 frame.getContentPane().add(helpViewer);
                 frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                 frame.setVisible(true);
              } catch (Exception exception) {
	         debugManager.print("  Cannot create new frame for helpViewer");
     	      }   
           }              
        }); 
        helpMenu.add(helpItem);  
        **/
        
        // help menu (using Peter's uk.ac.starlink.help classes)
        debugManager.print( "Creating Help Menu using uk.ac.starlink.help");
        final String helpString =  Utilities.getReleaseName() + " Help System";
        
        menuBar.add( Box.createHorizontalGlue() );
        JToolBar dummy = null;
        JMenu helpMenu = HelpFrame.createHelpMenu( null, null,
                                                  "Frog.SplashPage", helpString,
                                                   menuBar, dummy );
        
        HelpFrame.setHelpTitle( helpString);
        
        try {
       
           URL url = Frog.class.getResource( "/FrogHelpSet.hs" );
           if ( url == null ) {
             throw new FrogException( "Failed to locate HelpSet" );
           }
           debugManager.print("  HelpSet = " + url.toString() );
                   
           //HelpSet helpSet = new HelpSet( null, url );
           HelpFrame.addHelpSet( url );
	} catch (Exception exception) {
	         debugManager.print("  Cannot find FrogHelpSet.hs");
                 exception.printStackTrace();
     	}
        
        //  about dialog        
        final String aboutString =  "About " + Utilities.getReleaseName();
        JMenuItem aboutItem = new JMenuItem( aboutString);
        aboutItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) { 
               
               debugManager.print( "Creating an AboutFrame()...");
               
               // Create a new About Frame
               AboutFrame about = new AboutFrame( aboutString );
               
               // Determine optimal location for About Frame
               Dimension aboutSize = about.getSize();
               Dimension thisSize = getSize();
               Point location = getLocation();
               about.setLocation( 
                    ( thisSize.width - aboutSize.width ) / 2 + location.x,
                    ( thisSize.height - aboutSize.height ) / 2 + location.y );
               
               // Display frame
               about.show();
                    
           }
        }); 
        helpMenu.add(aboutItem);  
        
    }

    /**
      * Open File Action, called from FileMenu widget, calls the 
      * initFileChooser() method, and then the threadLoadChosenSeries()
      * method.
      */
    protected void openTimeSeries()
    {
        debugManager.print( "  openTimeSeries()");

        // initialise the file chooser
        initFileChooser( );
        
        int status = fileChooser.showOpenDialog(this);
        if ( status == JFileChooser.CANCEL_OPTION ) {
           debugManager.print( "      Cancelled chooser...");
           return;
        }
           
        try {
           if ( status == fileChooser.APPROVE_OPTION ) {
               newFiles = fileChooser.getSelectedFiles();
               if ( newFiles.length == 0 ) {
                   newFiles = new File[1];
                   newFiles[0] = fileChooser.getSelectedFile();
               }
               debugManager.print( "      Got list of files..." );
           }
        }    
        catch ( Exception error ) {
             
            error.printStackTrace();
            return;
        }
        
        // Open the chosen files into internal frames
        threadLoadChosenSeries( "TimeSeries" );
          
    }  


    /**
      * Open File Action, called from FileMenu widget, calls the 
      * initFileChooser() method, and then the threadLoadChosenSeries()
      * method.
      */
    protected void openPeriodogram()
    {
        debugManager.print( "  openPeriodogram()");

        // initialise the file chooser
        initFileChooser( );
        
        int status = fileChooser.showOpenDialog(this);
        if ( status == JFileChooser.CANCEL_OPTION ) {
           debugManager.print( "      Cancelled chooser...");
           return;
        }
           
        try {
           if ( status == fileChooser.APPROVE_OPTION ) {
               newFiles = fileChooser.getSelectedFiles();
               if ( newFiles.length == 0 ) {
                   newFiles = new File[1];
                   newFiles[0] = fileChooser.getSelectedFile();
               }
               debugManager.print( "      Got list of files..." );
           }
        }    
        catch ( Exception error ) {
             
            error.printStackTrace();
            return;
        }
        
        // Open the chosen files into internal frames
        threadLoadChosenSeries( "Gram" );
          
    }  

    /**
     * Initialise the file chooser to have the necessary filters.
     * called from openFile() method.
     */
    protected void initFileChooser( )
    {
        debugManager.print( "    initFileChooser()");
        
        if (fileChooser == null ) {
            fileChooser = new JFileChooser( System.getProperty( "user.dir" ) );
            fileChooser.setMultiSelectionEnabled( true );


            // FITS Files
            debugManager.print( "      FITS files...");
            String[] fitsExtensions = { "fit", "FIT", "fts" };
            SeriesFileFilter fitsFileFilter =
                new SeriesFileFilter( fitsExtensions, "FITS files" );
            fileChooser.addChoosableFileFilter( fitsFileFilter );


            // TEXT Files
            debugManager.print( "      Text files...");
            String[] textExtensions = { "txt", "lis", "cat", "dat", "bjd" };
            SeriesFileFilter textFileFilter =
                new SeriesFileFilter( textExtensions, "TEXT files" );
            fileChooser.addChoosableFileFilter( textFileFilter );
 
 /*
            // HDS Container Files
            debugManager.print( "      HDS files...");
            SeriesFileFilter hdsFileFilter =
                new SeriesFileFilter ( "sdf", "HDS container files" );
            fileChooser.addChoosableFileFilter( hdsFileFilter );

            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
*/                
        }
   
    }    
    
    /**
     * Load all the time series stored in the newFiles array. Use a new
     * Thread so that we do not block the GUI or event threads. Called
     * from the openFile() method. The spawned thread calls the addNewSeries()
     * method.
     */
    protected void threadLoadChosenSeries(String s)
    {
        debugManager.print( "    threadLoadChosenSeries()" );
        
        
        final String type = s;
        
        if ( newFiles != null ) {

            setWaitCursor();

            //  Now create the thread that reads the spectra.
            Thread loadThread = new Thread( "Series loader" ) {
                    public void run() {
                        try {
                            debugManager.print("      Spawned thread...");
                            addNewSeries( type );
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
     * Add series listed in the newFiles array to TimeSeriesManager
     * called from threadLoadChosenSeries() method as a separate threaded
     * process to prevent blocking the GUI.
     * <p>
     * Calls the addSeries( String name ) method on each file in newFiles
     * or addGram( String name) depending on type of file we're loading
     *
     * @param s Type, either "TimeSeries" or "Gram"
     * @see TimeSeriesManager
     */
    protected void addNewSeries(String s)
    {
        debugManager.print("        addNewSeries()");
        
        // Add all time series
        filesDone = 0;
        for ( int i = 0; i < newFiles.length; i++ ) {
            if( s == "Gram" ) {
                addGram( newFiles[i].getPath() );
            } else {
                addSeries( newFiles[i].getPath() );
            }
            filesDone++;
        }

    }

    /**
     * Add a new time series to the the TimeSeriesManager, called from
     * the addNewSeries() method, as part of the threadLoadChosenSeries()
     * thread. Calls the final addSeries( TimeSeries series) method.
     *
     *  @param name the name (i.e. file specification) of the series to add.
     *  @return true if series is added, false otherwise.
     *  @see TimeSeriesManager
     *  @see TimeSeriesFactory
     */
     public boolean addSeries( String name )  {
        
        debugManager.print("          bool addSeries(" + name + ")");

        // update frogStatus line
        frogStatus.setText( "Opening: " + name );
                 
        try {
   
            TimeSeriesFactory factory = TimeSeriesFactory.getReference();
            TimeSeries series = factory.get( name );
            series.setType( TimeSeries.TIMESERIES );
            addSeries( series );
            return true;
        }
        catch ( FrogException error ) {
            JOptionPane.showMessageDialog( this,
                                           error.getMessage(),
                                           "Error opening time series",
                                           JOptionPane.ERROR_MESSAGE);
                                           
            frogStatus.setText( "Error opening " + name );                               
        }
        return false;    
    
    }
    
    /**
     * Add a new TimeSeries object to the TimeSeriesManager, does the
     * actual work of constructing the TimeSeriesComp object inside a
     * PlotControlFrame. Called from addSeries() method inside the
     * threadLoadChosenSeries() thread.
     *
     * @param series the TimeSeries object.
     * @see TimeSeriesComp
     * @see PlotControlFrame
     */
    public void addSeries( TimeSeries series )
    {
        // verbose if in debug mode                   
        if (debugManager.getDebugFlag()) {                    
             debugManager.print( "            void addSeries(" + 
                                 series.getShortName() +")");
             double xData[] = series.getXData();                         
             double yData[] = series.getYData(); 
             double errors[] = series.getYDataErrors();
             for ( int i = 0; i < xData.length; i++ ) {
                if ( errors != null ) {
                   debugManager.print( "              " + i + ": " + 
                      xData[i] + "    " + yData[i] + "    " + errors[i] );
                } else {
                    debugManager.print( "              " + i + ": " + 
                      xData[i] + "    " + yData[i]  );  
                }                         
             }                       
        } 

        // update frogStatus line
        frogStatus.setText( "Registering: " + series.getShortName() );
                           
        // Create TimeSeriesComp
        TimeSeriesComp seriesComp = new TimeSeriesComp( series ); 
        
        // create an InternalFrame
        try {
        
           // Grab a label for the PlotControl Frame, we'll also use 
           // this as the key to reference the PlotControlFrame and 
           // assocaited TimeSeriesComp object in the TimeSeriesManager
           int seriesNumber = seriesManager.getCount();
           int seriesID = seriesManager.getNextID();
           
           String frameName = "Time Series " + seriesID;
           
           // create a new PlotControlFrame
           PlotControlFrame internal = 
               new PlotControlFrame( frameName, seriesComp ); 
           
           internal.setSize(650,500);
           internal.setLocation( seriesNumber*10, seriesNumber*10 );
           
           // Display the ephemeris if its a folded series
           if( series.getType() == TimeSeries.FOLDED || 
               series.getType() == TimeSeries.BINFOLDED ) {
                 internal.getPlot().setStatusTextOne(
                    " Period:  " + series.getEphemeris().getPeriod() );
                 internal.getPlot().setStatusTextTwo(
                    " Zero Point:  " + series.getEphemeris().getZeroPoint() );
           }
           
           // Toggle the PlotControlFrame yFlipped bit if the TimeSeries
           // data is in magnitudes
           if ( series.isInMags() ) {
            //internal.getPlot().getPlot().getDataLimits().setYFlipped( true );
              internal.setInMags( true );
              internal.getPlot().updatePlot();
           }   
                                                      
           // Index it in the TimeSeriesManager object and display it
           debugManager.print("            Adding internal to mainDesktop...");
           debugManager.print("              SeriesID     = " + seriesID);
           debugManager.print("              SeriesNumber = " + seriesNumber);
           seriesManager.put( frameName, seriesComp, internal );
           seriesManager.display( frameName, mainDesktop );
           
           // toggle the menu items in the operations menu
           toggleMenuItemsOnSeriesUpdate();
           
        
        }
        catch ( FrogException error ) {
            JOptionPane.showMessageDialog( this,
                                           error.getMessage(),
                                           "Error opening time series",
                                           JOptionPane.ERROR_MESSAGE);
        }        

        // Update frogStatus line
        frogStatus.setText( Utilities.getReleaseName() +  " version " +
                            Utilities.getReleaseVersion() );
               
              
    } 
    
   /**
    * Check the count of currently open series and toggle menu items
    */
    public void toggleMenuItemsOnSeriesUpdate() {
    
       // Combine Series Item
       if ( seriesManager.getCount() >= 2 ) {
          combineSeriesItem.setEnabled( true );
       } else {
          combineSeriesItem.setEnabled( false );
       } 
       
       // Arithmetic Item
       if ( seriesManager.getCount() >= 1 ) {
          arithSeriesItem.setEnabled( true );
       } else {
          arithSeriesItem.setEnabled( false );
       } 
       
       // Funtion item
       if ( seriesManager.getCount() >= 1 ) {
          trigArithSeriesItem.setEnabled( true );
       } else {
          trigArithSeriesItem.setEnabled( false );
       } 
       
       // Copy Fit from one series to next
       if ( seriesManager.getFitCount() >= 1 &&
            seriesManager.getCount() >= 2 ) {
          copyFitItem.setEnabled( true );
       } else {
          copyFitItem.setEnabled( false );
       }   
       
    }
    
   /**
     * Add a new periodogram to the the GramManager, called from
     * the addNewSeries() method, as part of the threadLoadChosenSeries()
     * thread. Calls the final addGram( Gram series) method.
     *
     *  @param name the name (i.e. file specification) of the series to add.
     *  @return true if series is added, false otherwise.
     *  @see GramManager
     *  @see GramFactory
     */
     public boolean addGram( String name )  {
        
        debugManager.print("          bool addGram(" + name + ")");

        // update frogStatus line
        frogStatus.setText( "Opening: " + name );
                 
        try {
   
            GramFactory factory = GramFactory.getReference();
            Gram gram = factory.get( name );
            addGram( gram );
            return true;
        }
        catch ( FrogException error ) {
            JOptionPane.showMessageDialog( this,
                                           error.getMessage(),
                                           "Error opening periodogram",
                                           JOptionPane.ERROR_MESSAGE);
                                           
            frogStatus.setText( "Error opening " + name );                               
        }
        return false;    
    
    }
    
  
    /**
     * Add a new Gram object to the GramManager, does the
     * actual work of constructing the GramComp object inside a
     * GramControlFrame. Called from addSeries() method inside the
     * threadLoadChosenSeries() thread.
     *
     * @param gram the Periodogram object.
     * @see GramComp
     * @see GramControlFrame
     */
    public void addGram( Gram gram )
    {
        // verbose if in debug mode                   
        if (debugManager.getDebugFlag()) {                    
             debugManager.print( "            void addGram(" + 
                                 gram.getShortName() +")");
             double xData[] = gram.getXData();                         
             double yData[] = gram.getYData(); 
             for ( int i = 0; i < xData.length; i++ ) {
                debugManager.print( "              " + i + ": " + 
                                    xData[i] + "    " + yData[i]  ); 
             }                        
        } 

        // update frogStatus line
        frogStatus.setText( "Registering: " + gram.getShortName() );
                           
        // Create GramComp
        GramComp gramComp = new GramComp( gram ); 
        
        // create an InternalFrame
        try {
        
           // Grab a label for the GramControl Frame, we'll also use 
           // this as the key to reference the GramControlFrame and 
           // assocaited GramComp object in the GramManager
           int gramNumber = gramManager.getCount();
           int gramID = gramManager.getNextID();
           
           String frameName = "Periodogram " + gramID;
           
           // create a new GramControlFrame
           GramControlFrame internal = 
               new GramControlFrame( frameName, gramComp ); 
           
           internal.setSize(650,500);
           internal.setLocation( 50+gramNumber*10, 10+gramNumber*10 );
                                                      
           // Index it in the GramManager object and display it
           debugManager.print("            Adding internal to mainDesktop...");
           debugManager.print("              GramID     = " + gramID);
           debugManager.print("              GramNumber = " + gramNumber);
           gramManager.put( frameName, gramComp, internal );
           gramManager.display( frameName, mainDesktop );
        
        }
        catch ( FrogException error ) {
            JOptionPane.showMessageDialog( this,
                                           error.getMessage(),
                                           "Error opening periodogram",
                                           JOptionPane.ERROR_MESSAGE);
        }        

        // Update frogStatus line
        frogStatus.setText( Utilities.getReleaseName() +  " version " +
                            Utilities.getReleaseVersion() );
               
              
    }     
    
  /**
     * Initialise the remote control services. These are currently via
     * a socket interface and SOAP based services.
     *
     * This creates a file ~/.splat/.remote with a configuration
     * necessary to contact this through the remote socket control
     * service.
     *
     * @see RemoteServer
     */
    protected void initRemoteServices()
    {
        debugManager.print("    initRemoteServices()");
        if ( ! embedded ) {
            try {
                //  Socket-based services.
                //RemoteServer remoteServer = new RemoteServer( this );
                //remoteServer.start();
                
                //  SOAP based services.
                FrogSOAPServer soapServer = FrogSOAPServer.getInstance();
                soapServer.setFrog( this );
                soapServer.start();
            }
            catch (Exception e) {
                // Not fatal, just no remote control.
                debugManager.print( "    Failed to start SOAP services..." );
                debugManager.print( e.getMessage() );
            }
        }
    }

    /**
     * Initialise the remote control services in a separate
     * thread. Use this to get UI going quickly, but note that the
     * remote services may take some time to initialise.
     */
    protected void threadInitRemoteServices()
    {
        debugManager.print("  threadInitRemoteServices()");
        Thread loadThread = new Thread( "Remote services loader" ) {
                public void run() {
                    try {
                        initRemoteServices();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

        // Thread runs at lowest priority and is a daemon (i.e. runs
        // until application exits, but does not hold lock to exit).
        loadThread.setDaemon( true );
        loadThread.setPriority( Thread.MIN_PRIORITY );
        loadThread.start();
    }
    
     /**
     * Set the main FROG status text
     *
     * @param statusText A String you want displayed on the Status Line
     */
    public void setStatus( String statusText )
    {
        frogStatus.setText( statusText );
    }    
    
    /**
     * Return a reference top the mainDesktop
     *
     * @return desktop Refernce to the main desktop
     */
    public JDesktopPane getDesktop( )
    {
        return mainDesktop;
    }    
    
    /**
     * Exit application when window is closed.
     *
     * @param e WindowEvent
     */
    protected void processWindowEvent( WindowEvent e )
    {
        super.processWindowEvent( e );
        if ( e.getID() == WindowEvent.WINDOW_CLOSING ) {
            debugManager.print( "WindowEvent.WINDOW_CLOSING");
            debugManager.print( "Checking for embedded flag...");
            exitApplicationEvent();
        }
        
        
    }    


    /**
     * Set whether the application should behave as embedded.
     */
    public void setEmbedded( boolean embedded )
    {
        this.embedded = embedded;
    }

    /**
     * A request to exit the application has been received. Only do
     * this if we're not embedded. In that case just make the window 
     * iconized.
     */
    protected void exitApplicationEvent()
    {
        if ( embedded ) {
            setExtendedState( JFrame.ICONIFIED );
        }
        else {
            System.exit( 0 );
        }
    }
       
// End of Class
}
