const API_BASE = '';

function show(id) {
  document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
  document.getElementById(id).classList.remove('hidden');
  document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
  document.getElementById('nav-' + id.replace('-view', '')).classList.add('active');
}

function showNotification(message, type = 'success') {
  const notif = document.getElementById('notification');
  notif.textContent = message;
  notif.className = `notification ${type}`;
  setTimeout(() => {
    notif.classList.add('hidden');
  }, 3000);
}

async function fetchJson(url, options = {}) {
  try {
    const res = await fetch(API_BASE + url, {
      headers: { 'Content-Type': 'application/json', ...options.headers },
      ...options,
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || res.statusText);
    }
    return res.json();
  } catch (e) {
    const msg = e.message || 'Network error';
    showNotification(msg, 'error');
    if (msg.includes('Failed to fetch') || msg.includes('Network error')) {
      if (confirm('Network error occurred. Retry?')) {
        return fetchJson(url, options);
      }
    }
    throw e;
  }
}

async function loadRules() {
  const rules = await fetchJson('/rules');
  const list = document.getElementById('rules-list');
  list.innerHTML = '';
  rules.forEach(r => {
    const li = document.createElement('li');
    li.innerHTML = `<strong>${r.name}</strong> (${r.severity})<br><pre>${r.dsl}</pre>`;
    list.appendChild(li);
  });
}

async function loadTeaLots() {
  const lots = await fetchJson('/tea-lots');
  const list = document.getElementById('tea-lots-list');
  list.innerHTML = '';
  lots.forEach(l => {
    const li = document.createElement('li');
    li.innerHTML = `<strong>${l.lotCode}</strong> – ${l.origin} / ${l.variety}<br>Moisture: ${l.moisture}, Pesticide: ${l.pesticideLevel}, Aroma: ${l.aromaScore}`;
    list.appendChild(li);
  });
  // populate simulation select
  const sel = document.getElementById('sim-tea-lot-select');
  sel.innerHTML = '<option value="">-- Select Tea Lot --</option>';
  lots.forEach(l => {
    const opt = document.createElement('option');
    opt.value = l.id;
    opt.textContent = `${l.lotCode} (${l.origin})`;
    sel.appendChild(opt);
  });
}

// Rules
document.getElementById('nav-rules').addEventListener('click', () => {
  show('rules-view');
  loadRules();
});
document.getElementById('rule-form').addEventListener('submit', async e => {
  e.preventDefault();
  const payload = {
    name: document.getElementById('rule-name').value,
    severity: document.getElementById('rule-severity').value,
    dsl: document.getElementById('rule-dsl').value,
  };
  await fetchJson('/rules', { method: 'POST', body: JSON.stringify(payload) });
  e.target.reset();
  loadRules();
  showNotification('Rule added successfully');
});
// DSL sample buttons
document.getElementById('sample-moisture').addEventListener('click', () => {
  document.getElementById('rule-name').value = 'Moisture Check';
  document.getElementById('rule-severity').value = 'BLOCK';
  document.getElementById('rule-dsl').value = 'rule("Moisture Check") { whenMoisture { it > 9.0 } then BLOCK }';
});
document.getElementById('sample-pesticide').addEventListener('click', () => {
  document.getElementById('rule-name').value = 'Pesticide Check';
  document.getElementById('rule-severity').value = 'WARNING';
  document.getElementById('rule-dsl').value = 'rule("Pesticide Check") { whenPesticideLevel { it > 0.15 } then WARNING }';
});
document.getElementById('sample-aroma').addEventListener('click', () => {
  document.getElementById('rule-name').value = 'Aroma Check';
  document.getElementById('rule-severity').value = 'INFO';
  document.getElementById('rule-dsl').value = 'rule("Aroma Check") { whenAromaScore { it < 70 } then INFO }';
});

// Tea Lots
document.getElementById('nav-tea-lots').addEventListener('click', () => {
  show('tea-lots-view');
  loadTeaLots();
});
document.getElementById('tea-lot-form').addEventListener('submit', async e => {
  e.preventDefault();
  const errorEl = document.getElementById('tea-lot-error');
  errorEl.textContent = '';
  
  const moisture = parseFloat(document.getElementById('moisture').value);
  const pesticideLevel = parseFloat(document.getElementById('pesticide-level').value);
  const aromaScore = parseInt(document.getElementById('aroma-score').value, 10);
  
  if (moisture < 0 || moisture > 20) {
    errorEl.textContent = 'Moisture must be between 0 and 20';
    return;
  }
  if (pesticideLevel < 0 || pesticideLevel > 1) {
    errorEl.textContent = 'Pesticide level must be between 0 and 1';
    return;
  }
  if (aromaScore < 0 || aromaScore > 100) {
    errorEl.textContent = 'Aroma score must be between 0 and 100';
    return;
  }
  
  const payload = {
    lotCode: document.getElementById('lot-code').value,
    origin: document.getElementById('origin').value,
    variety: document.getElementById('variety').value,
    moisture,
    pesticideLevel,
    aromaScore,
  };
  await fetchJson('/tea-lots', { method: 'POST', body: JSON.stringify(payload) });
  e.target.reset();
  loadTeaLots();
  showNotification('Tea Lot added successfully');
});

// Simulation
document.getElementById('nav-simulation').addEventListener('click', () => {
  show('simulation-view');
  loadTeaLots(); // ensure select is populated
});
document.getElementById('run-simulation').addEventListener('click', async () => {
  const teaLotId = document.getElementById('sim-tea-lot-select').value;
  if (!teaLotId) return;
  const result = await fetchJson(`/simulate/${teaLotId}`, { method: 'POST' });
  const resultEl = document.getElementById('sim-result');
  
  let html = `<strong>TeaLot ID: ${result.teaLotId}</strong><br>`;
  html += `<strong>Shippable: <span class="${result.shippable ? 'pass' : 'fail'}">${result.shippable ? 'YES' : 'NO'}</span></strong><br><br>`;
  html += '<strong>Rule Results:</strong><br>';
  
  result.results.forEach(r => {
    const cssClass = r.result === 'PASS' ? 'pass' : 'fail';
    html += `<div class="rule-result">
      <span class="${cssClass}">${r.result}</span> 
      <span class="severity-${r.severity.toLowerCase()}">[${r.severity}]</span> 
      ${r.message}
    </div>`;
  });
  
  resultEl.innerHTML = html;
});

// Initial view
show('rules-view');
