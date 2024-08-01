package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import uk.ac.starlink.datanode.tree.TreeTableLoadDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.FileChooserTableLoadDialog;
import uk.ac.starlink.table.gui.FilestoreTableLoadDialog;
import uk.ac.starlink.table.gui.LocationTableLoadDialog;
import uk.ac.starlink.table.gui.SQLTableLoadDialog;
import uk.ac.starlink.table.gui.SystemBrowser;
import uk.ac.starlink.table.gui.TableLoadClient;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.table.gui.TableLoadWorker;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.topcat.contrib.basti.BaSTITableLoadDialog;
import uk.ac.starlink.topcat.contrib.gavo.GavoTableLoadDialog;
import uk.ac.starlink.topcat.vizier.VizierTableLoadDialog;
import uk.ac.starlink.vo.Ri1RegistryTableLoadDialog;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Window which displays the main gui from which to load tables into the
 * application.  It contains toolbar buttons etc for different individual
 * load dialogues.
 */
public class LoadWindow extends AuxWindow {

    private final ToggleButtonModel stayOpenModel_;
    private final TableLoadDialog[] knownDialogs_;
    private final List<Action> actList_;
    private final LoadWorkerStack workerStack_;

    /**
     * Name of the system property which can be used to specify the class
     * names of additional {@link TableLoadDialog} implementations.
     * Each must have a no-arg constructor.  Multiple classnames should be
     * separated by colons.
     */
    public static final String LOAD_DIALOGS_PROPERTY = "startable.load.dialogs";

    /** Class names for the TableLoadDialogs known by default. */
    public final String[] DIALOG_CLASSES = new String[] {
        FilestoreTableLoadDialog.class.getName(),
        TreeTableLoadDialog.class.getName(),
        FileChooserTableLoadDialog.class.getName(),
        LocationTableLoadDialog.class.getName(),
        SQLTableLoadDialog.class.getName(),
        TopcatConeSearchDialog.class.getName(),
        TopcatSiapTableLoadDialog.class.getName(),
        TopcatSsapTableLoadDialog.class.getName(),
        TopcatTapTableLoadDialog.class.getName(),
        Ri1RegistryTableLoadDialog.class.getName(),
        VizierTableLoadDialog.class.getName(),
        TopcatHapiTableLoadDialog.class.getName(),
        GavoTableLoadDialog.class.getName(),
        BaSTITableLoadDialog.class.getName(),
    };

