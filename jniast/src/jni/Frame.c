/*
*+
*  Name:
*     Frame.c

*  Purpose:
*     JNI implementations of native methods of Frame class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)


*  History:
*     24-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Frame.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Frame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint naxes            /* Number of axes */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.Frame = astFrame( (int) naxes, "" );
   )
   jniastSetPointerField( env, this, pointer );
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Frame_angle(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jA,      /* First point coordinates */
   jdoubleArray jB,      /* Second point coordinates */
   jdoubleArray jC       /* Third point coordinates */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const double *a;
   const double *b;
   const double *c;
   int naxes;
   jdouble result;

   /* Check our arguments look OK. */
   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jA, naxes ) &&
        jniastCheckArrayLength( env, jB, naxes ) &&
        jniastCheckArrayLength( env, jC, naxes ) ) {

      /* Get C arrays from the java arrays. */
      a = (double *) (*env)->GetDoubleArrayElements( env, jA, NULL );
      b = (double *) (*env)->GetDoubleArrayElements( env, jB, NULL );
      c = (double *) (*env)->GetDoubleArrayElements( env, jC, NULL );

      /* Call the AST routine to do the work. */
      ASTCALL(
         result = (jdouble) astAngle( pointer.Frame, a, b, c );
      )

      /* Release resources. */
      ALWAYS( 
         (*env)->ReleaseDoubleArrayElements( env, jA, (jdouble *) a,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jB, (jdouble *) b,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jC, (jdouble *) c,
                                             JNI_ABORT );
      )
   }

   /* Return the result. */
   return result;
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Frame_axAngle(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jA,      /* First point coordinates */
   jdoubleArray jB,      /* Second point coordinates */
   jint axis             /* Index of axis to use */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const double *a;
   const double *b;
   int naxes;
   jdouble result;
   
   /* Check our arguments look OK. */
   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jA, naxes ) &&
        jniastCheckArrayLength( env, jB, naxes ) ) {

      /* Get C arrays from the java arrays. */
      a = (double *) (*env)->GetDoubleArrayElements( env, jA, NULL );
      b = (double *) (*env)->GetDoubleArrayElements( env, jB, NULL );

      /* Call the AST routine to do the work. */
      ASTCALL(
         result = (jdouble) astAxAngle( pointer.Frame, a, b, (int) axis );
      )

      /* Release resources. */
      ALWAYS(
         (*env)->ReleaseDoubleArrayElements( env, jA, (jdouble *) a,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jB, (jdouble *) b,
                                             JNI_ABORT );
      )
   }

   /* Return the result. */
   return result;
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Frame_axOffset(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint axis,            /* Index of axis to use */
   jdouble v1,           /* Original axis value */
   jdouble dist          /* Increment */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double result;

   ASTCALL(
      result = astAxOffset( pointer.Frame, 
                            (int) axis, (double) v1, (double) dist );
   )
   return (jdouble) result;
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Frame_axDistance(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint axis,            /* Index of axis to use */
   jdouble v1,           /* First axis value */
   jdouble v2            /* Second axis value */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double result;
 
   ASTCALL(
      result = astAxDistance( pointer.Frame, 
                              (int) axis, (double) v1, (double) v2 );
   )
   return (jdouble) result;
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Frame_convert(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jTo,          /* Frame to convert into */
   jstring jDomainlist   /* List of domains */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer to;
   const char *domainlist;
   AstFrameSet *newfs;

   /* Check the arguments look OK. */
   if ( jniastCheckNotNull( env, jTo ) &&
        jniastCheckNotNull( env, jDomainlist ) ) {

      /* Get C data from the java data. */
      to = jniastGetPointerField( env, jTo );
      domainlist = jniastGetUTF( env, jDomainlist );

      /* Call the AST function to do the work. */
      ASTCALL(
         newfs = astConvert( pointer.Frame, to.Frame, domainlist );
      )
   
      /* Release resources. */
      jniastReleaseUTF( env, jDomainlist, domainlist );
   }

   /* Create and return the new FrameSet object. */
   return jniastMakeObject( env, (AstObject *) newfs );
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Frame_distance(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jPoint1, /* Coordiates of first point */
   jdoubleArray jPoint2  /* Coordinates of second point */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const double *point1;
   const double *point2;
   jdouble dist;
   int naxes;

   /* Check the arguments look OK. */
   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jPoint1, naxes ) &&
        jniastCheckArrayLength( env, jPoint2, naxes ) ) {

      /* Get C data from AST data. */
      point1 = (double *) (*env)->GetDoubleArrayElements( env, jPoint1, NULL );
      point2 = (double *) (*env)->GetDoubleArrayElements( env, jPoint2, NULL );

      /* Call the AST function to do the work. */
      ASTCALL(
         dist = (jdouble) astDistance( pointer.Frame, point1, point2 );
      )

      /* Release resources. */
      ALWAYS(
         (*env)->ReleaseDoubleArrayElements( env, jPoint1, (jdouble *) point1, 
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jPoint2, (jdouble *) point2, 
                                             JNI_ABORT );
      )
   }

   /* Return the result. */
   return dist;
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Frame_findFrame(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jTemplate,    /* Template Frame */
   jstring jDomainlist   /* List of domains */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer template;
   const char *domainlist;
   AstFrameSet *newfs;

   /* Get C data from java data and check arguments. */
   if ( jniastCheckNotNull( env, jDomainlist ) &&
        ( template = jniastGetPointerField( env, jTemplate ) ).ptr != NULL ) {
      domainlist = jniastGetUTF( env, jDomainlist );

      /* Call the C function to do the work. */
      ASTCALL(
         newfs = astFindFrame( pointer.Frame, template.Frame, domainlist );
      )

      /* Release resources. */
      jniastReleaseUTF( env, jDomainlist, domainlist );
   }

   /* Create and return the new AstFrameSet. */
   return jniastMakeObject( env, (AstObject *) newfs );
}

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_Frame_format(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint axis,            /* Axis index */
   jdouble value         /* Value to format */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *result;

   /* Call the C function to do the work. */
   ASTCALL(
      result = astFormat( pointer.Frame, (int) axis, (double) value );
   )

   /* Create and return a new String holding the value. */
   return (*env)->ExceptionCheck( env ) ? NULL 
                                        : (*env)->NewStringUTF( env, result );
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_Frame_getActiveUnit(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int result;

   /* Call the C function to do the work. */
   ASTCALL(
      result = astGetActiveUnit( pointer.Frame );
   )
   return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Frame_norm(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jValue   /* Coordinates for normalisation */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double *value;
   int naxes;

   /* Check arguments look OK. */
   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jValue, naxes ) ) {

      /* Get C values from the java array. */
      value = (double *) (*env)->GetDoubleArrayElements( env, jValue, NULL );

      /* Call the AST function to do the work. */
      ASTCALL(
         astNorm( pointer.Frame, value );
      )

      /* Copy the results back to the java array. */
      ALWAYS( 
         (*env)->ReleaseDoubleArrayElements( env, jValue, value, 0 );
      )
   }
}

JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_Frame_offset(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jPoint1, /* First point coordinates */
   jdoubleArray jPoint2, /* Second point coordinates */
   jdouble offset        /* Required offset */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jdoubleArray jPoint3;
   const double *point1;
   const double *point2;
   double *point3;
   int naxes;

   /* Check the arguments look OK. */
   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jPoint1, naxes ) &&
        jniastCheckArrayLength( env, jPoint2, naxes ) ) {

      /* Create a java array to hold the results. */
      ( jPoint3 = (*env)->NewDoubleArray( env, naxes ) ) &&

      /* Get C data from the java data. */
      ( point1 = (double *) 
                 (*env)->GetDoubleArrayElements( env, jPoint1, NULL ) ) &&
      ( point2 = (double *) 
                 (*env)->GetDoubleArrayElements( env, jPoint2, NULL ) ) &&
      ( point3 = (double *) 
                 (*env)->GetDoubleArrayElements( env, jPoint3, NULL ) );

      /* Call the AST function to do the work. */
      ASTCALL(
         astOffset( pointer.Frame, point1, point2, (double) offset, point3 );
      )

      /* Release resources and copy result into java array. */
      ALWAYS(
         (*env)->ReleaseDoubleArrayElements( env, jPoint1, (jdouble *) point1,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jPoint2, (jdouble *) point2,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jPoint3, (jdouble *) point3,
                                             0 );
      )
   }

   /* Return the result. */
   return jPoint3;
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Frame_offset2(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jPoint1, /* First point coordinates */
   jdouble angle,        /* Angle */
   jdouble offset,       /* Offset */
   jdoubleArray jPoint2  /* Second point coordinates */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jdouble newangle;
   const double *point1;
   double *point2;

   /* Check the jPoint2 array is large enough to hold the data we will
    * write into it. */
   if ( jniastCheckArrayLength( env, jPoint1, 2 ) &&
        jniastCheckArrayLength( env, jPoint2, 2 ) ) {

      /* Get C data from java data. */
      point1 = (double *) (*env)->GetDoubleArrayElements( env, jPoint1, NULL );
      point2 = (double *) (*env)->GetDoubleArrayElements( env, jPoint2, NULL );

      /* Call the C function to do the work. */
      ASTCALL(
         newangle = (jdouble) astOffset2( pointer.Frame, point1, 
                                          (double) angle, (double) offset,
                                          point2 );
      )

      /* Release resources and copy values back to java array. */
      ALWAYS(
         (*env)->ReleaseDoubleArrayElements( env, jPoint1, (jdouble *) point1,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jPoint2, (jdouble *) point2,
                                             0 );
      )
   }

   /* Return the result. */
   return newangle;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Frame_permAxes(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jintArray jPerm       /* Axis permutation */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const int *perm;
   int naxes;

   /* Check the arguments look OK. */
   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jPerm, naxes ) ) {

      /* Get C data from the java data. */
      perm = (int *) (*env)->GetIntArrayElements( env, jPerm, NULL );

      /* Call the C function to do the work. */
      ASTCALL(
         astPermAxes( pointer.Frame, perm );
      )

      /* Release resources. */
      ALWAYS(
         (*env)->ReleaseIntArrayElements( env, jPerm, (jint *) perm, 0 );
      )
   }
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Frame_pickAxes(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint naxes,           /* Number of axes in output Frame */
   jintArray jAxes,      /* Axes to be picked */
   jobjectArray jMapArr  /* Slot for new mapping */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const int *axes;
   AstMapping *map;
   AstFrame *newframe;

   /* Check the arguments look OK. */
   if ( jniastCheckNotNull( env, jAxes ) &&
        ( jMapArr == NULL || jniastCheckArrayLength( env, jMapArr, 1 ) ) ) {

      /* Get C data from the java data. */
      axes = (int *) (*env)->GetIntArrayElements( env, jAxes, NULL );
      
      /* Call the C function to do the work. */
      ASTCALL( 
         newframe = astPickAxes( pointer.Frame, (int) naxes, axes, 
                                 ( jMapArr ? &map : NULL ) );
      )

      /* Write the mapping into the first slot of the supplied mapping 
       * array if reqired. */
      if ( jMapArr != NULL && ! (*env)->ExceptionCheck( env ) ) {
         (*env)->SetObjectArrayElement( env, jMapArr, 0, 
                                        jniastMakeObject( env, 
                                                          (AstObject *) map ) );
      }

      /* Release resources. */
      ALWAYS(
         (*env)->ReleaseIntArrayElements( env, jAxes, (jint *) axes,
                                          JNI_ABORT );
      )
   }

   /* Return the new frame. */
   return jniastMakeObject( env, (AstObject *) newframe );
}

JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_Frame_resolve(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jPoint1, /* First point (supplied) */
   jdoubleArray jPoint2, /* Second point (supplied) */
   jdoubleArray jPoint3, /* Third point (supplied) */
   jdoubleArray jPoint4  /* Fourth point (returned) */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const double *point1;
   const double *point2;
   const double *point3;
   double *point4;
   double *d;
   jdoubleArray jD = NULL;
   int naxes;

   /* Check arguments. */
   naxes = jniastGetNaxes( env, pointer.Frame );
   if ( jniastCheckArrayLength( env, jPoint1, naxes ) &&
        jniastCheckArrayLength( env, jPoint2, naxes ) &&
        jniastCheckArrayLength( env, jPoint3, naxes ) &&
        jniastCheckArrayLength( env, jPoint4, naxes ) &&
        ( jD = (*env)->NewDoubleArray( env, 2 ) ) ) {

      /* Get C data from java data. */
      point1 = (double *) (*env)->GetDoubleArrayElements( env, jPoint1, NULL );
      point2 = (double *) (*env)->GetDoubleArrayElements( env, jPoint2, NULL );
      point3 = (double *) (*env)->GetDoubleArrayElements( env, jPoint3, NULL );
      point4 = (double *) (*env)->GetDoubleArrayElements( env, jPoint4, NULL );
      d = (*env)->GetDoubleArrayElements( env, jD, NULL );

      /* Call the AST routine to do the work. */
      ASTCALL(
         astResolve( pointer.Frame, point1, point2, point3, point4, 
                     &d[ 0 ], &d[ 1 ] );
      )

      /* Release resources, copying output data back. */
      ALWAYS(
         (*env)->ReleaseDoubleArrayElements( env, jPoint1, (jdouble *) point1,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jPoint2, (jdouble *) point2,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jPoint3, (jdouble *) point3,
                                             JNI_ABORT );
         (*env)->ReleaseDoubleArrayElements( env, jPoint4, (jdouble *) point4,
                                             0 );
         (*env)->ReleaseDoubleArrayElements( env, jD, (double *) d, 
                                             0 );
      )
   }
   return jD;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Frame_setActiveUnit(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jboolean jvalue       /* New flag value */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int value;

   /* Turn the jboolean into an int. */
   value = ( jvalue != JNI_FALSE ) ? 1 : 0;

   /* Call the C function to do the work. */
   ASTCALL(
      astSetActiveUnit( pointer.Frame, value );
   )
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Frame_unformat(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint axis,            /* Axis index */
   jstring jString       /* Formatted coordinate value */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *string;
   double value;

   /* Check arguments look OK. */
   if ( jniastCheckNotNull( env, jString ) ) {

      /* Get C data from java data. */
      string = jniastGetUTF( env, jString );

      /* Call the C function to do the work. */
      ASTCALL(
         astUnformat( pointer.Frame, (int) axis, string, &value );
      )

      /* Release resources. */
      jniastReleaseUTF( env, jString, string );
   }

   /* Return the result. */
   return (jdouble) value;
}

/* $Id$ */
