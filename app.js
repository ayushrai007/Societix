/* ============ View toggle (website / app) ============ */
function setView(v){
  var isApp = v==='app';
  document.getElementById('view-website').classList.toggle('active', !isApp);
  document.getElementById('view-app').classList.toggle('active', isApp);
  document.getElementById('tab-website').classList.toggle('active', !isApp);
  document.getElementById('tab-app').classList.toggle('active', isApp);
  document.getElementById('tab-website').setAttribute('aria-selected', String(!isApp));
  document.getElementById('tab-app').setAttribute('aria-selected', String(isApp));
  window.scrollTo({top:0, behavior:'instant' in window ? 'instant' : 'auto'});
  if(isApp) render();
}

/* ============ Icons ============ */
var I = {
  home:'<path d="M3 11l9-7 9 7"/><path d="M5 10v10h14V10"/>',
  pay:'<path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
  visitor:'<circle cx="12" cy="8" r="3.5"/><path d="M5 21a7 7 0 0 1 14 0"/><path d="M19 5l2 2 3-3" transform="translate(-4 -1)"/>',
  vote:'<path d="M9 11l3 3 7-7"/><path d="M20 12v7H4V5h11"/>',
  help:'<circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 0 1 4.5 1.5c0 1.8-2.5 2-2.5 3.5"/><circle cx="12" cy="17" r=".6" fill="currentColor"/>',
  ledger:'<path d="M3 3v18h18"/><path d="M7 14l4-4 3 3 5-6"/>',
  siren:'<path d="M12 8v4l3 2"/><circle cx="12" cy="12" r="9"/>',
  gavel:'<path d="M9 11l3 3"/><path d="M4 20l6-6"/><rect x="11" y="3" width="8" height="5" rx="1" transform="rotate(45 15 5.5)"/>',
  bot:'<rect x="4" y="8" width="16" height="11" rx="3"/><path d="M12 8V4M9 13h.01M15 13h.01"/>',
  gate:'<rect x="3" y="4" width="18" height="14" rx="2"/><path d="M8 20h8"/><circle cx="12" cy="11" r="2.5"/>',
  arrow:'<path d="M5 12h14M13 6l6 6-6 6"/>',
  shield:'<path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z"/>',
  bolt:'<path d="M13 2L4 14h7l-1 8 9-12h-7z"/>'
};
function svg(p,s){return '<svg width="'+(s||18)+'" height="'+(s||18)+'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">'+p+'</svg>';}

/* ============ App state ============ */
var ST = { role:'resident', screen:'home', voted:false };

var PERSONA = {
  resident:{ hi:'Good morning,', name:'Ananya Rao', av:'AR',
    unit:'Flat B-1204 · Maple Residency',
    tabs:[['home','Home',I.home],['pay','Pay',I.pay],['visitor','Visitors',I.visitor],['vote','AGM',I.vote],['help','Help',I.help]] },
  committee:{ hi:'Committee console,', name:'R. Menon', av:'RM',
    unit:'Treasurer · Maple Residency MC',
    tabs:[['treasury','Treasury',I.ledger],['defaulters','Dues',I.siren],['agm','Quorum',I.gavel],['copilot','Copilot',I.bot],['gate','Gate',I.gate]] }
};

/* default screen per role */
var DEFAULT_SCREEN = { resident:'home', committee:'treasury' };

