package uk.ac.starlink.ttools.plot2.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * Skeleton implementation of Plotter.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2013
 */
public abstract class AbstractPlotter<S extends Style> implements Plotter<S> {

    private final String name_;
    private final Icon icon_;
    private final int npos_;
    private final Coord[] extraCoords_;

    /**
     * Constructor.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   npos   number of sets of positional coordinates
     * @param   extraCoords  coordinates other than positional coordinates
     */
    protected AbstractPlotter( String name, Icon icon, int npos,
                               Coord[] extraCoords ) {
        name_ = name;
        icon_ = icon;
        npos_ = npos;
        extraCoords_ = extraCoords;
    }

    public String getPlotterName() {
        return name_;
    }

    public Icon getPlotterIcon() {
        return icon_;
    }

    public int getPositionCount() {
        return npos_;
    }

    public Coord[] getExtraCoords() {
        return extraCoords_.clone();
    }
}
