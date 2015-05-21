package uk.ac.starlink.vo;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ParseException;
import adql.parser.Token;
import adql.parser.TokenMgrError;
import adql.query.TextPosition;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * Panel for display of a TAP query for a given TAP service.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2011
 */
public class TapQueryPanel extends JPanel {

    private final TableSetPanel tmetaPanel_;
    private final TapCapabilityPanel tcapPanel_;
    private final JToggleButton syncToggle_;
    private final Action examplesAct_;
    private final Action parseErrorAct_;
    private final AdqlExampleAction[] exampleActs_;
    private final JMenu serviceExampleMenu_;
    private final JTabbedPane textTabber_;
    private final CaretListener caretForwarder_;
    private final List<CaretListener> caretListeners_;
    private final Map<ParseTextArea,UndoManager> undoerMap_;
    private final AdqlTextAction clearAct_;
    private final AdqlTextAction interpolateColumnsAct_;
    private final AdqlTextAction interpolateTableAct_;;
    private final Action undoAct_;
    private final Action redoAct_;
    private final Action addTabAct_;
    private final Action copyTabAct_;
    private final Action removeTabAct_;
    private TapServiceKit serviceKit_;
    private Throwable parseError_;
    private AdqlValidator.ValidatorTable[] extraTables_;
    private AdqlValidator validator_;
    private ParseTextArea textPanel_;
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
     * @param   examples   list of example queries to be made available
     *          from the examples menu
     * @param   urlHandler  handles URLs that the user clicks on; may be null
     */
    public TapQueryPanel( AdqlExample[] examples, UrlHandler urlHandler ) {
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
        tcapPanel_ = new TapCapabilityPanel();

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
        undoAct_ = new AbstractAction( "Undo" ) {
            public void actionPerformed( ActionEvent evt ) {
                try {
                    undoer_.undo();
                }
                catch ( CannotUndoException e ) {
                }
                updateUndoState();
            }
        };
        configureAction( undoAct_, "undo.gif",
                         "Undo most recent edit to text" );
        undoAct_.putValue( Action.ACCELERATOR_KEY, UNDO_KEYS[ 0 ] );
        redoAct_ = new AbstractAction( "Redo" ) {
            public void actionPerformed( ActionEvent evt ) {
                try {
                    undoer_.redo();
                }
                catch ( CannotUndoException e ) {
                }
                updateUndoState();
            }
        };
        configureAction( redoAct_, "redo.gif",
                         "Redo most recently undone edit to text" );
        redoAct_.putValue( Action.ACCELERATOR_KEY, REDO_KEYS[ 0 ] );

        /* Actions for adding and removing text entry tabs. */
        addTabAct_ = new AbstractAction( "Add Tab" ) {
            public void actionPerformed( ActionEvent evt ) {
                addTextTab();
            }
        };
        configureAction( addTabAct_, "add_tab.gif",
                         "Add a new ADQL entry tab" );
        copyTabAct_ = new AbstractAction( "Copy Tab" ) {
            public void actionPerformed( ActionEvent evt ) {
                String text = textPanel_ == null ? null : textPanel_.getText();
                addTextTab();
                textPanel_.setText( text );
            }
        };
        configureAction( copyTabAct_, "copy_tab.gif",
                         "Add a new ADQL entry tab, with initial content "
                       + "copied from the currently visible one" );
        removeTabAct_ = new AbstractAction( "Remove Tab" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( textTabber_.getTabCount() > 1 ) {
                    undoerMap_.remove( textPanel_ );
                    textTabber_.removeTabAt( textTabber_.getSelectedIndex() );
                }
                updateTabState();
            }
        };
        configureAction( removeTabAct_, "remove_tab.gif",
                         "Delete the currently visible ADQL entry tab" );

        /* Button for selecting sync/async mode of query. */
        syncToggle_ = new JCheckBox( "Synchronous", true );
        syncToggle_.setToolTipText( "Determines whether the TAP query will "
                                  + "be carried out in synchronous (selected) "
                                  + "or asynchronous (unselected) mode" );

        /* Action to display parse error text. */
        parseErrorAct_ = new AbstractAction( "Parse Errors" ) {
            public void actionPerformed( ActionEvent evt ) {
                showParseError();
            }
        };
        configureAction( parseErrorAct_, "error.gif",
                         "Show details of error parsing current query text" );

