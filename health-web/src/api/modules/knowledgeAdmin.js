import request from '@/api/request'

export const getEducationContents = (params) => request.get('/v1/knowledge/education', { params })
export const getEducationContent = (id) => request.get(`/v1/knowledge/education/${id}`)
export const createEducationContent = (data) => request.post('/v1/knowledge/education', data)
export const updateEducationContent = (id, data) => request.put(`/v1/knowledge/education/${id}`, data)
export const deleteEducationContent = (id) => request.delete(`/v1/knowledge/education/${id}`)

export const getEducationWords = (params) => request.get('/v1/knowledge/education/word', { params })
export const getEducationWord = (id) => request.get(`/v1/knowledge/education/word/${id}`)
export const createEducationWord = (data) => request.post('/v1/knowledge/education/word', data)
export const updateEducationWord = (id, data) => request.put(`/v1/knowledge/education/word/${id}`, data)
export const deleteEducationWord = (id) => request.delete(`/v1/knowledge/education/word/${id}`)

export const getExercises = (params) => request.get('/v1/knowledge/exercise', { params })
export const getExercise = (id) => request.get(`/v1/knowledge/exercise/${id}`)
export const createExercise = (data) => request.post('/v1/knowledge/exercise', data)
export const updateExercise = (id, data) => request.put(`/v1/knowledge/exercise/${id}`, data)
export const deleteExercise = (id) => request.delete(`/v1/knowledge/exercise/${id}`)

export const getDiseases = (params) => request.get('/v1/knowledge/disease', { params })
export const getDisease = (id) => request.get(`/v1/knowledge/disease/${id}`)
export const createDisease = (data) => request.post('/v1/knowledge/disease', data)
export const updateDisease = (id, data) => request.put(`/v1/knowledge/disease/${id}`, data)
export const deleteDisease = (id) => request.delete(`/v1/knowledge/disease/${id}`)

export const getRecipes = (params) => request.get('/v1/knowledge/recipe', { params })
export const getRecipe = (id) => request.get(`/v1/knowledge/recipe/${id}`)
export const createRecipe = (data) => request.post('/v1/knowledge/recipe', data)
export const updateRecipe = (id, data) => request.put(`/v1/knowledge/recipe/${id}`, data)
export const deleteRecipe = (id) => request.delete(`/v1/knowledge/recipe/${id}`)
