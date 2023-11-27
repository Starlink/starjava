package uk.ac.starlink.topcat.vizier;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.AbstractTableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.vo.DoubleValueField;
import uk.ac.starlink.vo.SkyPositionEntry;

/**
 * TableLoadDialogue which directly interrogtes the VizieR catalogue service.
 *
 * @author   Mark Taylor
 * @author   Thomas Boch
 * @since    19 Oct 2009
 */
public class VizierTableLoadDialog extends AbstractTableLoadDialog {

    private final ContentCoding coding_;
    private JComboBox<String> serverSelector_;
    private SkyPositionEntry skyEntry_;
    private DoubleValueField srField_;
    private JComboBox<Object> colSelector_;
    private JComboBox<String> maxSelector_;
    private VizierMode[] vizModes_;
    private JTabbedPane tabber_;
    private JRadioButton allButt_;
    private JRadioButton coneButt_;
    private Set<VizierMode> dataReadSet_;
    private VizierInfo vizinfo_;
    private final static ValueInfo SR_INFO =
        new DefaultValueInfo( "Radius", Double.class, "Search Radius" );
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.vizier" );

    /**
     * URLs known to host a standard VizieR service.
     * This list is subject to change, but not very often.
     * The definitive list is available from
     * <a href="http://vizier.u-strasbg.fr/vizier/mirrors.gml"
     *         >http://vizier.u-strasbg.fr/vizier/mirrors.gml</a>.
     */
    public static final String[] SERVER_URLS = new String[] {
        // Default site.
        "https://vizier.u-strasbg.fr/",

        // Mirrors.
        "http://vizier.nao.ac.jp/",
        "https://vizier.cfa.harvard.edu/",
        "https://vizier.iucaa.in/",
        "http://vizier.china-vo.org/",
        "http://vizier.idia.ac.za/",

        // This one is listed, but at time of writing the certificate
        // is not recognised, so don't include it for now.
        // "https://vizier.inasan.ru/vizier/",

        // Experimental/beta services (no guarantees).
        "http://viz-beta.u-strasbg.fr/",
        "http://cdsarc.u-strasbg.fr/",
    };

    /**
     * Constructor.
     */
    public VizierTableLoadDialog() {
        super( "VizieR Catalogue Service",
               "Access the VizieR library"
             + " of published astronomical catalogues" );
        coding_ = ContentCoding.GZIP;
        setIcon( ResourceIcon.VIZIER );
    }

