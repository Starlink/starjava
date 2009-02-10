/*
*+
*  Name:
*     SpecFluxFrame.c

*  Purpose:
*     JNI implementations of native methods of SpecFluxFrame class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     13-DEC-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_SpecFluxFrame.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SpecFluxFrame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jSpecfrm,     /* SpecFrame */
   jobject jFluxfrm      /* FluxFrame */
) {
   AstPointer pointer;
   AstPointer specfrm;
   AstPointer fluxfrm;

   if ( jniastCheckNotNull( env, jSpecfrm ) && 
        jniastCheckNotNull( env, jFluxfrm ) ) {
      specfrm = jniastGetPointerField( env, jSpecfrm );
      fluxfrm = jniastGetPointerField( env, jFluxfrm );
      THASTCALL( jniastList( 2, specfrm.AstObject, fluxfrm.AstObject ),
         pointer.SpecFluxFrame = astSpecFluxFrame( specfrm.SpecFrame,
                                                   fluxfrm.FluxFrame, "" );
      )
      jniastInitObject( env, this, pointer );
   }
}
