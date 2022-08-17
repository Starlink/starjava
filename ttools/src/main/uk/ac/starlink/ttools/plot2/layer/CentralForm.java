package uk.ac.starlink.ttools.plot2.layer;

import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SphereDataGeom;

/**
 * ShapeForm that plots the central point of an Area.
 *
 * @author   Mark Taylor
 * @since    21 Apr 2020
 */
public class CentralForm<DG extends DataGeom> implements ShapeForm {

   private final AreaCoord<DG> areaCoord_;
   private final Coord[] otherCoords_;

    /** Instance for use with Plane plot. */
    public static final CentralForm<PlaneDataGeom> PLANE_INSTANCE =
            new CentralForm<PlaneDataGeom>( AreaCoord.PLANE_COORD,
                                            new Coord[ 0 ] );

    /** Instance for use with Sky plot. */
    public static final CentralForm<SkyDataGeom> SKY_INSTANCE =
            new CentralForm<SkyDataGeom>( AreaCoord.SKY_COORD,
                                          new Coord[ 0 ] );

    /** Instance for use with Sphere plot. */
    public static final CentralForm<SphereDataGeom> SPHERE_INSTANCE =
            new CentralForm<SphereDataGeom>( AreaCoord.SPHERE_COORD,
                                             new Coord[] { AreaForm
                                                          .RADIAL_COORD } );

    /**
     * Constructor.
     *
     * @param   areaCoord  area reading coordinate
     * @param   otherCoords   additional coordinates associated with this form
     */
    private CentralForm( AreaCoord<DG> areaCoord, Coord[] otherCoords ) {
        areaCoord_ = areaCoord;
        otherCoords_ = otherCoords;
    }

    public String getFormName() {
        return "Central";
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_MARK;
    }

    public String getFormDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots the nominal central point of an area.",
            "This appears just like a normal marker plot,",
            "but can be used when the available geometry information",
            "is an area description",
            "(such as an STC-S string or an array of polygon vertices)",
            "rather than coordinate values such as an X,Y pair.",
            "The position plotted is the nominal center of the shape",
            "as determined by the plotting code;",
            "that may or may not correspond to the actual center.",
            "</p>",
        } );
    }

    public int getBasicPositionCount() {
        return 0;       
    }

    public Coord[] getExtraCoords() {
        return PlotUtil.arrayConcat( new Coord[] { areaCoord_ }, otherCoords_ );
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            StyleKeys.MARKER_SHAPE,
            StyleKeys.SIZE,
        };
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        @SuppressWarnings("unchecked")
        DG tgeom = (DG) geom;
        return areaCoord_.getAreaDataGeom( tgeom );
    }

    public Outliner createOutliner( ConfigMap config ) {
        MarkerShape shape = config.get( StyleKeys.MARKER_SHAPE );
        int size = config.get( StyleKeys.SIZE );
        return MarkForm.createMarkOutliner( shape, size );
    }
}
