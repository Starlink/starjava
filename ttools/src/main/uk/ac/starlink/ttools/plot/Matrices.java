package uk.ac.starlink.ttools.plot;

/**
 * Utility class for linear algebra in 3-dimensional space.
 * The array arguments to the methods here are either 3-element arrays
 * representing 3-d vectors
 * <pre>
 *    ( v[0], v[1], v[2] )
 * </pre>
 * or 9-element arrays representing 3-d matrices:
 * <pre>
 *    ( m[0], m[1], m[2],
 *      m[3], m[4], m[5],
 *      m[6], m[7], m[8] )
 * </pre>
 *
 * @author   Mark Taylor
 * @since    25 Nov 2005
 */
public class Matrices {

    /**
     * Calculates the adjoint of a matrix.
     *
     * @param  m   input matrix as 9-element array
     * @return  adj(m) as 9-element array
     */
    public static double[] adj( double[] m ) {
        return new double[] {
            +m[4]*m[8]-m[5]*m[7], -m[1]*m[8]+m[2]*m[7], +m[1]*m[5]-m[2]*m[4],
            -m[3]*m[8]+m[5]*m[6], +m[0]*m[8]-m[2]*m[6], -m[0]*m[5]+m[2]*m[3],
            +m[3]*m[7]-m[4]*m[6], -m[0]*m[7]+m[1]*m[6], +m[0]*m[4]-m[1]*m[3],
        };
    }

    /**
     * Calculates the determinant of a matrix.
     *
     * @param  m  input matrix as 9-element array
     * @return  det(m)
     */
    public static double det( double[] m ) {
        return m[0]*(m[4]*m[8]-m[5]*m[7])
             - m[1]*(m[3]*m[8]-m[5]*m[6])
             + m[2]*(m[3]*m[7]-m[4]*m[6]);
    }

    /**
     * Inverts a matrix.
     *
     * @param  m  input matrix as 9-element array
     * @return   m<sup>-1</sup> as 9-element array
     */
    public static double[] invert( double[] m ) {
        return mult( adj( m ), 1.0 / det( m ) );
    }

    /**
     * Calclulates the scalar (dot) product of two vectors.
     *
     * @param   a  vector 1
     * @param   b  vector 2
     * @return  a.b
     */
    public static double dot( double[] a, double[] b ) {
        return a[0] * b[0] + a[1] *b[1] + a[2] * b[2];
    }

    /**
     * Calculates the vector (cross) product of two vectors.
     *
     * @param  a  vector 1
     * @param  b  vector 2
     * @return  a x b
     */
    public static double[] cross( double[] a, double[] b ) {
        return new double[] {
            + ( a[1]*b[2] - a[2]*b[1] ),
            - ( a[0]*b[2] - a[2]*b[0] ),
            + ( a[0]*b[1] - a[1]*b[0] ),
        };
    }

    /**
     * Returns a unit vector along an indicated axis.
     *
     * @param   iaxis  index of axis (0, 1 or 2)
     * @return  unit vector <code>iaxis</code>
     */
    public static double[] unit( int iaxis ) {
        double[] unit = new double[ 3 ];
        unit[ iaxis ] = 1.0;
        return unit;
    }

    /**
     * Calculates the modulus of a vector.
     *
     * @param  v  input vector
     * @return  <code>|v|
     */
    public static double mod( double[] v ) {
        double m2 = 0;
        for ( int i = 0; i < v.length; i++ ) {
            m2 += v[ i ] * v[ i ];
        }
        return Math.sqrt( m2 );
    }

    /**
     * Normalises a vector.
     *
     * @param  v  input vector
     * @return  <code>|v|</code>
     */
    public static double[] normalise( double[] v ) {
        return mult( v, 1.0 / mod( v ) );
    }

    /**
     * Multiplies a vector by a constant.
     *
     * @param   v  vector of arbitrary length
     * @param   c  constant factor
     * @return  v * c
     */
    public static double[] mult( double[] v, double c ) {
        double[] r = new double[ v.length ];
        for ( int i = 0; i < v.length; i++ ) {
            r[ i ] = v[ i ] * c;
        }
        return r;
    }

    /**
     * Multiplies a matrix by a vector.
     *
     * @param   m  input matrix as 9-element array
     * @param   v  input vector as 3-element array
     * @return  m * v
     */
    public static double[] mvMult( double[] m, double[] v ) {
        return new double[] {
            m[0]*v[0] + m[1]*v[1] + m[2]*v[2],
            m[3]*v[0] + m[4]*v[1] + m[5]*v[2],
            m[6]*v[0] + m[7]*v[1] + m[8]*v[2],
        };
    }

    /**
     * Multiplies two matrices together.
     * 
     * @param   a  input matrix 1 as 9-element array
     * @param   b  input matrix 2 as 9-element array
     * @return  a * b as 9-element array
     */               
    public static double[] mmMult( double[] a, double[] b ) {
        double[] r = new double[ 9 ];
        for ( int i = 0; i < 3; i++ ) {
            for ( int j = 0; j < 3; j++ ) {
                for ( int k = 0; k < 3; k++ ) {
                    r[ 3 * i + j ] += a[ 3 * i + k ] * b[ j + 3 * k ];
                } 
            }
        }
        return r;
    }

    /**
     * Returns a string giving the contents of an arbitrary length vector.
     *
     * @param  a  array
     * @return   stringified <code>a</code>
     */
    public static String toString( double[] a ) {
        StringBuffer sbuf = new StringBuffer( "(" );
        for ( int i = 0; i < a.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ',' );
            }
            sbuf.append( ' ' );
            sbuf.append( (float) a[ i ] );
        }
        sbuf.append( " )" );
        return sbuf.toString();
    }

    /**
     * Converts a 3-d matrix from Pal-friendly form (3x3) to the form used
     * elsewhere in this class (flat 9-element array).
     *
     * @param   m   flat matrix
     * @return  pal-friendly matrix
     */
    static double[] fromPal( double[][] m ) {
        return new double[] {
            m[0][0], m[0][1], m[0][2],
            m[1][0], m[1][1], m[1][2],
            m[2][0], m[2][1], m[2][2],
        };
    }

    /**
     * Converts a 3-d matrix from the form used in this class 
     * (flat 9-element array) to Pal-friendly form (3x3).
     *
     * @param   m  flat matrix
     * @return  pal-friendly matrix
     */
    static double[][] toPal( double[] m ) {
        return new double[][] {
            { m[0], m[1], m[2], },
            { m[3], m[4], m[5], },
            { m[6], m[7], m[8], },
        };
    }
}
