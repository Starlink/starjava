/*
*+
*  Name:
*     WcsMap.c

*  Purpose:
*     JNI implementations of native mathods of WcsMap class.

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
#include "uk_ac_starlink_ast_WcsMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_WcsMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint ncoord,          /* Number of coordinates */
   jint type,            /* Type of tranformation */
   jint lonax,           /* Index of longitude axis */
   jint latax            /* Index of latitude axis */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.WcsMap = astWcsMap( (int) ncoord, (int) type, (int) lonax,
                                  (int) latax, "" );
   )
   jniastInitObject( env, this, pointer );
}

/* $Id$ */
