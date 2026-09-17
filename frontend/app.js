const state = JSON.parse(localStorage.getItem('bankpulse-real-lab') || '{"done":[],"evidence":[],"sawPending":false,"github":false,"docker":false}');
const $ = (s) => document.querySelector(s);
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const short = (value) => String(value || '').slice(0, 8);
let latest = { payments: [], audit: [], outbox: null, paymentsUp: false, auditUp: false };

function save(){ localStorage.setItem('bankpulse-real-lab', JSON.stringify(state)); }
function now(){ return new Date().toLocaleTimeString('es-EC', {hour12:false}); }
function toast(message){ const el=$('#toast'); el.textContent=message; el.classList.add('show'); clearTimeout(toast.timer); toast.timer=setTimeout(()=>el.classList.remove('show'),2600); }
function evidence(message){
  if(state.evidence[0] !== message) state.evidence.unshift(message);
  state.evidence=state.evidence.slice(0,12); save(); renderEvidence();
}
function complete(id, message){
  if(!state.done.includes(id)){ state.done.push(id); evidence(message); toast(`Misión ${id} completada`); save(); renderProgress(); }
}
function renderProgress(){
  const weights={1:20,2:20,3:20,4:25,5:15};
  const score=state.done.reduce((sum,id)=>sum+(weights[id]||0),0);
  $('#score').textContent=score+'%'; $('#progressBar').style.width=score+'%';
  $('#rank').textContent=score===100?'Senior Platform Engineer':score>=65?'Incident Responder':score>=35?'Service Operator':'Observer';
  document.querySelectorAll('#missions li').forEach(li=>li.classList.toggle('done',state.done.includes(Number(li.dataset.mission))));
}
function renderEvidence(){
  $('#evidenceLog').innerHTML=state.evidence.length?state.evidence.map(x=>`<p><time>${now()}</time> ${esc(x)}</p>`).join(''):'<p><time>--:--:--</time> Esperando telemetría real…</p>';
}
function healthCard(card, label, up){
  $(card).classList.toggle('ok',up); $(card).classList.toggle('bad',!up); $(label).textContent=up?'UP':'DOWN';
}
async function json(url, options){
  const response=await fetch(url,options); if(!response.ok) throw new Error(`${response.status} ${response.statusText}`); return response.json();
}
async function refresh(){
  const results=await Promise.allSettled([json('/health/payments'),json('/health/audit'),json('/api/payments'),json('/api/audit'),json('/api/outbox')]);
  latest.paymentsUp=results[0].status==='fulfilled'&&results[0].value.status==='UP';
  latest.auditUp=results[1].status==='fulfilled'&&results[1].value.status==='UP';
  if(results[2].status==='fulfilled') latest.payments=results[2].value;
  if(results[3].status==='fulfilled') latest.audit=results[3].value;
  if(results[4].status==='fulfilled') latest.outbox=results[4].value;
  healthCard('#paymentsStatus','#paymentsHealth',latest.paymentsUp);
  healthCard('#auditStatus','#auditHealth',latest.auditUp);
  $('#paymentCount').textContent=latest.payments.length; $('#auditCount').textContent=latest.audit.length;
  const pending=latest.outbox?.pending;
  $('#pendingCount').textContent=pending ?? '—';
  $('#outboxStatus').classList.toggle('warn',pending>0); $('#outboxStatus').classList.toggle('ok',pending===0);
  $('#outboxHint').textContent=pending>0?'Entrega diferida: datos protegidos':pending===0?'Backlog drenado':'Sin telemetría';
  renderTables(); evaluate(pending);
}
function renderTables(){
  $('#paymentsTable').innerHTML=latest.payments.length?latest.payments.map(p=>`<tr><td title="${esc(p.id)}">${esc(short(p.id))}</td><td>${esc(p.account)}</td><td>${esc(p.currency)} ${Number(p.amount).toFixed(2)}</td><td>${esc(p.status)}</td></tr>`).join(''):'<tr><td colspan="4" class="empty">Sin pagos</td></tr>';
  $('#auditStream').innerHTML=latest.audit.length?latest.audit.map(a=>`<div class="event"><strong>${esc(a.eventType)} · ${esc(short(a.aggregateId))}</strong><small>event ${esc(short(a.eventId))} · ${new Date(a.receivedAt).toLocaleTimeString('es-EC',{hour12:false})}</small></div>`).join(''):'<div class="empty">Sin eventos o servicio no disponible</div>';
}
function evaluate(pending){
  if(latest.paymentsUp&&latest.auditUp) complete(1,'payments-api y audit-api reportan estado UP.');
  if(latest.payments.length&&latest.audit.some(a=>latest.payments.some(p=>p.id===a.aggregateId))) complete(2,'Pago de MariaDB correlacionado con evento de MongoDB.');
  if(pending>0){ state.sawPending=true; save(); $('#chaosState').textContent=`Auditoría degradada: ${pending} evento(s) protegido(s) en outbox`; $('#chaosProgress').style.width='55%'; evidence('Outbox detectó una entrega pendiente durante la caída.'); }
  if(state.sawPending&&pending===0&&latest.auditUp){ complete(4,'Outbox drenado tras recuperar MongoDB; no se perdió el pago.'); $('#chaosState').textContent='Recuperación verificada: backlog drenado'; $('#chaosProgress').style.width='100%'; }
}
async function sendPayment(key){
  return json('/api/payments',{method:'POST',headers:{'Content-Type':'application/json','X-Idempotency-Key':key},body:JSON.stringify({account:$('#account').value.trim(),amount:Number($('#amount').value),currency:$('#currency').value.trim().toUpperCase()})});
}
$('#paymentForm').addEventListener('submit',async e=>{
  e.preventDefault(); const button=$('#createPayment'); const key=crypto.randomUUID(); $('#idempotencyKey').textContent=key; button.disabled=true;
  try{ const p=await sendPayment(key); $('#requestResult').className='notice good'; $('#requestResult').textContent=`Pago ${short(p.id)} confirmado en MariaDB; publicación de auditoría en curso.`; evidence(`POST /api/payments aceptado: ${short(p.id)}.`); await refresh(); }
  catch(err){ $('#requestResult').className='notice bad'; $('#requestResult').textContent=`Solicitud rechazada: ${err.message}`; }
  finally{ button.disabled=false; }
});
$('#testIdempotency').addEventListener('click',async()=>{
  const button=$('#testIdempotency'); button.disabled=true; const key='idem-'+crypto.randomUUID(); $('#idempotencyKey').textContent=key;
  try{ const first=await sendPayment(key); const second=await sendPayment(key); if(first.id!==second.id) throw new Error('se generaron identificadores diferentes'); complete(3,`Reintento idempotente verificado sobre pago ${short(first.id)}.`); $('#requestResult').className='notice good'; $('#requestResult').textContent=`Dos solicitudes devolvieron el mismo pago ${short(first.id)}; no hubo duplicación.`; await refresh(); }
  catch(err){ $('#requestResult').className='notice bad'; $('#requestResult').textContent=`Prueba de idempotencia fallida: ${err.message}`; }
  finally{ button.disabled=false; }
});
$('#refreshData').addEventListener('click',refresh);
document.querySelectorAll('[data-copy]').forEach(button=>button.addEventListener('click',async()=>{ try{ await navigator.clipboard.writeText(button.dataset.copy); toast('Comando copiado'); }catch{ toast('Seleccione y copie el comando manualmente'); } }));

