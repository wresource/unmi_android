# 本地域名管理系统（Android 移动端）需求文档合集

## 文档说明

本文档将原有网页版需求重构为 **Android 移动端版本**，面向基于 **Kotlin** 开发的本地域名管理系统。系统采用 **Jetpack Compose** 构建 UI，使用 **Jetpack 体系组件** 进行架构设计与本地数据管理，强调：

- 本地优先（Local-first）
- 数据隐私与加密安全
- 支持云端密文备份
- 支持续费统计
- 支持批量导入导出
- 支持 Whois / 域名信息查询
- 代码开源，可审计

本文档包含以下三部分：

1. Android 产品需求文档（PRD）
2. Android 数据库与本地存储设计文档
3. Android 页面原型与交互结构说明

---

# 第一部分：Android 产品需求文档（PRD）

## 1. 项目背景

域名投资、域名持有和站点管理过程中，通常存在以下问题：

- 域名资产分散在不同注册商平台，缺乏统一管理入口
- 域名到期时间、续费价格、年度预算缺乏可视化统计
- 域名清单本身具有资产属性，不适合明文存储在云端
- 批量维护域名时依赖 Excel，但移动端缺少统一管理工具
- 查询 Whois、域名状态、解析信息时需要频繁切换多个网站或工具

因此，需要开发一套基于 **Android + Kotlin** 的本地域名管理系统，采用 **Jetpack Compose** 构建原生移动端 UI，使用 **Jetpack Room / DataStore / WorkManager / Navigation / ViewModel** 等组件实现本地优先、安全可控、可扩展的域名资产管理工具。

---

## 2. 项目目标

### 2.1 核心目标

构建一套适合个人域名投资者、站长或小团队使用的 Android 本地域名管理应用，实现以下目标：

1. 对域名资产进行统一录入、编辑、查询与分类管理
2. 对域名到期、续费费用、历史成本进行统计分析
3. 支持本地加密存储和云端加密备份
4. 支持 Excel / CSV 批量导入导出
5. 提供 Whois 查询与基础域名状态查询
6. 提供简洁高效的移动端交互体验
7. 提供开源信息、版本说明与项目链接展示

### 2.2 产品定位

本产品定位为：

- 面向个人用户与小团队的轻量级域名资产管理 App
- 强调本地控制与隐私安全，而非纯云端 SaaS
- 强调移动端随时查看、续费预算跟踪与到期提醒
- 非自动抢注、非自动续费平台

---

## 3. 用户角色

### 3.1 首期角色

首期默认采用单用户模式，仅包含一个角色：

- **管理员 / 使用者**
  - 解锁应用
  - 管理域名信息
  - 配置主口令与本地安全策略
  - 导入导出域名数据
  - 查看统计信息
  - 发起 Whois 查询
  - 执行备份与同步

### 3.2 后续可扩展角色

后续版本可扩展：

- 只读成员
- 编辑成员
- 超级管理员

> 首期 Android 版本不要求多用户体系。

---

## 4. 产品范围

### 4.1 本期范围（MVP）

- 本地域名列表管理
- 本地加密存储
- 云端加密备份
- 域名续费费用统计
- Excel / CSV 导入导出
- Whois / 域名基础信息查询
- 搜索、筛选、排序
- 到期提醒与本地通知
- 关于项目页（GitHub、项目站点、版本、License）

### 4.2 暂不纳入本期

- 多账号协作
- 自动接入注册商 API 完成续费
- DNS 自动变更
- 域名交易流水系统
- iOS 版本
- 平板专属大屏适配
- SaaS 化后台管理平台

---

## 5. 核心设计理念

### 5.1 Local-first 本地优先

应用核心数据默认保存在本地设备数据库中，用户拥有完整数据控制权。

### 5.2 云端仅保存密文

云端只保存加密后的域名数据和必要元数据，不保存可直接识别的域名明文。

### 5.3 安全解密依赖双因素

只有同时具备以下条件，才可恢复明文数据：

- 用户主口令
- 本地设备密钥 / 安全存储密钥

### 5.4 开源透明

项目代码开源，方便安全审计、社区协作和二次开发。

---

## 6. 功能需求

## 6.1 域名资产管理

### 6.1.1 功能目标

实现域名资产的统一录入、编辑、删除、搜索、筛选、分类和详情查看。

