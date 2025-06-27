package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.layer.ArrayShapePlotter;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.plot2.task.CoordSpec;

/**
 * Aggregates user-supplied information about a coordinate value used
 * as input for a plot.
 * The <code>dataLabels</code> and <code>colDatas</code> arrays both
 * correspond to (and have the same array size as) the
 * {@link uk.ac.starlink.ttools.plot2.data.Coord#getInputs Inputs}
 * arrays for the coord.
 *
 * @see   CoordPanel
 */
public class GuiCoordContent {

    private final Coord coord_;
    private final String[] dataLabels_;
    private final ColumnData[] colDatas_;
    private final DomainMapper[] domainMappers_;
    private final String[] valueIds_;

    /**
     * Constructor.
     *
     * @param   coord   plot coordinate definition
     * @param  dataLabels   array of strings naming quantities
     *                      for the user variables constituting the coord value;
     *                      these are typically the values typed in by the user
     * @param  colDatas  array of column data arrays supplying values
     *                   for the user variables constituting the coord value
     * @param  domainMappers  array of DomainMappers used to decode values
     *                        from the user variables
     * @param  valueIds  array of strings identifying the values returned
     *                   by the column datas; if these values change,
     *                   the numeric content of the colDatas can be expected
     *                   to change
     */
    public GuiCoordContent( Coord coord, String[] dataLabels,
                            ColumnData[] colDatas, DomainMapper[] domainMappers,
                            String[] valueIds ) {
        coord_ = coord;
        dataLabels_ = dataLabels;
        colDatas_ = colDatas;
        domainMappers_ = domainMappers;
        valueIds_ = valueIds;
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

    /**
     * Returns value identifiers for user input variables.
     * If these values change, the numeric values supplied by the column datas
     * may change.
     *
     * @return  value identifiers
     */
    public String[] getValueIds() {
        return valueIds_;
    }

    /**
     * Returns the domain mapper objects corresponding to the user input
     * variables.
     *
     * @return  nUserInfo-element array of domain mappers
     */
    public DomainMapper[] getDomainMappers() {
        return domainMappers_;
    }

    /**
     * Utility method to generate a mapping from user coordinate names
     * to their string specifications, given a set of GuiCoordContents.
     *
     * @param  contents  objects specifying selected coordinate values
     * @return  userInfo name-&gt;coord specifier map conveying
     *          the same information
     * @see   LayerCommand#getInputValues
     */
    public static CoordSpec[] getCoordSpecs( GuiCoordContent[] contents ) {
        List<CoordSpec> cspecs = new ArrayList<>();
        for ( GuiCoordContent content : contents ) {
            Input[] inputs = content.getCoord().getInputs();
            String[] dataLabels = content.getDataLabels();
            DomainMapper[] dms = content.getDomainMappers();
            ColumnData[] colDatas = content.getColDatas();
            int nuc = inputs.length;
            assert dataLabels.length == nuc;
            for ( int iuc = 0; iuc < nuc; iuc++ ) {
                Input input = inputs[ iuc ];
                String inName = LayerCommand.getInputName( input );
                String valueExpr = dataLabels[ iuc ];
                DomainMapper dm = AbstractPlot2Task.hasDomainMappers( input )
                                ? dms[ iuc ]
                                : null;
                DomainMapper dfltDm =
                      dm == null
                    ? null
                    : dm.getTargetDomain()
                        .getProbableMapper( colDatas[ iuc ].getColumnInfo() );
                cspecs.add( new CoordSpec( inName, valueExpr, dm, dfltDm ) );
            }
        }
        return cspecs.toArray( new CoordSpec[ 0 ] );
    }

    /**
     * Utility method to interrogate a list of GuiCoordContent objects
     * to get a suitable coordinate label (for instance for use as an
     * axis label) for one of the coordinates in a plot.
     * This is not bulletproof because the user coordinate name is not
     * guaranteed unique, but it will probably work as required.
     *
     * @param  userCoordName  user input coordinate name
     * @param  contents  list of GuiCoordContent values associated
     *                   with a plot; null is permitted, and will give
     *                   a null result
     * @return  string that the user will recognise as applying to
     *          <code>userCoordName</code> for plots generated by this control,
     *          or null if no result is found
     * @see uk.ac.starlink.ttools.plot2.data.Coord#getInputs
     */
    public static String getCoordLabel( String userCoordName,
                                        GuiCoordContent[] contents ) {

        /* Prepare a policy for which coordinate input selectors correspond
         * to values whose metadata are suitable for labelling the axes.
         * This list of criteria is a bit ad hoc. */
        List<Predicate<Input>> matchers = Arrays.asList(
            input -> input.getMeta().getLongName().equals( userCoordName ),
            input -> ArrayShapePlotter.matchesAxis( userCoordName, input )
        );

        /* Try to find a ColumnData whose metadata we should use,
         * according to those criteria. */
        if ( contents != null ) {
            for ( Predicate<Input> matcher : matchers ) {
                ColumnData cdata = getInputData( contents, matcher );

                /* If we get one, prepare an axis label accordingly. */
                if ( cdata != null ) {
                    ValueInfo dinfo = cdata.getColumnInfo();
                    String name = dinfo.getName();
                    String unit = dinfo.getUnitString();
                    return unit != null && unit.trim().length() > 0
                         ? name + " / " + unit
                         : name;
                }
            }
        }
        return null;
    }

    /**
     * Returns a ColumnData entered as the value of the first input coordinate
     * that matches a given criterion.
     *
     * @param  contents  list of GuiCoordContent values associated
     *                   with a plot; null is permitted, and will give
     *                   a null result
     * @param  inputMatcher  criterion for an Input to select
     * @return   column data corresponding to inputMatcher, or null
     */
    private static ColumnData getInputData( GuiCoordContent[] contents,
                                            Predicate<Input> inputMatcher ) {
        if ( contents != null ) {
            for ( GuiCoordContent content : contents ) {
                Input[] inputs = content.getCoord().getInputs();
                ColumnData[] coldatas = content.getColDatas();
                for ( int ii = 0; ii < inputs.length; ii++ ) {
                    if ( inputMatcher.test( inputs[ ii ] ) &&
                         coldatas[ ii ] != null ) {
                        return coldatas[ ii ];
                    }
                }
            }
        }
        return null;
    }
}
