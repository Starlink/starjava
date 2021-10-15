package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.vo.datalink.LinkColMap;
import uk.ac.starlink.vo.datalink.LinksDoc;
import uk.ac.starlink.vo.datalink.ServiceInvoker;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceParam;

/**
 * Displays information and invocation options corresponding to a single
 * row of a Datalink Links-response table.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2018
 */
public class LinkRowPanel extends JPanel {

    private final UrlOptions urlopts_;
    private final boolean hasAutoInvoke_;
    private final JLabel typeLabel_;
    private final JComponent displayContainer_;
    private final JComponent extraContainer_;
    private final LinkDisplay errorDisplay_;
    private final LinkDisplay accurlDisplay_;
    private final LinkDisplay serviceDisplay_;
    private final LinkDisplay badDisplay_;
    private LinksDoc linksDoc_;
    private LinkDisplay display_;

    /**
     * Constructor.
     *
     * @param  urlopts   options for URL invocation
     * @param  hasAutoInvoke  whether the URL panel should feature an
     *                        auto-invoke button
     */
    public LinkRowPanel( UrlOptions urlopts, boolean hasAutoInvoke ) {
        super( new BorderLayout() );
        urlopts_ = urlopts;
        hasAutoInvoke_ = hasAutoInvoke;

        typeLabel_ = new JLabel();
        JComponent lineBox = Box.createVerticalBox();
        lineBox.setBorder( createTitledBorder( "Row Link Type" ) );
        lineBox.add( new LineBox( null, typeLabel_ ) );
        displayContainer_ = new JPanel( new BorderLayout() );
        displayContainer_.setBorder( createTitledBorder( "Row Detail" ) );
        extraContainer_ = new JPanel( new BorderLayout() );
        badDisplay_ = new BadLinkDisplay();
        errorDisplay_ = new ErrorLinkDisplay();
        accurlDisplay_ = new AccessUrlLinkDisplay();
        serviceDisplay_ = new ServiceLinkDisplay();

        JComponent apanel = new JPanel( new BorderLayout() );
        apanel.add( displayContainer_, BorderLayout.NORTH );
        apanel.add( extraContainer_, BorderLayout.CENTER );
        add( lineBox, BorderLayout.NORTH );
        add( apanel, BorderLayout.CENTER );
    }

    /**
     * Sets the document whose rows are being displayed.
     *
     * @param  linksDoc  links response document
     */
    public void setLinksDoc( LinksDoc linksDoc ) {
        linksDoc_ = linksDoc;
    }

    /**
     * Sets the row contents to be displayed.
     * This row must correspond to the currently configured LinksDoc.
     *
     * @param  row   links document row data
     */
    public void setRow( Object[] row ) {
        LinkColMap colMap = linksDoc_.getColumnMap();

        /* Determine which detail panel to show. */
        final LinkDisplay display;
        if ( row == null ) {
            display = badDisplay_;
        }
        else if ( ! Tables.isBlank( colMap.getErrorMessage( row ) ) ) {
            display = errorDisplay_;
        }
        else if ( ! Tables.isBlank( colMap.getAccessUrl( row ) ) ) {
            display = accurlDisplay_;
        }
        else if ( ! Tables.isBlank( colMap.getServiceDef( row ) ) ) {
            display = serviceDisplay_;
        }
        else {
            display = badDisplay_;
        }
        display_ = display;

        /* Configure the detail panel for the selected row. */
        typeLabel_.setText( display.getTypeText() );
        display.configureRow( row );

        /* Place the relevant components in the appropriate containers. */
        displayContainer_.removeAll();
        extraContainer_.removeAll();
        JComponent detailPanel = display.getDetailPanel();
        JComponent extraPanel = display.getExtraPanel();
        if ( detailPanel != null ) {
            displayContainer_.add( display.getDetailPanel() );
        }
        if ( extraPanel != null ) {
            extraContainer_.add( display.getExtraPanel() );
        }
        displayContainer_.revalidate();
        displayContainer_.repaint();
        extraContainer_.revalidate();
        extraContainer_.repaint();
    }

    /**
     * Returns a short summary of the link described by the currently
     * configured row.
     *
     * @return  row summary text
     */
    public String getRowSummary() {
        return display_.getRowSummary();
    }

    /**
     * Performs an invocation action for the currently-configured row,
     * if appropriate.
     *
     * @return   outcome
     */
    public Outcome invokeRow() {
        return display_.invokeRow();
    }

    /**
     * Indicates whether this panel is currently set up for auto-invoke.
     * If true, then selecting a row in the displayed links document
     * will cause the link to be followed according to current settings
     * without further manual user intervention.
     *
     * @return   whether auto-invoke is in effect
     */
    public boolean isAutoInvoke() {
        return display_.isAutoInvoke();
    }

