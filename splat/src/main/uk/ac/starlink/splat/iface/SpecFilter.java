package uk.ac.starlink.splat.iface;


import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.AverageFilter;

/**
 * Filter of sections of a spectrum. It creates a new spectrum that is
 * added to the global list.
 *
 * @since $Date$
 * @since 14-JUN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see SpecData, "The Singleton Design Pattern".
 */
public class SpecFilter
{
    /**
     * Count of all spectra created. Used to generate unique names for
     * new spectra.
     */
    private static int count = 0;

    /**
     * Create the single class instance.
     */
    private static final SpecFilter instance = new SpecFilter();

    /**
     *  The global list of spectra and plots.
     */
    private GlobalSpecPlotList globalList = GlobalSpecPlotList.getReference();

    /**
     * Return reference to the only allowed instance of this class.
     */
    public static SpecFilter getReference()
    {
        return instance;
    }

    /**
     * Filter the current view of a spectrum. Returns true for
     * success.  The new spectrum created is added to the globallist
     * and a reference to it is returned (null for failure). The new
     * spectrum is memory resident and has shortname:
     * <pre>
     *   "Filter <i> of <spectrum.getShortName()>"
     * </pre>
     * Where <i> is replaced by a unique integer and
     * <spectrum.getShortName()> by the short name of the spectrum.
     *
     * @param spectrum the spectrum to filter.
     * @param plot the displaying PlotControl (used to get range of
     *             spectrum that is being viewed).
     *
     * @return the new spectrum.
     */
    public SpecData filter( SpecData spectrum, PlotControl plot )
    {
        try {
            AverageFilter averageFilter =
                new AverageFilter( spectrum.getYData(), 100 );

            double[] newdata = averageFilter.eval();

            EditableSpecData newSpec = SpecDataFactory.getReference().
                createEditable( makeName( spectrum ) );

            newSpec.setData( spectrum.getXData(), newdata );

            globalList.add( newSpec );
            return newSpec;

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cut out a range of coordinates from the spectrum, creating a
     * new single spectrum from the extracted data. The new spectrum
     * is added to the global list and a reference to it is returned
     * (null for failure of any kind). The new spectrum is memory
     * resident and has shortname:
     * <pre>
     *   "Cut <i> of <spectrum.getShortName()>"
     * </pre>
     * Where <i> is replaced by a unique integer and
     * <spectrum.getShortName()> by the short name of the spectrum.
     *
     * @param spectrum the spectrum to cut.
     * @param ranges the physical coordinate ranges of the regions
     *               that are to be extracted. These should be in
     *               pairs. The extracted values are sorted in
     *               increasing coordinate and any overlap regions are
     *               merged.
     *
     * @return the new spectrum created from the viewable cut.
     * @see #sortAndMerge
     */
    public SpecData cutView( SpecData spectrum, double[] ranges )
    {
        //  Sort and merge the ranges.
        double[] cleanRanges = sortAndMerge( ranges );
        try {
            //  Extract the new spectrum.
            SpecData newSpectrum = spectrum.getSect(makeName(spectrum),
                                                    cleanRanges );
            globalList.add( newSpectrum );
            return newSpectrum;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  Generate a name for the cut spectrum.
     *
     *  @param spectrum the spectrum to be cut.
     */
    private String makeName( SpecData spectrum )
    {
        return "Filter " + (++count) + " of " + spectrum.getShortName();
    }

    /**
     * Sort a list of ranges into increasing order and merge any
     * overlapped ranges into single ranges (do this to make sure that
     * ranges define a monotonic set).
     *
     * @param ranges the ranges that require sorting and merging.
     * @return the sorted and merged set of ranges.
     */
    public double[] sortAndMerge( double[] ranges )
    {
        //  Short number of ranges expected so nothing clever
        //  required, but we do need to retain the lower-upper bounds
        //  pairing for a proper merge when overlapped, cannot just
        //  sort the array and go with that.
        double[] sorted = (double[]) ranges.clone();

        //  Sort array using an insertion sort.
        //  ==========
        double xl = 0.0;
        double xh = 0.0;
        int j = 0;
        boolean bigger = false;
        for ( int i = 2; i < sorted.length; i+=2 ) {

            //  Store the current value (the bottom).
            xl = sorted[i];
            xh = sorted[i+1];

            //  Look at all values above this one on the stack.
            bigger = false;
            for ( j = i - 2; j >= 0; j-=2 ) {
                if ( xl > sorted[j] ) {
                    bigger = true;
                    break;
                }

                //  Move values up one to make room for next value
                //  (x or ranges[j] which ever greater).
                sorted[j+2] = sorted[j];
                sorted[j+3] = sorted[j+1];
            }
            if ( ! bigger ) {
                //  Nothing bigger so put this one on the top.
                j = -2;
            }

            //  Insert val below first value greater than it, or put on
            //  top if none bigger.
            sorted[j+2] = xl;
            sorted[j+3] = xh;
        }

        //  Sort completed so now merge any overlapped ranges into
        //  single ranges.
        double[] merged = new double[sorted.length];
        int n = 0;
        merged[0] = sorted[0];
        merged[1] = sorted[1];
        for ( int i = 2; i < merged.length; i+=2 ) {

            if ( merged[n+1] > sorted[i] ) {
                //  Current range end overlaps beginning of next
                //  range, so copy end of next range to be end of this
                //  range.
                merged[n+1] = sorted[i+1];
            } else {

                //  No overlap so just copy range.
                n+=2;
                merged[n] = sorted[i];
                merged[n+1] = sorted[i+1];
            }
        }
        n+=2;

        if ( n != sorted.length ) {

            //  Had some overlaps so trim off unnecessary end.
            double[] trimmed = new double[n];
            System.arraycopy( merged, 0, trimmed, 0, n );
            merged = trimmed;
        }

        return merged;
    }
}
