const message=document.querySelector('#account-status');
async function request(path, method='GET', body) {
  const headers={};
  if (method!=='GET') {
    const tokenResponse=await fetch('/api/account/csrf',{cache:'no-store'});
    if (!tokenResponse.ok) throw new Error('요청이 많거나 연결할 수 없습니다. 잠시 후 다시 시도해주세요.');
    headers['X-CSRF-TOKEN']=(await tokenResponse.json()).token;
    headers['Content-Type']='application/json';
  }
  const response=await fetch('/api/account/'+path,{method,headers,body:body?JSON.stringify(body):undefined,cache:'no-store'});
  if (!response.ok) {
    let detail; try {detail=await response.json();} catch {}
    throw new Error(detail?.title || (response.status===401?'아이디·비밀번호·이메일 인증 여부 또는 로그인 상태를 확인해주세요. 반복 실패 시 15분 후 다시 시도해주세요.':response.status===429?'요청이 많습니다. 잠시 후 다시 시도해주세요.':'요청을 처리하지 못했습니다.'));
  }
  return response.status===204?null:response.json();
}
async function refresh() {
  document.querySelector('#admin-link').hidden=true;
  const response=await fetch('/api/account/me',{cache:'no-store'});
  if (response.status===401) {document.querySelector('#anonymous').hidden=false;document.querySelector('#signed-in').hidden=true;document.querySelector('#details-form').reset();await loadSocial(false);return;}
  if (!response.ok) throw new Error('계정 정보를 불러오지 못했습니다. 잠시 후 새로고침해주세요.');
  const user=await response.json();
  document.querySelector('#admin-link').hidden=!user.isAdmin;
  document.querySelector('#anonymous').hidden=true;document.querySelector('#signed-in').hidden=false;
  document.querySelector('#profile').textContent=`${user.username} · ${user.email} · ${user.emailConfirmed?'인증된 이메일':'이메일 미인증'}`;
  document.querySelector('#profile-form').elements.displayName.value=user.displayName;
  await loadDetails();
  await loadSocial(true);
}
function bind(id,path,method,success) {
  const form=document.querySelector(id);
  form.addEventListener('submit',async event=>{
    event.preventDefault(); const button=form.querySelector('button'); button.disabled=true;
    try {
      const data=Object.fromEntries(new FormData(form));
      if (path==='register' && data.password!==data.confirmation) throw new Error('비밀번호 확인이 일치하지 않습니다.');
      delete data.confirmation; delete data.confirmed;
      await request(path,method,data); form.reset(); message.textContent=success; await refresh();
    } catch(error) {message.textContent=error.message;} finally {button.disabled=false;}
  });
}
bind('#login','login','POST','로그인했습니다.');
bind('#register','register','POST','가입했습니다. 이메일 인증이 필요한 경우 인증 후 로그인해주세요.');
bind('#profile-form','profile','PUT','표시 이름을 저장했습니다.');
bind('#password-form','password','POST','비밀번호를 변경했습니다. 다시 로그인해주세요.');
bind('#delete-form','me','DELETE','탈퇴했습니다.');
document.querySelector('#logout').addEventListener('click',async()=>{
  try {await request('logout','POST'); message.textContent='로그아웃했습니다.'; await refresh();}
  catch(error) {message.textContent=error.message;}
});
refresh().catch(error=>message.textContent=error.message);

async function loadDetails() {
  const form=document.querySelector('#details-form'),status=document.querySelector('#details-status');
  form.querySelector('button').disabled=true;
  try {
    const data=await request('details');
    for(const name of ['fullName','address','detailAddress','postcode','birthDate','sex','latitude','longitude','underlyingCondition']) form.elements[name].value=data[name]??'';
    form.elements.healthConsent.checked=Boolean(data.healthConsentAt);
    status.textContent='';form.querySelector('button').disabled=false;
  } catch(error){status.textContent=error.message;}
}
document.querySelector('#details-form').addEventListener('submit',async event=>{
  event.preventDefault();const form=event.currentTarget,button=form.querySelector('button'),status=document.querySelector('#details-status');button.disabled=true;
  try {
    const data=Object.fromEntries(new FormData(form));
    for(const name of ['latitude','longitude'])data[name]=data[name]===''?null:Number(data[name]);
    data.birthDate=data.birthDate||null;data.healthConsent=form.elements.healthConsent.checked;
    if(!data.healthConsent)data.underlyingCondition=null;
    await request('details','PUT',data);await loadDetails();status.textContent='추가 정보를 저장했습니다.';
  }catch(error){status.textContent=error.message;}finally{button.disabled=false;}
});

async function loadSocial(signedIn) {
  const login=document.querySelector('#social-login'),connect=document.querySelector('#social-connect'),links=document.querySelector('#social-links');
  login.replaceChildren();connect.replaceChildren();links.replaceChildren();
  const response=await fetch('/api/social/providers',{cache:'no-store'});if(!response.ok)return;
  const providers=await response.json();
  if(!providers.length){const p=document.createElement('p');p.textContent='소셜 로그인은 운영 설정 후 사용할 수 있습니다.';(signedIn?connect:login).append(p);return;}
  let connected=[];
  if(signedIn){const r=await fetch('/api/social/links',{cache:'no-store'});if(r.ok)connected=await r.json();}
  for(const provider of providers){
    const form=document.createElement('form'),button=document.createElement('button');
    const linked=connected.some(x=>x.provider===provider.provider);
    form.method='post';form.action='/api/social/start';
    for(const [name,value]of Object.entries({provider:provider.provider,intent:signedIn?'link':'login'})){const input=document.createElement('input');input.type='hidden';input.name=name;input.value=value;form.append(input);}
    if(signedIn){const label=document.createElement('label'),password=document.createElement('input');label.textContent='현재 비밀번호';password.name='password';password.type='password';password.required=true;password.maxLength=128;password.autocomplete='current-password';label.append(password);form.append(label);}
    button.textContent=provider.name+(signedIn?(linked?' 연결 해제':' 연결'):' 로그인');form.append(button);
    form.onsubmit=async event=>{
      event.preventDefault();button.disabled=true;
      try{
        const csrf=await fetch('/api/account/csrf',{cache:'no-store'});if(!csrf.ok)throw new Error('잠시 후 다시 시도해주세요.');
        const token=(await csrf.json()).token;
        if(linked){const r=await fetch('/api/social/links/'+provider.provider,{method:'DELETE',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':token},body:JSON.stringify({password:form.elements.password.value})});if(!r.ok)throw new Error('비밀번호와 연결 상태를 확인해주세요.');await refresh();message.textContent='소셜 계정 연결을 해제했습니다.';}
        else{const field=document.createElement('input');field.name='__RequestVerificationToken';field.type='hidden';field.value=token;form.append(field);form.submit();}
      }catch(error){message.textContent=error.message;button.disabled=false;}
    };
    (signedIn?(linked?links:connect):login).append(form);
  }
}
const socialResult=new URLSearchParams(location.search).get('social');
if(socialResult){history.replaceState(null,'',location.pathname);message.textContent=socialResult==='verify-email'?'가입했습니다. 이메일 인증 후 로그인해주세요.':'소셜 인증을 완료하지 못했습니다. 다시 시도해주세요.';}
