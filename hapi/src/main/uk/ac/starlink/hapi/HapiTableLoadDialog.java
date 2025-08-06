package uk.ac.starlink.hapi;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.AbstractTableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.IOConsumer;

/**
 * TableLoadDialog for working with HAPI services.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public class HapiTableLoadDialog extends AbstractTableLoadDialog {

    private final Consumer<URL> docUrlHandler_;
    private HapiBrowser browser_;
    private JMenu menu_;
    private Action[] toolActs_;
    private boolean failOnLimit_;
    private static final Icon ICON_TRUE_ALL = createIcon( "trueAll.png" );
    private static final Icon ICON_FALSE_ALL = createIcon( "falseAll.png" );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /**
     * No-arg constructor.
     */
    public HapiTableLoadDialog() {
        this( null );
    }

    /**
     * Constructor using a URL handler.
     *
     * @param  docUrlHandler  handler for documentation URLs,
     *                        typically displays in a browser;
     *                        may be null
     */
    @SuppressWarnings("this-escape")
    public HapiTableLoadDialog( Consumer<URL> docUrlHandler ) {
        super( "HAPI Query",
               "Load time series using " +
               "Heliophysics Data Application Programmer's Interface service" );
        docUrlHandler_ = docUrlHandler;
        setIconUrl( HapiTableLoadDialog.class.getResource( "hapi.png" ) );
    }

    protected Component createQueryComponent() {

        menu_ = new JMenu( "HAPI" );

        JMenu serverMenu = new JMenu( "Server List" );
        ButtonGroup serverButtGroup = new ButtonGroup();
        for ( ServerListing slist : ServerListing.LISTINGS ) {
            final ServerListing serverListing = slist;
            Action act = new AbstractAction( serverListing.toString() ) {
                public void actionPerformed( ActionEvent evt ) {
                    new Thread( () -> {
                        ServerMeta[] servers = serverListing.getServers();
                        SwingUtilities.invokeLater( () -> {
                            browser_.setServers( servers );
                        } );
                    }, "HAPI service list loader" ).start();
                }
            };
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            serverButtGroup.add( menuItem );
            serverMenu.add( menuItem );
        }
        menu_.add( serverMenu );

        JMenu formatMenu = new JMenu( "Streaming Format" );
        ButtonGroup formatButtGroup = new ButtonGroup();
        for ( String fmt : new String[] { null, "csv", "binary" } ) {
            final String format = fmt;
            Action act = new AbstractAction( fmt == null ? "Auto" : fmt ) {
                public void actionPerformed( ActionEvent evt ) {
                    browser_.setFormat( format );
                }
            };
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            formatButtGroup.add( menuItem );
            formatMenu.add( menuItem );
        }
        menu_.add( formatMenu );

        JCheckBoxMenuItem failmodeMenuItem =
            new JCheckBoxMenuItem( "Fail on Limit" );
        failmodeMenuItem.addActionListener( evt -> {
            failOnLimit_ = failmodeMenuItem.isSelected();
        } );
        menu_.add( failmodeMenuItem );

        JCheckBoxMenuItem headerMenuItem =
            new JCheckBoxMenuItem( "Include Header with Data" );
        headerMenuItem.addActionListener( evt -> {
            browser_.setIncludeHeader( headerMenuItem.isSelected() );
        } );
        headerMenuItem.setSelected( false );
        menu_.add( headerMenuItem );
        
        JCheckBoxMenuItem gzipMenuItem = new JCheckBoxMenuItem( "GZIP coding" );
        gzipMenuItem.setSelected( true );
        menu_.add( gzipMenuItem );
        Supplier<ContentCoding> codingSupplier =
            () -> gzipMenuItem.isSelected() ? ContentCoding.GZIP
                                            : ContentCoding.NONE;

        toolActs_ = new Action[] {
            createIncludeAllAction( false ),
            createIncludeAllAction( true ),
        };

        browser_ = new HapiBrowser( codingSupplier, docUrlHandler_ );
        browser_.addPropertyChangeListener( HapiBrowser.HAPISOURCE_PROP,
                                            evt ->{
            updateReady();
        } );
        serverMenu.getItem( 0 ).doClick();
        formatMenu.getItem( 0 ).doClick();
        return browser_;
    }

    public JMenu[] getMenus() {
        return new JMenu[] { menu_ };
    }

    public Action[] getToolbarActions() {
        return toolActs_;
    }

    public boolean isReady() {
        return browser_.getHapiSource() != null;
    }

    public TableLoader createTableLoader() {
        HapiSource hsrc = browser_.getHapiSource();
        int chunkLimit = browser_.getChunkLimit();
        boolean failOnLimit = failOnLimit_;
        if ( hsrc != null ) {
            HapiService service = browser_.getService();
            return new TableLoader() {
                public String getLabel() {
                    return hsrc.getLabel();
                }
                public TableSequence loadTables( StarTableFactory tfact )
                        throws IOException {
                    return new TableSequence() {
                        boolean isDone_;
                        public StarTable nextTable() throws IOException {
                            if ( ! isDone_ ) {
                                isDone_ = true;
                                return createHapiTable( tfact, hsrc, chunkLimit,
                                                        failOnLimit );
                            }
                            else {
                                return null;
                            }
                        }
                    };
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Returns an action that will do a blanket select or unselect
     * of all the fields for download in the currently selected dataset.
     *
     * @param  isIncluded  true for include, false for exclude
     * @return   new action
     */
    private Action createIncludeAllAction( final boolean isIncluded ) {
        Action act = new AbstractAction( ( isIncluded ? "Include" : "Exclude" )
                                       + " all fields" ) {
            public void actionPerformed( ActionEvent evt ) {
                ServicePanel sp = browser_.getServicePanel();
                if ( sp != null ) {
                    sp.setAllIncluded( isIncluded );
                }
            }
        };
        act.putValue( Action.SHORT_DESCRIPTION,
                      ( isIncluded ? "Select" : "Unselect" )
                    + " all rows of displayed dataset for download" );
        act.putValue( Action.SMALL_ICON,
                      isIncluded ? ICON_TRUE_ALL : ICON_FALSE_ALL );
        return act;
    }

    /**
     * Creates an icon from a local resource.
     *
     * @param  name  file name relative to package namespace
     * @return   new icon
     */
    static Icon createIcon( String name ) {
        URL url = HapiTableLoadDialog.class.getResource( name );
        if ( url != null ) {
            return new ImageIcon( url );
        }
        else {
            assert false;
            return null;
        }
    }

    /**
     * Loads a cached table from a HAPI data URL.
     * According to the supplied <code>failOnLimit</code> parameter,
     * in case the download request involves too many chunks,
     * the load will either fail with an IOException or
     * succeed with an overflow indicator attached to the table and
     * a message written through the logging system.
     *
     * @param  tfact  table factory
     * @param  hsrc   table source
     * @param  chunkLimit  maximum number of chunks to load
     * @param  failOnLimit  action if chunk limit is exceeded
     * @return  random-access table
     */
    private static StarTable createHapiTable( StarTableFactory tfact,
                                              HapiSource hsrc,
                                              int chunkLimit,
                                              boolean failOnLimit )
            throws IOException {
        RowStore rowStore = tfact.getStoragePolicy().makeRowStore();
        final boolean[] overflowFlag = new boolean[ 1 ];
        IOConsumer<String> limitCallback = msg -> {
            overflowFlag[ 0 ] = true;
            if ( failOnLimit ) {
                throw new IOException( msg );
            }
            else {
                logger_.warning( msg + " - table truncated" );
            }
        };
        hsrc.streamHapi( rowStore, chunkLimit, limitCallback );
        StarTable table = rowStore.getStarTable();
        if ( overflowFlag[ 0 ] ) {
            table.setParameter( new DescribedValue( Tables.QUERY_STATUS_INFO,
                                                    "OVERFLOW" ) );
        }
        table.setURL( hsrc.getStandaloneUrl() );
        return table;
    }
}
