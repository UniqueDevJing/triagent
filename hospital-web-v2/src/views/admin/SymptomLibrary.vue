<template>
  <div>
    <div class="card">
      <div class="section-title">症状标签库 · SYMPTOM LIBRARY</div>
      <p class="dns" style="margin:0 0 12px;line-height:1.7;">
        维护症状标签、症状→科室映射与危急征象三层结构：标签是预问诊追问的锚点；映射表是分诊 Agent
        <code>department_query</code> 容错回退的真实载体（科室名查不到时按关键词查此表）；危急征象命中即硬拦截转急诊。
        标签名若出现在危急规则的命中关键词中，行内会亮出「危急」徽标。
      </p>
      <div class="sym-grid">
        <!-- ① 症状标签 -->
        <div class="sym-card">
          <div class="sym-head">
            <b>① 症状标签（{{ tags.length }}）</b>
            <el-button size="small" type="primary" plain @click="openTag(null)">新增标签</el-button>
          </div>
          <table>
            <thead><tr><th>标签</th><th>默认科室</th><th>危急</th><th></th></tr></thead>
            <tbody>
              <tr v-for="t in tags" :key="t.id">
                <td><b>{{ t.name }}</b><div class="dns">{{ t.description }}</div></td>
                <td>{{ deptName(t.department_id) }}</td>
                <td>
                  <span v-if="isCritical(t.name)" class="chip red">危急</span>
                  <span v-else class="dns">—</span>
                </td>
                <td class="ops">
                  <el-button size="small" text type="primary" @click="openTag(t)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="del('/symptom-tags/delete', t.id, '标签')">删</el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- ② 症状→科室映射 -->
        <div class="sym-card">
          <div class="sym-head">
            <b>② 症状 → 科室映射（{{ maps.length }}）</b>
            <el-button size="small" type="primary" plain @click="openMap(null)">新增映射</el-button>
          </div>
          <table>
            <thead><tr><th>症状关键词</th><th>科室</th><th>权重</th><th></th></tr></thead>
            <tbody>
              <tr v-for="m in maps" :key="m.id">
                <td><b>{{ m.symptom_keyword }}</b></td>
                <td>{{ m.department_name || deptName(m.department_id) }}</td>
                <td><span class="readout">{{ m.weight }}</span></td>
                <td class="ops">
                  <el-button size="small" text type="primary" @click="openMap(m)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="del('/symptom-maps/delete', m.id, '映射')">删</el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- ③ 危急征象规则 -->
        <div class="sym-card">
          <div class="sym-head">
            <b>③ 危急征象规则（{{ flags.length }}）</b>
            <el-button size="small" type="danger" plain @click="openFlag(null)">新增规则</el-button>
          </div>
          <table>
            <thead><tr><th>征象 / 命中关键词</th><th>等级</th><th></th></tr></thead>
            <tbody>
              <tr v-for="f in flags" :key="f.id">
                <td><b>{{ f.name }}</b><div class="dns">{{ f.keywords }}</div><div class="dns">{{ f.advice }}</div></td>
                <td><span class="chip" :class="levelClass(f.level)">{{ levelText(f.level) }}</span></td>
                <td class="ops">
                  <el-button size="small" text type="primary" @click="openFlag(f)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="del('/red-flags/delete', f.id, '规则')">删</el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 标签对话框 -->
    <el-dialog v-model="tagDlg" :title="tagForm.id ? '编辑症状标签' : '新增症状标签'" width="440px">
      <el-form label-width="84px">
        <el-form-item label="标签名"><el-input v-model="tagForm.name" placeholder="如：胸痛"></el-input></el-form-item>
        <el-form-item label="默认科室">
          <el-select v-model="tagForm.department_id" placeholder="选择科室" style="width:100%" clearable>
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="tagForm.description" type="textarea" :rows="2"></el-input></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDlg = false">取消</el-button>
        <el-button type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>

    <!-- 映射对话框 -->
    <el-dialog v-model="mapDlg" :title="mapForm.id ? '编辑映射' : '新增症状→科室映射'" width="440px">
      <el-form label-width="84px">
        <el-form-item label="症状关键词"><el-input v-model="mapForm.symptom_keyword" placeholder="如：咳嗽（分诊回退检索用）"></el-input></el-form-item>
        <el-form-item label="目标科室">
          <el-select v-model="mapForm.department_id" placeholder="选择科室" style="width:100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="权重"><el-input-number v-model="mapForm.weight" :min="1" :max="100"></el-input-number></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mapDlg = false">取消</el-button>
        <el-button type="primary" @click="saveMap">保存</el-button>
      </template>
    </el-dialog>

    <!-- 危急规则对话框 -->
    <el-dialog v-model="flagDlg" :title="flagForm.id ? '编辑危急规则' : '新增危急规则'" width="500px">
      <el-form label-width="84px">
        <el-form-item label="征象名"><el-input v-model="flagForm.name" placeholder="如：急性心肌梗死"></el-input></el-form-item>
        <el-form-item label="命中关键词"><el-input v-model="flagForm.keywords" placeholder="逗号分隔，如：胸痛大汗,压榨样疼痛"></el-input></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="flagForm.level" style="width:120px">
            <el-option :value="1" label="1 紧急"></el-option>
            <el-option :value="2" label="2 高危"></el-option>
            <el-option :value="3" label="3 注意"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="处置建议"><el-input v-model="flagForm.advice" type="textarea" :rows="2" placeholder="如：立即转急诊绿色通道"></el-input></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="flagDlg = false">取消</el-button>
        <el-button type="primary" @click="saveFlag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const depts = ref([])
