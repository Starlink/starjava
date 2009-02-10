/*
*+
*  Name:
*     UnitMap.c

*  Purpose:
*     JNI implementations of native mathods of UnitMap class.

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
#include "uk_ac_starlink_ast_UnitMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_UnitMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint ncoord           /* Number of coordinates */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.UnitMap = astUnitMap( (int) ncoord, "" );
   )
   jniastInitObject( env, this, pointer );
}
/* $Id$ */
