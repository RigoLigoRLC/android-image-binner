package cc.rigoligo.imagebinner.ui.screens.profiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cc.rigoligo.imagebinner.domain.SessionManager
import cc.rigoligo.imagebinner.domain.StartDecision
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SessionStartPromptScaffold(
    requestedProfileId: Long,
    sessionManager: SessionManager,
    onResumeExistingSession: (Long) -> Unit,
    onStartRequestedProfile: (Long) -> Unit,
    modifier: Modifier = Modifier,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var pendingPrompt by remember { mutableStateOf<StartPromptType?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Button(
            onClick = {
                scope.launch {
                    when (withContext(ioDispatcher) { sessionManager.evaluateStartRequest(requestedProfileId) }) {
                        StartDecision.Start -> onStartRequestedProfile(requestedProfileId)
                        StartDecision.RequiresResumeOrRestartChoice -> {
                            pendingPrompt = StartPromptType.ResumeOrRestart
                        }
                        StartDecision.RequiresResumeOrDiscardChoice -> {
                            pendingPrompt = StartPromptType.ResumeOrDiscard
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Start sorting")
        }
    }

    val promptType = pendingPrompt
    if (promptType != null) {
        val title = when (promptType) {
            StartPromptType.ResumeOrRestart -> "Resume current session?"
            StartPromptType.ResumeOrDiscard -> "Resume existing session?"
        }
        val body = when (promptType) {
            StartPromptType.ResumeOrRestart -> {
                "A saved session already exists for this profile."
            }
            StartPromptType.ResumeOrDiscard -> {
                "Another profile has an in-progress session."
            }
        }
        val dismissLabel = when (promptType) {
            StartPromptType.ResumeOrRestart -> "Restart session"
            StartPromptType.ResumeOrDiscard -> "Discard and start new"
        }

        AlertDialog(
            onDismissRequest = { pendingPrompt = null },
            title = { Text(text = title) },
            text = { Text(text = body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val resumeProfileId = withContext(ioDispatcher) {
                                sessionManager.getCurrentSession()?.profileId ?: requestedProfileId
                            }
                            onResumeExistingSession(resumeProfileId)
                            pendingPrompt = null
                        }
                    }
                ) {
                    Text(text = "Resume existing")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(ioDispatcher) {
                                sessionManager.clearSession()
                            }
                            onStartRequestedProfile(requestedProfileId)
                            pendingPrompt = null
                        }
                    }
                ) {
                    Text(text = dismissLabel)
                }
            }
        )
    }
}

private enum class StartPromptType {
    ResumeOrRestart,
    ResumeOrDiscard
}
