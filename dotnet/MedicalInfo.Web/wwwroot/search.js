const form = document.querySelector('#search');
const status = document.querySelector('#status');
const mapStatus = document.querySelector('#map-status');
const results = document.querySelector('#results');
let map, markers, boundary, requestId = 0, controller;
function node(tag, text) { const e = document.createElement(tag); e.textContent = text; return e; }
function coordinate(i) { return Number.isFinite(i.LAT) && Number.isFinite(i.LNG) && Math.abs(i.LAT) <= 90 && Math.abs(i.LNG) <= 180; }
function time(value) {
  if (!value) return '정보 없음';
  const digits = String(value).padStart(4,'0');
  if (/^\d{4}$/.test(digits) && Number(digits.slice(0,2)) <= 24 && Number(digits.slice(2)) < 60 && (Number(digits.slice(0,2)) < 24 || digits.slice(2) === '00')) return digits.slice(0,2)+':'+digits.slice(2);
  return `${value} (원본 표기)`;
}
function facilityDetails(item) {
  const section=node('details',''); section.append(node('summary','진료시간·이용 안내'));
  const data=item.DETAILS || {};
  if (!Object.keys(data).length) { section.append(node('p','제공된 진료시간·이용 안내가 없습니다. 방문 전 기관에 문의해주세요.')); return section; }
  section.append(node('p',`${item.SOURCE_PERIOD || '제공 자료'} 기준 정보입니다. 임시 휴진이나 변경 사항은 방문 전 기관에 확인해주세요.`));
  const table=node('table',''); table.append(node('caption','요일별 진료시간'));
  for (const day of ['월요일','화요일','수요일','목요일','금요일','토요일','일요일']) {
    const tr=node('tr',''), th=node('th',day); th.scope='row';
    const start=data['진료시작시간_'+day], end=data['진료종료시간_'+day];
    tr.append(th,node('td',start || end ? `${time(start)} ~ ${time(end)}` : data['휴진안내_'+day] || '정보 없음')); table.append(tr);
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
  markers = L.layerGroup().addTo(map);
  map.on('moveend', () => { mapStatus.textContent = '현재 지도 중심을 기준으로 주변 검색할 수 있습니다.'; });
} catch { map = null; mapStatus.textContent = '지도를 불러오지 못했습니다. 목록 검색을 이용해주세요.'; document.querySelector('#nearby').disabled = true; }
async function search(url, center = null, radius = null) {
  const version = ++requestId;
  controller?.abort(); controller = new AbortController();
  status.textContent = '검색 중입니다.'; results.replaceChildren(); markers?.clearLayers();
  if (boundary) { boundary.remove(); boundary = null; }
  try {
    const response = await fetch(url, {signal:controller.signal});
    if (!response.ok) throw new Error(response.status === 429 ? '요청이 많습니다. 잠시 후 다시 검색해주세요.' : '검색 서비스를 이용할 수 없습니다. 검색 조건과 연결을 확인해주세요.');
    const items = await response.json(); if (version !== requestId) return;
    const points = []; let missing = 0;
    if (map && center) boundary = L.circle(center, {radius,weight:1,fillOpacity:0.04}).addTo(map);
    for (const [index,item] of items.entries()) {
      const li = node('li',''); li.tabIndex = -1; li.append(node('h2',item.NAME));
      for (const text of [item.CATEGORY_NAME,item.ADDRESS,item.PHONE,item.DEPT_NAME.join(' · ')]) if (text) li.append(node('p',text));
      li.append(facilityDetails(item));
      const reviews=node('a','리뷰 보기·작성'); reviews.href='/reviews.html?'+new URLSearchParams({facilityId:item.ID}); li.append(reviews);
      if (item.SOURCE === 'HIRA') { const a = node('a',`출처: 건강보험심사평가원 · ${item.SOURCE_PERIOD} 기준`); a.href='https://opendata.hira.or.kr/op/opc/selectOpenData.do?sno=11925'; li.append(a); }
      if (map && coordinate(item)) {
        const position=[item.LAT,item.LNG]; points.push(position);
        const popup=node('div',''); popup.append(node('strong',item.NAME),node('p',item.ADDRESS),node('p',item.PHONE));
        const marker=L.marker(position,{title:item.NAME,alt:item.NAME,icon:L.divIcon({className:'facility-marker',html:String(index+1),iconSize:[30,30],iconAnchor:[15,30]})}).bindPopup(popup).addTo(markers);
        marker.getElement().setAttribute('aria-label', `${index+1}. ${item.NAME}`);
        marker.on('click',()=>{ document.querySelector('.selected-facility')?.classList.remove('selected-facility'); li.classList.add('selected-facility'); li.focus({preventScroll:true}); });
        const select=node('button','지도에서 보기'); select.type='button'; select.addEventListener('click',()=>{ map.setView(position,Math.max(map.getZoom(),15)); marker.openPopup(); document.querySelector('#map').scrollIntoView({behavior:'smooth',block:'center'}); }); li.append(select);
      } else { missing++; li.append(node('p',coordinate(item)?'지도를 불러오지 못해 목록으로 표시합니다.':'좌표 정보가 없어 지도에 표시되지 않습니다.')); }
      results.append(li);
    }
    if (map && !center && points.length) map.fitBounds(points,{padding:[30,30],maxZoom:15});
    status.textContent=`${items.length}개 결과${center?' · 가까운 순':''}${items.length===200?' · 최대 200개이므로 검색 범위를 좁혀주세요.':''}${missing?` · 지도 미표시 ${missing}건`:''}`;
  } catch(error) { if(version===requestId && error.name!=='AbortError') status.textContent=error.message; }
}
form.addEventListener('submit',event=>{ event.preventDefault(); const keyword=form.elements.keyword.value.trim(); if(!keyword){status.textContent='검색어를 입력해주세요.';return;} search('/map/search/item?'+new URLSearchParams({keyword})); });
document.querySelector('#nearby').addEventListener('click',()=>{
  if(!map)return; const center=map.getCenter(),level=Number(document.querySelector('#radius').value);
  search('/map/medicalFacilityByDept?'+new URLSearchParams({lat:center.lat,lng:center.lng,level,category:document.querySelector('#category').value,dept:document.querySelector('#department').value.trim()}),center,80*(1<<level));
});
document.querySelector('#locate').addEventListener('click',()=>{
  if(!map || !navigator.geolocation){mapStatus.textContent='현재 위치를 사용할 수 없습니다. 지도를 직접 이동해주세요.';return;}
  mapStatus.textContent='위치를 확인하고 있습니다.';
  navigator.geolocation.getCurrentPosition(position=>{map.setView([position.coords.latitude,position.coords.longitude],15);mapStatus.textContent='내 위치로 이동했습니다. 주변 검색을 눌러주세요.';},()=>{mapStatus.textContent='위치 권한이 없거나 위치를 확인할 수 없습니다. 지도를 직접 이동해주세요.';},{timeout:10000,maximumAge:60000});
});
