package uk.ac.starlink.hapi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import org.json.JSONObject;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;

/**
 * Displays and allows selection of metadata for a HAPI service.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public class ServicePanel extends JPanel {

    private final HapiService service_;
    private final Consumer<URL> docUrlHandler_;
    private final Map<String,DatasetMeta> metaMap_;
    private final JList<String> dsList_;
    private final FilterListModel<String> dslistModel_;
    private final JTable paramTable_;
    private final ParamTableModel paramTableModel_;
    private final JSplitPane splitter_;
    private final DateRangePanel rangePanel_;
    private final JLabel cadenceLabel_;
    private final JLabel maxDurationLabel_;
    private final JTextField resourceUrlField_;
    private HapiVersion hapiVersion_;
    private boolean supportsBinary_;
    private Supplier<String> formatSupplier_;
    private BooleanSupplier headerInclusion_;
    private String dsId_;
    private HapiSource hapiSrc_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /**
     * Constructor.
     *
     * @param  service  service displayed by this panel
     * @param  docUrlHandler  handler for documentation URLs,
     *                        typically displays in a browser;
     *                        may be null
     */
    @SuppressWarnings("this-escape")
    public ServicePanel( HapiService service, Consumer<URL> docUrlHandler ) {
        super( new BorderLayout() );
        service_ = service;
        docUrlHandler_ = docUrlHandler;
        hapiVersion_ = HapiVersion.ASSUMED;
        metaMap_ = new HashMap<String,DatasetMeta>();

        /* Set up GUI components. */
        dslistModel_ = new FilterListModel<String>();
        dsList_ = new JList<String>( dslistModel_ );
        dsList_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        paramTableModel_ = new ParamTableModel();
        paramTable_ = new JTable( paramTableModel_ );
        paramTable_.setRowSelectionAllowed( false );
        paramTable_.setColumnSelectionAllowed( false );
        dsList_.addListSelectionListener( evt -> {
            setDataset( dsList_.getSelectedValue() );
        } );
        JComponent filterPanel = dslistModel_.getFilterPanel();
        rangePanel_ = new DateRangePanel();
        cadenceLabel_ = new JLabel();
        maxDurationLabel_ = new JLabel();
        resourceUrlField_ = new JTextField();
        resourceUrlField_.setEditable( false );
        if ( docUrlHandler_ != null ) {
            resourceUrlField_.setForeground( Color.BLUE );
            resourceUrlField_.addMouseListener( new MouseAdapter() {
                @Override
                public void mouseClicked( MouseEvent evt ) {
                    URL url;
                    try {
                        url = URLUtils.newURL( resourceUrlField_.getText() );
                    }
                    catch ( MalformedURLException e ) {
                        return;
                    }
                    docUrlHandler_.accept( url );
                }
            } );
        }

        /* Arrange to notify listeners if the data URL may have changed. */
        paramTableModel_.addTableModelListener( evt -> {
            updateHapiSource();
        } );
        rangePanel_.addPropertyChangeListener( evt -> {
            String prop = evt.getPropertyName();
            if ( DateRangePanel.PROP_ISOSTART.equals( prop ) ||
                 DateRangePanel.PROP_ISOSTOP.equals( prop ) ) {
                updateHapiSource();
            }
        } );

        /* Lay out components. */
        LabelledComponentStack metaStack = new LabelledComponentStack();
        metaStack.addLine( "Cadence", cadenceLabel_ );
        metaStack.addLine( "Max Duration", maxDurationLabel_ );
        metaStack.addLine( "Resource URL", null, resourceUrlField_, true );
        JComponent rangeBox = new JPanel( new BorderLayout() );
        rangeBox.add( rangePanel_, BorderLayout.NORTH );
        JPanel dsPanel = new JPanel( new BorderLayout() );
        JComponent filterBox = new JPanel( new BorderLayout() );
        filterBox.add( filterPanel, BorderLayout.NORTH );
        filterBox.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 5 ) );
        dsPanel.add( filterBox, BorderLayout.EAST );
        JScrollPane dsScroller = new JScrollPane( dsList_ );
        dsScroller.setVerticalScrollBarPolicy( JScrollPane
                                              .VERTICAL_SCROLLBAR_AS_NEEDED );
        dsScroller.setHorizontalScrollBarPolicy( JScrollPane
                                                .HORIZONTAL_SCROLLBAR_NEVER );
        dsScroller.setPreferredSize( new Dimension( 300, 120 ) );
        dsPanel.add( dsScroller, BorderLayout.CENTER );

        JPanel paramPanel = new JPanel( new BorderLayout() );
        JScrollPane paramScroller = new JScrollPane( paramTable_ );
        paramScroller.setVerticalScrollBarPolicy(
                          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
        paramScroller.setHorizontalScrollBarPolicy( 
                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        paramPanel.add( paramScroller, BorderLayout.CENTER );
        paramScroller.setPreferredSize( new Dimension( 300, 150 ) );
        boolean useSplit = true;
        if ( useSplit ) {
            splitter_ = new JSplitPane();
            splitter_.setOrientation( JSplitPane.VERTICAL_SPLIT );
            splitter_.setTopComponent( dsPanel );
            splitter_.setBottomComponent( paramPanel );
            add( splitter_, BorderLayout.CENTER );
        }
        else {
            splitter_ = null;
            add( dsPanel, BorderLayout.WEST );
            add( paramPanel, BorderLayout.CENTER );
        }
        Box fieldsBox = Box.createVerticalBox();
        fieldsBox.add( metaStack );
        fieldsBox.add( rangeBox );
        add( fieldsBox, BorderLayout.SOUTH );
        dsPanel.setBorder( HapiBrowser.createTitledBorder( "Datasets" ) );
        paramPanel.setBorder( HapiBrowser
                             .createTitledBorder( "Dataset Parameters" ) );
        metaStack.setBorder( HapiBrowser
                            .createTitledBorder( "Dataset Metadata" ) );
        rangeBox.setBorder( HapiBrowser.createTitledBorder( "Interval" ) );
       
        /* Asynchronously initialise state. */
        initForService();
    }

    /**
     * Provides a supplier that explicitly sets the data format to request.
     * A null format causes automatic choice.
     *
     * @param  formatSupplier  supplier for HAPI data format string,
     *                         one of "csv" or "binary"
     */
    public void setFormatSupplier( Supplier<String> formatSupplier ) {
        formatSupplier_ = formatSupplier;
    }

    /**
     * Provides a supplier that indicates whether the header is to be
     * included with data stream requests, or whether the metadata is
     * to be reused from earlier metadata requests.
     *
     * @param  headerInclusion  supplie for header inclusion flag
     */
    public void setHeaderInclusion( BooleanSupplier headerInclusion ) {
        headerInclusion_ = headerInclusion;
    }

    /**
     * Does a blanket inclusion or exclusion of all columns in the
     * currently selected dataset.
     *
     * @param  isIncluded  true to include all, false to exclude all
     */
    public void setAllIncluded( boolean isIncluded ) {
        DatasetMeta dsmeta = paramTableModel_.dsmeta_;
        if ( dsmeta != null ) {
            dsmeta.setAllIncluded( isIncluded );
        }
        paramTableModel_.fireTableDataChanged();
    }

    /**
     * Returns a URL from which HAPI data can be downloaded
     * corresponding to the current state of this GUI.
     *
     * @return   currently indicated HAPI data stream URL, or null
     */
    public HapiSource getHapiSource() {
        String dsId = dsId_;
        String startDate = rangePanel_.getIsoStart();
        String stopDate = rangePanel_.getIsoStop();
        DatasetMeta dsmeta = paramTableModel_.dsmeta_;
        if ( dsId != null &&
             Times.isoToUnixSeconds( startDate ) <
             Times.isoToUnixSeconds( stopDate ) ) {
            Map<String,String> reqMap = new LinkedHashMap<>();
            reqMap.put( hapiVersion_.getDatasetRequestParam(), dsId );
            reqMap.put( hapiVersion_.getStartRequestParam(), startDate );
            reqMap.put( hapiVersion_.getStopRequestParam(), stopDate );
            if ( dsmeta != null && ! dsmeta.isAllIncluded() ) {
                reqMap.put( "parameters",
                            Arrays.stream( dsmeta.params_ )
                                  .filter( p -> dsmeta.isIncluded( p ) )
                                  .map( p -> p.getName() )
                                  .collect( Collectors.joining( "," ) ) );
            }
            String fmt = formatSupplier_ == null ? null : formatSupplier_.get();
            if ( fmt != null ) {
                reqMap.put( "format", fmt );
            }
            else if ( supportsBinary_ ) {
                reqMap.put( "format", "binary" );
            }
            String format = reqMap.get( "format" );
            URL dataUrl =
                service_.createQuery( HapiEndpoint.DATA, reqMap );
            final HapiParam[] params;
            if ( dsmeta == null ||
                 ( headerInclusion_ != null &&
                   headerInclusion_.getAsBoolean() ) ) {
                params = null;
            }
            else {
                params = Arrays.stream( dsmeta.info_.getParameters() )
                               .filter( p -> dsmeta.isIncluded( p ) )
                               .toArray( n -> new HapiParam[ n ] );
            }
            return new HapiSource( service_, dataUrl, params );
        }
        else {
            return null;
        }
    }

    /**
     * Returns the date range selection panel used by this service panel.
     *
     * @return  date range panel
     */
    public DateRangePanel getDateRangePanel() {
        return rangePanel_;
    }

    /**
     * Called when the HapiSource might have changed.
     */
    private void updateHapiSource() {
        HapiSource hapiSrc = getHapiSource();
        if ( ! Objects.equals( hapiSrc_, hapiSrc ) ) {
            firePropertyChange( HapiBrowser.HAPISOURCE_PROP,
                                hapiSrc_, hapiSrc );
            hapiSrc_ = hapiSrc;
        }
    }

    /**
     * Update the list of known datasets.
     *
     * @param  catalog  HAPI catalog object
     */
    private void setCatalog( HapiCatalog catalog ) {
        hapiVersion_ = catalog.getHapiVersion();
        dslistModel_.setItems( Arrays.asList( catalog.getDatasetIds() ) );
        if ( dslistModel_.getSize() > 0 ) {
            dsList_.setSelectedIndex( 0 );
        }
        revalidate();
        if ( splitter_ != null ) {
            splitter_.setDividerLocation( 0.45 );
        }
        repaint();
    }

    /**
     * Update the currently selected dataset.
     *
     * @param  dsId   dataset identifier
     */
    private void setDataset( String dsId ) {
        if ( Objects.equals( dsId, dsId_ ) ) {
            return;
        }
        dsId_ = dsId;
        if ( dsId == null ) {
            setDatasetMeta( null );
        }

        /* Lazily create and store a DatasetMeta object for this dataset. */
        else if ( metaMap_.containsKey( dsId ) ) {
            setDatasetMeta( metaMap_.get( dsId ) );
        }
        else {
            setDatasetMeta( null );

            /* Asynchronously make metadata request. */
            final Map<String,String> infoMap = new LinkedHashMap<>();
            infoMap.put( hapiVersion_.getDatasetRequestParam(), dsId );
            new Thread( () -> {
                JSONObject infoJson;
                try {
                    infoJson = service_.readJson( HapiEndpoint.INFO, infoMap );
                }
                catch ( IOException e ) {
                    logger_.log( Level.WARNING, "Info query failed", e );
                    infoJson = new JSONObject();
                }
                DatasetMeta dsmeta =
                    new DatasetMeta( HapiInfo.fromJson( infoJson ) );
                SwingUtilities.invokeLater( () -> {
                    metaMap_.put( dsId, dsmeta );
                    if ( dsId.equals( dsId_ ) ) {
                        setDatasetMeta( dsmeta );
                    }
                } );
            }, "HAPI dataset metadata load" ).start();
        }
    }

    /**
     * Updates the state of the GUI with a new DatasetMetadata object.
     *
     * @param  dsmeta  dataset metadata
     */
    private void setDatasetMeta( DatasetMeta dsmeta ) {
        paramTableModel_.setDatasetMeta( dsmeta );
        HapiInfo info = dsmeta == null ? null : dsmeta.info_;
        String[] isoLimits =
              info == null
            ? new String[ 2 ]
            : new String[] { info.getStartDate(), info.getStopDate() };
        rangePanel_.setIsoLimits( isoLimits[ 0 ], isoLimits[ 1 ] );
        if ( rangePanel_.getIsoStart().trim().length() == 0 ) {
            String sampleStart = info == null ? null
                                              : info.getSampleStartDate();
            rangePanel_.setIsoStart( sampleStart );
        }
        if ( rangePanel_.getIsoStop().trim().length() == 0 ) {
            String sampleStop = info == null ? null
                                             : info.getSampleStopDate();
            rangePanel_.setIsoStop( sampleStop );
        }
        cadenceLabel_.setText( info == null ? null : info.getCadence() );
        maxDurationLabel_.setText( info == null
                                 ? null
                                 : info.getMaxRequestDuration() );
        resourceUrlField_.setText( info == null ? null
                                                : info.getResourceUrl() );
        resourceUrlField_.setCaretPosition( 0 );
        repaint();
        StarJTable.configureColumnWidths( paramTable_, 1200, 1000 );
    }

    /**
     * Asynchronously acquires basic information from this panel's service.
     */
    private void initForService() {
        new Thread( () -> {

            /* Get and install catalog information. */
            JSONObject catalogJson;
            try {
                catalogJson = service_.readJson( HapiEndpoint.CATALOG );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Catalog query failed", e );
                catalogJson = new JSONObject();
            }
            final HapiCatalog catalog = HapiCatalog.fromJson( catalogJson );
            SwingUtilities.invokeLater( () -> {
                setCatalog( catalog );
            } );

            /* Get and install capabilities information. */
            JSONObject capsJson;
            try {
                capsJson = service_.readJson( HapiEndpoint.CAPABILITIES );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Capabilities query failed", e );
                capsJson = new JSONObject();
            }
            HapiCapabilities hapiCaps = HapiCapabilities.fromJson( capsJson );
            SwingUtilities.invokeLater( () -> {
                if ( Arrays.asList( hapiCaps.getOutputFormats() )
                           .contains( "binary" ) ) {
                    supportsBinary_ = true;
                }
            } );
        }, "HAPI service catalog load" ).start();
    }

    /**
     * Encapsulates information about a dataset.
     * That includes its metadata and which of its parameters
     * are currently marked for inclusion when downloading data.
     */
    private static class DatasetMeta {

        private final HapiInfo info_;
        private final HapiParam[] params_;
        private final Set<String> excluded_;

        /**
         * Constructor.
         *
         * @param   info   dataset metadata
         */
        DatasetMeta( HapiInfo info ) {
            info_ = info;
            params_ = info.getParameters();
            excluded_ = new HashSet<String>();
        }

        /**
         * Set a given parameter as included or excluded for download.
         *
         * @param  param  param
         * @param  isIncluded  true to include, false to exclude
         */
        void setIncluded( HapiParam param, boolean isIncluded ) {
            if ( isIncluded ) {
                excluded_.remove( param.getName() );
            }
            else {
                excluded_.add( param.getName() );
            }
        }

        /**
         * Set all parameters as included or excluded for download.
         *
         * @param  isIncluded  true to include all, false to exclude all
         */
        void setAllIncluded( boolean isIncluded ) {
            if ( isIncluded ) {
                excluded_.clear();
            }
            else {
                for ( int i = 1; i < params_.length; i++ ) {
                    excluded_.add( params_[ i ].getName() );
                }
            }
        }

        /**
         * Indicates whether a given parameter is included for download.
         *
         * @param  param  param
         * @return  true for included, false for excluded
         */
        boolean isIncluded( HapiParam param ) {
            return ! excluded_.contains( param.getName() );
        }

        /**
         * Indicates whether all parameters are included.
         *
         * @return  true iff all included, else false
         */
        boolean isAllIncluded() {
            return excluded_.isEmpty();
        }
    }

    /**
     * TableModel used to display parameters for the given dataset.
     * The first column contains an editable Boolean value (checkbox)
     * so that the user can select download inclusion status for each
     * parameter.
     */
    private static class ParamTableModel extends ArrayTableModel<HapiParam> {

        private DatasetMeta dsmeta_;

        /**
         * Constructor.
         */
        ParamTableModel() {
            super( new HapiParam[ 0 ] );
            List<ArrayTableColumn<HapiParam,?>> colList = new ArrayList<>();
            colList.add( new ArrayTableColumn<HapiParam,Boolean>
                                             ( "Include", Boolean.class ) {
                public Boolean getValue( HapiParam param ) {
                    return dsmeta_.isIncluded( param );
                }
            } );
            colList.add( createStringColumn( "Name", p -> p.getName() ) );
            colList.add( createStringColumn( "Type",
                                             p -> p.getType().toString() ) );
            colList.add( createStringColumn( "Size", p -> {
                int leng = p.getLength();
                int[] size = p.getSize();
                StringBuffer sbuf = new StringBuffer();
                if ( leng > 0 ) {
                    sbuf.append( Integer.toString( leng ) );
                }
                if ( size != null ) {
                    if ( sbuf.length() > 0 ) {
                        sbuf.append( "; " );
                    }
                    sbuf.append( '[' )
                        .append( DefaultValueInfo.formatShape( size ) )
                        .append( ']' );
                }
                return sbuf.toString();
            } ) );
            colList.add( createStringColumn( "Units", p -> {
                String[] units = p.getUnits();
                if ( units == null || units.length == 0 ) {
                    return null;
                }
                else if ( units.length == 1 ) {
                    return units[ 0 ];
                }
                else {
                    return Arrays.toString( units );
                }
            } ) );
            colList.add( createStringColumn( "Description",
                                             p -> p.getDescription() ) );
            setColumns( colList );
        }

        @Override
        public boolean isCellEditable( int irow, int icol ) {

            /* Note that the first (timestamp) parameter is always included. */
            return icol == 0 && irow != 0;
        }

        @Override
        public void setValueAt( Object newValue, int irow, int icol ) {
            if ( icol == 0 && irow != 0 ) {
                dsmeta_.setIncluded( dsmeta_.params_[ irow ],
                                     Boolean.TRUE.equals( newValue ) );
            }
        }

        /**
         * Sets the content of this table model for a new dataset.
         *
         * @param  dsmeta  dataset metadata to display
         */
        public void setDatasetMeta( DatasetMeta dsmeta ) {
            dsmeta_ = dsmeta;
            setItems( dsmeta == null ? new HapiParam[ 0 ] : dsmeta.params_ );
        }

        /**
         * Convenience function to create a string-valued metadata column.
         *
         * @param  name  column display name
         * @param  func  maps HapiParam to column value
         */
        private static ArrayTableColumn<HapiParam,String>
                createStringColumn( String name,
                                    Function<HapiParam,String> func ) {
            return new ArrayTableColumn<HapiParam,String>( name,
                                                           String.class ) {
                public String getValue( HapiParam param ) {
                    return func.apply( param );
                }
            };
        }
    }
}
