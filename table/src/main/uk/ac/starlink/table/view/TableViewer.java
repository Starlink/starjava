package uk.ac.starlink.table.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableChooser;
import uk.ac.starlink.table.gui.StarTableSaver;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.util.Loader;

/**
 * Class defining the table viewer application.  Multiple viewers can
 * exist at once, but there is certain state (such as current directory
 * in file chooser dialogs) shared between them in class static members.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class TableViewer extends JFrame {

    private StarTable startab;
    private JTable jtab;
    private JScrollPane scrollpane;
    private JPanel mainpanel;
    private Action exitAct;
    private Action closeAct;
    private Action openAct;
    private Action newAct;
    private Action saveAct;
    private Action dupAct;
    private Action mirageAct;
    private Action plotAct;
    private Action paramAct;
    private Action colinfoAct;

    private static StarTableFactory tabfact = new StarTableFactory();
    private static StarTableOutput taboutput = new StarTableOutput();
    private static StarTableChooser chooser;
    private static StarTableSaver saver;
    private static List instances = new ArrayList();
    private static WindowTracker wtracker = new WindowTracker();
    private static final String DEFAULT_TITLE = "Table Viewer";

    /**
     * Constructs a new TableViewer not initially viewing any table.
     *
     * @param  sibling   a window for positioning relative to; the new
     *         one will generally come out a bit lower and to the right 
     *         of <tt>sibling</tt>.  May be <tt>null</tt>
     */
    public TableViewer( Window sibling ) {
        this( null, sibling );
    }
            
 
    /**
     * Constructs a new TableViewer to view a given table.
     * The given table must provide random access.
     *
     * @param  startab   a star table, or <tt>null</tt>
     * @param  sibling   a window for positioning relative to; the new
     *         one will generally come out a bit lower and to the right 
     *         of <tt>sibling</tt>.  May be <tt>null</tt>
     * @throws  IllegalArgumentException  if <tt>!startab.isRandom()</tt>
     */
    public TableViewer( StarTable startab, Window sibling ) {

        /* Do basic setup. */
        super();
        AuxWindow.positionAfter( sibling, this );
        scrollpane = new SizingScrollPane();
        mainpanel = new JPanel( new BorderLayout() );
        getContentPane().add( mainpanel );
        mainpanel.add( scrollpane, BorderLayout.CENTER );

        /* Create and configure actions. */
        exitAct = new ViewerAction( "Exit", 0,
                                    "Exit the application" );
        closeAct = new ViewerAction( "Close", 0,
                                     "Close this viewer" );
        newAct = new ViewerAction( "New", 0,
                                   "Open a new viewer window" );
        openAct = new ViewerAction( "Open", 0,
                                    "Open a new table in this viewer" );
        saveAct = new ViewerAction( "Save", 0,
                                    "Write out this table" );
        dupAct = new ViewerAction( "Duplicate", 0,
                       "Display another copy of this table in a new viewer" );

        mirageAct = new ViewerAction( "Mirage", 0,
                                      "Launch Mirage to display this table" );
        plotAct = new ViewerAction( "Plot", 0,
                                    "Plot columns from this table" );
        paramAct = new ViewerAction( "Parameters", 0,
                                     "Display table metadata" );
        colinfoAct = new ViewerAction( "Columns", 0,
                                       "Display column metadata" );

        /* Configure the table. */
        if ( startab != null ) {
            setStarTable( startab );
        }

        /* Keep track of instances. */
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        wtracker.register( this );

        /* Set up menus. */
        JMenuBar mb = new JMenuBar();
        setJMenuBar( mb );

        /* File menu. */
        JMenu fileMenu = new JMenu( "File" );
        mb.add( fileMenu );
        fileMenu.add( newAct ).setIcon( null );
        fileMenu.add( dupAct ).setIcon( null );
        fileMenu.add( openAct ).setIcon( null );
        fileMenu.add( saveAct ).setIcon( null );
        fileMenu.add( closeAct ).setIcon( null );
        fileMenu.add( exitAct ).setIcon( null );

        /* Launch menu. */
        if ( MirageHandler.isMirageAvailable() ) {
            JMenu launchMenu = new JMenu( "Launch" );
            mb.add( launchMenu );
            launchMenu.add( mirageAct ).setIcon( null );
        }

        /* Plot menu. */
        JMenu plotMenu = new JMenu( "Plot" );
        mb.add( plotMenu );
        plotMenu.add( plotAct ).setIcon( null );

        /* Metadata menu. */
        JMenu metaMenu = new JMenu( "Metadata" );
        mb.add( metaMenu );
        metaMenu.add( paramAct ).setIcon( null );
        metaMenu.add( colinfoAct ).setIcon( null );

        /* Display. */
        pack();
        setVisible( true );
    }
 

    /**
     * Sets the viewer to view a given StarTable.
     * The given table must provide random access.
     *
     * @param  startab   a star table, or <tt>null</tt> to clear the
     *          viewer
     * @throws  IllegalArgumentException  if <tt>!startab.isRandom()</tt>
     */
    public void setStarTable( StarTable startab ) {
        if ( startab != null ) {
            if ( ! startab.isRandom() ) {
                throw new IllegalArgumentException( 
                    "Can't use non-random table" );
            }
            this.startab = startab;
            jtab = new StarJTable( startab, true );
            ((StarJTable) jtab).configureColumnWidths( 300, 20 );
            scrollpane.setViewportView( jtab );
        }
        else {
            this.startab = null;
            this.jtab = null;
            scrollpane.setViewportView( null );
        }
        setTitle( AuxWindow.makeTitle( DEFAULT_TITLE, startab ) );
        configureActions();
    }

    /**
     * Returns a dialog used for loading new tables.
     *
     * @return  a table chooser
     */
    private static StarTableChooser getChooser() {
        if ( chooser == null ) {
            File dir = ( saver == null ) 
                           ? new File( "." )
                           : saver.getFileChooser().getCurrentDirectory();
            chooser = new StarTableChooser();
            chooser.getFileChooser().setCurrentDirectory( dir );
            chooser.setStarTableFactory( tabfact );
        }
        return chooser;
    }

    /**
     * Returns a dialog used for saving tables.
     *
     * @return  a table saver
     */
    private static StarTableSaver getSaver() {
        if ( saver == null ) {
            File dir = ( chooser == null )
                           ? new File( "." )
                           : chooser.getFileChooser().getCurrentDirectory();
            saver = new StarTableSaver();
            saver.getFileChooser().setCurrentDirectory( dir );
            saver.setStarTableOutput( taboutput );
        }
        return saver;
    }

    /**
     * Call this method to indicate that where there is a choice between
     * a command line and a graphical user interface, the graphical one
     * should be used.  The idea is that if the application is started
     * up from the command line, it might as well use a command line
     * interface until such time as it has posted any windows since
     * (a) that is where the user will be looking and (b) if there is
     * an error near startup it may never be necessary to to the 
     * expensive load of Swing classes.  But once the GUI has got going,
     * then the user will be thinking GUI and a few more dialog boxes
     * won't make any odds.
     */
    private static void setUseGUI() {
        JDBCHandler jh = tabfact.getJDBCHandler();
        if ( ! ( jh.getAuthenticator() instanceof SwingAuthenticator ) ) {
            jh.setAuthenticator( new SwingAuthenticator() );
        }
    }

    /**
     * Configures the enabledness of various actions for which this needs
     * doing based on the current state of this viewer.  To be called
     * both during viewer initialisation and when some relevant aspect
     * of the viewer changes.
     */
    private void configureActions() {
        boolean hasTable = startab != null;
        saveAct.setEnabled( hasTable );
        dupAct.setEnabled( hasTable );
    }

    /**
     * All actions are performed by instances of the one class defined here.  
     * This is a bit less fiddly and probably faster (fewer 
     * classes loaded) than defining a new anonymous class for 
     * each action.
     */
    private class ViewerAction extends AbstractAction {

        private final Window parent = TableViewer.this;

        ViewerAction( String name, int iconId, String shortdesc ) {
            super( name, null );
            putValue( SHORT_DESCRIPTION, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {

            /* Exit the application. */
            if ( this == exitAct ) {
                System.exit( 0 );
            }

            /* Close this viewer window. */
            else if ( this == closeAct ) {
                dispose();
            }

            /* Open a table from a file. */
            else if ( this == openAct ) {
                StarTable st = getChooser().getRandomTable( parent );
                if ( st != null ) {
                    setStarTable( st );
                }
            }

            /* Open a new table viewer window. */
            else if ( this == newAct ) {
                StarTable st = getChooser().getRandomTable( parent );
                if ( st != null ) {
                    new TableViewer( st, parent );
                }
            }

            /* Open the same table in a new viewer. */
            else if ( this == dupAct ) {
                assert startab != null;  // action would be disabled 
                new TableViewer( startab, parent );
            }

            /* Save the table to a file. */
            else if ( this == saveAct ) {
                assert startab != null;  // action would be disabled 
                getSaver().saveTable( startab, parent );
            }

            /* Launch Mirage. */
            else if ( this == mirageAct ) {
                assert MirageHandler.isMirageAvailable();
                try {
                    MirageHandler.invokeMirage( startab, null );
                }
                catch ( Exception e ) {
                    JOptionPane.showMessageDialog( parent, e.toString(),
                                                   "Error launching Mirage",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }

            /* Open a plot window. */
            else if ( this == plotAct ) {
                wtracker.register( new PlotWindow( startab, parent ) );
            }

            /* Display table parameters. */
            else if ( this == paramAct ) {
                wtracker.register( new ParameterWindow( startab, parent ) );
            }

            /* Display column parameters. */
            else if ( this == colinfoAct ) {
                wtracker.register( new ColumnInfoWindow( startab, parent ) );
            }

            /* Shouldn't happen. */
            else {
                throw new AssertionError( 
                    "Unhandled action (programming error)" );
            }
        }
    }

    /**
     * Main method for the table viewer application.
     */
    public static void main( String[] args ) {
        Loader.loadProperties();
        String cmdname = 
            System.getProperty( "uk.ac.starlink.table.view.cmdname" );
        if ( cmdname == null ) {
            cmdname = "TableViewer";
        }
        String usage = "Usage:\n" 
                     + "   " + cmdname + " [table ...]\n";
        if ( args.length > 0 ) {
            int nok = 0;
            TableViewer lastViewer = null;
            for ( int i = 0; i < args.length; i++ ) {

                /* Deal with any known flags. */
                if ( args[ i ].startsWith( "-h" ) ) {
                    System.out.println( usage );
                    System.exit( 0 );
                }

                /* Try to interpret each command line argument as a 
                 * table specification. */
                boolean ok = false;
                try {
                    StarTable startab = tabfact.makeStarTable( args[ i ] );
                    if ( startab == null ) {
                        System.err.println( "No table \"" + args[ i ] + "\"" );
                    }
                    else {
                        startab = Tables.randomTable( startab );
                        lastViewer = new TableViewer( startab, lastViewer );
                        setUseGUI();
                        ok = true;
                    }
                }
                catch ( Exception e ) {
                    System.err.println( "Can't view table \""
                                      + args[ i ] + "\"" );
                    e.printStackTrace( System.err );
                }
                if ( ok ) {
                    nok++;
                }

                /* Bail out if there was an error reading the table. 
                 * This clause could be removed so that the viewer would
                 * still start up when multiple tables were specified on
                 * the command line with some broken and some OK. */
                else {
                    System.exit( 1 );
                }
            }

            /* Bail out in any case if we have no working tables. */
            if ( nok == 0 ) {
                System.exit( 1 );
            }
        }

        /* No tables named on the command line - pop up a dialog asking
         * for one. */
        else {
            StarTable st = getChooser().getRandomTable( null );
            if ( st != null ) {
                new TableViewer( st, null );
            }
            else {
                System.exit( 1 );
            }
        }
    }

}
