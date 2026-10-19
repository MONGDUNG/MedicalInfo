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
    const data = await request(`${base}?`+new URLSearchParams({keyword,page,effect:document.querySelector('#effect').value.trim(),sort:document.querySelector('#sort')?.value||'name',ingredient:document.querySelector('#ingredient')?.value.trim()||''}));
    if (version !== generation) return;
    status.textContent = `${data.total}개 품목 · ${page}페이지`;
    if(!supplements&&!data.permitDataAvailable)status.textContent+=' · 현재 e약은요 제공 품목만 조회하며 주성분 자료는 아직 연결되지 않았습니다.';
    if(!supplements&&data.permitCollectedAt)status.textContent+=' · 허가자료 수집: '+new Date(data.permitCollectedAt).toLocaleString('ko-KR');
    for (const item of data.items) {
      const li = element('li', ''); const button = element('button', item.name); button.type = 'button';
      button.addEventListener('click', () => show(item.id));
      li.append(button, element('p', `${item.company} · ${item.dateLabel} ${item.date || '미제공'}`)); results.append(li);
      if(!supplements){
        if(item.koreanIngredients?.length)li.append(element('p','유효성분 원문: '+item.koreanIngredients.join(' / ')));
        else if(item.ingredients?.length)li.append(element('p','주성분: '+item.ingredients.join(' / ')));
        if(item.permitStatuses?.length)li.append(element('p','허가 상태 원본: '+item.permitStatuses.join(' / ')));
        if(item.hasEasyInformation===false)li.append(element('p','허가 목록 수록 품목 · e약은요 설명 미제공'));
      }
      if(supplements)li.append(element('p',`회원 추천 ${item.recommendations}개`));
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
    if(!supplements){
      if(!data.variants.length)detail.append(element('h2',data.permits[0].ITEM_NAME),element('p','이 품목의 e약은요 효능·사용법 설명은 제공되지 않습니다.'));
      if(data.permits?.length){
        detail.append(element('h2','식약처 제품 허가정보'));
        detail.append(element('p',data.permitCollectedAt?'자료 수집: '+new Date(data.permitCollectedAt).toLocaleString('ko-KR'):'자료 수집 시각은 제공되지 않았습니다.'));
        if(data.permits.length>1)detail.append(element('p','같은 품목번호의 서로 다른 허가 원본을 모두 표시합니다.'));
        const permitFields={ITEM_SEQ:'품목기준코드',ITEM_NAME:'허가 품목명',ENTP_NAME:'업체명',ITEM_INGR_NAME:'주성분',SPCLTY_PBLC:'전문·일반 구분',ITEM_PERMIT_DATE:'허가일',CANCEL_NAME:'허가 상태 원본',CANCEL_DATE:'취소·취하일'};
        for(const record of data.permits){const list=element('dl','');for(const [field,label]of Object.entries(permitFields))list.append(element('dt',label),element('dd',record[field]||'제공된 정보가 없습니다.'));detail.append(list);}
        const source=element('a','출처: 식품의약품안전처 의약품 제품 허가정보');source.href=data.permitSource;detail.append(source);
        detail.append(element('p','허가일은 정보 수정일이 아닙니다. 허가 자료만으로 현재 판매·공급 여부를 판단할 수 없습니다.'));
      }else detail.append(element('p',data.permitDataAvailable?'동일 품목번호의 일반의약품 허가 자료가 없습니다. 이름이 비슷한 제품과 합치지 않습니다.':'제품 허가정보가 아직 연결되지 않아 주성분을 표시할 수 없습니다.'));
      for(const record of data.permitDetails||[]){
        detail.append(element('h2','식약처 허가 상세 원문'),element('p',`${record.ITEM_NAME} · 원본 변경일 ${record.CHANGE_DATE||'미제공'}`));
        if(data.permitDetailsCollectedAt)detail.append(element('p','상세자료 수집: '+new Date(data.permitDetailsCollectedAt).toLocaleString('ko-KR')));
        const list=element('dl','');for(const [field,label]of Object.entries({MAIN_ITEM_INGR:'유효성분 원문',MATERIAL_NAME:'원료약품 및 분량 원문',STORAGE_METHOD:'저장 방법',VALID_TERM:'사용 기간'}))list.append(element('dt',label),element('dd',record[field]||'제공된 정보가 없습니다.'));detail.append(list);
        for(const [field,label,urlField]of [['EE_DOC_DATA','효능·효과','EE_DOC_ID'],['UD_DOC_DATA','용법·용량','UD_DOC_ID'],['NB_DOC_DATA','사용상의 주의사항','NB_DOC_ID']]){
          const area=element('details','');area.append(element('summary',label+' 허가 원문'),window.permitDocument(record[field]));
          try{const url=new URL(record[urlField]);if(url.protocol==='https:'&&url.hostname==='nedrug.mfds.go.kr'&&!url.username&&!url.password){const link=element('a','식약처 '+label+' 원문 열기');link.href=url.href;link.target='_blank';link.rel='noopener noreferrer';area.append(link);}}catch{}
          detail.append(area);
        }
      }
    }
    const community=element('section','');detail.append(community);
    try { await window.productCommunity(community,supplements?'supplements':'drugs',id); }
    catch(error) { community.replaceChildren(element('p','회원 의견을 불러오지 못했습니다. '+error.message)); }
    if(version===generation)detail.focus();
  } catch (error) { if (version === generation) detail.replaceChildren(element('p', error.message)); }
}
form.addEventListener('submit', event => { event.preventDefault(); keyword = document.querySelector('#keyword').value.trim(); page = 1; search(); });
previous.addEventListener('click', () => { page--; search(); });
next.addEventListener('click', () => { page++; search(); });
