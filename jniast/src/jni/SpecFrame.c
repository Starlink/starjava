/*
*+
*  Name:
*     SpecFrame.c

*  Purpose:
*     JNI implementations of native methods of SpecFrame class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     14-MAR-2003 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_SpecFrame.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SpecFrame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.SpecFrame = astSpecFrame( "" );
   )
   jniastInitObject( env, this, pointer );
}

JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_SpecFrame_getRefPos(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrm          /* SkyFrame at which result is defined */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer frmPointer;
   double lon;
   double lat;
   jdoubleArray result = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Get the AstFrame object. */
   if ( jFrm == NULL ) {
      frmPointer.SkyFrame = NULL;
   }
   else {
      frmPointer = jniastGetPointerField( env, jFrm );
   }

   /* Call the AST function to do the work. */ 
   THASTCALL( jniastList( 2, pointer.AstObject, frmPointer.AstObject ),
      astGetRefPos( pointer.SpecFrame, frmPointer.SkyFrame, &lon, &lat );
   )

   /* Package the result into a 2-element java double[] array. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      result = (*env)->NewDoubleArray( env, 2 );
      (*env)->SetDoubleArrayRegion( env, result, 0, 1, &lon );
      (*env)->SetDoubleArrayRegion( env, result, 1, 1, &lat );
   }

   /* Return the result. */
   return result;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SpecFrame_setRefPos(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrm,         /* SkyFrame at which result is defined */
   jdouble lon,          /* Longitude of the reference point */
   jdouble lat           /* Latitude of the reference point */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer frmPointer;

   /* Get the AstFrame object. */
   if ( jFrm == NULL ) {
      frmPointer.SkyFrame = NULL;
   }
   else {
      frmPointer = jniastGetPointerField( env, jFrm );
   }

   /* Call the AST function to do the work. */
   THASTCALL( jniastList( 2, pointer.AstObject, frmPointer.AstObject ),
      astSetRefPos( pointer.SpecFrame, frmPointer.SkyFrame, 
                    (double) lon, (double) lat );
   )
}

