# inmemory-manager-service接口文档

## 接口列表

### 健康检查

- URL: /inmemory-manager/inmemory-manager-service/api/health
- Method: GET
- Description: 检查服务运行状态
- Request Parameters: 无
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: object - 响应数据
    - status: string - 服务状态(UP)
    - timestamp: string - 当前时间

### 客户端列表

- URL: /inmemory-manager/inmemory-manager-service/api/client/list
- Method: GET
- Description: 查询客户端列表，支持按状态和关键字筛选
- Request Parameters:
  - status: string - 客户端状态（可选）
  - keyword: string - 搜索关键字（可选）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: array - 客户端列表
    - clientId: string - 客户端ID
    - address: string - 客户端地址
    - status: string - 客户端状态
    - lastHeartbeat: string - 最近心跳时间
    - firstConnect: string - 首次连接时间

### 仪表盘汇总

- URL: /inmemory-manager/inmemory-manager-service/api/dashboard/summary
- Method: GET
- Description: 查询仪表盘汇总信息（客户端、队列、任务统计）
- Request Parameters: 无
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: object - 汇总数据
    - client: object - 客户端统计
      - connected: int - 已连接数
      - disconnected: int - 已断开数
    - queue: object - 队列统计
      - completed: int - 已完成数
      - pending: int - 待处理数
      - deadLetter: int - 死信数
    - task: object - 任务统计（基于任务监控数据，最近2天）
      - success: int - 成功数
      - failed: int - 失败数
      - periodStart: string - 统计周期开始时间
      - periodEnd: string - 统计周期结束时间

### 消息列表

- URL: /inmemory-manager/inmemory-manager-service/api/message/list
- Method: GET
- Description: 查询消息列表，支持按状态和关键字筛选
- Request Parameters:
  - status: string - 消息状态（可选）
  - keyword: string - 搜索关键字（可选）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: array - 消息列表
    - messageId: string - 消息ID
    - taskId: string - 任务ID
    - batchId: string - 批次ID
    - status: string - 消息状态
    - createTime: string - 创建时间

### 消息配置

- URL: /inmemory-manager/inmemory-manager-service/api/message/{messageId}/config
- Method: GET
- Description: 查询指定消息的配置信息
- Request Parameters:
  - messageId: string - 消息ID（路径参数）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: object - 消息配置
    - resolverId: string - 解析器ID
    - retryCount: int - 重试次数
    - timeout: long - 超时时间(毫秒)

### 消息数据

- URL: /inmemory-manager/inmemory-manager-service/api/message/{messageId}/data
- Method: GET
- Description: 查询指定消息的承载数据
- Request Parameters:
  - messageId: string - 消息ID（路径参数）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: object - 消息数据
    - taskId: string - 任务ID
    - batchId: string - 批次ID
    - payload: object - 承载数据键值对

### Redis Stream 信息

- URL: /inmemory-manager/inmemory-manager-service/api/stream/info
- Method: GET
- Description: 查询 Redis Stream 总消息数、各消息属性与状态
- Request Parameters: 无
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: object - Stream 信息
    - streamName: string - Stream 名称
    - totalMessageCount: long - 总消息数
    - messages: array - 消息列表
      - messageId: string - 消息ID
      - taskId: string - 任务ID
      - batchId: string - 批次ID
      - resolverId: string - 解析器ID
      - status: string - 消息状态
      - consumerId: string - 消费者ID

### 任务列表

- URL: /inmemory-manager/inmemory-manager-service/api/task/list
- Method: GET
- Description: 查询任务列表，默认查询最近2天，支持按状态、任务ID和时间范围（startTime/endTime 对应 start_time 范围）筛选；status 取值 success/running/failed
- Request Parameters:
  - status: string - 任务状态（可选）
  - taskId: string - 任务ID（可选）
  - startTime: string - 开始时间（可选）
  - endTime: string - 结束时间（可选）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: array - 任务列表
    - taskId: string - 任务ID
    - completedBatch: int - 已完成批次数
    - totalBatch: int - 总批次数
    - status: string - 任务状态
    - startTime: string - 开始时间
    - endTime: string - 结束时间

### 任务监控

