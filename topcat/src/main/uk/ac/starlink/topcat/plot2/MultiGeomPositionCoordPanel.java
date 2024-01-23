package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.task.PlotContext;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.RenderingComboBox;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * PositionCoordPanel implementation that offers a choice of DataGeoms.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2024
 */
public class MultiGeomPositionCoordPanel implements PositionCoordPanel {

    private final JComboBox<DataGeom> geomSelector_;
    private final Map<DataGeom,SimplePositionCoordPanel> panelMap_;
    private final ActionForwarder forwarder_;
    private final JComponent panel_;
    private final JComponent cpBox_;
    private final JLabel geomLabel_;
    private DataGeom geom_;
    private PositionCoordPanel pcPanel_;

    /**
     * Constructs a MultiGeomPositionCoordPanel without CoordSpotters.
     *
     * @param  npos   number of sets of positional coordinates to host
     * @param  geoms   DataGeom options
     */
    public MultiGeomPositionCoordPanel( int npos, DataGeom[] geoms ) {
        this( npos,
              Arrays.stream( geoms )
             .collect( Collectors.toMap( g -> g, g -> new CoordSpotter[ 0 ],
                                         (a, b) -> a, LinkedHashMap::new ) ) );
    }

    /**
     * Constructs a MultiGeomPositionCoordPanel with CoordSpotters.
     * The supplied map defines the available geometry options,
     * along with a list of CoordSpotters, if any, to be used with
     * each of the geoms.
     *
     * @param  npos   number of sets of positional coordinates to host
     * @param  geomMap  map from geom to applicable CoordSpotters
     */
    public MultiGeomPositionCoordPanel( int npos,
                                        Map<DataGeom,CoordSpotter[]> geomMap ) {
        panelMap_ = new LinkedHashMap<DataGeom,SimplePositionCoordPanel>();
        forwarder_ = new ActionForwarder();
        for ( Map.Entry<DataGeom,CoordSpotter[]> entry : geomMap.entrySet() ) {
            DataGeom geom = entry.getKey();
            CoordSpotter[] spotters = entry.getValue();
            if ( spotters == null ) {
                spotters = new CoordSpotter[ 0 ];
            }
            panelMap_.put( geom,
                           SimplePositionCoordPanel.createPanel( geom, npos,
                                                                 spotters ) );
        }
        DataGeom[] geoms = geomMap.keySet().toArray( new DataGeom[ 0 ] );
        geomSelector_ = new RenderingComboBox<DataGeom>( geoms ) {
            @Override
            protected String getRendererText( DataGeom geom ) {
                return geom.getVariantName();
            }
        };
        geomSelector_.addActionListener( forwarder_ );
        geomSelector_.addItemListener( evt -> updateState() );
        geomLabel_ = new JLabel( "Coordinates: " );
        geomLabel_.addMouseListener( InstantTipper.getInstance() );

        /* Lay out components. */
        panel_ = new JPanel( new BorderLayout() );
        cpBox_ = Box.createVerticalBox();
        Box box = Box.createVerticalBox();
        Box geomLine = Box.createHorizontalBox();
        geomLine.add( geomLabel_ );
        geomLine.add( new ShrinkWrapper( geomSelector_ ) );
        geomLine.add( Box.createHorizontalStrut( 5 ) );
        geomLine.add( new ComboBoxBumper( geomSelector_ ) );
        geomLine.add( Box.createHorizontalGlue() );
        box.add( geomLine );
        box.add( Box.createVerticalStrut( 5 ) );
        box.add( cpBox_ );
        panel_.add( box, BorderLayout.NORTH );

        /* Initialise state. */
        geomSelector_.setSelectedItem( geoms[ 0 ] );
        updateState();
    }

    public JComponent getComponent() {
        return panel_;
    }

    public DataGeom getDataGeom() {
        return geom_;
    }

    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    public void setTable( TopcatModel tcModel, boolean autoPopulate ) {
        for ( SimplePositionCoordPanel pcPanel : panelMap_.values() ) {
            pcPanel.setTable( tcModel, autoPopulate );
        }
    }

    public GuiCoordContent[] getContents() {
        return pcPanel_.getContents();
    }

    public ColumnDataComboBoxModel getColumnSelector( int ic, int iu ) {
        return pcPanel_.getColumnSelector( ic, iu );
    }

    public Coord[] getAdditionalManagedCoords() {
        return pcPanel_.getAdditionalManagedCoords();
    }

    public List<Bi<String,JComponent>> getExtraTabs() {
        return Collections.emptyList();
    }

    public Coord[] getCoords() {
        return pcPanel_.getCoords();
    }

    public ConfigSpecifier getConfigSpecifier() {
        return pcPanel_.getConfigSpecifier();
    }

    /**
     * Called when the DataGeom selection may have changed.
     */
    private void updateState() {
        if ( pcPanel_ != null ) {
            pcPanel_.removeActionListener( forwarder_ );
        }
        DataGeom geom =
            geomSelector_.getItemAt( geomSelector_.getSelectedIndex() );
        if ( geom != geom_ ) {
            geom_ = geom;
            pcPanel_ = panelMap_.get( geom );
            cpBox_.removeAll();
            cpBox_.add( panelMap_.get( geom ).getComponent() );
            cpBox_.validate();
            String geomName =
                geom == null ? "" : geom.getVariantName().toLowerCase();
            geomLabel_.setToolTipText( PlotContext.GEOM_PARAM_NAME
                                     + "=" + geomName );
        }
        if ( pcPanel_ != null ) {
            pcPanel_.addActionListener( forwarder_ );
        }
    }
}
