package uk.ac.starlink.ttools.plot2.geom;

import skyview.data.AngScale;
import skyview.data.CoordinateFormatter;
import skyview.geometry.Util;
import skyview.geometry.Transformer;
import skyview.geometry.Converter;
import skyview.geometry.Scaler;
import skyview.geometry.TransformationException;
import skyview.geometry.Projecter;
import skyview.geometry.Rotater;

import java.awt.Rectangle;
import java.util.ArrayList;

class GridLine {
    /** a nx2 array of giving the x and y coordinates for a grid line. */
    double[][] line;
    /** The label for this line. */
    String label;
}

/** This class is used to lines and text to draw coordinate grid
  * grid overlays.
  * in a Graphics context.
  *
  * <p>This is basically the <code>skyview.data.Gridder</code> class
  * from Tom McGlynn's Skyview package.
  * Some very minor modifications have been made to adapt it to
  * the TTOOLS plotting requirements.
  *
  * @author   Tom McGlynn (NASA GSFC)
  * @author   Mark Taylor
  * @see      <a href="https://skyview.gsfc.nasa.gov/"
  *                   >https://skyview.gsfc.nasa.gov/</a>
  */
public class GridLiner  {
    
    private boolean      sexagesimal;
    private int          width;
    private int          height;
    private Projecter    projecter;
    private int          xoff;
    private int          yoff;
    private double       lonCrowd;
    private double       latCrowd;
    private static final int    GRID_SAMPLE   = 10;
    private static final int    MAX_SAMPLE    = 400;
    private static final int    MIN_POINTS    = GRID_SAMPLE*GRID_SAMPLE;
    
    private static final int    LINE_ELEMENT_MIN = 8;
    private static final int    LINE_ELEMENT_MAX = 1024;;
    private static final double BOUNDARY_LIMIT   = 0.00001;
    private static final double LIMIT_EXTENSION  = 0.4;
    private  ArrayList<GridLine> lines = new ArrayList<GridLine>();
    
    /** The label for the next line */
    private String label;
    
    /** Tiling ranges. */
    private int tileXMin = 0;
    private int tileXMax = 0;
    private int tileYMin = 0;
    private int tileYMax = 0;
    
    /** Tiling periods */
    private double tileDx;
    private double tileDy;
    
    /** The scaler used in the image.  This will
     *  be used when we see if we need to worry about tiling.
     */
    private Scaler imageScaler;
    
    private static final double MAX_CURVE = Math.cos(Math.toRadians(1));
    
    // The latitude limits (in decimal degrees)
    private double[] latLimits;
    
    // The longitude limits (in decimal degrees)
    private double[] lonLimits;
    
    /** The transformation between the desired coordinate
     *  system and image pixels.
     */
    Converter        forward;
    
    /** Create a new GridLiner object. 
     * @param lonCrowd  factor controlling how closely grid lines are spaced
     *                  in longitude; 1 is normal
     * @param latCrowd  factor controlling how closely grid lines are spaced
     *                  in latitude; 1 is normal
     */
    public GridLiner(Rectangle bounds,
                     Rotater rotater, Projecter projecter, Scaler scaler,
                     boolean sexagesimal, double lonCrowd, double latCrowd) {
        this.width = bounds.width;
        this.height = bounds.height;
        this.xoff = bounds.x;
        this.yoff = bounds.y;
        this.projecter = projecter;
        this.sexagesimal = sexagesimal;
        this.lonCrowd = lonCrowd;
        this.latCrowd = latCrowd;
        
        stdForward(rotater, projecter, scaler);
        
        try {
            imageScaler = scaler.inverse();
        } catch (TransformationException e) {
            System.err.println("Error inverting scaler in GridLiner");
            imageScaler = null;
        }
        
        tileDx      = projecter.getXTiling();
        tileDy      = projecter.getYTiling();
    }
            
