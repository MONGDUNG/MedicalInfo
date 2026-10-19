'use strict';
(async () => {
  const list = document.querySelector('#popular-items');
  const status = document.querySelector('#popular-status');
  try {
    const response = await fetch('/api/facilities/popular', {cache:'no-store'});
    if (!response.ok) throw new Error();
    const items = await response.json();
    for (const item of items) {
      const row = document.createElement('li');
      const link = document.createElement('a');
      link.href = '/reviews.html?' + new URLSearchParams({facilityId:item.facility.ID});
      link.textContent = item.facility.NAME + ' · ' + item.facility.ADDRESS;
      row.append(link, document.createTextNode(' · 조회 ' + item.views));
      list.append(row);
    }
    status.textContent = items.length ? '상세 페이지 누적 조회 기준이며 의료의 질이나 평점을 뜻하지 않습니다.' : '아직 집계된 조회가 없습니다.';
  } catch { status.textContent = '조회 목록을 불러오지 못했습니다. 병원 검색은 계속 이용할 수 있습니다.'; }
})();
