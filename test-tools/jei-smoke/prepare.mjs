import fs from 'node:fs/promises';import path from 'node:path';import {randomUUID,createHash} from 'node:crypto';import assert from 'node:assert/strict';import {fileURLToPath} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));const local=JSON.parse(await fs.readFile(path.join(root,'local.json'),'utf8'));
const hash=b=>createHash('sha256').update(b).digest('hex');const props=Object.fromEntries((await fs.readFile(path.join(root,'../../gradle.properties'),'utf8')).split(/\r?\n/).filter(l=>l.includes('=')&&!l.startsWith('#')).map(l=>[l.slice(0,l.indexOf('=')),l.slice(l.indexOf('=')+1)]));
await fs.mkdir(path.join(root,'profiles'),{recursive:true});
for(const loader of process.argv.slice(2)){
 assert(['forge','fabric'].includes(loader));const session=randomUUID(),port=loader==='forge'?25741:25742,instance='CG JEI Smoke 1.20.1 '+loader+' '+local.instanceSuffix;
 const target=path.join(local.prismData,'instances',instance),gameDir=path.join(target,'minecraft');try{await fs.access(target);throw Error('Instance already exists');}catch(e){if(e.code!=='ENOENT')throw e;}
 const staging=path.join(local.prismData,'cg-smoke-staging-'+session),game=path.join(staging,'minecraft');await fs.mkdir(path.join(game,'mods'),{recursive:true});
 const source={collapsible_groups:path.resolve(root,'../../'+loader+'/build/libs/collapsible_groups-'+loader+'-1.20.1-'+props.version+'.jar'),cgsmoke:path.join(root,loader,'build/libs/cg-jei-smoke-1.20.1-'+loader+'-0.1.0.jar'),jei:local[loader].jei,...(loader==='fabric'?{'fabric-api':local.fabric.api}:{})};
 const mods={};for(const [id,p] of Object.entries(source)){const bytes=await fs.readFile(p),file=path.basename(p);mods[id]={file,source:p,sha256:hash(bytes)};await fs.writeFile(path.join(game,'mods',file),bytes);}
 const manifest={loader,session,port,instance,gameDir,mods,jei:props.jei_version};
 const pack={formatVersion:1,components:[{uid:'net.minecraft',version:'1.20.1',important:true},{uid:loader==='forge'?'net.minecraftforge':'net.fabricmc.fabric-loader',version:loader==='forge'?'47.4.23':props.fabric_loader_version,important:true}]};
 await fs.writeFile(path.join(staging,'mmc-pack.json'),JSON.stringify(pack,null,2));
 await fs.writeFile(path.join(game,'.cg-smoke-owned.json'),JSON.stringify({session}));
 const config=path.join(game,'config/collapsiblegroups/groups');await fs.mkdir(config,{recursive:true});
 await fs.writeFile(path.join(config,'jei_smoke_latest.json'),JSON.stringify({id:'jei_smoke_latest',name:{key:'group.jei_smoke_latest',fallback:'JEI smoke latest'},enabled:true,filter:{any:['stone','dirt','grass_block'].map(id=>({type:'item',id:'minecraft:'+id}))}}));
 await fs.writeFile(path.join(game,'options.txt'),'lang:en_us\nguiScale:2\npauseOnLostFocus:false\nmaxFps:60\nrenderDistance:4\nsimulationDistance:5\nenableVsync:false\nonboardAccessibility:false\nsoundCategory_master:0.0\ntutorialStep:none\n');
 const args='-Dfile.encoding=UTF-8 -Djdk.net.unixdomain.tmpdir=D:/cg-bridge-no-socket -Dcgsmoke.enabled=true -Dcgsmoke.session='+session+' -Dcgsmoke.port='+port;
 await fs.writeFile(path.join(staging,'instance.cfg'),['[General]','ConfigVersion=1.3','InstanceType=OneSix','name='+instance,'OverrideJavaLocation=true','JavaPath='+local.java,'AutomaticJava=false','OverrideJavaArgs=true','JvmArgs="'+args+'"','OverrideMemory=true','MinMemAlloc=1024','MaxMemAlloc=4096','OverrideWindow=true','MinecraftWinWidth=1280','MinecraftWinHeight=800','LaunchMaximized=false','OverrideCommands=true','PreLaunchCommand=','PostExitCommand=','WrapperCommand=','ShowConsole=false','ShowConsoleOnError=true','UseAccountForInstance=false'].join('\n')+'\n');
 await fs.rename(staging,target);await fs.writeFile(path.join(root,'profiles',loader+'.json'),JSON.stringify(manifest,null,2));console.log(JSON.stringify({loader,instance,jei:props.jei_version}));
}
