/*
 * ESO Archive
 *
 * $Id: MinMaxDescriptor.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.operator;

import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import javax.media.jai.ROI;
import javax.media.jai.util.Range;
import javax.media.jai.registry.RIFRegistry;


/**
 * A single class that is both an OperationDescriptor and
 * a RenderedImageFactory.
 */

public class MinMaxDescriptor extends OperationDescriptorImpl implements RenderedImageFactory {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "minmax" operation.
     */
    private static final String[][] resources = {
        {"GlobalName", "minmax"},
        {"LocalName", "minmax"},
        {"Vendor", "jsky"},
        {"Description", "Calculate image min and max values, ignoring the given blank or bad pixel value"},
        {"DocURL", "ftp://ftp.archive.eso.org/pub/jsky/javadoc/jsky/image/operator/MinMaxDescriptor.html"},
        {"Version", "1.0"},
        {"arg0Desc", "The region of the image to scan"},
        {"arg1Desc", "The horizontal sampling rate, may not be less than 1."},
        {"arg2Desc", "The vertical sampling rate, may not be less than 1."},
        {"arg3Desc", "Ignore pixels with this value."}
    };

    /**
     * The modes that this operator supports. maybe one or more of
     * "rendered", "renderable", "collection", and "renderableCollection".
     */
    private static final String[] supportedModes = {
        "rendered", "renderable"
    };

    /**
     * The parameter names for the "minmax" operation.
     */
    private static final String[] paramNames = {
        "roi",
        "xPeriod",
        "yPeriod",
        "ignore"
    };

    /**
     *  The class types for the parameters of the "minmax" operation.
     *  User defined classes can be used here as long as the fully
     *  qualified name is used and the classes can be loaded.
     */
    private static final Class[] paramClasses = {
        javax.media.jai.ROI.class,
        java.lang.Integer.class,
        java.lang.Integer.class,
        java.lang.Double.class
    };

    /**
     * The default parameter values for the "minmax" operation
     * when using a ParameterBlockJAI.
     */
    private static final Object[] paramDefaults = {
        null,
        new Integer(1),
        new Integer(1),
        null
    };

    /**
     * Describes the valid parameter ranges (null means any range for that class)
     */
    private static final Object[] validParamValues = {
        null,
        new Range(Integer.class, new Integer(1), null), // must be >= 1
        new Range(Integer.class, new Integer(1), null), // must be >= 1
        null
    };


    /** Constructor. */
    public MinMaxDescriptor() {
        // XXX JAI 1.0.2 super(resources, 1, paramClasses, paramNames, paramDefaults);
        super(resources, supportedModes, 1, paramNames, paramClasses, paramDefaults, validParamValues);
    }


    /**
     * Register this operation descriptor
     */
    public static void register() {
        MinMaxDescriptor MinMaxDescriptor = new MinMaxDescriptor();
        OperationDescriptor odesc = MinMaxDescriptor;
        RenderedImageFactory rif = MinMaxDescriptor;

        String operationName = "minmax";
        String productName = "jsky";
        OperationRegistry or = JAI.getDefaultInstance().getOperationRegistry();
        or.registerDescriptor(odesc);
        // XXX JAI 1.0.2: or.registerOperationDescriptor(odesc,operationName);
        // XXX JAI 1.0.2: or.registerRIF(operationName,productName,rif);
        RIFRegistry.register(or, operationName, productName, rif);
    }


    /**
     *  Creates a MinMaxOpImage with the given ParameterBlock if the
     *  MinMaxOpImage can handle the particular ParameterBlock.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        if (!validateParameters(paramBlock)) {
            return null;
        }
        return new MinMaxOpImage(paramBlock.getRenderedSource(0),
                new ImageLayout(),
                (ROI) paramBlock.getObjectParameter(0),
                (Integer) paramBlock.getObjectParameter(1),
                (Integer) paramBlock.getObjectParameter(2),
                (Double) paramBlock.getObjectParameter(3));
    }


    /**
     * Checks that all parameters in the ParameterBlock have the
     * correct type before constructing the MinMaxOpImage
     */
    public boolean validateParameters(ParameterBlock paramBlock) {
        int n = getParameterListDescriptor(getSupportedModes()[0]).getNumParameters();
        for (int i = 0; i < n; i++) {
            Object arg = paramBlock.getObjectParameter(i);
            if (arg == null) {
                return false;
            }
            if (i == 0) {
                if (!(arg instanceof ROI)) {
                    return false;
                }
            }
            else if (i == 1 || i == 2) {
                if (!(arg instanceof Integer)) {
                    return false;
                }
                int val = ((Integer) arg).intValue();
                if (val < 1) {
                    return false;
                }
            }
            else if (i == 3) {
                if (!(arg instanceof Double)) {
                    return false;
                }
            }
        }
        return true;
    }
}

