package uk.ac.starlink.frog.gram;

import java.util.ArrayList;

import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.data.GramImpl;
import uk.ac.starlink.frog.data.MEMGramImpl;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.TimeSeriesComp;
/**
 * Statis class to build a Chi-Squared Periodogram
 *
 * @author Alasdair Allan
 * @version $Id$
 * @since 16-FEB-2003
 */
 
public class ChisqPeriodogram
{
    /**
     *  Application wide debug manager
     */
    protected static FrogDebug debugManager = FrogDebug.getReference();

    /**
     *  Default constructor
     */
    private ChisqPeriodogram()
    {
        //  Do nothing.
    }

    /**
     * Static method for generating a fourier transform
     */
    
    public static GramImpl make( TimeSeries currentSeries, boolean window,
                double minFreq, double maxFreq, double freqInterval ) 
    {
 
         debugManager.print( "            ChisqPeriodogram.make()" );
         debugManager.print( "            minFreq  = " + minFreq );
         debugManager.print( "            maxFreq  = " + maxFreq );
         debugManager.print( "            Interval = " + freqInterval );
          
         // Window function _only_ with Fourier Transforms
         window = false;
                 
         // grab data
         double xRef[] = currentSeries.getXData();
         double yRef[] = currentSeries.getYData();
         
         // grab the error if the exist
         double errRef[] = null;
         if( currentSeries.haveYDataErrors() ) {
            errRef = currentSeries.getYDataErrors();
         }   
         
         // copy the arrays, this sucks as it double the memory requirement
         // or the application at a stroke, but we currently have only
         // references to the data held in the currentSeries object.
         double xData[] = (double[]) xRef.clone();
         double yData[] = (double[]) yRef.clone();
         double errors[] = null;
         if( currentSeries.haveYDataErrors() ) {
         
            // grab the real errors
            errors = (double[]) errRef.clone();
         } else {
         
            // need to weight the sin() fit with bogus errors
            errors = new double[xData.length];
            for ( int k = 0; k < xData.length; k++ ) {
               errors[k] = 1.0;
            }     
         }
          
         // verbose for debugging    
         for ( int i = 0; i < xData.length; i++ ) {
             if ( errors != null ) {
                 debugManager.print( "              " + i + ": " + 
                    xData[i] + "    " + yData[i] + "    " + errors[i] );
           } else {
                 debugManager.print( "              " + i + ": " + 
                    xData[i] + "    " + yData[i]  );  
             }                         
         }  
       
         // Frequency loop
         // --------------
         //   
         // We could do this using doubles to increment the loop, as below
         //
         // double tolerance = freqInterval*1.0E-04; 
         // for(double f = minFreq; f<maxFreq+tolerance; f=f+freqInterval){}
         // 
         // Don't like this idea, so we'll do it like this...
         
         // Work out how many steps we need betweem freqMin and freqMax
         int numOfSteps = (int)((maxFreq - minFreq)/freqInterval) + 1;
         
         // Allocate arrays
         double[] frequency = new double[numOfSteps];
         double[] power = new double[numOfSteps];
          
         // Loop over freqency 
         for ( int i = 0; i < numOfSteps; i++ ) {
         
             // fill the frequency array
             frequency[i] = minFreq + ((double)i+1)*freqInterval;
             
             // period for this frequency
             double period = 1.0/frequency[i]; 
             
             // calculate power
             power[i] = sinfit( xData, yData, errors, period );

         }
    
         String name = "Chi Squared Periodogram of " +
                       currentSeries.getShortName();
                       
         MEMGramImpl memImpl = new MEMGramImpl( name );
        
         memImpl.setData( power, frequency );
         return memImpl;
    
    }
    
   /**
     * Original algorithim written in Fortran as part of KDHLIB and 
     * used by Keith to work out gamma, K-velocity and zero crossing 
     * phase for radial velocity fits. Modified by AA to take slightly
     * different inputs and removed all VAX specific code (1998), and 
     * then orted to Java (2003).
     *
     * Method fits Y = A + B*sin(2pi/period*X) + C*cos(2pi/period*X)
     * using linear least squares. 
     *
     * @param xData the date stamps
     * @param yData the fluxes
     * @param error the error in flux (if any)
     * @param period trial period
     * @return chisq the chisq of the resulting fit
     */
    public static double sinfit( double[] xData, double[] yData, 
                           double[] errors, double period )
    {    
   
        // define stuff
   
        int np = 0; // number of points with non-zero error
        double[] phase = new double[ xData.length ]; // phase for point
        
        double a = 0.0;   // constant term (fitted parameter)
        double b = 0.0;   // sin() term (fitted parameter)
        double c = 0.0;   // cos() term (fitted parameter)
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
            ww = 1.0/errors[j]/errors[j];
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

        double chiSq = sy2 + a*a*sw + b*b*ss2 + c*c*sc2 + 2.0*b*c*ssc + 
                2.0*a*b*ss + 2.0*a*c*sc - 2.0*a*sy - 2.0*b*sys - 2.0*c*syc;       
        // return the ChiSq of the fit
        return chiSq;

    }
    
}
