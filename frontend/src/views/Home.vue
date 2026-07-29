<template>
  <div class="home">
    <h1 class="home-title">在线点餐系统</h1>
    <div class="home-cards">
      <router-link to="/order/A1" class="home-card customer">
        <div class="card-icon">📱</div>
        <div class="card-label">顾客点餐</div>
        <div class="card-desc">扫码进入点餐页面</div>
      </router-link>
      <router-link to="/kitchen" class="home-card kitchen">
        <div class="card-icon">👨‍🍳</div>
        <div class="card-label">后厨看板</div>
        <div class="card-desc">查看和处理订单</div>
      </router-link>
      <router-link to="/admin/dishes" class="home-card admin">
        <div class="card-icon">⚙️</div>
        <div class="card-label">管理后台</div>
        <div class="card-desc">管理菜品、分类、餐桌</div>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg);
}
.home-title {
  font-size: var(--font-size-xxl);
  margin-bottom: var(--spacing-xl);
}
.home-cards {
  display: flex;
  gap: var(--spacing-lg);
  flex-wrap: wrap;
  justify-content: center;
}
.home-card {
  width: 240px;
  padding: var(--spacing-xl);
  background: var(--color-white);
  border-radius: var(--radius-lg);
  text-align: center;
  transition: transform 0.2s;
}
.home-card:hover { transform: translateY(-4px); }
.card-icon { font-size: 48px; }
.card-label { font-size: var(--font-size-xl); font-weight: bold; margin-top: var(--spacing-md); }
.card-desc { font-size: var(--font-size-sm); color: var(--color-text-light); margin-top: var(--spacing-sm); }
.customer { border-top: 4px solid var(--color-primary); }
.kitchen { border-top: 4px solid var(--color-warning); }
.admin { border-top: 4px solid var(--color-success); }
</style>
