package uk.ac.starlink.frog.fit;

import java.lang.Math;
import Jama.Matrix;

import uk.ac.starlink.frog.util.FrogDebug;


/**
 * Class to do a Least Squares Fit of a polynomial and return a ChiSq and 
 * fit co-efficients. The code a series of 2-D points and finds the best 
 * fit polynomial equation.
 *
 * The equation is of the form: y = co + c1*x + ... + ck*x^k
 * The system of equations becomes: Y= A*C
 *
 * The least square normal equation looks like: A'AC = AY; 
 * where, C = (c0, c1, ..., ck)'
 *
 * @author Alasdair Allan
 * @version $Id$
 * @since 21-MAR-2003
 */
 
public class LeastSquaresFitPoly
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
     * Array to hold the theoretical y values predicted by the polynomial fit
     */ 
     double[] yPredicted = null; 
         

    /**
     * Array to hold the Y Data Errors
     */ 
     double[] yError = null; 
     
    /**
     * order of polyynomial
     */
     int order;
     
    /**
     * number of data points
     */
     int numberOfPoints;

   /**
     * Matrix for the equation coefficients
     */
     Matrix mC = null;	

   /**
     * Correlation coefficient factor 
     */     
     double dR = 0.0; 		
     
      
    /**
     *  Default constructor
     */
    public LeastSquaresFitPoly( double [] x, double [] y, double [] e, int n )
    {
         debugManager.print( "            LeastSquaresFit(" + n + ")" );
         
         // Assign to class globals
         order = n;
         xData = x;
         yData = y;
         yError = e;
         numberOfPoints = xData.length;
         
         // do the fit
         doLeastSquares( );
        
    }
    
    /**
     * Carry out the least squares fitting of a polynomial
     * to the data provided by the user
     *
     */
     protected void doLeastSquares( ) 
     {
         Matrix mA, mAl, mY;

         double[][] dCols= new double[numberOfPoints][order+1];

         for (int i=0 ; i < numberOfPoints; i++ ){
            for (int j=0; j < order+1; j++ ){
               dCols[i][j]= Math.pow(xData[i], j);
            }
         }
         mA= new Matrix(dCols,numberOfPoints,order+1);
         mY= new Matrix(yData,numberOfPoints);
 
         mAl= (mA.transpose()).times(mA);	//mAl= mA'mA
         mC= mAl.inverse().times(mA.transpose()).times(mY);
           
         // move to class globals  
             
         return;
     }

    /**
     * Calculate a y value of the fitted polynomial for a provided x value
     *
     * @param x x data value
     */
     public double getValue(double dX)
     {
        double dY = 0.0;

        for (int j=0; j < order+1; j++){
           dY += mC.get(j,0)*Math.pow(dX, j); 
        }
 
        return dY;
     }

    /**
     * Calculate the correlation coefficient for the fit
     *
     * @return dR Correlation coefficient factor 
     */     
      public double getCorrelation() 
      {
            
         //dR is the correlation coefficient
         double dMean, dVarUnexplained, dVarTotal;

         //to calculate yPredicted:
         for (int i=0; i < numberOfPoints; i++) {
            yPredicted[i]= mC.get(0,0);
            for (int j=1; j < order+1; j++) {
               yPredicted[i] += mC.get(j,0)*Math.pow(xData[i],j); 
            }
         }

         //to calculate square sums:
         dMean= yData[0]/numberOfPoints;
         for (int i=1; i<numberOfPoints; i++) {
            dMean += yData[i]/numberOfPoints;
         }

         dVarUnexplained= Math.pow( yData[0]-yPredicted[0], 2);
         dVarTotal= Math.pow( yData[0]-dMean, 2);
         for (int i=1; i<numberOfPoints; i++) {
            dVarUnexplained += Math.pow( yData[i]-yPredicted[i], 2);
            dVarTotal += Math.pow( yData[i]-dMean, 2);
         }
      
         //calculate correlation coefficient:
         dR= Math.sqrt( 1-dVarUnexplained/dVarTotal );

         return dR;
      }           

    /**
     * Return the fitted polynomial equation as a string
     *
     * @return equation Representing the polynomial fitted to the data
     */  
      public String getEquation() 
      {
        
         String eq = new String();

         eq = "Y = " +mC.get(0,0)+ " + " +mC.get(1,0) + " X";
         for (int j=2; j < order+1; j++){
            eq += " + " +mC.get(j,0)+ " X^" +j;       
         }

         return eq;
      } 

    
}
