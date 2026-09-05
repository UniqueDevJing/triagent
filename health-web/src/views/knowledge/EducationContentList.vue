<template>
  <div class="page-container">
    <div class="page-header">
      <h2>宣教内容管理</h2>
      <el-button type="primary" @click="openDialog()">新增内容</el-button>
    </div>
    <div class="page-filters">
      <el-input v-model="keyword" placeholder="搜索标题..." style="width:200px" clearable @change="fetch" />
      <el-select v-model="typeFilter" placeholder="内容类型" clearable style="width:140px" @change="fetch">
        <el-option label="文章" value="ARTICLE" />
        <el-option label="视频" value="VIDEO" />
        <el-option label="信息图" value="INFOGRAPHIC" />
      </el-select>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }"><el-tag size="small">{{ typeMap[row.contentType] || row.contentType }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="author" label="作者" width="100" />
      <el-table-column prop="viewCount" label="阅读数" width="80" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" @click="viewContent(row)">预览</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState title="暂无宣教内容" description="创建健康教育内容，帮助会员了解健康知识" icon="Reading" action-text="新增内容" @action="openDialog()" />
      </template>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑宣教内容' : '新增宣教内容'" width="700px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.contentType"><el-option v-for="(v,k) in typeMap" :key="k" :label="v" :value="k" /></el-select></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="作者"><el-input v-model="form.author" /></el-form-item>
        <el-form-item label="媒体URL"><el-input v-model="form.mediaUrl" placeholder="视频/图片链接" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save" :loading="saving">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="内容预览" width="700px">
      <div v-html="previewContent" style="max-height:500px;overflow-y:auto" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/modules/knowledgeAdmin'
import EmptyState from '@/components/EmptyState.vue'

const typeMap = { ARTICLE: '文章', VIDEO: '视频', INFOGRAPHIC: '信息图' }
const tableData = ref([])
const loading = ref(false)
const page = ref(1), size = ref(20), total = ref(0)
const keyword = ref(''), typeFilter = ref('')
const dialogVisible = ref(false), form = ref({ contentType: 'ARTICLE' })
const previewVisible = ref(false), previewContent = ref('')

const saving = ref(false)

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  try {
    const res = await api.getEducationContents({ page: page.value, size: size.value, keyword: keyword.value, contentType: typeFilter.value })
    tableData.value = res.data.records; total.value = res.data.total
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}
function openDialog(row) { form.value = row ? { ...row } : { contentType: 'ARTICLE' }; dialogVisible.value = true }
async function save() {
  saving.value = true
  try {
    if (form.value.id) await api.updateEducationContent(form.value.id, form.value)
    else await api.createEducationContent(form.value)
    dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
  } catch (e) { ElMessage.error(e?.message || '保存失败') }
  saving.value = false
}
function viewContent(row) { previewContent.value = row.content; previewVisible.value = true }
async function handleDelete(row) {
  try { await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }) } catch { return }
  try {
    await api.deleteEducationContent(row.id); ElMessage.success('已删除'); fetch()
  } catch (e) { ElMessage.error(e?.message || '删除失败') }
}
</script>
