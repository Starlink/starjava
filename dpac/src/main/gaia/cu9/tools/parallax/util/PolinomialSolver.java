package gaia.cu9.tools.parallax.util;

public class PolinomialSolver {

	public static double solveSecondDegree(double a1, double a0){
		
		// get the lowest positive solution of
		// x^2 + a1*x + a0 = 0
		
		double discriminant = a1*a1 - 4* a0;
		if (discriminant < 0 ){
			// No real roots
			return Double.NaN;
		}
		
		if (discriminant == 0){
			// just one root
			return -0.5 * a1;
		} else {
			double discriminantSqrt = Math.sqrt(discriminant);
			double root = Double.MAX_VALUE;
			
			double root1 = -0.5 * (a1 + discriminantSqrt);
			if (root1>0) { root = root1; }

			double root2 = -0.5 * (a1 - discriminantSqrt);
			if ((root2 > 0) && (root2 < root)){
					root = root2;
			}
		
			if (root == Double.MAX_VALUE){
				// no positive roots
				return Double.NaN;
			}
			
			return root;
				
		}
	}
	
	public static double solveThirdDegree(double a2, double a1, double a0){
		
		// get the lowest positive solution of
		// x^3 + a2*x^2 + a1*x + a0 = 0
		
		double root = -1;
		
		// Vieta's substition
		// http://mathworld.wolfram.com/CubicFormula.html
		double Q = (3.*a1 - a2*a2)/9.;
		double R = (9.*a2*a1 - 27.*a0 - 2.*a2*a2*a2)/54.;
		
		double D = Q*Q*Q + R*R;
		
		if (D >= 0){
			double S = Math.cbrt((R + Math.sqrt(D)));
			double T = Math.cbrt((R - Math.sqrt(D)));
			root = -a2/3. + S + T;
		} else {
			double angle = Math.acos(R/Math.sqrt(-(Q*Q*Q)));
			double[] r = new double[3];
			root = Double.MAX_VALUE;
			for (int i=0; i<r.length; i++){
				r[i] = 2.*Math.sqrt(-Q)*Math.cos((angle + 2.*(double)i*Math.PI)/3.) - a2/3.;
				if ((r[i] > 0) && (r[i] < root)){
					root = r[i];
				}
			}
		}
		
		return root;
	}

}