/* ============ Screen renderers ============ */
var SCREENS = {
  /* ---------- RESIDENT ---------- */
  home:function(){ return ''+
    '<div class="a-card hl">'+
      '<div class="a-due"><div><div class="lab">Maintenance due · Oct 2026</div><div class="big" style="margin-top:2px">₹6,400</div></div>'+
      '<div class="st">'+svg(I.shield,13)+' Due in 4 days</div></div>'+
      '<button class="btn btn-primary btn-sm" style="width:100%;background:#0f9d6b" onclick="go(\'pay\')">Pay now · UPI / VAN</button>'+
    '</div>'+
    '<div class="quick">'+
      qa('pay',I.pay,'Pay dues')+qa('visitor',I.visitor,'Invite')+qa('vote',I.vote,'Vote AGM')+qa('help',I.help,'Help')+
    '</div>'+
    '<div class="sec-title">Community feed</div>'+
    row('em',I.vote,'AGM 2026 · e-voting open','Resolution 3 — sinking fund hike','<span class="li-badge ok">Live</span>')+
    row('gold',I.visitor,'Delivery at gate','Amazon · awaiting your approval','<span class="li-badge gold">Tap</span>')+
    row('',I.shield,'Water tank cleaning','Block B · tomorrow 10 AM','')+
    '<div class="sec-title">Your privacy</div>'+
    '<div class="a-card" style="display:flex;gap:11px;align-items:center">'+
      '<div class="li-ic em" style="width:38px;height:38px">'+svg(I.shield,18)+'</div>'+
      '<div style="flex:1"><b style="font-size:13px;color:#15224a">Directory masking on</b><br><small style="color:#5b6678;font-size:11.5px">Your number is hidden · visitor logs purge in 30 days</small></div>'+
    '</div>';
  },
  pay:function(){ return ''+
    '<div class="sec-title">Maintenance invoice · Oct 2026</div>'+
    '<div class="a-card">'+
      invRow('Common area maintenance','₹4,200')+
      invRow('Sinking fund','₹1,100')+
      invRow('Water (metered · 14 kL)','₹780')+
      invRow('Clubhouse &amp; amenities','₹320')+
      '<div style="border-top:1px dashed #d4dae6;margin-top:8px;padding-top:10px;display:flex;justify-content:space-between"><b style="font-family:var(--display);color:#0f1c3f">Total</b><b style="font-family:var(--display);color:#0f1c3f">₹6,400</b></div>'+
    '</div>'+
    '<div class="sec-title">Pay to your society account</div>'+
    '<div class="a-card" style="display:flex;gap:11px;align-items:center">'+
      '<div class="li-ic" style="width:38px;height:38px">'+svg(I.pay,18)+'</div>'+
      '<div style="flex:1"><b style="font-size:12.5px;color:#15224a">Virtual A/C · 7821 0049 1204</b><br><small style="font-size:11px;color:#5b6678">maple.b1204@societix · auto-reconciled</small></div>'+
    '</div>'+
    '<button class="btn btn-primary btn-sm" style="width:100%;margin-top:4px" onclick="toast(\'Opening UPI… funds settle to the society account.\')">Pay ₹6,400 via UPI</button>'+
    '<p style="font-size:11px;color:#8591a5;text-align:center;margin-top:12px">Settled in ~0.8s · Societix never holds your money.</p>';
  },
  visitor:function(){ return ''+
    '<div class="sec-title">Pre-approve a visitor</div>'+
    '<div class="qr-box">'+
      '<div class="qr"></div>'+
      '<b style="font-family:var(--display);color:#0f1c3f;font-size:15px">Riya\'s guest pass</b>'+
      '<div style="font-size:12px;color:#5b6678;margin-top:4px">Valid today · 6:00–9:00 PM · Gate 2</div>'+
    '</div>'+
    '<button class="btn btn-ghost btn-sm" style="width:100%" onclick="toast(\'Pass shared on WhatsApp.\')">Share pass on WhatsApp</button>'+
    '<div class="sec-title">Recent at your gate</div>'+
    row('em',I.visitor,'Swiggy · Delivery','Approved · 1:12 PM','<span class="li-badge ok">In</span>')+
    row('',I.visitor,'Mr. Kapoor · Guest','Checked out · 11:40 AM','<span class="li-badge">Out</span>')+
    row('gold',I.visitor,'Blue Dart · Courier','Awaiting approval','<span class="li-badge gold">Now</span>')+
    '<div class="a-card" style="margin-top:4px;display:flex;gap:10px;align-items:center;background:#e6f6ef;border:0">'+
      '<div style="color:#0b7d54">'+svg(I.shield,18)+'</div>'+
      '<small style="font-size:11.5px;color:#0b7d54;font-weight:600">Visitor photos &amp; numbers auto-delete after 30 days.</small>'+
    '</div>';
  },
  vote:function(){ return ''+
    '<div class="sec-title">AGM 2026 · Resolution 3</div>'+
    '<div class="a-card" style="padding:16px"><b style="font-family:var(--display);color:#0f1c3f;font-size:14.5px">Increase sinking fund by ₹2/sq.ft</b>'+
    '<p style="font-size:12px;color:#5b6678;margin-top:6px">Secret ballot · cryptographically logged · closes 6:00 PM today.</p></div>'+
    voteOpt('yes','In favour')+
    voteOpt('no','Against')+
    voteOpt('abstain','Abstain')+
    '<button class="btn btn-primary btn-sm" id="castBtn" style="width:100%;margin-top:6px" onclick="castVote()">Cast my vote</button>'+
    '<p style="font-size:11px;color:#8591a5;text-align:center;margin-top:12px">Your ballot is anonymous and tamper-evident.</p>';
  },
  help:function(){ return ''+
    '<div class="sec-title">How can we help?</div>'+
    row('red',I.siren,'Raise a complaint','Lift, water, security &amp; more','')+
    row('em',I.help,'Talk to the committee','Usually replies within 2 hours','')+
    row('gold',I.bolt,'Report an emergency','Direct line to gate &amp; guard','')+
    '<div class="sec-title">Your open tickets</div>'+
    '<div class="a-card"><div style="display:flex;justify-content:space-between;align-items:center"><div><b style="font-size:13px;color:#15224a">#T-2041 · Lift B noisy</b><br><small style="font-size:11px;color:#5b6678">Assigned to facilities</small></div><span class="li-badge gold">In progress</span></div></div>'+
    '<div class="a-card"><div style="display:flex;justify-content:space-between;align-items:center"><div><b style="font-size:13px;color:#15224a">#T-2033 · Parking tag</b><br><small style="font-size:11px;color:#5b6678">Resolved yesterday</small></div><span class="li-badge ok">Closed</span></div></div>';
  },

  /* ---------- COMMITTEE ---------- */
  treasury:function(){ return ''+
    '<div class="a-card hl">'+
      '<div class="lab">Collected · Oct cycle</div>'+
      '<div class="big">₹41.3L <small>of ₹43.8L</small></div>'+
      '<div class="a-mini-bar"><i style="width:94%"></i></div>'+
      '<div style="display:flex;justify-content:space-between;font-size:11.5px;color:#aeb9d4"><span>94.2% zero-touch matched</span><span>₹2.5L outstanding</span></div>'+
    '</div>'+
    '<div class="quick" style="grid-template-columns:repeat(3,1fr)">'+
      miniStat('Trial balance','Balanced','em')+miniStat('Sinking fund','₹1.84 Cr','')+miniStat('Vendor dues','₹92k','gold')+
    '</div>'+
    '<div class="sec-title">Live ledger <a>View audit pack</a></div>'+
    row('em',I.pay,'B-1204 · UPI in','Webhook auto-matched · 0.8s','<b style="color:#0b7d54;font-size:13px">+₹6,400</b>')+
    row('em',I.pay,'A-0702 · UPI in','Auto-allocated to Oct invoice','<b style="color:#0b7d54;font-size:13px">+₹5,900</b>')+
    row('red',I.ledger,'Lift AMC · payout','Maker-checker · TDS ₹480','<b style="color:#c0392b;font-size:13px">−₹24,000</b>')+
    row('',I.ledger,'Diesel · generator','GST ITC tracked','<b style="color:#c0392b;font-size:13px">−₹18,200</b>');
  },
  defaulters:function(){ return ''+
    '<div class="a-card" style="display:flex;justify-content:space-between;align-items:center">'+
      '<div><div class="lab" style="margin-bottom:2px">Active defaulters</div><div class="big" style="font-size:24px">7 flats · ₹2.5L</div></div>'+
      '<span class="li-badge warn">Cadence on</span>'+
    '</div>'+
    '<div class="sec-title">Escalation cadence · B-0904</div>'+
    '<div class="cadence">'+
      cadStep('done','+1','Day +1 · In-app + WhatsApp pay-link','Delivered · read')+
      cadStep('done','+10','Day +10 · Automated SMS + IVR call','Completed')+
      cadStep('active','+15','Day +15 · Statutory late interest','Compounding @ MCS 154B cap')+
      cadStep('','+30','Day +30 · Pre-drafted legal notice','Auto-generates PDF')+
    '</div>'+
    '<div class="a-card" style="margin-top:4px;background:#e6f6ef;border:0;display:flex;gap:10px;align-items:center">'+
      '<div style="color:#0b7d54">'+svg(I.bolt,18)+'</div>'+
      '<small style="font-size:11.5px;color:#0b7d54;font-weight:600">Autonomous rails recover dues 3.5× faster — no awkward WhatsApp chasing.</small>'+
    '</div>';
  },
  agm:function(){ return ''+
    '<div class="sec-title">AGM 2026 · live quorum</div>'+
    '<div class="a-card" style="text-align:center;padding:18px">'+
      '<div class="quorum-ring"><div class="qv"><b>72%</b><small>present</small></div></div>'+
      '<div style="display:flex;justify-content:center;gap:16px;font-size:12px"><span style="color:#0b7d54;font-weight:600">● 158 owners</span><span style="color:#5b6678;font-weight:600">● 23 proxies</span></div>'+
      '<div style="margin-top:10px;font-size:12px;color:#0b7d54;font-weight:600">Quorum met · resolutions can proceed</div>'+
    '</div>'+
    '<div class="sec-title">Resolutions</div>'+
    row('ok'=='ok'?'em':'',I.vote,'R1 · Approve FY26 accounts','Passed · 88% in favour','<span class="li-badge ok">Passed</span>')+
    row('em',I.vote,'R2 · Re-elect committee','Secret ballot · logged','<span class="li-badge ok">Passed</span>')+
    row('gold',I.gavel,'R3 · Sinking fund hike','Voting open · closes 6 PM','<span class="li-badge gold">Live</span>')+
    '<button class="btn btn-ghost btn-sm" style="width:100%;margin-top:4px" onclick="toast(\'Draft minutes synthesised by AI Copilot.\')">Auto-draft minutes</button>';
  },
  copilot:function(){ return ''+
    '<div class="sec-title">AI Committee Copilot</div>'+
    '<div style="padding:4px 2px">'+
      '<div class="copilot-you">Compare generator diesel spend — Q2 2025 vs Q2 2026.</div>'+
      '<div class="copilot-msg">Diesel spend fell <b>18.4%</b> — ₹1.71L (Q2 25) → ₹1.39L (Q2 26), despite 6% higher runtime. Driver: the May 2026 AMC renegotiation. Want the P&amp;L line export?</div>'+
      '<div style="margin:6px 0 2px">'+
        chip('List units over 60 days overdue')+chip('Can we allow EV chargers?')+chip('Draft the AGM notice')+
      '</div>'+
    '</div>'+
    '<div class="copilot-input"><input placeholder="Ask about finances, bylaws, vendors…" aria-label="Ask copilot"><button class="send" onclick="toast(\'Copilot is thinking…\')">'+svg(I.arrow,16)+'</button></div>'+
    '<p style="font-size:11px;color:#8591a5;text-align:center;margin-top:12px">Answers from your ledger &amp; state bylaws · &lt; 2.5s.</p>';
  },
  gate:function(){ return ''+
    '<div class="a-card" style="display:flex;justify-content:space-between;align-items:center">'+
      '<div><div class="lab" style="margin-bottom:2px">Gate status</div><b style="font-family:var(--display);color:#0f1c3f;font-size:16px">2 gates online</b></div>'+
      '<span class="li-badge ok">● Synced</span>'+
    '</div>'+
    '<div class="sec-title">Live gate log</div>'+
    row('em',I.visitor,'Swiggy · Delivery','Gate 2 · B-1204 · 1:12 PM','<span class="li-badge ok">In</span>')+
    row('',I.gate,'KA-01-MH-8841','ANPR · resident vehicle','<span class="li-badge ok">Auto</span>')+
    row('gold',I.visitor,'Blue Dart · Courier','Awaiting B-0702 approval','<span class="li-badge gold">Wait</span>')+
    '<div class="a-card" style="margin-top:4px;background:#0f1c3f;border:0;color:#fff;display:flex;gap:11px;align-items:center">'+
      '<div style="color:#8ff0c6">'+svg(I.shield,18)+'</div>'+
      '<div><b style="font-size:12.5px;color:#fff">Offline-ready · SQLite edge</b><br><small style="font-size:11px;color:#aeb9d4">Logs auto-purge in 30 days · zero ads</small></div>'+
    '</div>';
  }
};

