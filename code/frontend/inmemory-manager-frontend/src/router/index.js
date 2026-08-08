import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layouts/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'client',
        name: 'Client',
        component: () => import('@/views/ClientView.vue'),
        meta: { title: '客户端监控', icon: 'Monitor' }
      },
      {
        path: 'queue',
        name: 'Queue',
        component: () => import('@/views/QueueView.vue'),
        meta: { title: '消息监控', icon: 'List' }
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/TaskView.vue'),
        meta: { title: '任务监控', icon: 'Tickets' }
      },
      {
        path: 'template',
        name: 'Template',
        component: () => import('@/views/TemplateView.vue'),
        meta: { title: '模板管理', icon: 'Document' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory('/inmemory-manager/inmemory-manager-frontend/'),
  routes
})

export default router
