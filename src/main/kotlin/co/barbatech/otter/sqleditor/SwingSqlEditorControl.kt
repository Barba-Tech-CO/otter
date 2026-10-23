package co.barbatech.otter.sqleditor

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rtextarea.RTextScrollPane
import org.fife.ui.rtextarea.SearchContext
import org.fife.ui.rtextarea.SearchEngine
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.undo.UndoManager

/** RSyntaxTextArea implementation of [SqlEditorControl]. */
class SwingSqlEditorControl(
  onTextChanged: (String) -> Unit,
  onRun: (String?) -> Unit,
  onCancel: () -> Unit,
  onFind: () -> Unit,
) : SqlEditorControl {
  private var appliedDarkTheme: Boolean? = null
  private val textArea =
    RSyntaxTextArea(20, 60).apply {
      syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_SQL
      isCodeFoldingEnabled = true
      tabSize = 2
      antiAliasingEnabled = true
    }

  override val component: JComponent = RTextScrollPane(textArea).apply { lineNumbersEnabled = true }

  init {
    textArea.document.addDocumentListener(
      object : javax.swing.event.DocumentListener {
        override fun insertUpdate(event: javax.swing.event.DocumentEvent?) = onTextChanged(textArea.text)

        override fun removeUpdate(event: javax.swing.event.DocumentEvent?) = onTextChanged(textArea.text)

        override fun changedUpdate(event: javax.swing.event.DocumentEvent?) = onTextChanged(textArea.text)
      },
    )
    val undoManager = UndoManager()
    textArea.document.addUndoableEditListener(undoManager)
    textArea.actionMap.put(
      "undo",
      object : AbstractAction() {
        override fun actionPerformed(event: java.awt.event.ActionEvent?) {
          if (undoManager.canUndo()) undoManager.undo()
        }
      },
    )
    textArea.actionMap.put(
      "redo",
      object : AbstractAction() {
        override fun actionPerformed(event: java.awt.event.ActionEvent?) {
          if (undoManager.canRedo()) undoManager.redo()
        }
      },
    )
    textArea.actionMap.put(
      "run",
      object : AbstractAction() {
        override fun actionPerformed(event: java.awt.event.ActionEvent?) = onRun(getSelectedText())
      },
    )
    textArea.actionMap.put(
      "cancel",
      object : AbstractAction() {
        override fun actionPerformed(event: java.awt.event.ActionEvent?) = onCancel()
      },
    )
    textArea.actionMap.put(
      "find",
      object : AbstractAction() {
        override fun actionPerformed(event: java.awt.event.ActionEvent?) = onFind()
      },
    )

    listOf(InputEvent.CTRL_DOWN_MASK, InputEvent.META_DOWN_MASK).forEach { modifier ->
      textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier), "undo")
      textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, modifier), "redo")
      textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifier), "run")
      textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, modifier), "find")
    }
    textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel")
  }

  override fun getText(): String = textArea.text

  override fun setText(value: String) {
    textArea.text = value
  }

  override fun getSelectedText(): String? = textArea.selectedText

  override fun findNext(
    query: String,
    matchCase: Boolean,
  ): Boolean = find(query, matchCase, false)

  override fun findPrevious(
    query: String,
    matchCase: Boolean,
  ): Boolean = find(query, matchCase, true)

  fun applyTheme(dark: Boolean) {
    if (appliedDarkTheme == dark) return
    appliedDarkTheme = dark
    SwingUtilities.invokeLater {
      val themeName = if (dark) "dark.xml" else "default.xml"
      val resource = javaClass.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/$themeName")
      if (resource != null) resource.use { Theme.load(it).apply(textArea) }
    }
  }

  private fun find(
    query: String,
    matchCase: Boolean,
    forward: Boolean,
  ): Boolean {
    val context =
      SearchContext(query).apply {
        this.matchCase = matchCase
        searchForward = forward
        isRegularExpression = false
        wholeWord = false
      }
    return SearchEngine.find(textArea, context).wasFound()
  }
}
