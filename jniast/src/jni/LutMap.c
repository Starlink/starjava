/*
*+
*  Name:
*     LutMap.c

*  Purpose:
*     JNI implementations of native methods of LutMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     3-OCT-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_LutMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_LutMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jLut,    /* Array of entries */
   jdouble start,        /* Value of first lut entry */
   jdouble inc           /* Increment between entries */
) {
   AstPointer pointer;
   const double *lut;
   int nlut;

   
   if ( jniastCheckNotNull( env, jLut ) ) {
     
      nlut = (*env)->GetArrayLength( env, jLut );
      lut = (const double *) (*env)->GetDoubleArrayElements( env, jLut, NULL );
      ASTCALL(
         pointer.LutMap = astLutMap( nlut, lut, (double) start, 
                                     (double) inc, "" );
      )
      ALWAYS(
         (*env)->ReleaseDoubleArrayElements( env, jLut, (jdouble *) lut, 
                                             JNI_ABORT );
      )
      jniastSetPointerField( env, this, pointer );
   }
}
/* $Id$ */