### 6.1.2 域名基础字段

每个域名建议包含以下字段：

- 域名名称
- 域名后缀
- 注册商
- 注册时间
- 到期时间
- 自动续费状态
- 当前状态
- 购买价格
- 续费价格
- 持有成本
- DNS 服务商
- Whois 隐私保护状态
- 用途
- 标签
- 备注
- 创建时间
- 更新时间

### 6.1.3 支持操作

- 新增单个域名
- 编辑域名信息
- 删除域名
- 批量删除
- 批量标签管理
- 按注册商、状态、标签、到期时间筛选
- 按关键字搜索域名
- 按价格、到期时间、更新时间排序

### 6.1.4 列表展示建议

列表项建议显示：

- 域名
- 注册商
- 到期时间
- 剩余天数
- 续费价格
- 状态
- 标签
- 更新时间

剩余天数的视觉提示建议：

- 小于 7 天：高风险提示色
- 8～30 天：中风险提示色
- 大于 30 天：正常提示色

---

## 6.2 加密、本地安全与云端同步

### 6.2.1 功能目标

保障域名数据隐私与安全，确保即使云端数据泄露，也无法直接还原域名列表。

### 6.2.2 加密机制

建议采用以下设计：

1. 用户首次使用时设置主口令
2. 应用在本地生成随机主密钥
3. 主密钥使用 Android Keystore 或本地安全机制保护
4. 域名敏感数据在本地加密后再存储或上传
5. 上传云端的仅为密文、版本信息和必要元数据
6. 解密依赖用户主口令 + 本地安全密钥

### 6.2.3 业务要求

- 首次启动时引导设置主口令
- 支持修改主口令
- 支持生物识别辅助解锁（可选）
- 支持自动锁定
- 支持导出加密备份文件
- 支持导入加密备份文件
- 支持手动同步至云端
- 显示最近同步时间
- 同步失败时给出明确提示
- 后续支持冲突处理

### 6.2.4 安全要求

- 主口令不明文存储
- 敏感字段需加密保存
- 云端不允许明文数据上传
- 操作日志不得记录明文域名与密钥
- 应用切到后台后支持自动重新锁定
- 截图保护可作为可选安全配置

### 6.2.5 关于项目展示

应用需提供“关于项目”页面，展示：

- GitHub 地址
- 项目简介
- 安全机制说明
- 当前版本号
- License 信息
- 米表链接

---

## 6.3 域名续费费用统计

### 6.3.1 功能目标

帮助用户快速评估域名的续费预算、到期风险和历史支出情况。

### 6.3.2 统计维度

支持以下统计方式：

- 按月份统计续费费用
- 按季度统计续费费用
- 按年份统计续费费用
- 按注册商统计续费费用
- 按域名后缀统计续费费用
- 按时间窗口统计未来待续费费用
  - 7 天内
  - 30 天内
  - 90 天内
  - 365 天内

### 6.3.3 统计指标

- 域名总数
- 即将到期域名数
- 已过期域名数
- 预计续费总费用
- 历史续费总费用
- 总持有成本
- 平均续费单价
- 各注册商续费支出占比

### 6.3.4 展示建议

移动端建议采用：

- 概览统计卡片
- 月度续费趋势图
- 注册商支出对比图
- 后缀分布图
- 即将到期预算列表

### 6.3.5 数据逻辑

系统需要区分：

1. 当前标准续费价格
2. 历史真实续费记录

建议建立独立的续费记录表记录每次真实续费行为。

---

## 6.4 Excel / CSV 导入导出

### 6.4.1 功能目标

支持用户批量维护域名数据，并与桌面端办公工具衔接。

### 6.4.2 导入支持格式

- `.xlsx`
- `.csv`

### 6.4.3 导入字段建议

- 域名
- 注册商
- 注册时间
- 到期时间
- 续费价格
- 购买价格
- 状态
- 标签
- 备注
- DNS 服务商
- 自动续费状态

### 6.4.4 导入流程

1. 用户通过系统文件选择器选择文件
2. 系统读取表头
3. 用户确认字段映射
4. 系统展示预览结果
5. 系统校验错误项
6. 用户确认导入
7. 显示成功与失败数量

### 6.4.5 校验规则

- 域名格式是否合法
- 日期字段是否合法
- 数值字段是否合法
- 重复域名如何处理
- 必填字段是否为空

