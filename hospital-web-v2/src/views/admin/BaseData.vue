<template>
  <main class="workspace data-workspace">
    <section class="page-intro"><div><span class="eyebrow">HOSPITAL MASTER DATA</span><h2>医院资源数据台</h2><p>统一维护科室、医生、排班、患者档案与分诊规则，所有变化直接进入实时业务链路。</p></div></section>
    <section class="data-summary"><button :class="{ active: tab === 'dept' }" @click="tab='dept'"><strong>{{ depts.length }}</strong><span>科室</span></button><button :class="{ active: tab === 'doctor' }" @click="tab='doctor'"><strong>{{ doctors.length }}</strong><span>医生</span></button><button :class="{ active: tab === 'sched' }" @click="tab='sched'"><strong>{{ schedules.length }}</strong><span>排班号源</span></button><button :class="{ active: tab === 'patient' }" @click="tab='patient'"><strong>{{ patients.length }}</strong><span>患者档案</span></button><button :class="{ active: tab === 'tag' }" @click="tab='tag'"><strong>{{ tags.length }}</strong><span>症状标签</span></button><button :class="{ active: tab === 'flag' }" @click="tab='flag'"><strong>{{ flags.length }}</strong><span>危急规则</span></button></section>
    <section class="surface data-table-stage">
    <el-tabs v-model="tab" class="data-tabs">
      <!-- 科室 -->
      <el-tab-pane label="科室" name="dept">
        <div style="margin-bottom:10px"><el-button type="primary" size="small" @click="open('dept')">新增科室</el-button></div>
        <el-table :data="depts" size="small" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="科室名" width="140" />
          <el-table-column prop="code" label="编码" width="90" />
          <el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button size="small" link type="primary" @click="open('dept', row)">编辑</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 医生 -->
      <el-tab-pane label="医生" name="doctor">
        <div style="margin-bottom:10px"><el-button type="primary" size="small" @click="open('doctor')">新增医生（自动开通账号）</el-button></div>
        <el-table :data="doctors" size="small" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column label="科室" width="110"><template #default="{ row }">{{ deptName(row.department_id) }}</template></el-table-column>
          <el-table-column prop="title" label="职称" width="110" />
          <el-table-column prop="specialty" label="专长" width="150" />
          <el-table-column prop="good_at" label="擅长" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button size="small" link type="primary" @click="open('doctor', row)">编辑</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 排班 -->
      <el-tab-pane label="排班号源" name="sched">
        <div style="margin-bottom:10px;display:flex;gap:8px;flex-wrap:wrap">
          <el-button type="primary" size="small" @click="open('sched')">新增排班</el-button>
          <el-button size="small" @click="openPostpone()">排班一键顺延</el-button>
        </div>
        <el-table :data="schedules" size="small" border max-height="420">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="医生" width="100"><template #default="{ row }">{{ doctorName(row.doctor_id) }}</template></el-table-column>
          <el-table-column prop="work_date" label="日期" width="110" />
          <el-table-column prop="period" label="时段" width="70" />
          <el-table-column prop="total_num" label="总号源" width="80" />
          <el-table-column prop="remain_num" label="余号" width="70" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="open('sched', row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="delSchedule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 患者档案 -->
      <el-tab-pane label="患者档案" name="patient">
        <div style="margin-bottom:10px"><el-button type="primary" size="small" @click="open('patient')">新增患者</el-button></div>
        <el-table :data="patients" size="small" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="gender" label="性别" width="60" />
          <el-table-column prop="age" label="年龄" width="60" />
          <el-table-column prop="allergic_history" label="过敏史" width="160" />
          <el-table-column prop="past_history" label="既往史" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button size="small" link type="primary" @click="open('patient', row)">编辑</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 症状标签 -->
      <el-tab-pane label="症状标签" name="tag">
        <div style="margin-bottom:10px"><el-button type="primary" size="small" @click="open('tag')">新增症状标签</el-button></div>
        <el-table :data="tags" size="small" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="标签" width="140" />
          <el-table-column label="归属科室" width="120"><template #default="{ row }">{{ deptName(row.department_id) }}</template></el-table-column>
          <el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button size="small" link type="primary" @click="open('tag', row)">编辑</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 危急征象 -->
      <el-tab-pane label="危急征象" name="flag">
        <div style="margin-bottom:10px"><el-button type="primary" size="small" @click="open('flag')">新增危急规则</el-button></div>
        <el-table :data="flags" size="small" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="名称" width="120" />
          <el-table-column prop="keywords" label="触发关键词（逗号分隔）" />
          <el-table-column prop="level" label="级别" width="70" />
          <el-table-column prop="advice" label="处置建议" width="240" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button size="small" link type="primary" @click="open('flag', row)">编辑</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 通用编辑对话框 -->
    <el-dialog v-model="dlg.visible" :title="dlg.title" width="520px">
      <el-form label-width="90px" v-if="dlg.type === 'dept'">
        <el-form-item label="科室名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="lungs / heart / ..." /></el-form-item>
      </el-form>
      <el-form label-width="90px" v-else-if="dlg.type === 'doctor'">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="科室">
          <el-select v-model="form.department_id" style="width:100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="form.title" style="width:100%">
            <el-option label="主任医师" value="主任医师" /><el-option label="副主任医师" value="副主任医师" /><el-option label="主治医师" value="主治医师" />
          </el-select>
        </el-form-item>
        <el-form-item label="专长"><el-input v-model="form.specialty" /></el-form-item>
        <el-form-item label="擅长"><el-input v-model="form.good_at" /></el-form-item>
      </el-form>
      <el-form label-width="90px" v-else-if="dlg.type === 'sched'">
        <el-form-item label="医生">
          <el-select v-model="form.doctor_id" style="width:100%">
            <el-option v-for="d in doctors" :key="d.id" :label="d.name + '（' + deptName(d.department_id) + '）'" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期"><el-input v-model="form.work_date" placeholder="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="时段">
          <el-select v-model="form.period" style="width:100%"><el-option label="上午 AM" value="AM" /><el-option label="下午 PM" value="PM" /></el-select>
        </el-form-item>
        <el-form-item label="号源数"><el-input-number v-model="form.total_num" :min="1" :max="100" /></el-form-item>
      </el-form>
      <el-form label-width="90px" v-else-if="dlg.type === 'patient'">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
        </el-form-item>
        <el-form-item label="年龄"><el-input-number v-model="form.age" :min="0" :max="120" /></el-form-item>
        <el-form-item label="过敏史"><el-input v-model="form.allergic_history" /></el-form-item>
        <el-form-item label="既往史"><el-input v-model="form.past_history" /></el-form-item>
      </el-form>
      <el-form label-width="90px" v-else-if="dlg.type === 'tag'">
        <el-form-item label="标签名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="归属科室">
          <el-select v-model="form.department_id" style="width:100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <el-form label-width="90px" v-else-if="dlg.type === 'flag'">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="form.keywords" placeholder="逗号分隔，如：胸痛,大汗" /></el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" style="width:100%"><el-option :value="1" label="1 级（硬拦截转急诊）" /><el-option :value="2" label="2 级（建议急诊）" /></el-select>
        </el-form-item>
        <el-form-item label="处置建议"><el-input v-model="form.advice" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 一键顺延对话框 -->
    <el-dialog v-model="pp.visible" title="排班一键顺延" width="440px">
      <el-form label-width="90px">
        <el-form-item label="医生">
          <el-select v-model="pp.doctorId" style="width:100%">
            <el-option v-for="d in doctors" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起始日期"><el-input v-model="pp.fromDate" placeholder="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="顺延天数"><el-input-number v-model="pp.days" :min="1" :max="30" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pp.visible = false">取消</el-button>
        <el-button type="primary" @click="postpone">执行顺延</el-button>
      </template>
    </el-dialog>
    </section>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const tab = ref('dept')
