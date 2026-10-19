'use strict';
const status=document.querySelector('#status');
async function submit(path,data){
  const csrf=await fetch('/api/account/csrf',{cache:'no-store'});if(!csrf.ok)throw new Error('잠시 후 다시 시도해주세요.');
  const response=await fetch('/api/social/'+path,{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':(await csrf.json()).token},body:JSON.stringify(data)});
  const result=await response.json();if(!response.ok)throw new Error(result.title||'인증을 완료하지 못했습니다. 회원 계정 화면에서 다시 시작해주세요.');
  location.replace(result.redirect);
}
document.querySelector('#complete').onclick=async event=>{const button=event.currentTarget;button.disabled=true;try{await submit('complete',{});}catch(error){status.textContent=error.message;}finally{button.disabled=false;}};
document.querySelector('#registration').onsubmit=async event=>{event.preventDefault();const form=event.currentTarget,button=form.querySelector('button');button.disabled=true;try{const data=Object.fromEntries(new FormData(form));if(data.password!==data.confirmation)throw new Error('비밀번호 확인이 일치하지 않습니다.');delete data.confirmation;await submit('register',data);}catch(error){status.textContent=error.message;}finally{button.disabled=false;}};
(async()=>{try{const response=await fetch('/api/social/pending',{cache:'no-store'});if(!response.ok)throw new Error('인증 시간이 지났거나 진행 중인 인증이 없습니다. 회원 계정 화면에서 다시 시작해주세요.');const data=await response.json();document.querySelector('#provider').textContent=(data.provider==='Kakao'?'카카오':'네이버')+' 인증을 받았습니다.';document.querySelector('#pending').hidden=false;document.querySelector('#new-member').hidden=data.intent!=='login';status.textContent='';}catch(error){status.textContent=error.message;}})();
