'use strict';
(async () => {
  const status = document.querySelector('#news-status');
  try {
    const response = await fetch('/api/news');
    if (!response.ok) throw new Error();
    const data = await response.json();
    for (const item of data.items) {
      const row = document.createElement('li');
      const link = document.createElement('a');
      link.href = item.link;
      link.textContent = item.title;
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
      row.append(link);
      if (item.publishedAt) {
        const time = document.createElement('time');
        time.dateTime = item.publishedAt;
        time.textContent = ' · ' + new Date(item.publishedAt).toLocaleDateString('ko-KR', {timeZone: 'Asia/Seoul'});
        row.append(time);
      }
      document.querySelector('#news-items').append(row);
    }
    status.textContent = (data.stale ? '갱신이 지연되어 마지막으로 받은 소식을 표시합니다. ' : '') +
      '수집: ' + new Date(data.fetchedAt).toLocaleString('ko-KR');
  } catch {
    status.textContent = '소식을 불러오지 못했습니다. 아래 보건복지부 링크에서 확인해주세요.';
  }
})();
