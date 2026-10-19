const $=s=>document.querySelector(s),message=$('#status');
// Keep the bearer token only in memory; remove it from the address/history immediately.
const params=new URLSearchParams(location.hash.slice(1)),token=params.get('token'),purpose=params.get('purpose');
if(location.hash)history.replaceState(null,'',location.pathname);
window.addEventListener('hashchange',()=>{if(location.hash)location.reload();});
async function request(path,body){const csrf=await fetch('/api/account/csrf',{cache:'no-store'});if(!csrf.ok)throw Error('보안 확인을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
 const r=await fetch('/api/recovery/'+path,{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':(await csrf.json()).token},body:JSON.stringify(body),cache:'no-store'});
 let data;try{data=await r.json();}catch{}if(!r.ok)throw Error(data?.title||(r.status===429?'요청이 많습니다. 잠시 후 다시 시도해주세요.':'처리하지 못했습니다. 입력 내용을 확인해주세요.'));return data;}
$('#request').onsubmit=async event=>{event.preventDefault();const form=event.currentTarget,b=form.querySelector('button');b.disabled=true;try{const data=Object.fromEntries(new FormData(form)),result=await request('request/'+data.purpose,{email:data.email});message.textContent=result.message;}catch(error){message.textContent=error.message;}finally{b.disabled=false;}};
const labels={confirm:'이메일 인증',username:'아이디 확인',reset:'비밀번호 재설정'};
if(token&&/^[0-9A-Fa-f]{64}$/.test(token)&&labels[purpose]){
 $('#request-section').hidden=true;$('#redeem-section').hidden=false;$('#action-title').textContent=labels[purpose];$('#password-fields').hidden=purpose!=='reset';
 for(const input of $('#password-fields').querySelectorAll('input'))input.required=purpose==='reset';
 $('#redeem').onsubmit=async event=>{event.preventDefault();const form=event.currentTarget,b=form.querySelector('button');b.disabled=true;try{const data=Object.fromEntries(new FormData(form));if(purpose==='reset'&&data.newPassword!==data.confirmation)throw Error('비밀번호 확인이 일치하지 않습니다.');const result=await request('redeem',{token,purpose,newPassword:purpose==='reset'?data.newPassword:null});form.reset();form.hidden=true;$('#result').textContent=result.username?'가입한 아이디: '+result.username:result.message;}catch(error){message.textContent=error.message;b.disabled=false;}};
}else if(token){message.textContent='올바르지 않은 링크입니다. 새 메일을 요청해주세요.';}
fetch('/api/recovery/status',{cache:'no-store'}).then(r=>r.ok?r.json():null).then(data=>{if(data&&!data.enabled){message.textContent='메일 발송 설정을 준비 중입니다. 기존에 받은 유효한 링크는 사용할 수 있습니다.';$('#request').querySelector('button').disabled=true;}}).catch(()=>{});
