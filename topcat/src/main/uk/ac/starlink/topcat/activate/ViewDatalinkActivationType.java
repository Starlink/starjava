package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.ColumnDataComboBox;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.DatalinkPanel;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.LinkRowPanel;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.vo.datalink.LinksDoc;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Activation type for viewing a downloaded table as a DataLink file.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public class ViewDatalinkActivationType implements ActivationType {

    public String getName() {
        return "View Datalink Table";
    }

    public String getDescription() {
        return "View a referenced DataLink table";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        DatalinkInvoker invoker = new DatalinkInvoker( tinfo );
        return new ChoiceConfigurator( new ActivatorConfigurator[] {
            new UrlDatalinkConfigurator( tinfo, invoker ) {
                @Override
                public String toString() {
                    return "Datalink Table URL";
                }
            },
            new IdDatalinkConfigurator( tinfo, invoker ) {
                @Override
                public String toString() {
                    return "Links Service";
                }
            },
        } );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasFlag( ColFlag.DATALINK )
             ? Suitability.SUGGESTED
             : Suitability.AVAILABLE;
    }

    /**
     * Loads the file/URL at a given location into a given DataLink panel.
     *
     * @param  loc  location of Datalink {links}-response file
     * @param  dlPanel   display component for successfully-loaded datalink file
     * @param  window   window in which dlPanel is hosted;
     *                  if non-null, it will be set visible on load
     * @return  outcome
     */
    public static Outcome invokeLocation( String loc,
                                          final DatalinkPanel dlPanel,
                                          final Window window ) {

        /* Load a links table from the given location. */
        VOElementFactory voelFact = new VOElementFactory();
        final LinksDoc linksDoc;
        try {
            VOElement voel = voelFact.makeVOElement( loc );
            linksDoc = LinksDoc.randomAccess( LinksDoc.createLinksDoc( voel ) );
        }
        catch ( SAXException e ) {
            return Outcome.failure( "XML parse failure: " + e.getMessage() );
        }
        catch ( IOException e ) {
            return Outcome.failure( e );
        }
        long nrow = linksDoc.getResultTable().getRowCount();

        /* If that worked, load the table into the datalink panel.
         * We have to do this synchronously so that (a) we can work out
         * whether auto-invoke is in effect before proceeding and
         * (b) in the case of auto-invoke we need to return the result of
         * the invocation rather than just the result of loading the table. */
        try {
            SwingUtilities
           .invokeAndWait( () -> dlPanel.setLinksDoc( linksDoc ) );
        }
        catch ( InterruptedException | InvocationTargetException e ) {
            return Outcome.failure( e );
        }
        LinkRowPanel linkPanel = dlPanel.getLinkRowPanel();
        boolean autoInvoke = linkPanel.isAutoInvoke();

        /* If there's no auto-invoke, make the window visible so the user
         * can interact with it.  In the case of auto-invoke that's not
         * generally necessary and it's annoying because it brings the window
         * to the front and grabs focus, but if it's been closed altogther
         * bring it back, since otherwise there's no way for the user to
         * recover it. */
        if ( window != null &&
             ( ! autoInvoke ||
               ( autoInvoke && ! window.isVisible() ) ) ) {
            SwingUtilities.invokeLater( () -> window.setVisible( true ) );
        }

        /* In case of auto-invoke, invoke the row and return the result
         * of doing so; otherwise just return success. */
        return autoInvoke
             ? linkPanel.invokeRow()
             : Outcome.success( "Loaded " + nrow + " rows (" + loc + ")" );
    }

    /**
     * Constructs a DataLink URL given a base URL and an ID value.
     *
     * @param  baseUrl  base URL
     * @param  id   datalink ID value
     * @return  full URL as string
     */
    private static String getDatalinkUrl( String baseUrl, String id ) {
        return new CgiQuery( baseUrl )
              .addArgument( "ID", id )
              .toString(); 
    }

    /**
     * Configures a supplied ComboBox by attempting to select a default
     * value that matches a given target UCD, if any are available.
     * Some variations on the target UCD are tried out to get the best fit.
     *
     * @param  selector  combo box
     * @param  targetUcdBasic   basic UCD required
     */
    private static void selectColumnByUcd( ColumnDataComboBox selector,
                                           String targetUcdBasic ) {
        targetUcdBasic = targetUcdBasic.toLowerCase();
        String targetUcdMain = targetUcdBasic + ";meta.main";
        ColumnData cdataMain = null;
        ColumnData cdataBasic = null;
        ColumnData cdataAny = null;
        int n = selector.getItemCount();
        for ( int i = 0; i < n; i++ ) {
            ColumnData cdata = selector.getItemAt( i );
            String ucd = cdata != null
                       ? cdata.getColumnInfo().getUCD()
                       : null;
            ucd = ucd == null ? null : ucd.toLowerCase();
            if ( ucd != null ) {
                if ( cdataMain == null && ucd.equals( targetUcdMain ) ) {
                    cdataMain = cdata;
                }
                else if ( cdataBasic == null && ucd.equals( targetUcdBasic ) ) {
                    cdataBasic = cdata;
                }
                else if ( cdataAny == null && ucd.startsWith( targetUcdBasic )){
                    cdataAny = cdata;
                }
            }
        }
        ColumnData cdataUse = null;
        if ( cdataUse == null ) {
            cdataUse = cdataMain;
        }
        if ( cdataUse == null ) {
            cdataUse = cdataBasic;
        }
        if ( cdataUse == null ) {
            cdataUse = cdataAny;
        }
        if ( cdataUse != null ) {
            selector.setSelectedItem( cdataUse );
        }
    }

    /**
     * Consumes Datalink URLs to present the referenced DataLink tables
     * in a GUI.
     */
    private static class DatalinkInvoker {

        private final DatalinkPanel dlPanel_;
        private final JFrame window_;

        /**
         * Constructor.
         *
         * @param  tinfo   topcat model information
         */
        DatalinkInvoker( TopcatModelInfo tinfo ) { 
            dlPanel_ = new DatalinkPanel( true, true );
            String title = "TOPCAT(" + tinfo.getTopcatModel().getID() + "): "
                         + "Activation - View Datalink Table";
            window_ = new JFrame( title );
            window_.getContentPane().add( dlPanel_ );
            window_.pack();
        }

        /**
         * Loads the file/URL at a given location into this invoker's GUI.
         *
         * @param  loc  location of Datalink {links}-response file
         * @return  outcome
         */
        public Outcome invokeLocation( String loc ) {
            return ViewDatalinkActivationType
                  .invokeLocation( loc, dlPanel_, window_ );
        }
    }

    /**
     * Configurator implementation in which the user supplies the full URL
     * for a Datalink table.
     */
    private static class UrlDatalinkConfigurator extends UrlColumnConfigurator {

        private final DatalinkInvoker invoker_;

        /**
         * Constructor.
         *
         * @param  tinfo  topcat model information
         * @param  invoker  consumes datalink URLs
         */
        UrlDatalinkConfigurator( TopcatModelInfo tinfo,
                                 DatalinkInvoker invoker ) {
            super( tinfo, "Datalink",
                   new ColFlag[] { ColFlag.DATALINK, ColFlag.URL, } );
            invoker_ = invoker;
            setLocationLabel( "Links Table Location" );
            setLocationTooltip( "Column or expression giving "
                              + "URL/filename location of DataLink table" );
        }

        protected Activator createActivator( ColumnData cdata ) {
            return new LocationColumnActivator( cdata, false ) {
                protected Outcome activateLocation( String loc, long lrow ) {
                    return invoker_.invokeLocation( loc );
                }
            };
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return null;
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public ConfigState getState() {
            return getUrlState();
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
        }
    }

    /**
     * Configurator implementation in which the user supplies a base URL
     * and a column/expression giving DataLink ID, in accordance with
     * the DataLink standard.
     */
    private static class IdDatalinkConfigurator
            extends AbstractActivatorConfigurator {

        private final DatalinkInvoker invoker_;
        private final JTextField baseField_;
        private final ColumnDataComboBox idSelector_;

        private static final String BASEURL_KEY = "baseurl";
        private static final String DLID_KEY = "dlid";

        /**
         * Constructor.
         *
         * @param  tinfo  topcat model information
         * @param  invoker  consumes datalink URLs
         */
        IdDatalinkConfigurator( TopcatModelInfo tinfo,
                                DatalinkInvoker invoker ) {
            super( new JPanel( new BorderLayout() ) );
            final JComponent panel = getPanel();
            invoker_ = invoker;
            JComponent queryPanel = Box.createVerticalBox();
            panel.add( queryPanel, BorderLayout.NORTH );
            baseField_ = new JTextField() {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension( super.getMaximumSize().width,
                                          super.getPreferredSize().height );
                }
            };
            baseField_.getCaret().addChangeListener( getActionForwarder() );
            ColumnDataComboBoxModel idModel =
                new ColumnDataComboBoxModel( tinfo.getTopcatModel(),
                                             Object.class, true );
            idSelector_ = new ColumnDataComboBox();
            idSelector_.setModel( idModel );
            selectColumnByUcd( idSelector_, "meta.id" );
            idSelector_.addActionListener( getActionForwarder() );
            final LineBox baseBox = new LineBox( "Links Endpoint", baseField_ );
            final LineBox idBox = new LineBox( "Datalink ID", idSelector_ );
            baseBox.getLabel()
                   .setToolTipText( "Base URL for DataLink table, "
                                  + "corresponding to {links} endpoint "
                                  + "in DataLink standard" );
            idBox.getLabel()
                 .setToolTipText( "Column/expression giving dataset identifier,"
                                + " corresponding to ID parameter "
                                + "in DataLink standard" );
            queryPanel.add( baseBox );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( idBox );
            panel.addPropertyChangeListener( "enabled", evt -> {
                boolean isEnabled = panel.isEnabled();
                baseBox.setEnabled( isEnabled );
                idBox.setEnabled( isEnabled );
            } );
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public String getConfigMessage() {
            if ( getBaseUrl() == null ) {
                return "No Base URL";
            }
            else if ( getIdColumnData() == null ) {
                return "No Datalink ID value";
            }
            else {
                return null;
            }
        }

        public Activator getActivator() {
            String baseUrl = getBaseUrl();
            ColumnData idData = getIdColumnData();
            if ( baseUrl != null && idData != null ) {
                return new Activator() {
                    public boolean invokeOnEdt() {
                        return false;
                    }
                    public Outcome activateRow( long lrow,
                                                ActivationMeta meta ) {
                        Object value;
                        try {
                            value = idData.readValue( lrow );
                        }
                        catch ( IOException e ) {
                            return Outcome.failure( e );
                        }
                        String subloc = value != null ? value.toString().trim()
                                                      : null;
                        if ( subloc != null && subloc.length() > 0 ) {
                            String dlurl = getDatalinkUrl( baseUrl, subloc );
                            Outcome outcome = invoker_.invokeLocation( dlurl );
                            return UrlColumnConfigurator
                                  .decorateOutcomeWithUrl( outcome, dlurl );
                        }
                        else {
                            return Outcome.failure( "No Datalink ID" );
                        }
                    }
                };
            }
            else {
                return null;
            }
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveText( BASEURL_KEY, baseField_ );
            state.saveSelection( DLID_KEY, idSelector_ );
            return state;
        }

        public void setState( ConfigState state ) {
            state.restoreText( BASEURL_KEY, baseField_ );
            state.restoreSelection( DLID_KEY, idSelector_ );
        }

        /**
         * Returns the base URL currently entered in this configurator.
         *
         * @return   string syntactically capable of being a URL, or null
         */
        private String getBaseUrl() {
            String btxt = baseField_.getText();
            if ( btxt == null || btxt.trim().length() == 0 ) {
                return null;
            }
            else {
                try {
                    URLUtils.newURL( btxt );
                    return btxt;
                }
                catch ( MalformedURLException e ) {
                    return null;
                }
            }
        }

        /**
         * Returns the ColumnData representing datalink ID currently
         * entered in this configurator.
         *
         * @return  ID column data, or null
         */
        private ColumnData getIdColumnData() {
            Object idObj = idSelector_.getSelectedItem();
            return idObj instanceof ColumnData
                 ? (ColumnData) idObj
                 : null;
        }
    }
}
