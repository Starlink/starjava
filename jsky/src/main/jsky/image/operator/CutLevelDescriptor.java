/*
 * ESO Archive
 *
 * $Id: CutLevelDescriptor.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
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

public class CutLevelDescriptor extends OperationDescriptorImpl implements RenderedImageFactory {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "cutlevel" operation.
     */
    private static final String[][] resources = {
        {"GlobalName", "cutlevel"},
        {"LocalName", "cutlevel"},
        {"Vendor", "jsky"},
        {"Description", "Calculate image min and max values, ignoring the given blank or bad pixel value"},
        {"DocURL", "ftp://ftp.archive.eso.org/pub/jsky/javadoc/jsky/image/operator/CutLevelDescriptor.html"},
        {"Version", "1.0"},
        {"arg0Desc", "The region of the image to scan"},
        {"arg1Desc", "Ignore pixels with this value."},
        {"arg2Desc", "Median value (min+max/2.) to use in place of bad pixels."}
    };

    /**
     * The modes that this operator supports. maybe one or more of
     * "rendered", "renderable", "collection", and "renderableCollection".
     */
    private static final String[] supportedModes = {
        "rendered", "renderable"
    };

    /**
     * The parameter names for the "cutlevel" operation.
     */
    private static final String[] paramNames = {
        "roi",
        "ignore",
        "median"
    };

    /**
     *  The class types for the parameters of the "cutlevel" operation.
     *  User defined classes can be used here as long as the fully
     *  qualified name is used and the classes can be loaded.
     */
    private static final Class[] paramClasses = {
        javax.media.jai.ROI.class,
        java.lang.Double.class,
        java.lang.Double.class
    };

    /**
     * The default parameter values for the "cutlevel" operation
     * when using a ParameterBlockJAI.
     */
    private static final Object[] paramDefaults = {
        null,
        null,
        null
    };


    /**
     * Describes the valid parameter ranges (null means any range for that class)
     */
    private static final Object[] validParamValues = {
        null,
        null,
        null
    };


    /** Constructor. */
    public CutLevelDescriptor() {
        // XXX JAI 1.0.2 super(resources, 1, paramClasses, paramNames, paramDefaults);
        super(resources, supportedModes, 1, paramNames, paramClasses, paramDefaults, validParamValues);
    }

    /**
     * Register this operation descriptor
     */
    public static void register() {
        CutLevelDescriptor CutLevelDescriptor = new CutLevelDescriptor();
        OperationDescriptor odesc = CutLevelDescriptor;
        RenderedImageFactory rif = CutLevelDescriptor;

        String operationName = "cutlevel";
        String productName = "jsky";
        OperationRegistry or = JAI.getDefaultInstance().getOperationRegistry();
        // XXX JAI 1.0.2: or.registerOperationDescriptor(odesc,operationName);
        // XXX JAI 1.0.2: or.registerRIF(operationName,productName,rif);
        or.registerDescriptor(odesc);
        RIFRegistry.register(or, operationName, productName, rif);
    }


    /**
     *  Creates a CutLevelOpImage with the given ParameterBlock if the
     *  CutLevelOpImage can handle the particular ParameterBlock.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        if (!validateParameters(paramBlock)) {
            return null;
        }
        return new CutLevelOpImage(paramBlock.getRenderedSource(0),
                new ImageLayout(),
                (ROI) paramBlock.getObjectParameter(0),
                (Double) paramBlock.getObjectParameter(1),
                (Double) paramBlock.getObjectParameter(2));
    }


    /**
     * Checks that all parameters in the ParameterBlock have the
     * correct type before constructing the CutLevelOpImage
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
            else if (!(arg instanceof Double)) {
                return false;
            }
        }
        return true;
    }
}

