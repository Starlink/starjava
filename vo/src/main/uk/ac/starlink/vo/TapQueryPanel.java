package uk.ac.starlink.vo;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.grammar.ParseException;
import adql.parser.grammar.Token;
import adql.parser.grammar.TokenMgrError;
import adql.query.TextPosition;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.AuthStatus;
import uk.ac.starlink.auth.AuthType;
import uk.ac.starlink.auth.UserInterface;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Panel for display of a TAP query for a given TAP service.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2011
 */
public class TapQueryPanel extends JPanel {

    private final TableSetPanel tmetaPanel_;
    private final TapCapabilityPanel tcapPanel_;
    private final Action examplesAct_;
    private final Action parseErrorAct_;
    private final Action parseFixupAct_;
    private final JPopupMenu examplesMenu_;
    private final JMenu daliExampleMenu_;
    private final JTabbedPane textTabber_;
    private final JComponent controlBox_;
    private final TapExampleLine exampleLine_;
    private final CaretListener caretForwarder_;
    private final List<CaretListener> caretListeners_;
    private final Map<ParseTextArea,UndoManager> undoerMap_;
    private final AuthAction authAct_;
    private final AdqlTextAction clearAct_;
    private final AdqlTextAction interpolateColumnsAct_;
    private final AdqlTextAction interpolateTableAct_;;
    private final Action undoAct_;
    private final Action redoAct_;
    private final Action addTabAct_;
    private final Action copyTabAct_;
    private final Action removeTabAct_;
    private final Action titleTabAct_;
    private final DelegateAction prevExampleAct_;
    private final DelegateAction nextExampleAct_;
    private TapServiceKit serviceKit_;
    private Throwable parseError_;
    private AdqlValidator.ValidatorTable[] extraTables_;
    private AdqlValidator validator_;
    private ParseTextArea textPanel_;
    private int iCustomExampleMenu_;
    private int iTab_;
    private UndoManager undoer_;