    /**
     * Returns the service descriptor corresponding to a given service_def
     * value.
     *
     * @param   linksDoc  links document
     * @param   serviceDef  value from service_def column
     * @return   named service descriptor, or null if not found
     */
    private static ServiceDescriptor getServiceDescriptor( LinksDoc linksDoc,
                                                           String serviceDef ) {
        if ( serviceDef != null ) {
            for ( ServiceDescriptor sd : linksDoc.getServiceDescriptors() ) {
                if ( serviceDef.equals( sd.getDescriptorId() ) ) {
                    return sd;
                }
            }
        }
        return null;
    }

    /**
     * Creates a component border with a text title.
     *
     * @param  title  title text
     * @return   border
     */
    private static Border createTitledBorder( String title ) {
        return AuxWindow.makeTitledBorder( title );
    }

    /**
     * Positions a URL panel in a component suitable for adding to the
     * row detail panel.
     *
     * @param   panel  url panel
     * @return   container for panel
     */
    private static JComponent placeUrlPanel( UrlPanel panel ) {
        JComponent hbox = Box.createHorizontalBox();
        JComponent tbox = Box.createVerticalBox();
        tbox.add( new JLabel( "URL: " ) );
        tbox.add( Box.createVerticalGlue() );
        hbox.add( tbox );
        hbox.add( panel );
        hbox.setBorder( BorderFactory.createCompoundBorder(
                            BorderFactory.createEtchedBorder(),
                            BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) );
        return hbox;
    }

    /**
     * Aggregates some behaviour required for a link-type-specific
     * link detail display.
     */
    private interface LinkDisplay {

        /**
         * Returns a short name describing the type of link row.
         *
         * @return   link type
         */
        String getTypeText();

        /**
         * Returns a panel displaying basic description of a link.
         *
         * @return  link detail panel, may be null
         */
        JComponent getDetailPanel();

        /**
         * Returns a panel displaying additional details for a link.
         *
         * @return  link additional info panel, may be null
         */
        JComponent getExtraPanel();

        /**
         * Configures this display and its components with the data from
         * a given row object.
         *
         * @param  row  vector of values from the table for this link
         */
        void configureRow( Object[] row );

        /**
         * Indicates whether this display has a currently active
         * auto-invoke settting.
         *
         * @return  true iff auto-invoke is present and selected
         */
        boolean isAutoInvoke();

        /**
         * Returns a short summary of the link described by the currently
         * configured row.
         *
         * @return  row summary text
         */
        String getRowSummary();

        /**
         * Performs an invocation action for the currently-configured row,
         * if appropriate.
         *
         * @return   invocation outcome
         */
        Outcome invokeRow();
    }

    /**
     * LinkDisplay implementation for a badly-configured link row.
     */
    private static class BadLinkDisplay implements LinkDisplay {
        public String getTypeText() {
            return "(bad link information)";
        }
        public JComponent getDetailPanel() {
            return null;
        }
        public JComponent getExtraPanel() {
            return null;
        }
        public void configureRow( Object[] row ) {
        }
        public boolean isAutoInvoke() {
            return false;
        }
        public String getRowSummary() {
            return "Bad links table row";
        }
        public Outcome invokeRow() {
            return Outcome.failure( "Bad links table row" );
        }
    }

    /**
     * LinkDisplay implementation for a link error report.
     */
    private class ErrorLinkDisplay implements LinkDisplay {
        private final JComponent panel_;
        private final JTextComponent errorField_;
        private String errorText_;

        /**
         * Constructor.
         */
        public ErrorLinkDisplay() {
            panel_ = new JPanel( new BorderLayout() );
            panel_.add( new LineBox( "Error", null ), BorderLayout.NORTH );
            errorField_ = new JTextArea();
            errorField_.setEditable( false );
            errorField_.setOpaque( false );
            panel_.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
            panel_.add( errorField_, BorderLayout.CENTER );
        }
        public String getTypeText() {
            return "Link Error";
        }
        public JComponent getDetailPanel() {
            return panel_;
        }
        public JComponent getExtraPanel() {
            return null;
        }
        public void configureRow( Object[] row ) {
            errorText_ = linksDoc_.getColumnMap().getErrorMessage( row );
            errorField_.setText( errorText_ );
            errorField_.setCaretPosition( 0 );
        }
        public boolean isAutoInvoke() {
            return false;
        }
        public String getRowSummary() {
            return "Link Error: " + errorText_;
        }
        public Outcome invokeRow() {
            return Outcome.success( getRowSummary() );
        }
    }

    /**
     * LinkDisplay implementation for an AccessUrl-type link (fixed URL).
     */
    private class AccessUrlLinkDisplay implements LinkDisplay {
        private final JComponent panel_;
        private final InfoStack infoStack_;
        private final UrlPanel urlPanel_;

