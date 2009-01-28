/*
*+
*  Name:
*     SphMap.c

*  Purpose:
*     JNI implementations of native mathods of SphMap class.

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
#include "uk_ac_starlink_ast_SphMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SphMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;
   ASTCALL(
      pointer.SphMap = astSphMap( "" );
   )
   jniastInitObject( env, this, pointer );
}
/* $Id$ */
