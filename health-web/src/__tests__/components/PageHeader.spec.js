import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PageHeader from '@/components/PageHeader.vue'

describe('PageHeader', () => {
  it('should render title', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '用户管理' },
    })
    expect(wrapper.text()).toContain('用户管理')
  })

  it('should render subtitle when provided', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '工作台', subtitle: '查看系统数据概览' },
    })
    expect(wrapper.text()).toContain('工作台')
    expect(wrapper.text()).toContain('查看系统数据概览')
  })

  it('should not render subtitle element when omitted', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '设置' },
    })
    const subtitleEl = wrapper.find('.page-subtitle')
    expect(subtitleEl.exists()).toBe(false)
  })

  it('should render empty string when subtitle is empty string', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '标题', subtitle: '' },
    })
    expect(wrapper.find('.page-subtitle').exists()).toBe(false)
  })
})
