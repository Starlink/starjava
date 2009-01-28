/*
*+
*  Name:
*     SpecMap.c

*  Purpose:
*     JNI implementations of native methods of SpecMap class.

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
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_SpecMap.h"

#define SPECADD_MAX_ARGS 16  /* max size of args array in specAdd method */

/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SpecMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint nin,             /* Number of inputs */
   jint flags            /* Flags parameter */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.SpecMap = astSpecMap( (int) nin, (int) flags, "" );
   )
   jniastInitObject( env, this, pointer );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SpecMap_specAdd(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jCvt,         /* Conversion type */  
   jdoubleArray jArgs    /* Auxiliary arguments */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int ncopy;
   const char *cvt;
   double *args;

   /* Get a C string from the java string. */
   if ( jniastCheckNotNull( env, jCvt ) ) {
      cvt = jniastGetUTF( env, jCvt );

      /* Copy the args array into a buffer.  Use a buffer which is as big
       * as astSpecMap might attempt to read, rather than one matching 
       * the size of the supplied array, since an overread would likely be
       * catastrophic (coredump). */
      args = jniastCopyDoubleArray( env, jArgs, SPECADD_MAX_ARGS );

      /* Call the AST function to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astSpecAdd( pointer.SpecMap, cvt, args );
      )

      /* Release resources. */
      jniastReleaseUTF( env, jCvt, cvt );
      ALWAYS(
         free( args );
      )
   }
}

