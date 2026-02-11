const API_BASE = '';

function show(id) {
  document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
  document.getElementById(id).classList.remove('hidden');
  document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
  document.getElementById('nav-' + id.replace('-view', '')).classList.add('active');
}

async function fetchJson(url, options = {}) {
  const res = await fetch(API_BASE + url, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) throw new Error(res.statusText);
  return res.json();
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
});

// Tea Lots
document.getElementById('nav-tea-lots').addEventListener('click', () => {
  show('tea-lots-view');
  loadTeaLots();
});
document.getElementById('tea-lot-form').addEventListener('submit', async e => {
  e.preventDefault();
  const payload = {
    lotCode: document.getElementById('lot-code').value,
    origin: document.getElementById('origin').value,
    variety: document.getElementById('variety').value,
    moisture: parseFloat(document.getElementById('moisture').value),
    pesticideLevel: parseFloat(document.getElementById('pesticide-level').value),
    aromaScore: parseInt(document.getElementById('aroma-score').value, 10),
  };
  await fetchJson('/tea-lots', { method: 'POST', body: JSON.stringify(payload) });
  e.target.reset();
  loadTeaLots();
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
  document.getElementById('sim-result').textContent = JSON.stringify(result, null, 2);
});

// Initial view
show('rules-view');
