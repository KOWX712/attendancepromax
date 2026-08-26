window.automation = (function() {
  const startedAt = Date.now();
  const maxWaitMs = 5000;
  const pollIntervalMs = 250;
  const submissionTimeoutMs = 3000;
  const userId = '__USER_ID__';
  const password = '__PASSWORD__';
  const automationRunId = __RUN_ID__;
  const selectors = {
    user: 'input[type="text"], input[name*="user" i], input[id*="user" i], input[placeholder*="user" i]',
    pass: 'input[type="password"]',
    submit: 'input[type="submit"], button[type="submit"], input[value*="sign" i], button'
  };

  function notify(type, payload) {
    AttendanceBridge.postMessage(JSON.stringify({
      type: type,
      runId: automationRunId,
      reason: payload || null
    }));
  }

  function findFields() {
    return {
      user: document.querySelector(selectors.user),
      pass: document.querySelector(selectors.pass),
      submit: document.querySelector(selectors.submit)
    };
  }

  function setFieldValue(field, value) {
    if (!field) {
      return;
    }

    field.focus();
    field.value = '';
    field.dispatchEvent(new Event('input', { bubbles: true }));
    field.value = value;
    field.dispatchEvent(new Event('input', { bubbles: true }));
    field.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function isDocumentReady() {
    return document.readyState === 'interactive' || document.readyState === 'complete';
  }

  function waitForSubmissionComplete() {
    var indicator = document.getElementById('WAIT_win0');
    var submitTime = Date.now();

    function checkIndicator() {
      var elapsed = Date.now() - submitTime;

      if (!indicator) {
        notify('load_indicator_hidden');
        return;
      }

      var style = window.getComputedStyle(indicator);
      var isHidden = style.display === 'none' || style.visibility === 'hidden';

      if (isHidden && elapsed > 100) {
        notify('load_indicator_hidden');
        return;
      }

      if (elapsed >= submissionTimeoutMs) {
        notify('load_indicator_hidden');
        return;
      }

      setTimeout(checkIndicator, pollIntervalMs);
    }

    setTimeout(checkIndicator, pollIntervalMs);
  }

  function waitForForm() {
    const elapsedMs = Date.now() - startedAt;
    const fields = findFields();

    if (fields.user && fields.pass) {
      setTimeout(function() {
        setFieldValue(fields.user, userId);
        setFieldValue(fields.pass, password);
        if (fields.submit) {
          notify('submitted');
          fields.submit.click();
          waitForSubmissionComplete();
        } else {
          notify('failed', 'Submit button not found');
        }
      }, 100);
      return;
    }

    if (elapsedMs >= maxWaitMs) {
      notify('failed', 'Login fields were not ready before timeout');
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
