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

function showLoading(text = 'Loading...') {
  const loading = document.getElementById('loading');
  const loadingText = loading.querySelector('.loading-text');
  loadingText.textContent = text;
  loading.classList.remove('hidden');
}

function hideLoading() {
  document.getElementById('loading').classList.add('hidden');
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

let allRules = [];
let allTeaLots = [];

async function loadRules() {
  showLoading('Loading rules...');
  allRules = await fetchJson('/rules');
  renderRules();
  hideLoading();
}

function renderRules() {
  const list = document.getElementById('rules-list');
  const searchTerm = document.getElementById('rule-search').value.toLowerCase();
  const severityFilter = document.getElementById('rule-severity-filter').value;
  
  const filtered = allRules.filter(r => {
    const matchesSearch = r.name.toLowerCase().includes(searchTerm) || 
                         r.dsl.toLowerCase().includes(searchTerm);
    const matchesSeverity = !severityFilter || r.severity === severityFilter;
    return matchesSearch && matchesSeverity;
  });
  
  list.innerHTML = '';
  filtered.forEach(r => {
    const li = document.createElement('li');
    li.className = 'rule-item';
    li.innerHTML = `
      <input type="checkbox" class="rule-checkbox" value="${r.id}">
      <div>
        <strong>${r.name}</strong> (${r.severity})<br>
        <pre>${r.dsl}</pre>
      </div>
    `;
    list.appendChild(li);
  });
}

async function loadTeaLots() {
  showLoading('Loading tea lots...');
  allTeaLots = await fetchJson('/tea-lots');
  renderTeaLots();
  populateSimulationSelect();
  hideLoading();
}

function renderTeaLots() {
  const list = document.getElementById('tea-lots-list');
  const searchTerm = document.getElementById('tea-lot-search').value.toLowerCase();
  const originFilter = document.getElementById('tea-lot-origin-filter').value;
  const varietyFilter = document.getElementById('tea-lot-variety-filter').value;
  
  const filtered = allTeaLots.filter(l => {
    const matchesSearch = l.lotCode.toLowerCase().includes(searchTerm) || 
                         l.origin.toLowerCase().includes(searchTerm) ||
                         l.variety.toLowerCase().includes(searchTerm);
    const matchesOrigin = !originFilter || l.origin === originFilter;
    const matchesVariety = !varietyFilter || l.variety === varietyFilter;
    return matchesSearch && matchesOrigin && matchesVariety;
  });
  
  list.innerHTML = '';
  filtered.forEach(l => {
    const li = document.createElement('li');
    li.className = 'tea-lot-item';
    li.innerHTML = `
      <input type="checkbox" class="tea-lot-checkbox" value="${l.id}">
      <div>
        <strong>${l.lotCode}</strong> – ${l.origin} / ${l.variety}<br>
        Moisture: ${l.moisture}, Pesticide: ${l.pesticideLevel}, Aroma: ${l.aromaScore}
      </div>
    `;
    list.appendChild(li);
  });
}

function populateSimulationSelect() {
  const sel = document.getElementById('sim-tea-lot-select');
  sel.innerHTML = '';
  allTeaLots.forEach(l => {
    const opt = document.createElement('option');
    opt.value = l.id;
    opt.textContent = `${l.lotCode} (${l.origin})`;
    sel.appendChild(opt);
  });
  
  // Update filter options
  const originFilter = document.getElementById('tea-lot-origin-filter');
  const varietyFilter = document.getElementById('tea-lot-variety-filter');
  
  const origins = [...new Set(allTeaLots.map(l => l.origin))].sort();
  const varieties = [...new Set(allTeaLots.map(l => l.variety))].sort();
  
  originFilter.innerHTML = '<option value="">All Origins</option>';
  origins.forEach(origin => {
    const opt = document.createElement('option');
    opt.value = origin;
    opt.textContent = origin;
    originFilter.appendChild(opt);
  });
  
  varietyFilter.innerHTML = '<option value="">All Varieties</option>';
  varieties.forEach(variety => {
    const opt = document.createElement('option');
    opt.value = variety;
    opt.textContent = variety;
    varietyFilter.appendChild(opt);
  });
}

// Rules
document.getElementById('nav-rules').addEventListener('click', () => {
  show('rules-view');
  loadRules();
});

// Search and filter for rules
document.getElementById('rule-search').addEventListener('input', renderRules);
document.getElementById('rule-severity-filter').addEventListener('change', renderRules);
document.getElementById('rule-form').addEventListener('submit', async e => {
  e.preventDefault();
  showLoading('Adding rule...');
  const payload = {
    name: document.getElementById('rule-name').value,
    severity: document.getElementById('rule-severity').value,
    dsl: document.getElementById('rule-dsl').value,
  };
  await fetchJson('/rules', { method: 'POST', body: JSON.stringify(payload) });
  e.target.reset();
  loadRules();
  showNotification('Rule added successfully');
  hideLoading();
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

// Bulk actions for rules
document.getElementById('select-all-rules').addEventListener('click', () => {
  const checkboxes = document.querySelectorAll('.rule-checkbox');
  const allChecked = Array.from(checkboxes).every(cb => cb.checked);
  checkboxes.forEach(cb => cb.checked = !allChecked);
});

document.getElementById('delete-selected-rules').addEventListener('click', async () => {
  const selected = Array.from(document.querySelectorAll('.rule-checkbox:checked')).map(cb => cb.value);
  if (selected.length === 0) {
    showNotification('No rules selected', 'error');
    return;
  }
  if (!confirm(`Delete ${selected.length} rule(s)?`)) return;
  
  showLoading('Deleting rules...');
  await fetchJson('/rules', { method: 'DELETE', body: JSON.stringify(selected.map(Number)) });
  loadRules();
  showNotification(`Deleted ${selected.length} rule(s)`);
  hideLoading();
});

// Tea Lots
document.getElementById('nav-tea-lots').addEventListener('click', () => {
  show('tea-lots-view');
  loadTeaLots();
});

// Search and filter for tea lots
document.getElementById('tea-lot-search').addEventListener('input', renderTeaLots);
document.getElementById('tea-lot-origin-filter').addEventListener('change', renderTeaLots);
document.getElementById('tea-lot-variety-filter').addEventListener('change', renderTeaLots);
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
  
  showLoading('Adding tea lot...');
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
  hideLoading();
});

// Bulk actions for tea lots
document.getElementById('select-all-tea-lots').addEventListener('click', () => {
  const checkboxes = document.querySelectorAll('.tea-lot-checkbox');
  const allChecked = Array.from(checkboxes).every(cb => cb.checked);
  checkboxes.forEach(cb => cb.checked = !allChecked);
});

document.getElementById('delete-selected-tea-lots').addEventListener('click', async () => {
  const selected = Array.from(document.querySelectorAll('.tea-lot-checkbox:checked')).map(cb => cb.value);
  if (selected.length === 0) {
    showNotification('No tea lots selected', 'error');
    return;
  }
  if (!confirm(`Delete ${selected.length} tea lot(s)?`)) return;
  
  showLoading('Deleting tea lots...');
  await fetchJson('/tea-lots', { method: 'DELETE', body: JSON.stringify(selected.map(Number)) });
  loadTeaLots();
  showNotification(`Deleted ${selected.length} tea lot(s)`);
  hideLoading();
});

// Simulation
document.getElementById('nav-simulation').addEventListener('click', () => {
  show('simulation-view');
  loadTeaLots(); // ensure select is populated
});
document.getElementById('run-simulation').addEventListener('click', async () => {
  const sel = document.getElementById('sim-tea-lot-select');
  const selectedOptions = Array.from(sel.selectedOptions);
  if (selectedOptions.length === 0) {
    showNotification('Please select tea lots', 'error');
    return;
  }
  if (selectedOptions.length === 1) {
    // Single simulation
    const teaLotId = selectedOptions[0].value;
    showLoading('Running simulation...');
    const result = await fetchJson(`/simulate/${teaLotId}`, { method: 'POST' });
    displaySimulationResult([result]);
    hideLoading();
  } else {
    // Bulk simulation
    const teaLotIds = selectedOptions.map(opt => opt.value);
    showLoading('Running bulk simulation...');
    const bulkResult = await fetchJson('/simulate', { method: 'POST', body: JSON.stringify({ teaLotIds: teaLotIds.map(Number) }) });
    displaySimulationResult(bulkResult.results);
    hideLoading();
  }
});

document.getElementById('run-bulk-simulation').addEventListener('click', async () => {
  const sel = document.getElementById('sim-tea-lot-select');
  const allOptions = Array.from(sel.options);
  if (allOptions.length === 0) {
    showNotification('No tea lots available', 'error');
    return;
  }
  const teaLotIds = allOptions.map(opt => opt.value);
  showLoading('Running bulk simulation for all tea lots...');
  const bulkResult = await fetchJson('/simulate', { method: 'POST', body: JSON.stringify({ teaLotIds: teaLotIds.map(Number) }) });
  displaySimulationResult(bulkResult.results);
  hideLoading();
});

document.getElementById('sim-select-all').addEventListener('change', (e) => {
  const sel = document.getElementById('sim-tea-lot-select');
  Array.from(sel.options).forEach(opt => opt.selected = e.target.checked);
});

function displaySimulationResult(results) {
  const resultEl = document.getElementById('sim-result');
  let html = '';
  
  results.forEach((result, index) => {
    html += `<div class="simulation-result-item">
      <h4>TeaLot ID: ${result.teaLotId}</h4>
      <p><strong>Shippable: <span class="${result.shippable ? 'pass' : 'fail'}">${result.shippable ? 'YES' : 'NO'}</span></strong></p>
      <div class="rule-results">
        ${result.results.map(r => `
          <div class="rule-result">
            <span class="${r.result === 'PASS' ? 'pass' : 'fail'}">${r.result}</span> 
            <span class="severity-${r.severity.toLowerCase()}">[${r.severity}]</span> 
            ${r.message}
          </div>
        `).join('')}
      </div>
    </div>`;
  });
  
  resultEl.innerHTML = html;
}

// Data export/import
document.getElementById('export-data').addEventListener('click', async () => {
  showLoading('Exporting data...');
  
  try {
    // Export rules
    const rulesBlob = await fetchBlob('/export/rules');
    downloadBlob(rulesBlob, 'rules.json');
    
    // Export tea lots
    const teaLotsBlob = await fetchBlob('/export/tea-lots');
    downloadBlob(teaLotsBlob, 'tea-lots.json');
    
    showNotification('Data exported successfully');
  } catch (error) {
    showNotification('Export failed: ' + error.message, 'error');
  } finally {
    hideLoading();
  }
});

function fetchBlob(url) {
  return fetch(url).then(response => {
    if (!response.ok) throw new Error('Network response was not ok');
    return response.blob();
  });
}

document.getElementById('import-data').addEventListener('click', () => {
  document.getElementById('import-file').click();
});

document.getElementById('import-file').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  
  if (!file.name.endsWith('.json')) {
    showNotification('Please select a JSON file', 'error');
    return;
  }
  
  showLoading('Importing data...');
  
  try {
    const text = await file.text();
    const data = JSON.parse(text);
    
    if (data.length > 0) {
      // Determine data type based on structure
      if (data[0].name && data[0].dsl && data[0].severity) {
        // Rules data
        await importRules(data);
      } else if (data[0].lotCode && data[0].origin && data[0].variety) {
        // Tea lots data
        await importTeaLots(data);
      } else {
        throw new Error('Unknown data format');
      }
    } else {
      showNotification('No data to import', 'error');
    }
  } catch (error) {
    showNotification('Import failed: ' + error.message, 'error');
  } finally {
    hideLoading();
    e.target.value = ''; // Reset file input
  }
});

