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
 * Skeleton implementation of Plotter used for plotters that plot positional
 * data.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public abstract class TuplePlotter<S extends Style> implements Plotter<S> {

    private final String name_;
    private final Icon icon_;
    private final Coord[] extraCoords_;

    /**
     * Constructor.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   extraCoords  coordinates other than positional coordinates
     */
    protected TuplePlotter( String name, Icon icon, Coord[] extraCoords ) {
        name_ = name;
        icon_ = icon;
        extraCoords_ = extraCoords;
    }

    public String getPlotterName() {
        return name_;
    }

    public Icon getPlotterIcon() {
        return icon_;
    }

    /**
     * Returns true.
     */
    public boolean hasPosition() {
        return true;
    }

    public Coord[] getExtraCoords() {
        return extraCoords_.clone();
    }
}