/* ---- small html helpers ---- */
function qa(screen,icon,label){return '<a onclick="go(\''+screen+'\')"><span class="qi">'+svg(icon,17)+'</span><small>'+label+'</small></a>';}
function row(tone,icon,title,sub,meta){
  return '<div class="list-row"><div class="li-ic '+tone+'">'+svg(icon,17)+'</div>'+
    '<div class="li-tx"><b>'+title+'</b><small>'+sub+'</small></div>'+
    (meta?'<div class="li-meta">'+meta+'</div>':'')+'</div>';
}
function invRow(l,v){return '<div style="display:flex;justify-content:space-between;padding:7px 0;font-size:13px;color:#5b6678"><span>'+l+'</span><span style="color:#15224a;font-weight:600">'+v+'</span></div>';}
function miniStat(l,v,tone){var c=tone==='em'?'#0b7d54':tone==='gold'?'#8a6d2a':'#0f1c3f';return '<div style="background:#fff;border:1px solid #e4e8f0;border-radius:14px;padding:11px 8px;text-align:center"><div style="font-size:10px;color:#8591a5;font-weight:700;letter-spacing:.04em;text-transform:uppercase">'+l+'</div><div style="font-family:var(--display);font-weight:800;font-size:16px;color:'+c+';margin-top:3px">'+v+'</div></div>';}
function cadStep(state,n,title,sub){return '<div class="cad-step '+state+'"><div class="dot">'+n+'</div><div class="ct"><b>'+title+'</b><small>'+sub+'</small></div></div>';}
function voteOpt(id,label){var sel = ST.voteSel===id?' sel':'';return '<div class="vote-opt'+sel+'" onclick="pickVote(\''+id+'\')"><div class="rb"></div><b>'+label+'</b></div>';}
function chip(t){return '<span class="copilot-chip" onclick="toast(\'Copilot is thinking…\')">'+t+'</span>';}

