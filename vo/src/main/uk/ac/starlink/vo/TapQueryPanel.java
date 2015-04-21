package uk.ac.starlink.vo;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ParseException;
import adql.parser.Token;
import adql.parser.TokenMgrError;
import adql.query.TextPosition;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * Panel for display of a TAP query for a given TAP service.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2011
 */
public class TapQueryPanel extends JPanel {

    private TapServiceKit serviceKit_;
    private Throwable parseError_;
    private AdqlValidator.ValidatorTable[] validatorTables_;
    private final ParseTextArea textPanel_;
    private final TableSetPanel tmetaPanel_;
    private final TapCapabilityPanel tcapPanel_;
    private final JToggleButton syncToggle_;
    private final Action examplesAct_;
    private final Action parseErrorAct_;
    private final AdqlExampleAction[] exampleActs_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   examples   list of example queries to be made available
     *          from the examples menu
     */
    public TapQueryPanel( AdqlExample[] examples ) {
        super( new BorderLayout() );

        /* Prepare a panel for table metadata display. */
        tmetaPanel_ = new TableSetPanel();

        /* Prepare a panel to contain service capability information. */
        tcapPanel_ = new TapCapabilityPanel();

        /* Prepare a panel to contain user-entered ADQL text. */
        textPanel_ = new ParseTextArea();
        textPanel_.setEditable( true );
        textPanel_.setFont( Font.decode( "Monospaced" ) );
        JComponent textScroller = new JScrollPane( textPanel_ );

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
        parseErrorAct_.putValue( Action.SHORT_DESCRIPTION,
                                 "Show details of error parsing "
                               + "current query ADQL text" );
        setParseError( null );

        /* Action to clear text in ADQL panel. */
        final AdqlTextAction clearAct =
                new AdqlTextAction( "Clear",
                                    "Clear currently visible ADQL text "
                                  + "from editor" ) {
        };
        clearAct.setAdqlText( "" );
        clearAct.setEnabled( false );
        textPanel_.getDocument().addDocumentListener( new DocumentListener() {
            public void changedUpdate( DocumentEvent evt ) {
            }
            public void insertUpdate( DocumentEvent evt ) {
                changed();
            }
            public void removeUpdate( DocumentEvent evt ) {
                changed();
            }
            private void changed() {
                clearAct.setEnabled( textPanel_.getDocument().getLength() > 0 );
                validateAdql();
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
                               "Choose from example ADQL quries" );

        /* Controls for ADQL text panel. */
        Box buttLine = Box.createHorizontalBox();
        buttLine.setBorder( BorderFactory.createEmptyBorder( 0, 2, 2, 0 ) );
        buttLine.add( syncToggle_ );
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( new JButton( examplesAct_ ) );
        buttLine.add( Box.createHorizontalStrut( 5 ) );
        buttLine.add( new JButton( clearAct ) );
        buttLine.add( Box.createHorizontalStrut( 5 ) );
        buttLine.add( new JButton( parseErrorAct_ ) );

        /* Place components on ADQL panel. */
        JComponent adqlPanel = new JPanel( new BorderLayout() );
        adqlPanel.add( buttLine, BorderLayout.NORTH );
        adqlPanel.add( textScroller, BorderLayout.CENTER );
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
     * Returns the text panel used for the ADQL text entered by the user.
     *
     * @return   ADQL text entry component
     */
    public JTextComponent getAdqlPanel() {
        return textPanel_;
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
        validatorTables_ = null;

        /* Dispatch a request to acquire the table metadata from
         * the service. */
        tmetaPanel_.setServiceKit( serviceKit );

        /* Dispatch a request to acquire the service capability information
         * from the service. */
        if ( serviceKit != null ) {
            serviceKit.acquireCapability( new ResultHandler<TapCapability>() {
                public boolean isActive() {
                    return serviceKit_ == serviceKit;
                }
                public void showWaiting() {
                    tcapPanel_.setCapability( null );
                }
                public void showResult( TapCapability tcap ) {
                    tcapPanel_.setCapability( tcap );
                }
                public void showError( IOException error ) {
                    logger_.warning( "Failed to acquire TAP service capability "
                                   + "information" );
                }
            } );
        }
    }

    /**
     * Returns any extra tables available for valid queries.
     * By default ADQL validation is done on a list of tables acquired
     * by reading the service's declared table metadata.
     * Subclasses which override this method can arrange for additional
     * tables to be passed by the validator.
     * This method is called immediately prior to any validation attempt.
     * The default implementation returns an empty array.
     *
     * @return   array of additional tables to be passed by the validator
     */
    protected AdqlValidator.ValidatorTable[] getExtraTables() {
        return new AdqlValidator.ValidatorTable[ 0 ];
    }

    /**
     * Works with the known table and service metadata currently displayed
     * to set up example queries.
     */
    private void configureExamples() {
        String lang = tcapPanel_.getQueryLanguage();
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
     * Returns a validator for validating ADQL text.
     *
     * @return  ADQL validator
     */
    private AdqlValidator getValidator() {

        /* Prepare a list of table metadata objects to inform the validator
         * what tables and columns are available. */
        List<AdqlValidator.ValidatorTable> vtList =
            new ArrayList<AdqlValidator.ValidatorTable>();
        if ( validatorTables_ == null ) {
            SchemaMeta[] schemas = tmetaPanel_.getSchemas();
            if ( schemas != null ) {
                validatorTables_ = createValidatorTables( schemas );
            }
        }
        if ( validatorTables_ != null ) {
            vtList.addAll( Arrays.asList( validatorTables_ ) );
        }
        vtList.addAll( Arrays.asList( getExtraTables() ) );
        AdqlValidator.ValidatorTable[] vtables =
            vtList.toArray( new AdqlValidator.ValidatorTable[ 0 ] );

        /* Construct and return a validator. */
        return new AdqlValidator( vtables, true );
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
     * Action which replaces the current content of the ADQL text entry
     * area with some fixed string.
     */
    private class AdqlTextAction extends AbstractAction {
        private String text_;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  description   action short description
         */
        public AdqlTextAction( String name, String description ) {
            super( name );
            putValue( SHORT_DESCRIPTION, description );
            setAdqlText( null );
        }

        public void actionPerformed( ActionEvent evt ) {
            textPanel_.setText( text_ );
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
            super( example.getName(), example.getDescription() );
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
