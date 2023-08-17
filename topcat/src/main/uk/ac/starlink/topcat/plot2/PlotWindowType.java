package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ListModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Enum defining the different plot window types.
 * A method to create an instance of each window is provided.
 *
 * @author   Mark Taylor
 * @since    10 May 2018
 */
public enum PlotWindowType {

    /** Histogram. */
    HISTOGRAM( "Histogram", ResourceIcon.PLOT2_HISTOGRAM,
               "Plane plotting window configured for convenience "
             + "when plotting histograms") {
        public StackPlotWindow<?,?>
                createWindow( Component parent, ListModel<TopcatModel> tm ) {
            return new HistogramPlotWindow( parent, tm );
        }
    },

    /** Plane. */
    PLANE( "Plane", ResourceIcon.PLOT2_PLANE, "Plane plotting window" ) {
        public StackPlotWindow<?,?>
                createWindow( Component parent, ListModel<TopcatModel> tm ) {
            return new PlanePlotWindow( parent, tm );
        }
    },

    /** Sky. */
    SKY( "Sky", ResourceIcon.PLOT2_SKY, "Sky plotting window" ) {
        public StackPlotWindow<?,?>
                createWindow( Component parent, ListModel<TopcatModel> tm ) {
            return new SkyPlotWindow( parent, tm );
        }
    },

    /** Cube. */
    CUBE( "Cube", ResourceIcon.PLOT2_CUBE,
          "3D plotting window using Cartesian coordinates") {
        public StackPlotWindow<?,?>
                createWindow( Component parent, ListModel<TopcatModel> tm ) {
            return new CubePlotWindow( parent, tm );
        }
    },

    /** Sphere. */
    SPHERE( "Sphere", ResourceIcon.PLOT2_SPHERE,
            "3D plotting window using spherical polar coordinates" ) {
        public StackPlotWindow<?,?>
                createWindow( Component parent, ListModel<TopcatModel> tm ) {
            return new SpherePlotWindow( parent, tm );
        }
    },

    /** Matrix. */
    MATRIX( "Corner", ResourceIcon.PLOT2_MATRIX,
            "Window for grid of scatter plots" ) {
        public StackPlotWindow<?,?>
                createWindow( Component parent, ListModel<TopcatModel> tm ) {
            return new MatrixPlotWindow( parent, tm );
        }
    },

    /** Time. */
    TIME( "Time", ResourceIcon.PLOT2_TIME, "Time series plotting window" ) {
        public StackPlotWindow<?,?>
                createWindow( Component parent, ListModel<TopcatModel> tm ) {
            return new TimePlotWindow( parent, tm );
        }
    };

    private final String name_;
    private final Icon icon_;
    private final String description_;

    /**
     * Constructor.
     *
     * @param  name  one-word plot type name
     * @param  icon  icon
     * @param  description  short description
     */
    private PlotWindowType( String name, Icon icon, String description ) {
        name_ = name;
        icon_ = icon;
        description_ = description;
    }

    /**
     * Returns plot type name.
     *
     * @return  one-word name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns plot type icon.
     *
     * @return  icon
     */
    public Icon getIcon() {
        return icon_;
    }

    /**
     * Returns short description for plot type.
     *
     * @return  description phrase
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Creates an instance of this plot type's window.
     *
     * @param  parent   parent component, used for placement
     * @param  tablesModel  list of available tables
     */
    public abstract StackPlotWindow<?,?>
            createWindow( Component parent,
                          ListModel<TopcatModel> tablesModel );

    @Override
    public String toString() {
        return name_;
    }
}
