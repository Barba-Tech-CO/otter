package co.barbatech.otter.sqleditor

import javax.swing.JComponent

/** Editor surface contract, so the SQL editing widget can change without changing its Compose shell. */
interface SqlEditorControl {
  val component: JComponent

  fun getText(): String

  fun setText(value: String)

  fun getSelectedText(): String?

  fun findNext(
    query: String,
    matchCase: Boolean,
  ): Boolean

  fun findPrevious(
    query: String,
    matchCase: Boolean,
  ): Boolean
}
