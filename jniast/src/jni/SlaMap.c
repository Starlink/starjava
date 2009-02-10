/*
*+
*  Name:
*     SlaMap.c

*  Purpose:
*     JNI implementations of native methods of SlaMap class.

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
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_SlaMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SlaMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint flags            /* Flags */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.SlaMap = astSlaMap( (int) flags, "" );
   );
   jniastInitObject( env, this, pointer );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SlaMap_add(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jCvt,         /* Conversion type string */
   jdoubleArray jArgs    /* Extra arguments */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *cvt;
   double *args;

   if ( jniastCheckNotNull( env, jCvt ) ) {
      cvt = jniastGetUTF( env, jCvt );
      args = jniastCopyDoubleArray( env, jArgs, 16 );

      THASTCALL( jniastList( 1, pointer.AstObject ),
         astSlaAdd( pointer.SlaMap, cvt, args );
      )

      jniastReleaseUTF( env, jCvt, cvt );
      ALWAYS(
         free( args );
      )
   }
}
/* $Id$ */
