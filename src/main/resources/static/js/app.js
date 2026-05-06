/* ============================================================
   MedStore Order Module — Main JS
   ============================================================ */

'use strict';

/* ── Basket state ────────────────────────────────────────────── */
let basket = [];

/* Sample medicines catalogue (in real app loaded from API) */
const catalogue = [
  { id: 'MED001', name: 'Paracetamol 500mg',    price: 45.00 },
  { id: 'MED002', name: 'Amoxicillin 250mg',    price: 120.00 },
  { id: 'MED003', name: 'Ibuprofen 400mg',       price: 65.00 },
  { id: 'MED004', name: 'Omeprazole 20mg',       price: 95.00 },
  { id: 'MED005', name: 'Cetirizine 10mg',       price: 35.00 },
  { id: 'MED006', name: 'Metformin 500mg',       price: 78.00 },
  { id: 'MED007', name: 'Atorvastatin 10mg',     price: 155.00 },
  { id: 'MED008', name: 'Azithromycin Syrup',    price: 280.00 },
  { id: 'MED009', name: 'Insulin (Injection)',   price: 1200.00 },
  { id: 'MED010', name: 'Vitamin C 1000mg',      price: 55.00 },
];

/* ── DOMContentLoaded ────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  initCatalogue();
  initPaymentTabs();
  initFlashDismiss();
  initStatusHighlight();
});

/* ── Catalogue / Medicine Picker ────────────────────────────── */
function initCatalogue() {
  const grid    = document.getElementById('medicine-catalogue');
  const search  = document.getElementById('med-search');
  if (!grid) return;

  renderCatalogue(catalogue);

  search?.addEventListener('input', e => {
    const q = e.target.value.toLowerCase();
    renderCatalogue(catalogue.filter(m => m.name.toLowerCase().includes(q) || m.id.toLowerCase().includes(q)));
  });
}

function renderCatalogue(items) {
  const grid = document.getElementById('medicine-catalogue');
  if (!items.length) {
    grid.innerHTML = '<div class="cat-empty">No medicines found</div>';
    return;
  }
  grid.innerHTML = items.map(m => `
    <div class="cat-item" data-id="${m.id}">
      <div class="cat-name">${m.name}</div>
      <div class="cat-id">${m.id}</div>
      <div class="cat-price">Rs. ${m.price.toFixed(2)}</div>
      <div class="cat-controls">
        <input type="number" class="cat-qty" min="1" max="99" value="1" id="qty-${m.id}">
        <button class="btn btn-primary btn-sm" onclick="addToBasket('${m.id}')">Add</button>
      </div>
    </div>
  `).join('');
}

function addToBasket(medicineId) {
  const med = catalogue.find(m => m.id === medicineId);
  if (!med) return;

  const qtyInput = document.getElementById(`qty-${medicineId}`);
  const qty      = parseInt(qtyInput?.value || '1', 10);
  if (isNaN(qty) || qty < 1) return;

  const existing = basket.find(b => b.medicineId === medicineId);
  if (existing) {
    existing.quantity += qty;
  } else {
    basket.push({ medicineId: med.id, medicineName: med.name, quantity: qty, unitPrice: med.price });
  }

  renderBasket();
  pulseBasket();
}

function removeFromBasket(medicineId) {
  basket = basket.filter(b => b.medicineId !== medicineId);
  renderBasket();
}

function renderBasket() {
  const wrap  = document.getElementById('basket-wrap');
  const total = document.getElementById('basket-total-value');
  const input = document.getElementById('itemsRaw');
  if (!wrap) return;

  if (!basket.length) {
    wrap.innerHTML = '<div class="basket-empty">No items yet — add medicines from the catalogue above</div>';
    if (total) total.textContent = 'Rs. 0.00';
    if (input) input.value = '';
    return;
  }

  const rows = basket.map(b => `
    <div class="basket-item">
      <div class="basket-item-name">${b.medicineName}</div>
      <div class="basket-item-qty">× ${b.quantity}</div>
      <div class="basket-item-price">Rs. ${(b.unitPrice * b.quantity).toFixed(2)}</div>
      <button class="btn btn-ghost btn-sm" onclick="removeFromBasket('${b.medicineId}')" title="Remove">✕</button>
    </div>
  `).join('');

  const grandTotal = basket.reduce((s, b) => s + b.unitPrice * b.quantity, 0);

  wrap.innerHTML = rows;
  if (total) total.textContent = 'Rs. ' + grandTotal.toFixed(2);

  // Serialise for hidden form field: "id|name|qty|price,..."
  if (input) {
    input.value = basket.map(b => `${b.medicineId}|${b.medicineName}|${b.quantity}|${b.unitPrice}`).join(',');
  }
}

function pulseBasket() {
  const total = document.getElementById('basket-total-value');
  if (!total) return;
  total.classList.add('pulse');
  setTimeout(() => total.classList.remove('pulse'), 600);
}

/* ── Payment Tabs ────────────────────────────────────────────── */
function initPaymentTabs() {
  const tabs = document.querySelectorAll('.pay-tab');
  if (!tabs.length) return;

  tabs.forEach(tab => {
    tab.addEventListener('click', () => selectPaymentTab(tab.dataset.method));
  });

  // Activate first tab
  selectPaymentTab(tabs[0]?.dataset.method);
}

function selectPaymentTab(method) {
  if (!method) return;

  document.querySelectorAll('.pay-tab').forEach(t => {
    t.classList.toggle('selected', t.dataset.method === method);
  });

  document.querySelectorAll('.payment-panel').forEach(p => {
    p.classList.toggle('active', p.id === `panel-${method}`);
  });

  // Update hidden select so Spring picks it up
  const sel = document.getElementById('paymentMethodSelect');
  if (sel) sel.value = method;
}

/* ── Flash dismiss ───────────────────────────────────────────── */
function initFlashDismiss() {
  document.querySelectorAll('.alert').forEach(el => {
    setTimeout(() => el.style.opacity = '0', 5000);
    setTimeout(() => el.remove(), 5500);
  });
}

/* ── Status row highlight ────────────────────────────────────── */
function initStatusHighlight() {
  document.querySelectorAll('tbody tr[data-status]').forEach(row => {
    row.style.cursor = 'pointer';
    row.addEventListener('click', e => {
      if (e.target.closest('a, button, form')) return;
      const link = row.querySelector('a[data-detail]');
      if (link) window.location.href = link.href;
    });
  });
}

/* ── Status update confirm ───────────────────────────────────── */
function confirmStatusUpdate(form) {
  const sel = form.querySelector('select[name="newStatus"]');
  const val = sel?.options[sel.selectedIndex]?.text || 'new status';
  return confirm(`Update order status to "${val}"?`);
}

function confirmCancel() {
  return confirm('Are you sure you want to cancel this order? This cannot be undone for shipped/delivered orders.');
}

function confirmDelete() {
  return confirm('Permanently delete this order record? This action cannot be undone.');
}
