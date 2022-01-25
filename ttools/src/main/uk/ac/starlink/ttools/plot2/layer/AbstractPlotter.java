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
 * supplied at construction time.  It also returns a null range style key.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2013
 */
public abstract class AbstractPlotter<S extends Style> implements Plotter<S> {

    private final String name_;
    private final Icon icon_;
    private final CoordGroup coordGrp_;
    private final boolean hasReports_;

    /**
     * Constructs a plotter with a given coordinate group and indication
     * of reporting status.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   coordGrp  coordinate group
     * @param   hasReports  whether plot reports are generated
     */
    protected AbstractPlotter( String name, Icon icon, CoordGroup coordGrp,
                               boolean hasReports ) {
        name_ = name;
        icon_ = icon;
        coordGrp_ = coordGrp;
        hasReports_ = hasReports;
    }

    /**
     * Constructs a plotter with no data coordinates or reports.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     */
    protected AbstractPlotter( String name, Icon icon ) {
        this( name, icon, CoordGroup.createEmptyCoordGroup(), false );
    }

    /**
     * Constructs a plotter with specified data positions and additional
     * coordinates, no report keys.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   npos   number of sets of positional coordinates
     * @param   extraCoords  coordinates other than positional coordinates
     */
    protected AbstractPlotter( String name, Icon icon, int npos,
                               Coord[] extraCoords ) {
        this( name, icon, CoordGroup.createCoordGroup( npos, extraCoords ),
              false );
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

    public boolean hasReports() {
        return hasReports_;
    }

    /**
     * The AbstractPlotter implementation returns null.
     */
    public Object getRangeStyleKey( S style ) {
        return null;
    }
}