        /**
         * Constructor.
         */
        public AccessUrlLinkDisplay() {
            infoStack_ = new InfoStack( new LinkColMap.ColDef<?>[] {
                LinkColMap.COL_ACCESSURL,
                LinkColMap.COL_CONTENTTYPE,
                LinkColMap.COL_CONTENTLENGTH,
                LinkColMap.COL_DESCRIPTION,
                LinkColMap.COL_SEMANTICS,
            } );
            JComponent box = Box.createVerticalBox();
            box.add( infoStack_ );
            urlPanel_ = new UrlPanel( urlopts_, hasAutoInvoke_ );
            box.add( placeUrlPanel( urlPanel_ ) );
            panel_ = new JPanel( new BorderLayout() );
            panel_.add( box, BorderLayout.NORTH );
        }
        public String getTypeText() {
            return "Fixed Access URL";
        }
        public JComponent getDetailPanel() {
            return panel_;
        }
        public JComponent getExtraPanel() {
            return null;
        }
        public void configureRow( Object[] row ) {
            LinkColMap colMap = linksDoc_.getColumnMap();
            infoStack_.configureForRow( colMap, row );
            String urlStr = colMap.getAccessUrl( row );
            URL url;
            if ( urlStr != null ) {
                try {
                    url = new URL( urlStr );
                }
                catch ( MalformedURLException e ) {
                    url = null;
                }
            }
            else {
                url = null;
            }
            String ctype = colMap.getContentType( row );
            urlPanel_.configure( url, ctype, null );
        }
        public boolean isAutoInvoke() {
            return urlPanel_.isAutoInvoke();
        }
        public String getRowSummary() {
            return "Access URL: " + urlPanel_.getUrl();
        }
        public Outcome invokeRow() {
            return urlPanel_.invokeUrl();
        }
    }

    /**
     * LinkDisplay implementation for a ServiceDef-type link
     * (URL parameterised by a Service Descriptor).
     */
    private class ServiceLinkDisplay implements LinkDisplay {
        private final JComponent detailPanel_;
        private final InfoStack infoStack_;
        private final JTextField standardIdField_;
        private final JTextField resourceIdField_;
        private final UrlPanel urlPanel_;
        private JComponent uiPanel_;

        /**
         * Constructor.
         */
        ServiceLinkDisplay() {
            infoStack_ = new InfoStack( new LinkColMap.ColDef<?>[] {
                LinkColMap.COL_CONTENTTYPE,
                LinkColMap.COL_DESCRIPTION,
                LinkColMap.COL_SEMANTICS,
            } );
            standardIdField_ = infoStack_.addField( "Standard ID" );
            resourceIdField_ = infoStack_.addField( "Resource ID" );
            JComponent box = Box.createVerticalBox();
            box.add( infoStack_ );
            urlPanel_ = new UrlPanel( urlopts_, hasAutoInvoke_ );
            box.add( placeUrlPanel( urlPanel_ ) );
            detailPanel_ = new JPanel( new BorderLayout() );
            detailPanel_.add( box, BorderLayout.NORTH );
        }
        public String getTypeText() {
            return "Service Invocation";
        }
        public JComponent getDetailPanel() {
            return detailPanel_;
        }
        public JComponent getExtraPanel() {
            return uiPanel_;
        }
        public void configureRow( Object[] row ) {
            LinkColMap colMap = linksDoc_.getColumnMap();
            infoStack_.configureForRow( colMap, row );
            String serviceDef = colMap.getServiceDef( row );
            ServiceDescriptor sd =
                getServiceDescriptor( linksDoc_, serviceDef );
            String idref = "ID=\"" + serviceDef + "\"";
            ServiceInvoker invoker;
            String excuseMsg;
            final String stdId;
            final String resourceId;
            if ( sd == null ) {
                invoker = null;
                excuseMsg = "No service descriptor " + idref;
                stdId = null;
                resourceId = null;
            }
            else {
                stdId = sd.getStandardId();
                resourceId = sd.getResourceIdentifier();
                try {
                    invoker =
                        new ServiceInvoker( sd, linksDoc_.getResultTable() );
                    excuseMsg = null;
                }
                catch ( IOException e ) {
                    invoker = null;
                    excuseMsg = "Broken service " + idref + ": "
                              + e.getMessage();
                }
            }
            standardIdField_.setText( stdId );
            resourceIdField_.setText( resourceId );
            JComponent uiPanel;
            if ( invoker != null ) {
                ServicePanel servicePanel =
                    new ServicePanel( invoker, urlPanel_, colMap, row );
                uiPanel = new JScrollPane( servicePanel );
                uiPanel.setBorder( createTitledBorder( "Parameters" ) );
            }
            else {
                uiPanel = createExcusePanel( excuseMsg );
                urlPanel_.configure( null, null, null );
            }
            uiPanel_ = uiPanel;
        }
        public boolean isAutoInvoke() {
            return urlPanel_.isAutoInvoke();
        }
        public String getRowSummary() {
            return "Access URL: " + urlPanel_.getUrl();
        }
        public Outcome invokeRow() {
            return urlPanel_.invokeUrl();
        }