### 6.4.6 导出要求

支持导出：

- 当前筛选结果
- 全部域名
- 续费记录
- 统计结果

导出格式：

- `.xlsx`
- `.csv`
- 加密备份文件（应用自定义格式，可选）

### 6.4.7 模板下载

应用内需提供导入模板下载或导出示例模板能力。

---

## 6.5 Whois / 域名信息查询

### 6.5.1 功能目标

支持快速查询域名基础注册信息与状态信息。

### 6.5.2 查询方式

- 单个域名查询
- 从域名详情页直接查询
- 后续支持批量查询

### 6.5.3 查询结果建议

- 是否已注册
- 注册商
- 注册时间
- 到期时间
- 更新时间
- 域名状态
- Name Server
- 原始 Whois 信息
- 隐私保护状态（若可识别）

### 6.5.4 后续扩展能力

- DNS 解析查询
- SSL 证书信息
- ICP 备案信息
- 网站可访问性检测

### 6.5.5 注意事项

- Whois 数据来源需标注
- 部分后缀返回数据不完整
- 查询接口可能存在频率限制
- 对失败、超时、限流返回进行友好提示

---

## 6.6 到期提醒与通知

### 6.6.1 功能目标

通过本地通知提醒用户关注即将到期的域名。

### 6.6.2 提醒规则建议

支持以下提醒策略：

- 到期前 30 天提醒
- 到期前 7 天提醒
- 到期前 1 天提醒
- 已过期提醒

### 6.6.3 通知方式

- 本地通知
- 应用首页提醒卡片
- 状态栏提醒（可选）

### 6.6.4 用户可配置项

- 是否启用提醒
- 提醒时间点
- 是否合并多个域名提醒
- 通知优先级

---

## 7. 非功能需求

### 7.1 性能要求

- 本地 1 万条以内域名数据可流畅查询和筛选
- 域名列表首次加载应具备良好体验
- 导入 1000 条数据时应在可接受时间内完成
- 统计页面图表加载应平滑
- 查询、筛选、分页滚动不卡顿

### 7.2 安全要求

- 敏感数据加密存储
- 主口令不明文保存
- 云端仅保存密文
- 支持应用锁定
- 支持本地备份恢复
- 提供基础防暴力破解与频繁失败限制策略

### 7.3 可用性要求

- 单手操作尽可能顺畅
- 核心路径不超过 3～4 步
- 支持深色模式
- 提供清晰的错误提示和操作反馈
- 适配常见安卓手机分辨率

### 7.4 可维护性要求

- 采用分层架构
- UI、Domain、Data 解耦
- Repository 模式明确
- 支持后续模块扩展
- 支持数据库迁移

### 7.5 开源要求

- 代码托管于 GitHub
- 提供 README、架构说明、隐私安全说明
- 提供 APK 构建说明
- 标注开源协议

---

## 8. 技术选型与架构要求

## 8.1 技术栈

- 开发语言：Kotlin
- 最低建议 Android 版本：Android 8.0+（可根据实际调整）
- UI：Jetpack Compose
- 架构：MVVM / Clean Architecture
- 本地数据库：Room（Jetpack）
- 键值配置：DataStore
- 导航：Navigation Compose
- 生命周期管理：ViewModel + Lifecycle
- 依赖注入：Hilt / Koin（推荐 Hilt）
- 后台任务：WorkManager
- 网络请求：Retrofit + OkHttp + Kotlin Serialization / Moshi
- 协程：Kotlin Coroutines + Flow
- 本地通知：NotificationManager + WorkManager
- 图表：Compose 图表库或自定义 Canvas 图表
- 文件导入导出：Storage Access Framework
- 加密：Android Keystore + Jetpack Security Crypto（或等效方案）

## 8.2 推荐架构分层

建议采用以下模块划分：

- `app`：应用入口
- `core-ui`：通用 UI 组件
- `core-common`：通用工具类
- `core-database`：Room 数据库与 DAO
- `core-network`：网络层
- `core-security`：加密与安全能力
- `feature-domain-list`：域名列表
- `feature-domain-detail`：域名详情
- `feature-domain-edit`：新增/编辑域名
- `feature-statistics`：统计分析
- `feature-import-export`：导入导出
- `feature-whois`：Whois 查询
- `feature-settings`：设置
- `feature-about`：关于页面
- `feature-backup-sync`：备份与同步

