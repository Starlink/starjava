/*
*+
*  Name:
*     GrismMap.c

*  Purpose:
*     JNI implementations of native methods of GrismMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     4-FEB-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_GrismMap.h"

/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_GrismMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;

   ASTCALL(
       pointer.GrismMap = astGrismMap( "" );
   )
   jniastInitObject( env, this, pointer );
}
