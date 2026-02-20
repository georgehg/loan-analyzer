import { For, Show, createMemo, createSignal } from 'solid-js';

const FIELD_SPEC = {
  fields: [
    { key: 'firstName', label: 'First name', type: 'text', required: true },
    { key: 'age', label: 'Age', type: 'number', required: true, min: 18, max: 120 },
    { key: 'income', label: 'Annual income (USD)', type: 'number', required: true, min: 0 },
    { key: 'creditScore', label: 'Credit score', type: 'number', required: true, min: 300, max: 850 },
    { key: 'requestedAmount', label: 'Requested amount (USD)', type: 'number', required: true, min: 1000 },
    {
      key: 'employmentStatus',
      label: 'Employment status',
      type: 'select',
      required: true,
      options: ['employed', 'self-employed', 'unemployed']
    },
    { key: 'employmentYears', label: 'Years employed', type: 'number', required: true, min: 0 }
  ]
};

function initialValues() {
  const v = {};
  for (const f of FIELD_SPEC.fields) {
    // Keep everything as string in the UI; coerce on submit.
    v[f.key] = '';
  }
  return v;
}

function coercePayload(values) {
  const out = {};
  for (const f of FIELD_SPEC.fields) {
    const raw = values[f.key];
    if (f.type === 'number') {
      // backend expects double; empty stays null (but will be blocked by validation)
      out[f.key] = raw === '' ? null : parseFloat(raw);
    } else {
      out[f.key] = raw;
    }
  }
  return out;
}

function validateField(field, value) {
  const errors = [];
  const isEmpty = value === '' || value == null;

  if (field.required && isEmpty) {
    errors.push('Required.');
    return errors;
  }

  if (isEmpty) return errors;

  if (field.type === 'number') {
    const n = Number(value);
    if (Number.isNaN(n)) {
      errors.push('Must be a number.');
      return errors;
    }
    if (field.min != null && n < field.min) errors.push(`Must be ≥ ${field.min}.`);
    if (field.max != null && n > field.max) errors.push(`Must be ≤ ${field.max}.`);
  }

  if (field.type === 'select') {
    if (field.options && !field.options.includes(value)) {
      errors.push('Invalid option.');
    }
  }

  return errors;
}

