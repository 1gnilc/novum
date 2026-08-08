# Mobile Application

[English](CONTEXT.md)

Mobile Application 独立于管理系统为 Customer 提供服务。

## 身份

**Customer（客户）**：可以使用 Mobile Application 的个人身份。它与 Admin User 不同，不继承管理员角色、导航或会话契约。_避免使用_：Mobile User、Admin User、End User、App User

**Customer Session（客户会话）**：Customer 经过认证的登录状态。它独立于 Admin Session，可以在不改变 Customer 身份的情况下刷新或撤销。

**Customer Online Presence（客户在线状态）**：Customer 级 Redis 标记，表示任意 Customer Session 在过去 15 分钟内调用过 `GET /customer/user-info`。每次调用都会续期共享生命周期；一个会话退出不会移除该标记，且只有此前不存在在线状态时才会更改活跃 IP 字段。

**Customer Access Baseline Role（客户访问基线角色）**：每个有效 Customer 都必须保留的内置 `customer` 角色。_避免使用_：Customer Admin Role、Mobile User Role

**Country Calling Code（国际区号）**：限定 Customer 本地手机号的国际电话前缀。Customer 注册和登录当前只支持 `+1` 和 `+234`；界面语言与该值相互独立。_避免使用_：Area Code、Registration Market、Language Code

**Customer Phone Identity（客户手机号身份）**：`dialCode` 与归一化本地 `phone` 的唯一组合，用于注册和登录。`phone` 只包含本地号码数字；Customer 没有单独的用户名或拼接后的完整手机号值。_避免使用_：Username、Full Phone String

**Phone Validation（手机号校验）**：后端使用 Google libphonenumber 和 `dialCode` 隐含的国家规则校验 Customer Phone Identity。Mobile 将选择范围限制在受支持区号内，但不拥有最终有效性决策。

**Customer Password Rule（客户密码规则）**：Customer 密码长度为 8-64 个字符，必须至少包含大写字母、小写字母、数字和特殊字符这四类中的三类，并且不能包含空白。密码使用 BCrypt 存储，同一规则适用于注册和每次密码修改。Customer 自助服务只支持经过认证的“当前密码改为新密码”；成功后撤销全部 Customer Session，且不存在忘记密码找回流程。

**Customer Profile（客户资料）**：由 Customer 维护的 `nickname`、`avatar` 和可选 `email`。它不包括 Customer Phone Identity、Invitation Code、Referral Relationship、Referral Reward Tier、Customer Account Status、财务值或登录密码。

**Customer Account Status（客户账户状态）**：Customer 的启用或禁用访问状态。禁用状态会撤销 Customer Session，并阻止登录、刷新和 Customer 主动操作，但不会删除身份、取消已有财务记录、移除 Referral Relationship、抑制被动 Referral Reward、暂停 Qualified Direct Referral 计数或暂停自动 Referral Reward Tier Progression。重新启用后必须重新登录。财务限制属于不同关注点。_避免使用_：Reward Status、Financial Freeze

## 财务

**Platform Currency（平台货币）**：所有 Customer 余额和财务记录使用的唯一货币。当前每个 Customer 的 Platform Currency 都是尼日利亚奈拉（`NGN`），与 Country Calling Code 无关。

**Financial Amount Rule（财务金额规则）**：每个 Platform Currency 金额都恰好保留两位小数，计算值使用 `HALF_UP` 舍入。财务金额是定点数值，不是二进制浮点数。

**Deposit（充值）**：Customer 通过 Deposit Channel 增加资金的请求，包括待处理、已支付或已取消生命周期。中文产品标签保持为“充值”。_避免使用_：Recharge、Top Up

**Deposit Channel（充值渠道）**：已部署的 Deposit Channel Strategy 代码为支付 Provider 路由使用的配置。只有在启用状态，且所选 Deposit Amount Option 位于其配置范围内时才可选择；Admin 持久化不代表当前代码支持该配置，之后的更新、禁用或删除不会改变已有交易。

**Deposit Channel Snapshot（充值渠道快照）**：创建时复制到 Payment Transaction 的不可变渠道展示与执行配置。支付发起、回调和历史记录使用该快照，而不是渠道后续状态。

**Deposit Gateway（充值网关）**：由 Admin 配置、复制到 Deposit Channel Snapshot 并由 Deposit Channel Strategy 检查的 Provider 支付端点，大多数情况下应为 URL。它不是持久化的策略身份。_避免使用_：Strategy Key、Channel Name

**Deposit Amount Option（充值金额选项）**：Customer 发起 Deposit 时必须选择的预定义金额，包含关联的赠送金额。所选金额和赠送金额会被快照到 Deposit 中，不随后续选项管理改变。

**Deposit Principal（充值本金）**：Customer 通过 Deposit Channel 成功支付的金额。它是 Referral Level Reward 和 Referral Tier Reward 的计算基数，不包含任何 Deposit Gift。

