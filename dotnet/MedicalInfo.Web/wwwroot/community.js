const status=document.querySelector('#community-status'), postForm=document.querySelector('#post-form'), commentForm=document.querySelector('#comment-form');
let page=1, currentPost=null, editPost=null, editComment=null, commentPage=1, listVersion=0, detailVersion=0;
const node=(tag,text)=>{const e=document.createElement(tag);e.textContent=text;return e;};
async function api(path,method='GET',data) {
  const headers={};
  if (method!=='GET') {
    const response=await fetch('/api/account/csrf',{cache:'no-store'});
    if(!response.ok)throw new Error('보안 확인을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
    headers['X-CSRF-TOKEN']=(await response.json()).token;headers['Content-Type']='application/json';
  }
  const response=await fetch('/api/community/'+path,{method,headers,body:data?JSON.stringify(data):undefined,cache:'no-store'});
  if(!response.ok){let problem;try{problem=await response.json();}catch{}throw new Error(problem?.title || ({401:'로그인이 필요합니다.',403:'작성자 또는 관리자만 처리할 수 있습니다.',404:'글이나 댓글을 찾을 수 없습니다.',429:'요청이 많습니다. 잠시 후 다시 시도해주세요.'}[response.status] || '처리하지 못했습니다. 다시 시도해주세요.'));}
  return response.status===204?null:response.json();
}
function action(label,work){const button=node('button',label);button.type='button';button.addEventListener('click',async()=>{button.disabled=true;try{await work();}catch(error){status.textContent=error.message;}finally{button.disabled=false;}});return button;}
async function list(){
  const version=++listVersion, params=new URLSearchParams(new FormData(document.querySelector('#filter')));params.set('page',page);
  const data=await api('posts?'+params);if(version!==listVersion)return;
  const container=document.querySelector('#posts');container.replaceChildren();
  for(const item of data.items){const li=node('li',''),link=node('a',`[${item.category}] ${item.title}`);link.href='#'+item.id;li.append(link,node('p',item.author));container.append(li);}
  document.querySelector('#page').textContent=`${data.page}페이지 · ${data.total}개`;
  document.querySelector('#previous').disabled=page<=1;document.querySelector('#next').disabled=page*data.pageSize>=data.total;
}
async function comments(version){
  const data=await api(`posts/${currentPost.id}/comments?page=${commentPage}`);if(version!==detailVersion)return;
  const container=document.querySelector('#comments');if(commentPage===1)container.replaceChildren();
  for(const item of data.items){const li=node('li','');li.append(node('strong',item.author),node('p',item.content));
    if(item.canEdit)li.append(action('댓글 수정',()=>{editComment=item.id;commentForm.elements.content.value=item.content;commentForm.elements.content.focus();}),action('댓글 삭제',async()=>{if(!confirm('이 댓글을 삭제할까요?'))return;await api('comments/'+item.id,'DELETE');commentPage=1;await comments(detailVersion);}));container.append(li);}
  document.querySelector('#more-comments').hidden=commentPage*data.pageSize>=data.total;
}
function cancelPost(){editPost=null;postForm.reset();document.querySelector('#editor-title').textContent='새 글 작성';}
async function detail(){
  const version=++detailVersion,id=location.hash.slice(1); document.querySelector('#post-detail').hidden=true;currentPost=null;editComment=null;commentForm.reset();
  if(!/^\d+$/.test(id))return;
  const post=await api('posts/'+id);if(version!==detailVersion)return;currentPost=post;
  document.querySelector('#post-title').textContent=post.title;document.querySelector('#post-meta').textContent=`${post.category} · ${post.author}`;document.querySelector('#post-content').textContent=post.content;
  const actions=document.querySelector('#post-actions');actions.replaceChildren();
  if(post.canEdit)actions.append(action('글 수정',()=>{editPost=post.id;postForm.elements.title.value=post.title;postForm.elements.content.value=post.content;postForm.elements.category.value=post.category;document.querySelector('#editor-title').textContent='글 수정';postForm.elements.title.focus();}),action('글 삭제',async()=>{if(!confirm('글과 댓글을 삭제할까요?'))return;await api('posts/'+post.id,'DELETE');location.hash='';cancelPost();await list();}));
  document.querySelector('#post-detail').hidden=false;commentPage=1;await comments(version);
}
function report(work){return async event=>{event?.preventDefault();try{await work();}catch(error){status.textContent=error.message;}};}
document.querySelector('#filter').addEventListener('submit',report(async()=>{page=1;await list();}));
document.querySelector('#previous').addEventListener('click',report(async()=>{page--;await list();}));document.querySelector('#next').addEventListener('click',report(async()=>{page++;await list();}));
document.querySelector('#more-comments').addEventListener('click',report(async()=>{commentPage++;await comments(detailVersion);}));
document.querySelector('#cancel-edit').addEventListener('click',cancelPost);document.querySelector('#cancel-comment').addEventListener('click',()=>{editComment=null;commentForm.reset();});
postForm.addEventListener('submit',report(async()=>{const button=postForm.querySelector('button');button.disabled=true;try{const result=await api(editPost?'posts/'+editPost:'posts',editPost?'PUT':'POST',Object.fromEntries(new FormData(postForm)));const id=editPost || result.id;cancelPost();status.textContent='글을 저장했습니다.';await list();if(location.hash==='#'+id)await detail();else location.hash=id;}finally{button.disabled=false;}}));
commentForm.addEventListener('submit',report(async()=>{if(!currentPost)throw new Error('글을 선택해주세요.');const button=commentForm.querySelector('button');button.disabled=true;try{await api(editComment?'comments/'+editComment:`posts/${currentPost.id}/comments`,editComment?'PUT':'POST',Object.fromEntries(new FormData(commentForm)));editComment=null;commentForm.reset();commentPage=1;await comments(detailVersion);status.textContent='댓글을 저장했습니다.';}finally{button.disabled=false;}}));
window.addEventListener('hashchange',report(detail));report(async()=>{await list();await detail();})();
