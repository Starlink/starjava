/*
*+
*  Name:
*     CmpRegion.c

*  Purpose:
*     JNI implementations of native methods of CmpRegion class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     15-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_CmpRegion.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_CmpRegion_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jRegion1,     /* First region */
   jobject jRegion2,     /* Second region */
   jint oper             /* Operation flag */
) {
   AstPointer pointer;
   AstRegion *region1;
   AstRegion *region2;

   if ( jniastCheckNotNull( env, jRegion1 ) &&
        jniastCheckNotNull( env, jRegion2 ) ) {
       region1 = jniastGetPointerField( env, jRegion1 ).Region;
       region2 = jniastGetPointerField( env, jRegion2 ).Region;
       THASTCALL( jniastList( 2, region1, region2 ),
          pointer.CmpRegion = astCmpRegion( region1, region2, (int) oper, "" );
       )
       jniastInitObject( env, this, pointer );
   }
}
