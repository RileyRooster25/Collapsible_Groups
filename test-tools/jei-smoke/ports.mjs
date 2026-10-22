import net from 'node:net';
export async function occupied(port) {
 return new Promise((resolve,reject)=>{
  const socket=net.connect({host:'127.0.0.1',port});socket.setTimeout(500);
  socket.once('connect',()=>{socket.destroy();resolve(true);});
  socket.once('error',error=>{socket.destroy();if(error.code==='ECONNREFUSED')resolve(false);else reject(error);});
  socket.once('timeout',()=>{socket.destroy();reject(Error('Unable to determine whether test port '+port+' is occupied'));});
 });
}