/* ---- vote interactions ---- */
function pickVote(id){ ST.voteSel=id; renderBody(); }
function castVote(){
  if(!ST.voteSel){ toast('Select an option first.'); return; }
  toast('Vote cast securely · tamper-evident receipt sent.');
}

/* ============ Feature side panel copy ============ */
var FEATS = {
  resident:{ eyebrow:'Resident experience', h3:'Everything in a tap — nothing tracked.',
    intro:'Pay maintenance, pre-approve guests, and vote in the AGM from one calm, ad-free app.',
    rows:[
      [I.pay,false,'Pay in one tap','Dedicated virtual account &amp; UPI VPA per flat — settles to the society account in under a second.'],
      [I.visitor,false,'Pre-approve &amp; QR passes','Share guest &amp; delivery passes on WhatsApp; photos and numbers auto-purge in 30 days.'],
      [I.vote,false,'Binding e-AGM voting','Anonymous, cryptographically logged ballots — no more disputed hand counts.'],
      [I.shield,true,'Privacy by default','Directory masking, zero ad SDKs, and granular consent you control.']
    ]},
  committee:{ eyebrow:'Committee console', h3:'Run the society in under 45 minutes a week.',
    intro:'Autonomous treasury, self-running defaulter rails, live quorums, and an AI copilot for the books.',
    rows:[
      [I.ledger,false,'Autopilot treasury','Webhook auto-matching with 99.8% zero-touch allocation and a real-time double-entry ledger.'],
      [I.siren,false,'Defaulter escalation rails','WhatsApp → IVR → statutory interest → legal notice, fully automated. Dues recovered 3.5× faster.'],
      [I.gavel,false,'Legally binding e-AGMs','Dynamic quorum engine with validated proxy weights and audited secret balloting.'],
      [I.bot,true,'AI committee copilot','Query the ledger in plain language, auto-draft minutes, and resolve state-bylaw conflicts.']
    ]}
};

