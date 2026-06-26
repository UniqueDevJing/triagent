<template>
  <div class="page-container">
    <div class="page-header">
      <h2>检测项管理</h2>
    </div>
    <el-tabs v-model="activeTab">
      <!-- Tab 1: 项目组 -->
      <el-tab-pane label="检测项目组" name="category">
        <div style="margin-bottom: 12px">
          <el-button type="primary" @click="openCategoryDialog()">新增项目组</el-button>
        </div>
        <el-table :data="categories" v-loading="catLoading" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button text size="small" @click="openCategoryDialog(row)">编辑</el-button>
              <el-button text size="small" type="danger" @click="handleDeleteCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="catPage" :total="catTotal" :page-size="catSize"
          @current-change="fetchCategories" layout="prev,pager,next" small
        />
      </el-tab-pane>

      <!-- Tab 2: 检测项 -->
      <el-tab-pane label="检测项" name="item">
        <div class="search-bar">
          <el-input
            v-model="itemKeyword" placeholder="搜索名称" clearable style="width: 200px"
            @keyup.enter="handleItemSearch" @clear="handleItemSearch"
          />
          <el-select v-model="itemCategoryId" placeholder="按项目组筛选" clearable style="width: 180px" @change="handleItemSearch">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button type="primary" @click="openItemDialog()">新增检测项</el-button>
        </div>
        <el-table :data="items" v-loading="itemLoading" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column prop="referenceRange" label="参考范围" width="160" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="categoryName" label="所属项目组" width="140" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button text size="small" @click="openItemDialog(row)">编辑</el-button>
              <el-button text size="small" type="danger" @click="handleDeleteItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="itemPage" :total="itemTotal" :page-size="itemSize"
          @current-change="fetchItems" layout="prev,pager,next"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 项目组对话框 -->
    <el-dialog v-model="catDialogVisible" :title="catForm.id ? '编辑项目组' : '新增项目组'" width="450px">
      <el-form :model="catForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="catForm.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catForm.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <!-- 检测项对话框 -->
    <el-dialog v-model="itemDialogVisible" :title="itemForm.id ? '编辑检测项' : '新增检测项'" width="550px">
      <el-form :model="itemForm" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="itemForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="itemForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="参考范围">
              <el-input v-model="itemForm.referenceRange" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="itemForm.unit" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="所属项目组" required>
          <el-select v-model="itemForm.categoryId" placeholder="请选择项目组" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as examApi from '@/api/modules/examItems'

const activeTab = ref('category')

// 项目组
const categories = ref([])
const catLoading = ref(false)
const catPage = ref(1)
const catSize = ref(20)
const catTotal = ref(0)
const catDialogVisible = ref(false)
const catForm = ref({})

// 检测项
const items = ref([])
const itemLoading = ref(false)
const itemPage = ref(1)
const itemSize = ref(20)
const itemTotal = ref(0)
const itemKeyword = ref('')
const itemCategoryId = ref('')
const itemDialogVisible = ref(false)
const itemForm = ref({})

// 项目组选项（用于检测项的下拉选择）
const categoryOptions = ref([])

onMounted(() => {
  fetchCategories()
  fetchItems()
  loadCategoryOptions()
})

async function fetchCategories() {
  catLoading.value = true
  const res = await examApi.getCategories({ page: catPage.value, size: catSize.value })
  categories.value = res.data.records
  catTotal.value = res.data.total
  catLoading.value = false
}

async function loadCategoryOptions() {
  const res = await examApi.getCategories({ page: 1, size: 999 })
  categoryOptions.value = res.data.records
}

function openCategoryDialog(row) {
  catForm.value = row ? { ...row } : { sortOrder: 0 }
  catDialogVisible.value = true
}

async function saveCategory() {
  if (!catForm.value.name) {
    ElMessage.warning('请输入名称')
    return
  }
  if (catForm.value.id) {
    await examApi.updateCategory(catForm.value.id, catForm.value)
  } else {
    await examApi.createCategory(catForm.value)
  }
  catDialogVisible.value = false
  ElMessage.success('保存成功')
  fetchCategories()
  loadCategoryOptions()
}

async function handleDeleteCategory(row) {
  await ElMessageBox.confirm('确认删除该项目组？', '提示', { type: 'warning' })
  await examApi.deleteCategory(row.id)
  ElMessage.success('已删除')
  fetchCategories()
  loadCategoryOptions()
}

async function fetchItems() {
  itemLoading.value = true
  const params = { page: itemPage.value, size: itemSize.value }
  if (itemKeyword.value) params.keyword = itemKeyword.value
  if (itemCategoryId.value) params.categoryId = itemCategoryId.value
  const res = await examApi.getItems(params)
  items.value = res.data.records
  itemTotal.value = res.data.total
  itemLoading.value = false
}

function handleItemSearch() {
  itemPage.value = 1
  fetchItems()
}

function openItemDialog(row) {
  itemForm.value = row ? { ...row } : { categoryId: '' }
  itemDialogVisible.value = true
}

async function saveItem() {
  if (!itemForm.value.name || !itemForm.value.categoryId) {
    ElMessage.warning('请填写名称并选择所属项目组')
    return
  }
  if (itemForm.value.id) {
    await examApi.updateItem(itemForm.value.id, itemForm.value)
  } else {
    await examApi.createItem(itemForm.value)
  }
  itemDialogVisible.value = false
  ElMessage.success('保存成功')
  fetchItems()
}

async function handleDeleteItem(row) {
  await ElMessageBox.confirm('确认删除该检测项？', '提示', { type: 'warning' })
  await examApi.deleteItem(row.id)
  ElMessage.success('已删除')
  fetchItems()
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
</style>
