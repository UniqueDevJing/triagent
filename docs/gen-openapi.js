const fs = require('fs');

// Response wrapper helper
function listResp(name, itemRef) {
  return {
    type: 'object',
    properties: {
      code: { type: 'integer', example: 200 },
      msg: { type: 'string', example: '操作成功' },
      data: { type: 'array', items: { $ref: '#/components/schemas/' + itemRef } }
    }
  };
}
function singleResp(name, itemRef) {
  return {
    type: 'object',
    properties: {
      code: { type: 'integer', example: 200 },
      msg: { type: 'string', example: '操作成功' },
      data: { $ref: '#/components/schemas/' + itemRef }
    }
  };
}

const schemas = {
  UserInfo: { type: 'object', properties: { id: { type: 'integer' }, name: { type: 'string' }, gender: { type: 'string' }, birthday: { type: 'string' }, phone: { type: 'string' }, address: { type: 'string' }, height: { type: 'number' }, weight: { type: 'number' }, bloodType: { type: 'string' }, allergyHistory: { type: 'string' }, status: { type: 'string' }, memberLevel: { type: 'string' }, age: { type: 'integer' } } },
  Record: { type: 'object', properties: { id: { type: 'integer' }, memberId: { type: 'integer' }, memberName: { type: 'string' }, type: { type: 'string' }, totalScore: { type: 'number' }, riskLevel: { type: 'string' }, conclusion: { type: 'string' }, suggestion: { type: 'string' }, assessDate: { type: 'string' } } },
  PlanItem: { type: 'object', properties: { id: { type: 'integer' }, memberId: { type: 'integer' }, planName: { type: 'string' }, planDate: { type: 'string' }, packageId: { type: 'integer' }, status: { type: 'string' } } },
  Booking: { type: 'object', properties: { id: { type: 'integer' }, memberId: { type: 'integer' }, packageId: { type: 'integer' }, appointmentDate: { type: 'string' }, appointmentTime: { type: 'string' }, status: { type: 'string' }, remark: { type: 'string' } } },
  DiseaseInfo: { type: 'object', properties: { id: { type: 'integer' }, diseaseName: { type: 'string' }, category: { type: 'string' }, symptoms: { type: 'string' }, causes: { type: 'string' }, treatment: { type: 'string' }, prevention: { type: 'string' } } },
  ExerciseItem: { type: 'object', properties: { id: { type: 'integer' }, exerciseName: { type: 'string' }, exerciseType: { type: 'string' }, difficulty: { type: 'string' }, duration: { type: 'integer' }, caloriesBurn: { type: 'number' }, description: { type: 'string' } } },
  RecipeItem: { type: 'object', properties: { id: { type: 'integer' }, recipeName: { type: 'string' }, mealType: { type: 'string' }, suitableFor: { type: 'string' }, totalCalories: { type: 'number' }, cookingTime: { type: 'integer' }, difficulty: { type: 'string' }, ingredients: { type: 'string' }, steps: { type: 'string' }, nutritionInfo: { type: 'string' } } },
  ArticleItem: { type: 'object', properties: { id: { type: 'integer' }, title: { type: 'string' }, content: { type: 'string' }, category: { type: 'string' }, author: { type: 'string' }, viewCount: { type: 'integer' } } },
  ExamItemInfo: { type: 'object', properties: { id: { type: 'integer' }, itemName: { type: 'string' }, itemCode: { type: 'string' }, unit: { type: 'string' }, price: { type: 'number' }, referenceRange: { type: 'string' }, categoryId: { type: 'integer' }, remark: { type: 'string' } } },
  IndicatorInfo: { type: 'object', properties: { id: { type: 'integer' }, indicatorName: { type: 'string' }, indicatorType: { type: 'string' }, unit: { type: 'string' }, minValue: { type: 'number' }, maxValue: { type: 'number' }, riskLevel: { type: 'string' } } },
  EducationItem: { type: 'object', properties: { id: { type: 'integer' }, title: { type: 'string' }, summary: { type: 'string' }, content: { type: 'string' }, contentType: { type: 'string' }, author: { type: 'string' }, targetAudience: { type: 'string' }, viewCount: { type: 'integer' } } }
};

// Add response wrappers as schemas too
schemas.UserListResponse = listResp('UserListResponse', 'UserInfo');
schemas.UserDetailResponse = singleResp('UserDetailResponse', 'UserInfo');
schemas.RecordListResponse = listResp('RecordListResponse', 'Record');
schemas.PlanListResponse = listResp('PlanListResponse', 'PlanItem');
schemas.BookingResponse = singleResp('BookingResponse', 'Booking');
schemas.DiseaseListResponse = listResp('DiseaseListResponse', 'DiseaseInfo');
schemas.DiseaseDetailResponse = singleResp('DiseaseDetailResponse', 'DiseaseInfo');
schemas.ExerciseListResponse = listResp('ExerciseListResponse', 'ExerciseItem');
schemas.RecipeListResponse = listResp('RecipeListResponse', 'RecipeItem');
schemas.ArticleListResponse = listResp('ArticleListResponse', 'ArticleItem');
schemas.ArticleDetailResponse = singleResp('ArticleDetailResponse', 'ArticleItem');
schemas.ExamItemListResponse = listResp('ExamItemListResponse', 'ExamItemInfo');
schemas.IndicatorListResponse = listResp('IndicatorListResponse', 'IndicatorInfo');
schemas.EducationListResponse = listResp('EducationListResponse', 'EducationItem');