    private static final KeyStroke[] UNDO_KEYS = new KeyStroke[] {
        KeyStroke.getKeyStroke( KeyEvent.VK_Z, Event.CTRL_MASK ),
        KeyStroke.getKeyStroke( KeyEvent.VK_Z, Event.META_MASK ),
    };
    private static final KeyStroke[] REDO_KEYS = new KeyStroke[] {
        KeyStroke.getKeyStroke( KeyEvent.VK_Z, Event.CTRL_MASK
                                             | Event.SHIFT_MASK ),
        KeyStroke.getKeyStroke( KeyEvent.VK_Z, Event.META_MASK
                                             | Event.SHIFT_MASK ),
        KeyStroke.getKeyStroke( KeyEvent.VK_Y, Event.CTRL_MASK ),
    };
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   urlHandler  handles URLs that the user clicks on; may be null
     */
    public TapQueryPanel( UrlHandler urlHandler ) {
        super( new BorderLayout() );

        /* Prepare a panel for table metadata display. */
        tmetaPanel_ = new TableSetPanel( urlHandler );
        tmetaPanel_.addPropertyChangeListener( TableSetPanel.SCHEMAS_PROPERTY,
                                               new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                validator_ = null;
                validateAdql();
            }
        } );

        /* Prepare a panel to contain service capability information. */
        authAct_ = new AuthAction();
        tcapPanel_ = new TapCapabilityPanel( authAct_ );
        tcapPanel_.addPropertyChangeListener(
                TapCapabilityPanel.LANGUAGE_PROPERTY,
                evt -> {
                    validateAdql();
                    AdqlVersion adqlVers =
                        tcapPanel_.getSelectedLanguage().getAdqlVersion();
                    tmetaPanel_.setAdqlVersion( adqlVers );
                } );

        /* Prepare a component to contain user-entered ADQL text. */
        textTabber_ = new JTabbedPane();
        textTabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                updateTextTab();
            }
        } );

        /* Support ADQL text undo/redo. */
        undoerMap_ = new HashMap<ParseTextArea,UndoManager>();
        UndoManager undoer0 = new UndoManager();
        undoer0.setLimit( 0 );
        undoerMap_.put( null, undoer0 );
        undoer_ = undoer0;
        undoAct_ = new AbstractAction( "Undo", ResourceIcon.ADQL_UNDO ) {
            public void actionPerformed( ActionEvent evt ) {
                try {
                    undoer_.undo();
                }
                catch ( CannotUndoException e ) {
                }
                updateUndoState();
            }
        };
        undoAct_.putValue( Action.SHORT_DESCRIPTION,
                           "Undo most recent edit to text" );
        undoAct_.putValue( Action.ACCELERATOR_KEY, UNDO_KEYS[ 0 ] );
        redoAct_ = new AbstractAction( "Redo", ResourceIcon.ADQL_REDO ) {
            public void actionPerformed( ActionEvent evt ) {
                try {
                    undoer_.redo();
                }
                catch ( CannotUndoException e ) {
                }
                updateUndoState();
            }
        };
        redoAct_.putValue( Action.SHORT_DESCRIPTION,
                           "Redo most recently undone edit to text" );
        redoAct_.putValue( Action.ACCELERATOR_KEY, REDO_KEYS[ 0 ] );

        /* Actions for adding and removing text entry tabs. */
        addTabAct_ = new AbstractAction( "Add Tab", ResourceIcon.ADQL_ADDTAB ) {
            public void actionPerformed( ActionEvent evt ) {
                addTextTab();
            }
        };
        addTabAct_.putValue( Action.SHORT_DESCRIPTION,
                             "Add a new ADQL entry tab" );
        copyTabAct_ = new AbstractAction( "Copy Tab",
                                          ResourceIcon.ADQL_COPYTAB ) {
            public void actionPerformed( ActionEvent evt ) {
                String text = textPanel_ == null ? null : textPanel_.getText();
                addTextTab();
                textPanel_.setText( text );
            }
        };
        copyTabAct_.putValue( Action.SHORT_DESCRIPTION,
                              "Add a new ADQL entry tab, with initial content "
                            + "copied from the currently visible one" );
        removeTabAct_ = new AbstractAction( "Remove Tab",
                                            ResourceIcon.ADQL_REMOVETAB ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( textTabber_.getTabCount() > 1 ) {
                    undoerMap_.remove( textPanel_ );
                    textTabber_.removeTabAt( textTabber_.getSelectedIndex() );
                }
                updateTextTab();
            }
        };
        removeTabAct_.putValue( Action.SHORT_DESCRIPTION,
                                "Delete the currently visible ADQL entry tab" );
        titleTabAct_ = new AbstractAction( "Title Tab",
                                           ResourceIcon.ADQL_TITLETAB ) {
            public void actionPerformed( ActionEvent evt ) {
                int itab = textTabber_.getSelectedIndex();
                if ( itab >= 0 ) {
                    Object response =
                        JOptionPane
                       .showInputDialog( TapQueryPanel.this,
                                         "Tab Title", "Re-title Edit Tab",
                                         JOptionPane.QUESTION_MESSAGE, null,
                                         null, textTabber_.getTitleAt( itab ) );
                    if ( response instanceof String ) {
                        String title = ((String) response).trim();
                        if ( title.length() > 0 ) {
                            textTabber_.setTitleAt( itab, title );
                        }
                    }
                }
            }
        };
        titleTabAct_.putValue( Action.SHORT_DESCRIPTION,
                               "Re-title the currently visible "
                             + "ADQL entry tab" );

        /* Action to display parse error text. */
        parseErrorAct_ = new AbstractAction( "Parse Errors",
                                             ResourceIcon.ADQL_ERROR ) {
            public void actionPerformed( ActionEvent evt ) {
                showParseError();
            }
        };
        parseErrorAct_.putValue( Action.SHORT_DESCRIPTION,
                                 "Show details of error "
                               + "parsing current query text" );

        /* Option for making simple fixes to ADQL. */
        parseFixupAct_ = new AbstractAction( "Fix Errors",
                                             ResourceIcon.ADQL_FIXUP ) {
            public void actionPerformed( ActionEvent evt ) {
                String fixText = getFixedAdql();
                if ( fixText != null ) {
                    textPanel_.setText( fixText );
                }
            }
        };
        parseFixupAct_.putValue( Action.SHORT_DESCRIPTION,
                                 "Attempt a quick fix of common "
                               + "ADQL syntax errors" );

        /* Action to clear text in ADQL panel. */
        clearAct_ = new AdqlTextAction( "Clear", true );
        clearAct_.putValue( Action.SMALL_ICON, ResourceIcon.ADQL_CLEAR );
        clearAct_.putValue( Action.SHORT_DESCRIPTION,
                            "Delete currently visible ADQL text from editor" );
        clearAct_.setAdqlText( "" );
        clearAct_.setEnabled( false );

        /* Prepare to warn listeners when the visible ADQL text changes. */
        caretListeners_ = new ArrayList<CaretListener>();
        caretForwarder_ = new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                clearAct_.setEnabled( textPanel_.getDocument()
                                                .getLength() > 0 );
                validateAdql();
                for ( CaretListener l : caretListeners_ ) {
                    l.caretUpdate( evt );
                }
            }
        };

        /* Action to insert table name. */
        interpolateTableAct_ = new AdqlTextAction( "Insert Table", false );
        interpolateTableAct_.putValue( Action.SMALL_ICON,
                                       ResourceIcon.ADQL_INSERTTABLE );
        interpolateTableAct_.putValue( Action.SHORT_DESCRIPTION,
                                       "Insert name of currently selected "
                                     + "table into ADQL text panel" );
        tmetaPanel_.addPropertyChangeListener( TableSetPanel
                                              .TABLE_SELECTION_PROPERTY,
                                               new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                TableMeta tmeta = tmetaPanel_.getSelectedTable();
                String txt = tmeta == null ? null : tmeta.getName();
                interpolateTableAct_.setAdqlText( txt );
            }
        } );

        /* Action to insert column names. */
        interpolateColumnsAct_ = new AdqlTextAction( "Insert Columns", false );
        interpolateColumnsAct_.putValue( Action.SMALL_ICON,
                                         ResourceIcon.ADQL_INSERTCOLS );
        interpolateColumnsAct_.putValue( Action.SHORT_DESCRIPTION,
                                         "Insert names of currently selected "
                                       + "columns into ADQL text panel" );
        tmetaPanel_.addPropertyChangeListener( TableSetPanel
                                              .COLUMNS_SELECTION_PROPERTY,
                                               new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                ColumnMeta[] cmetas = tmetaPanel_.getSelectedColumns();
                StringBuffer sbuf = new StringBuffer();
                for ( int i = 0; i < cmetas.length; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( cmetas[ i ].getName() );
                }
                String txt = sbuf.length() == 0 ? null : sbuf.toString();
                interpolateColumnsAct_.setAdqlText( txt );
            }
        } );

        /* Action for examples menu. */
        daliExampleMenu_ = new JMenu( "Service-Provided" );
        setDaliExamples( null );
        examplesMenu_ = new JPopupMenu( "Examples" );
        examplesMenu_.add( createExampleMenu( "Basic",
                                              AbstractAdqlExample
                                             .createSomeExamples() ) );
        iCustomExampleMenu_ = examplesMenu_.getSubElements().length;
        examplesMenu_.add( daliExampleMenu_ );
        examplesMenu_.add( createExampleMenu( "TAP_SCHEMA",
                                              AbstractAdqlExample
                                             .createTapSchemaExamples() ) );
        examplesMenu_.add( createExampleMenu( "ObsTAP",
                                              DataModelAdqlExample
                                             .createObsTapExamples() ) );
        examplesMenu_.add( createExampleMenu( "RegTAP",
                                              DataModelAdqlExample
                                             .createRegTapExamples() ) );
        examplesAct_ = new AbstractAction( "Examples" ) {
            public void actionPerformed( ActionEvent evt ) {
                Object src = evt.getSource();
                if ( src instanceof Component ) {
                    Component comp = (Component) src;

                    /* The example text will be affected by various aspects
                     * of the state of this component; they must be configured
                     * appropriately before display.  It would be possible
                     * to keep them up to date at all times by monitoring
                     * the state of constituent components, but it needs
                     * a lot of listeners and plumbing. */
                    configureExamples();
                    examplesMenu_.show( comp, 0, 0 );
                }
            }
        };
        examplesAct_.putValue( Action.SHORT_DESCRIPTION,
                               "Choose from example ADQL queries" );

        /* Set up the line which displays control and display of
         * ADQL example text and information. */
        exampleLine_ = new TapExampleLine( urlHandler );
        prevExampleAct_ =
            new DelegateAction( null, ComboBoxBumper.DEC_ICON,
                                "Previous example in group" );
        nextExampleAct_ =
            new DelegateAction( null, ComboBoxBumper.INC_ICON,
                                "Next example in group" );
        addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                exampleLine_.setExample( null, null );
                prevExampleAct_.setDelegate( null );
                nextExampleAct_.setDelegate( null );
            }
        } );
        JComponent exampleBox = Box.createHorizontalBox();
        exampleBox.setBorder( BorderFactory.createEmptyBorder( 4, 0, 2, 0 ) );
        exampleBox.add( new JButton( examplesAct_ ) );
        exampleBox.add( Box.createHorizontalStrut( 5 ) );
        exampleBox.add( createIconButton( prevExampleAct_ ) );
        exampleBox.add( Box.createHorizontalStrut( 2 ) );
        exampleBox.add( createIconButton( nextExampleAct_ ) );
        exampleBox.add( Box.createHorizontalStrut( 5 ) );
        exampleBox.add( exampleLine_ );

        /* Prepare initial ADQL entry text panel. */
        addTextTab();
        setParseError( null );

        /* Controls for ADQL text panel. */
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable( false );
        toolbar.setBorderPainted( false );
        for ( Action act : getEditActions() ) {
            toolbar.add( act );
        }
        controlBox_ = Box.createHorizontalBox();
        Box buttLine = Box.createHorizontalBox();
        buttLine.setBorder( BorderFactory.createEmptyBorder( 0, 2, 2, 0 ) );
        buttLine.add( controlBox_ );
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( toolbar );

        /* Place components on ADQL panel. */
        JComponent adqlPanel = new JPanel( new BorderLayout() );
        adqlPanel.add( buttLine, BorderLayout.NORTH );
        adqlPanel.add( textTabber_, BorderLayout.CENTER );
        adqlPanel.add( exampleBox, BorderLayout.SOUTH );
        JComponent qPanel = new JPanel( new BorderLayout() );
        qPanel.add( tcapPanel_, BorderLayout.NORTH );
        qPanel.add( adqlPanel, BorderLayout.CENTER );

        /* Arrange the components in a split pane. */
        final JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        JComponent servicePanel = new JPanel( new BorderLayout() );
        servicePanel.add( tmetaPanel_, BorderLayout.CENTER );
        adqlPanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "ADQL Text" ) );
        servicePanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Metadata" ) );
        tcapPanel_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Service Capabilities" ) );
        splitter.setTopComponent( servicePanel );
        splitter.setBottomComponent( qPanel );
        servicePanel.setPreferredSize( new Dimension( 500, 500 ) );
        adqlPanel.setPreferredSize( new Dimension( 500, 200 ) );
        splitter.setResizeWeight( 0.6 );
        add( splitter, BorderLayout.CENTER );
    }

    /**
     * Returns the panel used to hold and display the TAP capability
     * information.
     *
     * @return  capability display panel
     */
    public TapCapabilityPanel getCapabilityPanel() {
        return tcapPanel_;
    }

    /**
     * Returns the text currently entered in the ADQL text component.
     *
     * @return  adql text supplied by user
     */
    public String getAdql() {
        return textPanel_.getText().replaceFirst( "\\s*\\Z", "" );
    }

    /**
     * Sets the TAP service access used by this panel.
     * Calling this will unconditionally initiate an asynchronous attempt
     * to fill in service metadata from the given service.
     *
     * @param  serviceKit   defines TAP service
     */
    public void setServiceKit( final TapServiceKit serviceKit ) {
        serviceKit_ = serviceKit;

        /* Outdate service-related state. */
        validator_ = null;

        if ( serviceKit == null ) {
            updateServiceKit( null );
        }
        else {

            /* Attempt initial contact with the service to establish
             * authentication, if any.  On success only, the rest of
             * this panel will be updated with the service kit.
             * On failure, leave it blank. */
            Runnable popParent = pushUiParent( this );
            boolean forceLogin = false;
            serviceKit.acquireAuthStatus( new ResultHandler<AuthStatus>() {
                public boolean isActive() {
                    return serviceKit_ == serviceKit;
                }
                public void showWaiting() {
                    String txt = "Checking Authentication...";
                    tmetaPanel_.replaceTreeComponent( new JLabel( txt ) );
                    tmetaPanel_.setAuthId( null );
                }
                public void showResult( AuthStatus authStatus ) {
                    String authId = authStatus.getIdentityString();
                    popParent.run();
                    String msg = new StringBuffer()
                        .append( authStatus.isAuthenticated()
                               ? "Authenticated as " + authId
                               : "Anonymous connection" )
                        .append( " to TAP service " )
                        .append( serviceKit.getTapService().getIdentity() )
                        .toString();
                    logger_.info( msg );
                    tmetaPanel_.setAuthId( authId );
                    authAct_.setAuthStatus( authStatus );
                    warnAuthStatus( authStatus );
                    tmetaPanel_.replaceTreeComponent( null );
                    updateServiceKit( serviceKit );
                }
                public void showError( IOException e ) {
                    popParent.run();
                    tmetaPanel_.setAuthId( null );
                    authAct_.setAuthStatus( new AuthStatus( AuthType.UNKNOWN ));
                    ErrorDialog
                   .showError( TapQueryPanel.this, "Authentication Error", e,
                               "Authentication to TAP service failed");
                    tmetaPanel_.replaceTreeComponent( null );
                }
            }, forceLogin );
        }
    }

    /**
     * Dispatch various asynchronous requests to populate the state of this
     * panel displaying characteristics of the TAP service.
     * No attempt is made to re-establish authentication.
     *
     * @param  serviceKit  service to be contacted
     */
    public void updateServiceKit( TapServiceKit serviceKit ) {

        /* Dispatch a request to acquire the table metadata from
         * the service. */
        tmetaPanel_.setServiceKit( serviceKit );

        /* Dispatch request for other information from the service. */
        if ( serviceKit != null ) {
            serviceKit.acquireCapability( new ResultHandler<TapCapability>() {
                public boolean isActive() {
                    return serviceKit_ == serviceKit;
                }
                public void showWaiting() {
                    tcapPanel_.setCapability( null );
                    tmetaPanel_.setCapability( null );
                    validator_ = null;
                }
                public void showResult( TapCapability tcap ) {
                    tcapPanel_.setCapability( tcap );
                    tmetaPanel_.setCapability( tcap );
                    validator_ = null;
                }
                public void showError( IOException error ) {
                    logger_.log( Level.WARNING,
                                 "Failed to acquire TAP service capability "
                               + "information: " + error, error );
                }
            } );
            serviceKit.acquireExamples(
                    new ResultHandler<List<Tree<DaliExample>>>() {
                public boolean isActive() {
                    return serviceKit_ == serviceKit;
                }
                public void showWaiting() {
                    setDaliExamples( null );
                }
                public void showResult( List<Tree<DaliExample>> daliExamples ) {
                    setDaliExamples( daliExamples );
                }
                public void showError( IOException error ) {
                    logger_.info( "No TAP examples: " + error );
                    setDaliExamples( null );
                }
            } );
        }
    }

    /**
     * Sets a list of extra tables available for valid queries.
     * By default ADQL validation is done on a list of tables acquired
     * by reading the service's declared table metadata,
     * but additional tables may be added for consideration using this call.
     *
     * @param   extraTables  additional tables to be passed by the validator
     */
    public void setExtraTables( AdqlValidator.ValidatorTable[] extraTables ) {
        extraTables_ = extraTables;
        validator_ = null;
        validateAdql();
    }

    /**
     * Returns an array of GUI actions related to editing the ADQL text.
     *
     * @return  edit action list
     */
    public Action[] getEditActions() {
        return new Action[] {
            addTabAct_, copyTabAct_, removeTabAct_, titleTabAct_,
            clearAct_, undoAct_, redoAct_,
            interpolateTableAct_, interpolateColumnsAct_,
            parseErrorAct_, parseFixupAct_,
        };
    }

    /**
     * Returns the action that logs in and out of the TAP service.
     *
     * @return  authentication action
     */
    public Action getAuthenticateAction() {
        return authAct_;
    }

    /**
     * Adds a given control to the line of buttons displayed at the top
     * of this panel.
     *
     * @param  comp  component to add
     */
    public void addControl( JComponent comp ) {
        controlBox_.add( comp );
    }

    /**
     * Adds a listener for changes to the text in the displayed ADQL
     * text entry panel.
     * This uses a CaretListener rather than (what might be more
     * appropriate) DocumentListener because the DocumentListener
     * interface looks too hairy, especially for use by components
     * that are themselves behaving asynchronously.
     *
     * @param  listener  listener to add
     */
    public void addCaretListener( CaretListener listener ) {
        caretListeners_.add( listener );
    }

    /**
     * Removes a listener previously added with addCaretListener.
     *
     * @param  listener  listener to remove
     */
    public void removeCaretListener( CaretListener listener ) {
        caretListeners_.remove( listener );
    }

    /**
     * Adds a submenu to the examples menu giving a list of custom ADQL
     * example queries.
     *
     * @param  menuName  name of submenu
     * @param  examples  example list
     */
    public void addCustomExamples( String menuName, AdqlExample[] examples ) {
        examplesMenu_.insert( createExampleMenu( menuName, examples ),
                              iCustomExampleMenu_++ );
        configureExamples();
    }

    /**
     * Returns the currently preferred sky position to use in examples.
     * The default implementation returns null, which means examples
     * must come up with some default themselves, but subclasses may
     * override this.
     *
     * @return  2-element (RA,Dec) array, or null for no position
     */
    public double[] getSkyPos() {
        return null;
    }

    /**
     * Creates a new menu for display of an array of ADQL example queries.
     * Menu items not only install their own ADQL in the text panel,
     * they also configure the Previous/Next actions to invoke the
     * adjacent items in the menu.
     *
     * @param  name  menu name
     * @param  examples  list of examples
     * @return   new menu
     */
    private JMenu createExampleMenu( final String name,
                                     final AdqlExample[] examples ) {
        return createExampleMenu( name,
                                  Arrays.stream( examples )
                                 .map( ex -> new Tree.Leaf<AdqlExample>( ex ) )
                                 .collect( Collectors.toList() ) );
    }

    /**
     * Creates a new menu for display of a possibly hierarchical tree
     * of ADQL example queries.
     * Menu items not only install their own ADQL in the text panel,
     * they also configure the Previous/Next actions to invoke the
     * adjacent items in the menu.
     *
     * @param  name  menu name
     * @param  examples  list of examples
     * @return   new menu
     */
    private JMenu createExampleMenu( final String name,
                                     final List<Tree<AdqlExample>> examples ) {
        int nex = (int) examples.stream().filter( Tree::isLeaf ).count();
        final AdqlExampleAction[] exActs = new AdqlExampleAction[ nex ];
        JMenu menu = new JMenu( name );
        int ileaf = 0;
        for ( Tree<AdqlExample> tree : examples ) {
            if ( tree.isLeaf() ) {
                final int iex = ileaf++;
                AdqlExample ex = tree.asLeaf().getItem();
                String label = name + " " + ( iex + 1 ) + "/" + nex;
                exActs[ iex ] = new AdqlExampleAction( ex ) {
                    @Override
                    public void actionPerformed( ActionEvent evt ) {
                        super.actionPerformed( evt );
                        exampleLine_.setExample( ex, label );
                        if ( iex > 0 ) {
                            prevExampleAct_.setDelegate( exActs[ iex - 1 ] );
                        }
                        if ( iex < nex - 1 ) {
                            nextExampleAct_.setDelegate( exActs[ iex + 1 ] );
                        }
                    }
                };
                menu.add( exActs[ iex ] );
            }
            else {
                Tree.Branch<AdqlExample> branch = tree.asBranch();
                menu.add( createExampleMenu( branch.getLabel(),
                                             branch.getChildren() ) );
            }
        }
        assert ileaf == nex;
        return menu;
    }

    /**
     * Works with the known table and service metadata currently displayed
     * to set up example queries.
     */
    private void configureExamples() {
        VersionedLanguage lang = tcapPanel_.getSelectedLanguage();
        TapCapability tcap = tcapPanel_.getCapability();
        SchemaMeta[] schemas = tmetaPanel_.getSchemas();
        double[] skyPos = getSkyPos();
        final TableMeta[] tables;
        if ( schemas != null ) {
            List<TableMeta> tlist = new ArrayList<TableMeta>();
            for ( SchemaMeta schema : schemas ) {
                tlist.addAll( Arrays.asList( schema.getTables() ) );
            }
            tables = tlist.toArray( new TableMeta[ 0 ] );
        }
        else {
            tables = null;
        }
        TableMeta table = tmetaPanel_.getSelectedTable();
        configureExamples( examplesMenu_, lang, tcap, tables, table, skyPos );
    }

    /**
     * Configures the examples displayed in the contents of a given menu.
     * The examples are configured to provide the correct text,
     * and the enabled status of the menu items is set appropriately.
     *
     * @param   menu  parent of menu items to configure
     * @param  lang  ADQL language variant
     * @param  tcap  TAP capability object
     * @param  tables  table metadata set
     * @param  table  currently selected table
     * @param  skypos  2-element array giving preferred (RA,Dec) sky position,
     *                 or null if none preferred
     * @return   number of descendents of the supplied menu that are
     *           enabled for use
     */
    private static int configureExamples( MenuElement menu,
                                          VersionedLanguage lang,
                                          TapCapability tcap,
                                          TableMeta[] tables,
                                          TableMeta table,
                                          double[] skypos ) {
        int nActive = 0;
        for ( MenuElement el : menu.getSubElements() ) {
            if ( el instanceof JMenuItem ) {
                Action act = ((JMenuItem) el).getAction();
                if ( act instanceof AdqlExampleAction ) {
                    AdqlExampleAction exAct = (AdqlExampleAction) act;
                    String adql =
                        exAct.getExample()
                       .getAdqlText( true, lang, tcap, tables, table, skypos );
                    exAct.setAdqlText( adql );
                    if ( exAct.isEnabled() ) {
                        nActive++;
                    }
                }
            }
            int nSubActive =
                configureExamples( el, lang, tcap, tables, table, skypos );
            if ( el instanceof JMenu ) {
                ((JMenu) el).setEnabled( nSubActive > 0 );
            }
            nActive += nSubActive;
        }
        return nActive;
    }

    /**
     * Sets the list of examples to be included in the service-specific
     * examples sub-menu.
     *
     * @param  examples  example list, may be null
     */
    private void setDaliExamples( List<Tree<DaliExample>> daliExamples ) {
        boolean hasExamples = daliExamples != null && daliExamples.size() > 0;
        JMenu menu = daliExampleMenu_;
        menu.removeAll();
        menu.setEnabled( hasExamples );
        if ( hasExamples ) {

            /* Turn the hierarchical structure of DaliExamples into a
             * corresponding structure of AdqlExamples. */
            List<Tree<AdqlExample>> adqlExamples =
                daliExamples
               .stream()
               .map( t -> t.map( this::daliToAdqlExample ) )
               .collect( Collectors.toList() );

            /* Create a menu from these; this call does more than simply
             * wrap the actions into a menu, so use this call and then
             * pull the menu items out into a different menu later. */
            JMenu dummyMenu = createExampleMenu( menu.getText(), adqlExamples );
            while ( dummyMenu.getItemCount() > 0 ) {
                JMenuItem item = dummyMenu.getItem( 0 );
                dummyMenu.remove( 0 );
                menu.add( item );
            }
        }
        tmetaPanel_.setHasExamples( hasExamples );
    }

    /**
     * Adapts a DaliExample to an AdqlExample.
     *
     * @param  daliEx  DALI example
     * @return  ADQL example
     */
    private AdqlExample daliToAdqlExample( DaliExample daliEx ) {
        final String name = daliEx.getName();
        final String adql = getExampleQueryText( daliEx );
        return new AbstractAdqlExample( name, null ) {
            public String getAdqlText( boolean lineBreaks,
                                       VersionedLanguage lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table, double[] skypos ) {
                return adql;
            }
            public URL getInfoUrl() {
                return daliEx.getUrl();
            }
        };
    }

    /**
     * Returns the ADQL text corresponding to the query part of an example.
     * Implementation is contentious; override it if you want.
     *
     * @param  daliEx  example object
     * @return   ADQL query text
     */
    public String getExampleQueryText( DaliExample daliEx ) {

        /* At time of writing (Aug 2015) there is dispute about how example
         * query text is correctly encoded into RDFa at the /examples
         * endpoint of a TAP service.
         * Here we take a belt and braces approach - look for a query
         * marked up in either of the two standard(?) ways. */
 
        /* If a TAP Note 1.0-style "property='query'" element is present,
         * use that. */
        String propQuery = daliEx.getProperties().get( "query" );
        if ( propQuery != null ) {
            return propQuery;
        }

        /* Otherwise, if a DALI 1.0-style generic-parameter key/value
         * property pair is present, use that. */
        String genericQuery = daliEx.getGenericParameters().get( "QUERY" );
        if ( genericQuery != null ) {
            return genericQuery;
        }

        /* Or nothing. */
        return null;
    }

    /**
     * Performs best-efforts validation on the ADQL currently visible
     * in the query text entry field, updating the GUI accordingly.
     * This validation is currently performed synchronously on the
     * Event Dispatch Thread.
     */
    private void validateAdql() {
        String text = textPanel_.getText();
        if ( text.trim().length() > 0 ) {
            AdqlValidator validator = getValidator();
            try {
                validator.validate( text );
                setParseError( null );
            }
            catch ( Throwable e ) {
                setParseError( e );
            }
        }
        else {
            setParseError( null );
        }
    }

    /**
     * Adds a new tab containing a text panel in the editing area,
     * for entry of ADQL text.
     */
    private void addTextTab() {
        ParseTextArea textPanel = new ParseTextArea();
        textPanel.setEditable( true );
        textPanel.setFont( Font.decode( "Monospaced" ) );
        InputMap inputMap = textPanel.getInputMap();
        for ( KeyStroke key : UNDO_KEYS ) {
            inputMap.put( key, undoAct_ );
        }
        for ( KeyStroke key : REDO_KEYS ) {
            inputMap.put( key, redoAct_ );
        }
        final UndoManager undoer = new UndoManager();
        textPanel.getDocument()
                 .addUndoableEditListener( new UndoableEditListener() {
            public void undoableEditHappened( UndoableEditEvent evt ) {
                undoer.addEdit( evt.getEdit() );
                updateUndoState();
            }
        } );
        undoerMap_.put( textPanel, undoer );
        String tabName = Integer.toString( ++iTab_ );
        textTabber_.addTab( tabName, new JScrollPane( textPanel ) );
        textTabber_.setSelectedIndex( textTabber_.getTabCount() - 1 );
        assert textPanel_ == textPanel;
    }

    /**
     * Updates the GUI as appropriate when the currently visible ADQL
     * text entry tab may have changed.
     */
    private void updateTextTab() {
        if ( textPanel_ != null ) {
            textPanel_.removeCaretListener( caretForwarder_ );
        }
        textPanel_ = (ParseTextArea)
                     ((JScrollPane) textTabber_.getSelectedComponent())
                    .getViewport().getView();
        textPanel_.addCaretListener( caretForwarder_ );
        undoer_ = undoerMap_.get( textPanel_ );
        Caret caret = textPanel_.getCaret();
        final int dot = caret.getDot();
        final int mark = caret.getMark();
        caretForwarder_.caretUpdate( new CaretEvent( textPanel_ ) {
            public int getDot() {
                return dot;
            }
            public int getMark() {
                return mark;
            }
        } );
        updateTabState();
        updateUndoState();
        validateAdql();
    }

    /**
     * Updates the state of undo actions,
     * invoked if the undo state may have changed.
     */
    private void updateUndoState() {
        undoAct_.setEnabled( undoer_.canUndo() );
        redoAct_.setEnabled( undoer_.canRedo() );
    }

    /**
     * Updates the state of actions related to text tab manipulation,
     * invoked if tabs may have been added or removed.
     */
    private void updateTabState() {
        copyTabAct_.setEnabled( textPanel_ != null );
        removeTabAct_.setEnabled( textPanel_ != null &&
                                  textTabber_.getTabCount() > 1 );
        titleTabAct_.setEnabled( textTabber_.getSelectedIndex() >= 0 );
    }

    /**
     * Returns a validator for validating ADQL text.
     *
     * @return  ADQL validator
     */
    private AdqlValidator getValidator() {
        VersionedLanguage vlang = tcapPanel_.getSelectedLanguage();
        assert vlang != null;
        if ( validator_ == null ) {
            List<AdqlValidator.ValidatorTable> vtList =
                new ArrayList<AdqlValidator.ValidatorTable>();
            SchemaMeta[] schemas = tmetaPanel_.getSchemas();
            if ( schemas != null ) {
                AdqlValidator.ValidatorTable[] serviceTables =
                    createValidatorTables( schemas );
                vtList.addAll( Arrays.asList( serviceTables ) );
            }
            if ( extraTables_ != null ) {
                vtList.addAll( Arrays.asList( extraTables_ ) );
            }
            AdqlValidator.ValidatorTable[] vtables =
                vtList.toArray( new AdqlValidator.ValidatorTable[ 0 ] );
            TapLanguage tapLang = vlang.getLanguage();
            validator_ = AdqlValidator.createValidator( vtables, tapLang );
        }
        validator_.setAdqlVersion( vlang.getAdqlVersion() );
        return validator_;
    }

    /**
     * Turns a list of schemas into a list of ValidatorTables.
     * These validator tables are capable of scheduling requests for
     * unavailable column metadata followed by repeat validation operations
     * (see implementation).
     *
     * @param   schemas  schema metadata objects populated with tables
     * @return  validator tables representing schema content
     */
    private AdqlValidator.ValidatorTable[]
            createValidatorTables( SchemaMeta[] schemas ) {
        List<AdqlValidator.ValidatorTable> vtList =
            new ArrayList<AdqlValidator.ValidatorTable>();
        int nNoName = 0;
        for ( SchemaMeta smeta : schemas ) {
            final String sname = smeta.getName();
            for ( TableMeta tmeta : smeta.getTables() ) {
                final TableMeta tmeta0 = tmeta;
                final String tname = tmeta0.getName();
                if ( tname != null && tname.trim().length() > 0 ) {
                    vtList.add( new AdqlValidator.ValidatorTable() {
                        public String getSchemaName() {
                            return sname;
                        }
                        public String getTableName() {
                            return tname;
                        }
                        public Collection<String> getColumnNames() {
                            ColumnMeta[] cmetas = tmeta0.getColumns();

                            /* If the table knows its columns, report them. */
                            if ( cmetas != null ) {
                                Collection<String> list = new HashSet<String>();
                                for ( ColumnMeta cmeta : cmetas ) {
                                    list.add( cmeta.getName() );
                                }
                                return list;
                            }

                            /* Otherwise, return null to indicate that no
                             * column information is available,
                             * but also schedule a request
                             * to acquire the column information
                             * and subsequently have another go at validating
                             * the ADQL; this method will have been called by
                             * a current validation attempt, but next time it
                             * should be able to report the columns. */
                            else {
                                TapServiceKit serviceKit =
                                    tmetaPanel_.getServiceKit();
                                if ( serviceKit != null ) {
                                    serviceKit.onColumns( tmeta0,
                                                          new Runnable() {
                                        public void run() {
                                            validateAdql();
                                        }
                                    } );
                                }
                                return null;
                            }
                        }
                    } );
                }
                else {
                    nNoName++;
                }
            }
        }
        if ( nNoName > 0 ) {
            logger_.warning( "Ignoring " + nNoName + " nameless tables" );
        }
        return vtList.toArray( new AdqlValidator.ValidatorTable[ 0 ] );
    }

    /**
     * Displays the current parse error, if any.
     */
    private void showParseError() {
        if ( parseError_ != null ) {
            Object msg = parseError_.getMessage();
            JOptionPane.showMessageDialog( this, msg, "ADQL Parse Error",
                                           JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Sets the parse error relating to the currently entered ADQL text,
     * possibly null.
     *
     * @param  parseError  parse error or null
     */
    private void setParseError( Throwable parseError ) {
        parseError_ = parseError;
        textPanel_.setParseError( parseError );
        boolean hasError = parseError != null;
        parseErrorAct_.setEnabled( hasError );
        final boolean hasFix = hasError && getFixedAdql() != null;
        parseFixupAct_.setEnabled( hasFix );
    }

    /**
     * Attempts to provide an improved version of the ADQL currently
     * being edited.  If such an improvement (generally, fixing common errors)
     * can be made, the improved version is returned.
     * If no such improvements can be made, for whatever reason,
     * null is returned.
     *
     * @return  improved version of ADQL being edited, or null
     */
    private String getFixedAdql() {
        String text = textPanel_.getText();
        if ( text != null || text.trim().length() > 0 ) {
            AdqlValidator validator = getValidator();
            return validator == null ? null : validator.fixup( text );
        }
        else {
            return null;
        }
    }

    /**
     * Alerts the user if the supplied status is something to be worried about.
     * Otherwise, does nothing.
     *
     * @param  status  auth status
     */
    private void warnAuthStatus( AuthStatus status ) {
        if ( ! status.isAuthenticated() &&
             status.getAuthType() == AuthType.REQUIRED ) {
            Object msg = new String[] {
                "This TAP service requires authentication, " +
                "but you are not logged in.",
                "Things probably won't work.",
            };
            JOptionPane
           .showMessageDialog( this, msg, "Not Authenticated",
                               JOptionPane.WARNING_MESSAGE );
        }
    }

    /**
     * Returns a button for a given action, with a shape suitable for a
     * little icon in a row of tools.
     *
     * @param   act  action
     * @return  icon
     */
    private static JButton createIconButton( Action act ) {
        JButton butt = new JButton( act ) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension( super.getMaximumSize().width,
                                      Integer.MAX_VALUE );
            }
        };
        butt.setMargin( new Insets( 2, 2, 2, 2 ) );
        return butt;
    }

    /**
     * Temporarily sets the parent of the default AuthManager to the
     * given component.  The return value is a callback which must be
     * run to restore the parent to its original value.
     *
     * @param  tmpParent  component to install temporarily as
     *                    AuthManager parent
     * @return  runnable that restores AuthManager to previous state
     */
    private static Runnable pushUiParent( Component tmpParent ) {
        UserInterface ui = AuthManager.getInstance().getUserInterface();
        final Component parent0 = ui.getParent();
        ui.setParent( tmpParent );
        return () -> {
            if ( ui.getParent() == tmpParent ) {
                ui.setParent( parent0 );
            }
        };
    }

    /**
     * Action which replaces the current content of the ADQL text entry
     * area with some fixed string.
     */
    private class AdqlTextAction extends AbstractAction {
        private final boolean replace_;
        private String text_;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  replace  true to replace entire contents,
         *                  false to insert at current position,
         */
        public AdqlTextAction( String name, boolean replace ) {
            super( name );
            replace_ = replace;
            setAdqlText( null );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( replace_ ) {
                textPanel_.setText( text_ );
                textPanel_.setCaretPosition( 0 );
            }
            else {
                textPanel_.insert( text_, textPanel_.getCaretPosition() );
            }
            textPanel_.requestFocusInWindow();
        }

        /**
         * Sets the text which this action will insert.
         * Enabledness is determined by whether <code>text</code> is null.
         *
         * @param  text  ADQL text
         */
        public void setAdqlText( String text ) {
            text_ = text;
            setEnabled( text != null );
        }
    }

    /**
     * AdqlTextAction based on an AdqlExample.
     */
    private class AdqlExampleAction extends AdqlTextAction {
        private final AdqlExample example_;

        /**
         * Constructor.
         *
         * @param   example  the example which this action will display
         */
        public AdqlExampleAction( AdqlExample example ) {
            super( example.getName(), true );
            putValue( SHORT_DESCRIPTION, example.getDescription() );
            example_ = example;
        }

        /**
         * Returns this action's example.
         *
         * @return  example
         */
        public AdqlExample getExample() {
            return example_;
        }

        @Override
        public void actionPerformed( ActionEvent evt ) {
            super.actionPerformed( evt );
        }
    }

    /**
     * Action which delegates some of its behaviour to an underlying and
     * dynamically set action.
     */
    private class DelegateAction extends AbstractAction {
        private Action act_;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  icon   icon
         * @param  descrip   action short_description
         */
        DelegateAction( String name, Icon icon, String descrip ) {
            super( name );
            putValue( SMALL_ICON, icon );
            putValue( SHORT_DESCRIPTION, descrip );
            setDelegate( null );
        }

        /**
         * Sets the delegate.
         *
         * @param  act  new delegate, may be null
         */
        public void setDelegate( Action act ) {
            act_ = act;
            setEnabled( act_ != null );
        }

        public void actionPerformed( ActionEvent evt ) {
            act_.actionPerformed( evt );
        }
    }

    /**
     * Action to (re)-authenticate with authenticated TAP service.
     */
    private class AuthAction extends AbstractAction {

        private AuthStatus authStatus_;
        private final TapQueryPanel tqPanel_;

        public AuthAction() {
            super( "Log In/Out" );
            setAuthStatus( AuthStatus.NO_AUTH );
            tqPanel_ = TapQueryPanel.this;
        }

        @Override
        public void actionPerformed( ActionEvent evt ) {
            relogin();
        }

        /**
         * Updates this action's state with a given status.
         *
         * @param  authStatus  new status
         */
        private void setAuthStatus( AuthStatus authStatus ) {
            authStatus_ = authStatus;
            AuthType authType = authStatus.getAuthType();
            boolean isAuth = authStatus_.isAuthenticated();
            setEnabled( ( authType == AuthType.OPTIONAL ||
                          authType == AuthType.REQUIRED )
                     && serviceKit_ != null );
            final Icon icon;
            if ( isAuth ) {
                icon = ResourceIcon.AUTH_YES;
            }
            else {
                icon = authType == AuthType.REQUIRED ? ResourceIcon.AUTH_REQNO
                                                     : ResourceIcon.AUTH_NO;
            }
            putValue( SMALL_ICON, icon );
            putValue( SHORT_DESCRIPTION,
                      isAuth ? ( "Log in again (currently " +
                                 authStatus.getAuthenticatedId() + ")" )
                             : "Log in" );
            // Note careful about changing the NAME here;
            // it is currently used as a key in TapTableLoadDialog.
        }

        /**
         * Attempts to interact with the user to login to an
         * authenticated service if possible.
         */
        private void relogin() {
            if ( serviceKit_ == null ) {
                return;
            }

            /* If we are already authenticated, prompt to discard auth. */
            AuthStatus status0 = authStatus_;
            boolean isAuth = status0.isAuthenticated();
            if ( status0.isAuthenticated() ) {
                String msg = "Log out of TAP service as "
                           + status0.getIdentityString() + "?";
                int choice =
                    JOptionPane
                   .showConfirmDialog( tqPanel_, msg, "TAP Logout",
                                       JOptionPane.OK_CANCEL_OPTION );
                if ( choice != JOptionPane.OK_OPTION ) {
                    return;
                }
            }

            /* Attempt authentication. */
            Runnable popParent = pushUiParent( tqPanel_ );
            boolean isReset = true;
            serviceKit_.acquireAuthStatus( new ResultHandler<AuthStatus>() {
                public boolean isActive() {
                    return true;
                }
                public void showError( IOException error ) {
                    popParent.run();
                    setAuthStatus( authStatus_ );
                    ErrorDialog.showError( tqPanel_, "Authentication", error );
                }
                public void showWaiting() {
                    setEnabled( false );
                }
                public void showResult( AuthStatus status ) {
                    popParent.run();
                    setAuthStatus( status );
                    warnAuthStatus( status );
                    tmetaPanel_.setAuthId( status.getIdentityString() );
                    updateServiceKit( serviceKit_ );
                }
            }, isReset );
        }
    }

    /**
     * Text area which can highlight the location of a parse error.
     */
    private class ParseTextArea extends JTextArea {

        private Rectangle[] errorRects_;
        private final Color highlighter_;

        /**
         * Constructor.
         */
        public ParseTextArea() {
            super( new CustomReplacePlainDocument() );
            highlighter_ = new Color( 0x40ff0000, true );
            errorRects_ = new Rectangle[ 0 ];
        }

        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            Color col0 = g.getColor();
            g.setColor( highlighter_ );
            if ( errorRects_.length > 0 ) {
                for ( int ir = 0; ir < errorRects_.length; ir++ ) {
                    Rectangle erect = errorRects_[ ir ];
                    g.fillRect( erect.x, erect.y, erect.width, erect.height );
                }
            }
            g.setColor( col0 );
        }

        /**
         * Sets the parse error whose location to highlight.
         *
         * @param  perr  parse error, or null if there is none
         */
        public void setParseError( Throwable perr ) {
            Rectangle[] ers = toRectangles( perr );
            if ( ! Arrays.equals( errorRects_, ers ) ) {
                errorRects_ = ers;
                repaint();
            }
        }

        /**
         * Returns zero or more rectangles on this text area which mark
         * the positions of tokens corresponding to parse errors indicated
         * by the given parse exception.
         *
         * @param  perr  parse error (may be null)
         * @return  array of error token rectangles
         */
        private Rectangle[] toRectangles( Throwable perr ) {
            List<Rectangle> rectList = new ArrayList<Rectangle>();
            if ( perr instanceof UnresolvedIdentifiersException ) {
                UnresolvedIdentifiersException uerr =
                   (UnresolvedIdentifiersException) perr;
                Rectangle rect = toRectangle( uerr );
                if ( rect != null ) {
                    rectList.add( rect );
                }
                for ( ParseException pe : uerr ) {
                    rectList.addAll( Arrays.asList( toRectangles( pe ) ) );
                }
            }
            else if ( perr instanceof ParseException ) {
                Rectangle rect = toRectangle( (ParseException) perr );
                if ( rect != null ) {
                    rectList.add( rect );
                }
            }
            else if ( perr instanceof TokenMgrError ) {
                Rectangle rect = toRectangle( (TokenMgrError) perr );
                if ( rect != null ) {
                    rectList.add( rect );
                }
            }
            else if ( perr != null ) {
                logger_.log( Level.WARNING,
                             "Unexpected parse exception: " + perr, perr );
            }
            return rectList.toArray( new Rectangle[ 0 ] );
        }

        /**
         * Indicates the coordinates of a rectangle on this text area
         * corresponding to the token indicated by a parse error.
         *
         * @param  perr  parse error (may be null)
         * @return   rectangle coordinates of error-causing token
         */
        private Rectangle toRectangle( ParseException perr ) {
            TextPosition tpos = perr == null ? null : perr.getPosition();
            if ( tpos == null ) {
                return null;
            }
            Rectangle r0 = toRectangle( tpos.beginLine, tpos.beginColumn );
            Rectangle r1 = toRectangle( tpos.endLine, tpos.endColumn );
            if ( r0 == null || r1 == null ) {
                return null;
            }
            else {
                r0.add( r1 );
                return r0;
            }
        }

        /**
         * Indicates the coordinates of a rectangle on this text area
         * corresponding to the token indicated by a TokenMgrError.
         * The coordinates are approximate; a TokenMgrError does not have
         * such complete position information as a ParseException.
         *
         * @param  tmerr  parse error
         * @return   rectangle coordinates of error-causing token
         */
        private Rectangle toRectangle( TokenMgrError tmerr ) {
            int iline = tmerr.getErrorLine();
            int icol = tmerr.getErrorColumn();
            Rectangle r0 = toRectangle( iline, icol );
            if ( icol > 0 ) {
                r0.add( toRectangle( iline, icol - 1 ) );
            }
            return r0;
        }

        /**
         * Returns the coordinates of a (1d?) rectangle corresponding to
         * a line/column position in the text document displayed by this area.
         *
         * @param  iline   text position line
         * @param  icol    text position column
         * @return   rectangle coordinates of text position
         */
        private Rectangle toRectangle( int iline, int icol ) {
            if ( iline >= 0 && icol >= 0 ) {
                Element line = getDocument().getDefaultRootElement()
                                            .getElement( iline - 1 );
                int pos = line.getStartOffset() + ( icol - 1 );
                try {
                    return modelToView( pos );
                }
                catch ( BadLocationException e ) {
                    return null;
                }
            }
            else {
                return null;
            }
        }
    }
}
