package co.barbatech.otter.connectionmanager.state

internal sealed interface PasswordOutcome {
  data class Ready(
    val password: String?,
  ) : PasswordOutcome

  data object Prompted : PasswordOutcome
}