---

## 9. 关键业务流程

### 9.1 首次启动流程

1. 用户首次打开应用
2. 应用显示欢迎与安全说明
3. 用户设置主口令
4. 应用生成本地加密主密钥
5. 初始化 Room 数据库和 DataStore 配置
6. 进入首页仪表盘

### 9.2 应用解锁流程

1. 用户启动应用
2. 进入解锁页
3. 输入主口令或使用生物识别解锁
4. 应用读取本地安全密钥
5. 解锁后进入主界面

### 9.3 新增域名流程

1. 点击新增按钮
2. 输入域名信息
3. 校验表单字段
4. 保存至本地数据库
5. 更新首页统计和提醒任务

### 9.4 导入流程

1. 选择 Excel / CSV 文件
2. 读取并解析文件
3. 显示字段映射界面
4. 校验并预览数据
5. 用户确认导入
6. 写入数据库并刷新索引与统计

### 9.5 云端同步流程

1. 用户解锁应用
2. 应用读取本地密钥
3. 本地数据加密打包
4. 上传密文到云端
5. 记录同步日志
6. 返回同步结果

### 9.6 到期提醒流程

1. 应用检测域名到期时间
2. WorkManager 创建定时检查任务
3. 命中规则后发送本地通知
4. 用户点击通知后跳转至域名详情或列表页

### 9.7 Whois 查询流程

1. 用户输入域名或从详情页发起查询
2. 应用调用查询接口
3. 获取 Whois 结果
4. 格式化展示
5. 可选缓存最近查询结果

---

## 10. 异常场景处理

需覆盖以下异常情况：

- 主口令错误
- 生物识别失败
- 本地密钥丢失
- 数据库损坏
- 导入格式错误
- 查询超时
- 查询接口被限流
- 云端同步失败
- 网络不可用
- 重复域名冲突
- 文件读取权限被拒绝

每种异常都应提供明确、用户可理解的提示文案，不应直接暴露底层异常栈信息。

---

## 11. 验收标准

- 应用可正常安装运行
- 可完成首次初始化与口令设置
- 可新增、编辑、删除域名
- 可搜索和筛选域名
- 可完成续费统计展示
- 可导入导出 Excel / CSV
- 可查询 Whois 信息
- 可导出和恢复加密备份
- 云端同步仅上传密文
- 到期提醒可正常触发
- 主要页面与导航可正常使用

---

## 12. 版本优先级建议

### P0

- 应用解锁
- 域名列表管理
- 新增 / 编辑 / 删除
- 本地数据库
- 本地加密存储
- 续费统计
- Whois 查询
- 关于页面

### P1

- 云端加密备份
- 导入导出
- 到期提醒
- 历史续费记录管理
- 图表优化

### P2

- 生物识别增强
- DNS 查询
- SSL 查询
- 多用户协作
- 多设备同步冲突处理

---

# 第二部分：Android 数据库与本地存储设计文档

## 1. 设计目标

本地数据层采用 Jetpack 推荐方案，目标如下：

- 适合移动端离线使用
- 数据结构清晰
- 支持加密与备份
- 支持迁移与扩展
- 与 Kotlin 协程 / Flow 兼容

---

## 2. 本地存储方案选型

建议采用以下组合：

- **Room**：存储结构化域名、续费、日志等数据
- **DataStore**：存储轻量配置项
- **Android Keystore**：保护密钥材料
- **EncryptedFile / Security Crypto**：用于备份文件加密（可选）

---

## 3. 数据表设计总览

建议首期包含以下实体：

1. `DomainEntity`
2. `RenewalRecordEntity`
3. `SyncBackupLogEntity`
4. `TagEntity`
5. `DomainTagCrossRef`
6. `WhoisQueryLogEntity`
7. `AppSetting`（建议由 DataStore 管理，而非 Room）

---

## 4. Room 实体设计

## 4.1 DomainEntity

