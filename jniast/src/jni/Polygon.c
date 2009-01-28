/*
*+
*  Name:
*     Polygon.c

*  Purpose:
*     JNI implementations of native methods for Polygon class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     19-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Polygon.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Polygon_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame,       /* Frame */
   jint npnt,            /* Number of points */
   jdoubleArray jXcoords,/* X coordinate array */
   jdoubleArray jYcoords,/* Y coordinate array */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer;
   AstFrame *frame;
   AstRegion *unc;
   double *points;

   ENSURE_SAME_TYPE(double,jdouble)

   unc = jUnc ? jniastGetPointerField( env, jUnc ).Region : NULL;
   if ( jniastCheckNotNull( env, jFrame ) &&
        jniastCheckArrayLength( env, jXcoords, npnt ) &&
        jniastCheckArrayLength( env, jYcoords, npnt ) ) {
      frame = jniastGetPointerField( env, jFrame ).Frame;

      points = jniastMalloc( env, sizeof( double ) * npnt * 2 );
      if ( ! points ) {
         return;
      }
      (*env)->GetDoubleArrayRegion( env, jXcoords, 0, npnt, points );
      (*env)->GetDoubleArrayRegion( env, jYcoords, 0, npnt, points + npnt );
      THASTCALL( jniastList( 2, frame, unc ),
         pointer.Polygon = astPolygon( frame, (int) npnt, npnt, points, unc,
                                       "" );
      )
      ALWAYS(
         free( points );
      )
      jniastInitObject( env, this, pointer );
   }
}