        /**
         * Returns a component that reports the reason why it can't
         * do anything interesting.
         *
         * @param  msg  error message
         */
        private JComponent createExcusePanel( String msg ) {
            JTextComponent field = new JTextArea();
            field.setEditable( false );
            field.setOpaque( false );
            field.setText( msg );
            field.setCaretPosition( 0 );
            JComponent box = new JPanel( new BorderLayout() );
            box.add( field, BorderLayout.NORTH );
            box.setBorder( createTitledBorder( "Error" ) );
            return box;
        }
    }

    /**
     * Component that displays the detailed additional information
     * corresponding to a Service Descriptor and the values it acquires
     * from the relevant row of its result table.
     *
     * <p>It presents a GUI; as the user interacts with this, the URL
     * in the supplied UrlPanel will be updated accordingly.
     */
    private static class ServicePanel extends JPanel {
        private final ServiceInvoker invoker_;
        private final UrlPanel urlPanel_;
        private final ServiceParamPanel paramPanel_;
        private final String contentType_;
        private final String standardId_;

        /**
         * Constructor.
         *
         * @param  invoker  service invoker
         * @param  urlPanel  URL display panel which this service panel
         *                   is helping to configure
         * @param  colMap    link column map describing the links document
         * @parm   row       row value vector from the links document
         */
        ServicePanel( ServiceInvoker invoker, UrlPanel urlPanel,
                      LinkColMap colMap, Object[] row ) {
            super( new BorderLayout() );
            invoker_ = invoker;
            urlPanel_ = urlPanel;
            ServiceDescriptor descriptor = invoker.getServiceDescriptor();
            standardId_ = descriptor.getStandardId();
            contentType_ = colMap.getContentType( row );
            Map<ServiceParam,String> suppliedMap =
                new HashMap<ServiceParam,String>();
            suppliedMap.putAll( invoker.getFixedParamMap() );
            suppliedMap.putAll( invoker.getRowParamMap( row ) );
            ServiceParam[] params = descriptor.getInputParams();
            paramPanel_ = new ServiceParamPanel( params );
            paramPanel_.setValueMap( suppliedMap );
            paramPanel_.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    configureUrl();
                }
            } );
            configureUrl();
            add( paramPanel_, BorderLayout.NORTH );
        }

        /**
         * Invoked following user interaction that might have changed
         * the URL defined by this panel's components.
         */
        private void configureUrl() {
            Map<ServiceParam,String> valueMap = paramPanel_.getValueMap();
            URL url = invoker_.completeUrl( valueMap );
            urlPanel_.configure( url, contentType_, standardId_ );
        }
    }

    /**
     * LabelledComponentStack subclass that displays LinkColMap items.
     */
    private static class InfoStack extends LabelledComponentStack {
        final Map<LinkColMap.ColDef<?>,JTextField> fieldMap_;

        /**
         * Constructor.
         *
         * @param  cols  items to display
         */
        public InfoStack( LinkColMap.ColDef<?>[] cols ) {
            super();
            fieldMap_ = new LinkedHashMap<LinkColMap.ColDef<?>,JTextField>();
            for ( LinkColMap.ColDef<?> col : cols ) {
                JTextField field = addField( col.getName() );
                fieldMap_.put( col, field );
            }
        }

        /**
         * Adds a field to this stack in house style.
         *
         * @param  name  field display name
         * @return   field displaying the named item
         */
        public JTextField addField( String name ) {
            JTextField field = new JTextField();
            field.setBorder( BorderFactory.createEmptyBorder() );
            field.setEditable( false );
            addLine( name, field );
            return field;
        }

        /**
         * Configures this display for a given row in the links document.
         *
         * @param  colMap  links document column map
         * @param  row    row vector from links document
         */
        public void configureForRow( LinkColMap colMap, Object[] row ) {
            for ( Map.Entry<LinkColMap.ColDef<?>,JTextField> entry :
                  fieldMap_.entrySet() ) {
                LinkColMap.ColDef<?> col = entry.getKey();
                JTextField field = entry.getValue();
                Object value = colMap.getValue( col, row );
                field.setText( value == null ? null : value.toString() );
                field.setCaretPosition( 0 );
            }
        }
    }
}
