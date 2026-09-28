/* LoyaltyPoints frontend - talks to the Spring Boot REST API, which stores everything in MySQL. */
const API = '/api';

let customersCache = [];
let purchasesCache = [];
let tiersCache = [];
let toastTimer = null;

/* ------------------------------------------------------------ helpers */
async function api(path, options) {
    const opts = Object.assign({ headers: { 'Content-Type': 'application/json' } }, options || {});
    let res;
    try {
        res = await fetch(API + path, opts);
    } catch (e) {
        throw new Error('Cannot reach the server. Is the Spring Boot application running?');
    }
    if (res.status === 204) return null;
    let data = null;
    try { data = await res.json(); } catch (e) { /* no body */ }
    if (!res.ok) {
        let msg = (data && data.message) ? data.message : 'Request failed (' + res.status + ')';
        if (data && data.details) {
            const parts = Object.entries(data.details).map(function (kv) { return kv[0] + ': ' + kv[1]; });
            if (parts.length) msg += ' - ' + parts.join('; ');
        }
        throw new Error(msg);
    }
    return data;
}

function esc(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function money(n) { return '\u20B9' + Number(n).toFixed(2); }

function fmtDate(s) {
    if (!s) return '';
    const d = new Date(s);
    return isNaN(d.getTime()) ? esc(s) : d.toLocaleString();
}

function tierBadge(name) {
    const cls = String(name || '').toLowerCase();
    const known = (cls === 'silver' || cls === 'gold' || cls === 'platinum') ? cls : '';
    return '<span class="badge ' + known + '">' + esc(name) + '</span>';
}

function toast(message, type) {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.className = 'toast ' + (type || 'success');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.className = 'toast hidden'; }, 5000);
}

function showError(e) { toast(e.message || 'Something went wrong', 'error'); }

/* ------------------------------------------------------------ tabs */
document.querySelectorAll('.tab-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
        document.querySelectorAll('.tab-btn').forEach(function (b) { b.classList.remove('active'); });
        document.querySelectorAll('.tab').forEach(function (t) { t.classList.remove('active'); });
        btn.classList.add('active');
        document.getElementById(btn.dataset.tab).classList.add('active');
    });
});

/* ------------------------------------------------------------ settings */
async function loadSettings() {
    try {
        const s = await api('/settings');
        const perRupee = Number(s.pointsPerRupee);
        const rupeesPerPoint = perRupee > 0 ? (1 / perRupee) : 0;
        document.getElementById('settings-info').innerHTML =
            'Earn rate: <b>' + perRupee + '</b> pt per \u20B91 paid' +
            (rupeesPerPoint ? ' (1 pt per \u20B9' + Number(rupeesPerPoint.toFixed(2)) + ')' : '') +
            '<br>Redeem value: <b>1 pt = \u20B9' + Number(s.redemptionValuePerPoint) + '</b>';
    } catch (e) { showError(e); }
}

/* ------------------------------------------------------------ customers */
async function loadCustomers() {
    try {
        customersCache = await api('/customers');
        renderCustomers();
        fillCustomerSelects();
    } catch (e) { showError(e); }
}

function renderCustomers() {
    const body = document.getElementById('customers-body');
    if (!customersCache.length) {
        body.innerHTML = '<tr><td colspan="9" class="empty">No customers yet. Add one above.</td></tr>';
        return;
    }
    body.innerHTML = customersCache.map(function (c) {
        const next = c.nextTierName
            ? esc(c.nextTierName) + ' (' + c.pointsToNextTier + ' pts to go)'
            : 'Top tier';
        return '<tr>' +
            '<td>' + c.id + '</td>' +
            '<td>' + esc(c.name) + '</td>' +
            '<td>' + esc(c.email) + '</td>' +
            '<td>' + esc(c.phone) + '</td>' +
            '<td>' + tierBadge(c.tierName) + ' <small>' + Number(c.tierDiscountPercent) + '% off</small></td>' +
            '<td><b>' + c.pointsBalance + '</b></td>' +
            '<td>' + c.lifetimePoints + '</td>' +
            '<td>' + next + '</td>' +
            '<td><div class="cell-actions">' +
            '<button class="btn small" onclick="showHistory(' + c.id + ')">History</button>' +
            '<button class="btn small" onclick="editCustomer(' + c.id + ')">Edit</button>' +
            '<button class="btn small danger" onclick="deleteCustomer(' + c.id + ')">Delete</button>' +
            '</div></td></tr>';
    }).join('');
}

