package uk.ac.starlink.topcat;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableSource;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.AsciiTableWriter;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Provides a common superclass for windows popped up by TOPCAT.
 * This implements some common look and feel elements.
 * <p>
 * Some window-type utility methods are also provided.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AuxWindow extends JFrame {

    private JMenu windowMenu;
    private JMenu helpMenu;
    private JToolBar toolBar;
    private JLabel headingLabel;
    private JPanel mainArea;
    private JPanel controlPanel;
    private JMenuBar menuBar;
    private Map<String,SaveTableQueryWindow> saveWindows;
    private boolean closeIsExit;
    private boolean isStandalone;
    private boolean packed;

    private final JComponent overPanel;
    private Action aboutAct;
    private final Action controlAct;
    private final Action closeAct;
    private final Action exitAct;
    private final ToggleButtonModel scrollableModel;

    private static final Cursor busyCursor = new Cursor( Cursor.WAIT_CURSOR );
    private static final Logger logger = 
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static final Icon LOGO = getBadge();

    /**
     * Constructs an AuxWindow.
     * 
     * @param  title  the window basic title
     * @param  parent   the parent component of the new window - may be
     *         used for positioning
     */
    @SuppressWarnings("this-escape")
    public AuxWindow( String title, Component parent ) {
        setTitle( title );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        if ( parent != null ) {
            positionAfter( parent, this );
        }
        saveWindows = new HashMap<String,SaveTableQueryWindow>();

        /* Set up a switch for whether this window's content pane will be
         * wrapped in a scrollpane or not.  This is not generally advisable,
         * but for people using very small laptop screens it is sometimes
         * the only way for the larger windows. */
        scrollableModel =
            new ToggleButtonModel( "Scrollable", ResourceIcon.SCROLLER,
                                   "Make the entire window contents "
                                 + "scrollable (for small screens)" );
        final BorderLayout layout = new BorderLayout();
        final JComponent contentPane = new JPanel( layout );
        setContentPane( contentPane );
        final Object centerPos = BorderLayout.CENTER;
        setLayout( layout );
        scrollableModel.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                Component center0 = layout.getLayoutComponent( centerPos );
                if ( center0 != null ) {
                    Component center1 = scrollableModel.isSelected()
                                      ? new JScrollPane( overPanel )
                                      : overPanel;
                    contentPane.remove( center0 );
                    contentPane.add( center1, centerPos );
                    contentPane.revalidate();
                }
            }
        } );

        /* Set up a basic menubar with a Window menu. */
        menuBar = new JMenuBar();
        setJMenuBar( menuBar );
        windowMenu = new JMenu( "Window" );
        windowMenu.setMnemonic( KeyEvent.VK_W );
        menuBar.add( windowMenu );
        controlAct = new AuxAction( "Control Window", ResourceIcon.CONTROL,
                                    "Ensure Control Window is visible" );
        controlAct.putValue( Action.MNEMONIC_KEY, KeyEvent.VK_W );
        closeAct = new AuxAction( "Close", ResourceIcon.CLOSE,
                                  "Close this window" );
        exitAct = new AuxAction( "Exit", ResourceIcon.EXIT,
                                 "Exit the application" );
        windowMenu.add( controlAct );
        windowMenu.add( scrollableModel.createMenuItem() );
        JMenuItem closeItem = windowMenu.add( closeAct );
        closeItem.setMnemonic( KeyEvent.VK_C );
        isStandalone = Driver.isStandalone();
        if ( isStandalone ) {
            JMenuItem exitItem = windowMenu.add( exitAct );
            exitItem.setMnemonic( KeyEvent.VK_X );
        }

        /* Set up a toolbar. */
        toolBar = new JToolBar();
        toolBar.addSeparator();
        toolBar.setFloatable( false );
        contentPane.add( toolBar, BorderLayout.NORTH );

        /* Divide the main area into heading, main area, and control panels. */
        overPanel = new JPanel();
        overPanel.setLayout( new BoxLayout( overPanel, BoxLayout.Y_AXIS ) );
        headingLabel = new JLabel();
        headingLabel.setAlignmentX( 0.0f );
        Box headingBox = new Box( BoxLayout.X_AXIS );
        headingBox.add( headingLabel );
        headingBox.add( Box.createHorizontalGlue() );
        mainArea = new JPanel( new BorderLayout() );
        controlPanel = new JPanel();
        overPanel.add( headingBox );
        overPanel.add( mainArea );
        overPanel.add( controlPanel );
        overPanel.setBorder( BorderFactory
                            .createEmptyBorder( 10, 10, 10, 10 ) );
        contentPane.add( overPanel, centerPos );
    }

    /**
     * Constructs an AuxWindow which will watch a given table.
     * Its title is modified as necessary if the table's label changes.
     * This constructor is only suitable if the window is going to watch
     * (be a view of) a single TopcatModel throughout its life.
     *
     * @param  tcModel  the model owned by this window
     * @param  viewName   name of the type of view provided by this window
     * @param  parent   parent component, may be used for window positioning
     */
    @SuppressWarnings("this-escape")
    public AuxWindow( final TopcatModel tcModel, final String viewName,
                      Component parent ) {
        this( "TOPCAT(" + tcModel.getID() + "): " + viewName, parent );
        TopcatListener labelListener = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                if ( evt.getCode() == TopcatEvent.LABEL ) {
                    setMainHeading( viewName + " for " + tcModel.toString() );
                }
            }
        };
        labelListener.modelChanged( new TopcatEvent( tcModel, TopcatEvent.LABEL,
                                                     null ) );
        tcModel.addTopcatListener( labelListener );
    }

    /**
     * Adds standard actions to this window, in the menu and toolbar.
     * This method should generally be called by subclasses after they
     * have added any other menus and toolbar buttons specific to their
     * function, since the standard buttons appear as the last ones.
     * <p>
     * An ID can be supplied to indicate the page which should be shown
     * in the help viewer when context-sensitive help is requested.
     * This may be <code>null</code> if no change in the help page should
     * be made (for instance if there is no help specific to this window).
     *
     * @param  helpID  the ID of the help item for this window
     */
    protected void addHelp( String helpID ) {

        /* Add a new help menu. */
        helpMenu = new JMenu( "Help" );
        helpMenu.setMnemonic( KeyEvent.VK_H );
        menuBar.add( helpMenu );

        /* Add an action to activate the help browser. */
        Action helpAct = new HelpAction( helpID, this );
        toolBar.add( helpAct );

        /* Add items to the help menu. */
        helpMenu.add( new HelpAction( null, this ) );
        if ( helpID != null ) {
            helpMenu.add( helpAct );
        }
        helpMenu.add( BrowserHelpAction.createManualAction( this ) );
        helpMenu.add( BrowserHelpAction.createManual1Action( this ) );
        if ( helpID != null ) {
            helpMenu.add( BrowserHelpAction.createIdAction( helpID, this ) );
        }
        helpMenu.addSeparator();

        /* Add an About action. */
        aboutAct = new AuxAction( "About TOPCAT",
                                  ResourceIcon.getTopcatLogoSmall(), null );
        helpMenu.add( aboutAct );

        /* Add a close button. */
        toolBar.add( closeIsExit ? exitAct : closeAct );

        /* Add logo and padding. */
        if ( LOGO != null &&
             LOGO.getIconWidth() > 0 && LOGO.getIconHeight() > 0 ) {
            toolBar.add( Box.createHorizontalGlue() );
            toolBar.addSeparator();
            toolBar.add( new JLabel( LOGO ) );
        }
        toolBar.addSeparator();
    }

    /**
     * Makes the window look like it's doing something.  This currently
     * modifies the cursor to be busy/normal.
     *
     * @param  busy  whether the window should look busy
     */
    public void setBusy( boolean busy ) {
        setCursor( busy ? busyCursor : null );
    }

    /**
     * Ensures that this window is posted in a visible fashion.
     */
    public void makeVisible() {
        setState( Frame.NORMAL );
        setVisible( true );
    }

    public void setVisible( boolean isVis ) {
        if ( ! packed && isVis ) {
            pack();
            packed = true;
        }
        super.setVisible( isVis );
    }

    /**
     * Creates a JProgressBar and places it in the the window.
     * It will replace any other progress bar which has been placed
     * by an earlier call of this method.
     *
     * @return   the progress bar which has been placed
     */
    public JProgressBar placeProgressBar() {
        JProgressBar progBar = new JProgressBar();
        getContentPane().add( progBar, BorderLayout.SOUTH );
        return progBar;
    }

    /**
     * Irrevocably marks this window as one for which the Close action has
     * the same effect as the Exit action.  Any Close invocation buttons
     * are replaced with exit ones, duplicates removed, etc.
     * Should be called <em>before</em> any call to {@link #addHelp}.
     */
    public void setCloseIsExit() {
        if ( isStandalone ) {
            closeIsExit = true;

            /* Remove any Close item in the Window menu. */
            boolean exitFound = false;
            for ( int i = windowMenu.getItemCount() - 1; i >= 0; i-- ) {
                JMenuItem item = windowMenu.getItem( i );
                if ( item != null ) {
                    Action act = item.getAction();
                    if ( act == closeAct ) {
                        windowMenu.remove( item );
                    }
                    else if ( act == exitAct ) {
                        exitFound = true;
                    }
                    else if ( act == controlAct ) {
                        windowMenu.remove( item );
                    }
                }
            }
            assert exitFound;
        }
    }

    /**
     * Returns this window's toolbar.  Any client which adds a group of
     * tools to the toolbar should add a separator <em>after</em> the
     * group.
     *
     * @return  the toolbar
     */
    public JToolBar getToolBar() {
        return toolBar;
    }

    /**
     * Returns this window's "Window" menu.
     *
     * @return  the window menu
     */
    public JMenu getWindowMenu() {
        return windowMenu;
    }

    /**
     * Returns this window's "Help" menu.
     *
     * @return  the help menu
     */
    public JMenu getHelpMenu() {
        return helpMenu;
    }

    /**
     * Sets the in-window text which heads up the main display area.
     *
     * @param   text  heading text
     */
    public void setMainHeading( String text ) {
        headingLabel.setText( text );
    }

    /**
     * Returns the container which should be used for the main user 
     * component(s) in this window.  It will have a BorderLayout.
     *
     * @return  main container
     */
    public JPanel getMainArea() {
        return mainArea;
    }

    /**
     * Returns the container which should be used for controls and buttons.
     * This will probably be placed below the mainArea.
     *
     * @return  control container
     */
    public JPanel getControlPanel() {
        return controlPanel;
    }

    /**
     * Returns the panel containing the body of this window.
     * This contains most of the content but not the parts that have to
     * go at the top and bottom like the toolbar and progress bar.
     *
     * @return   body panel
     */
    public JComponent getBodyPanel() {
        return overPanel;
    }

    /**
     * Obtains simple confirmation from a user.
     * This is just a convenience method wrapping a JOptionPane invocation.
     *
     * @param  message  confirmation text for user
     * @param  title    confirmation window title
     * @return  true  iff the user provides positive confirmation
     */
    public boolean confirm( Object message, String title ) {
        return JOptionPane.showConfirmDialog( this, message, title,
                                              JOptionPane.OK_CANCEL_OPTION )
            == JOptionPane.OK_OPTION;
    }

    /**
     * Constructs and returns an action which allows a user to save a supplied
     * table to disk.
     *
     * @param  dataType  short textual description of the table content
     * @param  tSrc   table supplier object
     */
    public Action createSaveTableAction( final String dataType,
                                         final TableSource tSrc ) {
        return new BasicAction( "Save as Table", ResourceIcon.SAVE,
                                "Save " + dataType + " as a table to disk" ) {
            public void actionPerformed( ActionEvent evt ) {

                /* Get a table saver window for saving this kind of data.
                 * If there is a previously opened one not currently
                 * on screen then use that, since it will contain state
                 * that may be useful to the user.
                 * Otherwise create one and save it for later. */
                SaveTableQueryWindow saveWindow = saveWindows.get( dataType );
                if ( saveWindow == null || saveWindow.isVisible() ) {
                    StarTableOutput sto =
                        ControlWindow.getInstance().getTableOutput();
                    saveWindow =
                        new SaveTableQueryWindow( "Save " + dataType +
                                                  " as table",
                                                  AuxWindow.this, sto, false );
                    saveWindows.put( dataType, saveWindow );
                }

                /* Pop up the window thereby inviting the user to save the
                 * table. */
                saveWindow.setTableSource( tSrc );
                saveWindow.setVisible( true );
            }
        };
    }

    /**
     * Constructs and returns an action which allows a user to import a
     * supplied table into TOPCAT as if it had just been loaded.
     *
     * @param   dataType  short textual description of the table content
     * @param   tSrc     table supplier object
     * @param   label    TocpatModel identifier label
     */
    public Action createImportTableAction( String dataType,
                                           final TableSource tSrc,
                                           final String label ) {
        return new BasicAction( "Import as Table", ResourceIcon.IMPORT,
                                "Import " + dataType + " into " + 
                                TopcatUtils.getApplicationName() + 
                                " as a new table" ) {
            public void actionPerformed( ActionEvent evt ) {
                StarTable table = tSrc.getStarTable();
                if ( table == null ) {
                    JOptionPane
                   .showMessageDialog( AuxWindow.this, "No table to export",
                                       "Export Failure",
                                       JOptionPane.ERROR_MESSAGE );
                    return;
                }

                /* Ensure the table is random access, since TOPCAT requires
                 * this. */
                try {
                    table = Tables.randomTable( table );
                }
                catch ( IOException e ) {
                    ErrorDialog.showError( AuxWindow.this,
                                           "Table Conversion Error", e );
                    return;
                }
                catch ( OutOfMemoryError e ) {
                    TopcatUtils.memoryError( e );
                    return;
                }

                /* Load into TOPCAT. */
                ControlWindow.getInstance().addTable( table, label, true );
            }
        };
    }
 
    public Image getIconImage() {
        return ResourceIcon.TOPCAT_LOGO.getImage();
    }

    /**
     * It beeps.
     */
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Returns a new border which features a given title.
     *
     * @param  title  window title
     * @return  border
     */
    public static Border makeTitledBorder( String title ) {
        return BorderFactory
              .createTitledBorder( BorderFactory
                                  .createLineBorder( Color.BLACK ),
                                   title );
    }

    /**
     * Locates one window 'after' another one - probably a bit lower and
     * to the right.  The second window is repositioned relative to the
     * first one.
     * 
     * @param   first   first window, or <code>null</code>
     * @param   second  second window
     */
    public static void positionAfter( Component first, Window second ) {

        /* Only attempt anything if the two windows exist on the same
         * display device. */
        GraphicsConfiguration gc = second.getGraphicsConfiguration();
        if ( first == null || gc.equals( first.getGraphicsConfiguration() ) ) {

            /* Work out the position of the first window. */
            Point pos = null; 
            if ( first != null ) {
                pos = first.getLocation();
            }
            if ( pos == null ) {
                pos = new Point( 20, 20 );
            }

            /* Set a new position relative to that. */
            pos.x += 60;
            pos.y += 60;

            /* As long as we won't go outside the bounds of the screen,
             * reposition the second window accordingly. */
            // This code, though well-intentioned, doesn't do anything very
            // useful since the bounds of the new window are typically
            // both zero (because it's not been posted to the screen yet
            // or something).  Not sure what to do about this.
            Rectangle newloc = new Rectangle( second.getBounds() );
            newloc.setLocation( pos );
            Rectangle screen = gc.getBounds();
            // The Rectangle.contains(Rectangle) method is no good here -
            // always returns false for height/width = 0 for some reason.
            if ( screen.x <= newloc.x &&
                 screen.y <= newloc.y &&
                 ( screen.x + screen.width ) >= ( newloc.x + newloc.width ) &&
                 ( screen.y + screen.height ) >= ( newloc.y + newloc.height ) ){
                second.setLocation( pos );
            }
        }
    }

    /**
     * Recursively calls {@link java.awt.Component#setEnabled} on a component
     * and (if it is a container) any of the components it contains.
     *
     * @param  comp  top-level component to enable/disable
     * @param  enabled  whether to enable or disable it
     */
    public static void recursiveSetEnabled( Component comp, boolean enabled ) {
        if ( comp.isFocusable() ) {
            comp.setEnabled( enabled );
        }
        if ( comp instanceof Container ) {
            Component[] subComps = ((Container) comp).getComponents();
            for ( int i = 0; i < subComps.length; i++ ) {
                Component subComp = subComps[ i ];
                if ( ! ( subComp instanceof JLabel ) ) {
                    recursiveSetEnabled( subComp, enabled );
                }
            }
        }
    }

    /**
     * Returns an Icon suitable for labelling all the windows in this
     * application.
     *
     * @return  badge icon, or null if there is none
     */
    private static Icon getBadge() {

        /* Get the Starlink logo and scale it to the right size. */
        final Icon starlinkIcon = 
            new ImageIcon( ResourceIcon.STAR_LOGO.getImage()
                          .getScaledInstance( -1, 34, Image.SCALE_SMOOTH ) );

        /* Get a fade factor. */
        long fadeStart =
            new GregorianCalendar( 2006, GregorianCalendar.APRIL, 1 )
           .getTimeInMillis();
        long fadeEnd =
            new GregorianCalendar( 2006, GregorianCalendar.SEPTEMBER, 1 )
           .getTimeInMillis();
        long now = new GregorianCalendar().getTimeInMillis();
        double fade =
            (double) ( now - fadeStart ) / (double) ( fadeEnd - fadeStart );

        /* Calculate corresponding rendering constants. */
        final float alpha = 1f - Math.max( 0f, Math.min( 1f, (float) fade ) );
        final boolean visible = alpha > 0f;
        final Composite fadeComposite = visible
            ? AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha )
            : null;

        /* Construct and return an icon based on the Starlink logo but
         * appropriately faded. */
        return new Icon() {
            public int getIconHeight() {
                return visible ? starlinkIcon.getIconHeight() : 0;
            }
            public int getIconWidth() {
                return visible ? starlinkIcon.getIconWidth() : 0;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                if ( visible ) {
                    if ( g instanceof Graphics2D ) {
                        Graphics2D g2 = (Graphics2D) g;
                        Composite origComposite = g2.getComposite();
                        g2.setComposite( fadeComposite );
                        starlinkIcon.paintIcon( c, g2, x, y );
                        g2.setComposite( origComposite );
                    }
                }
            }
        };
    }

    /**
     * Returns a component containing logos for the various organisations
     * which have sponsored TOPCAT development.
     *
     * @return   logo bar
     */
    public static Component getSponsorLogos() {
        Box box = Box.createHorizontalBox();
        int igap = 20;
        box.add( Box.createHorizontalGlue() );
        JLabel starLogo = new JLabel( ResourceIcon.STAR_LOGO );
        starLogo.setToolTipText( "Starlink project: "
                               + "http://www.starlink.ac.uk/" );
        box.add( starLogo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel agLogo = new JLabel( ResourceIcon.ASTROGRID_LOGO );
        agLogo.setToolTipText( "AstroGrid project: "
                             + "http://www.astrogrid.org/" );
        box.add( agLogo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel stfcLogo = new JLabel( ResourceIcon.STFC_LOGO );
        stfcLogo.setToolTipText( "Science and Technology Facilities Council: "
                               + "http://www.stfc.ac.uk/" );
        box.add( stfcLogo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel brisLogo = new JLabel( ResourceIcon.BRISTOL_LOGO );
        brisLogo.setToolTipText( "Bristol University, Astrophysics group: "
                               + "http://www.star.bristol.ac.uk/" );
        box.add( brisLogo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel votechLogo = new JLabel( ResourceIcon.VOTECH_LOGO );
        votechLogo.setToolTipText( "VO-Tech project: "
                                 + "http://www.eurovotc.org/" );
        box.add( votechLogo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel gavoLogo = new JLabel( ResourceIcon.GAVO_LOGO );
        gavoLogo.setToolTipText( "German Astrophysical Virtual Observatory: "
                               + "http://www.g-vo.org/" );
        box.add( gavoLogo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel esaLogo = new JLabel( ResourceIcon.ESA_LOGO );
        esaLogo.setToolTipText( "European Space Agency: "
                              + "http://www.esa.int/" );
        box.add( esaLogo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel fp7Logo = new JLabel( ResourceIcon.EU_LOGO );
        fp7Logo.setToolTipText( "EU Seventh Framework Programme: "
                              + "http://ec.europa.eu/research/fp7/" );
        box.add( fp7Logo );
        box.add( Box.createHorizontalStrut( igap ) );
        JLabel europlanetLogo = new JLabel( ResourceIcon.EUROPLANET_LOGO );
        europlanetLogo.setToolTipText( "Europlanet 2024 RI: "
                                     + "https://www.europlanet-society.org/"
                                     + "europlanet-2024-ri/" );
        box.add( europlanetLogo );
        return box;
    }

    /**
     * Implementation of actions for this class.
     */
    private class AuxAction extends BasicAction {

        AuxAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( this == closeAct ) {
                dispose();
            }
            else if ( this == exitAct ) {
                ControlWindow.getInstance().exit( true );
            }
            if ( this == controlAct ) {
                ControlWindow.getInstance().makeVisible();
            }
            else if ( this == aboutAct ) {
                TopcatUtils.showAbout( AuxWindow.this );
            }
        }
    }
}
