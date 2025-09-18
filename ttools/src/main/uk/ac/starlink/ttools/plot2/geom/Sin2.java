package uk.ac.starlink.ttools.plot2.geom;

/* This class implements the Sine (Orthographic)
 * projection.  Note that the tangent point
 * is assumed to be at the north pole.
 * This class assumes preallocated arrays for
 * maximum efficiency.
 */

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;

/**
 *  Sine (Orthographic) projecter implementation.
 *
 *  <p>This class is copied from the Skyview original class
 *  <code>skyview.geometry.projecter.Sin</code>.
 *  Apart from changing the name to Sin2 (which is significant since
 *  some implementation behaviour is name-dependent), it just shuffles
 *  the order of the coordinates so that the tangent point is
 *  at (1,0,0) instead of (0,0,1).
 *
 *  @author   Tom McGlynn
 *  @author   Mark Taylor
 *  @see      <a href="https://skyview.gsfc.nasa.gov/"
 *                    >https://skyview.gsfc.nasa.gov/</a>
 */
public final class Sin2 extends Projecter {
    

    /** Get the name of the component */
    public String getName() {
	return "Sin2";
    }
    
    /** Get a description of the component */
    public String getDescription () {
	return "Project as if seeing the sphere from a great distance";
    }
    
    /** Get the inverse transformation */
    public Deprojecter inverse() {
	return new Sin2.Sin2Deproj();
    }
    
    /** Is this an inverse of some other transformation? */
    public boolean isInverse(Transformer t) {
	return t.getName().equals("Sin2Deproj");
    }
    
    /** Project a point from the sphere to the plane.
     *  @param sphere a double[3] unit vector
     *  @param plane  a double[2] preallocated vector.
     */
    public final void transform(double[] sphere, double[] plane) {
	
	if (Double.isNaN(sphere[0]) || sphere[0] <= 0) {
	    plane[0] = Double.NaN;
	    plane[1] = Double.NaN;
	} else {
	    plane[0] = sphere[1];
	    plane[1] = sphere[2];
	}
    }
    
    public boolean validPosition(double[] plane) {
	return super.validPosition(plane) &&
	  (plane[0]*plane[0] + plane[1]*plane[1] <= 1);
    }
    
    public class Sin2Deproj extends skyview.geometry.Deprojecter {
	
	/** Get the name of the component */
	public String getName() {
	    return "Sin2Deproj";
	}
	
	/** Get a description of the component */
	public String getDescription() {
	    return "Invert the sine projection";
	}
	
	/** Get the inverse transformation */
	public Projecter inverse() {
	    return Sin2.this;
	}

        /** Is this an inverse of some other transformation? */
        public boolean isInverse(Transformer t) {
            return t.getName().equals("Sin2");
        }

    
        /** Deproject a point from the plane to the sphere.
         *  @param plane a double[2] vector in the tangent plane.
         *  @param sphere a preallocated double[3] vector.
         */
        public final void transform(double[] plane, double[] sphere) {
	
	    if (!validPosition(plane)) {
	        sphere[0] = Double.NaN;
	        sphere[1] = Double.NaN;
	        sphere[2] = Double.NaN;
	    
	    } else {
	        sphere[1] = plane[0];
	        sphere[2] = plane[1];
	        sphere[0] = Math.sqrt(1 - plane[0]*plane[0] - plane[1]*plane[1]);
	    }
        }
    }
}
