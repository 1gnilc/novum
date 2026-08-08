# Server

[English](CONTEXT.md)

Server 建立身份、评估对受保护目标的访问，并实施应用 API。Admin 专用词汇以 [`apps/admin/CONTEXT.md`](../admin/CONTEXT.md) 为规范，Customer 词汇以 [`apps/mobile/CONTEXT.md`](../mobile/CONTEXT.md) 为规范；本文件不重复定义二者。

## 认证与授权

**Authentication（认证）**：确认哪个主体发起了一次访问。认证成功会建立身份，但本身不会授予访问权限。

**Authorization（授权）**：准备决定一次访问是否获准所需的事实和权限集合。

**Permission Checking（权限检查）**：评估已授予权限是否满足所需权限。它不发现权限来源，也不处理拒绝结果。_避免使用_：Authorization check

**Granted Permission（已授予权限）**：在一次授权决策中可供某个访问身份使用的权限。它可能来自角色、组、系统身份、临时授权或匿名默认值。

**Required Permission（所需权限）**：受保护目标在一次授权决策中要求的权限。

**Public Access Permission（公开访问权限）**：无需角色绑定即可授予每次访问的权限，包括匿名访问。它不意味着对应导航项可见。

**Access Context（访问上下文）**：一次访问的授权事实，包括环境、身份、目标和可选属性。请求、连接和缓存等运行时对象不是访问事实。

**Access Environment（访问环境）**：发生授权决策的执行环境，例如 Servlet、消息传递或定时任务。它防止无关环境向同一个决策提供事实。

**Access Identity（访问身份）**：参与授权的身份事实，例如用户、匿名访客、系统身份、服务账户或任务身份。_避免使用_：User、account

**Access Target（访问目标）**：访问的受保护目的地，可用限定符区分 HTTP 方法或操作等变体。

**Access Decision（访问决策）**：权限检查针对一个访问上下文产生的允许或拒绝结果。

**Access Denied Handling（访问拒绝处理）**：访问决策为拒绝后执行的环境专用操作，例如返回 HTTP 响应、拒绝消息或停止任务。

## 图片存储

**Managed Image（托管图片）**：通过 Novum 上传并确认可供应用使用的图片。业务资源通过 Image Object Key 引用它，而不是通过公开 URL。

**Image Object Key（图片对象键）**：Managed Image 的稳定存储身份。公开展示 URL 可以改变，而无需改变该身份。_避免使用_：Image URL、Image ID

**Image Upload（图片上传）**：为图片预留存储空间的生命周期，最终以存储对象被确认为 Managed Image 或预留过期结束。

## Setting

**Setting**：由后端拥有的运行时业务规则，以唯一的大写常量名称和字符串值持久化。项目代码通过集中声明的命名常量访问 Setting，而不是重复其名称字面量。_避免使用_：Business Setting、Application Property、Feature Flag

**Setting Log（Setting 日志）**：一次 Admin 修改 Setting 所产生的仅追加记录，保留旧值、新值、Admin 身份和创建时间。

## 客户端网络身份

**Client IP（客户端 IP）**：为 Customer 请求记录的 IP。解析 `X-Forwarded-For` 中第一个有效地址；缺失或无效时使用 Servlet 远程地址；二者均无效时使用 `unknown`。_避免使用_：Proxy IP
