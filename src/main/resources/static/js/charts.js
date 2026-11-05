// drawBars(element, labels, values, formatter): a simple bar chart made of divs, no library needed.
function drawBars(el, labels, values, fmt) {
    if (!el) { return; }
    var max = 0;
    values.forEach(function (v) { if (Number(v) > max) { max = Number(v); } });
    if (labels.length > 14) { el.classList.add('dense'); }
    el.innerHTML = '';
    labels.forEach(function (label, i) {
        var v = Number(values[i]) || 0;
        var col = document.createElement('div');
        col.className = 'col';
        col.title = label + ': ' + (fmt ? fmt(v) : v);
        var bar = document.createElement('div');
        bar.className = 'bar';
        bar.style.height = (max > 0 ? Math.round(v / max * 100) : 0) + '%';
        var lbl = document.createElement('div');
        lbl.className = 'lbl';
        lbl.textContent = label;
        col.appendChild(bar);
        col.appendChild(lbl);
        el.appendChild(col);
    });
}

// Tabs: <div class="tabs-container" data-tabs> <button class="tab" data-target="x"> ... <div class="tab-panel" id="x">
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-tabs]').forEach(function (group) {
        var tabs = group.querySelectorAll('.tab');
        function show(id) {
            tabs.forEach(function (t) { t.classList.toggle('active', t.dataset.target === id); });
            document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.toggle('active', p.id === id); });
        }
        tabs.forEach(function (t) { t.addEventListener('click', function () { show(t.dataset.target); }); });
        if (tabs.length) { show(tabs[0].dataset.target); }
    });
});
