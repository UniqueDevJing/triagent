<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h2 class="page-title">知识库</h2>
      <p class="page-subtitle">健康知识文章管理与分享</p>
    </div>
    <el-row :gutter="20" class="content-row">
      <!-- 分类侧栏 -->
      <el-col :span="5">
        <el-card class="side-card">
          <template #header><span>知识分类</span></template>
          <el-menu :default-active="activeCategory" @select="onCategorySelect">
            <el-menu-item index="">
              <el-icon><Grid /></el-icon><span>全部</span>
            </el-menu-item>
            <el-menu-item v-for="cat in categories" :key="cat.id" :index="String(cat.id)">
              <el-icon><component :is="cat.icon || 'Folder'" /></el-icon>
              <span>{{ cat.name }}</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- 文章列表 -->
      <el-col :span="19">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-input v-model="searchKeyword" placeholder="搜索文章..." clearable style="width: 280px;" @keyup.enter="fetchArticles" />
                <el-button type="primary" @click="fetchArticles" style="margin-left: 12px;">搜索</el-button>
              </div>
              <el-button type="primary" @click="showArticleDialog(null)">新建文章</el-button>
            </div>
          </template>

          <el-row :gutter="20" v-loading="loading">
            <el-col :span="8" v-for="article in articles" :key="article.id">
              <el-card shadow="hover" class="article-card" @click="viewArticle(article)">
                <h4>{{ article.title }}</h4>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <span><el-icon><User /></el-icon> {{ article.author }}</span>
                  <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
                  <span>{{ article.createdAt?.slice(0, 10) }}</span>
                </div>
              </el-card>
            </el-col>
            <el-col :span="24" v-if="articles.length === 0 && !loading">
              <el-empty description="暂无文章" />
            </el-col>
          </el-row>

          <el-pagination
            v-model:current-page="page"
            :total="total"
            layout="total, prev, pager, next"
            @change="fetchArticles"
            style="margin-top: 20px; justify-content: center;"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 文章详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentArticle?.title" width="800px">
      <div class="article-content" v-html="currentArticle?.content"></div>
      <div class="article-info">
        作者: {{ currentArticle?.author }} | 阅读: {{ currentArticle?.viewCount }}
      </div>
    </el-dialog>

    <!-- 新建/编辑文章弹窗 -->
    <el-dialog v-model="articleDialogVisible" :title="editArticleId ? '编辑文章' : '新建文章'" width="700px">
      <el-form ref="articleFormRef" :model="articleForm" :rules="articleRules" label-width="80px">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="articleForm.categoryId" style="width: 100%" placeholder="请选择分类">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title"><el-input v-model="articleForm.title" placeholder="请输入文章标题" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="articleForm.summary" type="textarea" :rows="2" placeholder="请输入文章摘要" /></el-form-item>
        <el-form-item label="内容">
          <el-input v-model="articleForm.content" type="textarea" :rows="10" placeholder="支持 HTML 格式" />
        </el-form-item>
        <el-form-item label="作者"><el-input v-model="articleForm.author" placeholder="请输入作者" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="articleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveArticle" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const categories = ref([
  { id: 1, name: '慢病管理', icon: 'Opportunity' },
  { id: 2, name: '营养饮食', icon: 'Dish' },
  { id: 3, name: '运动康复', icon: 'Football' },
  { id: 4, name: '心理健康', icon: 'Sunny' },
  { id: 5, name: '养老护理', icon: 'UserFilled' },
])

const articles = ref([])
const loading = ref(false)
const saving = ref(false)
const activeCategory = ref('')
const searchKeyword = ref('')
const page = ref(1)
const size = ref(9)
const total = ref(0)

const detailVisible = ref(false)
const articleDialogVisible = ref(false)
const articleFormRef = ref(null)
const currentArticle = ref(null)
const editArticleId = ref(null)
const articleForm = reactive({ categoryId: null, title: '', summary: '', content: '', author: '' })

const articleRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const res = await request.get('/knowledge/categories')
    if (res.data?.length > 0) categories.value = res.data
  } catch { /* use default categories */ }
  fetchArticles()
})

async function fetchArticles() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeCategory.value) params.categoryId = Number(activeCategory.value)
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await request.get('/knowledge/articles', { params })
    articles.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    // 后端不可用时保持已有数据不变
  }
  loading.value = false
}

function onCategorySelect(index) {
  activeCategory.value = index
  page.value = 1
  fetchArticles()
}

async function viewArticle(article) {
  currentArticle.value = article
  detailVisible.value = true
  try {
    const res = await request.get(`/knowledge/articles/${article.id}`)
    currentArticle.value = res.data
  } catch { /* use cached data */ }
}

function showArticleDialog(article) {
  if (article) {
    editArticleId.value = article.id
    Object.assign(articleForm, article)
  } else {
    editArticleId.value = null
    Object.keys(articleForm).forEach(k => articleForm[k] = '')
  }
  articleDialogVisible.value = true
}

async function saveArticle() {
  const valid = await articleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editArticleId.value) {
      await request.put(`/knowledge/articles/${editArticleId.value}`, articleForm)
    } else {
      await request.post('/knowledge/articles', { ...articleForm, viewCount: 0 })
    }
    ElMessage.success('保存成功')
    articleDialogVisible.value = false
    fetchArticles()
  } catch { ElMessage.warning('保存失败，请检查后端服务') }
  saving.value = false
}
</script>

<style scoped>
.knowledge-page { animation: fadeIn 0.4s ease; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.content-row { margin-top: 0; }
.side-card, .content-card { border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; }
.article-card {
  cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 16px; border-radius: 12px; border: 1px solid #EBEEF5;
}
.article-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); border-color: #5B8DEF; }
.article-card h4 { margin-bottom: 10px; color: #303133; font-size: 15px; font-weight: 600; }
.article-summary { color: #606266; font-size: 13px; margin-bottom: 14px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.6; }
.article-meta { display: flex; gap: 16px; color: #909399; font-size: 12px; padding-top: 10px; border-top: 1px solid #F5F7FA; }
.article-meta span { display: flex; align-items: center; gap: 4px; }
.article-content { padding: 16px; line-height: 1.8; }
.article-content :deep(h2) { color: #303133; margin: 16px 0 8px; }
.article-content :deep(h3) { color: #606266; margin: 12px 0 6px; }
.article-content :deep(ul) { padding-left: 20px; }
.article-content :deep(li) { margin-bottom: 4px; }
.article-info { color: #909399; font-size: 13px; text-align: right; margin-top: 16px; padding-top: 16px; border-top: 1px solid #ebeef5; }
</style>
