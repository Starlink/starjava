package uk.ac.starlink.ttools.plot2.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;

/**
 * Skeleton implementation of Plotter.
 * This doesn't do anything clever, just manages the basic members
 * supplied at construction time.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2013
 */
public abstract class AbstractPlotter<S extends Style> implements Plotter<S> {

    private final String name_;
    private final Icon icon_;
    private final CoordGroup coordGrp_;

    /**
     * Constructs a plotter given a coordinate group.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   coordGrp  coordinate group
     */
    protected AbstractPlotter( String name, Icon icon, CoordGroup coordGrp ) {
        name_ = name;
        icon_ = icon;
        coordGrp_ = coordGrp;
    }

    /**
     * Constructs a plotter with no data coordinates.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     */
    protected AbstractPlotter( String name, Icon icon ) {
        this( name, icon, CoordGroup.createEmptyCoordGroup() );
    }

    /**
     * Constructs a plotter with specified data positions and additional
     * coordinates.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   npos   number of sets of positional coordinates
     * @param   extraCoords  coordinates other than positional coordinates
     */
    protected AbstractPlotter( String name, Icon icon, int npos,
                               Coord[] extraCoords ) {
        this( name, icon, CoordGroup.createCoordGroup( npos, extraCoords ) );
    }

    public String getPlotterName() {
        return name_;
    }

    public Icon getPlotterIcon() {
        return icon_;
    }

    public CoordGroup getCoordGroup() {
        return coordGrp_;
    }
}
