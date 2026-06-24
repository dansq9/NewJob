import { chromium } from 'playwright';
import http from 'http';
import fs from 'fs';
import path from 'path';

const DIR = process.cwd();
const MIME = {'.html':'text/html','.js':'text/javascript','.ttf':'font/ttf','.css':'text/css'};
const server = http.createServer((req,res)=>{
  let p = decodeURIComponent(req.url.split('?')[0]);
  if (p==='/') p='/Ascend.render.html';
  const fp = path.join(DIR, p);
  if (!fp.startsWith(DIR) || !fs.existsSync(fp)) { res.writeHead(404); return res.end('nf'); }
  res.writeHead(200,{'Content-Type':MIME[path.extname(fp)]||'application/octet-stream'});
  fs.createReadStream(fp).pipe(res);
});
await new Promise(r=>server.listen(0,r));
const PORT = server.address().port;
const URL = `http://127.0.0.1:${PORT}/Ascend.render.html`;

const browser = await chromium.launch({
  executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome',
  headless: true, args:['--no-sandbox','--disable-dev-shm-usage','--force-color-profile=srgb']
});
const ctx = await browser.newContext({ viewport:{width:480,height:980}, deviceScaleFactor:3 });
const page = await ctx.newPage();
page.on('console', m=>{ if(m.type()==='error') console.log('  [page error]', m.text()); });
await page.goto(URL, { waitUntil:'load' });
await page.waitForFunction(()=>window.__asc && document.querySelector('[data-asc-device]'), null, {timeout:20000});
await page.evaluate(()=>document.fonts.ready);
console.log('booted, instance ready');

// unlock paywalls
await page.evaluate(()=>window.__asc.setState({pro:true, onboarded:true}));

const OUT = path.join(DIR,'shots'); fs.mkdirSync(OUT,{recursive:true});
async function shot(name){
  await page.evaluate(()=>document.fonts.ready);
  await page.waitForTimeout(450);
  const el = await page.$('[data-asc-device]');
  await el.screenshot({ path: path.join(OUT, name+'.png') });
  console.log('  shot', name);
}

const steps = {
  home:    a=>a.replace('home'),
  search:  a=>{ a.replace('search'); a.setState({query:'Product Manager', searchLoading:false, jobsLoaded:6}); },
  tracker: a=>{ a.setState({tracker:{4:'saved',6:'saved',8:'saved',2:'applied',7:'applied',3:'applied',1:'interview',5:'interview'}}); a.replace('tracker'); },
  optimizer: a=>{ a.replace('optimizer'); a.setState({optTarget:1, optPhase:'results', optScore:93, optFixed:true}); },
  mock:    a=>{ a.replace('mock'); a._doBeginMock(); a.setState({mockTargetRole:'Senior Product Manager', mockIdx:2, mockMode:'type', mockText:"I'd clarify scale and consistency needs first, then sketch a consistent-hashing router with read replicas and automatic failover to cut p99 latency by ~35%."}); },
};

for (const [name, fn] of Object.entries(steps)){
  await page.evaluate(`(${fn.toString()})(window.__asc)`);
  await page.waitForTimeout(600);
  await shot(name);
}

// copilot: drive the real live simulation then wait for the answer to appear
await page.evaluate(()=>{ const a=window.__asc; a.replace('copilot'); a.setState({coRole:'Senior Product Manager', coCompany:'Northwind'}); a.startCopilotSession(); });
await page.waitForFunction(()=>window.__asc.state.coAnswer, null, {timeout:12000}).catch(()=>console.log('  (coAnswer timeout, capturing anyway)'));
await page.waitForTimeout(900);
await shot('copilot');

await browser.close(); server.close();
console.log('DONE');
