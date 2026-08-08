# Admin System

[English](CONTEXT.md)

Admin System 对管理员账户、RBAC 资源、导航和动态国际化消息提供经过认证的管理能力。

## 管理员

**Admin User（管理员用户）**：可登录管理系统并拥有可变个人资料数据的个人身份。每个 Admin User 都有对应的 RBAC 主体。_避免使用_：User、backend user

**Current Admin User（当前管理员用户）**：当前有效管理员会话所代表的 Admin User。自助操作始终从该身份派生目标。

**Admin Credentials（管理员凭据）**：用于证明 Admin User 身份的用户名和密码。凭据不是认证后签发的会话或令牌。

**Admin Session（管理员会话）**：Admin User 经过认证的登录状态。它可以独立于用户个人资料刷新或撤销。

**Current Admin Self-Service（当前管理员自助服务）**：Current Admin User 无需选择其他 Admin User，即可读取或更新自己的个人资料和密码。

**Admin User Administration（管理员用户管理）**：管理所选 Admin User 的账户数据和角色分配。它与 Current Admin Self-Service 不同，且不能禁用或删除当前操作者。

**Default Admin Baseline（默认管理员基线）**：系统初始化后所需的可恢复引导 Admin User、RBAC 主体、基线角色和强制绑定。恢复过程保留操作者管理的个人资料和凭据数据。

## 角色与资源

**Admin Access Baseline Role（管理员访问基线角色）**：每个 Admin User 都必须保留的内置 `admin` 角色。它提供自助服务和基本管理外壳访问；专用角色增加管理能力。

**Built-in Role（内置角色）**：身份、权限和菜单由系统维护的角色。可以按其规则分配，但不能通过管理功能重新定义或删除。

**Built-in RBAC Resource（内置 RBAC 资源）**：内置状态为固有属性且由系统维护的角色、权限或菜单。分配操作不会让普通资源变成内置资源。

**RBAC Manager（RBAC 管理员）**：可管理 Admin User、角色、权限、菜单及其分配关系的内置 `rbac:manager` 角色。

**I18n Manager（国际化管理员）**：可跨分类查询和维护动态国际化消息的内置 `i18n:manager` 角色。

## 导航

**Current Admin Navigation Route Tree（当前管理员导航路由树）**：Current Admin User 可用的已启用、可到达导航层级。它排除孤立节点和没有可用导航后代的目录。

**Menu Hierarchy（菜单层级）**：由根菜单和非根菜单组成的无环树，每个非根菜单恰好有一个父级。父子类型会约束有效关系。

**Menu Type（菜单类型）**：定义菜单层级与运行时行为的不可变分类。更改类型会创建新的菜单身份，而不是修改现有身份。

**Menu Authorization Closure（菜单授权闭包）**：某角色的有效菜单授权，加上将这些菜单连接到根节点所需的全部祖先。禁用菜单即使不出现在导航中，也可以继续保留授权。

**Menu Disablement（菜单禁用）**：临时从导航中排除一个菜单及其后代，但不删除已有授权。

**Menu Subtree Removal（菜单子树删除）**：在同一操作中删除一个菜单、其全部后代以及对应的角色菜单授权。

**Button Menu（按钮菜单）**：代表前端操作的叶子菜单。其访问代码控制操作可见性，但绝不能代替后端权限实施。

## 动态国际化

**I18n Message Administration（国际化消息管理）**：跨分类管理动态国际化消息。消息提供展示文本，但不拥有引用它们的资源。

**Message Key（消息键）**：一条动态消息跨语言区域和分类的不可变、全局唯一身份。

**I18n Message Category（国际化消息分类）**：必需且可变的分组，用于控制管理筛选和运行时 Bundle 范围。分类不是消息身份的一部分。

**I18n Fallback Locale（国际化回退语言区域）**：当前语言区域没有消息文本时使用的语言区域。回退语言为 `en-US`，每条动态消息都必须提供该语言的值。

## 客户财务

**Setting Administration（Setting 管理）**：通过注册、提现、支付和推荐层级奖励分组管理固定运行时业务规则的 Admin 能力。它不提供任意键值创建或删除。_避免使用_：Generic Setting CRUD

**Withdrawal Export（提现导出）**：符合当前 Admin 筛选条件的全部 Customer Withdrawal 的 `.xlsx` 表示，由后端生成并直接作为导出响应返回。空结果属于业务错误，不会创建文件。

**Deposit Export（充值导出）**：符合当前 Admin 筛选条件的全部 Deposit 的 `.xlsx` 表示，由后端生成并直接作为导出响应返回。空结果属于业务错误，不会创建文件。

**Ledger Export（账本导出）**：符合当前 Admin 筛选条件的全部 Customer Balance Ledger 条目的 `.xlsx` 表示，由后端生成并直接作为导出响应返回。空结果属于业务错误，不会创建文件。

**Admin Deposit Completion（管理员完成充值）**：Admin 财务操作，通过与已验证 Provider 回调相同的 Deposit Completion 工作流，手动确认符合条件的 Deposit 已支付。它不是 Provider 回调。_避免使用_：Manual Callback、Callback Repair

**Payment Callback Log（支付回调日志）**：一次面向 Provider 的 Payment Transaction 回调尝试所形成的仅追加诊断记录。每次尝试相互独立，而 Admin Deposit Completion 绝不会创建该记录。

**Customer Credential Administration（客户凭据管理）**：Admin 无需知道旧密码即可替换所选 Customer 登录密码的能力。它实施 Customer Password Rule，绝不暴露凭据数据，并在修改提交后撤销该 Customer 的全部会话。_避免使用_：Customer Password Recovery

**Customer Access Administration（客户访问管理）**：Admin 在不删除身份的情况下启用或禁用 Customer 的能力。禁用会撤销 Customer Session 并阻止 Customer 主动操作，同时保留已有财务记录、Provider 回调、Admin 处理和被动推荐效果。_避免使用_：Customer Deletion、Financial Freeze

**Customer Administration（客户管理）**：通过分页列表和详情视图对现有 Customer 进行以读取为主的管理，并提供凭据、访问状态、Referral Reward Tier 和自动等级状态的专用操作。它不能创建或删除 Customer，也不能通用编辑身份、Profile、推荐或财务数据。_避免使用_：Customer CRUD
