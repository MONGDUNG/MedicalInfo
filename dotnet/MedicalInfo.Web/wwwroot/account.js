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
  if (response.status===401) {document.querySelector('#anonymous').hidden=false;document.querySelector('#signed-in').hidden=true;return;}
  if (!response.ok) throw new Error('계정 정보를 불러오지 못했습니다. 잠시 후 새로고침해주세요.');
  const user=await response.json();
  document.querySelector('#admin-link').hidden=!user.isAdmin;
  document.querySelector('#anonymous').hidden=true;document.querySelector('#signed-in').hidden=false;
  document.querySelector('#profile').textContent=`${user.username} · ${user.email} · ${user.emailConfirmed?'인증된 이메일':'이메일 미인증'}`;
  document.querySelector('#profile-form').elements.displayName.value=user.displayName;
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
