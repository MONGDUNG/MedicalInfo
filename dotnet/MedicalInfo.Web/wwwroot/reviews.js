const facilityId=new URLSearchParams(location.search).get('facilityId'),status=document.querySelector('#review-status'),form=document.querySelector('#review-form');
let page=1,editId=null,version=0;
const node=(tag,text)=>{const el=document.createElement(tag);el.textContent=text;return el;};
async function api(method='GET',id='',data){
  const headers={};
  if(method!=='GET'){
    const response=await fetch('/api/account/csrf',{cache:'no-store'});if(!response.ok)throw new Error('보안 확인을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
    headers['X-CSRF-TOKEN']=(await response.json()).token;headers['Content-Type']='application/json';
  }
  const url='/api/reviews'+(id?'/'+id:method==='GET'?'?'+new URLSearchParams({facilityId,page}):'');
  const response=await fetch(url,{method,headers,body:data?JSON.stringify(data):undefined,cache:'no-store'});
  if(!response.ok){let problem;try{problem=await response.json();}catch{}throw new Error(problem?.title || ({401:'로그인이 필요합니다.',403:'리뷰 작성자 또는 관리자만 처리할 수 있습니다.',404:'기관이나 리뷰를 찾을 수 없습니다.',429:'요청이 많습니다. 잠시 후 다시 시도해주세요.'}[response.status] || '리뷰를 처리하지 못했습니다.'));}
  return response.status===204?null:response.json();
}
function reset(){editId=null;form.reset();document.querySelector('#editor-title').textContent='리뷰 작성';}
function button(label,work){const el=node('button',label);el.type='button';el.addEventListener('click',async()=>{el.disabled=true;try{await work();}catch(error){status.textContent=error.message;}finally{el.disabled=false;}});return el;}
async function load(){
  if(!facilityId)throw new Error('검색 결과에서 기관의 리뷰 링크를 열어주세요.');
  const current=++version,data=await api();if(current!==version)return;
  document.querySelector('#facility').textContent=data.facility.NAME+' 리뷰';document.querySelector('#review-summary').textContent=`${data.total}개 리뷰 · ${data.average===null?'평가 없음':data.average.toFixed(1)+' / 5점'} · ${page}페이지`;
  const list=document.querySelector('#reviews');list.replaceChildren();
  for(const item of data.items){const li=node('li',''),text=node('p',item.content);text.className='drug-text';li.append(node('strong',`${item.author} · ${item.rating}점`),text);
    if(item.canEdit)li.append(button('리뷰 수정',()=>{editId=item.id;form.elements.rating.value=item.rating;form.elements.content.value=item.content;document.querySelector('#editor-title').textContent='리뷰 수정';form.elements.content.focus();}),button('리뷰 삭제',async()=>{if(!confirm('이 리뷰를 삭제할까요?'))return;await api('DELETE',item.id);reset();page=1;await load();}));list.append(li);}
  document.querySelector('#previous').disabled=page<=1;document.querySelector('#next').disabled=page*data.pageSize>=data.total;
}
form.addEventListener('submit',async event=>{event.preventDefault();const submit=form.querySelector('button');submit.disabled=true;try{await api(editId?'PUT':'POST',editId || '',{facilityId,rating:Number(form.elements.rating.value),content:form.elements.content.value});reset();page=1;await load();status.textContent='리뷰를 저장했습니다.';}catch(error){status.textContent=error.message;}finally{submit.disabled=false;}});
document.querySelector('#cancel-edit').addEventListener('click',reset);
document.querySelector('#previous').addEventListener('click',()=>{page--;load().catch(e=>status.textContent=e.message);});document.querySelector('#next').addEventListener('click',()=>{page++;load().catch(e=>status.textContent=e.message);});
load().catch(e=>status.textContent=e.message);
