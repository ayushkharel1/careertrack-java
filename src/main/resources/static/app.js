'use strict';
const $ = id => document.getElementById(id);
const stages = ['SAVED', 'APPLIED', 'INTERVIEW', 'OFFER', 'CLOSED'];
const labels = { SAVED: 'Saved', APPLIED: 'Applied', INTERVIEW: 'Interview', OFFER: 'Offer', CLOSED: 'Closed' };
let applications = [], editing = null, view = 'board', upcomingOnly = false, toastTimer;
const dateKey = (date = new Date()) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const shortDate = value => value ? new Intl.DateTimeFormat('en-US', {month:'short',day:'numeric'}).format(new Date(value+'T12:00:00')) : 'Not set';
function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}
async function api(path = '', options = {}) {
  const response = await fetch('/api/applications'+path, {
    ...options, headers: {'Content-Type':'application/json','X-Requested-With':'CareerTrack',...options.headers}
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `Request failed (${response.status}). Please try again.`);
  }
  return response.status === 204 ? null : response.json();
}
async function load() {
  try {
    applications = await api();
    $('errorBanner').hidden = true;
    render();
  } catch (error) {
    $('errorBanner').textContent = error.message + ' Reload the page to reconnect.';
    $('errorBanner').hidden = false;
    if (!applications.length) $('content').replaceChildren(element('p','loading','Unable to load your workspace.'));
  }
}
function toast(message) {
  $('toast').textContent=message; $('toast').hidden=false;
  clearTimeout(toastTimer); toastTimer=setTimeout(() => $('toast').hidden=true, 4000);
}
function filtered() {
  const query=$('search').value.trim().toLowerCase(), stage=$('stageFilter').value;
  return applications.filter(app => (!stage || app.stage===stage)
    && (!upcomingOnly || (app.stage==='INTERVIEW' && app.interviewOn && app.interviewOn>=dateKey()))
    && `${app.company} ${app.role} ${app.location}`.toLowerCase().includes(query));
}
function render() {
  $('total').textContent=applications.length;
  $('navCount').textContent=applications.length;
  $('progress').textContent=applications.filter(a => ['APPLIED','INTERVIEW'].includes(a.stage)).length;
  $('interviews').textContent=applications.filter(a => a.stage==='INTERVIEW').length;
  $('offers').textContent=applications.filter(a => a.stage==='OFFER').length;
  $('demoBtn').hidden=applications.length>0;
  const items=filtered();
  $('resultCount').textContent=`${items.length} ${items.length===1?'opportunity':'opportunities'}`;
  $('content').replaceChildren();
  if (!applications.length || !items.length) {
    const empty=element('div','empty-state');
    empty.append(element('span','spark','✳'),element('h3','',applications.length?'No matching opportunities':'Make space for your next chapter'),
      element('p','',applications.length?'Try another search or filter. Upcoming interviews need an interview date and the Interview stage.':'Add your first application or explore the workspace with clearly fictional sample opportunities.'));
    const button=element('button','primary',applications.length?'Clear filters':'＋ Add your first application');
    button.addEventListener('click',() => applications.length?resetFilters():openEditor()); empty.append(button);
    $('content').append(empty); return;
  }
  if(view==='list') return renderList(items);
  const board=element('div','board');
  stages.filter(stage=>!$('stageFilter').value||$('stageFilter').value===stage).forEach(stage => {
    const column=element('section','column '+stage), columnItems=items.filter(a=>a.stage===stage), heading=element('div','column-heading');
    heading.append(element('span','dot'),element('span','',labels[stage]),element('span','column-count',columnItems.length));
    const add=element('button','', '+'); add.setAttribute('aria-label',`Add ${labels[stage]} application`); add.onclick=()=>openEditor(null,stage); heading.append(add); column.append(heading);
    if(!columnItems.length) column.append(element('div','empty-column','Room for what’s next'));
    columnItems.forEach(app=>column.append(card(app))); board.append(column);
  });
  $('content').append(board);
}
function card(app) {
  const button=element('button','card'); button.setAttribute('aria-label',`Edit ${app.role} at ${app.company}`); button.onclick=()=>openEditor(app);
  const top=element('div','card-top'); top.append(element('span','company-avatar',app.company.slice(0,2).toUpperCase()),element('span','more','···'));
  button.append(top,element('div','company',app.company),element('div','role',app.role),element('div','location',app.location||'Location not set'));
  if(app.stage==='INTERVIEW' && app.interviewOn) button.append(element('div','interview-date','◷ Interview · '+shortDate(app.interviewOn)));
  const foot=element('div','card-foot'); foot.append(element('span','',app.appliedOn?'Applied '+shortDate(app.appliedOn):'Not applied yet'),element('span','',app.notes?'Notes ↗':'↗')); button.append(foot); return button;
}
function renderList(items) {
  const wrapper=element('div','table-wrap'), table=element('table'), head=element('thead'), row=element('tr');
  ['Company / role','Location','Stage','Applied','Interview'].forEach(label=>{const th=element('th','',label); th.scope='col';row.append(th)});
  head.append(row); table.append(head); const body=element('tbody');
  items.forEach(app=>{const row=element('tr'), company=element('td'), edit=element('button','',app.company); edit.onclick=()=>openEditor(app);edit.setAttribute('aria-label',`Edit ${app.role} at ${app.company}`);
    company.append(edit,element('small','',app.role)); const stage=element('td'); stage.append(element('span','badge '+app.stage,labels[app.stage]));
    row.append(company,element('td','',app.location||'—'),stage,element('td','',shortDate(app.appliedOn)),element('td','',shortDate(app.interviewOn)));body.append(row);
  }); table.append(body);wrapper.append(table);$('content').append(wrapper);
}
function openEditor(app=null,stage='SAVED') {
  editing=app; const form=$('applicationForm'); form.reset();
  $('modalTitle').textContent=app?'Edit application':'Add application'; $('deleteBtn').hidden=!app; $('formError').textContent='';
  form.elements.appliedOn.max=dateKey();
  for(const name of ['company','role','location','stage','appliedOn','interviewOn','notes']) form.elements[name].value=app?.[name]??(name==='stage'?stage:'');
  $('editor').showModal(); form.elements.company.focus();
}
function setBusy(busy) { for(const id of ['saveBtn','deleteBtn','cancelBtn','closeDialog']) $(id).disabled=busy; }
$('applicationForm').addEventListener('submit',async event=>{
  event.preventDefault(); const form=event.currentTarget, data=Object.fromEntries(new FormData(form));
  data.appliedOn=data.appliedOn||null;data.interviewOn=data.interviewOn||null; data.version=editing?.version??null;
  setBusy(true);$('formError').textContent='';
  try {await api(editing?'/'+editing.id:'',{method:editing?'PUT':'POST',body:JSON.stringify(data)});$('editor').close();toast(editing?'Application updated':'Opportunity added');await load();}
  catch(error){$('formError').textContent=error.message;}
  finally{setBusy(false);}
});
$('deleteBtn').onclick=async()=>{
  if(!editing||!confirm(`Delete your ${editing.role} application at ${editing.company}? This cannot be undone.`))return;
  setBusy(true);try{await api(`/${editing.id}?version=${editing.version}`,{method:'DELETE'});$('editor').close();toast('Application deleted');await load();}
  catch(error){$('formError').textContent=error.message;}finally{setBusy(false);}
};
$('closeDialog').onclick=$('cancelBtn').onclick=()=>$('editor').close();
$('editor').addEventListener('cancel',event=>{if($('saveBtn').disabled)event.preventDefault()});
$('addBtn').onclick=()=>openEditor();$('search').oninput=render;$('stageFilter').onchange=render;
function setView(next){view=next;for(const key of ['board','list']){$(key+'Btn').classList.toggle('selected',key===view);$(key+'Btn').setAttribute('aria-pressed',String(key===view));}render();}
$('boardBtn').onclick=()=>setView('board');$('listBtn').onclick=()=>setView('list');
function setUpcoming(value){upcomingOnly=value;$('pipelineNav').classList.toggle('active',!value);$('interviewNav').classList.toggle('active',value);$('sectionTitle').textContent=value?'Upcoming interviews':'Application pipeline';$('sectionDescription').textContent=value?'Your scheduled conversations, from today onward.':'A clear view of where things stand.';$('breadcrumb').textContent=value?'Interviews':'Overview';render();}
function resetFilters(){$('search').value='';$('stageFilter').value='';setUpcoming(false);}
$('pipelineNav').onclick=()=>{resetFilters()};$('interviewNav').onclick=()=>{$('search').value='';$('stageFilter').value='';setUpcoming(true);setView('list');};
$('demoBtn').onclick=async()=>{
  if(!confirm('Add seven fictional sample applications to your local database? You can edit or delete them afterward.'))return;
  $('demoBtn').disabled=true;
  const offset=days=>{const d=new Date();d.setDate(d.getDate()+days);return dateKey(d)};
  const samples=[['Northstar Labs','Java Developer','Austin, TX · Hybrid','SAVED'],['Cedar Systems','Backend Engineer','Remote · US','SAVED'],['Juniper Cloud','Software Engineer I','Houston, TX · Hybrid','APPLIED'],['Orbit Studio','Java Developer Intern','Remote · US','APPLIED'],['Atlas Works','Backend Developer','Dallas, TX · On-site','INTERVIEW'],['Fern Technologies','Associate Engineer','Remote · US','OFFER'],['Lumen Labs','Platform Engineer','Austin, TX · Hybrid','CLOSED']];
  try{for(const [company,role,location,stage] of samples)await api('',{method:'POST',body:JSON.stringify({company,role,location,stage,appliedOn:stage==='SAVED'?null:offset(-7),interviewOn:stage==='INTERVIEW'?offset(3):null,notes:'Fictional sample application — replace with your own opportunity.'})});toast('Sample workspace ready');}
  catch(error){toast('Some sample records may have been added. '+error.message);}
  finally{await load();$('demoBtn').disabled=false;}
};
$('today').textContent=new Intl.DateTimeFormat('en-US',{weekday:'short',month:'long',day:'numeric',year:'numeric'}).format(new Date());
load();
