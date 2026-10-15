// Downloads always pass through server-side visibility checks; never use public storage URLs.
window.showAttachments=async function(container,kind,parent,editable){
  container.replaceChildren();
  const note=document.createElement('p');note.textContent='이미지 첨부: PNG·JPEG·WebP, 파일당 5MB, 글당 3개. 파일은 다운로드로 제공됩니다.';container.append(note);
  const message=document.createElement('p');message.setAttribute('role','status');container.append(message);
  async function request(path,method='GET',body){
    const headers={};if(method!=='GET'){const csrf=await fetch('/api/account/csrf',{cache:'no-store'});if(!csrf.ok)throw Error('보안 확인을 불러오지 못했습니다.');headers['X-CSRF-TOKEN']=(await csrf.json()).token;}
    const response=await fetch('/api/attachments/'+path,{method,headers,body,cache:'no-store'});
    if(!response.ok){let error;try{error=await response.json();}catch{}throw Error(error?.title||'첨부파일을 처리하지 못했습니다. 로그인·권한·파일 크기를 확인해주세요.');}
    return response.status===204?null:response.json();
  }
  const files=await request(kind+'/'+parent);
  // Ignore a completed fetch if its parent detail was replaced while loading.
  if(!container.contains(message))return;
  const list=document.createElement('ul');container.append(list);
  for(const file of files){const row=document.createElement('li'),link=document.createElement('a');link.textContent=`${file.name} (${Math.ceil(file.length/1024)}KB)`;link.href='/api/attachments/'+file.id;row.append(link);
    if(editable){const button=document.createElement('button');button.type='button';button.textContent='첨부 삭제';button.onclick=async()=>{if(!confirm('첨부파일을 삭제할까요?'))return;button.disabled=true;try{await request(file.id,'DELETE');await window.showAttachments(container,kind,parent,editable);}catch(error){message.textContent=error.message;button.disabled=false;}};row.append(button);}list.append(row);
  }
  if(editable){const form=document.createElement('form'),label=document.createElement('label'),input=document.createElement('input'),button=document.createElement('button');label.textContent='추가할 이미지';input.type='file';input.accept='.png,.jpg,.jpeg,.webp';input.required=true;button.textContent='파일 첨부';label.append(input);form.append(label,button);container.append(form);
    form.onsubmit=async event=>{event.preventDefault();const file=input.files[0];if(!file)return;if(file.size>5*1024*1024){message.textContent='5MB 이하 파일을 선택해주세요.';return;}button.disabled=true;try{const data=new FormData();data.append('file',file);await request(kind+'/'+parent,'POST',data);await window.showAttachments(container,kind,parent,editable);}catch(error){message.textContent=error.message;button.disabled=false;}};
  }
};
