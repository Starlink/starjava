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
      ASTCALL(
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
   ASTCALL(
      frm = astGetRegionFrame( pointer.Region );
   )
   return jniastMakeObject( env, (AstObject *) frm );
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Region_getUnc(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jboolean def          /* Defined behaviour flag */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstRegion *unc;
   ASTCALL(
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
   AstRegion *region;
   jobject jRegion = NULL;

   if ( jniastCheckNotNull( env, jMap ) && jniastCheckNotNull( env, jFrame ) ) {
      ASTCALL(
         region = astMapRegion( pointer.Region, 
                                jniastGetPointerField( env, jMap ).Mapping,
                                jniastGetPointerField( env, jFrame ).Frame );
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
   ASTCALL(
      astNegate( pointer.Region );
   )
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_Region_overlap(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jOther        /* Comparison region */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int result;
   if ( jniastCheckNotNull( env, jOther ) ) {
      ASTCALL(
         result = astOverlap( pointer.Region,
                              jniastGetPointerField( env, jOther ).Region );
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

   if ( jniastCheckNotNull( env, jUnc ) ) {
      ASTCALL(
         astSetUnc( pointer.Region, 
                    jniastGetPointerField( env, jUnc ).Region );
      )
   }
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
           ( in = (*env)->Get##XJtype##ArrayElements( env, jIn, NULL ) ) ) { \
         ASTCALL( \
            result = astMask##Xletter( pointer.Region, map, \
                                       inside == JNI_TRUE, (int) ndim, \
                                       lbnd, ubnd, in, (Xtype) val ); \
         ) \
         ALWAYS( \
            (*env)->Release##XJtype##ArrayElements( env, jIn, in, 0 ); \
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
/* MAKE_MASKX(L,long,jlong,Long) */
MAKE_MASKX(I,int,jint,Int)
MAKE_MASKX(S,short,jshort,Short)
MAKE_MASKX(B,signed char,jbyte,Byte)
#undef MAKE_MASKX


