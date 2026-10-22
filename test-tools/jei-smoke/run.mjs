import {occupied} from './ports.mjs';
import fs from 'node:fs/promises';import path from 'node:path';import {spawn} from 'node:child_process';import {createHash} from 'node:crypto';import assert from 'node:assert/strict';import {fileURLToPath} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url)),local=JSON.parse(await fs.readFile(path.join(root,'local.json'),'utf8')),sleep=ms=>new Promise(r=>setTimeout(r,ms));
const loader=process.argv[2];assert(['forge'].includes(loader));const m=JSON.parse(await fs.readFile(path.join(root,'profiles',loader+'.json'),'utf8'));assert.equal(JSON.parse(await fs.readFile(path.join(m.gameDir,'.cg-smoke-owned.json'),'utf8')).session,m.session);
const hash=b=>createHash('sha256').update(b).digest('hex');for(const mod of Object.values(m.mods))assert.equal(hash(await fs.readFile(path.join(m.gameDir,'mods',mod.file))),mod.sha256);
assert.deepEqual((await fs.readdir(path.join(m.gameDir,'mods'))).sort(),Object.values(m.mods).map(x=>x.file).sort());
const folder=path.join(root,'evidence',loader+'-'+new Date().toISOString().replaceAll(/[:.]/g,'-'));await fs.mkdir(folder,{recursive:true});
const report={status:'running',manifest:m,cases:[]};let identity,launcherError,ownedIdentity=false;
async function rpc(method,params={},timeout=120000){const r=await fetch('http://127.0.0.1:'+m.port+'/rpc',{method:'POST',headers:{Authorization:'Bearer '+m.session},body:JSON.stringify({method,params}),signal:AbortSignal.timeout(timeout)});assert(r.ok);const data=await r.json();if(data.error)throw Error(data.error);return data.result;}
async function save(){await fs.writeFile(path.join(folder,'result.json'),JSON.stringify(report,null,2));}
async function stable(predicate,timeout=30000){const end=Date.now()+timeout,samples=[];let s;while(Date.now()<end){s=await rpc('observe');if(s.error)throw Error(s.error);if(s.fresh&&predicate(s)){if(samples.every(v=>v.frame!==s.frame&&v.tick!==s.tick))samples.push(s);if(samples.length===3)return samples;}else samples.length=0;await sleep(150);}await fs.writeFile(path.join(folder,'failure-state.json'),JSON.stringify(s,null,2));throw Error('State timeout');}
async function check(name,predicate){report.phase=name;const samples=await stable(predicate);report.cases.push({name,status:'passed',samples});await save();return samples.at(-1);}
async function input(params){const r=await rpc('input',params);assert.equal(r.route,'MouseHandler/KeyboardHandler');await sleep(200);}
const center=r=>({x:r.x+r.w/2,y:r.y+r.h/2});const click=r=>input({action:'click',...center(r)});const text=(r,value)=>input({action:'text',...center(r),text:value,previousLength:[...r.value].length});
async function shot(name){await sleep(200);const image=await rpc('screenshot');await fs.writeFile(path.join(folder,name+'.png'),Buffer.from(image.png,'base64'));}
const header=s=>s.cells?.find(c=>c.role==='header'&&c.groupId==='jei_smoke_latest');
async function find(){let s=(await stable(s=>s.indexReady&&s.search)).at(-1);for(let n=0;n<35;n++){if(header(s))return s;assert(s.cells.length);await input({action:'scroll',...center(s.cells[0]),dy:-1});s=(await stable(s=>s.indexReady&&s.search)).at(-1);}throw Error('Header not found');}
try{
 if(!process.argv.includes('--attach')) {
 assert.equal(await occupied(m.port),false,'Test port is already occupied; refusing another launch');
 try{await rpc('identity',{},800);throw Error('Test client already running');}catch(e){if(String(e).includes('already running'))throw e;}
 const child=spawn(local.prism,['--dir',local.prismData,'--launch',m.instance],{windowsHide:true,detached:false,stdio:'ignore'});await new Promise((res,rej)=>{child.once('spawn',res);child.once('error',rej);});child.once('exit',(code,signal)=>{if(code!==0)launcherError=Error('Configured launcher failed: '+(signal??code));});child.unref();
 }
 const until=Date.now()+180000;while(Date.now()<until){if(launcherError)throw launcherError;try{identity=await rpc('identity',{},1000);break;}catch{await sleep(1000);}}assert(identity,'Client bridge unavailable');report.identity=identity;assert.equal(identity.session,m.session);assert.equal(identity.minecraft,'1.21.1');assert.equal((await fs.realpath(identity.gameDir)).toLowerCase(),(await fs.realpath(m.gameDir)).toLowerCase());assert(Number.isSafeInteger(identity.pid)&&identity.pid>0);ownedIdentity=true;
 await stable(s=>!s.inWorld);await rpc('world.create');await stable(s=>s.inWorld&&s.indexReady,90000);await input({action:'key',key:69});let s=await check('viewer-search-and-manager-visible',s=>s.search&&s.managerButton&&s.cells.length);
 s=await find();await click(header(s));s=await check('expand-group',s=>header(s)?.expanded&&['stone','dirt','grass_block'].every(id=>s.cells.some(c=>c.id==='item:minecraft:'+id)));await shot('expanded');
 await click(header(s));s=await check('collapse-group',s=>header(s)&&!header(s).expanded&&!s.cells.some(c=>c.role==='child'&&c.groupId==='jei_smoke_latest'));
 await click(s.managerButton);await check('manager-opens',s=>s.screen==='GroupManagerScreen');await shot('manager');await input({action:'key',key:256});s=(await stable(s=>s.search&&s.managerButton)).at(-1);
 await text(s.search,'cg_no_such_item_93df2e');s=await check('empty-search-retains-manager-button',s=>s.search?.value==='cg_no_such_item_93df2e'&&s.cells.length===0&&s.managerButton);await shot('empty-search');
 await click(s.managerButton);await check('manager-opens-from-empty-search',s=>s.screen==='GroupManagerScreen');await input({action:'key',key:256});s=(await stable(s=>s.search&&s.managerButton)).at(-1);
 await text(s.search,'stone');s=await check('positive-search-ungroups-single-match',s=>s.search?.value==='stone'&&s.cells.some(c=>c.id==='item:minecraft:stone')&&!header(s));await shot('search-positive');
 await text(s.search,'');s=await find();await check('clear-search-restores-collapsed-group',s=>s.search?.value===''&&header(s)&&!header(s).expanded);report.status='passed';
}catch(e){report.status='failed';report.error=String(e);report.stack=e.stack;try{await shot('failure');}catch{}}
finally{
 if(identity&&ownedIdentity){try{await rpc('quit',{},30000);}catch(e){report.quitRpc=String(e);}const alive=()=>{try{process.kill(identity.pid,0);return true;}catch(e){if(e.code==='ESRCH')return false;throw e;}};for(let n=0;n<150&&alive();n++)await sleep(200);report.processExited=!alive();if(!report.processExited)report.status='failed';}else if(identity)report.shutdownSkipped='Runtime identity was not verified';
 const log=await fs.readFile(path.join(m.gameDir,'logs/latest.log'),'utf8').catch(()=> '');report.normalShutdown=log.includes('Stopping!');if(!report.normalShutdown)report.status='failed';await fs.writeFile(path.join(folder,'latest.log'),log);await save();
}
console.log(JSON.stringify({status:report.status,loader,cases:report.cases.length,phase:report.phase,error:report.error,folder}));if(report.status!=='passed')process.exitCode=1;
