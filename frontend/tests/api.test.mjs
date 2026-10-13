import test from 'node:test'
import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'

const endpoint='https://example.test/api'
const planner=new URL('../src/utils/planner.mjs',import.meta.url).href
async function loadApi() {
  let source=await readFile(new URL('../src/api/index.js',import.meta.url),'utf8')
  source=source.replace("from '../utils/planner.mjs'",`from '${planner}'`)
  source=source.replaceAll('import.meta.env',`({VITE_API_MODE:'server',VITE_API_BASE_URL:'${endpoint}'})`)
  return import('data:text/javascript;base64,'+Buffer.from(source+'\n// '+Math.random()).toString('base64'))
}
test('concurrent cloud requests share one WeChat login and ignore local login tokens',async()=>{
  const storage=new Map([['dkm-token','old-local-token']])
  let logins=0,configs=0,exchanges=0
  globalThis.uni={
    getStorageSync:k=>storage.get(k),setStorageSync:(k,v)=>storage.set(k,v),removeStorageSync:k=>storage.delete(k),
    login:({success})=>{logins++;queueMicrotask(()=>success({code:'test-code'}))},
    request:({url,success,header})=>queueMicrotask(()=>{
      if(url.endsWith('/config')){configs++;success({statusCode:200,data:{demoLogin:false}})}
      else if(url.endsWith('/auth/login')){exchanges++;success({statusCode:200,data:{token:'test-cloud-token'}})}
      else {assert.equal(header.Authorization,'Bearer test-cloud-token');success({statusCode:200,data:[]})}
    })
  }
  const {api}=await loadApi()
  await Promise.all([api.plans(),api.plans()])
  assert.equal(configs,1);assert.equal(logins,1);assert.equal(exchanges,1)
  assert.equal(storage.get('dkm-token:'+endpoint),'test-cloud-token')
  assert.equal(storage.get('dkm-token'),'old-local-token')
})
test('expired cloud tokens are removed without affecting another backend or local plan selection',async()=>{
  const storage=new Map([['dkm-token:'+endpoint,'expired'],['dkm-token:https://other.test/api','other'],['dkm-selected','local-plan']])
  globalThis.uni={getStorageSync:k=>storage.get(k),setStorageSync:(k,v)=>storage.set(k,v),removeStorageSync:k=>storage.delete(k),request:({success})=>success({statusCode:401,data:{message:'登录已过期'}})}
  const {api,select,selected}=await loadApi()
  await assert.rejects(api.plans(),/登录已过期/)
  assert.equal(storage.has('dkm-token:'+endpoint),false)
  assert.equal(storage.get('dkm-token:https://other.test/api'),'other')
  assert.equal(selected(),'')
  select('cloud-plan');assert.equal(selected(),'cloud-plan');assert.equal(storage.get('dkm-selected'),'local-plan')
})
test('domain validation failures show an actionable message without echoing request details',async()=>{
  globalThis.uni={getStorageSync:()=>'',request:({fail})=>fail({errMsg:'request:fail url not in domain list; sensitive-request-detail'})}
  const {request}=await loadApi()
  await assert.rejects(request('/config'),e=>e.message.includes('request 合法域名')&&!e.message.includes('sensitive-request-detail'))
})
