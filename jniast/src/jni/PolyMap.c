/*
*+
*  Name:
*     PolyMap.c

*  Purpose:
*     JNI implementations of native methods of PolyMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     4-FEB-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_PolyMap.h"

/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_PolyMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint nin,             /* Number of input coordinates. */
   jint nout,            /* Number of output coordinates. */
   jint ncoeff_f,        /* Number of forward coefficients. */
   jdoubleArray jCoeff_f,/* Forward coefficients. */
   jint ncoeff_i,        /* Number of inverse coefficients. */
   jdoubleArray jCoeff_i /* Inverse coefficients. */
) {
   AstPointer pointer;
   const double *coeff_f = NULL;
   const double *coeff_i = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Validate arguments. */
   if ( ( ncoeff_f <= 0 ||
          jniastCheckArrayLength( env, jCoeff_f, ncoeff_f * ( 2 + nin ) ) ) &&
        ( ncoeff_i <= 0 ||
          jniastCheckArrayLength( env, jCoeff_i, ncoeff_i * ( 2 + nout ) ) ) ) {

      coeff_f = ( ncoeff_f <= 0 )
              ? NULL
              : (const double *) 
                (*env)->GetDoubleArrayElements( env, jCoeff_f, NULL );
      coeff_i = ( ncoeff_i <= 0 )
              ? NULL
              : (const double *)
                (*env)->GetDoubleArrayElements( env, jCoeff_i, NULL );
      ASTCALL(
         pointer.PolyMap = astPolyMap( nin, nout, ncoeff_f, coeff_f,
                                                  ncoeff_i, coeff_i, "" );
      )
      ALWAYS(
         if ( coeff_f ) {
            (*env)->ReleaseDoubleArrayElements( env, jCoeff_f, 
                                                (jdouble *) coeff_f,
                                                JNI_ABORT );
         }
         if ( coeff_i ) {
            (*env)->ReleaseDoubleArrayElements( env, jCoeff_i,
                                                (jdouble *) coeff_i,
                                                JNI_ABORT );
         }
      )
      jniastInitObject( env, this, pointer );
   }
}
