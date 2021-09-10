package uk.ac.starlink.ttools.plot2.layer;

import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.SkyCoord;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SphereDataGeom;

/**
 * ShapeForm that can plot Area coordinates as shapes on a plane or sky plot.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2020
 */
public abstract class AreaForm<DG extends DataGeom> implements ShapeForm {

    private final AreaCoord<DG> areaCoord_;
    private final Coord[] otherCoords_;

    /** Coordinate for specifying radial coordinate. */
    public static final FloatingCoord RADIAL_COORD =
        FloatingCoord.createCoord( SkyCoord.createRadiusInputMeta(), false );

    /** Instance for use with Plane plot. */
    public static final AreaForm<PlaneDataGeom> PLANE_INSTANCE =
            new AreaForm<PlaneDataGeom>( AreaCoord.PLANE_COORD, new Coord[0] ) {
        protected PolygonOutliner createOutliner( PolygonMode.Glypher pg,
                                                  int minSize,
                                                  MarkerShape minShape ) {
            return PolygonOutliner
                  .createPlaneAreaOutliner( getAreaCoord(), 0, pg,
                                            minSize, minShape );
        }
    };

    /** Instance for use with Sky plot. */
    public static final AreaForm<SkyDataGeom> SKY_INSTANCE =
            new AreaForm<SkyDataGeom>( AreaCoord.SKY_COORD, new Coord[ 0 ] ) {
        protected PolygonOutliner createOutliner( PolygonMode.Glypher pg,
                                                  int minSize,
                                                  MarkerShape minShape ) {
            return PolygonOutliner
                  .createSkyAreaOutliner( getAreaCoord(), 0, pg,
                                          minSize, minShape );
        }
    };

    /** Instance for use with Sphere plot. */
    public static final AreaForm<SphereDataGeom> SPHERE_INSTANCE =
            new AreaForm<SphereDataGeom>( AreaCoord.SPHERE_COORD,
                                          new Coord[] { RADIAL_COORD } ) {
        protected PolygonOutliner createOutliner( PolygonMode.Glypher pg,
                                                  int minSize,
                                                  MarkerShape minShape ) {
            return PolygonOutliner
                  .createSphereAreaOutliner( getAreaCoord(), 0, RADIAL_COORD, 1,
                                             pg, minSize, minShape );
        }
    };

    /** Config key for polygon painting mode option. */
    public static final ConfigKey<PolygonMode> POLYMODE_KEY =
        PolygonForms.POLYMODE_KEY;

    /** Config key for polygon fast drawing flag key. */
    public static final ConfigKey<Boolean> ISFAST_KEY =
        PolygonForms.ISFAST_KEY;

    /**
     * Constructor.
     *
     * @param   areaCoord  area reading coordinate
     * @param   otherCoords   additional coordinates associated with this form
     */
    private AreaForm( AreaCoord<DG> areaCoord, Coord[] otherCoords ) {
        areaCoord_ = areaCoord;
        otherCoords_ = otherCoords;
    }

    public String getFormName() {
        return "Area";
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_AREA;
    }

    public String getFormDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a region on the plotting surface",
            "specified by a string or array of numbers.",
            "The area may be specified as an STC-S string",
            "(as for example in an ObsCore or EPN-TAP",
            "<code>s_region</code> column)",
            "or using an array of numbers representing a",
            "polygon, circle or point as flagged using the DALI/VOTable",
            "extended type (xtype) marker,",
            "or as an ASCII-encoded MOC.",
            "</p>",
            "<p>Areas smaller than a configurable threshold size",
            "in pixels are by default represented by a replacement marker,",
            "so the position of even a very small area",
            "is still visible on the screen.",
            "</p>",
            "<p>This plot type is generally intended for displaying",
            "relatively small shapes such as instrument footprints.",
            "It can be used for areas that are larger as well,",
            "but there may be issues with use,",
            "for instance auto-determination of the initial plot region",
            "may not work so well,",
            "and the rendering of shapes that are large relative to the sky",
            "may be inaccurate.",
            "These issues may be addressed in future releases.",
            "</p>",
        } );
    }

    public int getPositionCount() {
        return 0;
    }

    public Coord[] getExtraCoords() {
        return PlotUtil.arrayConcat( new Coord[] { areaCoord_ }, otherCoords_ );
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            POLYMODE_KEY,
            ISFAST_KEY,
            PolygonOutliner.MINSIZE_KEY,
            PolygonOutliner.MINSHAPE_KEY,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        PolygonMode polyMode = config.get( POLYMODE_KEY );
        boolean isFast = config.get( ISFAST_KEY ).booleanValue();
        int minSize = config.get( PolygonOutliner.MINSIZE_KEY );
        MarkerShape minShape = config.get( PolygonOutliner.MINSHAPE_KEY );
        PolygonMode.Glypher polyGlypher = polyMode.getGlypher( isFast );
        return createOutliner( polyGlypher, minSize, minShape );
    }

    /**
     * Constructs a PolygonOutliner from a glypher for this form.
     *
     * @param  polyGlypher  glyph painter
     * @param  minSize   threshold size for replacment markers
     * @param  minShape   shape for replacement markers
     * @return   new outliner
     */
    protected abstract PolygonOutliner
            createOutliner( PolygonMode.Glypher polyGlypher,
                            int minSize, MarkerShape minShape );

    /**
     * Returns the coordinate associated with this form.
     *
     * @return  coord
     */
    public AreaCoord<DG> getAreaCoord() {
        return areaCoord_;
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        @SuppressWarnings("unchecked")
        DG tgeom = (DG) geom;
        return areaCoord_.getAreaDataGeom( tgeom );
    }
}