```kotlin
@Entity(
    tableName = "domains",
    indices = [
        Index(value = ["domainName"], unique = true),
        Index(value = ["expireDate"]),
        Index(value = ["registrar"]),
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ]
)
data class DomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domainName: String,
    val tld: String,
    val registrar: String?,
    val registerDate: String?,
    val expireDate: String,
    val autoRenew: Boolean = false,
    val status: String = "active",
    val purchasePrice: Double = 0.0,
    val renewPrice: Double = 0.0,
    val holdCost: Double = 0.0,
    val currency: String = "CNY",
    val dnsProvider: String? = null,
    val privacyProtection: Boolean = false,
    val usageType: String? = null,
    val note: String? = null,
    val encryptedPayload: String? = null,
    val createdAt: String,
    val updatedAt: String
)
```

### 字段说明

- `domainName`：完整域名
- `tld`：后缀
- `registrar`：注册商
- `registerDate`：注册日期
- `expireDate`：到期日期
- `autoRenew`：自动续费状态
- `status`：域名状态
- `purchasePrice`：购买价格
- `renewPrice`：标准续费价格
- `holdCost`：累计持有成本
- `currency`：货币
- `dnsProvider`：DNS 服务商
- `privacyProtection`：是否开启 Whois 隐私
- `usageType`：用途
- `note`：备注
- `encryptedPayload`：加密扩展字段
- `createdAt` / `updatedAt`：时间戳

---

## 4.2 RenewalRecordEntity

```kotlin
@Entity(
    tableName = "renewal_records",
    foreignKeys = [
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["domainId"]),
        Index(value = ["renewalDate"])
    ]
)
data class RenewalRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domainId: Long,
    val renewalDate: String,
    val renewalYears: Int = 1,
    val renewalPrice: Double,
    val currency: String = "CNY",
    val note: String? = null,
    val createdAt: String
)
```

---

## 4.3 SyncBackupLogEntity

```kotlin
@Entity(tableName = "sync_backup_logs")
data class SyncBackupLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncType: String,
    val syncStatus: String,
    val versionNo: String? = null,
    val syncTime: String,
    val remoteProvider: String? = null,
    val fileHash: String? = null,
    val remark: String? = null
)
```

---

## 4.4 TagEntity

```kotlin
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String? = null,
    val createdAt: String
)
```

---

## 4.5 DomainTagCrossRef

```kotlin
@Entity(
    tableName = "domain_tag_cross_ref",
    primaryKeys = ["domainId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("domainId"), Index("tagId")]
)
data class DomainTagCrossRef(
    val domainId: Long,
    val tagId: Long
)
```

---

## 4.6 WhoisQueryLogEntity（可选）

```kotlin
@Entity(tableName = "whois_query_logs")
data class WhoisQueryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domainName: String,
    val queryStatus: String,
    val provider: String? = null,
    val rawText: String? = null,
    val parsedResultJson: String? = null,
    val queriedAt: String,
    val errorMessage: String? = null
)
```

---

## 5. 关系模型设计

### 5.1 Domain 与 RenewalRecord

- 一个域名可对应多条续费记录
- 通过 `domainId` 建立一对多关系

### 5.2 Domain 与 Tag

- 一个域名可关联多个标签
- 一个标签可关联多个域名
- 通过 `DomainTagCrossRef` 建立多对多关系

---

## 6. DAO 设计建议

建议至少包含：

- `DomainDao`
- `RenewalRecordDao`
- `TagDao`
- `SyncBackupLogDao`
- `WhoisQueryLogDao`

示例：

```kotlin
@Dao
interface DomainDao {

    @Query("SELECT * FROM domains ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DomainEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DomainEntity): Long

    @Update
    suspend fun update(entity: DomainEntity)

    @Delete
    suspend fun delete(entity: DomainEntity)
}
```

---

## 7. Room Database 设计建议

```kotlin
@Database(
    entities = [
        DomainEntity::class,
        RenewalRecordEntity::class,
        SyncBackupLogEntity::class,
        TagEntity::class,
        DomainTagCrossRef::class,
        WhoisQueryLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun domainDao(): DomainDao
    abstract fun renewalRecordDao(): RenewalRecordDao
    abstract fun syncBackupLogDao(): SyncBackupLogDao
    abstract fun tagDao(): TagDao
    abstract fun whoisQueryLogDao(): WhoisQueryLogDao
}
```

---

## 8. DataStore 配置项设计

适合放入 DataStore 的配置项包括：

- 是否首次启动
- 自动锁定时间
- 是否启用生物识别
- 默认货币单位
- 日期格式
- 深色模式设置
- 导入重复项策略
- GitHub 链接
- 项目站点链接
- 云同步开关
- 最近一次同步时间