    protected Component createQueryComponent() {

        /* Server panel. */
        JComponent serverBox = Box.createHorizontalBox();
        serverBox.add( new JLabel( "Server: " ) );
        serverSelector_ = new JComboBox<>( SERVER_URLS );
        serverSelector_.setSelectedIndex( 0 );
        serverSelector_.setEditable( true );
        serverBox.add( new ShrinkWrapper( serverSelector_ ) );
        serverBox.add( Box.createHorizontalGlue() );
        serverSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                updateServer();
            }
        } );

        /* Rows panel. */
        JComponent rowsBox = Box.createVerticalBox();
        ButtonGroup rowgrp = new ButtonGroup();
        allButt_ = new JRadioButton( "All Rows" );
        coneButt_ = new JRadioButton( "Cone Selection" );
        rowgrp.add( allButt_ );
        rowgrp.add( coneButt_ );
        Box coneLine = Box.createHorizontalBox();
        coneLine.add( coneButt_ );
        coneLine.add( Box.createHorizontalGlue() );
        rowsBox.add( coneLine );
        skyEntry_ = new SkyPositionEntry( "J2000" );
        srField_ = DoubleValueField.makeSizeDegreesField( SR_INFO );
        skyEntry_.addField( srField_ );
        Box skyLine = Box.createHorizontalBox();
        skyLine.add( Box.createHorizontalStrut( 20 ) );
        skyLine.add( skyEntry_ );
        rowsBox.add( skyLine );
        Box allLine = Box.createHorizontalBox();
        allLine.add( allButt_ );
        allLine.add( Box.createHorizontalGlue() );
        rowsBox.add( allLine );
        ActionListener rowsListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                skyEntry_.setEnabled( ! isAllRows() );
            }
        };
        coneButt_.addActionListener( rowsListener );
        allButt_.addActionListener( rowsListener );
        coneButt_.setSelected( true );
        maxSelector_ = new JComboBox<>( new String[] {
            "10000", "50000", "100000", "unlimited",
        } );
        maxSelector_.setEditable( true );
        maxSelector_.setSelectedIndex( 1 );
        Box maxLine = Box.createHorizontalBox();
        maxLine.add( new JLabel( "Maximum Row Count: " ) );
        maxLine.add( new ShrinkWrapper( maxSelector_ ) );
        maxLine.add( Box.createHorizontalGlue() );
        rowsBox.add( maxLine );

        /* Columns panel. */
        JComponent colsBox = Box.createVerticalBox();
        Box colLine = Box.createHorizontalBox();
        colLine.add( new JLabel( "Output Columns: " ) );
        colSelector_ = new JComboBox<>( new ColsSpec[] {
            new ColsSpec( "standard", encodeArg( "-out.add", "_RAJ,_DEJ,_r" ),
                                      encodeArg( "-out.add", "_RAJ,_DEJ" ) ),
            new ColsSpec( "default", encodeArg( "-out", "*" ) ),
            new ColsSpec( "all", encodeArg( "-out.all" ) ),
        } );
        colSelector_.setEditable( true );
        colSelector_.setSelectedIndex( 0 );
        colLine.add( colSelector_ );
        colLine.add( Box.createHorizontalGlue() );
        colsBox.add( colLine );

        /* Vizier query modes. */
        vizModes_ = new VizierMode[] {
            new CategoryVizierMode( this ),
            new WordVizierMode( this ),
            new SurveyVizierMode(),
            new MissionVizierMode(),
        };

        /* Tab pane, which presents one of the modes at any one time. */
        dataReadSet_ = new HashSet<VizierMode>();
        tabber_ = new JTabbedPane( JTabbedPane.TOP );
        for ( int iv = 0; iv < vizModes_.length; iv++ ) {
            VizierMode vizMode = vizModes_[ iv ];
            JComponent container = new JPanel( new BorderLayout() );
            container.setBorder( BorderFactory
                                .createEmptyBorder( 5, 5, 5, 5 ) );
            container.add( vizMode.getComponent(), BorderLayout.CENTER );
            tabber_.add( vizMode.getName(), container );
        }
        tabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                readData( getCurrentMode() );
            }
        } );

        /* Keep action enabledness up to date. */
        tabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                updateReady();
            }
        } );
        addTargetActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateReady();
            }
        } );
        addTargetCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateReady();
            }
        } );
        ListSelectionListener readySelListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                updateReady();
            }
        };
        for ( int i = 0; i < vizModes_.length; i++ ) {
            vizModes_[ i ].getQueryableTable().getSelectionModel()
                          .addListSelectionListener( readySelListener );
        }
        updateReady();

        /* Listen for double-clicks or Enter keystrokes and interpret these
         * as submit actions. */
        final Action submitAct = getSubmitAction();
        final ActionEvent actEvt = new ActionEvent( this, 0, "viz-invoke" );
        MouseListener mousey = new MouseAdapter() {
            public void mouseClicked( MouseEvent evt ) {
                if ( evt.getClickCount() == 2 ) {
                    submitAct.actionPerformed( actEvt );
                }
            }
        };
        KeyStroke enterKey = KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 );
        for ( int i = 0; i < vizModes_.length; i++ ) {
            JTable table = vizModes_[ i ].getQueryableTable();
            table.addMouseListener( mousey );
            table.getInputMap().put( enterKey, "invoke-viz" );
            table.getActionMap().put( "invoke-viz", submitAct );
        }

        /* Arrange to initialise the visual state with asynchronous reads
         * from the server when the window is first displayed. */
        final JPanel queryPanel = new JPanel( new BorderLayout() );
        queryPanel.addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {
                queryPanel.removeAncestorListener( this );
                updateServer();
            }
            public void ancestorMoved( AncestorEvent evt ) {
            }
            public void ancestorRemoved( AncestorEvent evt ) {
            }
        } );

        /* Place components. */
        Box controlsBox = Box.createVerticalBox();
        controlsBox.add( serverBox );
        controlsBox.add( rowsBox );
        controlsBox.add( colsBox );
        Box logoBox = Box.createVerticalBox();
        logoBox.add( new JLabel( ResourceIcon.VIZIER_LOGO ) );
        logoBox.add( Box.createVerticalGlue() );
        Box topLine = Box.createHorizontalBox();
        topLine.add( controlsBox );
        topLine.add( Box.createHorizontalGlue() );
        topLine.add( logoBox );
        queryPanel.add( topLine, BorderLayout.NORTH );
        queryPanel.add( tabber_, BorderLayout.CENTER );

        /* Cosmetics. */
        Border lineBorder = BorderFactory.createLineBorder( Color.BLACK );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        serverBox.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
            "VizieR Server" ) );
        rowsBox.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
            "Row Selection" ) );
        colsBox.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
            "Column Selection" ) );
        tabber_.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
            "Catalogue Selection" ) );
        tabber_.setPreferredSize( new Dimension( 500, 330 ) );
        queryPanel.setBorder( gapBorder );
        return queryPanel;
    }

    public boolean isAvailable() {
        return true;
    }

    /**
     * Indicates whether the row selector has been filled in by the user to
     * to represent all catalogue rows, or just a cone.
     *
     * @return   true  for all rows, false for a cone
     */
    private boolean isAllRows() {
        return allButt_.isSelected();
    }

    /**
     * Returns the search target, as presented to the VizieR server's
     * <code>-c</code> parameter.
     *
     * @return   formatted target string; empty string for all rows
     */
    public String getTarget() {
        if ( isAllRows() ) {
            return "";
        }
        else {
            double ra = skyEntry_.getRaDegreesField().getValue();
            double dec = skyEntry_.getDecDegreesField().getValue();
            String sra = CgiQuery.formatDouble( ra );
            String sdec = CgiQuery.formatDouble( dec );
            char sd0 = sdec.charAt( 0 );
            if ( sd0 != '+' && sd0 != '-' ) {
                sdec = '+' + sdec;
            }
            return sra + " " + sdec;
        }
    }

    /**
     * Returns the radius string in degrees.
     *
     * @return  formatted radius string; empty string for all rows
     */
    public String getRadius() {
        return isAllRows() ? ""
                           : CgiQuery.formatDouble( srField_.getValue() );
    }

    public TableLoader createTableLoader() {

        /* Identify the catalogue to query. */
        JTable catTable = getCurrentMode().getQueryableTable();
        int irow = catTable.getSelectedRow();
        Queryable queryable =
            (Queryable)
            ((ArrayTableModel) catTable.getModel()).getItems()[ irow ];
        if ( queryable == null ) {
            throw new IllegalArgumentException( "No catalogue selected" );
        }

        /* Construct the query URL. */
        StringBuffer ubuf = new StringBuffer();
        ubuf.append( new CgiQuery( vizinfo_.getBaseUrl().toString() )
                    .addArgument( "-source", queryable.getQuerySource() )
                    .toString() );
        ubuf.append( encodeArg( "-oc.form", "dec" ) );
        ubuf.append( encodeArg( "-out.meta", "Dhul" ) );
        boolean allRows = isAllRows();
        if ( ! allRows ) {
            ubuf.append( encodeArg( "-c", getTarget() ) );
            ubuf.append( encodeArg( "-c.rd", getRadius() ) );
        }
        Object colsSpec = colSelector_.getSelectedItem();
        if ( colsSpec instanceof ColsSpec ) {
            ubuf.append( ((ColsSpec) colsSpec).getArgs( allRows ) );
        }
        else if ( colsSpec instanceof String ) {
            String txt = ((String) colsSpec);
            ComboBoxModel<?> colModel = colSelector_.getModel();
            assert colModel instanceof DefaultComboBoxModel;
            if ( colModel instanceof DefaultComboBoxModel &&
                 ((DefaultComboBoxModel) colModel).getIndexOf( txt ) < 0 ) {
                colSelector_.addItem( txt );
            }
            txt = txt.trim();
            if ( txt.length() > 0 ) {
                if ( txt.charAt( 0 ) != '&' ) {
                    ubuf.append( '&' );
                }
                ubuf.append( txt );
            }
        }
        String maxrows = (String) maxSelector_.getSelectedItem();
        ubuf.append( encodeArg( "-out.max", maxrows ) );
        final URL url;
        try {
            url = new URL( ubuf.toString() );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URL: " + ubuf )
                 .initCause( e );
        }

        /* Construct and return an object which will submit the query. */
        final String id = queryable.getQueryId();
        return new TableLoader() {
            public String getLabel() {
                return "Vizier-" + id;
            }
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                logger_.info( "VizieR query: " + url );
                final TableSequence tseq =
                    tfact.makeStarTables( new URLDataSource( url, coding_ ),
                                          "votable" );
                return new TableSequence() {
                    int ix_;
                    public StarTable nextTable() throws IOException {
                        StarTable table;
                        boolean isEmpty;
                        do {
                            table = tseq.nextTable();
                            if ( table == null ) {
                                return null;
                            }
                            ix_++;
                            isEmpty = table.getRowCount() == 0;
                            if ( isEmpty ) {
                                String name = table.getName();
                                logger_.info( "Ignoring VizieR table "
                                            + name == null ? ( "#" + ix_ )
                                                           : name
                                            + " with no rows" );
                            }
                        } while ( isEmpty );
                        String name = table.getName();
                        if ( name != null ) {
                            name = name.replace( '/', '_' );
                            table.setParameter( new DescribedValue( SOURCE_INFO,
                                                                    name ) );
                        }
                        return table;
                    }
                };
            }
        };
    }

    /**
     * Returns the VizierMode currently visible (selected by the user).
     *
     * @return  current mode
     */
    private VizierMode getCurrentMode() {
        return vizModes_[ tabber_.getSelectedIndex() ];
    }

    public boolean isReady() {
        boolean hasCatalog =
            getCurrentMode().getQueryableTable().getSelectedRow() >= 0;
        return hasCatalog && hasTarget();
    }

    /**
     * Looks at the contents of the server selector, and makes the 
     * appropriate updates to objects which need a VizerInfo.
     */
    private void updateServer() {
        Object server = serverSelector_.getSelectedItem();
        if ( server instanceof String ) {
            URL url;
            try {
                url = new URL( (String) server + "viz-bin/votable" );
            }
            catch ( MalformedURLException e ) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            VizierInfo vizinfo =
                new VizierInfo( getQueryComponent(), url, coding_ );
            vizinfo_ = vizinfo;
            for ( int i = 0; i < vizModes_.length; i++ ) {
                vizModes_[ i ].setVizierInfo( vizinfo );
            }
            dataReadSet_.clear();
            readData( getCurrentMode() );
        }
    }

    /**
     * Indicates whether enough information has been filled in by the user
     * to specify a search on a given catalogue.
     *
     * @return  true if a queryable query can take place; false if more
     *          info needs to be entered
     */
    public boolean hasTarget() {
        try {
            getTarget();
            getRadius();
            return true;
        }
        catch ( RuntimeException e ) {
            return false;
        }
    }

    /**
     * Adds a listener which will be notified when the user-selected target 
     * may change.
     *
     * @param  listener  listener to add
     */
    public void addTargetActionListener( ActionListener listener ) {
        skyEntry_.addActionListener( listener );
        allButt_.addActionListener( listener );
        coneButt_.addActionListener( listener );
    }

    /**
     * Removes a listener previously added by {@link #addTargetActionListener}.
     *
     * @param  listener   listener to remove
     */
    public void removeTargetActionListener( ActionListener listener ) {
        skyEntry_.removeActionListener( listener );
        allButt_.addActionListener( listener );
        coneButt_.addActionListener( listener );
    }

    /**
     * Adds a listener which will be notified when the text entered in
     * the user-selected target boxes may change.
     *
     * @param  listener  the listener to add
     */
    public void addTargetCaretListener( CaretListener listener ) {
        skyEntry_.addCaretListener( listener );
    }

    /**
     * Removes a listener previously added by {@link #addTargetCaretListener}.
     *
     * @param  listener  listener to remove
     */
    public void removeTargetCaretListener( CaretListener listener ) {
        skyEntry_.removeCaretListener( listener );
    }

    /**
     * Turns a name, value pair into a string which can be appended to
     * a URL to specify a query argument and its value.
     * An ampersand is prepended.
     *
     * @param  name   arg name
     * @param  value  arg value
     * @return   &amp;name=value (properly encoded)
     */
    public static String encodeArg( String name, String value ) {
        return new StringBuffer()
              .append( '&' )
              .append( urlEncode( name ) )
              .append( '=' )
              .append( urlEncode( value ) )
              .toString();
    }

    /**
     * Turns a text string into a string which can be appended to a URL
     * to specify a value-less query argument.
     * An ampersand is prepended.
     *
     * @param  txt  arg text
     * @return  &amp;txt (properly encoded)
     */
    public static String encodeArg( String txt ) {
        return new StringBuffer()
              .append( '&' )
              .append( urlEncode( txt ) )
              .toString();
    }

    /**
     * URL-encodes text.
     *
     * @param   txt   text to encode
     * @return  encoded text
     */
    static String urlEncode( String txt ) {
        try {
            return URLEncoder.encode( txt, "utf-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            assert false;
            return txt;
        }
    }

    /**
     * Tell the given mode to update its state by reading from the server, 
     * if it hasn't already done so since it got its current VizInfo.
     *
     * @param  mode to update
     */
    private void readData( VizierMode mode ) {
        if ( dataReadSet_.add( mode ) ) {
            mode.readData();
        }
    }

    /**
     * Specifies the selection of what columns are required from a query.
     */
    private static class ColsSpec {
        private final String name_;
        private final String argsCone_;
        private final String argsAllRows_;

        /**
         * Constructs a ColsSpec which does the same thing whether the
         * query is for all rows or a cone.
         *
         * @param  name   spec name
         * @param  args   arg string for query URL
         */
        ColsSpec( String name, String args ) {
            this( name, args, args );
        }

        /**
         * Constructs a ColsSpec which behaves differently for all-rows 
         * and cone-only queries.
         *
         * @param  name  spec name
         * @param  argsCone  arg string for query URL in cone search case
         * @param  argsAllRows  arg string for query URL in all rows case
         */
        ColsSpec( String name, String argsCone, String argsAllRows ) {
            name_ = name;
            argsCone_ = argsCone;
            argsAllRows_ = argsAllRows;
        }

        /**
         * Returns the string to append to a query URL to indicate this
         * column selection.
         *
         * @param   allRows  true for an all-row query, false for a cone
         * @param   vizier server query URL fragment
         */
        String getArgs( boolean allRows ) {
            return allRows ? argsAllRows_ : argsCone_ ;
        }

        /**
         * Returns this specification's name.
         */
        public String toString() {
            return name_;
        }
    }
}
