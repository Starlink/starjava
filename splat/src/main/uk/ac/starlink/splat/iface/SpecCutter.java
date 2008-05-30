/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-JUN-2001 (Peter W. Draper):
 *       Original version.
 *     28-JAN-2003 (Peter W. Draper):
 *       Added delete facilities.
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Sort;

/**
 * Cutter and deleter of sections of a spectrum. It creates a new
 * spectrum that is added to the global list and can then be used as a
 * normal spectrum (i.e. displayed, deleted or saved).
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 * @see "The Singleton Design Pattern"
 */
public class SpecCutter
{
    /**
     * Count of all spectra created. Used to generate unique names for
     * new spectra.
     */
    private static int count = 0;

    /**
     * Create the single class instance.
     */
    private static final SpecCutter instance = new SpecCutter();

    /**
     *  The global list of spectra and plots.
     */
    private GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Return reference to the only allowed instance of this class.
     */
    public static SpecCutter getInstance()
    {
        return instance;
    }

    /**
     * Cut out the current view of a spectrum. Returns true for
     * success.  The new spectrum created is added to the globallist
     * and a reference to it is returned (null for failure). The new
     * spectrum is memory resident and has shortname:
     * <pre>
     *   "Cut <i> of <spectrum.getShortName()>"
     * </pre>
     * Where <i> is replaced by a unique integer and
     * <spectrum.getShortName()> by the short name of the spectrum.
     *
     * @param spectrum the spectrum to cut.
     * @param plot the displaying PlotControl (used to get range of
     *             spectrum that is being viewed).
     *
     * @return the new spectrum created from the viewable cut.
     */
    public SpecData cutView( SpecData spectrum, PlotControl plot )
    {
        try {
            //  Get the range of physical coordinates that are displayed?
            double[] viewRange = plot.getViewRange();

            //  Extract the new spectrum.
            SpecData newSpectrum = spectrum.getSect( makeName(spectrum),
                                                     viewRange );
            globalList.add( newSpectrum );
            return newSpectrum;

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
     * @return the new spectrum.
     */
    public SpecData cutRanges( SpecData spectrum, double[] ranges )
    {
        //  Sort and merge the ranges.
        double[] cleanRanges = Sort.sortAndMerge( ranges );
        try {
            //  Extract the new spectrum.
            SpecData newSpectrum = spectrum.getSect( makeName( spectrum ),
                                                     cleanRanges );
            globalList.add( newSpectrum );
            return newSpectrum;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Delete a range of coordinates from the spectrum, creating a
     * new single spectrum from the remaining data. The new spectrum
     * is added to the global list and a reference to it is returned
     * (null for failure of any kind). The new spectrum is memory
     * resident and has shortname:
     * <pre>
     *   "Cut <i> of <spectrum.getShortName()>"
     * </pre>
     * Where <i> is replaced by a unique integer and
     * <spectrum.getShortName()> by the short name of the spectrum.
     *
     * @param spectrum the spectrum.
     * @param ranges the physical coordinate ranges of the regions
     *               that are to be deleted. These should be in
     *               pairs. The extracted values are sorted in
     *               increasing coordinate and any overlap regions are
     *               merged.
     *
     * @return the new spectrum.
     */
    public SpecData deleteRanges( SpecData spectrum, double[] ranges )
    {
        //  Sort and merge the ranges.
        double[] cleanRanges = Sort.sortAndMerge( ranges );
        try {
            //  Extract the new spectrum.
            SpecData newSpectrum = spectrum.getSubSet( makeName(spectrum),
                                                       cleanRanges );
            globalList.add( newSpectrum );
            return newSpectrum;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Remove ranges of values from the spectrum, creating a new single
     * spectrum from the remaining data, and filling the removed values with
     * linear interpolation. The new spectrum is added to the global list and
     * a reference to it is returned (null for failure of any kind). The new
     * spectrum is memory resident and has shortname:
     * <pre>
     *   "Cut <i> of <spectrum.getShortName()>"
     * </pre>
     * Where <i> is replaced by a unique integer and
     * <spectrum.getShortName()> by the short name of the spectrum.
     *
     * @param spectrum the spectrum.
     * @param ranges the physical coordinate ranges of the regions
     *               that are to be deleted and interpolated. These should be
     *               in pairs. The extracted values are sorted in
     *               increasing coordinate and any overlap regions are
     *               merged.
     *
     * @return the new spectrum.
     */
    public SpecData interpRanges( SpecData spectrum, double[] ranges )
    {
        //  Sort and merge the ranges.
        double[] cleanRanges = Sort.sortAndMerge( ranges );
        try {
            //  Extract the new spectrum.
            SpecData newSpectrum = 
                spectrum.getInterpolatedSubSet( makeName( spectrum ),
                                                cleanRanges );
            globalList.add( newSpectrum );
            return newSpectrum;
        }
        catch (Exception e) {
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
        return "Cut " + (++count) + " of " + spectrum.getShortName();
    }
}
