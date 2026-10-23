/* Pixel-cropped board mode. All live modules remain available through the Data switch. */
(function(){
'use strict';
const KEY='weatherwithyou.visualSkin';
const files={hero:'hero',hourly:'hourly',daily:'daily',radar:'radar',details:'details',clothes:'clothes',family:'family',warnings:'warnings'};
let skin=localStorage.getItem(KEY)==='render' && (window.WWYLocale?.language()||'ru')==='ru';
function ensureSwitcher(){
  const bar=document.querySelector('.topbar');
  if(!bar || document.querySelector('#renderSwitch'))return;
  const btn=document.createElement('button');
  btn.id='renderSwitch';btn.className='render-switch';btn.type='button';
  bar.insertBefore(btn,bar.querySelector('#editorBtn'));
  btn.addEventListener('click',()=>{if((window.WWYLocale?.language()||'ru')!=='ru')return;skin=!skin;localStorage.setItem(KEY,skin?'render':'data');paint()});
}
function designLabel(){
  const home=document.querySelector('#home');
  if(!home)return;
  let tag=document.querySelector('#renderModeTag');
  if(!tag){tag=document.createElement('div');tag.id='renderModeTag';tag.className='render-mode-tag';home.insertBefore(tag,home.firstChild)}
  tag.textContent=skin?'Эталонный дизайн · изображения содержат примерные значения · живой прогноз — кнопка «Данные»':'Живой прогноз · блоки получают текущие погодные данные';
}
function liveSummary(el){
  const id=el.dataset.module;
  if(id==='hero'){
    const city=document.querySelector('#cityInput')?.value || localStorage.getItem('weather.city') || 'Берлин';
    const temperature=el.querySelector('.temp')?.textContent?.trim()||'—';
    const condition=el.querySelector('.condition')?.textContent?.trim()||'';
    return city+' · '+temperature+' · '+condition;
  }
  if(id==='hourly'){return 'Живые часы: '+[...el.querySelectorAll('.h-value')].slice(0,3).map(x=>x.textContent).join(' / ')}
  if(id==='daily'){return 'Данные прогноза доступны в режиме «Данные»'}
  if(id==='details'){return 'Измерения обновляются в режиме «Данные»'}
  return '';
}
let busy=false;
function paint(){
  if(busy)return;busy=true;
  try{
    ensureSwitcher();designLabel();
    const btn=document.querySelector('#renderSwitch');
    if(btn){btn.textContent=skin?'Данные':'Рендер';btn.classList.toggle('active',!skin);}
    const root=document.querySelector('#moduleGrid');
    const editor=document.querySelector('#editorCanvas');
    [root,editor].forEach(container=>{
      if(!container)return;
      container.querySelectorAll('.module[data-module]').forEach(el=>{
        const id=el.dataset.module;if(!files[id])return;
        if(!skin){el.classList.remove('render-tile');el.querySelectorAll(':scope > .render-art,:scope > .render-actions').forEach(x=>x.remove());return}
        if(el.classList.contains('render-tile'))return;
        el.classList.add('render-tile');
        const art=document.createElement('img');art.className='render-art';art.src='render/'+files[id]+'.webp';art.alt='Эталонное изображение модуля '+id;art.loading='eager';art.draggable=false;
        el.insertBefore(art,el.firstChild);
        const actions=document.createElement('div');actions.className='render-actions';
        if(container===root){
          const refresh=document.createElement('button');refresh.type='button';refresh.className='render-action render-refresh';refresh.title='Обновить модуль';refresh.textContent='↻';refresh.addEventListener('click',e=>{e.stopPropagation();skin=false;localStorage.setItem(KEY,'data');paint();el.querySelector('[data-refresh]')?.click()});actions.appendChild(refresh);
          /* Render mode preserves screenshot pixels; live values remain in Data mode. */
        }
        el.appendChild(actions);
      });
    });
  }finally{busy=false}
}
let scheduled=false;
const observer=new MutationObserver(records=>{
  if(busy||scheduled)return;
  if(!records.some(r=>r.addedNodes?.length && [...r.addedNodes].some(n=>n.nodeType===1 && (n.classList?.contains('module') || n.querySelector?.('.module')))))return;
  scheduled=true;requestAnimationFrame(()=>{scheduled=false;paint()})
});
observer.observe(document.body,{subtree:true,childList:true});
document.addEventListener('DOMContentLoaded',paint);
paint();
})();
