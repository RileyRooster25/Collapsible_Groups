import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import http from 'node:http';
import {spawn} from 'node:child_process';
import {createHash,randomUUID} from 'node:crypto';

const minecraft=(await fs.readFile(new URL('../../gradle.properties',import.meta.url),'utf8')).match(/^minecraft_version=(.+)$/m)[1].trim();
const listen=server=>new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});
const close=server=>new Promise((resolve,reject)=>{server.closeAllConnections?.();server.close(error=>error?reject(error):resolve());});

async function fixture(run) {
 const temporaryRoot=await fs.realpath(os.tmpdir());
 const directory=await fs.mkdtemp(path.join(temporaryRoot,'cg-smoke-runner-'));
 assert.equal(path.dirname(directory),temporaryRoot);
 try {
  for(const file of ['run.mjs','ports.mjs'])await fs.copyFile(new URL(file,import.meta.url),path.join(directory,file));
  const gameDir=path.join(directory,'minecraft');await fs.mkdir(path.join(gameDir,'mods'),{recursive:true});
  const bytes=Buffer.from('unit-test artifact; never loaded by Minecraft');
  await fs.writeFile(path.join(gameDir,'mods/test.jar'),bytes);
  const session=randomUUID();await fs.writeFile(path.join(gameDir,'.cg-smoke-owned.json'),JSON.stringify({session}));
  await fs.mkdir(path.join(directory,'profiles'));
  const manifest={session,port:0,instance:'CG smoke runner unit fixture',gameDir,mods:{test:{file:'test.jar',sha256:createHash('sha256').update(bytes).digest('hex')}}};
  async function execute(args) {
   await fs.writeFile(path.join(directory,'local.json'),JSON.stringify({prism:process.execPath,prismData:directory}));
   await fs.writeFile(path.join(directory,'profiles/forge.json'),JSON.stringify(manifest));
   const result=await new Promise((resolve,reject)=>{
    const child=spawn(process.execPath,[path.join(directory,'run.mjs'),'forge',...args],{cwd:directory,windowsHide:true,stdio:['ignore','pipe','pipe'],timeout:20000});
    let stdout='',stderr='';child.stdout.on('data',b=>stdout+=b);child.stderr.on('data',b=>stderr+=b);
    child.once('error',reject);child.once('exit',(code,signal)=>resolve({code,signal,stdout,stderr}));
   });
   assert.equal(result.signal,null,result.stderr);assert.equal(result.code,1,result.stdout+result.stderr);
   const entries=await fs.readdir(path.join(directory,'evidence'));assert.equal(entries.length,1);
   return JSON.parse(await fs.readFile(path.join(directory,'evidence',entries[0],'result.json'),'utf8'));
  }
  await run({manifest,execute,directory});
 } finally {
  assert.equal(path.dirname(path.resolve(directory)),temporaryRoot);
  assert(path.basename(directory).startsWith('cg-smoke-runner-'));
  await fs.rm(directory,{recursive:true});
 }
}

for(const mismatch of ['session','gameDir'])test('identity mismatch never sends Quit: '+mismatch,async()=>fixture(async({manifest,execute,directory})=>{
 const methods=[];const otherGame=path.join(directory,'other-game');await fs.mkdir(otherGame);
 const server=http.createServer(async(request,response)=>{
  let body='';for await(const b of request)body+=b;
  const {method}=JSON.parse(body);methods.push(method);
  const identity={session:manifest.session,minecraft,pid:1073741824,gameDir:manifest.gameDir};
  identity[mismatch]=mismatch==='session'?'different-session':otherGame;
  response.setHeader('Content-Type','application/json');response.end(JSON.stringify({result:method==='identity'?identity:{png:''}}));
 });
 await listen(server);manifest.port=server.address().port;
 try {
  const report=await execute(['--attach']);assert.equal(report.status,'failed');
  assert.equal(report.shutdownSkipped,'Runtime identity was not verified');assert(!methods.includes('quit'));
 } finally {await close(server);}
}));

test('a failed configured launcher is reported before the startup timeout',async()=>fixture(async({manifest,execute})=>{
 const server=http.createServer();await listen(server);manifest.port=server.address().port;await close(server);
 const report=await execute([]);assert.equal(report.status,'failed');assert.match(report.error,/Configured launcher failed/);
}));