**Deposit Credit（充值入账金额）**：已支付 Deposit 增加到 Customer 余额中的总金额，由 Deposit Principal 加上任何适用的 Deposit Gift 组成。

**Customer Deposit Total（客户充值总额）**：Customer 已支付 Deposit 的累计 Deposit Principal，存储为 `depositTotal`。它不包括 Deposit Gift、Registration Gift、Referral Reward 和其他任何余额变动；只要已完成 Deposit 保持最终状态，该值就不会减少。

**Registration Gift（注册赠送）**：从已配置 Setting 一次性计入新注册 Customer 的收入。它以独立账本类型记录，并且只有注册事务提交时才存在。

**Payment Transaction（支付交易）**：一个 Deposit 对应的一对一面向 Provider 的支付流程，包括发起、支付 URL、回调处理和完成状态。它与 Deposit 业务记录不同。_避免使用_：Deposit Transaction、Recharge Transaction

**Payment Redirect URL（支付重定向 URL）**：支付后 Provider 将 Customer 浏览器重定向到的 Mobile 目的地。Mobile 在 Deposit 请求中以 `redirectUrl` 提供该值，并受后端配置的 Mobile Origin 边界限制。_避免使用_：Return URL、Callback URL

**Payment Callback URL（支付回调 URL）**：由后端生成、供 Provider 报告交易结果的端点。它不由 Mobile 或 Admin 提供，并且与 Customer 浏览器的 `redirectUrl` 不同。_避免使用_：Return URL

**Withdrawal（提现）**：Customer 通过 Withdrawal Account 从余额中移除资金的请求。中文产品标签保持为“提现”；后续银行付款操作是 Payout。_避免使用_：Cash Out、Payout

**Withdrawal Account（提现账户）**：Customer 保存的持卡人、银行、账号和支付密码组合之一，用于提交 Withdrawal。一个 Customer 可以拥有多个 Withdrawal Account，每个账户都有独立支付密码。删除账户会释放其 Bank Account Identity；之后的绑定是新的 Withdrawal Account，不是恢复已删除账户。

**Withdrawal Account Payment Password（提现账户支付密码）**：由一个 Withdrawal Account 拥有的六位凭据，提交 Withdrawal 或修改该账户详情、密码时需要使用。只有证明当前值后才能修改；没有重置流程，连续失败五次后会针对该账户临时锁定。_避免使用_：Customer Payment Password、Withdrawal PIN

**Bank Account Identity（银行账户身份）**：Withdrawal Bank 代码和国家无关银行账户标识符的组合，在有效 Withdrawal Account 中全局唯一。删除会释放该身份，之后任意 Customer 都可独立绑定；账户标识符不假设固定国家格式。_避免使用_：单独使用 Account Number

**Withdrawal Bank（提现银行）**：Withdrawal Account 可使用的银行，通过唯一且不可变的银行代码标识。已禁用银行不能用于新账户或 Withdrawal，但对已有账户和历史 Withdrawal 仍有意义。_避免使用_：Affiliated Bank

**Withdrawal Amount Option（提现金额选项）**：Customer 提交 Withdrawal 时可以选择的预定义金额。Customer 不能输入任意金额，每个选项都必须保持在配置的 Withdrawal 范围内。

**Withdrawal Fee（提现手续费）**：根据申请 Withdrawal 金额按百分比计算的费用。计算出的费用金额和最终净支付金额在提交时固定到 Withdrawal 中。

**Withdrawal Processing Window（提现处理时间窗口）**：Mobile 向 Customer 展示、说明 Withdrawal 处理时间的每日时间范围。它不是提交窗口；Customer 可以随时提交 Withdrawal。_避免使用_：Withdrawal Time Limit、Submission Window

**Withdrawal Hold（提现占款）**：创建待处理 Withdrawal 时从可用余额中移除的完整申请金额。非最终 Withdrawal 代表被占用的资金；批准和 Payout 不会再次扣除。

**Withdrawal Release（提现释放）**：待处理 Withdrawal 被拒绝时恢复 Withdrawal Hold。它不是 Deposit 退款或冲正。_避免使用_：Withdrawal Refund

**Customer Withdrawal（客户提现）**：持久化的 Withdrawal 请求，只有 `pending -> rejected` 或 `pending -> approved -> paid` 单向进程。已拒绝和已支付的 Withdrawal 都是最终状态，Customer 无法取消。

**Withdrawal Account Snapshot（提现账户快照）**：提交 Withdrawal 时，后端从 Customer 所选 Withdrawal Account 复制的不可变账户持有人、银行和账号事实。之后修改或删除账户都不会改变该快照。

**Payout（打款）**：将已批准 Withdrawal 发送到 Customer 银行账户的运营操作。在当前范围内，它在导出后于 Novum 外部手工执行。_避免使用_：Withdrawal

## 推荐