/* ============ Render ============ */
function renderBody(){ document.getElementById('app-body').innerHTML = SCREENS[ST.screen](); }

function render(){
  var p = PERSONA[ST.role];
  document.getElementById('app-hi').textContent = p.hi;
  document.getElementById('app-name').textContent = p.name;
  document.getElementById('app-av').textContent = p.av;
  document.getElementById('app-unit').innerHTML =
    '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M3 21h18M5 21V8l7-5 7 5v13"/></svg> '+p.unit;

  // tab bar
  document.getElementById('app-tabbar').innerHTML = p.tabs.map(function(t){
    var active = t[0]===ST.screen ? ' class="active"' : '';
    return '<button'+active+' onclick="go(\''+t[0]+'\')">'+svg(t[2],20)+'<span>'+t[1]+'</span></button>';
  }).join('');

  renderBody();

  // feature side
  var f = FEATS[ST.role];
  document.getElementById('app-feats').innerHTML =
    '<span class="eyebrow">'+f.eyebrow+'</span>'+
    '<h3>'+f.h3+'</h3><p>'+f.intro+'</p>'+
    f.rows.map(function(r){
      return '<div class="feat-row'+(r[1]?' gold':'')+'"><div class="fic">'+svg(r[0],20)+'</div>'+
        '<div><b>'+r[2]+'</b><p>'+r[3]+'</p></div></div>';
    }).join('');
}

