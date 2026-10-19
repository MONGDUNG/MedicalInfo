'use strict';
(() => {
  document.documentElement.classList.add('ui-ready');
  const menu = document.querySelector('#site-nav');
  const toggle = document.querySelector('#menu-toggle');
  if (menu && toggle) {
    toggle.addEventListener('click', () => {
      const expanded = toggle.getAttribute('aria-expanded') !== 'true';
      toggle.setAttribute('aria-expanded', String(expanded));
      menu.classList.toggle('is-open', expanded);
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
        menu.classList.remove('is-open'); toggle.setAttribute('aria-expanded', 'false'); toggle.focus();
      }
    });
  }
  const tabs = [...document.querySelectorAll('[data-auth-tab]')];
  const panels = [...document.querySelectorAll('[data-auth-panel]')];
  if (tabs.length && panels.length) {
    const select = (name, moveFocus = false) => {
      tabs.forEach(tab => {
        const selected = tab.dataset.authTab === name;
        tab.setAttribute('aria-selected', String(selected)); tab.tabIndex = selected ? 0 : -1;
        if (selected && moveFocus) tab.focus();
      });
      panels.forEach(panel => { panel.hidden = panel.dataset.authPanel !== name; });
    };
    document.querySelector('.auth-tabs').hidden = false;
    tabs.forEach((tab, index) => {
      tab.addEventListener('click', () => select(tab.dataset.authTab));
      tab.addEventListener('keydown', event => {
        let target;
        if (event.key === 'ArrowRight') target = (index + 1) % tabs.length;
        if (event.key === 'ArrowLeft') target = (index + tabs.length - 1) % tabs.length;
        if (event.key === 'Home') target = 0;
        if (event.key === 'End') target = tabs.length - 1;
        if (target !== undefined) { event.preventDefault(); select(tabs[target].dataset.authTab, true); }
      });
    });
    select(location.hash === '#register' ? 'register' : 'login');
    window.addEventListener('hashchange', () => select(location.hash === '#register' ? 'register' : 'login'));
  }
})();