$('#githubForm').addEventListener('submit',async e=>{
  e.preventDefault(); const repo=$('#githubRepo').value.trim().replace(/^https?:\/\/github\.com\//,'').replace(/\/$/,''); if(!/^[\w.-]+\/[\w.-]+$/.test(repo)){toast('Use el formato owner/repository');return;}
  $('#githubResult').textContent='Consultando GitHub…';
  try{ const [runs,commit]=await Promise.all([json(`https://api.github.com/repos/${repo}/actions/runs?per_page=5`),json(`https://api.github.com/repos/${repo}/commits?per_page=1`)]); const last=runs.workflow_runs?.[0]; $('#githubResult').innerHTML=`<strong>${esc(repo)}</strong><br>Commit ${esc(short(commit[0]?.sha))} · ${last?`${esc(last.name)}: ${esc(last.conclusion||last.status)}`:'sin ejecuciones'}`; state.github=true; localStorage.setItem('bankpulse-github-repo',repo); evidence(`GitHub conectado: ${repo}.`); checkSupplyChain(); }
  catch(err){ $('#githubResult').textContent=`No disponible: ${err.message}. Verifique que el repositorio sea público.`; }
});
$('#dockerForm').addEventListener('submit',async e=>{
  e.preventDefault(); const ns=$('#dockerNamespace').value.trim(); if(!/^[a-z0-9][a-z0-9_-]+$/i.test(ns)){toast('Namespace de Docker Hub no válido');return;}
  $('#dockerResult').textContent='Consultando Docker Hub…';
  try{ const data=await json(`https://hub.docker.com/v2/repositories/${encodeURIComponent(ns)}/bankpulse-payments-api/tags?page_size=5`); const tags=(data.results||[]).map(t=>t.name); if(!tags.length) throw new Error('sin tags publicados'); $('#dockerResult').innerHTML=`<strong>${esc(ns)}/bankpulse-payments-api</strong><br>Tags: ${tags.map(esc).join(', ')}`; state.docker=true; localStorage.setItem('bankpulse-docker-ns',ns); evidence(`Docker Hub verificó ${tags.length} tag(s) publicados.`); checkSupplyChain(); }
  catch(err){ $('#dockerResult').textContent=`No disponible: ${err.message}`; }
});
function checkSupplyChain(){ save(); if(state.github&&state.docker) complete(5,'Pipeline de GitHub y artefacto de Docker Hub verificados.'); }
$('#resetProgress').addEventListener('click',()=>{ if(confirm('¿Reiniciar solo la puntuación y evidencias del laboratorio?')){ localStorage.removeItem('bankpulse-real-lab'); location.reload(); } });

$('#githubRepo').value=localStorage.getItem('bankpulse-github-repo')||'';
$('#dockerNamespace').value=localStorage.getItem('bankpulse-docker-ns')||'';
setInterval(()=>$('#clock').textContent=now(),1000); setInterval(refresh,3000); renderProgress(); renderEvidence(); refresh();