    /**
     * Constructor.
     *
     * @param   parent  parent component
     * @param   tfact  table factory
     */
    @SuppressWarnings("this-escape")
    public LoadWindow( Component parent, final StarTableFactory tfact ) {
        super( "Load New Table", parent );

        /* Define action for whether to stay open after loading. */
        stayOpenModel_ =
            new ToggleButtonModel( "Stay Open", ResourceIcon.KEEP_OPEN,
                                   "Keep window open even after " +
                                   "successful load" );
        getToolBar().add( stayOpenModel_.createToolbarButton() );
        getToolBar().addSeparator();
        JMenu windowMenu = getWindowMenu();
        windowMenu.insert( stayOpenModel_.createMenuItem(),
                           windowMenu.getItemCount() - 2 );

        /* Create and place components for loading by entering location. */
        JComponent locBox = Box.createVerticalBox();
        final LocationTableLoadDialog locTld = new LocationTableLoadDialog();
        LoaderAction locAct =
            new LoaderAction( "OK", locTld.getIcon(),
                              "Load table by giving its filename or URL" ) {
            public TableLoader createTableLoader() {
                return locTld.createTableLoader();
            }
        };
        locTld.configure( tfact, locAct );
        JComponent formatLine = Box.createHorizontalBox();
        formatLine.add( new JLabel( "Format: " ) );
        formatLine.add( new ShrinkWrapper( locTld.createFormatSelector() ) );
        formatLine.add( Box.createHorizontalGlue() );
        locBox.add( formatLine );
        JComponent locLine = Box.createHorizontalBox();
        locLine.add( new JLabel( "Location: " ) );
        locLine.add( Box.createHorizontalStrut( 5 ) );
        locLine.add( locTld.getLocationField() );
        locLine.add( Box.createHorizontalStrut( 5 ) );
        JButton locButt = new JButton( locAct );
        locButt.setIcon( null );
        locLine.add( locButt );
        locBox.add( Box.createVerticalStrut( 5 ) );
        locBox.add( locLine );
        JComponent entryBox = Box.createVerticalBox();
        entryBox.add( locBox );
        getMainArea().add( entryBox, BorderLayout.NORTH );

        /* Prepare actions for all known dialogues. */
        actList_ = new ArrayList<Action>();
        knownDialogs_ =
            Loader.getClassInstances( DIALOG_CLASSES, LOAD_DIALOGS_PROPERTY,
                                      TableLoadDialog.class )
           .toArray( new TableLoadDialog[ 0 ] );
        for ( int i = 0; i < knownDialogs_.length; i++ ) {
            actList_.add( new DialogAction( knownDialogs_[ i ], tfact ) );
        }

        /* Prepare action for system browser load. */
        Action sysAct = new LoaderAction( "System Browser", ResourceIcon.SYSTEM,
                                          "Load table using system browser") {
            private final SystemBrowser browser_ = new SystemBrowser();
            public TableLoader createTableLoader() {
                String format = locTld.getSelectedFormat();
                return browser_.showLoadDialog( LoadWindow.this, format );
            }
        };
        actList_.add( 1, sysAct );

        /* Add actions to toolbar. */
        List<Action> toolList = new ArrayList<Action>( actList_ );
        toolList.remove( getDialogAction( Ri1RegistryTableLoadDialog.class ) );
        toolList.remove( getDialogAction( FileChooserTableLoadDialog.class ) );
        toolList.remove( getDialogAction( LocationTableLoadDialog.class ) );
        for ( Action act : toolList ) {
            getToolBar().add( act );
        }
        getToolBar().addSeparator();

        /* Add a menu for actions. */
        JMenu actMenu = new JMenu( "DataSources" );
        actMenu.setMnemonic( KeyEvent.VK_D );
        for ( Action act : actList_ ) {
            actMenu.add( act );
        }
        getJMenuBar().add( actMenu );

        /* Add larger buttons for the most common load types. */
        List<Action> commonList = new ArrayList<Action>();
        commonList.add( getDialogAction( FilestoreTableLoadDialog.class ) );
        commonList.add( sysAct );
        List<JButton> buttList = new ArrayList<JButton>();
        int buttw = 0;
        for ( Action act : commonList ) {
            JButton butt = new JButton( act );
            buttList.add( butt );
            buttw = Math.max( buttw, butt.getPreferredSize().width );
        }
        JComponent buttBox = Box.createVerticalBox();
        for ( JButton butt : buttList ) {
            Dimension max = butt.getMaximumSize();
            max.width = buttw;
            butt.setMaximumSize( max );
            buttBox.add( Box.createVerticalStrut( 5 ) );
            buttBox.add( butt );
        }
        JComponent buttLine =
            new JPanel( new FlowLayout( FlowLayout.RIGHT, 0, 0 ) );
        buttLine.add( buttBox );
        buttLine.setAlignmentX( LEFT_ALIGNMENT );
        entryBox.add( buttLine );
        entryBox.add( Box.createVerticalStrut( 5 ) );

        /* Table for pending loads. */
        workerStack_ = new LoadWorkerStack();
        JScrollPane workScroller =
            new JScrollPane( workerStack_,
                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        workScroller.getViewport()
                    .setBackground( workerStack_.getBackground() );
        workScroller.setBorder( makeTitledBorder( "Loading Tables" ) );
        getMainArea().add( workScroller, BorderLayout.CENTER );

        /* Demo actions. */
        JMenu demoMenu = new JMenu( "Examples" );
        demoMenu.setMnemonic( KeyEvent.VK_X );
        for ( Example ex : createExamples() ) {
            demoMenu.add( new AbstractAction( ex.name_ ) {
                public void actionPerformed( ActionEvent evt ) {
                    try {
                        StarTable table = tfact.makeStarTable( ex.location_ );
                        ControlWindow.getInstance()
                                     .addTable( table, ex.location_, true );
                        conditionallyClose();
                    }
                    catch ( IOException e ) {
                        ErrorDialog.showError( LoadWindow.this,
                                               "Example Table Load Failure", e,
                                               "Can't load " + ex.location_ );
                    }
                }
            } );
        }
        demoMenu.addSeparator();
        demoMenu.add( new AbstractAction( "All Examples" ) {
            public void actionPerformed( ActionEvent evt ) {
                try {
                    ControlWindow cwin = ControlWindow.getInstance();
                    for ( Example ex : createExamples() ) {
                        cwin.addTable( tfact.makeStarTable( ex.location_ ),
                                       ex.location_, true );
                    }
                    conditionallyClose();
                }
                catch ( IOException e ) {
                    ErrorDialog.showError( LoadWindow.this,
                                           "Example Table Load Failure", e );
                }
            }
        } );
        Action treeDemoAct = new DialogAction( new DemoLoadDialog(), tfact );
        treeDemoAct.putValue( Action.SMALL_ICON, null );
        demoMenu.add( treeDemoAct );
        getJMenuBar().add( demoMenu );

        /* Add standard help actions. */
        addHelp( "LoadWindow" );
    }

    /**
     * Returns list of dialogues known by this window.
     *
     * @return  dialogue list
     */
    public TableLoadDialog[] getKnownDialogs() {
        return knownDialogs_;
    }

    /**
     * Returns a TableLoadDialog in the list known by this window which 
     * has a given class.
     *
     * @param  clazz  class, some subclass of TableLoadDialog
     * @return  existing dialog instance of clazz, or null
     */
    public TableLoadDialog
            getKnownDialog( Class<? extends TableLoadDialog> clazz ) {
        for ( int i = 0; i < knownDialogs_.length; i++ ) {
            TableLoadDialog tld = knownDialogs_[ i ];
            if ( clazz.isAssignableFrom( tld.getClass() ) ) {
                return tld;
            }
        }
        return null;
    }

    /**
     * Returns the action associated with a TableLoadDialog of a given class,
     * if one is currently in use by this window.
     *
     * @param  tldClazz  class, some subclass of TableLoadDialog
     * @return  action which invokes an instance of tldClazz, if one is in use
     */
    public Action getDialogAction( Class<? extends TableLoadDialog> tldClazz ) {
        for ( Action act : actList_ ) {
            if ( act instanceof DialogAction ) {
                DialogAction dact = (DialogAction) act;
                if ( tldClazz
                    .isAssignableFrom( dact.getLoadDialog().getClass() ) ) {
                    return dact;
                }
            }
        }
        return null;
    }

    /**
     * Indicates whether a given load dialogue controlled by this window
     * is currently visible.
     *
     * @param  tld  dialogue
     * @return   true iff a window containing tld's query component
     *                is currently showing
     */
    public boolean isShowing( TableLoadDialog tld ) {
        for ( Action act : actList_ ) {
            if ( act instanceof DialogAction ) {
                DialogAction dact = (DialogAction) act;
                if ( dact.getLoadDialog() == tld ) {
                    return dact.isDialogShowing();
                }
            }
        }
        return false;
    }

    /**
     * Adds a thread which is loading a table to the display in this window.
     *
     * @param   worker  loading thread
     * @param   icon   optional icon indicatig table source
     */
    public void addWorker( TableLoadWorker worker, Icon icon ) {
        workerStack_.addWorker( worker, icon );
        makeVisible();
    }

    /**
     * Removes a load worker thread which was previously added to 
     * the display in this window.
     *
     * @param   worker  loading thread
     */
    public void removeWorker( TableLoadWorker worker ) {
        workerStack_.removeWorker( worker );
        conditionallyClose();
    }

    /**
     * Indicates that an activity has finished which might cause this window
     * to close.  This may or may not cause the window to close, depending on
     * its internal state.
     */
    public void conditionallyClose() {
        if ( workerStack_.getComponentCount() == 0 &&
             ! stayOpenModel_.isSelected() && isShowing() ) {
            dispose();
        }
    }

    /**
     * Returns a list of example table locations
     * to be provided in the Examples menu.
     */
    static Example[] createExamples() {
        ClassLoader loader = LoadWindow.class.getClassLoader();
        return new Example[] {
            new Example( "Messier",
                         loader.getResource( TopcatUtils.DEMO_LOCATION + "/"
                                           + "messier.xml" )
                               .toString() ),
            new Example( "6dfgs Sample",
                         loader.getResource( TopcatUtils.DEMO_LOCATION + "/"
                                           + TopcatUtils.DEMO_TABLE )
                               .toString() ),
            new Example( "2d Attractor 1 Mrow", ":attractor:1e6,clifford" ),
            new Example( "3d Attractor 1 Mrow", ":attractor:1e6,rampe" ),
            new Example( "Fake Sky 1 Mrow", ":skysim:1e6" ),
        };
    }

    /**
     * Utility class that aggregates a table load example name and location.
     */
    static class Example {
        final String name_;
        final String location_;

        /**
         * Constructor.
         *
         * @param  name  presentation name for use in menu text
         * @param  location  table location to be presented to
         *                   StarTableFactory.makeStarTable(String)
         */
        Example( String name, String location ) {
            name_ = name;
            location_ = location;
        }
    }

    /**
     * Action to display a given TableLoadDialog.
     */
    private class DialogAction extends BasicAction {
        private final TableLoadDialog tld_;
        private final StarTableFactory tfact_;
        private TableLoadDialogWindow win_;

        /**
         * Constructor.
         *
         * @param  tld  load dialogue
         * @param  tfact  table factory
         */
        DialogAction( TableLoadDialog tld, StarTableFactory tfact ) {
            super( tld.getName(), tld.getIcon(), tld.getDescription() );
            tld_ = tld;
            tfact_ = tfact;
            if ( ! tld.isAvailable() ) {
                setEnabled( false );
            }
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( win_ == null ) {
                win_ = new TableLoadDialogWindow( LoadWindow.this, tld_,
                                                  LoadWindow.this, tfact_ );
            }
            win_.setVisible( true );
        }

        /**
         * Returns the dialogue associated with this action.
         *
         * @return  dialogue
         */
        public TableLoadDialog getLoadDialog() {
            return tld_;
        }

        /**
         * Indicates whether this dialogue is currently displayed in a
         * non-hidden window.
         *
         * @return  true  iff this dialogue is showing
         */
        public boolean isDialogShowing() {
            return win_ != null && win_.isShowing();
        }
    }

    /**
     * Abstract action which can pop up a window for loading.
     * It basically mediates between a supplied TableLoader and a 
     * TopcatLoadClient.
     * Concrete implementations must implement {@link #createTableLoader}.
     */
    private abstract class LoaderAction extends BasicAction {

        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  name   action name
         * @param  icon   action icon
         * @param  description  action description (tooltip)
         */
        LoaderAction( String name, Icon icon, String description ) {
            super( name, icon, description );
            icon_ = icon;
        }

        /**
         * Returns a TableLoader to provide the table(s) to load.
         *
         * @return  table loader
         */
        protected abstract TableLoader createTableLoader();

        public void actionPerformed( ActionEvent evt ) {
            TableLoader loader = createTableLoader();
            if ( loader == null ) {
                return;
            }
            TopcatLoadClient client =
                new TopcatLoadClient( LoadWindow.this,
                                      ControlWindow.getInstance() );
            TableLoadWorker worker = new TableLoadWorker( loader, client ) {
                protected void finish( boolean cancelled ) {
                    super.finish( cancelled );
                    LoadWindow.this.removeWorker( this );
                }
            };
            LoadWindow.this.addWorker( worker, icon_ );
            worker.start();
        }
    }
}