const spec = {
  openapi: '3.0.3',
  info: { title: '业务数据查询接口', description: '提供用户信息查询和数据管理功能。', version: '1.0.0' },
  servers: [{ url: 'https://uniquejingclaudecoding.top', description: '服务器' }],
  components: {
    schemas
  },
  paths: {
    '/api/v1/agent/members/search': { get: { tags: ['用户查询'], summary: '搜索用户', operationId: 'searchMembers', parameters: [{ name: 'keyword', 'in': 'query', required: true, schema: { type: 'string' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/UserListResponse' } } } } } } },
    '/api/v1/agent/members/{id}': { get: { tags: ['用户查询'], summary: '用户详情', operationId: 'getMember', parameters: [{ name: 'id', 'in': 'path', required: true, schema: { type: 'integer' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/UserDetailResponse' } } } } } } },
    '/api/v1/agent/members/{id}/assessments': { get: { tags: ['记录查询'], summary: '评估记录', operationId: 'getAssessments', parameters: [{ name: 'id', 'in': 'path', required: true, schema: { type: 'integer' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/RecordListResponse' } } } } } } },
    '/api/v1/agent/exam-plans/available': { get: { tags: ['预约管理'], summary: '可用计划', operationId: 'getAvailablePlans', parameters: [{ name: 'memberId', 'in': 'query', required: false, schema: { type: 'integer' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/PlanListResponse' } } } } } } },
    '/api/v1/agent/appointments': { post: { tags: ['预约管理'], summary: '创建预约', operationId: 'createAppointment', requestBody: { required: true, content: { 'application/json': { schema: { type: 'object', required: ['memberId'], properties: { memberId: { type: 'integer' }, packageId: { type: 'integer' }, appointmentDate: { type: 'string' }, appointmentTime: { type: 'string' }, remark: { type: 'string' } } } } } }, responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/BookingResponse' } } } } } } },
    '/api/v1/agent/diseases/search': { get: { tags: ['知识库'], summary: '搜索疾病', operationId: 'searchDiseases', parameters: [{ name: 'keyword', 'in': 'query', required: true, schema: { type: 'string' } }, { name: 'category', 'in': 'query', required: false, schema: { type: 'string' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/DiseaseListResponse' } } } } } } },
    '/api/v1/agent/diseases/{id}': { get: { tags: ['知识库'], summary: '疾病详情', operationId: 'getDisease', parameters: [{ name: 'id', 'in': 'path', required: true, schema: { type: 'integer' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/DiseaseDetailResponse' } } } } } } },
    '/api/v1/agent/exercises/search': { get: { tags: ['知识库'], summary: '搜索运动', operationId: 'searchExercises', parameters: [{ name: 'keyword', 'in': 'query', required: true, schema: { type: 'string' } }, { name: 'exerciseType', 'in': 'query', required: false, schema: { type: 'string' } }, { name: 'difficulty', 'in': 'query', required: false, schema: { type: 'string' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/ExerciseListResponse' } } } } } } },
    '/api/v1/agent/recipes/search': { get: { tags: ['知识库'], summary: '搜索食谱', operationId: 'searchRecipes', parameters: [{ name: 'keyword', 'in': 'query', required: true, schema: { type: 'string' } }, { name: 'mealType', 'in': 'query', required: false, schema: { type: 'string' } }, { name: 'difficulty', 'in': 'query', required: false, schema: { type: 'string' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/RecipeListResponse' } } } } } } },
    '/api/v1/agent/articles/search': { get: { tags: ['知识库'], summary: '搜索文章', operationId: 'searchArticles', parameters: [{ name: 'keyword', 'in': 'query', required: true, schema: { type: 'string' } }, { name: 'category', 'in': 'query', required: false, schema: { type: 'string' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/ArticleListResponse' } } } } } } },
    '/api/v1/agent/articles/{id}': { get: { tags: ['知识库'], summary: '文章详情', operationId: 'getArticle', parameters: [{ name: 'id', 'in': 'path', required: true, schema: { type: 'integer' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/ArticleDetailResponse' } } } } } } },
    '/api/v1/agent/exam-items/search': { get: { tags: ['知识库'], summary: '搜索检测项目', operationId: 'searchExamItems', parameters: [{ name: 'keyword', 'in': 'query', required: true, schema: { type: 'string' } }, { name: 'categoryId', 'in': 'query', required: false, schema: { type: 'integer' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/ExamItemListResponse' } } } } } } },
    '/api/v1/agent/indicators': { get: { tags: ['知识库'], summary: '评估指标', operationId: 'getIndicators', parameters: [{ name: 'indicatorType', 'in': 'query', required: false, schema: { type: 'string' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/IndicatorListResponse' } } } } } } },
    '/api/v1/agent/education/search': { get: { tags: ['知识库'], summary: '搜索宣教', operationId: 'searchEducation', parameters: [{ name: 'keyword', 'in': 'query', required: true, schema: { type: 'string' } }, { name: 'contentType', 'in': 'query', required: false, schema: { type: 'string' } }, { name: 'targetAudience', 'in': 'query', required: false, schema: { type: 'string' } }], responses: { 200: { description: '成功', content: { 'application/json': { schema: { $ref: '#/components/schemas/EducationListResponse' } } } } } } }
  }
};

const json = JSON.stringify(spec, null, 2);
JSON.parse(json);
console.log('Valid. Paths:', Object.keys(spec.paths).length, 'Schemas:', Object.keys(spec.components.schemas).length);
fs.writeFileSync('docs/agent-api-openapi.json', json);
console.log('Written.');