function setRole(role){
  ST.role = role;
  ST.screen = DEFAULT_SCREEN[role];
  document.getElementById('role-resident').classList.toggle('active', role==='resident');
  document.getElementById('role-committee').classList.toggle('active', role==='committee');
  render();
}
function go(screen){ ST.screen = screen; render(); document.getElementById('app-body').scrollTop=0; }

/* ============ Toast ============ */
var toastTimer;
function toast(msg){
  var t = document.getElementById('toast');
  if(!t){ t=document.createElement('div'); t.id='toast';
    t.style.cssText='position:fixed;left:50%;bottom:34px;transform:translateX(-50%) translateY(20px);background:#0f1c3f;color:#fff;padding:13px 20px;border-radius:12px;font-size:13.5px;font-weight:600;box-shadow:0 20px 40px -14px rgba(15,28,63,.5);z-index:999;opacity:0;transition:.25s;max-width:88vw;text-align:center';
    document.body.appendChild(t);
  }
  t.textContent=msg;
  requestAnimationFrame(function(){ t.style.opacity='1'; t.style.transform='translateX(-50%) translateY(0)'; });
  clearTimeout(toastTimer);
  toastTimer=setTimeout(function(){ t.style.opacity='0'; t.style.transform='translateX(-50%) translateY(20px)'; },2600);
}

/* ============ Demo form ============ */
function demoInit(){
  var form = document.getElementById('demo-form');
  if(!form) return;
  form.addEventListener('submit', function(e){
    e.preventDefault();
    var name = document.getElementById('df-name');
    var society = document.getElementById('df-society');
    var contact = document.getElementById('df-contact');
    var note = document.getElementById('df-note');
    [name,society,contact].forEach(function(el){ el.classList.remove('invalid'); });

    var missing = [name,society,contact].filter(function(el){ return !el.value.trim(); });
    var c = contact.value.trim();
    var contactOk = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(c) || /^[0-9][0-9\s\-]{7,}$/.test(c);

    if(missing.length || !contactOk){
      missing.forEach(function(el){ el.classList.add('invalid'); });
      if(c && !contactOk) contact.classList.add('invalid');
      note.textContent = 'Please add your name, society, and a valid email or phone number.';
      note.classList.add('err');
      (missing[0]||contact).focus();
      return;
    }

    // "Capture" the lead — persisted locally so a refresh remembers it; swap for an API POST in production.
    var lead = {
      name:name.value.trim(), society:society.value.trim(),
      units:(document.getElementById('df-units').value||'').trim(), contact:c,
      at:new Date().toISOString()
    };
    try{
      var all = JSON.parse(localStorage.getItem('societix_leads')||'[]');
      all.push(lead); localStorage.setItem('societix_leads', JSON.stringify(all));
    }catch(err){ /* storage may be blocked — confirmation still shows */ }

    document.getElementById('dd-msg').textContent =
      'Thanks, '+lead.name.split(' ')[0]+' — we’ll reach out about '+lead.society+' within one business day to lock in your walkthrough.';
    form.hidden = true;
    document.getElementById('demo-done').hidden = false;
  });
}
function resetDemo(){
  var form = document.getElementById('demo-form');
  form.reset();
  document.getElementById('df-note').classList.remove('err');
  document.getElementById('df-note').textContent = 'No ads, no spam — your details are used only to schedule the walkthrough.';
  document.getElementById('demo-done').hidden = true;
  form.hidden = false;
  document.getElementById('df-name').focus();
}
demoInit();

/* close mobile nav on anchor click is native; initial render */
render();
