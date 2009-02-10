/*
*+
*  Name:
*     MatrixMap.c

*  Purpose:
*     JNI implementations of native methods of MatrixMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     3-OCT-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_MatrixMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_MatrixMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint nin,             /* Number of input coordinates */
   jint nout,            /* Number of output coordinates */
   jint form,            /* Form of the matrix argument */
   jdoubleArray jMatrix  /* Matrix defining the mapping */
) {
   AstPointer pointer;
   const double *matrix;
   int nmin;

   ENSURE_SAME_TYPE(double,jdouble)

   nmin = (nin < nout) ? nin : nout;
   if ( ( form == 0 && jniastCheckArrayLength( env, jMatrix, nin * nout ) ) ||
        ( form == 1 && jniastCheckArrayLength( env, jMatrix, nmin ) ) ||
        ( form == 2 ) ) {
      if ( form == 0 || form == 1 ) {
         matrix = (*env)->GetDoubleArrayElements( env, jMatrix, NULL );
      }
      else {
         matrix = NULL;
      }
      ASTCALL(
         pointer.MatrixMap = astMatrixMap( (int) nin, (int) nout, form, 
                                           matrix, "" );
      )
      if ( matrix != NULL ) {
         (*env)->ReleaseDoubleArrayElements( env, jMatrix,
                                             (jdouble *) matrix, JNI_ABORT );
      }
      jniastInitObject( env, this, pointer );
   }
}

/* $Id$ */
