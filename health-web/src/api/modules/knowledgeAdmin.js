import request from '@/api/request'

// 宣教内容
export const getEducationContents = (params) => request.get('/admin/education-contents', { params })
export const getEducationContent = (id) => request.get(`/admin/education-contents/${id}`)
export const createEducationContent = (data) => request.post('/admin/education-contents', data)
export const updateEducationContent = (id, data) => request.put(`/admin/education-contents/${id}`, data)
export const deleteEducationContent = (id) => request.delete(`/admin/education-contents/${id}`)

// 宣教词
export const getEducationWords = (params) => request.get('/admin/education-words', { params })
export const getEducationWord = (id) => request.get(`/admin/education-words/${id}`)
export const createEducationWord = (data) => request.post('/admin/education-words', data)
export const updateEducationWord = (id, data) => request.put(`/admin/education-words/${id}`, data)
export const deleteEducationWord = (id) => request.delete(`/admin/education-words/${id}`)

// 运动项目库
export const getExercises = (params) => request.get('/admin/exercise-library', { params })
export const getExercise = (id) => request.get(`/admin/exercise-library/${id}`)
export const createExercise = (data) => request.post('/admin/exercise-library', data)
export const updateExercise = (id, data) => request.put(`/admin/exercise-library/${id}`, data)
export const deleteExercise = (id) => request.delete(`/admin/exercise-library/${id}`)

// 疾病库
export const getDiseases = (params) => request.get('/admin/disease-library', { params })
export const getDisease = (id) => request.get(`/admin/disease-library/${id}`)
export const createDisease = (data) => request.post('/admin/disease-library', data)
export const updateDisease = (id, data) => request.put(`/admin/disease-library/${id}`, data)
export const deleteDisease = (id) => request.delete(`/admin/disease-library/${id}`)

// 健康食谱库
export const getRecipes = (params) => request.get('/admin/health-recipes', { params })
export const getRecipe = (id) => request.get(`/admin/health-recipes/${id}`)
export const createRecipe = (data) => request.post('/admin/health-recipes', data)
export const updateRecipe = (id, data) => request.put(`/admin/health-recipes/${id}`, data)
export const deleteRecipe = (id) => request.delete(`/admin/health-recipes/${id}`)
