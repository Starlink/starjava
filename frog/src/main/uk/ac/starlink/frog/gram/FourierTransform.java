package uk.ac.starlink.frog.gram;

import java.util.ArrayList;

import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.data.GramImpl;
import uk.ac.starlink.frog.data.MEMGramImpl;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.TimeSeriesComp;
/**
 * Statis class to build a FourierTransform
 *
 * @author Alasdair Allan
 * @version $Id$
 * @since 16-FEB-2003
 */
 
public class FourierTransform
{
    /**
     *  Application wide debug manager
     */
    protected static FrogDebug debugManager = FrogDebug.getReference();

    /**
     *  Default constructor
     */
    private FourierTransform()
    {
        //  Do nothing.
    }

    /**
     * Static method for generating a fourier transform
     */
    
    public static GramImpl make( TimeSeries currentSeries, boolean window,
                double minFreq, double maxFreq, double freqInterval ) 
    {
 
         debugManager.print( "            FourierTransform.make()" );
         debugManager.print( "            minFreq  = " + minFreq );
         debugManager.print( "            maxFreq  = " + maxFreq );
         debugManager.print( "            Interval = " + freqInterval );
        
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
            errors = (double[]) errRef.clone();
         }   
          
         // do we want a window function?
         if ( window ) {
            for ( int i = 0; i < yData.length; i++ ) {
               yData[i] = 1.0;
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
             frequency[i] = minFreq + ((double)i)*freqInterval;
             
             // angular frequency
             double omega = 2.0*Math.PI*frequency[i];
             
             // loop over data points
             double fourierRealComponent = 0.0;
             double fourierImagComponent = 0.0;
             for ( int j = 1; j < xData.length; j++ ) {
             
                double expo = omega*xData[j];
                double c = Math.cos( expo );
                double s = Math.sin( expo );
                
                fourierRealComponent = fourierRealComponent + (yData[j]*c);
                fourierImagComponent = fourierImagComponent + (yData[j]*s);
             }   
        
             // calculate power
             power[i] = ( Math.pow(fourierRealComponent, 2.0) +
                          Math.pow(fourierImagComponent, 2.0) ) /
                          Math.pow( (double)xData.length, 2.0 );

         }
    
         String name = "Fourier Transform of " + currentSeries.getShortName();
         MEMGramImpl memImpl = new MEMGramImpl( name );
        
         memImpl.setData( power, frequency );
         return memImpl;
    
    }
    
}
