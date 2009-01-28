/*
*+
*  Name:
*     PcdMap.c

*  Purpose:
*     JNI implementations of native mathods of PcdMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     27-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_PcdMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_PcdMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdouble disco,        /* Disco value */
   jdoubleArray jPcdcen  /* Central coordinates pair */
) {
   AstPointer pointer;
   const double *pcdcen = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   if ( jniastCheckArrayLength( env, jPcdcen, 2 ) ) {
      pcdcen = (const double *) 
               (*env)->GetDoubleArrayElements( env, jPcdcen, NULL );
      ASTCALL(
         pointer.PcdMap = astPcdMap( (double) disco, pcdcen, "" );
      )
      ALWAYS(
         if ( pcdcen ) {
            (*env)->ReleaseDoubleArrayElements( env, jPcdcen, 
                                                (jdouble *) pcdcen, JNI_ABORT );
         }
      )
      jniastInitObject( env, this, pointer );
   }
}
/* $Id$ */
