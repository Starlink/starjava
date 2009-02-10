/*
*+
*  Name:
*     Interval.c

*  Purpose:
*     JNI implementations of native methods of Interval class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     18-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Interval.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Interval_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame,       /* Frame */
   jdoubleArray jLbnd,   /* Lower limits */
   jdoubleArray jUbnd,   /* Upper limits */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer;
   AstFrame *frame;
   AstRegion *unc;
   double *lbnd;
   double *ubnd;
   int naxes;

   ENSURE_SAME_TYPE(double,jdouble)

   unc = jUnc ? jniastGetPointerField( env, jUnc ).Region : NULL;
   if ( jniastCheckNotNull( env, jFrame ) ) {
      frame = jniastGetPointerField( env, jFrame ).Frame;
      naxes = jniastGetNaxes( env, frame );
      if ( jniastCheckArrayLength( env, jLbnd, naxes ) &&
           jniastCheckArrayLength( env, jUbnd, naxes ) ) {
          lbnd = (*env)->GetDoubleArrayElements( env, jLbnd, NULL );
          ubnd = (*env)->GetDoubleArrayElements( env, jUbnd, NULL );
          THASTCALL( jniastList( 2, frame, unc ),
             pointer.Interval = astInterval( frame, lbnd, ubnd, unc, "" );
          )
          ALWAYS(
             (*env)->ReleaseDoubleArrayElements( env, jLbnd, lbnd, JNI_ABORT );
             (*env)->ReleaseDoubleArrayElements( env, jUbnd, ubnd, JNI_ABORT );
          )
          jniastInitObject( env, this, pointer );
      }
   }
}
