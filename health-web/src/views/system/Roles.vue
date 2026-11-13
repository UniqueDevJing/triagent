<template>
  <div class="page-container">
    <div class="page-header"><h2>角色管理</h2></div>
    <el-table :data="tableData" stripe>
      <el-table-column prop="name" label="角色" />
      <el-table-column prop="code" label="编码" />
      <el-table-column label="菜单权限" width="200">
        <template #default="{ row }">
          <el-button text size="small" @click="openMenuDialog(row)">配置菜单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="menuVisible" title="配置菜单权限" width="400px">
      <el-checkbox-group v-model="selectedMenus">
        <el-checkbox v-for="m in menus" :key="m.path" :value="m.path" :label="m.name" style="display:block;margin-bottom:8px" />
      </el-checkbox-group>
      <template #footer><el-button @click="menuVisible = false">取消</el-button><el-button type="primary" @click="saveMenus">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as sysApi from '@/api/modules/system'

const tableData = ref([])
const menus = ref([])
const menuVisible = ref(false)
const selectedMenus = ref([])
let currentRole = null

onMounted(async () => {
  const [r, m] = await Promise.all([sysApi.getRoles({ page: 1, size: 20 }), sysApi.getMenuTree()])
  tableData.value = r.data.records; menus.value = m.data
})

function openMenuDialog(row) {
  currentRole = row
  selectedMenus.value = typeof row.menus === 'string' ? JSON.parse(row.menus) : (row.menus || [])
  menuVisible.value = true
}

async function saveMenus() {
  await sysApi.updateRoleMenus(currentRole.id, JSON.stringify(selectedMenus.value))
  currentRole.menus = JSON.stringify(selectedMenus.value)
  menuVisible.value = false; ElMessage.success('保存成功')
}
</script>
