package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * Aggregates user-supplied information about a coordinate value used
 * as input for a plot.
 * The <code>dataLabels</code> and <code>colDatas<code> arrays both
 * correspond to (and have the same array size as) the 
 * {@link uk.ac.starlink.ttools.plot2.data.Coord#getUserInfos userInfos}
 * arrays for the coord.
 *
 * @see   CoordPanel
 */
public class GuiCoordContent {

    private final Coord coord_;
    private final String[] dataLabels_;
    private final ColumnData[] colDatas_;

    /**
     * Constructor.
     *
     * @param   coord   plot coordinate definition
     * @param  dataLabels   array of strings naming quantities
     *                      for the user variables constituting the coord value
     * @param  colDatas  array of column data arrays supplyig values
     *                   for the user variables constituting the coord value
     */
    public GuiCoordContent( Coord coord, String[] dataLabels,
                            ColumnData[] colDatas ) {
        coord_ = coord;
        dataLabels_ = dataLabels;
        colDatas_ = colDatas;
    }

    /**
     * Returns the coordinate definition.
     *
     * @return   coord definition
     */
    public Coord getCoord() {
        return coord_;
    }

    /**
     * Returns the labels describing user input variables.
     *
     * @return   nUserInfo-element array of user variable labels
     */
    public String[] getDataLabels() {
        return dataLabels_;
    }

    /**
     * Returns the column data objects for user input variables.
     *
     * @return   nUserInfo-element array of column data objects
     */
    public ColumnData[] getColDatas() {
        return colDatas_;
    }
}
