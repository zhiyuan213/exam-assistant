export const templates = [
  {code:'postgraduate',name:'考研',symbol:'研',subjects:[{name:'数学',weight:40},{name:'英语',weight:20},{name:'政治',weight:15},{name:'专业课',weight:25}]},
  {code:'civil',name:'公务员',symbol:'公',subjects:[{name:'行测',weight:60},{name:'申论',weight:40}]},
  {code:'teacher',name:'教师资格证',symbol:'教',subjects:[{name:'综合素质',weight:35},{name:'教育知识与能力',weight:35},{name:'学科知识',weight:30}]},
  {code:'english',name:'四六级',symbol:'英',subjects:[{name:'词汇',weight:25},{name:'听力',weight:25},{name:'阅读',weight:30},{name:'写作与翻译',weight:20}]},
  {code:'custom',name:'自定义考试',symbol:'自',subjects:[{name:'自定义科目',weight:100}]}
]
export function today() {return new Date(Date.now()+8*3600000).toISOString().slice(0,10)}
export function addDays(date,n) {return new Date(Date.parse(date+'T00:00:00Z')+n*86400000).toISOString().slice(0,10)}
export function daysBetween(a,b) {return Math.round((Date.parse(b+'T00:00:00Z')-Date.parse(a+'T00:00:00Z'))/86400000)}
let sequence=0
export function uid() {return Date.now().toString(36)+'-'+(++sequence)+'-'+Math.random().toString(36).slice(2,9)}
export function generate(input,start=today()) {
  const days=daysBetween(start,input.examDate),minutes=Number(input.dailyMinutes),subjects=input.subjects
  if(!Number.isInteger(minutes)||minutes<30||minutes>720||!Number.isInteger(days)||days<1||days>730) throw Error('考试需在未来730天内，每日学习30至720分钟')
  if(!input.examName?.trim()||input.examName.length>80) throw Error('请填写80字以内的考试名称')
  if(!templates.some(t=>t.code===input.examType)) throw Error('请选择考试类型')
  if(!subjects?.length||subjects.length>8||subjects.some(s=>!s.name?.trim()||s.name.length>80||!Number.isInteger(Number(s.weight))||s.weight<=0||s.weight>1000)) throw Error('请填写1至8个科目和有效权重')
  if(new Set(subjects.map(s=>s.name.trim())).size!==subjects.length) throw Error('科目名称不能重复')
  const ratio={weak:.5,normal:.4,good:.25}[input.level]
  if(!ratio) throw Error('请选择基础水平')
  const total=subjects.reduce((a,s)=>a+Number(s.weight),0),budgets=subjects.map(s=>Math.floor(minutes*s.weight/total))
  const remainders=subjects.map((s,i)=>({i,r:minutes*s.weight%total})).sort((a,b)=>b.r-a.r)
  const remaining=minutes-budgets.reduce((a,b)=>a+b,0)
  for(let i=0;i<remaining;i++)budgets[remainders[i].i]++
  const cuts=[0,Math.round(days*ratio),Math.round(days*.75),days],stages=[],tasks=[]
  const names=['基础阶段','强化阶段','冲刺阶段'],goals=['梳理知识框架，建立学习习惯，掌握基础方法','专题练习与复盘，巩固薄弱环节','限时模拟与错题回顾，调整考试节奏'],topics=['基础梳理与例题练习','专题练习与错题复盘','限时练习与重点回顾']
  for(let i=0;i<3;i++) {
    if(cuts[i]===cuts[i+1])continue
    const stage={id:uid(),name:names[i],goal:goals[i],startDate:addDays(start,cuts[i]),endDate:addDays(start,cuts[i+1]-1),progress:0};stages.push(stage)
    for(let d=cuts[i];d<cuts[i+1];d++)subjects.forEach((s,j)=>{if(budgets[j]>0)tasks.push({id:uid(),stageId:stage.id,date:addDays(start,d),subject:s.name.trim(),name:topics[i],duration:budgets[j],completed:false,completedDate:null})})
  }
  return {...input,examName:input.examName.trim(),id:uid(),startDate:start,stages,tasks}
}
