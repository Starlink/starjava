/*
 * Copyright (C) 2003-2004 Central Laboratory of the Research Councils
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     1-JAN-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.WinMap;

import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.util.AverageFilter;
import uk.ac.starlink.splat.util.BinFilter;
import uk.ac.starlink.splat.util.KernelFactory;
import uk.ac.starlink.splat.util.KernelFilter;
import uk.ac.starlink.splat.util.MedianFilter;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.WaveletFilter;

/**
 * Filter of sections of a spectrum. It creates a new spectrum that is
 * added to the global list.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpecFilter
{
    /**
     * Count of all spectra created. Used to generate unique names for
     * new spectra.
     */
    protected static int count = 0;

    /**
     * Create the single class instance.
     */
    protected static final SpecFilter instance = new SpecFilter();

    /**
     * The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Return reference to the only allowed instance of this class.
     */
    public static SpecFilter getInstance()
    {
        return instance;
    }

    /**
     * Filter a spectrum or parts of a spectrum using a windowed
     * average filter. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the window size for averaging.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData averageFilter( SpecData spectrum, int width,
                                   double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        AverageFilter filter = new AverageFilter( localSpec.getYData(),
                                                  width );
        return updateSpectrum( localSpec, filter.eval(), null,
                               makeName( "Average", spectrum ) );
    }

    /**
     * Rebin a spectrum. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to rebin.
     * @param width the width used for rebinning.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData rebinFilter( SpecData spectrum, int width )
    {

        //  Odd filter as uses full spectrum and handles errors (for most
        //  filters errors become correlated, so just as well to have none,
        //  rebinning doesn't have that problem).
        BinFilter binFilter = new BinFilter( spectrum.getYData(), 
                                             spectrum.getYDataErrors(),
                                             width );
        double[] newData = binFilter.eval();
        double[] newDataErrors = binFilter.errors();

        try {
            EditableSpecData newSpec = SpecDataFactory.getInstance().
                createEditable( spectrum.getShortName(), spectrum );
            globalList.add( newSpec );

            //  Need to update AST frameset to shift to centre of new bin and
            //  have a scale factor...
            FrameSet frameSet = newSpec.getFrameSet();
            Frame newFrame = frameSet.getFrame( FrameSet.AST__BASE );
            double outa[] = new double[1];
            double outb[] = new double[1];
            double ina[] = new double[1];
            double inb[] = new double[1];
            ina[0] = 0.5 + 0.5 * (double) width;
            inb[0] = ina[0] + (double) width;
            outa[0] = 1.0;
            outb[0] = 2.0;
            WinMap winMap = new WinMap( 1, ina, inb, outa, outb );
            int current = frameSet.getCurrent();
            frameSet.addFrame( FrameSet.AST__BASE, winMap, newFrame );
            int base = frameSet.getCurrent();

            //  Restore current frame and set new base frame.
            frameSet.setCurrent( current );
            frameSet.setBase( base );

            return updateSpectrum( newSpec, newData, newDataErrors,
                                   makeName( "Rebin", spectrum ) );
        }
        catch (SplatException e) {
            //  Do nothing.
        }
        return null;
    }

    /**
     * Filter a spectrum or parts of a spectrum using a windowed
     * median filter. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the window size for estimated local median.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData medianFilter( SpecData spectrum, int width,
                                  double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        MedianFilter filter = new MedianFilter( localSpec.getYData(),
                                                width );
        return updateSpectrum( localSpec, filter.eval(), null,
                               makeName( "Median", spectrum ) );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a gaussian
     * weighted kernel. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the width to apply the gaussian weighting
     * @param fwhm the full width half maximum for the gaussian.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData gaussianFilter( SpecData spectrum, int width,
                                    double fwhm, double[] ranges,
                                    boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        double[] kernel = KernelFactory.gaussianKernel( width, fwhm );
        return kernelFilter( localSpec, kernel, "Gaussian" );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a lorentzian
     * weighted kernel. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the width to apply the gaussian weighting
     * @param lwidth the full width for the lorentzian.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData lorentzFilter( SpecData spectrum, int width,
                                   double lwidth, double[] ranges,
                                   boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        double[] kernel = KernelFactory.lorentzKernel( width, lwidth );
        return kernelFilter( localSpec, kernel, "Lorentz" );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a voigt
     * weighted kernel. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the width to apply the gaussian weighting.
     * @param gwidth the width for the gaussian part.
     * @param lwidth the width for the lorentzian part.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData voigtFilter( SpecData spectrum, int width,
                                 double gwidth, double lwidth,
                                 double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        double[] kernel = KernelFactory.voigtKernel( width, gwidth, lwidth );
        return kernelFilter( localSpec, kernel, "Voigt" );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a Hanning
     * weighted kernel. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the width of the filter.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData hanningFilter( SpecData spectrum, int width,
                                   double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        double[] kernel = KernelFactory.hanningKernel( width );
        return kernelFilter( localSpec, kernel, "Hanning" );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a Hamming
     * weighted kernel. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the width of the filter.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData hammingFilter( SpecData spectrum, int width,
                                   double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        double[] kernel = KernelFactory.hammingKernel( width );
        return kernelFilter( localSpec, kernel, "Hamming" );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a Welch
     * weighted kernel. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the width of the filter.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData welchFilter( SpecData spectrum, int width,
                                 double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        double[] kernel = KernelFactory.welchKernel( width );
        return kernelFilter( localSpec, kernel, "Welch" );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a Bartlett
     * weighted kernel. The new spectrum created is added to the global
     * list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param width the width of the filter.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData bartlettFilter( SpecData spectrum, int width,
                                    double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        double[] kernel = KernelFactory.bartlettKernel( width );
        return kernelFilter( localSpec, kernel, "Bartlett" );
    }

    /**
     * Filter a spectrum or parts of a spectrum using another spectrum
     * as a source of a weighted kernel. The new spectrum created is
     * added to the global list and a reference to it is returned
     * (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param kernelSpec the spectrum whose data component is a
     *                   weighting kernel.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData specKernelFilter( SpecData spectrum, SpecData kernelSpec,
                                      double[] ranges, boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        return kernelFilter( localSpec, kernelSpec.getYData(),
                             kernelSpec.getShortName() );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a wavelet
     * to "denoise" it. The new spectrum created is added to the
     * global list and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param wavelet the name of the wavelet filter
     *                (from WaveletFilter.WAVELETS)
     * @param percent the percentage of wavelet coefficients to remove.
     * @param ranges a series of coordinate ranges to include or
     *        exclude (null for none).
     * @param include true if the ranges should be included.
     *
     * @return the new spectrum, null if fails.
     */
    public SpecData waveletFilter( SpecData spectrum, String wavelet,
                                   double percent, double[] ranges,
                                   boolean include )
    {
        EditableSpecData localSpec = applyRanges( spectrum, ranges, include );
        WaveletFilter filter = new WaveletFilter( localSpec.getYData(),
                                                  wavelet, percent );
        return updateSpectrum( localSpec, filter.eval(), null,
                               makeName( wavelet, spectrum ) );
    }

    /**
     * Filter a spectrum or parts of a spectrum using a weighted
     * kernel. The modified spectrum is added to the global list
     * and a reference to it is returned (null for failure).
     *
     * @param spectrum the spectrum to filter.
     * @param kernel the weighting kernel.
     * @param name short name for the spectrum.
     *
     * @return the new spectrum, null if fails.
     */
    protected SpecData kernelFilter( EditableSpecData spectrum,
                                     double[] kernel, String name )
    {
        KernelFilter filter = new KernelFilter( spectrum.getYData(),
                                                kernel );
        return updateSpectrum( spectrum, filter.eval(), null,
                               makeName( name, spectrum ) );
    }

    /**
     * Update a spectrum with a new data component and optional associated
     * errors. Also gives it a hopefully useful name.
     */
    protected SpecData updateSpectrum( EditableSpecData newSpec,
                                       double[] newData, 
                                       double[] newDataErrors,
                                       String newName )
    {
        try {
            if ( newDataErrors == null ) {
                newSpec.setFullDataQuick( newSpec.getFrameSet(),
                                          newSpec.getCurrentDataUnits(),
                                          newData );
            }
            else {
                newSpec.setFullDataQuick( newSpec.getFrameSet(),
                                          newSpec.getCurrentDataUnits(),
                                          newData, newDataErrors );
            }
            newSpec.setShortName( newName );
            return newSpec;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  Generate a name for the cut spectrum.
     *
     *  @param ident an identifying prefix for name (defaults to Filter).
     *  @param spectrum the spectrum to be cut.
     */
    protected String makeName( String ident, SpecData spectrum )
    {
        String name = "Filter " + (++count) + " of " + spectrum.getShortName();
        if ( ident != null ) {
            name = ident + " " + name;
        }
        return name;
    }

    /**
     * Apply any range cuts or deletes to a spectrum and return a new
     * spectrum. If no changes are needed than return an editable copy
     * of the original spectrum.
     */
    protected EditableSpecData applyRanges( SpecData spectrum,
                                            double[] ranges, boolean include )
    {
        try {
            EditableSpecData newSpec = null;
            if ( ranges != null && ranges.length != 0 ) {
                SpecCutter cutter = SpecCutter.getInstance();
                if ( include ) {
                    newSpec = (EditableSpecData) cutter.cutRanges( spectrum,
                                                                   ranges );
                }
                else {
                    newSpec = (EditableSpecData) cutter.deleteRanges( spectrum,
                                                                      ranges );
                }
            }
            else {
                newSpec = SpecDataFactory.getInstance().
                    createEditable( spectrum.getShortName(), spectrum );
                globalList.add( newSpec );
            }
            return newSpec;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
