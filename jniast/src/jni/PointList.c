/*
*+
*  Name:
*     PointList.c

*  Purpose:
*     JNI implementations of native methods of PointList class.

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
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_PointList.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_PointList_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame,       /* Frame */
   jint npnt,            /* Number of points */
   jobjectArray jPoints, /* Point list array */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer;
   AstFrame *frame;
   AstRegion *unc;
   double *points;
   int naxes;
   jobject jCoords;
   int iaxis;

   ENSURE_SAME_TYPE(double,jdouble)

   unc = jUnc ? jniastGetPointerField( env, jUnc ).Region : NULL;
   if ( jniastCheckNotNull( env, jFrame ) &&
        jniastCheckNotNull( env, jPoints ) ) {
      frame = jniastGetPointerField( env, jFrame ).Frame;
      naxes = jniastGetNaxes( env, frame );
      if ( ! jniastCheckArrayLength( env, jPoints, naxes ) ) {
          return;
      }
      points = jniastMalloc( env, sizeof( double ) * npnt * naxes );
      if ( ! points ) {
         return;
      }
      for ( iaxis = 0; iaxis < naxes; iaxis++ ) {
         jCoords = (*env)->GetObjectArrayElement( env, jPoints, iaxis );
         if ( ! jCoords ) {
            jniastThrowIllegalArgumentException( env, 
                       "Element %d of points array is null", iaxis );
            free( points );
            return;
         }
         if ( (*env)->GetArrayLength( env, jCoords ) != npnt ) {
            jniastThrowIllegalArgumentException( env,
                       "Element %d of points array is wrong length", iaxis );
            free( points );
            return;
         }
         (*env)->GetDoubleArrayRegion( env, jCoords, 0, npnt, 
                                       points + iaxis * npnt );
      }
      THASTCALL( jniastList( 2, frame, unc ),
         pointer.PointList = astPointList( frame, npnt, naxes, npnt, 
                                           points, unc, 
                                           "" );
      )
      ALWAYS(
         free( points );
      )
      jniastInitObject( env, this, pointer );
   }
}

JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_PointList_points(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int naxes = 0;
   int npoint = 0;
   double *out = NULL;
   jobjectArray jResult = NULL;
   jdoubleArray axValues = NULL;
   int i;
   int ok;

   ENSURE_SAME_TYPE(double,jdouble)

   THASTCALL( jniastList( 1, pointer.AstObject ),
      naxes = astGetI( pointer.AstObject, "Naxes" );
      npoint = astGetI( pointer.AstObject, "ListSize" );
   )

   ok =  ( ! (*env)->ExceptionCheck( env ) )
      && ( out = jniastMalloc( env, naxes * npoint * sizeof( double ) ) )
      && ( jResult = (*env)->NewObjectArray( env, naxes, DoubleArrayClass,
                                             NULL ) );
   for ( i = 0; i < naxes; i++ ) {
      ok = ok && ( axValues = (*env)->NewDoubleArray( env, npoint ) );
      if ( ok ) {
         (*env)->SetObjectArrayElement( env, jResult, i, axValues );
      }
   }
   if ( ok ) {
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astPoints( pointer.PointList, naxes, npoint, out );
      )
      if ( ! (*env)->ExceptionCheck( env ) ) {
         for ( i = 0; i < naxes; i++ ) {
            if ( ! (*env)->ExceptionCheck( env ) ) {
               axValues = (*env)->GetObjectArrayElement( env, jResult, i );
               if ( ! (*env)->ExceptionCheck( env ) ) {
                  (*env)->SetDoubleArrayRegion( env, axValues, 0, npoint,
                                                out + i * npoint );
               }
            }
         }
      }
   }
   free( out );
   return jResult;
}
