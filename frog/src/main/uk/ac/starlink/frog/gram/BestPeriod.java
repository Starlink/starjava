package uk.ac.starlink.frog.gram;

import java.util.ArrayList;

import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.data.Gram;

/**
 * Statis class to find the best period in a passed periodogram
 *
 * @author Alasdair Allan
 * @version $Id$
 * @since 17-JAN-2004
 */
 
public class BestPeriod
{
    /**
     *  Application wide debug manager
     */
    protected static FrogDebug debugManager = FrogDebug.getReference();

    /**
     *  Default constructor
     */
    private BestPeriod()
    {
        //  Do nothing.
    }

    /**
     * Static method for finding the best fit period
     */
    
    public static double find( Gram gram, double minFreq, double maxFreq ) 
    {
 
         //debugManager.print( "            BestPeriod.find()" );
         //debugManager.print( "            minFreq  = " + minFreq );
         //debugManager.print( "            maxFreq  = " + maxFreq );
        
         // grab data
         double xRef[] = gram.getXData();
         double yRef[] = gram.getYData();
         
         // copy the arrays, this sucks as it double the memory requirement
         // or the application at a stroke, but we currently have only
         // references to the data held in the currentSeries object.
         double xData[] = (double[]) xRef.clone();
         double yData[] = (double[]) yRef.clone();
         
         // verbose for debugging    
         for ( int i = 0; i < xData.length; i++ ) {
             debugManager.print( "              " + i + ": " + 
                                 xData[i] + "    " + yData[i]  );  
         }  
       
         // Search for highest (or lowest) peak in periodogram between ]
         // given frequency range.
       
         double bestFrequency = 0.0;
         double bestValue = 0.0;
         int gramType = gram.getType();
         debugManager.print( "              Gram Type = " + gramType );
         
         debugManager.print( "              Searching...");
         for ( int i = 0; i < xData.length ; i++ ) {
              
               if( i == 0 ) {
                  bestValue = yData[i];
                  bestFrequency = xData[i];
               }   
               
               if ( gramType == Gram.CHISQ ) {
                  
                  if ( yData[i] < bestValue ) {
                     bestValue = yData[i];
                     bestFrequency = xData[i];
                     debugManager.print( "              " + i + ": " +
                                         bestFrequency + " " + bestValue );
                  }
               } else {
                  
                  if ( yData[i] > bestValue ) {
                     bestValue = yData[i];
                     bestFrequency = xData[i];
                     debugManager.print( "              " + i + ": " +
                                         bestFrequency + " " + bestValue );
                  }                        
              }
         }
         
         double bestPeriod = 1/bestFrequency;
         debugManager.print( "              Best Period " + bestPeriod );
         return bestPeriod;    
    
    }
    
}