        /* Action to clear text in ADQL panel. */
        clearAct_ = new AdqlTextAction( "Clear", true );
        configureAction( clearAct_, "clear.gif",
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
        configureAction( interpolateTableAct_, "insert_table.gif",
                         "Insert name of currently selected table "
                       + "into ADQL text panel" );
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
        configureAction( interpolateColumnsAct_, "insert_columns.gif",
                         "Insert names of currently selected columns "
                       + "into ADQL text panel" );
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
        final JPopupMenu examplesMenu = new JPopupMenu( "Examples" );
        int nex = examples.length;
        exampleActs_ = new AdqlExampleAction[ nex ];
        for ( int ie = 0; ie < nex; ie++ ) {
            exampleActs_[ ie ] = new AdqlExampleAction( examples[ ie ] );
            examplesMenu.add( exampleActs_[ ie ] );
        }
        serviceExampleMenu_ = new JMenu( "Service-Specific" );
        examplesMenu.add( serviceExampleMenu_ );
        setDaliExamples( null );
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
                    examplesMenu.show( comp, 0, 0 );
                }
            }
        };
        examplesAct_.putValue( Action.SHORT_DESCRIPTION,
                               "Choose from example ADQL queries" );

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
        Box buttLine = Box.createHorizontalBox();
        buttLine.setBorder( BorderFactory.createEmptyBorder( 0, 2, 2, 0 ) );
        buttLine.add( syncToggle_ );
        buttLine.add( Box.createHorizontalStrut( 5 ) );
        buttLine.add( new JButton( examplesAct_ ) );
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( toolbar );

        /* Place components on ADQL panel. */
        JComponent adqlPanel = new JPanel( new BorderLayout() );
        adqlPanel.setPreferredSize( new Dimension( 500, 200 ) );
        adqlPanel.add( buttLine, BorderLayout.NORTH );
        adqlPanel.add( textTabber_, BorderLayout.CENTER );
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
        splitter.setResizeWeight( 0.8 );
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
     * Indicates whether synchronous operation has been selected.
     *
     * @return   true for sync, false for async
     */
    public boolean isSynchronous() {
        return syncToggle_.isSelected();
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
                }
                public void showResult( TapCapability tcap ) {
                    tcapPanel_.setCapability( tcap );
                    tmetaPanel_.setCapability( tcap );
                }
                public void showError( IOException error ) {
                    logger_.log( Level.WARNING,
                                 "Failed to acquire TAP service capability "
                               + "information: " + error, error );
                }
            } );
            serviceKit.acquireExamples( new ResultHandler<DaliExample[]>() {
                public boolean isActive() {
                    return serviceKit_ == serviceKit;
                }
                public void showWaiting() {
                    setDaliExamples( null );
                }
                public void showResult( DaliExample[] examples ) {
                    setDaliExamples( examples );
                }
                public void showError( IOException error ) {
                    logger_.info( "No TAP examples: " + error );
                    setDaliExamples( new DaliExample[ 0 ] );
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
            addTabAct_, copyTabAct_, removeTabAct_,
            clearAct_, undoAct_, redoAct_,
            interpolateTableAct_, interpolateColumnsAct_,
            parseErrorAct_,
        };
    }

    /**
     * Adds a listener for changes to the text in the displayed ADQL
     * text entry panel.
     * This uses a CaretListener rather than (what might be more
     * appropriate) DocumentListener because the DocumentListener
     * interface looks too hairy.
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
     * Works with the known table and service metadata currently displayed
     * to set up example queries.
     */
    private void configureExamples() {
        String lang = tcapPanel_.getQueryLanguageName();
        TapCapability tcap = tcapPanel_.getCapability();
        SchemaMeta[] schemas = tmetaPanel_.getSchemas();
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
        for ( int ie = 0; ie < exampleActs_.length; ie++ ) {
            AdqlExampleAction exAct = exampleActs_[ ie ];
            String adql =
                exAct.getExample().getText( true, lang, tcap, tables, table );
            exAct.setAdqlText( adql );
        }
    }

    /**
     * Sets the list of examples to be included in the service-specific
     * examples sub-menu.
     *
     * @param  examples  example list, may be null
     */
    private void setDaliExamples( DaliExample[] examples ) {
        JMenu menu = serviceExampleMenu_;
        menu.removeAll();
        menu.setEnabled( examples != null && examples.length > 0 );
        if ( examples != null ) {
            for ( DaliExample example : examples ) {
                String name = example.getName();
                String adql = example.getGenericParameters().get( "QUERY" );
                AdqlTextAction act = new AdqlTextAction( name, true );
                act.setAdqlText( adql );
                menu.add( act );
            }
        }
        tmetaPanel_.setHasExamples( examples != null && examples.length > 0 );
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
        CaretEvent evt = new CaretEvent( textPanel_ ) {
            public int getDot() {
                return dot;
            }
            public int getMark() {
                return mark;
            }
        };
        for ( CaretListener l : caretListeners_ ) {
            l.caretUpdate( evt );
        }
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
    }

    /**
     * Returns a validator for validating ADQL text.
     *
     * @return  ADQL validator
     */
    private AdqlValidator getValidator() {
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
            TapLanguage tapLang = null;
            validator_ = AdqlValidator.createValidator( vtables, tapLang );
        }
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
        for ( SchemaMeta smeta : schemas ) {
            final String sname = smeta.getName();
            for ( TableMeta tmeta : smeta.getTables() ) {
                final TableMeta tmeta0 = tmeta;
                vtList.add( new AdqlValidator.ValidatorTable() {
                    public String getSchemaName() {
                        return sname;
                    }
                    public String getTableName() {
                        return tmeta0.getName();
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
                         * column information is available, but also schedule
                         * a request to acquire the column information
                         * and subsequently have another go at validating
                         * the ADQL; this method will have been called by
                         * a current validation attempt, but next time it
                         * should be able to report the columns. */
                        else {
                            TapServiceKit serviceKit =
                                tmetaPanel_.getServiceKit();
                            if ( serviceKit != null ) {
                                serviceKit.onColumns( tmeta0, new Runnable() {
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
        parseErrorAct_.setEnabled( parseError != null );
    }

    /**
     * Adds description text and an icon to a given action.
     *
     * @param   act  action
     * @param   iconName  name of icon filename in this class's directory
     * @param   description  description of action
     */
    private static void configureAction( Action act, String iconName,
                                         String description ) {
        if ( iconName != null ) {
            URL iconUrl = TapQueryPanel.class.getResource( iconName );
            if ( iconUrl != null ) {
                act.putValue( Action.SMALL_ICON, new ImageIcon( iconUrl ) );
            }
        }
        if ( description != null ) {
            act.putValue( Action.SHORT_DESCRIPTION, description );
        }
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
            super( new PlainDocument() );
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