**Referral Reward Tier（推荐奖励等级）**：Customer 的奖励费率等级，根据 Qualified Direct Referral 数量提升，并用于计算直接推荐人的 Deposit 奖励。等级包括 regular、gold、platinum 和 diamond；regular 的 Referral Tier Reward 费率为零。_避免使用_：Membership Tier、Benefit Tier、Invitation Benefit Tier

**Referral Reward Tier Progression（推荐奖励等级进程）**：从 regular 到 gold、platinum、diamond 的上升进程。每个新 Qualified Direct Referral 使计数增加一，并且最多使 Customer 提升一个等级。

**Managed Referral Reward Tier（受管理推荐奖励等级）**：由 Admin 维护的三个 Referral Reward Tier 之一：gold、platinum 或 diamond。Regular 是 Customer 的隐式默认等级，不是受管理的等级记录。

**Qualified Direct Referral（合格直接推荐）**：由某 Customer 直接推荐，且任意金额的首次成功 Deposit 已完成的 Customer。每个 Customer 对直接邀请人的合格计数最多贡献一次。在当前范围内，已完成 Deposit 是最终状态。

**Invitation Code（邀请码）**：分配给 Customer、用于注册绑定的全局唯一六位数字代码。前导零有意义；`000000` 无效。_避免使用_：Referral Code、Invite Token

**Invitation Code Immutability（邀请码不可变性）**：Customer 的 Invitation Code 在创建时分配，不能通过 Customer 或 Admin 工作流修改。只有经过审计的例外数据修复才能纠正无效分配。

**Referrer（推荐人）**：Customer 不可变的直接邀请人，存储为 `referrerId`。它是直接邀请归属的唯一事实来源；三级关系记录由其派生。_避免使用_：`pid`、Parent Account

**Referral Relationship（推荐关系）**：从祖先 Customer 到被推荐 Customer 的一条派生祖先记录，层级恰好为 L1、L2 或 L3。祖先/被推荐人组合唯一，支持无需递归遍历的团队和奖励查询。

**Referral Level Reward（推荐层级奖励）**：由 Deposit 提供资金，使用各关系层级配置费率，分别为充值人的 L1、L2 和 L3 推荐人计算的奖励。_避免使用_：Level Rebate、Level Commission

**Referral Tier Reward（推荐等级奖励）**：由 Deposit 提供资金，只使用充值人直接推荐人的当前 Referral Reward Tier 费率为其计算的奖励。_避免使用_：Tier Rebate、Benefit Reward

**Deposit Completion（充值完成）**：由已验证 Provider 回调或 Admin Deposit Completion 发起的一次性原子转换；它将 Payment Transaction 标记为已完成、将 Deposit 标记为已支付、增加 Customer 余额、记录余额账本、应用两种 Referral Reward 计划，并评估首次 Deposit 合格状态。重复完成尝试不能再次执行这些效果。

**Final Deposit（最终充值）**：资金、合格效果和 Referral Reward 均为最终状态的已完成 Deposit。当前范围没有 Deposit 退款或冲正工作流。

**Customer Balance Ledger（客户余额账本）**：Customer 余额变动的仅追加记录。条目永远不能更新或删除；每次余额变化都会原子创建一个新条目。

**Ledger Reference（账本引用）**：导致某条 Customer Balance Ledger Entry 的业务记录身份。条目类型决定该引用标识何种资源，同一类型和引用对一个 Customer 只能产生一次影响。_避免使用_：Generic Source

**Deposit Ledger Entry（充值账本条目）**：链接到 Payment Transaction 的收入条目。Deposit Principal 使用 `DEPOSIT` 类型；非零 Deposit Gift 使用独立的 `DEPOSIT_GIFT` 类型。

**Referral Reward Ledger Entry（推荐奖励账本条目）**：一笔 Referral Level Reward 或 Referral Tier Reward 的独立收入条目，链接到触发它的 Payment Transaction。两种计划绝不会合并为一个账本条目。

**Platform-Funded Referral Reward（平台出资的推荐奖励）**：平台在充值人的 Deposit Credit 之外计入的 Referral Level Reward 或 Referral Tier Reward。它绝不会减少充值人的本金、赠送金额或余额入账金额，也不存在单独的平台资金余额来阻止其结算。

**Reward Settlement Order（奖励结算顺序）**：Deposit Completion 期间，系统先计入 Deposit、记录首次 Deposit 合格状态、在达到门槛时提升直接推荐人的 Referral Reward Tier，然后使用由此得到的当前等级以及该完成事务读取的最新已提交奖励费率配置，计算 Referral Level Reward 和 Referral Tier Reward。

**Referral Level Reward Rate（推荐层级奖励费率）**：由后端拥有的 Setting 值之一，决定 L1、L2 或 L3 Referral Level Reward 的计算费率。Mobile 通过 API 使用展示配置，不拥有 Setting Key。
