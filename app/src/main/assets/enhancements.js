/* Independent presentation controls layered onto the approved modular editor.
   Preferences are stored locally and survive app relaunch. */
(function(){
  'use strict';
  const KEY='weatherwithyou.module-options.v1';
  const defaultOptions={hourlyCount:6,dailyCount:7,details:['Температура','Осадки','Ветер','Влажность','Давление','УФ-индекс','Восход','Закат'],heroInfo:['Осадки','Ветер','Влажность'],showFamilyLabel:true};
  let options;
  try{options=Object.assign({},defaultOptions,JSON.parse(localStorage.getItem(KEY)||'{}'))}
  catch(e){options=Object.assign({},defaultOptions)}
  const save=()=>{localStorage.setItem(KEY,JSON.stringify(options));renderExtra()};
  function renderExtra(){
    document.querySelectorAll('.module[data-module="hourly"]').forEach(module=>{
      module.querySelectorAll('.hour').forEach((el,i)=>{el.style.display=i<options.hourlyCount?'':'none'})
    });
    document.querySelectorAll('.module[data-module="daily"]').forEach(module=>{
      module.querySelectorAll('.day').forEach((el,i)=>{el.style.display=i<options.dailyCount?'':'none'})
    });
    document.querySelectorAll('.module[data-module="details"]').forEach(module=>{
      module.querySelectorAll('.detail').forEach(el=>{
        let label=el.querySelector('.dlabel')?.textContent?.trim();
        el.dataset.wwyHidden=options.details.includes(label)?'false':'true';
      })
    });
    document.querySelectorAll('.module.hero').forEach(module=>{
      module.querySelectorAll('.hero-pill').forEach(el=>{
        const label=el.lastElementChild?.textContent?.trim()||'';
        el.dataset.wwyHidden=options.heroInfo.includes(label)?'false':'true';
      });
      let existing=module.querySelector('.hero-family-indicator');
      let family;
      try{family=JSON.parse(localStorage.getItem('weatherwithyou')||'{}').family||['female']}catch(e){family=['female']}
      const people=family.map(p=>({male:'Мужчина',female:'Женщина',child:'Ребёнок',dog:'Собака',cat:'Кошка'})[p]||p).join(' · ');
      if(options.showFamilyLabel && people){
        if(!existing){
          existing=document.createElement('div');
          existing.className='hero-family-indicator';
          module.querySelector('.hero-content')?.insertBefore(existing,module.querySelector('.hero-spacer'));
        }
        existing.textContent=people;
      }else existing?.remove();
    })
  }
  const layoutWatcher=new MutationObserver(mutations=>{
    if(mutations.some(m=>m.type==='childList' && (m.target.id==='moduleGrid'||m.target.id==='editorCanvas'))){
      renderExtra()
    }
  });
  const app=document.getElementById('app');
  if(app)layoutWatcher.observe(app,{subtree:true,childList:true});

  let editing='';
  const MODULE_NAME={'Текущая погода (герой)':'hero','Почасовой прогноз':'hourly','Прогноз на 7 дней':'daily','Подробные показатели':'details'};
  function fieldChoices(name,items,selected){
    return '<h3>'+name+'</h3>'+items.map(item=>
      '<label class="extra-row"><span>'+item+'</span><input type="checkbox" data-extra-group="'+
      name+'" value="'+item+'" '+(selected.includes(item)?'checked':'')+'></label>').join('');
  }
  function attachOptions(){
    const body=document.getElementById('sheetBody');
    const title=document.getElementById('sheetTitle')?.textContent?.trim();
    const key=MODULE_NAME[title];
    if(!key || !body || body.querySelector('.module-extras'))return;
    editing=key;
    const extra=document.createElement('div');extra.className='module-extras';
    if(key==='hourly') extra.innerHTML='<h3>Почасовой прогноз</h3><label class="extra-row">Количество часов <select id="extraCount">'+[4,6,8,12].map(v=>'<option value="'+v+'" '+(options.hourlyCount===v?'selected':'')+'>'+v+'</option>').join('')+'</select></label><p class="extra-hint">Большее число часов прокручивается по горизонтали.</p>';
    if(key==='daily')extra.innerHTML='<h3>Прогноз по дням</h3><label class="extra-row">Количество дней <select id="extraCount">'+[3,4,5,6,7].map(v=>'<option value="'+v+'" '+(options.dailyCount===v?'selected':'')+'>'+v+'</option>').join('')+'</select></label>';
    if(key==='details')extra.innerHTML=fieldChoices('Показатели',['Температура','Осадки','Ветер','Влажность','Давление','УФ-индекс','Восход','Закат'],options.details);
    if(key==='hero')extra.innerHTML=fieldChoices('Быстрые показатели',['Осадки','Ветер','Влажность'],options.heroInfo)+'<label class="extra-row">Показывать состав семьи <input id="extraFamily" type="checkbox" '+(options.showFamilyLabel?'checked':'')+'></label>';
    body.append(extra);
  }
  document.addEventListener('click',e=>{
    const btn=e.target.closest?.('[data-pencil],[data-edit-button]');
    if(btn) setTimeout(attachOptions,20);
  },true);
  const apply=document.getElementById('applySheet');
  if(apply)apply.addEventListener('click',()=>{
    const extra=document.querySelector('#sheetBody .module-extras');
    if(!extra)return;
    if(editing==='hourly') options.hourlyCount=Number(extra.querySelector('#extraCount')?.value||6);
    if(editing==='daily') options.dailyCount=Number(extra.querySelector('#extraCount')?.value||7);
    if(editing==='details') options.details=[...extra.querySelectorAll('input:checked')].map(x=>x.value);
    if(editing==='hero'){
      options.heroInfo=[...extra.querySelectorAll('[data-extra-group]:checked')].map(x=>x.value);
      options.showFamilyLabel=!!extra.querySelector('#extraFamily')?.checked;
    }
    save();
  },true);
  document.addEventListener('click',e=>{
    if(e.target.closest('[data-tab],[id=editorBtn],[id=previewBtn],[id=saveBtn]'))setTimeout(renderExtra,100);
  });
  setTimeout(renderExtra,300);
})();