    private void stdForward(Rotater rotater, Projecter projecter,
                            Scaler scaler) {
        try {
            forward = new Converter();
            forward.add(rotater);
            forward.add(projecter);
            forward.add(scaler);
        } catch (TransformationException e) {
            System.err.println("Error in building GridLiner:"+e);
            throw new Error("Irrecoverable error"+e);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle( xoff, yoff, width, height );
    }
            
    public void grid() throws TransformationException {
        
        getLimits();
        CoordinateFormatter fm = new CoordinateFormatter();
        fm.setSexagesimal(sexagesimal);
        if (sexagesimal) {
            fm.setZeroFill(true);
        }
        
        // Use custom AngScale implementation that allows for sparser
        // grid lines than the skyview one.
        AngScale ang = new AngScale2();
        ang.setSexagesimal(sexagesimal);
        double[] latValues = scaling(ang, false, latLimits[0], latLimits[1], latCrowd);
        double[] lonValues;
        if (sexagesimal) {
            ang.setTime(true);
            lonValues = scaling(ang, true, lonLimits[0]/15, lonLimits[1]/15, lonCrowd);
            lonValues[0] *= 15;
            lonValues[1] *= 15;
        } else {
            lonValues = scaling(ang, false, lonLimits[0], lonLimits[1], lonCrowd);
        }
        double lonLog = Math.log10(lonValues[1]);
        if (sexagesimal) {
            lonLog = Math.log10(lonValues[1]/15);
        }
        double latLog = Math.log10(latValues[1]);
        int lonPrec = (int)(3-lonLog);
        int latPrec = (int)(3-latLog);
        
        if (sexagesimal) {
            // Sexageimal fractions may require min/sec to
            // express evenly (e.g., 20' = 1/3 degree).
            lonPrec += 1;
            if (lonPrec < 6 && lonPrec%2 == 1) {
                lonPrec += 1;
            }
            if (latPrec < 6 && latPrec%2 == 1) {
                latPrec += 1;
            }
        }
        
        extendLimits(LIMIT_EXTENSION);
        
        double lonMax = lonLimits[1];
        if (lonLimits[0] > lonLimits[1]) {
            lonMax += 360;
        }

        // Special handling for grid spacing values so sparse that grid
        // lines should not be drawn.
        boolean drawLon = lonValues[1] < 360;
        boolean drawLat = latValues[1] < 180;
        
        Converter save = forward;
        
        // This seems to work more reliably (e.g., all sky
        // flat equator Hpx projection) but probably
        // means that we haven't thought out the tiling
        // projections properly...
        int mx =  Math.abs(tileXMin);
        if (Math.abs(tileXMax) > mx) {
            mx = Math.abs(tileXMax);
        }
        
        for (int itileX= -mx; itileX <= mx; itileX += 1) {
//        for (int itileX= tileXMin; itileX <= tileXMax; itileX += 1) {
          for (int itileY=tileYMin; itileY<=tileYMax;itileY += 1) {
              Scaler tiler = new Scaler(itileX*tileDx, itileY*tileDy, 1, 0, 0, 1);
              forward = new Converter();
              forward.add(save);
              if (imageScaler != null) {
                  forward.add(imageScaler); // Remember we inverted this when
                                            // we created it.
                  forward.add(tiler);       // Put in the shift to the desired tile.
                  forward.add(imageScaler.inverse());
              }
              
        if (drawLon) {
            for (double qlon=lonValues[0]; qlon <= lonMax; qlon += lonValues[1]) {
            
                double lon = qlon;
            
                if (lon > 360) {
                    lon -= 360;
                }
            
                if (Math.abs(lon-360) < 1.e-8) {
                    lon = 0;
                }
            
                if (sexagesimal) {
                    fm.setSeparators(new String[]{"h", "m", "s"});
                    setLabel(fm.format(lon/15, lonPrec));
                } else {
                    fm.setSeparators(new String[]{"\u00B0", "'", "\""});
                    setLabel(fm.format(lon, lonPrec));
                }
                // The lines of longitude that we tend to choose for
                // drawing are likely to be ones that touch singularities
                // in the projection.  This can cause instabilities
                // in drawing the grid points.  So we draw the line
                // infinitesimally above and below the requested
                // longitude -- this will also accommodate cases
                // where the longitude shows up on both sides of the map
                // (e.g., lon=180 on a 0,0-centered Cartesian all-sky image).
//              drawLine(lon, lon, latLimits[0], latLimits[1]);
                drawLine(lon+1.e-10, lon+1.e-10, latLimits[0],latLimits[1]);
                drawLine(lon-1.e-10, lon-1.e-10, latLimits[0],latLimits[1]);
            }
        }
        if (drawLat) {
            for (double lat=latValues[0]; lat <= latLimits[1]; lat += latValues[1]) {
                fm.setSeparators(new String[]{"\u00B0", "'", "\""});
                setLabel(fm.format(lat, latPrec));
                drawLine(lonLimits[0], lonLimits[1], lat, lat);
            }
        }
    }
    }
  }

    private double[] scaling(AngScale ang, boolean isTime, double min, double max, double crowd) {
        // The implementation of this method is informed by looking at the
        // source code of the AngScale class.
        double delt = max-min;
        if (min > max) {
            if (!isTime) {
                delt += 360;
            } else {
                delt += 24;
            }
        }

        max = min + delt / crowd;
        return ang.scaling(min, max);
    }
    
    private void setLabel(String label) {
        if (label != null) {
            if (label != null) {
                label = label.trim();

                // Hack to cover the cases when the Locale has used a
                // comma instead of a period for the decimal separator.
                label = label.replace( ',', '.' );

                if (label.indexOf(".") > 0) {
                    while (label.endsWith("0"))  {
                        label = label.substring(0,label.length()-1);
                    }
                    if (label.endsWith(".")) {
                        label = label.substring(0, label.length()-1);
                    }
                }
                // Strip leading zeroes
                label = label.replaceFirst( "^(?<=[-+]?)0+(?=[0-9].*)", "" );
            }
        }
        this.label = label;
    }
    
    private void extendLimits(double amount) {
        
        double db = amount/2 * (latLimits[1]-latLimits[0]);
        
        latLimits[0] -= db;
        latLimits[1] += db;
        
        if (latLimits[0] < -90) {
            latLimits[0] = -90;
        }
        if (latLimits[1] > 90) {
            latLimits[1] = 90;
        }
        
        // Have to be careful to handle what happens when we are wrapping around.
        if (lonLimits[0] > lonLimits[1]) {
            
            db = 0.5*amount*(360 +lonLimits[1] - lonLimits[0]);
            lonLimits[0] -= db;
            lonLimits[1] += db;
            
            // If we've we cross a second time that must mean we're doing all possible values.
            if (lonLimits[1] > lonLimits[0]) {
                lonLimits[0] = 0;
                lonLimits[1] = 360;
            }
            
        } else {
            
            db = 0.5*amount*(lonLimits[1] - lonLimits[0]);
            
            lonLimits[0] -= db;
            lonLimits[1] += db;
            
            // May be wrapping around now... 
            // 
            // First check if we're doing the entire circle
            if (lonLimits[1] - lonLimits[0] > 360) {
                lonLimits[0] = 0;
                lonLimits[1] = 360;
            } else {
                
                // Otherwise just set it up so we're wrapping.
                // Only one of the following two conditions can apply
                // (the full circle check would be valid if both did).
                if (lonLimits[0] < 0) {
                    lonLimits[0] += 360;
                }
                if (lonLimits[1] > 360) {
                    lonLimits[1] -= 360;
                }
            }
        }
    }
    
    private void drawLine(double l0, double l1, double b0, double b1) {
        
        int npt = LINE_ELEMENT_MIN;
        double[][] line = null;
        
        while (npt <= LINE_ELEMENT_MAX) {
            
            line = getLine(line, l0, l1, b0, b1, npt);
            if (npt >= LINE_ELEMENT_MAX || !curvature(line)) {
                break;
            }
            npt *= 2;
        }
        parseLine(line, l0, l1, b0, b1, npt);
    }
    
    private double[][] getLine(double[][] oldLine, 
                              double l0, double l1, double b0, double b1,
                              int npt) {
        
        double dl = l1-l0;
        if (l0 > l1) {
            dl += 360;
        }
        double db = b1-b0;
        
        dl = dl/npt;
        db = db/npt;
        
        double[][] newLine = new double[npt+1][2];
        int istart = 0;
        int idelt  = 1;
        if (oldLine != null) {
            // Copy in the old points rather than recomputing them.
            for (int i=0; i<oldLine.length; i += 1) {
                newLine[2*i] = oldLine[i];
            }
            istart = 1;
            idelt  = 2;
        }
        
        for (int i=istart; i<npt+1; i += 1) {
            double[] coords = Util.unit(Math.toRadians(l0+dl*i), Math.toRadians(b0+db*i));
            forward.transform(coords, newLine[i]);
        }
        return newLine;
    }
    
    /** Check to see if this meets the curvature criterion */
    private boolean curvature(double[][] points) {
        
        int nx = this.width;
        int ny = this.height;
        
        int consec    = 0;
        int maxConsec = 0;
        int realCount = 0;
        
        for (int i=0; i<points.length; i += 1) {
            if (Double.isNaN(points[i][1])) {
                consec = 0;
            } else {
                consec += 1;
                if (consec > maxConsec) {
                    maxConsec = consec;
                }
                realCount += 1;
            }
        }
        
        // Don't have enough consecutive points
        // to check the curvature.
        if (realCount > 0 && consec < 3) {
            return true;
        }
        
        // Nothing here... We'll just be ignoring this value.
        if (realCount == 0) {
            return false;
        }
        
        for (int i=2; i<points.length; i += 1) {
            
            double[] p = points[i-2];
            double[] q = points[i-1];
            double[] r = points[i];
            
            double dx0 = q[0]-p[0];
            double dx1 = r[0]-q[0];
            double dy0 = q[1]-p[1];
            double dy1 = r[1]-q[1];
            
            
            // If either point is off the edge, that's OK.
            if (Double.isNaN(dx0+dx1+dy0+dy1)) {
                continue;
            }
            
            // If either segment is less than a pixel then we're OK.
            if ( (Math.abs(dx0)+Math.abs(dy0)) < 1 ||
                 (Math.abs(dx1)+Math.abs(dy1)) < 1) {
                continue;
            }
            
            // Off the edge we don't care...
            if ( (p[0] < 0 || p[0] > nx) && (p[1] < 0 || p[1] > ny) && 
                 (q[0] < 0 || q[0] > nx) && (q[1] < 0 || q[1] > ny) &&
                 (r[0] < 0 || r[0] > nx) && (r[1] < 0 || r[1] > ny)) {
                continue;
            }
            
            // Now compute the angle...
            double costh = (dx0*dx1 + dy0*dy1)/ 
                            (Math.sqrt(dx0*dx0 + dy0*dy0)*Math.sqrt(dx1*dx1+dy1*dy1));
            if (costh < MAX_CURVE) {
                return true;
            }
        }
        return false;
    }
    
    // Analyze the line and break up into valid segments. 
    private void parseLine(double[][] line, double l0, double l1, 
                           double b0, double b1, int npt) {
        
        int min = 0;
        int nx = this.width;
        int ny = this.height;
        
        double dl = l1-l0;
        if (l0 > l1) {
            dl = 360+dl;
        }
        dl /= npt;
        double db = (b1-b0)/npt;
        int     segStart  = -1;
        boolean beginning = true;
        
        while (min < npt+1) {
            int i;
            
            beginning = true;
            double lastDistSq = -1;
            
            for (i=min; i<npt+1; i += 1) {
                double[] p = line[i];
                
                // Inside point
                if (p[0] > 0 && p[0] < nx  && p[1] > 0 && p[1] < ny) {
                    
                    if (beginning) {
                        
                        if (i != min) {
                            double lx  = (l0 + (i-1)*dl)%360;
                            double lxx = (lx + dl)%360;
                            double bx =   b0 + (i-1)*db;
                    
                            if (Double.isNaN(line[i-1][0]+line[i-1][1])) {
                                fixNaN(lxx, bx+db, lx, bx, line[i], line[i-1]);
                            } else {
                                fixOut(lxx, bx+db, lx, bx, line[i], line[i-1]);
                            }
                            segStart = i-1;
                        } else {
                            segStart = i;
                        }
                        beginning = false;
                    } else {
                        double[] pm     = line[i-1];
                        double   distSq = (pm[0]-p[0])*(pm[0]-p[0])+ (pm[1]-p[1])*(pm[1]-p[1]);
                        // Check for a sudden discontinuity in the length of the lines.
                        // It may mean that we are going over a coordinate discontinuity in the
                        // projection.
                        if (lastDistSq > 0) {
                            if (distSq / lastDistSq > 100) {
                                addSegment(line, segStart, i-1, null);
                                beginning = true;
                                lastDistSq = -1;
                                // Don't skip this point.
                                i -= 1;
                                break;
                            }
                        }
                            
                        lastDistSq = distSq;
                    }
                                
                                
                        
                    
                // Outside point but previous point was inside
                } else if (!beginning) {
                    
                    
                    double lx  = (l0 + (i-1)*dl)%360;
                    double lxx = (lx + dl)%360;
                    double bx  = b0 + (i-1)*db;
                    
                    // Can't re-use line since there might
                    // be another segment starting at the next point.
                    double[] newpt = line[i].clone();
                    
                    // We already started a line, but apparently we've moved
                    // out of the region.
                    if (Double.isNaN(line[i][0]+line[i][1])) {
                        fixNaN(lx, bx, lxx, bx+db, line[i-1], newpt);
                    } else {
                        // Check if the last point is jumpting away somewhere.
                        if (lastDistSq > 0) {
                            double dx = line[i][0]-line[i-1][0];
                            double dy = line[i][1]-line[i-1][1];
                            if ((dx*dx+dy*dy)/lastDistSq > 100) {
                                newpt = null;
                            } else {
                                fixOut(lx, bx, lxx, bx+db, line[i-1], newpt);
                            }
                        } else {
                            fixOut(lx, bx, lxx, bx+db, line[i-1], newpt);
                        }
                            
                    }
                    addSegment(line, segStart, i-1, newpt);
                    beginning = true;
                    min = i+1;
                    break;
                } else {
                }
                // Just ignore outside points is the previous point was also outside.
            }
            min = i+1;
            
        }
        if (!beginning) {
            addSegment(line, segStart, npt, null);
        }
    }
    
    private void addSegment(double[][] line, int segStart, int segEnd, double[] lastPt) {
        
        
        // Check if the initial point might be beyond a discontinuity
        if (segEnd - segStart > 2) {
            double[] p = line[segStart];
            double[] q = line[segStart+1];
            double[] r = line[segStart+2];
            double sq1 = (p[0]-q[0])*(p[0]-q[0])+ (p[1]-q[1])*(p[1]-q[1]);
            double sq2 = (r[0]-q[0])*(r[0]-q[0])+ (r[1]-q[1])*(r[1]-q[1]);
            
            if (sq2 > 0 && sq1/sq2 > 100) {
                segStart += 1;
            }
        }
        
        int npt = segEnd-segStart + 1;
        if (lastPt != null) {
            npt += 1;
        }
        if (npt < 3) {
            return;
        }
        if (segStart == segEnd && lastPt == null) {
            return;
        }
        double[][] seg;
        if (lastPt == null) {
            seg = new double[segEnd-segStart+1][];
        } else {
            seg = new double[segEnd-segStart+2][];
        }
    
        for (int i=segStart; i<=segEnd; i += 1) {
            seg[i-segStart] = new double[] { line[i][0] + xoff,
                                             line[i][1] + yoff };
        }
        
        if (lastPt != null) {
            seg[segEnd-segStart+1] = new double[] { lastPt[0] + xoff,
                                                    lastPt[1] + yoff };
        }
        GridLine gl = new GridLine();
        gl.line  = seg;
        
        gl.label = this.label;
        lines.add(gl);
    }
    
    /** Given that the point l1,b1 returns a NaN, but l0,b0 does not,
     *  find a point at the edge of the valid region.
     */
    private void fixNaN(double l0, double b0, double l1, double b1, 
                        double[] goodPt, double[] bound) {
        
        int nx = this.width;
        int ny = this.height;
        
        double db = (b1-b0);
        double dl = (l1-l0);
        if (Math.abs(dl) > 180) {
            if (dl > 0) {
                dl = 360-dl;
            } else {
                dl = -360-dl;
            }
        }
        double lastGoodL = l0;
        double lastGoodB = b0;
        double[] lastGoodPix= goodPt.clone();
        double[] pix = new double[2];
        
        while (Math.abs(dl) > BOUNDARY_LIMIT || Math.abs(db) > BOUNDARY_LIMIT) {
            
            dl /= 2;
            db /= 2;
            
            double testL = lastGoodL + dl;
            
            if (testL > 360) {
                testL -= 360;
            } 
            
            if (testL < 0) {
                testL += 360;
            }
            
            double testB = lastGoodB + db;
            
            double[] unit = Util.unit(Math.toRadians(testL), Math.toRadians(testB));
            forward.transform(unit, pix);
            if (!Double.isNaN(pix[0]+pix[1])) {
                if (pix[0] < 0 || pix[0] > nx || pix[1] < 0 || pix[1] > ny) {
                    bound[0] = pix[0];
                    bound[1] = pix[1];
                    fixOut(l0, b0, testL, testB, goodPt, bound);
                    return;
                }
                lastGoodL = testL;
                lastGoodB = testB;
                lastGoodPix[0] = pix[0];
                lastGoodPix[1] = pix[1];
            }
        }
        bound[0] = lastGoodPix[0];
        bound[1] = lastGoodPix[1];
        return;
    }
    
    private void fixOut(double l0, double b0, double l1, double b1,
                        double[] goodPt, double[] bound) {
        
        double nx = this.width;
        double ny = this.height;
            
        // There are four ways the point can be outside
        // Two of them may be true.  We need to find which
        // applies first...
        // Find where the line crosses each border (if it does)
        // 
        // 
        // We don't really need the frac versus minFrac tests for
        // the X coordinate (they are always true) but we leave them in, in case we
        // want to re-order things later.
        double minFrac = 1;
        if (bound[0] < 0) {
            double frac = goodPt[0]/(goodPt[0]-bound[0]);
            if (frac < minFrac) {
                minFrac = frac;
            }
        } else if (bound[0] > nx) {
            double frac = (nx-goodPt[0])/(bound[0]-goodPt[0]);
            if (frac < minFrac) {
                minFrac = frac;
            }
        } else if (bound[1] < 0) {
            double frac = goodPt[1]/(goodPt[1]-bound[1]);
            if (frac < minFrac) {
                minFrac = frac;
            }
        } else if (bound[1] > ny) {
            double frac = (ny - goodPt[1])/(bound[1]-goodPt[1]);
            if (frac < minFrac) {
                minFrac = frac;
            }
        }
        bound[0] = goodPt[0] + minFrac*(bound[0]-goodPt[0]);
        bound[1] = goodPt[1] + minFrac*(bound[1]-goodPt[1]);
    }
        
    /** Get the coordinate limits to be used in computing the grid for the
     *  given image.
     */
    private void getLimits() throws TransformationException {
        
        int npt = GRID_SAMPLE;
        while (npt <= MAX_SAMPLE) {
            if (getLimitGrid(npt, npt == MAX_SAMPLE)) {
                return;
            }
            npt *= 2;
        }
    }
    
    
    private boolean getLimitGrid(int npts, boolean tryRegardless) throws TransformationException {
        
        Transformer reverse = forward.inverse();
        
        int nx = this.width;
        int ny = this.height;
        
        double dx = nx/(npts-1);
        double dy = ny/(npts-1);
        int g2 = npts*npts;
          
        
        double[] x = new double[g2];
        double[] y = new double[g2];
        double[] z = new double[g2];
        
        int count = 0;
        
        int okCount = 0;
        
        double[] unscaled = new double[2];
        
        for (int ix=0; ix<npts; ix += 1) { 
            for (int iy=0; iy<npts; iy += 1) {
                
                double[] tt = new double[]{ix*dx, iy*dy};
                double[] vec = reverse.transform(tt);
                if (!Double.isNaN(vec[2])) {
                    okCount += 1;
                    
                    if (tileDx > 0 || tileDy > 0) {
                        // This gets us the unscaled (i.e., radians) 
                        // unrotated coordinates which we can compare with
                        // the tile offsets
                        unscaled = imageScaler.transform(tt);
                        if (tileDx > 0) {
                            int test = (int) ((unscaled[0]+Math.signum(unscaled[0])*tileDx/2)/tileDx);
                            if (test > tileXMax) { 
                                tileXMax = test;
                            } else if (test < tileXMin) {
                                tileXMin = test;
                            }
                        }
                        if (tileDy > 0) {
                            int test = (int) ((unscaled[1]+Math.signum(unscaled[1])*tileDy/2)/tileDy);
                            if (test > tileYMax) {
                                tileYMax = test;
                            }
                            if (test < tileYMin) {
                                tileYMin = test;
                            }
                        }
                    }
                }
                x[count] = vec[0];
                y[count] = vec[1];
                z[count] = vec[2];
                count += 1;
            }
        }
        
        if (okCount < MIN_POINTS && ! tryRegardless) {
            return false;
        }
        
        // Find the min/max of z 
        double minz =  2;
        double maxz = -2;
        for (int i=0; i<g2; i += 1) {
            if (Double.isNaN(z[i])) {
                continue;
            }
            if (z[i] < minz) {
                minz = z[i];
            }
            if (z[i] > maxz) {
                maxz = z[i];
            }
        }
        
        // Convert to latitudes.
        double minLat = Math.toDegrees(Math.asin(minz));
        double maxLat = Math.toDegrees(Math.asin(maxz));
        
        double minLon    =  720;
        double maxLon    = -720;
        double maxLon180 = -720;
        double minLon180 =  720;
        // Now find the min and max for the longitude.
        boolean pole = false;
        for (int i=0; i<g2; i += 1) {
            if (Double.isNaN(z[i])) {
                continue;
            }
            
            double fac = (1-z[i]*z[i]);
            if (fac <= 0) {
                pole = true;
                break;
            }
            
            double lon = Math.toDegrees(Math.atan2(y[i], x[i]));
            if (lon < 0) {
                lon += 360;
            }
            if (lon < minLon) {
                minLon = lon;
            }
            if (lon > maxLon) {
                maxLon = lon;
            }
            if (lon < 180 && lon > maxLon180) {
                maxLon180 = lon;
            }
            if (lon > 180 && lon < minLon180) {
                minLon180 = lon;
            }
        }
        if (pole) {
            
            minLon = 0;
            maxLon = 359.999;
            
        } else {
        
            if (maxLon-minLon > 240) {
                // See if we are just wrapping around.
                boolean noNegatives = true;                
                // Check if there are any points in the 2nd or
                // 3rd quadrants.  If not then assume we
                // are just wrapping around.
                for (int i=0; i<g2; i += 1) {
                    if (x[i] < 0) {
                        noNegatives = false;
                        break;
                    }
                }
                
                if (noNegatives) {
                    minLon = minLon180;
                    maxLon = maxLon180;
                }
            }
        }
        
        if (minLon < maxLon && maxLon-minLon>200) {
            minLon = 0;
            maxLon = 359.999;
        }
        if (maxLat-minLat > 100) {
            minLat = -90.;
            maxLat = 90.;
        }
        
        setLimits(new double[] {minLon, maxLon}, new double[]{minLat, maxLat});
        return true;
    }
    
    /** Set the limits to be used in calculating the grid.
     *  Note that these limits will be used in determining
     *  the grid values, but the limit values themselves will
     *  only be used if they happen to correspond to
     *  an appropirate delta.
     */
    private void setLimits(double[] lon, double[] lat) {
        if (lat == null || lon == null || lat.length != 2 || lon.length != 2) {
            System.err.println("Warning: Invalid limit arrays for grid");
        }
        this.latLimits = lat;
        this.lonLimits = lon;
    }
    
    
    /** Set whether you want sexagesimal labels */
    public void setSexigesimal(boolean flag) {
        this.sexagesimal = flag;
    }
    
    /** Display the lines */
    public void dumpLines() {
        
        java.text.DecimalFormat fm = new java.text.DecimalFormat("####.#");
        
        for (GridLine g: lines) {
            
            System.out.print(g.label+":\n  ");
            for (int i=0; i<g.line.length; i += 1) {
                System.out.print(fm.format(g.line[i][0])+" ");
            }
            System.out.print("\n  ");
            for (int i=0; i<g.line.length; i += 1) {
                System.out.print(fm.format(g.line[i][1])+" ");
            }
            System.out.println("");
        }
    }
    
    /** Get the labels for the lines */
    public String[] getLabels() {
        
        String[] labels = new String[lines.size()];
        
        for (int i=0; i <labels.length; i += 1) {
            labels[i] = lines.get(i).label;
        }
        return labels;
    }
    /** Get the line points  */
    public double[][][] getLines() {
        
        double[][][] points = new double[lines.size()][][];
        for (int i=0; i<points.length; i += 1) {
            points[i] = lines.get(i).line;
        }
        return points;
    }
}
