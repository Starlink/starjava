/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import uk.ac.starlink.splat.data.AnalyticSpectrum;

/**
 * Interface defining the comment elements needed for a {@link FunctionFitter} 
 * that just evaluates the function value and its derivates. Intended for 
 * use with a {@link CompositeFunctionFitter} that wants to amalgamate
 * a series of {@link FunctionFitter}s.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see GaussianGenerator
 * @see LorentzGenerator
 * @see VoigtGenerator
 * @see FunctionFitter
 * @see LevMarqFunc
 * @see AnalyticSpectrum
 */
public interface FunctionGenerator
    extends FunctionFitter, LevMarqFunc, AnalyticSpectrum
{
    // Nothing to add.
}
