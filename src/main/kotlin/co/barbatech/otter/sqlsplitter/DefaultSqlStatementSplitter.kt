package co.barbatech.otter.sqlsplitter

/** Splits SQL while preserving semicolons inside quoted text and comments. */
class DefaultSqlStatementSplitter : SqlStatementSplitter {
  override fun split(script: String): List<String> {
    val statements = mutableListOf<String>()
    var start = 0
    var index = 0
    var blockCommentDepth = 0
    var quote: Char? = null
    var dollarTag: String? = null
    var hasStatementContent = false

    while (index < script.length) {
      if (dollarTag != null) {
        if (script.startsWith(dollarTag, index)) {
          index += dollarTag.length
          dollarTag = null
        } else {
          index++
        }
        continue
      }

      if (quote != null) {
        if (script[index] == quote) {
          if (quote == '\'' && index + 1 < script.length && script[index + 1] == '\'') {
            index += 2
          } else {
            quote = null
            index++
          }
        } else {
          index++
        }
        continue
      }

      if (blockCommentDepth > 0) {
        when {
          script.startsWith("/*", index) -> {
            blockCommentDepth++
            index += 2
          }

          script.startsWith("*/", index) -> {
            blockCommentDepth--
            index += 2
          }

          else -> {
            index++
          }
        }
        continue
      }

      when {
        script.startsWith("--", index) -> {
          index += 2
          while (index < script.length && script[index] != '\n' && script[index] != '\r') index++
        }

        script.startsWith("/*", index) -> {
          blockCommentDepth = 1
          index += 2
        }

        script[index] == '\'' || script[index] == '"' -> {
          quote = script[index]
          hasStatementContent = true
          index++
        }

        script[index] == '$' -> {
          val tagEnd = script.indexOf('$', index + 1)
          if (tagEnd >= 0 && script.substring(index + 1, tagEnd).all { it.isLetterOrDigit() || it == '_' }) {
            dollarTag = script.substring(index, tagEnd + 1)
            hasStatementContent = true
            index = tagEnd + 1
          } else {
            index++
          }
        }

        script[index] == ';' -> {
          if (hasStatementContent) {
            script
              .substring(
                start,
                index,
              ).trim()
              .takeIf(String::isNotEmpty)
              ?.let(statements::add)
          }
          start = index + 1
          hasStatementContent = false
          index++
        }

        else -> {
          if (!script[index].isWhitespace()) hasStatementContent = true
          index++
        }
      }
    }

    if (hasStatementContent) {
      script
        .substring(start)
        .trim()
        .takeIf(String::isNotEmpty)
        ?.let(statements::add)
    }
    return statements
  }
}
