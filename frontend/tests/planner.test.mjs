import test from 'node:test'
import assert from 'node:assert/strict'
import {generate,templates,addDays} from '../src/utils/planner.mjs'
const start='2026-10-13',base={examType:'postgraduate',examName:'考研',dailyMinutes:137,level:'normal',subjects:templates[0].subjects}
test('conserves every daily budget and produces contiguous stages, including short plans',()=>{
  for(const days of [1,2,3,180,730])for(const level of ['weak','normal','good']){
    const p=generate({...base,level,examDate:addDays(start,days)},start),budgets={}
    p.tasks.forEach(t=>budgets[t.date]=(budgets[t.date]||0)+t.duration)
    assert.equal(Object.keys(budgets).length,days)
    Object.values(budgets).forEach(n=>assert.equal(n,137))
    assert.equal(p.stages[0].startDate,start);assert.equal(p.stages.at(-1).endDate,addDays(start,days-1))
    for(let i=1;i<p.stages.length;i++)assert.equal(p.stages[i].startDate,addDays(p.stages[i-1].endDate,1))
  }
})
test('rejects past dates, invalid budgets, duplicate subjects and malformed dates',()=>{
  for(const change of [{examDate:start},{examDate:'garbage'},{examDate:addDays(start,731)},{dailyMinutes:0},{dailyMinutes:30.5},{subjects:[{name:'a',weight:0}]},{subjects:[{name:'a',weight:50},{name:'a',weight:50}]}])assert.throws(()=>generate({...base,examDate:addDays(start,20),...change},start))
})