async function importRules(rules) {
  const importData = rules.map(rule => ({
    name: rule.name,
    dsl: rule.dsl,
    severity: rule.severity
  }));
  
  const result = await fetchJson('/import/rules', { 
    method: 'POST', 
    body: JSON.stringify({ rules: importData }) 
  });
  
  showNotification(`Imported ${result.imported} rules successfully`);
  loadRules(); // Refresh rules list
}

async function importTeaLots(teaLots) {
  const importData = teaLots.map(teaLot => ({
    lotCode: teaLot.lotCode,
    origin: teaLot.origin,
    variety: teaLot.variety,
    moisture: teaLot.moisture,
    pesticideLevel: teaLot.pesticideLevel,
    aromaScore: teaLot.aromaScore
  }));
  
  const result = await fetchJson('/import/tea-lots', { 
    method: 'POST', 
    body: JSON.stringify({ teaLots: importData }) 
  });
  
  showNotification(`Imported ${result.imported} tea lots successfully`);
  loadTeaLots(); // Refresh tea lots list
}

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
  // Ctrl/Cmd + N: New Rule
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    show('rules-view');
    document.getElementById('rule-name').focus();
  }
  // Ctrl/Cmd + Shift + N: New Tea Lot
  if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'N') {
    e.preventDefault();
    show('tea-lots-view');
    document.getElementById('lot-code').focus();
  }
  // Ctrl/Cmd + S: Run Simulation
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault();
    show('simulation-view');
    loadTeaLots();
  }
  // Ctrl/Cmd + /: Focus search
  if ((e.ctrlKey || e.metaKey) && e.key === '/') {
    e.preventDefault();
    const currentView = document.querySelector('.view:not(.hidden)').id;
    if (currentView === 'rules-view') {
      document.getElementById('rule-search').focus();
    } else if (currentView === 'tea-lots-view') {
      document.getElementById('tea-lot-search').focus();
    }
  }
  // Escape: Clear search/focus
  if (e.key === 'Escape') {
    const activeElement = document.activeElement;
    if (activeElement && activeElement.id.includes('search')) {
      activeElement.value = '';
      activeElement.dispatchEvent(new Event('input'));
      activeElement.blur();
    }
  }
});
