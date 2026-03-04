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
import androidx.compose.ui.res.stringResource
import cc.rigoligo.imagebinner.R
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
            Text(text = stringResource(R.string.session_start_sorting))
        }
    }

    val promptType = pendingPrompt
    if (promptType != null) {
        val title = when (promptType) {
            StartPromptType.ResumeOrRestart -> stringResource(R.string.session_prompt_resume_restart_title)
            StartPromptType.ResumeOrDiscard -> stringResource(R.string.session_prompt_resume_discard_title)
        }
        val body = when (promptType) {
            StartPromptType.ResumeOrRestart -> {
                stringResource(R.string.session_prompt_resume_restart_body)
            }
            StartPromptType.ResumeOrDiscard -> {
                stringResource(R.string.session_prompt_resume_discard_body)
            }
        }
        val dismissLabel = when (promptType) {
            StartPromptType.ResumeOrRestart -> stringResource(R.string.session_prompt_restart_session)
            StartPromptType.ResumeOrDiscard -> stringResource(R.string.session_prompt_discard_and_start_new)
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
                    Text(text = stringResource(R.string.session_prompt_resume_existing))
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
