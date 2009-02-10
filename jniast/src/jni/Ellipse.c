/*
*+
*  Name:
*     Ellipse.c

*  Purpose:
*     JNI implementations of native methods of Ellise class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     15-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Ellipse.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Ellipse_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame,       /* Frame */
   jint form,            /* Form flag */
   jdoubleArray jCentre, /* Centre coordinates */
   jdoubleArray jPoint1, /* Point 1 coordinates */
   jdoubleArray jPoint2, /* Point 2 coordinates */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer;
   AstFrame *frame;
   double centre[ 2 ];
   double point1[ 2 ];
   double point2[ 2 ];
   AstRegion *unc;

   ENSURE_SAME_TYPE(double,jdouble)

   unc = jUnc ? jniastGetPointerField( env, jUnc ).Region : NULL;
   if ( jniastCheckNotNull( env, jFrame ) &&
        jniastCheckArrayLength( env, jCentre, 2 ) &&
        jniastCheckArrayLength( env, jPoint1, 2 ) &&
        jniastCheckArrayLength( env, jPoint2, 2 ) ) {
      frame = jniastGetPointerField( env, jFrame ).Frame;
      (*env)->GetDoubleArrayRegion( env, jCentre, 0, 2, centre );
      (*env)->GetDoubleArrayRegion( env, jPoint1, 0, 2, point1 );
      (*env)->GetDoubleArrayRegion( env, jPoint2, 0, 2, point2 );
      THASTCALL( jniastList( 2, frame, unc ),
         pointer.Ellipse = astEllipse( frame, (int) form, centre, 
                                       point1, point2, unc, "" );
      )
      jniastInitObject( env, this, pointer );
   }
}
