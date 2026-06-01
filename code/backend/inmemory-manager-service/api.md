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