建议封装为：

```kotlin
interface AppPreferenceRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun updateLockTimeout(minutes: Int)
    suspend fun updateBiometricEnabled(enabled: Boolean)
    suspend fun updateDarkMode(enabled: Boolean)
}
```

---

## 9. 加密与安全存储设计建议

### 9.1 推荐方案

- 主口令仅用于校验与派生能力
- 真正的数据加密密钥由系统随机生成
- 随机密钥存放于 Android Keystore 保护域
- 敏感字段使用 AES-GCM 加密
- DataStore / Room 中只保存必要的密文和元数据

### 9.2 建议加密字段

- 域名备注中的敏感信息
- 扩展字段
- 云同步凭证
- 自定义注册信息
- 导出备份内容

### 9.3 生物识别

可选支持以下模式：

- 生物识别快速解锁
- 生物识别仅作为便捷解锁，不替代主口令初始化
- 更改主口令或安全配置时仍需重新验证主口令

---

## 10. Repository 设计建议

建议每个功能模块基于 Repository 暴露统一数据入口，例如：

- `DomainRepository`
- `StatisticsRepository`
- `ImportExportRepository`
- `WhoisRepository`
- `BackupSyncRepository`
- `SecurityRepository`
- `SettingsRepository`

这样可确保：

- ViewModel 不直接操作 DAO
- 本地与远程数据源解耦
- 业务逻辑集中管理

---

## 11. 迁移策略

Room 版本升级时应提供 Migration：

- 新增字段
- 新增表
- 修改索引
- 迁移历史数据

建议：

- 始终开启 `exportSchema`
- 使用显式 `Migration`，避免粗暴删库
- 调整加密结构时提供兼容逻辑

---

## 12. 最小可落地版本建议

若先做最小版本，优先实现：

- `DomainEntity`
- `RenewalRecordEntity`
- `SyncBackupLogEntity`
- `AppSettings(DataStore)`

后续再扩展：

- `TagEntity`
- `DomainTagCrossRef`
- `WhoisQueryLogEntity`

---

# 第三部分：Android 页面原型与交互结构说明

## 1. 说明目标

本部分用于描述 Android App 的页面结构、信息层级、导航关系与交互逻辑，可作为：

- Jetpack Compose 页面设计依据
- 前端 UI 组件拆分依据
- 交互原型说明
- 开发排期依据

---

## 2. 全局导航结构建议

建议采用 **Bottom Navigation + Nested Navigation** 结构。

### 2.1 底部主导航建议

建议主导航包含以下 4～5 个 Tab：

- 首页
- 域名
- 统计
- 工具
- 设置

其中：

- **首页**：概览、提醒
- **域名**：列表、搜索、详情、新增编辑
- **统计**：续费预算与图表
- **工具**：导入导出、Whois、备份同步
- **设置**：安全、主题、关于项目

### 2.2 浮动按钮建议

在域名列表页显示 FAB：

- 新增域名

---

## 3. 页面原型说明

## 3.1 启动页 / 解锁页

### 页面目标

应用启动后，用于解锁本地数据。

### 页面模块

- 应用 Logo
- 应用名称
- 主口令输入框
- 生物识别解锁按钮（可选）
- 解锁按钮
- 首次初始化入口
- 安全提示文案

### 交互说明

- 首次启动进入初始化流程
- 非首次启动默认进入解锁页
- 解锁成功进入首页
- 失败时给出明确提示
- 多次失败可触发延迟重试策略

### Compose 组件建议

- `Scaffold`
- `Column`
- `OutlinedTextField`
- `Button`
- `TextButton`
- `IconButton`

---

## 3.2 首次初始化页

### 页面目标

首次安装后完成主口令初始化和安全说明。

### 页面模块

- 欢迎说明
- 主口令输入
- 确认主口令输入
- 安全提示
- 初始化按钮

### 交互说明

- 两次主口令需一致
- 强度不满足时给出提示
- 初始化完成后进入首页

---

## 3.3 首页（Dashboard）

### 页面目标

展示资产概览、到期提醒和最近动态。

### 页面布局建议

#### 顶部概览区

- 域名总数
- 即将到期域名数
- 本月预算
- 年度预算

