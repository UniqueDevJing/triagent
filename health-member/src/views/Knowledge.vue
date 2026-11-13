<template>
  <div class="page-container">
    <h2 class="page-title">健康知识</h2>
    <van-tabs v-model:active="active">
      <van-tab title="健康文章">
        <van-pull-refresh v-model="refreshing" @refresh="fetchArticles">
          <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="fetchArticles">
            <van-cell-group inset v-for="a in articles" :key="a.id" style="margin-bottom:12px">
              <van-cell :title="a.title" :label="a.summary">
                <template #value><van-tag size="small">{{ a.type }}</van-tag></template>
              </van-cell>
            </van-cell-group>
          </van-list>
        </van-pull-refresh>
      </van-tab>
      <van-tab title="健康食谱">
        <van-pull-refresh v-model="refreshing2" @refresh="fetchRecipes">
          <van-list v-model:loading="loading2" :finished="finished2" finished-text="没有更多了" @load="fetchRecipes">
            <van-card v-for="r in recipes" :key="r.id" :title="r.name" :desc="r.calories + 'kcal · ' + r.cookingTime + '分钟'" :thumb="r.imageUrl || 'https://img.yzcdn.cn/vant/cat.jpeg'">
              <template #tags><van-tag v-for="t in formatTags(r.suitableFor)" :key="t" type="primary" size="small">{{ t }}</van-tag></template>
            </van-card>
          </van-list>
        </van-pull-refresh>
      </van-tab>
      <van-tab title="运动指导">
        <van-pull-refresh v-model="refreshing3" @refresh="fetchExercises">
          <van-list v-model:loading="loading3" :finished="finished3" finished-text="没有更多了" @load="fetchExercises">
            <van-cell-group inset v-for="e in exercises" :key="e.id" style="margin-bottom:12px">
              <van-cell :title="e.name" :label="e.description">
                <template #value><van-tag :type="e.intensity==='HIGH'?'danger':e.intensity==='MEDIUM'?'warning':'success'" size="small">{{ e.intensity }}</van-tag></template>
              </van-cell>
            </van-cell-group>
          </van-list>
        </van-pull-refresh>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getKnowledgeArticles, getRecipes, getExercises } from '@/api/modules/member'

const active = ref(0)
const articles = ref([]), recipes = ref([]), exercises = ref([])
const loading = ref(false), finished = ref(false), refreshing = ref(false)
const loading2 = ref(false), finished2 = ref(false), refreshing2 = ref(false)
const loading3 = ref(false), finished3 = ref(false), refreshing3 = ref(false)

async function fetchArticles() {
  if (refreshing.value) { articles.value = []; refreshing.value = false }
  loading.value = true
  try {
    const res = await getKnowledgeArticles({ page: articles.value.length / 20 + 1, size: 20 })
    articles.value = [...articles.value, ...res.data.records]
    finished.value = res.data.records.length < 20
  } catch {} finally { loading.value = false }
}

async function fetchRecipes() {
  if (refreshing2.value) { recipes.value = []; refreshing2.value = false }
  loading2.value = true
  try {
    const res = await getRecipes({ page: recipes.value.length / 20 + 1, size: 20 })
    recipes.value = [...recipes.value, ...res.data.records]
    finished2.value = res.data.records.length < 20
  } catch {} finally { loading2.value = false }
}

async function fetchExercises() {
  if (refreshing3.value) { exercises.value = []; refreshing3.value = false }
  loading3.value = true
  try {
    const res = await getExercises({ page: exercises.value.length / 20 + 1, size: 20 })
    exercises.value = [...exercises.value, ...res.data.records]
    finished3.value = res.data.records.length < 20
  } catch {} finally { loading3.value = false }
}

function formatTags(t) { return t ? t.split(',').slice(0, 2) : [] }
</script>
