# 用户提供的 UI 参考

## 当前绿色页面设计

以用户最新提供的 `green-layout-reference.png` 为视觉依据：薄荷背景、绿色主色、白色圆角卡片、胶囊按钮，底部为首页/计划/任务/统计/我的五栏导航。

首页展示考试倒计时、透明猫咪、每日任务与直接勾选打卡；计划页展示考试摘要、科目实际完成率；任务页支持科目和未完成筛选；统计页展示四个数据卡片和绿色时长趋势；我的页面使用参考图中的列表结构。创建表单保留当前接口支持的考试日期、学习预算、基础和科目字段。

设计图中的删除/编辑考试、自定义新增任务、月统计、目标院校和备注需要新增服务端接口，本次没有制作虚假的可编辑入口；已有入口仍执行真实操作。云端地址、微信登录与数据库接口保持不变。

透明猫咪通过内置 imagegen 处理，原图保留。最终文件：`assets/brand/dengkemiao-study-cutout.png`，消费文件：`frontend/src/static/dengkemiao.png`。提示词要求：仅移除绿色圆形背景、外部白色区域与背景装饰，保留橘白猫、眼镜、红围巾、书签耳饰、金色登字徽章、绿色铅笔与备考计划本，尽量保持原角色造型及文字，输出透明背景。

手机尺寸预览见 `docs/screenshots/green-home.jpg`、`green-plan.jpg`、`green-tasks.jpg`、`green-stats.jpg`。

## 当前页面猫咪

后续用户提供 `C:/Users/Administrator/Desktop/登科喵设计图.zip`，现已使用其中「登科喵备考计划.png」替换所有页面的猫咪。原图保存为 `assets/brand/dengkemiao-study-original.png`，页面统一引用 `frontend/src/static/dengkemiao.png`。图片内容未经重绘或修改，圆形展示由页面CSS实现。旧品牌素材保留供历史参考。

覆盖首页、计划总览/每日一句、计划生成完成、创建时的生成状态、今日任务、方案页、个人页与公共空状态。当前预览为 `docs/screenshots/home-cat-update.jpg`。

## 初始 UI 参考

来源：本聊天用户提供的 `D:/Backup/Downloads/dengke-exam-assistant-ui.zip`。

原始设计图保存为 `design-reference.png`，作为布局与视觉参考，不把图片内示例数据当成真实用户数据。

本次已应用：浅蓝底色、圆角卡片、渐变蓝色横幅、考试猫咪IP、四个彩色快捷入口、考试类型网格、生成完成页、计划总览/阶段切换、任务状态筛选、学习记录与个人统计、底部图形导航。

设计图中的会员价格、AI功能、资料库以及示例考试日期不作为V1功能变更。当前保持9.9元完整方案价格预览，真实支付尚未开放；没有虚构学习记录或会员权益。

猫咪素材使用内置 imagegen，提示：参照设计图蓝色横幅中的白色学习猫，蓝色耳朵与轮廓、橙红考试头带、金色铃铛、手持学习清单；独立透明背景，不含文字与UI。最终文件：`assets/brand/dengkemiao-mascot.png`；小程序消费文件：`frontend/src/static/dengkemiao.png`。原先头像图标保留为 `assets/brand/dengkemiao-icon.png`。
