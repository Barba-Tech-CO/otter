package co.barbatech.otter.connectionmanager.helper

import javax.swing.JFileChooser

/** Native file chooser for the SQLite database path. */
internal object ConnectionFileChooserHelper {
  fun chooseSqliteFile(): String? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Choose a SQLite database file"
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      chooser.selectedFile?.absolutePath
    } else {
      null
    }
  }
}
