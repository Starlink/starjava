/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import diva.canvas.interactor.Interactor;
import diva.util.java2d.ShapeUtilities;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import jsky.image.graphics.ImageFigure;
import java.awt.Paint;
import java.awt.Graphics2D;

/**
 * Create a composite figure that represents a circular aperture plus
 * annulus.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AnnulusFigure extends ImageFigure
{
    private AnnulusArea mainArea = null;

    /**
     * Create an annulus figure.
     *
     * @param interactor determines the selection behavior of the
     *                   figure group (may be null)
     */
    public AnnulusFigure( double radius, Paint fill, Paint outline,
                          float lineWidth, Interactor interactor )
    {
        super( new AnnulusArea( radius ), fill, outline, lineWidth,
               interactor );
        mainArea = (AnnulusArea) getShape();
        repaint();
    }

    /**
     * Set the central position. XXX account for relativeness..
     */
    public void setPosition( Point2D.Double pt )
    {
        repaint();
        mainArea.setPosition( pt );
        repaint();
    }

    /**
     * Get the position of the aperture.
     */
    public Point2D.Double getPosition()
    {
        return mainArea.getPosition();
    }

    /**
     * Set the radius of the aperture
     */
    public void setRadius( double radius )
    {
        repaint();
        mainArea.setRadius( radius );
        repaint();
    }

    /**
     * Get the radius of the aperture
     */
    public double getRadius()
    {
        return mainArea.getRadius();
    }

    /**
     * Set the innerscale.
     */
    public void setInnerscale( double innerscale )
    {
        repaint();
        mainArea.setInnerscale( innerscale );
        repaint();
    }

    /**
     * Get the innerscale
     */
    public double getInnerscale()
    {
        return mainArea.getInnerscale();
    }

    /**
     * Set the outerscale.
     */
    public void setOuterscale( double outerscale )
    {
        repaint();
        mainArea.setOuterscale( outerscale );
        repaint();
    }

    /**
     * Get the outerscale
     */
    public double getOuterscale()
    {
        return mainArea.getOuterscale();
    }

    //
    // Make transformation changes apply to the underlying
    // object. Note there is no need to pass this on to the
    // sub-class (which offers less optimal transformations on
    // generalised shapes).
    //
    public void transform( AffineTransform at ) 
    {
        repaint();  // Attempt to stop trailing parts...
        mainArea.transform( at );
        repaint();
    }
    public void translate( double x, double y ) 
    {
        repaint();
        mainArea.translate( x, y );
        repaint();
    }
}
