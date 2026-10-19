const form = document.querySelector('#search');
const status = document.querySelector('#status');
const mapStatus = document.querySelector('#map-status');
const mapZoom = document.querySelector('#map-zoom');
const results = document.querySelector('#results');
const emptyState = document.querySelector('#result-empty');
let map, markers, boundary, requestId = 0, controller;
function node(tag, text) { const e = document.createElement(tag); e.textContent = text; return e; }
function coordinate(i) { return Number.isFinite(i.LAT) && Number.isFinite(i.LNG) && Math.abs(i.LAT) <= 90 && Math.abs(i.LNG) <= 180; }
function emptyMessage(title, description) {
  if (!emptyState) return;
  emptyState.hidden = false;
  document.querySelector('#empty-title').textContent = title;
  document.querySelector('#empty-description').textContent = description;
}
function selectFacility(li) {
  document.querySelector('.selected-facility')?.classList.remove('selected-facility');
  li.classList.add('selected-facility');
}
function scrollToElement(element) {
  element?.scrollIntoView({behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth',block:'center'});
}
function time(value) {
  if (!value) return '정보 없음';
  const digits = String(value).padStart(4,'0');
  if (/^\d{4}$/.test(digits) && Number(digits.slice(0,2)) <= 24 && Number(digits.slice(2)) < 60 && (Number(digits.slice(0,2)) < 24 || digits.slice(2) === '00')) return digits.slice(0,2)+':'+digits.slice(2);
  return value + ' (원본 표기)';
}
function facilityDetails(item) {
  const isStore=['SEOUL_OTC','MOIS_OTC'].includes(item.SOURCE);
  const section=node('details',''); section.append(node('summary',isStore?'판매점 이용 안내':item.SOURCE==='NMC'?'기관 진료시간·이용 안내':'진료시간·이용 안내'));
  if(item.SOURCE==='NMC')section.append(node('p','기관의 일반 진료시간 자료입니다. 응급환자 수용 가능 여부·실시간 가용 병상은 제공하지 않습니다. 이용 전 기관에 확인해주세요.'));
  if(isStore)section.append(node('p','안전상비의약품 판매 등록 정보입니다. 현재 영업 여부·약품 재고·24시간 운영을 보장하지 않습니다. 방문 전 판매점에 확인해주세요.'));
  const data=item.DETAILS || {};
  if (!Object.keys(data).length) { section.append(node('p',isStore?'판매점 영업시간 자료가 없습니다.':'제공된 진료시간·이용 안내가 없습니다. 방문 전 기관에 문의해주세요.')); return section; }
  section.append(node('p',(item.SOURCE_PERIOD || '제공 자료') + ' ' + (item.SOURCE==='NMC'?'수집':'기준') + ' 정보입니다. 임시 휴진이나 변경 사항은 방문 전 기관에 확인해주세요.'));
  const table=node('table',''); table.append(node('caption',item.SOURCE==='NMC'?'기관 일반 진료시간':'요일별 진료시간'));
  for (const day of ['월요일','화요일','수요일','목요일','금요일','토요일','일요일','공휴일']) {
    const tr=node('tr',''), th=node('th',day); th.scope='row';
    const start=data['진료시작시간_'+day], end=data['진료종료시간_'+day];
    tr.append(th,node('td',start || end ? time(start)+' ~ '+time(end) : data['휴진안내_'+day] || '정보 없음')); table.append(tr);
  }
  section.append(table);
  const list=node('dl','');
  for (const [key,raw] of Object.entries(data)) {
    if (key.startsWith('진료시작시간_') || key.startsWith('진료종료시간_')) continue;
    const value=key==='주차_비용 부담여부' && ['Y','N'].includes(raw) ? (raw==='Y'?'유료':'무료') : raw;
    list.append(node('dt',key.replaceAll('_',' · ')),node('dd',value));
  }
  section.append(list); return section;
}
try {
  map = L.map('map').setView([37.5665, 126.978], 13);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {maxZoom:19, attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'}).on('tileerror', () => { mapStatus.textContent = '배경 지도를 불러오지 못했습니다. 목록 검색은 계속 사용할 수 있습니다.'; }).addTo(map);
  markers = L.markerClusterGroup({
    maxClusterRadius: 44,
    showCoverageOnHover: false,
    animate: false,
    spiderfyDistanceMultiplier: 1.5,
    spiderLegPolylineOptions: {weight: 1.5, color: '#2563eb', opacity: 0.7},
    iconCreateFunction(cluster) {
      const count = cluster.getChildCount();
      return L.divIcon({className: 'facility-cluster', iconSize: [44,44],
        html: '<span aria-hidden="true">'+count+'<small>곳</small></span><span class="sr-only">'+count+'개 기관 모음. 클릭하면 확대하거나 겹친 위치를 펼칩니다.</span>'});
    }
  }).addTo(map);
  markers.on('spiderfied', () => { mapStatus.textContent = '겹친 마커를 펼쳤습니다. 연결선은 제공기관의 원래 좌표를 가리킵니다. 각 핀을 눌러 기관을 선택하세요.'; });
  markers.on('unspiderfied', () => { mapStatus.textContent = '원형 마커의 ‘곳’은 모인 기관 수입니다. 클릭하면 확대하거나 겹친 위치를 펼칩니다.'; });
  const updateZoom = () => { mapZoom.textContent = '확대 '+map.getZoom()+' / '+map.getMaxZoom(); };
  map.on('zoomend', updateZoom); updateZoom();
  map.on('moveend', () => { mapStatus.textContent = '현재 지도 중심을 기준으로 주변 검색할 수 있습니다.'; });
} catch { map = null; mapStatus.textContent = '지도를 불러오지 못했습니다. 목록 검색을 이용해주세요.'; document.querySelector('#nearby').disabled = true; }
async function search(url, center = null, radius = null) {
  const version = ++requestId;
  controller?.abort(); controller = new AbortController();
  status.textContent = '검색 중입니다.'; results.replaceChildren(); markers?.clearLayers();
  results.setAttribute('aria-busy', 'true');
  if (emptyState) emptyState.hidden = true;
  if (boundary) { boundary.remove(); boundary = null; }
  try {
    const response = await fetch(url, {signal:controller.signal});
    if (!response.ok) throw new Error(response.status === 429 ? '요청이 많습니다. 잠시 후 다시 검색해주세요.' : '검색 서비스를 이용할 수 없습니다. 검색 조건과 연결을 확인해주세요.');
    const items = await response.json(); if (version !== requestId) return;
    const points = []; let missing = 0;
    if (map && center) boundary = L.circle(center, {radius,weight:1,fillOpacity:0.04}).addTo(map);
    for (const [index,item] of items.entries()) {
      const li = node('li',''); li.tabIndex = -1;
      const meta = node('div',''); meta.className = 'facility-meta';
      const badge = node('span',item.CATEGORY_NAME); badge.className = 'category-badge';
      const number = node('span',String(index+1).padStart(2,'0')); number.className = 'result-number';
      meta.append(badge,number); li.append(meta,node('h2',item.NAME));
      const departments = node('p',item.DEPT_NAME.join(' · ')); departments.className = 'facility-departments';
      if (departments.textContent) li.append(departments);
      if (item.ADDRESS) li.append(node('p',item.ADDRESS));
      if (item.PHONE) {
        const phone=node('p',''), call=node('a',item.PHONE), dial=item.PHONE.replace(/[^0-9+]/g,'');
        if (dial && /^\+?\d+$/.test(dial)) call.href='tel:'+dial;
        phone.append(call); li.append(phone);
      }
      li.append(facilityDetails(item));
      const actions=node('div',''); actions.className='facility-actions';
      const reviews=node('a','상세정보·리뷰'); reviews.href='/reviews.html?'+new URLSearchParams({facilityId:item.ID}); actions.append(reviews);
      let source;
      if (item.SOURCE==='HIRA') { source=node('a','출처: 건강보험심사평가원 · '+item.SOURCE_PERIOD+' 기준'); source.href='https://opendata.hira.or.kr/op/opc/selectOpenData.do?sno=11925'; }
      const sources={SEOUL_OTC:['서울특별시 · 서울 지역 자료','https://data.seoul.go.kr/dataList/OA-16483/S/1/datasetView.do'],MOIS_OTC:['행정안전부 · 전국 자료','https://www.data.go.kr/data/15154791/openapi.do'],NMC:['국립중앙의료원 · 전국 기관 목록','https://www.data.go.kr/data/15000563/openapi.do']};
      if (sources[item.SOURCE]) { const [label,url]=sources[item.SOURCE]; source=node('a','출처: '+label+' · '+item.SOURCE_PERIOD+' 수집'); source.href=url; }
      if (map && coordinate(item)) {
        const position=[item.LAT,item.LNG]; points.push(position);
        const popup=node('div',''); popup.append(node('strong',item.NAME),node('p',item.ADDRESS),node('p',item.PHONE));
        const locationNote=node('p','제공기관의 좌표로 표시합니다. 건물 대표 위치일 수 있으니 주소도 확인해주세요.'); locationNote.className='location-note'; popup.append(locationNote);
        const marker=L.marker(position,{title:item.NAME,alt:item.NAME,icon:L.divIcon({className:'facility-marker',html:String(index+1),iconSize:[30,30],iconAnchor:[15,30]})}).bindPopup(popup);
        marker.on('add',()=>marker.getElement()?.setAttribute('aria-label',(index+1)+'. '+item.NAME));
        marker.addTo(markers);
        marker.on('click',()=>{ selectFacility(li); li.focus({preventScroll:true}); li.scrollIntoView({behavior:'instant',block:'nearest'}); });
        const select=node('button','지도에서 보기'); select.type='button';
        select.addEventListener('click',()=>{
          selectFacility(li); map.setView(position,Math.max(map.getZoom(),17),{animate:false});
          const visible = markers.getVisibleParent(marker);
          if (visible && visible !== marker) visible.spiderfy();
          marker.openPopup();
          scrollToElement(document.querySelector('#map'));
        });
        actions.append(select);
      } else { missing++; li.append(node('p',coordinate(item)?'지도를 불러오지 못해 목록으로 표시합니다.':'좌표 정보가 없어 지도에 표시되지 않습니다.')); }
      li.append(actions);
      if (source) { source.className='facility-source'; li.append(source); }
      results.append(li);
    }
    if (map && !center && points.length) map.fitBounds(points,{padding:[30,30],maxZoom:15});
    status.textContent=items.length+'개 결과'+(center?' · 가까운 순':'')+(items.length===200?' · 최대 200개이므로 검색 범위를 좁혀주세요.':'')+(missing?' · 지도 미표시 '+missing+'건':'');
    if (!items.length) emptyMessage('검색 결과가 없어요','다른 지역이나 기관 이름으로 검색하거나, 진료과와 검색 반경을 조정해보세요.');
  } catch(error) {
    if(version===requestId && error.name!=='AbortError') { status.textContent=error.message; emptyMessage('잠시 검색할 수 없어요','검색 조건과 연결을 확인한 뒤 다시 시도해주세요.'); }
  } finally { if(version===requestId) results.setAttribute('aria-busy','false'); }
}
form.addEventListener('submit',event=>{
  event.preventDefault(); const keyword=form.elements.keyword.value.trim();
  if(!keyword){status.textContent='검색어를 입력해주세요.';return;}
  search('/map/search/item?'+new URLSearchParams({keyword}));
  document.querySelector('.finder')?.scrollIntoView({behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth',block:'start'});
});
document.querySelectorAll('[data-keyword]').forEach(button=>button.addEventListener('click',()=>{
  form.elements.keyword.value=button.dataset.keyword; form.requestSubmit();
}));
document.querySelector('#nearby').addEventListener('click',()=>{
  if(!map)return; const center=map.getCenter(),level=Number(document.querySelector('#radius').value);
  search('/map/medicalFacilityByDept?'+new URLSearchParams({lat:center.lat,lng:center.lng,level,category:document.querySelector('#category').value,dept:document.querySelector('#department').value.trim()}),center,80*(1<<level));
});
document.querySelector('#category').addEventListener('change',event=>{
  const store=event.target.value==='안전상비의약품 판매업소',department=document.querySelector('#department');
  department.disabled=store;if(store)department.value='';
});
document.querySelector('#locate').addEventListener('click',()=>{
  if(!map || !navigator.geolocation){mapStatus.textContent='현재 위치를 사용할 수 없습니다. 지도를 직접 이동해주세요.';return;}
  mapStatus.textContent='위치를 확인하고 있습니다.';
  navigator.geolocation.getCurrentPosition(position=>{map.setView([position.coords.latitude,position.coords.longitude],15);mapStatus.textContent='내 위치로 이동했습니다. 주변 검색을 눌러주세요.';},()=>{mapStatus.textContent='위치 권한이 없거나 위치를 확인할 수 없습니다. 지도를 직접 이동해주세요.';},{timeout:10000,maximumAge:60000});
});
