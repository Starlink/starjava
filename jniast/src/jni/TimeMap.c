/*
*+
*  Name:
*     TimeMap.c

*  Purpose:
*     JNI implementations of native methods of the TimeMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     1-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_TimeMap.h"

/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_TimeMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint flags            /* Flag value */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.TimeMap = astTimeMap( (int) flags, "" );
   )
   jniastInitObject( env, this, pointer );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_TimeMap_timeAdd(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jCvt,         /* Conversion type */
   jdoubleArray jArgs    /* Conversion arguments */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *cvt;
   double *args;
   if ( jniastCheckNotNull( env, jCvt ) ) {
      cvt = jniastGetUTF( env, jCvt );
      args = jniastCopyDoubleArray( env, jArgs, 16 );

      /* Call the AST function to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astTimeAdd( pointer.TimeMap, cvt, args );
      )
   
      ALWAYS(
         jniastReleaseUTF( env, jCvt, cvt );
         free( args );
      )
   }
}
