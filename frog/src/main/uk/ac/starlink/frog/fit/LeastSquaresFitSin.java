package uk.ac.starlink.frog.fit;

import java.lang.Math;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.data.SinFit;


/**
 * Class to do a Least Squares Fit of a sin function and return a ChiSq and 
 * fit co-efficients. Original alogorithim written in Fortran as part of 
 * KDHLIB and used by Keith to work out gamma, K-velocity and zero crossing 
 * phase for radial velocity fits. Modified by AA to take slightly different
 * inputs and removed all VAX specific code (1998), and  then ported to Java
 * (2003). Method fits Y = A + B*sin(2pi/period*X) + C*cos(2pi/period*X)
 * using linear least squares. 
 *
 * @author Alasdair Allan
 * @version $Id$
 * @since 21-MAR-2003
 */
 
public class LeastSquaresFitSin
{
    /**
     *  Application wide debug manager
     */
    protected static FrogDebug debugManager = FrogDebug.getReference();

    /**
     * Array to hold the X Data
     */ 
     double[] xData = null; 

    /**
     * Array to hold the Y Data
     */ 
     double[] yData = null;     

    /**
     * Array to hold the Y Data Errors
     */ 
     double[] yError = null; 

    /**
     * The period of the sin() + cos() function
     */ 
     double period; 
        
    /**
     * constant term
     */
     double a = 0.0;
     
    /**
     * sin() term
     */
     double b = 0.0;     
     
    /**
     * cos() term
     */
     double c = 0.0;

    /**
     * chisq of resulting fit
     */
     double chiSq;

    /**
     * number of data points
     */
     int numberOfPoints;

      
    /**
     *  Default constructor
     */
    public LeastSquaresFitSin( double [] x, double [] y, double [] e, double p )
    {
         debugManager.print( "            LeastSquaresFitSin(" + p + ")" );
         
         // Assign to class globals
         xData = x;
         yData = y;
         
         // evenly weight errors if we don't have any
         if ( e == null ) {
            yError = new double[xData.length];
            for( int j = 0; j < xData.length; j++ ) {
               yError[j] = 1.0;
            }
         } else {   
            yError = e;
         }
            
         numberOfPoints = xData.length;
         period = p;
         
         // do the fit
         doLeastSquares( );
        
    }
    
    /**
     * Carry out the least squares fitting of  sin() + cos()
     * to the data provided by the user
     */
     protected void doLeastSquares( ) 
     {
      
        // define stuff
   
        int np = 0; // number of points with non-zero error
        double[] phase = new double[ xData.length ]; // phase for point
        
        double sw = 0.0;  // Sum of 1/(y-error)**2 (all valid data points)
        double sy = 0.0;  // Sum of y/(y-error)**2 
        double sy2 = 0.0; // Sum of y**2/(y-error)**2 
        double ss = 0.0;  // Sum of sin(phase)/(y-error)**2 
        double ss2 = 0.0; // Sum of sin**2(phase)/(y-error)**2
        double sys = 0.0; // Sum of y*sin(phase)/(y-error)**2
        double syc = 0.0; // Sum of y*cos(phase)/(y-error)**2
        double sc = 0.0;  // Sum of cos(phase)/(y-error)**2
        double ssc = 0.0; // Sum of cos(phase)*sin(phase)/(y-error)**2
        double sc2 = 0.0; // Sum of cos**2(phase)*sin(phase)/(y-error)**2
        double xx;  // Phase in degrees
        double sn;  // sin() of phase (in degrees)
        double cn;  // cos() of phase (in degrees)
        double ww;  // 1/(y-error)**2
        double w1;  // sin(phase)/(y-error)**2
        double c1;  // Co-factor 1
        double c2;  // Co-factor 2
        double c3;  // Co-factor 3
        double c4;  // Co-factor 4
        double c5;  // Co-factor 5
        double c6;  // Co-factor 6
        double det; //Determinant of matrix
   
        // loop round data points
        for ( int j = 0; j < xData.length; j++ ) { 
    
            // increment counter
            np += 1;
            
            // calculate phase for data point on this trial period
            phase[j] = xData[j]/period;
            phase[j] = Math.abs( phase[j] - (int)phase[j] );
                 
            // define some commonly used terms
            xx = 360.0*phase[j];
            sn = Math.sin(xx*Math.PI/180.0);
            cn = Math.cos(xx*Math.PI/180.0);
            ww = 1.0/yError[j]/yError[j];
            w1 = ww*sn;
            
            // accumulate sums
            sw = sw + ww;
            sy  = sy  + ww*yData[j];
            sy2 = sy2 + ww*yData[j]*yData[j];
            ss  = ss  + w1;
            ss2 = ss2 + w1*sn;
            sys = sys + w1*yData[j];
            syc = syc + ww*cn*yData[j];
            sc  = sc  + ww*cn;
            ssc = ssc + w1*cn;
            sc2 = sc2 + ww*cn*cn;
        
        }
        
        // linear least squares to find A, B and C
        c1 = ss2*sc2 - ssc*ssc;
        c2 = sc*ssc - ss*sc2;
        c3 = ss*ssc - sc*ss2;
        c4 = sw*sc2 - sc*sc;
        c5 = sc*ss - sw*ssc;
        c6 = sw*ss2 - ss*ss;
        
        // calculate determinent
        det = sw*c1 + ss*c2 + sc*c3;
        
        // calculate A, B and C
        a = (c1*sy + c2*sys + c3*syc) / det;
        b = (c2*sy + c4*sys + c5*syc) / det;
        c = (c3*sy + c5*sys + c6*syc) / det;

        // Calculate chi-squared

        chiSq = sy2 + a*a*sw + b*b*ss2 + c*c*sc2 + 2.0*b*c*ssc + 
                2.0*a*b*ss + 2.0*a*c*sc - 2.0*a*sy - 2.0*b*sys - 2.0*c*syc;             
        // end
        return;
     }

    /**
     * Calculate a y value of the fitted polynomial for a provided x value
     *
     * @param x the x data value
     */
     public double getValue(double dX)
     {
        double dY = 0.0;

        dY = a + b*Math.sin((2.0*Math.PI/period)*dX) +
                 c*Math.cos((2.0*Math.PI/period)*dX);
 
        return dY;
     }

    /**
     * Return the correlation coefficient for the fit
     *
     * @return chisq The chisq correlation coefficient for the fit
     */     
      public double getCorrelation() 
      {
            
         return chiSq;
      }           

    /**
     * Return the fitted polynomial equation as a string
     *
     * @return equation Representing the polynomial fitted to the data
     */  
      public String getEquation() 
      {
        
         String eq = new String();

         eq = "Y = " + a + " + " + b + "sin(2pi/" + period + " X) + " +
                       c + "cos(2pi/" + period + " X)";

         return eq;
      }
      
     /**
      * return a SinFit object
      *
      * @return sinfit A SinFit object wrapping this fit
      */
      public SinFit getFit()
      {
          SinFit sinFit = new SinFit();
          sinFit.setA(a);
          sinFit.setB(b);
          sinFit.setC(c);
          sinFit.setPeriod(period);
          sinFit.setChiSq(chiSq);
          
          return sinFit;
      }     

    
}
