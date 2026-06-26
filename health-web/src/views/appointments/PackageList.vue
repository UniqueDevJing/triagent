<template>
  <div class="page-container">
    <div class="page-header">
      <h2>套餐管理</h2>
      <el-button type="primary" @click="openDialog()">新增套餐</el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="keyword" placeholder="搜索套餐名称" clearable style="width: 260px"
        @keyup.enter="handleSearch" @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe row-key="id" @expand-change="handleExpand">
      <el-table-column type="expand" width="40">
        <template #default="{ row }">
          <div v-if="row.expandItems" class="expand-items">
            <div v-if="row.expandItems.length === 0" class="expand-empty">暂无检测项</div>
            <el-tag v-for="ei in row.expandItems" :key="ei.id" style="margin: 2px 4px">
              {{ ei.examItemName || ei.name }}
            </el-tag>
          </div>
          <div v-else class="expand-empty">加载中...</div>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">
          <span>{{ formatPrice(row.price) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page" :total="total" :page-size="size"
      @current-change="fetch" layout="prev,pager,next"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑套餐' : '新增套餐'" width="700px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" required>
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标名称，如 el-icon-xxx" />
        </el-form-item>
        <el-form-item label="检测项目">
          <div class="tree-wrapper">
            <el-input
              v-model="treeFilterText" placeholder="搜索检测项" clearable size="small"
              style="margin-bottom: 8px"
            />
            <el-tree
              ref="treeRef"
              :data="treeData"
              :props="treeProps"
              show-checkbox
              node-key="id"
              :filter-node-method="filterTreeNode"
              :default-checked-keys="checkedKeys"
              @check-change="onTreeCheckChange"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as packageApi from '@/api/modules/packages'
import * as examApi from '@/api/modules/examItems'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const dialogVisible = ref(false)
const form = ref({})
const treeRef = ref(null)
const treeFilterText = ref('')
const treeData = ref([])
const checkedKeys = ref([])
// 用于保存时组装 items 数组
const checkedItems = ref([])

const treeProps = {
  children: 'items',
  label: 'name',
  disabled: 'disabled',
}

onMounted(() => { fetch(); loadTreeData() })

watch(treeFilterText, (val) => {
  treeRef.value?.filter(val)
})

async function fetch() {
  loading.value = true
  const params = { page: page.value, size: size.value }
  if (keyword.value) params.keyword = keyword.value
  const res = await packageApi.getPackages(params)
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

function handleSearch() {
  page.value = 1
  fetch()
}

function formatPrice(val) {
  if (val == null) return '0.00'
  return '¥' + Number(val).toFixed(2)
}

async function loadTreeData() {
  const catRes = await examApi.getCategories({ page: 1, size: 999 })
  const itemRes = await examApi.getItems({ page: 1, size: 999 })
  const categories = catRes.data.records
  const items = itemRes.data.records
  treeData.value = categories.map(cat => ({
    id: `cat_${cat.id}`,
    name: cat.name,
    disabled: true,
    items: (items.filter(it => it.categoryId === cat.id)).map(it => ({
      id: `item_${it.id}`,
      name: it.name,
      rawId: it.id,
    }))
  }))
}

function filterTreeNode(value, data) {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

function onTreeCheckChange() {
  // no-op, we read checked keys on save
}

async function openDialog(row) {
  checkedKeys.value = []
  checkedItems.value = []
  if (row) {
    form.value = { ...row }
    // load current items
    try {
      const res = await packageApi.getPackageItems(row.id)
      const pkgItems = res.data || []
      checkedKeys.value = pkgItems.map(it => `item_${it.examItemId || it.id}`)
      checkedItems.value = pkgItems.map(it => ({
        examItemId: it.examItemId || it.id,
        sortOrder: it.sortOrder || 0,
      }))
    } catch {}
  } else {
    form.value = { status: 1, price: 0, name: '', description: '', icon: '' }
  }
  dialogVisible.value = true
  await nextTick()
  if (checkedKeys.value.length) {
    treeRef.value?.setCheckedKeys(checkedKeys.value)
  }
}

async function save() {
  if (!form.value.name || form.value.price == null) {
    ElMessage.warning('请填写名称和价格')
    return
  }
  // collect checked items from tree
  const halfChecked = treeRef.value?.getHalfCheckedKeys() || []
  const checked = treeRef.value?.getCheckedKeys() || []
  const allIds = [...checked, ...halfChecked]
  const itemIds = allIds.filter(k => k.startsWith('item_')).map(k => parseInt(k.replace('item_', '')))

  const itemsData = itemIds.map((id, idx) => ({ examItemId: id, sortOrder: idx + 1 }))

  const payload = {
    name: form.value.name,
    description: form.value.description || '',
    price: form.value.price,
    icon: form.value.icon || '',
    status: form.value.status ?? 1,
    items: itemsData,
  }

  if (form.value.id) {
    await packageApi.updatePackage(form.value.id, payload)
  } else {
    await packageApi.createPackage(payload)
  }
  dialogVisible.value = false
  ElMessage.success('保存成功')
  fetch()
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const payload = {
    name: row.name,
    description: row.description || '',
    price: row.price,
    icon: row.icon || '',
    status: newStatus,
    items: [],
  }
  await packageApi.updatePackage(row.id, payload)
  row.status = newStatus
  ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该套餐？', '提示', { type: 'warning' })
  await packageApi.deletePackage(row.id)
  ElMessage.success('已删除')
  fetch()
}

async function handleExpand(row, expanded) {
  if (expanded && !row.expandItems) {
    try {
      const res = await packageApi.getPackageItems(row.id)
      row.expandItems = res.data || []
    } catch {
      row.expandItems = []
    }
  }
}
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.tree-wrapper {
  border: 1px solid #DCDFE6;
  border-radius: 4px;
  padding: 8px;
  max-height: 300px;
  overflow-y: auto;
}
.expand-items {
  padding: 12px 24px;
}
.expand-empty {
  padding: 12px 24px;
  color: #909399;
  font-size: 13px;
}
</style>
