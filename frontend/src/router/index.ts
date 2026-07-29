import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/order/:tableNumber', name: 'order', component: () => import('@/views/customer/OrderPage.vue') },
    { path: '/order-status/:orderId', name: 'orderStatus', component: () => import('@/views/customer/OrderStatus.vue') },
    { path: '/kitchen', name: 'kitchen', component: () => import('@/views/kitchen/KitchenBoard.vue') },
    { path: '/admin/dishes', name: 'adminDishes', component: () => import('@/views/admin/DishManage.vue') },
    { path: '/admin', name: 'adminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
    { path: '/admin/categories', name: 'adminCategories', component: () => import('@/views/admin/CategoryManage.vue') },
    { path: '/admin/tables', name: 'adminTables', component: () => import('@/views/admin/TableManage.vue') },
    { path: '/admin/orders', name: 'adminOrders', component: () => import('@/views/admin/OrderView.vue') },
    { path: '/admin/combos', name: 'adminCombos', component: () => import('@/views/admin/ComboManage.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ]
})

router.beforeEach((to) => {
  const auth = sessionStorage.getItem('auth')
  const needsAuth = to.path.startsWith('/admin') || to.path === '/kitchen'
  if (needsAuth && !auth) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