- URL: /inmemory-manager/inmemory-manager-service/api/task/{taskId}/monitor
- Method: GET
- Description: 查询指定任务的监控详情（统计与执行明细）
- Request Parameters:
  - taskId: string - 任务ID（路径参数）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: object - 任务监控信息
    - taskId: string - 任务ID
    - statistics: object - 任务统计
      - allCount: int - 总数
      - doneCount: int - 已完成数
      - runningCount: int - 运行中数
      - errorCount: int - 错误数
      - status: int - 状态码
      - startTime: string - 开始时间
      - endTime: string - 结束时间
      - nowTime: string - 当前时间
      - duration: long - 执行时长(毫秒)，有endTime为endTime-startTime，无则为数据库当前时间-startTime
      - errorStack: array - 错误堆栈信息
    - executes: array - 执行明细列表
      - executeId: string - 执行ID
      - executeType: string - 执行类型
      - runCount: int - 运行次数
      - errorCount: int - 错误次数
      - inCount: int - 输入数
      - outCount: int - 输出数
      - duration: int - 持续时间(毫秒)
      - avgIn: int - 平均输入
      - avgOut: int - 平均输出
      - outPerSecond: int - 每秒输出
      - createTime: string - 创建时间
      - updateTime: string - 更新时间
      - errorStack: array - 错误堆栈信息

### 模板列表

- URL: /inmemory-manager/inmemory-manager-service/api/template/list
- Method: GET
- Description: 查询执行模板列表（按更新时间倒序）
- Request Parameters: 无
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: array - 模板列表
    - id: int - 模板ID
    - templateName: string - 模板名称
    - resolverId: string - 解析器ID
    - subResolverId: string - 子解析器ID
  - inputId: string - 输入统计ID
  - outputId: string - 输出统计ID
    - createTime: string - 创建时间
    - updateTime: string - 更新时间

### 模板定制数据

- URL: /inmemory-manager/inmemory-manager-service/api/template/{id}/customData
- Method: GET
- Description: 查询指定模板的定制数据(customData)
- Request Parameters:
  - id: int - 模板ID（路径参数）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: string - 定制数据内容

### 模板输入数据

- URL: /inmemory-manager/inmemory-manager-service/api/template/{id}/inputData
- Method: GET
- Description: 查询指定模板的输入数据(inputData)
- Request Parameters:
  - id: int - 模板ID（路径参数）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息
  - data: string - 输入数据内容

### 新增模板

- URL: /inmemory-manager/inmemory-manager-service/api/template
- Method: POST
- Description: 新增执行模板（id 由数据库自增生成，create_time/update_time 自动写入）
- Request Parameters（Body JSON）:
  - templateName: string - 模板名称（必填）
  - resolverId: string - 解析器ID
  - subResolverId: string - 子解析器ID
  - inputId: string - 输入统计ID
  - outputId: string - 输出统计ID
  - customData: string - 定制数据
  - inputData: string - 输入数据
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息

### 修改模板

- URL: /inmemory-manager/inmemory-manager-service/api/template
- Method: PUT
- Description: 按主键修改模板（create_time 不变，update_time 自动刷新）
- Request Parameters（Body JSON）:
  - id: int - 模板ID（必填）
  - templateName: string - 模板名称
  - resolverId: string - 解析器ID
  - subResolverId: string - 子解析器ID
  - inputId: string - 输入统计ID
  - outputId: string - 输出统计ID
  - customData: string - 定制数据
  - inputData: string - 输入数据
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息

### 删除模板

- URL: /inmemory-manager/inmemory-manager-service/api/template/{id}
- Method: DELETE
- Description: 按主键删除模板
- Request Parameters:
  - id: int - 模板ID（路径参数）
- Response:
  - code: int - 响应状态码
  - message: string - 响应消息

### 执行模板

- URL: /inmemory-manager/inmemory-manager-service/api/template/{id}/execute
- Method: POST
- Description: 按模板发起执行请求，后端转发到执行服务(inmemory.execute.service+/api/inmemory/executeResolver)，并将结果记录到inmemory_task
- Request Parameters:
  - id: int - 模板ID（路径参数）
- Response:
  - code: int - 响应状态码（200成功，500失败）
  - message: string - 响应消息（失败时为错误信息）
  - data: object - 执行结果（成功时返回）
    - taskId: string - 任务ID（失败时无此字段）
