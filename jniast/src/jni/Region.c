/*
*+
*  Name:
*     Region.c

*  Purpose:
*     JNI implementations of native methods of Region class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     5-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Region.h"

JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_Region_getRegionBounds(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jdoubleArray jLbnds = NULL;
   jdoubleArray jUbnds = NULL;
   jdoubleArray jResult = NULL;
   double *lbnds;
   double *ubnds;
   int naxes;

   ENSURE_SAME_TYPE(double,jdouble)

   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( naxes > 0 && 
        ! (*env)->ExceptionCheck( env ) &&
        ( jLbnds = (*env)->NewDoubleArray( env, naxes ) ) &&
        ( jUbnds = (*env)->NewDoubleArray( env, naxes ) ) &&
        ( lbnds = (*env)->GetDoubleArrayElements( env, jLbnds, NULL ) ) &&
        ( ubnds = (*env)->GetDoubleArrayElements( env, jUbnds, NULL ) ) &&
        ( jResult = (*env)->NewObjectArray( env, 2, 
                                            DoubleArrayClass, NULL ) ) ) {
      (*env)->SetObjectArrayElement( env, jResult, 0, jLbnds );
      (*env)->SetObjectArrayElement( env, jResult, 1, jUbnds );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astGetRegionBounds( pointer.Region, lbnds, ubnds );
      )
      ALWAYS(
         (*env)->ReleaseDoubleArrayElements( env, jLbnds, lbnds, 0 );
         (*env)->ReleaseDoubleArrayElements( env, jUbnds, ubnds, 0 );
      )
   }
   return jResult;
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Region_getRegionFrame(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstFrame *frm;
   THASTCALL( jniastList( 1, pointer.AstObject ),
      frm = astGetRegionFrame( pointer.Region );
   )
   return jniastMakeObject( env, (AstObject *) frm );
}

JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_Region_getRegionPoints(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int npoint;
   int npoint2;
   int naxes;
   double *points = NULL;
   jobjectArray jResult = NULL;
   jdoubleArray axValues = NULL;
   int ok;
   int i;

   ENSURE_SAME_TYPE(double, jdouble)

   THASTCALL( jniastList( 1, pointer.AstObject ),
      astGetRegionPoints( pointer.Region, 0, 0, &npoint, (double *) NULL );
      naxes = astGetI( pointer.AstObject, "Naxes" );
   )

   ok = ( ! (*env)->ExceptionCheck( env ) )
     && ( points = jniastMalloc( env, naxes * npoint * sizeof( double ) ) )
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
         astGetRegionPoints( pointer.Region, npoint, naxes, &npoint2, points );
      )
      if ( ! (*env)->ExceptionCheck( env ) ) {
         for ( i = 0; i < naxes; i++ ) {
            if ( ! (*env)->ExceptionCheck( env ) ) {
               axValues = (*env)->GetObjectArrayElement( env, jResult, i );
               if ( ! (*env)->ExceptionCheck( env ) ) {
                  (*env)->SetDoubleArrayRegion( env, axValues, 0, npoint,
                                                points + i * npoint );
               }
            }
         }
      }
   }
   free( points );
   return jResult;
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Region_getUnc(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jboolean def          /* Defined behaviour flag */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstRegion *unc;
   THASTCALL( jniastList( 1, pointer.AstObject ),
      unc = astGetUnc( pointer.Region, def == JNI_TRUE );
   )
   return jniastMakeObject( env, (AstObject *) unc );
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Region_mapRegion(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jMap,         /* Mapping */
   jobject jFrame        /* Frame */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer map;
   AstPointer frame;
   AstRegion *region;
   jobject jRegion = NULL;

   if ( jniastCheckNotNull( env, jMap ) && jniastCheckNotNull( env, jFrame ) ) {
      map = jniastGetPointerField( env, jMap );
      frame = jniastGetPointerField( env, jFrame );
      THASTCALL( jniastList( 3, pointer.AstObject, map.AstObject,
                                frame.AstObject ),
         region = astMapRegion( pointer.Region, map.Mapping, frame.Frame );
      )
      jRegion = jniastMakeObject( env, (AstObject *) region );
   }
   return jRegion;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Region_negate(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   THASTCALL( jniastList( 1, pointer.AstObject ),
      astNegate( pointer.Region );
   )
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_Region_overlap(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jOther        /* Comparison region */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer other;
   int result;
   if ( jniastCheckNotNull( env, jOther ) ) {
      other = jniastGetPointerField( env, jOther );
      THASTCALL( jniastList( 2, pointer.AstObject, other.AstObject ),
         result = astOverlap( pointer.Region, other.Region );
      )
   }
   return result;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Region_setUnc(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer unc;

   if ( jniastCheckNotNull( env, jUnc ) ) {
      unc = jniastGetPointerField( env, jUnc );
      THASTCALL( jniastList( 2, pointer.AstObject, unc.AstObject ),
         astSetUnc( pointer.Region, unc.Region );
      )
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Region_showMesh(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jboolean format,      /* Whether to format axis values */
   jstring jTtl          /* Title string */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *ttl;

   ttl = jniastGetUTF( env, jTtl );
   THASTCALL( jniastList( 1, pointer.AstObject ),
      astShowMesh( pointer.Region, format == JNI_TRUE, ttl );
   )
   jniastReleaseUTF( env, jTtl, ttl );
}

#define MAKE_MASKX(Xletter,Xtype,Xjtype,XJtype) \
 \
JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_Region_mask##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jobject jMap,         /* Mapping */ \
   jboolean inside,      /* Inside flag */ \
   jint ndim,            /* Number of dimensions */ \
   jintArray jLbnd,      /* Lower bounds */ \
   jintArray jUbnd,      /* Upper bounds */ \
   Xjtype##Array jIn,    /* Pixel grid */ \
   Xjtype val            /* Mask substitution value */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   AstMapping *map; \
   int *lbnd = NULL; \
   int *ubnd = NULL; \
   Xtype *in = NULL; \
   int npix; \
   int i; \
   jint result; \
 \
   ENSURE_SAME_TYPE(Xtype,Xjtype) \
   ENSURE_SAME_TYPE(int,jint) \
 \
   map = jMap ? jniastGetPointerField( env, jMap ).Mapping \
              : NULL; \
   if ( jniastCheckArrayLength( env, jLbnd, ndim ) && \
        jniastCheckArrayLength( env, jUbnd, ndim ) && \
        ( lbnd = (*env)->GetIntArrayElements( env, jLbnd, NULL ) ) && \
        ( ubnd = (*env)->GetIntArrayElements( env, jUbnd, NULL ) ) ) { \
      npix = 1; \
      for ( i = 0; i < ndim; i++ ) { \
         npix *= ( abs( ubnd[ i ] - lbnd[ i ] ) + 1 ); \
      } \
      if ( jniastCheckArrayLength( env, jIn, npix ) && \
           ( in = (Xtype *) \
                  (*env)->Get##XJtype##ArrayElements( env, jIn, NULL ) ) ) { \
         THASTCALL( jniastList( 2, pointer.AstObject, (AstObject *) map ),  \
            result = astMask##Xletter( pointer.Region, map, \
                                       inside == JNI_TRUE, (int) ndim, \
                                       lbnd, ubnd, in, (Xtype) val ); \
         ) \
         ALWAYS( \
            (*env)->Release##XJtype##ArrayElements( env, jIn, \
                                                    (Xjtype *) in, 0 ); \
         ) \
      } \
      ALWAYS( \
         if ( lbnd ) { \
            (*env)->ReleaseIntArrayElements( env, jLbnd, lbnd, JNI_ABORT ); \
         } \
         if ( ubnd ) { \
            (*env)->ReleaseIntArrayElements( env, jUbnd, ubnd, JNI_ABORT ); \
         } \
      ) \
   } \
   return result; \
}
MAKE_MASKX(D,double,jdouble,Double)
MAKE_MASKX(F,float,jfloat,Float)
MAKE_MASKX(L,long,jlong,Long)
MAKE_MASKX(I,int,jint,Int)
MAKE_MASKX(S,short,jshort,Short)
MAKE_MASKX(B,signed char,jbyte,Byte)
#undef MAKE_MASKX


