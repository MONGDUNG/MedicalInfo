// Display official document text and table structure without inserting active provider HTML.
window.permitDocument=function(value){
 const root=document.createElement('div');root.className='permit-document';
 const text=(tag,value)=>{const e=document.createElement(tag);e.textContent=value;return e;};
 if(!value){root.append(text('p','제공된 허가 원문이 없습니다.'));return root;}
 const fallback=()=>{root.replaceChildren(text('p','문서 형식을 표시할 수 없어 원문을 그대로 표시합니다. 식약처 원문 링크도 확인할 수 있습니다.'),text('pre',value));return root;};
 if(value.length>5_000_000||/<!DOCTYPE|<!ENTITY/i.test(value))return fallback();
 const xml=new DOMParser().parseFromString(value,'application/xml');
 if(xml.querySelector('parsererror'))return fallback();
 const allowed=new Set(['p','br','div','span','strong','b','em','i','u','sup','sub','ul','ol','li','table','thead','tbody','tfoot','tr','th','td','caption','colgroup','col','h1','h2','h3','h4','h5','h6']);
 function clean(source,target){
  for(const child of source.childNodes){
   if(child.nodeType===3||child.nodeType===4){target.append(document.createTextNode(child.textContent));continue;}
   if(child.nodeType!==1)continue;
   const tag=child.localName.toLowerCase();
   if(tag==='img'){target.append(text('span','[원문 이미지: '+(child.getAttribute('alt')||'식약처 원문에서 확인')+']'));continue;}
   if(!allowed.has(tag)){clean(child,target);continue;}
   const output=document.createElement(/^h[1-6]$/.test(tag)?'h4':tag);
   for(const attribute of ['colspan','rowspan','start','value']){
    const raw=child.getAttribute(attribute);
    if(raw!==null&&/^-?\d{1,4}$/.test(raw)&&((['colspan','rowspan'].includes(attribute)&&['td','th'].includes(tag))||(attribute==='start'&&tag==='ol')||(attribute==='value'&&tag==='li')))output.setAttribute(attribute,raw);
   }
   clean(child,output);target.append(output);
  }
 }
 function walk(element,target){
  const title=element.getAttribute('title');if(title)target.append(text('h4',title));
  if(element.tagName==='PARAGRAPH'){
   if(element.getAttribute('tagName')==='br'){target.append(document.createElement('br'));return;}
   const template=document.createElement('template');template.innerHTML=element.getAttribute('tagName')==='table'?'<table>'+element.textContent+'</table>':element.textContent;
   const paragraph=document.createElement('div');clean(template.content,paragraph);target.append(paragraph);return;
  }
  for(const child of element.childNodes){
   if(child.nodeType===1)walk(child,target);
   else if((child.nodeType===3||child.nodeType===4)&&child.textContent.trim())target.append(document.createTextNode(child.textContent));
  }
 }
 walk(xml.documentElement,root);return root;
};
