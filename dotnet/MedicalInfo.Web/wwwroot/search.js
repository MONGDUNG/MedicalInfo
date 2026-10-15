const form = document.querySelector('#search');
const status = document.querySelector('#status');
const results = document.querySelector('#results');
const button = form.querySelector('button');
form.addEventListener('submit', async event => {
  event.preventDefault();
  const keyword = form.elements.keyword.value.trim();
  if (!keyword) { status.textContent = '검색어를 입력해주세요.'; return; }
  button.disabled = true;
  results.replaceChildren();
  status.textContent = '검색 중입니다.';
  try {
    const response = await fetch('/map/search/item?' + new URLSearchParams({ keyword }));
    if (!response.ok) {
      status.textContent = response.status === 429 ? '요청이 많습니다. 잠시 후 다시 검색해주세요.' :
        response.status === 400 ? '검색어를 확인해주세요.' : '검색 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.';
      return;
    }
    const items = await response.json();
    status.textContent = items.length ? `${items.length}개 결과${items.length === 200 ? ' (최대 200개입니다. 검색어를 구체적으로 입력해주세요.)' : ''}` : '검색 결과가 없습니다.';
    for (const item of items) {
      const li = document.createElement('li');
      const title = document.createElement('h2'); title.textContent = item.NAME;
      li.append(title);
      for (const value of [item.CATEGORY_NAME, item.ADDRESS, item.PHONE, item.DEPT_NAME.join(' · ')]) {
        if (!value) continue;
        const text = document.createElement('p'); text.textContent = value; li.append(text);
      }
      results.append(li);
    }
  } catch { status.textContent = '연결이 끊어졌습니다. 다시 시도해주세요.'; }
  finally { button.disabled = false; }
});
