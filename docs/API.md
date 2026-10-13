# API V1

Base URL：`http://localhost:8080/api`。请求及响应均为 JSON。

登录后的接口使用 `Authorization: Bearer <token>`。token 服务端只保存 SHA-256 摘要，有效期7天。openid 不向前端返回。

| 方法 | 路径 | 功能 / 请求 |
|---|---|---|
| GET | `/health` | 服务状态、上海日期 |
| GET | `/config` | 体验登录与支付开关 |
| GET | `/templates` | 考试模板、默认科目和权重 |
| POST | `/auth/login` | `{ "code": "微信临时code", "demo": false }`；本地支持 `{ "demo": true }` |
| GET | `/plans` | 当前用户计划列表 |
| POST | `/plans` | 创建计划，见下例 |
| GET | `/plans/{id}` | 计划及阶段、任务数、完成率 |
| GET | `/plans/{id}/tasks?date=2026-10-13` | 指定日任务；不传日期则用上海当日 |
| PATCH | `/tasks/{id}` | `{ "completed": true }`，false 撤销；重复完成不会增加记录或修改原打卡日期 |
| GET | `/plans/{id}/records` | 完成任务数、累计计划分钟、连续打卡天数、科目与七日分布 |
| GET | `/products` | 商品预览，价格字段 priceFen 为整数分 |
| POST | `/orders` | `{ "planId": "...", "productId": "full-plan" }`；当前固定返回503，未接入真实支付 |

创建请求：

```json
{
  "examType": "postgraduate",
  "examName": "2027考研",
  "examDate": "2027-12-20",
  "dailyMinutes": 240,
  "level": "normal",
  "subjects": [
    {"name":"数学", "weight":40},
    {"name":"英语", "weight":20},
    {"name":"政治", "weight":15},
    {"name":"专业课", "weight":25}
  ]
}
```

日期只是输入示例，不代表官方考试安排。type 为 `postgraduate/civil/teacher/english/custom`；level 为 `weak/normal/good`。每日分钟30至720，日期未来1至730天，科目1至8个，权重正整数且最大1000，不必合计100。剩余分钟按最大余数分配，确保每日预算守恒。准备期包含创建当天，截止考试前一天。

错误响应：`{"message":"说明"}`。400 参数错误、401 未登录或过期、403 禁止体验登录、404 资源不存在或不属于当前用户、503 登录未配置/支付未开通。非当前用户资源不暴露其内容。

阶段与任务在创建时事务写入，接口不接受前端指定 user_id、价格、付款状态或权益状态。后续支付接入必须以服务端验证回调及查询结果发放权益。
