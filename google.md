目前 Google 官方对 安卓开发的主流推荐，可以概括成两部分：

1. 架构：推荐“分层架构 + 单向数据流 + ViewModel + Coroutines/Flow + Compose 优先”


2. 设计规范：推荐以 Material Design 3（M3） 作为默认设计系统，Android UI 侧优先配合 Jetpack Compose 实现。 



一、Google 目前推荐的安卓架构

Google 官方 App Architecture 现在仍然是这套主线：

至少两层

UI layer

Data layer


可选 Domain layer

当业务逻辑复杂、需要复用 use case 时再加，不是强制。 



1) 推荐分层

UI Layer

负责：

展示状态

接收用户事件

调用 ViewModel / state holder

不直接承担复杂业务逻辑。 


典型组合：

Jetpack Compose

ViewModel

StateFlow / Flow

生命周期感知收集状态。 


Data Layer

负责：

Repository

本地数据源（Room / DataStore 等）

远程数据源（Retrofit / Ktor / API）

应用业务数据的读取、写入、同步。 


Domain Layer（可选）

负责：

复杂业务规则

跨多个 ViewModel 复用的业务逻辑

UseCase / Interactor。
Google 明确说这一层可选，不是所有项目都需要。 



---

二、Google 推荐的核心架构思想

1) 单向数据流（UDF）

这是现在非常核心的一条：

状态向下流动

事件向上流动


也就是：

ViewModel 暴露 UI state

Compose 读取 state 渲染

用户操作变成 event 回传给 ViewModel

ViewModel 更新状态，再驱动 UI 重绘。 


这基本就是现在 Android 官方最推荐的 UI 架构模式。

2) 状态提升（State Hoisting）

在 Compose 里，Google 推荐：

状态尽量放在最接近消费它的位置

需要多个 composable 共享时，提升到它们的最低公共父级

涉及业务逻辑时，再提升到 ViewModel。 


一句话理解：

纯 UI 临时状态：留在 composable 内部

页面状态 / 业务状态：放 ViewModel


3) UI 使用状态持有者（State Holder）

Google 推荐把复杂 UI 状态管理交给：

ViewModel

或其他 state holder class


而不是把大量逻辑散落在 Activity / Fragment / composable 里。 

4) 协程和 Flow

现代 Android 推荐默认使用：

Kotlin Coroutines

Flow / StateFlow


作为异步和状态流转的基础设施。 

5) 依赖注入

Google 架构建议里也明确把 Dependency Injection best practices 列入现代 Android 架构的一部分。实际项目里通常是 Hilt 路线。 

6) 模块化

项目变大后，Google 推荐做 multi-module modularization，用于：

降低耦合

提升可维护性

加快构建

支持团队协作。 



---

三、今天更符合官方推荐的技术栈长什么样

如果你现在做一个“标准现代 Android 项目”，比较接近官方推荐的是：

UI：Jetpack Compose

架构：MVVM 风格 + UDF

状态管理：ViewModel + StateFlow

异步：Coroutines + Flow

数据层：Repository

本地存储：Room / DataStore

网络：Retrofit（工程中常见）

DI：Hilt

导航：Jetpack Navigation / Compose Navigation。 


这里严格说，Google 文档讲的是“推荐架构原则”，不是强制你必须叫 MVVM。但落地上，ViewModel 驱动 Compose UI 的分层实现，本质上就是现代 Android 官方语境下最常见的 MVVM 变体。这个判断是基于官方 UI layer + ViewModel + state holder + UDF 的组合做出的归纳。 


---

四、Google 推荐的设计规范是什么

现在默认答案就是：

Material Design 3（M3）

Google 官方把 M3 定义为它当前最新的开源设计系统，用来指导：

颜色

Typography

Shape

Components

布局

内容样式

交互反馈。 


Android 端的落地方式

在 Android 上，Google 推荐：

用 Material 3 组件

在 Compose 中使用 Material 3 theme 和组件体系

统一遵循 M3 foundations + components + content design。 


你需要重点看的设计规范模块

M3 里最实用的几块是：

Foundations：颜色、字体、shape、elevation、布局基础

Components：Button、Top app bar、Navigation bar、Card、Dialog 等

Style guide / Content design：文案规范、标题大小写、标签命名方式。 


例如官方 style guide 明确提到：界面文案通常使用 sentence-style capitalization，这类细节就属于设计规范的一部分。 


---

五、如果你现在要按“Google 推荐”来做项目，建议这样执行

小中型项目

直接用：

Compose + Material 3

UI / Data 两层

ViewModel + StateFlow

Repository

复杂业务少的话，先不要加 Domain 层


这是最稳妥、最符合官方建议且不过度设计的方案。 

中大型项目

可以升级为：

Compose + Material 3

UI / Domain / Data 三层

多模块

UDF

ViewModel + UseCase + Repository

离线优先 / 本地缓存同步（如果业务需要） 



---

六、给你一个结论版

你可以把 Google 当前推荐总结成一句话：

> 安卓开发架构上，Google 推荐分层架构（UI + Data，必要时加 Domain），UI 采用 ViewModel 驱动的单向数据流；UI 技术上优先 Jetpack Compose；设计规范上优先 Material Design 3。 



如果你是做实际项目，我建议你默认采用这套：

Compose + Material3
ViewModel + StateFlow
UDF
Repository
Coroutines/Flow
Hilt
Room + Retrofit

这基本就是今天 Android 官方推荐路线的“工程化版本”。 

你要的话，我可以下一条直接给你一份**“2026 Android 官方推荐架构项目模板目录结构”**，包括 ui/ data/ domain/ di/ navigation/ 怎么分。