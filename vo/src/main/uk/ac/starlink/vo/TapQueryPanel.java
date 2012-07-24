package uk.ac.starlink.vo;

import adql.parser.ParseException;
import adql.parser.Token;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
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

    private URL serviceUrl_;
    private Thread metaFetcher_;
    private Thread capFetcher_;
    private AdqlValidator validator_;
    private ParseException parseError_;
    private final ParseTextArea textPanel_;
    private final TableSetPanel tmetaPanel_;
    private final TapCapabilityPanel tcapPanel_;
    private final JLabel serviceLabel_;
    private final JLabel countLabel_;
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
                String text = textPanel_.getText();
                if ( text.trim().length() > 0 ) {
                    try {
                        validator_.validate( text );
                        setParseError( null );
                    }
                    catch ( ParseException e ) {
                        setParseError( e );
                    }
                }
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
                    examplesMenu.show( comp, 0, 0 );
                }
            }
        };
        examplesAct_.putValue( Action.SHORT_DESCRIPTION,
                               "Choose from example ADQL quries" );
        tmetaPanel_.getTableSelector().addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                configureExamples();
            }
        } );

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

        /* Prepare a panel for the TAP service heading. */
        serviceLabel_ = new JLabel();
        countLabel_ = new JLabel();
        JComponent tableHeading = Box.createHorizontalBox();
        tableHeading.add( new JLabel( "Service: " ) );
        tableHeading.add( serviceLabel_ );
        tableHeading.add( Box.createHorizontalStrut( 10 ) );
        tableHeading.add( countLabel_ );

        /* Arrange the components in a split pane. */
        final JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        JComponent servicePanel = new JPanel( new BorderLayout() );
        servicePanel.add( tableHeading, BorderLayout.NORTH );
        servicePanel.add( tmetaPanel_, BorderLayout.CENTER );
        adqlPanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "ADQL Text" ) );
        servicePanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Table Metadata" ) );
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
     * Sets a short text string describing the TAP service used by this panel.
     *
     * @param  serviceHeading  short, human-readable label for the
     *         service this panel relates to
     */
    public void setServiceHeading( String serviceHeading ) {
        serviceLabel_.setText( serviceHeading );
    }

    /**
     * Reload table and capability metadata from the server.
     */
    public void reload() {
        if ( serviceUrl_ != null ) {
            setServiceUrl( serviceUrl_.toString() );
        }
    }

    /**
     * Sets the service URL for the TAP service used by this panel.
     * Calling this will initiate an asynchronous attempt to fill in
     * service metadata from the service at the given URL.
     *
     * @param  serviceUrl  base URL for a TAP service
     */
    public void setServiceUrl( final String serviceUrl ) {

        /* Prepare the URL where we can find the TableSet document. */
        final URL url;
        try {
            url = new URL( serviceUrl );
        }
        catch ( MalformedURLException e ) {
            return;
        }
        serviceUrl_ = url;

        /* Dispatch a request to acquire the table metadata from
         * the service. */
        setTables( null );
        tmetaPanel_.showFetchProgressBar( "Fetching Table Metadata" );
        metaFetcher_ = new Thread( "Table metadata fetcher" ) {
            public void run() {
                final Thread fetcher = this;
                final TableMeta[] tableMetas;
                try {
                    tableMetas = TapQuery.readTableMetadata( url );
                }
                catch ( final Exception e ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( fetcher == metaFetcher_ ) {
                                tmetaPanel_.showFetchFailure( url + "/tables",
                                                              e );
                            }
                        }
                    } );
                    return;
                }

                /* On success, install this information in the GUI. */
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( fetcher == metaFetcher_ ) {
                            setTables( tableMetas );
                        }
                    }
                } );
            }
        };
        metaFetcher_.setDaemon( true );
        metaFetcher_.start();

        /* Dispatch a request to acquire the service capability information
         * from the service. */
        tcapPanel_.setCapability( null );
        capFetcher_ = new Thread( "Table capability fetcher" ) {
            public void run() {
                final Thread fetcher = this;
                final TapCapability cap;
                try {
                    cap = TapQuery.readTapCapability( url );
                }
                catch ( final Exception e ) {
                    logger_.warning( "Failed to acquire TAP service capability "
                                   + "information" );
                    return;
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( fetcher == capFetcher_ ) {
                            tcapPanel_.setCapability( cap );
                            configureExamples();
                        }
                    }
                } );
            }
        };
        capFetcher_.setDaemon( true );
        capFetcher_.start();
    }

    /**
     * Sets the metadata panel to display a given set of table metadata.
     *
     * @param  tmetas  table metadata list; null if no metadata is available
     */
    private void setTables( TableMeta[] tmetas ) {

        /* Populate table metadata JTable. */
        tmetaPanel_.setTables( tmetas );

        /* Set validator. */
        validator_ = new AdqlValidator( tmetas );

        /* Display number of tables. */
        String countText;
        if ( tmetas == null ) {
            countText = "";
        }
        else if ( tmetas.length == 1 ) {
            countText = "(1 table)";
        }
        else {
            countText = "(" + tmetas.length + " tables)";
        }
        countLabel_.setText( countText );
        configureExamples();
    }

    /**
     * Works with the known table and service metadata currently displayed
     * to set up example queries.
     */
    public void configureExamples() {
        String lang = tcapPanel_.getQueryLanguage();
        TapCapability tcap = tcapPanel_.getCapability();
        TableMeta[] tables = tmetaPanel_.getTables();
        TableMeta table = tmetaPanel_.getSelectedTable();
        for ( int ie = 0; ie < exampleActs_.length; ie++ ) {
            AdqlExampleAction exAct = exampleActs_[ ie ];
            String adql =
                exAct.getExample().getText( true, lang, tcap, tables, table );
            exAct.setAdqlText( adql );
        }
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
    private void setParseError( ParseException parseError ) {
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

        private Rectangle errorRect_;
        private final Color highlighter_;

        /**
         * Constructor.
         */
        public ParseTextArea() {
            super( new PlainDocument() );
            highlighter_ = new Color( 0x40ff0000, true );
        }

        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            Color col0 = g.getColor();
            g.setColor( highlighter_ );
            if ( errorRect_ != null ) {
                g.fillRect( errorRect_.x, errorRect_.y,
                            errorRect_.width, errorRect_.height );
            }
            g.setColor( col0 );
        }

        /**
         * Sets the parse error whose location to highlight.
         *
         * @param  perr  parse error, or null if there is none
         */
        public void setParseError( ParseException perr ) {
            Rectangle er = toRectangle( perr );
            if ( ( er == null && errorRect_ != null ) ||
                 ( er != null && ! er.equals( errorRect_ ) ) ) {
                if ( er != null ) {
                    repaint( er );
                }
                if ( errorRect_ != null ) {
                    repaint( errorRect_ );
                }
            }
            errorRect_ = er;
        }

        /**
         * Indicates the coordinates of a rectangle on this text area
         * corresponding to the token indicated by a parse error.
         *
         * @param  perr  parse error
         * @return   rectangle coordinates of error-causing token
         */
        private Rectangle toRectangle( ParseException perr ) {
            if ( perr == null ) {
                return null;
            }
            Rectangle r0 =
                toRectangle( perr.getBeginLine(), perr.getBeginColumn() );
            Rectangle r1 =
                toRectangle( perr.getEndLine(), perr.getEndColumn() + 1 );
            if ( r0 == null || r1 == null ) {
                return null;
            }
            else {
                r0.add( r1 );
                return r0;
            }
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
