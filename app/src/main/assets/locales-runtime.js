/* UI localization: centralized keys, automatic Android locale, manual override. */
(function(){
'use strict';
const catalog=window.WWY_TRANSLATIONS;
if(!catalog)return;
const supported=catalog.codes;
const raw=new WeakMap(),attrs=new WeakMap();
const names={ru:'Русский',en:'English',de:'Deutsch',pl:'Polski',be:'Беларуская'};
const tags={ru:'ru-RU',en:'en-US',de:'de-DE',pl:'pl-PL',be:'be-BY'};
const labels={
ru:['Язык приложения','Как в системе','Автоматически используется язык Android. Для остальных языков доступен английский перевод.'],
en:['App language','Follow system','Follows Android by default. Unsupported languages use English.'],
de:['App-Sprache','Systemsprache','Standardmäßig wird die Android-Sprache verwendet. Sonst Englisch.'],
pl:['Język aplikacji','Jak w systemie','Domyślnie używany jest język Androida. Pozostałe języki: angielski.'],
be:['Мова праграмы','Як у сістэме','Па змаўчанні мова Android. Іншыя мовы — англійская.']};
let selected='system';
try{const cache=JSON.parse(localStorage.getItem('weatherwithyou')||'{}');
selected=localStorage.getItem('weatherwithyou.language')||cache.language||'system';}catch(e){}
if(selected!=='system'&&!supported.includes(selected))selected='system';
function systemLanguage(){let s='';try{s=window.WeatherAndroid?.systemLanguage?.()||''}catch(e){}return(s||navigator.language||'en').toLowerCase().split(/[-_]/)[0]}
function language(){const s=selected==='system'?systemLanguage():selected;return supported.includes(s)?s:'en'}
function intlLocale(){return tags[language()]||'en-US'}
function toSource(v){return catalog.reverse[language()][v]||v}
function escaped(v){return v.replace(/[-/\\^$*+?.()|[\]{}]/g,'\\$&')}
const phrases=new RegExp(catalog.rows.map(r=>r[0]).sort((a,b)=>b.length-a.length).map(escaped).join('|'),'g');
function translate(v){
if(typeof v!=='string'||language()==='ru'||!/[А-Яа-яЁёІіЎў]/.test(v))return v;
const d=catalog.dict[language()];let out=d[v]||v.replace(phrases,m=>d[m]||m);
out=out.replace(/(\d+)\s*м\/с/g,'$1 m/s').replace(/(\d+)\s*мм(?!\w)/g,'$1 mm')
 .replace(/(\d+)\s*км\/ч/g,'$1 km/h').replace(/(\d+)\s*рт\.\s*ст\./g,'$1 mmHg');
return out;
}
function handle(node){
if(node.nodeType===3){
if(!node.parentNode||/^(SCRIPT|STYLE|TEXTAREA|NOSCRIPT)$/.test(node.parentNode.nodeName))return;
if(!raw.has(node))raw.set(node,node.nodeValue);
const next=translate(raw.get(node));if(next!==node.nodeValue)node.nodeValue=next;return;
}
if(node.nodeType!==1)return;
let orig=attrs.get(node);if(!orig){orig={};attrs.set(node,orig)}
for(const a of ['placeholder','aria-label','title','alt']){
if(!node.hasAttribute(a))continue;
if(!(a in orig))orig[a]=node.getAttribute(a);
const next=translate(orig[a]);if(node.getAttribute(a)!==next)node.setAttribute(a,next);
}
}
function setText(id,value){const el=document.getElementById(id);if(el&&el.textContent!==value)el.textContent=value}
function localize(root){
if(!root)return;
handle(root);const walk=document.createTreeWalker(root,NodeFilter.SHOW_ELEMENT|NodeFilter.SHOW_TEXT);let el;
while((el=walk.nextNode()))handle(el);
document.documentElement.lang=language();
const pick=document.getElementById('languageSelect');
if(pick&&pick.value!==selected)pick.value=selected;
setText('wwy-language-label',labels[language()][0]);
setText('wwy-language-title',labels[language()][0]);
setText('wwy-language-hint',labels[language()][2]);
const opt=document.querySelector('#languageSelect option[value="system"]');
if(opt&&opt.textContent!==labels[language()][1])opt.textContent=labels[language()][1];
const button=document.getElementById('renderSwitch');
if(button&&language()!=='ru')button.style.display='none';
}
let scheduled=false;
function schedule(){if(scheduled)return;scheduled=true;requestAnimationFrame(()=>{scheduled=false;localize(document.getElementById('app')||document.body)})}
new MutationObserver(schedule).observe(document.body,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['placeholder','title','alt','aria-label']});
function set(code){
if(code!=='system'&&!supported.includes(code))code='system';
selected=code;localStorage.setItem('weatherwithyou.language',code);
let config={};try{config=JSON.parse(localStorage.getItem('weatherwithyou')||'{}')}catch(e){}
config.language=code;localStorage.setItem('weatherwithyou',JSON.stringify(config));
try{window.WeatherAndroid?.saveConfig?.(JSON.stringify(config))}catch(e){}
if(language()!=='ru')localStorage.setItem('weatherwithyou.visualSkin','data');
window.location.reload();
}
function mount(){
const page=document.getElementById('settings');if(!page||document.getElementById('languageSelect'))return;
const group=document.createElement('section');group.className='settings-group';group.id='wwy-language-settings';
group.innerHTML='<div class="section-title" id="wwy-language-label"></div><div class="optionrow"><label for="languageSelect" id="wwy-language-title"></label><select id="languageSelect"></select></div><p class="muted" id="wwy-language-hint"></p>';
const select=group.querySelector('select');
[{code:'system',label:''},...supported.map(code=>({code,label:names[code]}))].forEach(entry=>{
const option=document.createElement('option');option.value=entry.code;option.textContent=entry.label;select.appendChild(option)});
page.insertBefore(group,page.querySelector('.settings-group'));select.value=selected;
select.addEventListener('change',e=>set(e.target.value));localize(group);
}
window.WWYLocale={language,intlLocale,selected:()=>selected,set,toSource,translate,localize,supported};
mount();localize(document.getElementById('app')||document.body);
})();