package org.jetbrains.plugins.scala
package lang
package psi
package impl
package toplevel
package imports

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.extensions.BooleanExt
import org.jetbrains.plugins.scala.lang.TokenSets.ID_SET
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes.IMPORT_SELECTOR
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReferenceElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.{ScImportExpr, ScImportSelector}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createImportExprFromText
import org.jetbrains.plugins.scala.lang.psi.stubs.ScImportSelectorStub

/**
  * @author Alexander Podkhalyuzin
  *         Date: 20.02.2008
  */
class ScImportSelectorImpl private(stub: StubElement[ScImportSelector], nodeType: IElementType, node: ASTNode)
  extends ScalaStubBasedElementImpl(stub, nodeType, node) with ScImportSelector {
  def this(node: ASTNode) =
    this(null, null, node)

  def this(stub: ScImportSelectorStub) =
    this(stub, IMPORT_SELECTOR, null)

  override def toString: String = "ImportSelector"

  def importedName: Option[String] =
    Option(getStub).collect {
      case stub: ScImportSelectorStub => stub
    }.flatMap {
      _.importedName
    }.orElse {
      Option(findChildByType[PsiElement](ID_SET)).map {
        _.getText
      }
    }.orElse {
      reference.map {
        _.refName
      }
    }

  def reference: Option[ScStableCodeReferenceElement] =
    Option(getStub).collect {
      case stub: ScImportSelectorStub => stub
    }.flatMap {
      _.reference
    }.orElse {
      Option(getFirstChild).collect {
        case element: ScStableCodeReferenceElement => element
      }
    }

  override def deleteSelector(): Unit = {
    val expr: ScImportExpr = PsiTreeUtil.getParentOfType(this, classOf[ScImportExpr])
    if (expr.selectors.length + expr.isSingleWildcard.toInt == 1) {
      expr.deleteExpr()
    }
    val forward: Boolean = expr.selectors.head == this
    var node = this.getNode
    var prev = if (forward) node.getTreeNext else node.getTreePrev
    var t: IElementType = null
    do {
      node.getTreeParent.removeChild(node)
      node = prev
      if (node != null) {
        prev = if (forward) node.getTreeNext else node.getTreePrev
        t = node.getElementType
      }
    } while (node != null && !(t == IMPORT_SELECTOR || t == ScalaTokenTypes.tUNDER))

    expr.selectors match {
      case Seq(sel: ScImportSelector) if !sel.isAliasedImport =>
        sel.reference.foreach { reference =>
          val withoutBracesText = expr.qualifier.getText + "." + reference.getText
          expr.replace(createImportExprFromText(withoutBracesText))
        }
      case _ =>
    }
  }

  def isAliasedImport: Boolean = {
    getStub match {
      case stub: ScImportSelectorStub => stub.isAliasedImport
      case _ =>
        PsiTreeUtil.getParentOfType(this, classOf[ScImportExpr]).selectors.nonEmpty &&
          !getLastChild.isInstanceOf[ScStableCodeReferenceElement]
    }
  }
}