#### 中部提醒区

- 7 天内到期域名
- 30 天内到期域名
- 已过期域名

#### 下方动态区

- 最近新增域名
- 最近更新域名
- 最近同步状态

### 交互说明

- 点击统计卡片跳转到筛选后的列表页
- 点击提醒项可跳转域名详情
- 支持下拉刷新

### Compose 建议

- `LazyColumn`
- `Card`
- `Row`
- `FlowRow`
- `PullRefresh`（或等效实现）

---

## 3.4 域名列表页

### 页面目标

提供域名统一管理入口。

### 页面布局建议

#### 顶部搜索区

- 搜索输入框
- 筛选按钮
- 排序按钮

#### 筛选条件底部弹层

- 注册商
- 状态
- 标签
- 后缀
- 自动续费状态
- 到期时间范围

#### 列表区

每个列表项建议展示：

- 域名名称
- 注册商
- 到期时间
- 剩余天数
- 续费价格
- 标签
- 状态

#### 悬浮操作

- FAB：新增域名

### 交互说明

- 支持下拉刷新
- 支持空状态页
- 支持长按进入批量选择模式
- 支持列表项点击进入详情页

### Compose 建议

- `LazyColumn`
- `ModalBottomSheet`
- `ExtendedFloatingActionButton`
- `FilterChip`
- `AssistChip`

---

## 3.5 新增 / 编辑域名页

### 页面目标

录入或编辑单个域名信息。

### 表单分区建议

#### 基础信息区

- 域名名称
- 后缀
- 注册商
- 状态
- 用途

#### 时间区

- 注册日期
- 到期日期
- 自动续费开关

#### 费用区

- 购买价格
- 续费价格
- 持有成本
- 货币单位

#### 扩展区

- DNS 服务商
- 隐私保护状态
- 标签
- 备注

### 底部操作区

- 保存
- 删除（编辑状态时）
- 取消

### 交互说明

- 表单校验失败时显示字段错误
- 可使用日期选择器
- 保存成功后返回上一页并刷新

### Compose 建议

- `Scaffold`
- `LazyColumn`
- `OutlinedTextField`
- `ExposedDropdownMenuBox`
- `Switch`
- `DatePickerDialog`（Material 3 适配）

---

## 3.6 域名详情页

### 页面目标

查看单个域名完整资料与扩展信息。

### 页面模块

#### 顶部摘要卡片

- 域名
- 状态
- 到期时间
- 剩余天数

#### 基础信息卡片

- 注册商
- 注册日期
- 自动续费
- DNS 服务商

#### 费用信息卡片

- 购买价格
- 续费价格
- 累计持有成本

#### 标签与备注卡片

- 标签
- 备注

#### 续费记录区域

- 历史续费列表
- 新增续费记录按钮

#### Whois 结果区域

- 最近查询结果
- 查询时间
- 原始文本展开区

### 页面操作

- 编辑
- 删除
- 查询 Whois
- 添加续费记录
- 导出该域名信息

---

## 3.7 统计页

### 页面目标

提供续费统计、预算分析和域名分布概览。

### 页面模块

#### 顶部筛选条

- 时间范围
- 注册商
- 域名后缀
- 状态

#### 概览卡片

- 域名总数
- 待续费数量
- 本期续费预算
- 平均续费价格

#### 图表区域

- 月度趋势图
- 注册商对比图
- 后缀占比图

#### 明细列表

- 即将续费域名列表
- 历史续费记录列表

### Compose 建议

- `LazyColumn`
- `Card`
- 自定义图表 Composable
- `TabRow`（在明细视图中切换）

---

## 3.8 工具页

### 页面目标

集中展示导入导出、Whois、备份同步等工具入口。

### 页面模块建议

- 导入 / 导出
- Whois 查询
- 备份与恢复
- 云端同步
- 模板下载

### 交互说明

- 点击某项进入具体功能页
- 显示最近一次操作状态

---

## 3.9 导入导出页

### 页面目标

用于批量导入和导出域名数据。

### 页面模块

#### 导入区

- 选择文件按钮
- 支持格式说明
- 字段映射入口
- 数据预览区
- 导入按钮

#### 导出区

- 导出范围选择
- 导出格式选择
- 导出按钮

#### 模板区

- 下载示例模板按钮

### 交互说明