function fillCustomerSelects() {
    const sel = document.getElementById('purchase-customer');
    const filter = document.getElementById('purchase-filter');
    const selVal = sel.value;
    const filVal = filter.value;

    sel.innerHTML = customersCache.length
        ? '<option value="">-- select customer --</option>' + customersCache.map(function (c) {
            return '<option value="' + c.id + '">' + esc(c.name) + ' (' + esc(c.tierName) + ', ' + c.pointsBalance + ' pts)</option>';
        }).join('')
        : '<option value="">-- add a customer first --</option>';
    filter.innerHTML = '<option value="">All customers</option>' + customersCache.map(function (c) {
        return '<option value="' + c.id + '">' + esc(c.name) + '</option>';
    }).join('');

    if (selVal) sel.value = selVal;
    if (filVal) filter.value = filVal;
}

function resetCustomerForm() {
    document.getElementById('customer-form').reset();
    document.getElementById('customer-id').value = '';
    document.getElementById('customer-form-title').textContent = 'Add Customer';
}

function editCustomer(id) {
    const c = customersCache.find(function (x) { return x.id === id; });
    if (!c) return;
    document.getElementById('customer-id').value = c.id;
    document.getElementById('customer-name').value = c.name;
    document.getElementById('customer-email').value = c.email;
    document.getElementById('customer-phone').value = c.phone || '';
    document.getElementById('customer-form-title').textContent = 'Edit Customer #' + c.id;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function deleteCustomer(id) {
    const c = customersCache.find(function (x) { return x.id === id; });
    if (!c) return;
    if (!confirm('Delete customer "' + c.name + '"? Their purchases and points history will also be deleted.')) return;
    try {
        await api('/customers/' + id, { method: 'DELETE' });
        toast('Customer deleted');
        resetCustomerForm();
        await Promise.all([loadCustomers(), loadPurchases()]);
    } catch (e) { showError(e); }
}

document.getElementById('customer-form').addEventListener('submit', async function (ev) {
    ev.preventDefault();
    const id = document.getElementById('customer-id').value;
    const payload = {
        name: document.getElementById('customer-name').value.trim(),
        email: document.getElementById('customer-email').value.trim(),
        phone: document.getElementById('customer-phone').value.trim()
    };
    try {
        if (id) {
            await api('/customers/' + id, { method: 'PUT', body: JSON.stringify(payload) });
            toast('Customer updated');
        } else {
            await api('/customers', { method: 'POST', body: JSON.stringify(payload) });
            toast('Customer added');
        }
        resetCustomerForm();
        await loadCustomers();
    } catch (e) { showError(e); }
});
document.getElementById('customer-cancel').addEventListener('click', resetCustomerForm);

/* ------------------------------------------------------------ points history */
async function showHistory(id) {
    try {
        const results = await Promise.all([api('/customers/' + id), api('/customers/' + id + '/points-history')]);
        const c = results[0];
        const rows = results[1];
        document.getElementById('history-title').textContent = 'Points History - ' + c.name;
        document.getElementById('history-summary').innerHTML =
            'Current tier: ' + tierBadge(c.tierName) + ' &nbsp;|&nbsp; Available balance: <b>' + c.pointsBalance +
            '</b> pts &nbsp;|&nbsp; Lifetime points: <b>' + c.lifetimePoints + '</b>';
        document.getElementById('history-body').innerHTML = rows.length
            ? rows.map(function (r) {
                const cls = r.pointsChange >= 0 ? 'pos' : 'neg';
                const sign = r.pointsChange > 0 ? '+' : '';
                return '<tr><td>' + fmtDate(r.createdAt) + '</td><td>' + esc(r.entryType) + '</td>' +
                    '<td class="' + cls + '">' + sign + r.pointsChange + '</td><td>' + r.balanceAfter + '</td>' +
                    '<td>' + esc(r.description) + '</td></tr>';
            }).join('')
            : '<tr><td colspan="5" class="empty">No points activity yet.</td></tr>';
        document.getElementById('history-modal').classList.remove('hidden');
    } catch (e) { showError(e); }
}
document.getElementById('history-close').addEventListener('click', function () {
    document.getElementById('history-modal').classList.add('hidden');
});
document.getElementById('history-modal').addEventListener('click', function (ev) {
    if (ev.target === this) this.classList.add('hidden');
});

/* ------------------------------------------------------------ purchases */
async function loadPurchases() {
    try {
        const cid = document.getElementById('purchase-filter').value;
        purchasesCache = await api('/purchases' + (cid ? '?customerId=' + encodeURIComponent(cid) : ''));
        renderPurchases();
    } catch (e) { showError(e); }
}

function renderPurchases() {
    const body = document.getElementById('purchases-body');
    if (!purchasesCache.length) {
        body.innerHTML = '<tr><td colspan="11" class="empty">No purchases yet.</td></tr>';
        return;
    }
    body.innerHTML = purchasesCache.map(function (p) {
        const cancelled = p.status === 'CANCELLED';
        return '<tr>' +
            '<td>' + p.id + '</td>' +
            '<td>' + fmtDate(p.purchaseDate) + '</td>' +
            '<td>' + esc(p.customerName) + '</td>' +
            '<td>' + money(p.amount) + '</td>' +
            '<td>' + tierBadge(p.tierAtPurchase) + ' ' + Number(p.discountPercent) + '% (-' + money(p.discountAmount) + ')</td>' +
            '<td>' + p.pointsRedeemed + ' pts (-' + money(p.redemptionValue) + ')</td>' +
            '<td><b>' + money(p.finalAmount) + '</b></td>' +
            '<td class="pos">+' + p.pointsEarned + '</td>' +
            '<td><span class="badge ' + p.status.toLowerCase() + '">' + esc(p.status) + '</span></td>' +
            '<td>' + esc(p.description) + '</td>' +
            '<td><div class="cell-actions">' +
            '<button class="btn small" onclick="editPurchase(' + p.id + ')">Edit note</button>' +
            (cancelled ? '' : '<button class="btn small danger" onclick="cancelPurchase(' + p.id + ')">Cancel</button>') +
            '</div></td></tr>';
    }).join('');
}

function purchasePayload() {
    const customerId = document.getElementById('purchase-customer').value;
    const amount = document.getElementById('purchase-amount').value;
    const redeem = document.getElementById('purchase-redeem').value;
    return {
        customerId: customerId ? Number(customerId) : null,
        amount: amount === '' ? null : Number(amount),
        pointsToRedeem: redeem === '' ? 0 : Number(redeem),
        description: document.getElementById('purchase-description').value.trim()
    };
}

async function previewPurchase() {
    const box = document.getElementById('preview-box');
    const p = purchasePayload();
    if (!p.customerId || !p.amount) {
        box.className = 'preview error';
        box.textContent = 'Select a customer and enter an amount to preview.';
        return;
    }
    try {
        const r = await api('/purchases/preview', { method: 'POST', body: JSON.stringify(p) });
        box.className = 'preview';
        box.innerHTML = '<dl>' +
            '<dt>Purchase amount</dt><dd>' + money(r.amount) + '</dd>' +
            '<dt>Tier discount (' + esc(r.tierName) + ', ' + Number(r.discountPercent) + '%)</dt><dd>-' + money(r.discountAmount) + '</dd>' +
            '<dt>After discount</dt><dd>' + money(r.amountAfterDiscount) + '</dd>' +
            '<dt>Points redeemed (of ' + r.availableBalance + ' available)</dt><dd>' + r.pointsToRedeem + ' pts (-' + money(r.redemptionValue) + ')</dd>' +
            '<dt>Customer pays</dt><dd>' + money(r.finalAmount) + '</dd>' +
            '<dt>Points to be earned</dt><dd class="pos">+' + r.pointsToEarn + '</dd>' +
            '</dl>';
    } catch (e) {
        box.className = 'preview error';
        box.textContent = e.message;
    }
}
document.getElementById('purchase-preview-btn').addEventListener('click', previewPurchase);

document.getElementById('purchase-form').addEventListener('submit', async function (ev) {
    ev.preventDefault();
    const payload = purchasePayload();
    try {
        const r = await api('/purchases', { method: 'POST', body: JSON.stringify(payload) });
        let msg = 'Purchase recorded. Customer earned ' + r.pointsEarned + ' points.';
        if (r.upgradedToTier) msg += ' Tier upgraded to ' + r.upgradedToTier + '!';
        toast(msg);
        document.getElementById('purchase-amount').value = '';
        document.getElementById('purchase-redeem').value = '0';
        document.getElementById('purchase-description').value = '';
        document.getElementById('preview-box').className = 'preview hidden';
        await Promise.all([loadCustomers(), loadPurchases()]);
    } catch (e) { showError(e); }
});

async function editPurchase(id) {
    const p = purchasesCache.find(function (x) { return x.id === id; });
    if (!p) return;
    const text = prompt('Edit description for purchase #' + id + ':', p.description || '');
    if (text === null) return;
    try {
        await api('/purchases/' + id, { method: 'PUT', body: JSON.stringify({ description: text.trim() }) });
        toast('Purchase updated');
        await loadPurchases();
    } catch (e) { showError(e); }
}

async function cancelPurchase(id) {
    if (!confirm('Cancel purchase #' + id + '? Points earned will be removed and redeemed points refunded.')) return;
    try {
        await api('/purchases/' + id, { method: 'DELETE' });
        toast('Purchase cancelled and points reversed');
        await Promise.all([loadCustomers(), loadPurchases()]);
    } catch (e) { showError(e); }
}

document.getElementById('purchase-filter').addEventListener('change', loadPurchases);

/* ------------------------------------------------------------ tiers */
async function loadTiers() {
    try {
        tiersCache = await api('/tiers');
        renderTiers();
    } catch (e) { showError(e); }
}

function renderTiers() {
    const body = document.getElementById('tiers-body');
    if (!tiersCache.length) {
        body.innerHTML = '<tr><td colspan="5" class="empty">No tiers defined.</td></tr>';
        return;
    }
    body.innerHTML = tiersCache.map(function (t) {
        return '<tr>' +
            '<td>' + t.id + '</td>' +
            '<td>' + tierBadge(t.name) + '</td>' +
            '<td>' + t.minPoints + '</td>' +
            '<td>' + Number(t.discountPercent) + '%</td>' +
            '<td><div class="cell-actions">' +
            '<button class="btn small" onclick="editTier(' + t.id + ')">Edit</button>' +
            '<button class="btn small danger" onclick="deleteTier(' + t.id + ')">Delete</button>' +
            '</div></td></tr>';
    }).join('');
}

function resetTierForm() {
    document.getElementById('tier-form').reset();
    document.getElementById('tier-id').value = '';
    document.getElementById('tier-form-title').textContent = 'Add Tier';
}

function editTier(id) {
    const t = tiersCache.find(function (x) { return x.id === id; });
    if (!t) return;
    document.getElementById('tier-id').value = t.id;
    document.getElementById('tier-name').value = t.name;
    document.getElementById('tier-min').value = t.minPoints;
    document.getElementById('tier-discount').value = t.discountPercent;
    document.getElementById('tier-form-title').textContent = 'Edit Tier #' + t.id;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function deleteTier(id) {
    const t = tiersCache.find(function (x) { return x.id === id; });
    if (!t) return;
    if (!confirm('Delete tier "' + t.name + '"?')) return;
    try {
        await api('/tiers/' + id, { method: 'DELETE' });
        toast('Tier deleted');
        resetTierForm();
        await loadTiers();
    } catch (e) { showError(e); }
}

document.getElementById('tier-form').addEventListener('submit', async function (ev) {
    ev.preventDefault();
    const id = document.getElementById('tier-id').value;
    const payload = {
        name: document.getElementById('tier-name').value.trim(),
        minPoints: Number(document.getElementById('tier-min').value),
        discountPercent: Number(document.getElementById('tier-discount').value)
    };
    try {
        if (id) {
            await api('/tiers/' + id, { method: 'PUT', body: JSON.stringify(payload) });
            toast('Tier updated');
        } else {
            await api('/tiers', { method: 'POST', body: JSON.stringify(payload) });
            toast('Tier added');
        }
        resetTierForm();
        await Promise.all([loadTiers(), loadCustomers()]);
    } catch (e) { showError(e); }
});
document.getElementById('tier-cancel').addEventListener('click', resetTierForm);

/* ------------------------------------------------------------ init */
(async function init() {
    await loadSettings();
    await loadTiers();
    await loadCustomers();
    await loadPurchases();
})();
