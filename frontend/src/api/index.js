import {templates,generate,today,daysBetween,addDays} from '../utils/planner.mjs'
export const localMode=import.meta.env.VITE_API_MODE!=='server'
const base=import.meta.env.VITE_API_BASE_URL||'http://localhost:8080/api',KEY='dkm-local-plans-v1'
const sessionKey='dkm-token:'+base,selectedKey=localMode?'dkm-selected':'dkm-selected:'+base
let loginPending=null
let cloudInitialized=false
const cloudEnv=import.meta.env.VITE_CLOUD_ENV||'',cloudService=import.meta.env.VITE_CLOUD_SERVICE||''
export const selected=()=>uni.getStorageSync(selectedKey)||''
export function select(id){uni.setStorageSync(selectedKey,id)}
function read(){return uni.getStorageSync(KEY)||[]}
function save(plans){try{uni.setStorageSync(KEY,plans)}catch(e){throw Error('设备存储空间不足，计划未保存，请使用服务端模式')}}
function summarize(p){const done=p.tasks.filter(t=>t.completed).length;return {...p,tasks:undefined,remainingDays:Math.max(0,daysBetween(today(),p.examDate)),totalTasks:p.tasks.length,completedTasks:done,progress:Math.round(done*100/p.tasks.length),stages:p.stages.map(s=>{const ts=p.tasks.filter(t=>t.stageId===s.id);return {...s,progress:Math.round(ts.filter(t=>t.completed).length*100/ts.length)}})}}
function find(id){const p=read().find(p=>p.id===id);if(!p)throw Error('计划不存在');return p}
export function request(path,method='GET',data){return new Promise((resolve,reject)=>{
  const options={method,data,timeout:15000,header:{'Content-Type':'application/json',Authorization:'Bearer '+(uni.getStorageSync(sessionKey)||'')},success:r=>{if(r.statusCode>=200&&r.statusCode<300)resolve(r.data);else {if(r.statusCode===401)uni.removeStorageSync(sessionKey);reject(Error(r.data?.message||'服务暂不可用'))}},fail:e=>{
  const domainBlocked=/domain.*(?:list|invalid)|合法域名/i.test(e?.errMsg||'')
  reject(Error(domainBlocked?'服务域名未加入微信 request 合法域名，请在公众平台开发设置中添加后重新打开项目':'无法连接服务，请检查后端地址、微信合法域名与网络'))
  }}
  if(import.meta.env.VITE_API_TRANSPORT==='cloudrun'&&typeof wx!=='undefined'){
    if(!cloudEnv||!cloudService){reject(Error('云托管环境和服务名称尚未配置'));return}
    if(!wx.cloud?.callContainer){reject(Error('当前微信版本不支持云托管调用，请升级微信基础库'));return}
    try{
      if(!cloudInitialized){wx.cloud.init({});cloudInitialized=true}
      const prefix=base.replace(/^https?:\/\/[^/]+/,'').replace(/\/$/,'')
      wx.cloud.callContainer({...options,config:{env:cloudEnv},path:prefix+path,header:{...options.header,'X-WX-SERVICE':cloudService},fail:()=>reject(Error('云托管调用失败，请确认环境已绑定此小程序、服务正在运行并已开通云调用'))})
    }catch(e){reject(Error('云托管初始化失败，请检查当前微信基础库和环境绑定'))}
  }else uni.request({...options,url:base+path})
})}
export const api={
  async config(){return localMode?{demoLogin:true,paymentEnabled:false}:request('/config')},
  async login(){
    if(localMode||uni.getStorageSync(sessionKey))return
    if(loginPending)return loginPending
    loginPending=(async()=>{
      const cfg=await api.config()
      let response
      if(cfg.demoLogin)response=await request('/auth/login','POST',{demo:true})
      else {
        const code=await new Promise((resolve,reject)=>uni.login({provider:'weixin',success:r=>r.code?resolve(r.code):reject(Error('未获取微信登录code，请在小程序中重试')),fail:()=>reject(Error('请在微信小程序内登录'))}))
        response=await request('/auth/login','POST',{code,demo:false})
      }
      uni.setStorageSync(sessionKey,response.token)
    })()
    try{return await loginPending}finally{loginPending=null}
  },
  async templates(){return localMode?templates:request('/templates')},
  async plans(){if(localMode)return read().map(summarize).reverse();await api.login();return request('/plans')},
  async create(input){if(localMode){const p=generate(input);save([...read(),p]);select(p.id);return summarize(p)}await api.login();const p=await request('/plans','POST',input);select(p.id);return p},
  async plan(id){if(localMode)return summarize(find(id));return request('/plans/'+id)},
  async tasks(id,date=today()){if(localMode)return find(id).tasks.filter(t=>t.date===date);return request('/plans/'+id+'/tasks?date='+date)},
  async complete(id,completed){if(localMode){const plans=read();let found=false;for(const p of plans){const t=p.tasks.find(t=>t.id===id);if(t){found=true;t.completed=completed;t.completedDate=completed?today():null}}if(!found)throw Error('任务不存在');save(plans);return}return request('/tasks/'+id,'PATCH',{completed})},
  async records(id){if(!localMode)return request('/plans/'+id+'/records');const tasks=find(id).tasks.filter(t=>t.completed),dates=new Set(tasks.map(t=>t.completedDate));let cursor=dates.has(today())?today():addDays(today(),-1),streak=0;while(dates.has(cursor)){streak++;cursor=addDays(cursor,-1)}const subjects={};tasks.forEach(t=>subjects[t.subject]=(subjects[t.subject]||0)+t.duration);return {completedTasks:tasks.length,minutes:tasks.reduce((a,t)=>a+t.duration,0),streak,subjects:Object.entries(subjects).map(([name,minutes])=>({name,minutes})),week:Array.from({length:7},(_,i)=>{const date=addDays(today(),i-6);return {date,minutes:tasks.filter(t=>t.completedDate===date).reduce((a,t)=>a+t.duration,0)}})}},
  async products(){return localMode?[{id:'full-plan',name:'完整备考方案',priceFen:990,description:'完整阶段规划、每日任务与学习路线'}]:request('/products')}
}
export function showError(e){uni.showToast({title:e.message||'操作失败',icon:'none'})}
export async function current(){const plans=await api.plans(),id=selected(),p=plans.find(p=>p.id===id)||plans[0];if(p)select(p.id);return p}