export default function App() {
  const [values, setValues] = createSignal(initialValues());
  const [touched, setTouched] = createSignal({});

  const [submitting, setSubmitting] = createSignal(false);
  const [apiError, setApiError] = createSignal('');
  const [result, setResult] = createSignal(null);

  const errors = createMemo(() => {
    const v = values();
    const e = {};
    for (const f of FIELD_SPEC.fields) {
      const errs = validateField(f, v[f.key]);
      if (errs.length) e[f.key] = errs;
    }
    return e;
  });

  const isValid = createMemo(() => Object.keys(errors()).length === 0);

  function markTouched(key) {
    setTouched((t) => ({ ...t, [key]: true }));
  }

  function setField(key, value) {
    setValues((v) => ({ ...v, [key]: value }));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setApiError('');
    setResult(null);

    // Mark all as touched so errors show
    const allTouched = {};
    for (const f of FIELD_SPEC.fields) allTouched[f.key] = true;
    setTouched(allTouched);

    if (!isValid()) return;

    setSubmitting(true);
    try {
      const payload = coercePayload(values());

      const res = await fetch('/api/v1/evaluate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const body = await res.text();
      const data = JSON.parse(body);

      if (!res.ok) {
         setResult(data.error);
         throw new Error(data.message);
      }

      setResult(data);
    } catch (err) {
      setApiError(err?.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div class="page">
      <header class="header">
        <h1>Loan Analyzer</h1>
        <p class="subtitle">Fill the form and submit to <code>/api/v1/evaluate</code>.</p>
      </header>

      <main class="grid">
        <section class="card">
          <h2>Application</h2>

          <form onSubmit={onSubmit} novalidate>
            <div class="form">
              <For each={FIELD_SPEC.fields}>
                {(field) => {
                  const fieldErrors = () => errors()[field.key] || [];
                  const showErrors = () => touched()[field.key] && fieldErrors().length > 0;

                  return (
                    <div class="field">
                      <label for={field.key}>
                        {field.label}
                        <Show when={field.required}>
                          <span class="req"> *</span>
                        </Show>
                      </label>

                      <Show
                        when={field.type !== 'select'}
                        fallback={
                          <select
                            id={field.key}
                            name={field.key}
                            value={values()[field.key]}
                            onInput={(ev) => setField(field.key, ev.currentTarget.value)}
                            onBlur={() => markTouched(field.key)}
                          >
                            <option value="" disabled>
                              Select...
                            </option>
                            <For each={field.options ?? []}>
                              {(opt) => <option value={opt}>{opt}</option>}
                            </For>
                          </select>
                        }
                      >
                        <input
                          id={field.key}
                          name={field.key}
                          type={field.type === 'number' ? 'number' : 'text'}
                          inputMode={field.type === 'number' ? 'numeric' : undefined}
                          min={field.min}
                          max={field.max}
                          step={field.type === 'number' ? '0.01' : undefined}
                          onInput={(ev) => setField(field.key, ev.currentTarget.value)}
                          onBlur={() => markTouched(field.key)}
                        />
                      </Show>

                      <Show when={showErrors()}>
                        <ul class="errors" aria-live="polite">
                          <For each={fieldErrors()}>{(err) => <li>{err}</li>}</For>
                        </ul>
                      </Show>

                      <Show when={field.type === 'number' && (field.min != null || field.max != null)}>
                        <div class="hint">
                          <span>
                            Range:{' '}
                            {field.min != null ? field.min : '−∞'} – {field.max != null ? field.max : '+∞'}
                          </span>
                        </div>
                      </Show>
                    </div>
                  );
                }}
              </For>
            </div>

            <div class="actions">
              <button type="submit" disabled={submitting()}>
                <Show when={!submitting()} fallback={'Submitting...'}>
                  Submit
                </Show>
              </button>
              <span class="status">
                <Show when={!isValid()}>Fix validation errors to submit.</Show>
              </span>
            </div>
          </form>

          <Show when={apiError()}>
            <div class="alert alert-error" role="alert">
              <strong>Error:</strong> {apiError()}
            </div>
          </Show>
        </section>

        <section class="card">
          <h2>Result</h2>
          <Show
            when={result()}
            fallback={<p class="muted">Submit the form to see the backend response.</p>}
          >
            {(r) => (
              <>
                <Show when={r()?.["validation-errors"]}>
                  <div class="pill-error">Validation Errors</div>
                   <ul class="reasons">
                    <For each={Object.keys(r()?.["validation-errors"])}>
                      {(field) => (
                        <li>
                          <div class="reason-code">{field}</div>
                          <div class="reason-msg">{r()?.["validation-errors"]?.[field]}</div>
                        </li>
                      )}
                    </For>
                  </ul>
                </Show>

                <Show when={r().decision}>
                  <div class="pill">Decision: <strong>{String(r().decision)}</strong></div>
                </Show>

                <Show when={Array.isArray(r().reasons) && r().reasons.length > 0}>
                  <h3>Reasons</h3>
                  <ul class="reasons">
                    <For each={r().reasons}>
                      {(reason) => (
                        <li>
                          <div class="reason-code">{reason.code}</div>
                          <div class="reason-msg">{reason.message}</div>
                        </li>
                      )}
                    </For>
                  </ul>
                </Show>

                <details class="raw">
                  <summary>Raw JSON</summary>
                  <pre>{JSON.stringify(r(), null, 2)}</pre>
                </details>
              </>
            )}
          </Show>
        </section>
      </main>
    </div>
  );
}
