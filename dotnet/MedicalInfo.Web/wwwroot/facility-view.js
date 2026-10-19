'use strict';
(async () => {
  const id = new URLSearchParams(location.search).get('facilityId');
  if (!id) return;
  try {
    const response = await fetch('/api/account/csrf', {cache:'no-store'});
    if (!response.ok) return;
    const {token} = await response.json();
    await fetch('/api/facilities/' + encodeURIComponent(id) + '/view', {method:'POST', headers:{'X-CSRF-TOKEN':token}});
  } catch { /* Optional aggregate statistics must not prevent reading reviews. */ }
})();