- 使用系统文件选择器
- 读取失败时提示权限或格式问题
- 导入后展示成功/失败数量

---

## 3.10 Whois 查询页

### 页面目标

用于查询域名注册与状态信息。

### 页面模块

- 域名输入框
- 查询按钮
- 查询结果摘要
- Name Server 列表
- 原始 Whois 文本
- 错误信息区域

### 交互说明

- 查询中显示加载状态
- 查询成功后展示结构化结果
- 查询失败时展示失败原因
- 支持复制原始文本

---

## 3.11 备份与同步页

### 页面目标

用于管理本地加密备份、恢复和云端同步。

### 页面模块

#### 本地备份区

- 导出加密备份按钮
- 最近备份时间
- 备份摘要

#### 恢复区

- 选择备份文件
- 输入验证信息
- 恢复按钮

#### 云端同步区

- 同步开关
- 最近同步时间
- 当前版本号
- 手动同步按钮

#### 日志区

- 最近同步 / 备份记录列表

### 交互说明

- 恢复操作需二次确认
- 同步失败时显示可重试按钮
- 同步前必须完成解锁验证

---

## 3.12 设置页

### 页面目标

管理应用配置、安全策略与显示偏好。

### 页面模块

#### 安全设置

- 修改主口令
- 生物识别开关
- 自动锁定时间
- 截图保护开关（可选）

#### 显示设置

- 深色模式
- 主题色（后续）
- 日期格式
- 默认货币单位

#### 导入导出设置

- 重复项处理方式
- 默认导出格式

#### 项目信息设置

- GitHub 地址
- 项目主页
- 米表链接

---

## 3.13 关于页面

### 页面目标

展示开源信息、版本说明和项目介绍。

### 页面模块

- 应用图标与名称
- 当前版本号
- 项目简介
- GitHub 地址
- License
- 米表链接
- 更新日志
- 安全说明

---

## 4. 导航路由建议

建议采用如下 Navigation 结构：

```kotlin
sealed class AppRoute(val route: String) {
    data object Unlock : AppRoute("unlock")
    data object Init : AppRoute("init")
    data object Home : AppRoute("home")
    data object DomainList : AppRoute("domain_list")
    data object DomainDetail : AppRoute("domain_detail/{id}")
    data object DomainEdit : AppRoute("domain_edit/{id}")
    data object DomainCreate : AppRoute("domain_create")
    data object Statistics : AppRoute("statistics")
    data object Tools : AppRoute("tools")
    data object ImportExport : AppRoute("import_export")
    data object Whois : AppRoute("whois")
    data object BackupSync : AppRoute("backup_sync")
    data object Settings : AppRoute("settings")
    data object About : AppRoute("about")
}
```

---

## 5. UI 组件拆分建议

### 通用组件

- 统计卡片组件
- 域名列表项组件
- 风险状态标签组件
- 标签 Chip 组件
- 搜索栏组件
- 空状态组件
- 错误状态组件
- 加载状态组件
- 顶部栏组件
- 底部导航组件

### 业务组件

- 域名表单组件
- 续费记录列表组件
- Whois 结果组件
- 导入预览组件
- 字段映射组件
- 同步日志组件
- 解锁表单组件

---

## 6. 页面开发优先级建议

### 第一阶段

- 启动 / 解锁页
- 首页
- 域名列表页
- 新增 / 编辑页
- 详情页

### 第二阶段

- 统计页
- Whois 查询页
- 设置页

### 第三阶段

- 导入导出页
- 备份与同步页
- 关于页
- 到期通知能力

---

## 7. 原型推进建议

建议按以下顺序推进开发：

1. 先定义导航结构和基础主题
2. 再完成域名列表与详情链路
3. 再接入 Room 和 DataStore
4. 再补统计、Whois、导入导出
5. 最后实现备份同步与安全增强

---

# 附录：产品一句话描述

这是一个基于 **Kotlin + Jetpack Compose + Room + DataStore** 开发的 Android 本地域名管理系统，采用 **Local-first** 设计，默认本地存储，支持云端加密备份。云端仅保存加密后的域名数据，只有主口令和本地安全密钥同时存在时才能解密。系统支持域名资产管理、续费成本统计、Excel 批量导入导出、Whois 查询和到期提醒等功能，代码开源，强调隐私、安全与可控。
