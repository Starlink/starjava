/*
*+
*  Name:
*     Box.c

*  Purpose:
*     JNI implementations of native methods of Box class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     6-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Box.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Box_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame,       /* Frame */
   jint form,            /* Form flag */
   jdoubleArray jPoint1, /* First point */
   jdoubleArray jPoint2, /* Second point */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer;
   AstFrame *frame;
   AstRegion *unc;
   double *point1;
   double *point2;
   int naxes;

   ENSURE_SAME_TYPE(double,jdouble)

   unc = jUnc ? jniastGetPointerField( env, jUnc ).Region : NULL;
   if ( jniastCheckNotNull( env, jFrame ) ) {
      frame = jniastGetPointerField( env, jFrame ).Frame;
      naxes = jniastGetNaxes( env, frame );
      if ( jniastCheckArrayLength( env, jPoint1, naxes ) &&
           jniastCheckArrayLength( env, jPoint2, naxes ) ) {
         point1 = (*env)->GetDoubleArrayElements( env, jPoint1, NULL );
         point2 = (*env)->GetDoubleArrayElements( env, jPoint2, NULL );
         THASTCALL( jniastList( 2, frame, unc ),
            pointer.Box = astBox( frame, (int) form, point1, point2, unc, "" );
         )
         ALWAYS(
            (*env)->ReleaseDoubleArrayElements( env, jPoint1, point1,
                                                JNI_ABORT );
            (*env)->ReleaseDoubleArrayElements( env, jPoint2, point2,
                                                JNI_ABORT );
         )
         jniastInitObject( env, this, pointer );
      }
   }
}
