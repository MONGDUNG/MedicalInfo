const form = document.querySelector('#search');
const status = document.querySelector('#status');
const results = document.querySelector('#results');
const detail = document.querySelector('#detail');
const previous = document.querySelector('#previous');
const next = document.querySelector('#next');
let page = 1, keyword = '', generation = 0;
const supplements = document.body.dataset.catalog === 'supplements';
const base = supplements ? '/api/supplements' : '/api/drugs';
const fields = supplements ? {MAIN_FNCTN:'기능성',SRV_USE:'섭취량 및 섭취방법',INTAKE_HINT1:'섭취 시 주의사항',PRSRV_PD:'보관 방법',DISTB_PD:'유통기한',SUNGSANG:'성상',BASE_STANDARD:'기준 및 규격'} : {efcyQesitm:'효능',useMethodQesitm:'사용법',atpnWarnQesitm:'주의사항 경고',atpnQesitm:'주의사항',intrcQesitm:'상호작용',seQesitm:'부작용',depositMethodQesitm:'보관법'};
function element(tag, text) { const node = document.createElement(tag); node.textContent = text; return node; }
async function request(url) { const response = await fetch(url); const data = await response.json(); if (!response.ok) throw new Error(data.title || '정보를 불러오지 못했습니다.'); return data; }
async function search() {
  const version = ++generation;
  results.replaceChildren(); detail.replaceChildren(); previous.disabled = next.disabled = true; status.textContent = '검색 중입니다.';
  try {
    const data = await request(`${base}?keyword=${encodeURIComponent(keyword)}&page=${page}`);
    if (version !== generation) return;
    status.textContent = `${data.total}개 품목 · ${page}페이지`;
    for (const item of data.items) {
      const li = element('li', ''); const button = element('button', item.name); button.type = 'button';
      button.addEventListener('click', () => show(item.id));
      li.append(button, element('p', `${item.company} · ${item.dateLabel} ${item.date || '미제공'}`)); results.append(li);
    }
    previous.disabled = page <= 1; next.disabled = page * data.pageSize >= data.total;
  } catch (error) { if (version === generation) status.textContent = error.message; }
}
async function show(id) {
  const version = ++generation; detail.replaceChildren(element('p', '상세정보를 불러오는 중입니다.'));
  try {
    const data = await request(`${base}/${encodeURIComponent(id)}`);
    if (version !== generation) return;
    detail.replaceChildren();
    if (data.variants.length > 1) detail.append(element('p', '공식 원본에 같은 품목번호의 서로 다른 정보가 있습니다. 각 기록을 원본 그대로 표시합니다.'));
    for (const record of data.variants) {
      detail.append(element('h2', supplements ? record.PRDUCT : record.itemName), element('p', supplements ? `${record.ENTRPS} · 등록일 ${record.REGIST_DT || '미제공'}` : `${record.entpName} · 수정일 ${record.updateDe || '미제공'}`));
      for (const [field, label] of Object.entries(fields)) {
        const text = element('p', record[field] || '제공된 정보가 없습니다.'); text.className = 'drug-text';
        detail.append(element('h3', label), text);
      }
    }
    detail.focus();
  } catch (error) { if (version === generation) detail.replaceChildren(element('p', error.message)); }
}
form.addEventListener('submit', event => { event.preventDefault(); keyword = document.querySelector('#keyword').value.trim(); page = 1; search(); });
previous.addEventListener('click', () => { page--; search(); });
next.addEventListener('click', () => { page++; search(); });
