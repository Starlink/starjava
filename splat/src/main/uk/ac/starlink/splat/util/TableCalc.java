/*
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     17-DEC-2008 (Peter W. Draper):
 *        Original version.
 */

package uk.ac.starlink.splat.util;


import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;

import uk.ac.starlink.splat.data.PropertyTable;
import uk.ac.starlink.splat.data.SpecData;

import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

/**
 * Class for performing JEL expression calculations using the meta-data of a
 * {@link SpecData} instance. Use the facilities of the TTOOLS package, so can
 * perform expressions using the special functions, (dates and times are what
 * we needed), as well as the more usual functions.
 *
 * @author Peter W. Draper
 * @version $Id:$
 */
public class TableCalc 
{
    private TableCalc() 
    {
        //  Do nothing.
    }

    /**
     * Perform a calculation using a SpecData instance. Returning the
     * result as a double.
     *
     * @param a {@link SpecData} instance to provide any necessary meta-data.
     * @param expression the JEL expression to evaluate
     * 
     * @return the value, or {@link SpecData.BAD} if the calculation fails.
     *
     * @throws SplatException if an error occurs or the expression cannot be
     *         compiled.
     */
    public static double calc( SpecData specData, String expression )
        throws SplatException
    {
        return calc( new PropertyTable( specData ), expression );
    }

    /**
     * Perform a calculation using a wrapped SpecData instance. Returning the
     * result as a double.
     *
     * @param table a {@link SpecData} instance wrapped so that it's meta-data
     *              are available as a single table row and columns.
     * @param expression the JEL expression to evaluate
     * 
     * @return the value, or {@link SpecData.BAD} if the calculation fails.
     *
     * @throws SplatException if an error occurs or the expression cannot be
     *         compiled.
     */
    public static double calc( PropertyTable table, String expression )
        throws SplatException
    {
        // Wrap the table for reading by the JEL utilities.
        final JELRowReader jelReader = new RandomJELRowReader( table );

        // Define library of functions that can be used (Math & specials).
        Library lib = JELUtils.getLibrary( jelReader );

        //  Compile the expression in this context.
        final CompiledExpression compEx;
        try {
            compEx = Evaluator.compile( expression, lib );
        }
        catch ( CompilationException e ) {
            throw new SplatException( e.getMessage(), e );
        }

        //  And evaluate.
        double result = 0.0;
        try {
            result = jelReader.evaluateDouble( compEx );
        }
        catch ( NullPointerException e ) {
            result = SpecData.BAD;
        }
        catch ( Throwable e ) {
            throw new SplatException( "Failed to calculate expression: " +
                                      expression, e );
        }
        if ( Double.isNaN( result ) ) {
            result = SpecData.BAD;
        }
        return result;
    }
}
