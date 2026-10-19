// Run only on the isolated local fixture page, never in the application.
try{
 let passed=0;const check=(condition,label)=>{if(!condition)throw Error(label);passed++;};
 const xml='<DOC title="검사 문서"><SECTION title="조건 A"><ARTICLE title="원본 제목"><PARAGRAPH tagName="p"><![CDATA[원문 <strong>강조</strong> &amp; 숫자 1.5]]></PARAGRAPH><PARAGRAPH tagName="table"><![CDATA[<tbody><tr><th rowspan="2">조건</th><td>2.0</td></tr><tr><td>3.0</td></tr></tbody>]]></PARAGRAPH><PARAGRAPH tagName="p"><![CDATA[<img src="https://untrusted.invalid/image" onerror="window.injected=true" alt="원본 도표"><iframe src="https://untrusted.invalid/frame"></iframe><a href="javascript:window.injected=true">링크 문구</a><script>window.injected=true</script><span onclick="window.injected=true" style="background:url(https://untrusted.invalid/style)">끝 문구</span>]]></PARAGRAPH></ARTICLE></SECTION></DOC>';
 const rendered=window.permitDocument(xml);document.querySelector('#output').append(rendered);
 check(rendered.textContent.includes('원본 제목')&&rendered.textContent.includes('원문 강조 & 숫자 1.5'),'original titles, entities and paragraph text');
 check(rendered.querySelectorAll('table').length===1&&rendered.querySelectorAll('tr').length===2&&rendered.querySelectorAll('td,th').length===3,'table context and every cell retained');
 check(rendered.querySelector('th').getAttribute('rowspan')==='2'&&rendered.querySelector('table').textContent.includes('3.0'),'table span and original numeric text retained');
 check(!rendered.querySelector('script,img,iframe,a,[style],[onclick],[onerror],[src],[href]')&&!window.injected,'provider active content and resource attributes excluded');
 check(rendered.textContent.includes('원본 도표')&&rendered.textContent.includes('링크 문구'),'image alternatives and link text remain visible');
 const dtd='<!DOCTYPE DOC [<!ENTITY x SYSTEM "https://untrusted.invalid/entity">]><DOC>&x;</DOC>';
 check(window.permitDocument(dtd).querySelector('pre')?.textContent===dtd,'DTD documents displayed as inert literal text');
 check(window.permitDocument('<DOC>').querySelector('pre')?.textContent==='<DOC>','malformed XML stays visible rather than being silently discarded');
 document.querySelector('#result').textContent=passed+' browser document checks passed';
}catch(error){document.querySelector('#result').textContent='FAIL: '+error.message;}
