import request from '../utils/request'
import { API_URLS } from '../config/api'

export function getDashboardSummary() {
  return request.get(API_URLS.dashboard.summary)
}

export function getClientList(params) {
  return request.get(API_URLS.client.list, { params })
}

export function getMessageList(params) {
  return request.get(API_URLS.message.list, { params })
}

export function getMessageConfig(messageId) {
  return request.get(API_URLS.message.config(messageId))
}

export function getMessageData(messageId) {
  return request.get(API_URLS.message.data(messageId))
}

export function getTaskList(params) {
  return request.get(API_URLS.task.list, { params })
}

export function getTaskMonitor(taskId) {
  return request.get(API_URLS.task.monitor(taskId))
}

export function getTemplateList() {
  return request.get(API_URLS.template.list)
}

export function getTemplateCustomData(id) {
  return request.get(API_URLS.template.customData(id))
}

export function getTemplateInputData(id) {
  return request.get(API_URLS.template.inputData(id))
}

export function addTemplate(data) {
  return request.post(API_URLS.template.add, data)
}

export function updateTemplate(data) {
  return request.put(API_URLS.template.update, data)
}

export function deleteTemplate(id) {
  return request.delete(API_URLS.template.delete(id))
}
