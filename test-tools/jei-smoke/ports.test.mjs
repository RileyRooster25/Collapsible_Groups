import {test} from 'node:test';
import assert from 'node:assert/strict';
import net from 'node:net';
import {occupied} from './ports.mjs';
test('detects a listening test port and its normal closure',async()=>{
 const server=net.createServer(socket=>socket.end());
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});
 const port=server.address().port;
 try{assert.equal(await occupied(port),true);}finally{await new Promise((resolve,reject)=>server.close(error=>error?reject(error):resolve()));}
 assert.equal(await occupied(port),false);
});
test('invalid ports cannot be treated as an available test port',async()=>{
 await assert.rejects(occupied(-1));
});
