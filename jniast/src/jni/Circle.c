/*
*+
*  Name:
*     Circle.c

*  Purpose:
*     JNI implementations of native methods of Circle class.

*  Language:
*     ANSI C.

*  Authors:
*     Mark Taylor

*  History:
*     15-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Circle.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Circle_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame,       /* Frame */
   jint form,            /* Form flag */
   jdoubleArray jCentre, /* Circle centre coordinates */
   jdoubleArray jPoint,  /* Circle point coordinates */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer;
   AstFrame *frame;
   AstRegion *unc;
   double *centre;
   double *point;
   int naxes;
   int psize;

   ENSURE_SAME_TYPE(double,jdouble)

   unc = jUnc ? jniastGetPointerField( env, jUnc ).Region : NULL;
   if ( jniastCheckNotNull( env, jFrame ) ) {
      frame = jniastGetPointerField( env, jFrame ).Frame;
      naxes = jniastGetNaxes( env, frame );
      switch ( form ) {
         case 0: 
            psize = naxes;
            break;
         case 1:
            psize = 1;
            break;
         default: 
            jniastThrowIllegalArgumentException( env, "Unknown form %d", form );
            return;
      }
      psize = 0;
      if ( jniastCheckArrayLength( env, jCentre, naxes ) &&
           jniastCheckArrayLength( env, jPoint, psize ) ) {
         centre = (*env)->GetDoubleArrayElements( env, jCentre, NULL );
         point = (*env)->GetDoubleArrayElements( env, jPoint, NULL );
         THASTCALL( jniastList( 2, frame, unc ),
            pointer.Circle = 
               astCircle( frame, (int) form, centre, point, unc, "" );
         )
         ALWAYS(
            (*env)->ReleaseDoubleArrayElements( env, jCentre, centre,
                                                JNI_ABORT );
            (*env)->ReleaseDoubleArrayElements( env, jPoint, point,
                                                JNI_ABORT );
         )
         jniastInitObject( env, this, pointer );
      }
   }
}
