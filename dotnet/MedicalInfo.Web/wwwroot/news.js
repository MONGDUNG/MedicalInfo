'use strict';
(async () => {
  const status = document.querySelector('#news-status');
  try {
    const response = await fetch('/api/news');
    if (!response.ok) throw new Error();
    const data = await response.json();
    for (const [index, item] of data.items.entries()) {
      const row = document.createElement('li');
      row.hidden = index >= 5;
      const link = document.createElement('a');
      link.href = item.link;
      link.textContent = item.title;
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
      row.append(link);
      if (item.publishedAt) {
        const time = document.createElement('time');
        time.dateTime = item.publishedAt;
        time.textContent = new Date(item.publishedAt).toLocaleDateString('ko-KR', {timeZone: 'Asia/Seoul', month:'2-digit', day:'2-digit'});
        row.append(time);
      }
      document.querySelector('#news-items').append(row);
    }
    status.textContent = (data.stale ? '갱신이 지연되어 마지막으로 받은 소식을 표시합니다. ' : '') +
      '수집: ' + new Date(data.fetchedAt).toLocaleString('ko-KR');
    const more = document.querySelector('#news-more');
    if (more && data.items.length > 5) {
      more.hidden = false;
      more.addEventListener('click', () => {
        const expanded = more.getAttribute('aria-expanded') !== 'true';
        more.setAttribute('aria-expanded', String(expanded));
        more.textContent = expanded ? '소식 접기' : '소식 더 보기';
        document.querySelectorAll('#news-items li').forEach((row, index) => { row.hidden = !expanded && index >= 5; });
      });
    }
    if (!data.items.length) status.textContent = '표시할 소식이 없습니다. 보건복지부 전체 소식에서 확인해주세요.';
  } catch {
    status.textContent = '소식을 불러오지 못했습니다. 아래 보건복지부 링크에서 확인해주세요.';
  }
})();