const depts = ref([]), doctors = ref([]), schedules = ref([]), patients = ref([]), tags = ref([]), flags = ref([])
const dlg = reactive({ visible: false, type: '', title: '' })
const form = reactive({})
const pp = reactive({ visible: false, doctorId: null, fromDate: '', days: 7 })

const deptName = id => (depts.value.find(d => d.id === id) || {}).name || id
const doctorName = id => (doctors.value.find(d => d.id === id) || {}).name || id

async function loadAll() {
  const [a, b, c, d, e, f] = await Promise.all([
    api.get('/admin/departments'), api.get('/admin/doctors'), api.get('/admin/schedules'),
    api.get('/admin/patients'), api.get('/admin/symptom-tags'), api.get('/admin/red-flags')
  ])
  depts.value = a; doctors.value = b; schedules.value = c; patients.value = d; tags.value = e; flags.value = f
}
onMounted(loadAll)

function open(type, row) {
  dlg.type = type; dlg.visible = true
  dlg.title = (row ? '编辑' : '新增') + { dept: '科室', doctor: '医生', sched: '排班', patient: '患者档案', tag: '症状标签', flag: '危急规则' }[type]
  Object.keys(form).forEach(k => delete form[k])
  const blank = {
    dept: { name: '', code: '', description: '', icon: '' },
    doctor: { name: '', department_id: null, title: '主治医师', specialty: '', good_at: '' },
    sched: { doctor_id: null, work_date: new Date().toISOString().slice(0, 10), period: 'AM', total_num: 20 },
    patient: { name: '', gender: '男', age: 30, allergic_history: '', past_history: '' },
    tag: { name: '', department_id: null, description: '' },
    flag: { name: '', keywords: '', level: 1, advice: '' }
  }[type]
  Object.assign(form, row ? { ...row } : blank)
}
async function save() {
  const url = { dept: '/admin/departments/save', doctor: '/admin/doctors/save', sched: '/admin/schedules/save',
    patient: '/admin/patients/save', tag: '/admin/symptom-tags/save', flag: '/admin/red-flags/save' }[dlg.type]
  await api.post(url, null, form)
  ElMessage.success('已保存'); dlg.visible = false; loadAll()
}
async function delSchedule(row) {
  await api.post('/admin/schedules/delete?1=1', { id: row.id })
  ElMessage.success('已删除'); loadAll()
}
function openPostpone() {
  pp.doctorId = doctors.value[0]?.id || null
  pp.fromDate = new Date().toISOString().slice(0, 10)
  pp.visible = true
}
async function postpone() {
  const r = await api.post('/admin/schedules/postpone',
    { doctorId: pp.doctorId, fromDate: pp.fromDate, days: pp.days })
  ElMessage.success('顺延完成，新增 ' + r.created + ' 个班次')
  pp.visible = false; loadAll()
}
</script>
