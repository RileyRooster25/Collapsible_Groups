import {test} from 'node:test';
import assert from 'node:assert/strict';
import {verifyRuntime} from './session.mjs';
const manifest={loaderVersion:'21.1.249',mods:{cgbridgeprobe:{version:'0.1.0'}}};
const runtime=()=>({protocol:1,probeVersion:'0.1.0',pid:123,loader:'neoforge',mcVersion:'1.21.1',mods:{minecraft:{version:'1.21.1'},neoforge:{version:'21.1.249'}}});
test('accepts matching runtime metadata',()=>verifyRuntime(runtime(),manifest));
test('Fabric verifies its actual loader entry',()=>{
 const m={...manifest,loader:'fabric',loaderVersion:'0.16.14'};
 const r={...runtime(),loader:'fabric',mods:{minecraft:{version:'1.21.1'},fabricloader:{version:'0.16.14'}}};
 verifyRuntime(r,m);r.mods.fabricloader.version='0.16.9';assert.throws(()=>verifyRuntime(r,m));
});
test('rejects actual Minecraft mismatch even when the probe target is correct',()=>{
 const wrong=runtime();wrong.mods.minecraft.version='1.21.2';
 assert.throws(()=>verifyRuntime(wrong,manifest));
});
test('rejects invalid PID, protocol and mismatched probe or loader',()=>{
 for(const mutate of [r=>r.pid=0,r=>r.protocol=2,r=>r.probeVersion='0.2.0',r=>r.mods.neoforge.version='21.1.238']) {
  const wrong=runtime();mutate(wrong);assert.throws(()=>verifyRuntime(wrong,manifest));
 }
});