const tags = ref([])
const maps = ref([])
const flags = ref([])

const tagDlg = ref(false)
const mapDlg = ref(false)
const flagDlg = ref(false)
const tagForm = reactive({ id: null, name: '', department_id: null, description: '' })
const mapForm = reactive({ id: null, symptom_keyword: '', department_id: null, weight: 1 })
const flagForm = reactive({ id: null, name: '', keywords: '', level: 1, advice: '' })

function deptName(id) { const d = depts.value.find(x => x.id === id); return d ? d.name : '未指定' }
function levelText(l) { return { 1: '紧急', 2: '高危', 3: '注意' }[l] || l }
function levelClass(l) { return { 1: 'red', 2: 'warn', 3: '' }[l] || '' }
function isCritical(name) {
  return flags.value.some(f => String(f.keywords).split(/[,，]/).map(s => s.trim())
    .filter(Boolean).some(k => name.includes(k) || k.includes(name)))
}

async function load() {
  const [d, t, m, f] = await Promise.all([
    api.get('/api/admin/departments'),
    api.get('/api/admin/symptom-tags'),
    api.get('/api/admin/symptom-maps'),
    api.get('/api/admin/red-flags')
  ])
  depts.value = d; tags.value = t; maps.value = m; flags.value = f
}

function openTag(row) {
  Object.assign(tagForm, row || { id: null, name: '', department_id: null, description: '' })
  tagDlg.value = true
}
async function saveTag() {
  if (!tagForm.name) { ElMessage.warning('请填标签名'); return }
  await api.post('/api/admin/symptom-tags/save', null, { ...tagForm })
  ElMessage.success('已保存'); tagDlg.value = false; load()
}

function openMap(row) {
  Object.assign(mapForm, row || { id: null, symptom_keyword: '', department_id: null, weight: 1 })
  mapDlg.value = true
}
async function saveMap() {
  if (!mapForm.symptom_keyword || !mapForm.department_id) { ElMessage.warning('请填关键词与科室'); return }
  await api.post('/api/admin/symptom-maps/save', null, { ...mapForm })
  ElMessage.success('已保存'); mapDlg.value = false; load()
}

function openFlag(row) {
  Object.assign(flagForm, row || { id: null, name: '', keywords: '', level: 1, advice: '' })
  flagDlg.value = true
}
async function saveFlag() {
  if (!flagForm.name || !flagForm.keywords) { ElMessage.warning('请填征象名与命中关键词'); return }
  await api.post('/api/admin/red-flags/save', null, { ...flagForm })
  ElMessage.success('已保存'); flagDlg.value = false; load()
}

async function del(url, id, label) {
  await ElMessageBox.confirm(`确认删除该${label}？删除立即生效并影响分诊行为。`, '删除确认', { type: 'warning' })
  await api.post(url, { id })
  ElMessage.success('已删除'); load()
}

onMounted(load)
</script>

<style scoped>
.sym-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; }
.sym-card { background: #f9fafc; border: 1px solid #e8edf3; border-radius: 8px; padding: 12px; }
.sym-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.sym-head b { font-size: 13px; color: #1f6feb; }
.sym-card table { width: 100%; font-size: 12px; }
.sym-card th { text-align: left; color: #8a94a6; font-weight: 500; padding: 4px 6px; border-bottom: 1px solid #e8edf3; }
.sym-card td { padding: 7px 6px; border-bottom: 1px dashed #eef2f7; vertical-align: top; }
.ops { white-space: nowrap; text-align: right; }
.ops .el-button { padding: 0 4px; }
@media (max-width: 1100px) { .sym-grid { grid-template-columns: 1fr; } }
</style>
