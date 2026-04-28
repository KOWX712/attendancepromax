package io.github.kowx712.mmuautoqr

internal const val LOGIN_FORM_POLL_INTERVAL_MS = 250
internal const val LOGIN_FORM_MAX_WAIT_MS = 5_000

internal fun buildLoginAutomationScript(userId: String, password: String, automationRunId: Int): String {
    val escapedUserId = userId.replace("\\", "\\\\").replace("'", "\\'")
    val escapedPassword = password.replace("\\", "\\\\").replace("'", "\\'")
    return """
        javascript:
        (function() {
          var startedAt = Date.now();
          var maxWaitMs = $LOGIN_FORM_MAX_WAIT_MS;
          var pollIntervalMs = $LOGIN_FORM_POLL_INTERVAL_MS;
          var selectors = {
            user: "input[type=\"text\"], input[name*=\"user\" i], input[id*=\"user\" i], input[placeholder*=\"user\" i]",
            pass: "input[type=\"password\"]",
            submit: "input[type=\"submit\"], button[type=\"submit\"], input[value*=\"sign\" i], button"
          };

          function findFields() {
            return {
              user: document.querySelector(selectors.user),
              pass: document.querySelector(selectors.pass),
              submit: document.querySelector(selectors.submit)
            };
          }

          function isDocumentReady() {
            return document.readyState === 'interactive' || document.readyState === 'complete';
          }

          function waitForForm() {
            var elapsedMs = Date.now() - startedAt;
            var fields = findFields();

            if (fields.user && fields.pass) {
              fields.user.value = '';
              fields.pass.value = '';
              setTimeout(function() {
                fields.user.value = '$escapedUserId';
                fields.pass.value = '$escapedPassword';
                if (fields.submit) {
                  fields.submit.click();
                  Android.onLoginSubmitted($automationRunId);
                } else {
                  Android.onLoginFailed($automationRunId, 'Submit button not found');
                }
              }, 100);
              return;
            }

            if (elapsedMs >= maxWaitMs) {
              Android.onLoginFailed($automationRunId, 'Login fields were not ready before timeout');
              return;
            }

            if (!isDocumentReady() || document.visibilityState === 'hidden') {
              setTimeout(waitForForm, pollIntervalMs);
              return;
            }

            setTimeout(waitForForm, pollIntervalMs);
          }

          waitForForm();
        })();
    """.trimIndent()
}
