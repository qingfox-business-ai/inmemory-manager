const BASE_URL = '/inmemory-manager/inmemory-manager-service/api'

export const API_URLS = {
  dashboard: {
    summary: `${BASE_URL}/dashboard/summary`
  },
  client: {
    list: `${BASE_URL}/client/list`
  },
  message: {
    list: `${BASE_URL}/message/list`,
    config: (messageId) => `${BASE_URL}/message/${messageId}/config`,
    data: (messageId) => `${BASE_URL}/message/${messageId}/data`
  },
  task: {
    list: `${BASE_URL}/task/list`,
    monitor: (taskId) => `${BASE_URL}/task/${taskId}/monitor`,
    batches: (taskId) => `${BASE_URL}/task/${taskId}/batches`
  },
  template: {
    list: `${BASE_URL}/template/list`,
    customData: (id) => `${BASE_URL}/template/${id}/customData`,
    inputData: (id) => `${BASE_URL}/template/${id}/inputData`,
    add: `${BASE_URL}/template`,
    update: `${BASE_URL}/template`,
    delete: (id) => `${BASE_URL}/template/${id}`
  },
  stream: {
    messages: `${BASE_URL}/stream/messages`
  }
}
