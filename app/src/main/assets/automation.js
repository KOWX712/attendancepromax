window.automation = (function() {
  const startedAt = Date.now();
  const maxWaitMs = 5000;
  const pollIntervalMs = 250;
  const userId = '__USER_ID__';
  const password = '__PASSWORD__';
  const automationRunId = __RUN_ID__;
  const selectors = {
    user: 'input[type="text"], input[name*="user" i], input[id*="user" i], input[placeholder*="user" i]',
    pass: 'input[type="password"]',
    submit: 'input[type="submit"], button[type="submit"], input[value*="sign" i], button'
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
    const elapsedMs = Date.now() - startedAt;
    const fields = findFields();

    if (fields.user && fields.pass) {
      fields.user.value = '';
      fields.pass.value = '';
      setTimeout(function() {
        fields.user.value = userId;
        fields.pass.value = password;
        if (fields.submit) {
          fields.submit.click();
          Android.onLoginSubmitted(automationRunId);
        } else {
          Android.onLoginFailed(automationRunId, 'Submit button not found');
        }
      }, 100);
      return;
    }

    if (elapsedMs >= maxWaitMs) {
      Android.onLoginFailed(automationRunId, 'Login fields were not ready before timeout');
      return;
    }

    if (!isDocumentReady() || document.visibilityState === 'hidden') {
      setTimeout(waitForForm, pollIntervalMs);
      return;
    }

    setTimeout(waitForForm, pollIntervalMs);
  }

  return {
    init: function() {
      waitForForm();
    }
  };
})();
