import sys
sys.stdout.reconfigure(encoding='utf-8')

BODY_PARTS = ["头痛", "头晕", "胸", "腹", "胃", "背", "腰", "颈", "肩", "膝", "关节",
              "腿", "手", "脚", "眼", "耳", "鼻", "咽喉", "喉咙", "口腔", "皮肤", "心前区"]
INFERRED = ["咳嗽", "咳痰", "鼻塞", "流涕", "咽痛", "腹泻", "便秘", "皮疹", "瘙痒", "尿痛", "尿频"]
ACCOMPANY = ["发热", "发烧", "恶心", "呕吐", "乏力", "疲劳", "盗汗", "消瘦", "食欲",
             "腹泻", "便秘", "麻木", "疼痛加剧", "心悸", "心慌", "咳嗽", "咳痰", "皮疹"]
DUR = ["天", "周", "月", "年", "小时", "长期", "反复", "最近"]


def decide(text, st):
    body = any(w in text for w in BODY_PARTS) or any(w in text for w in INFERRED)
    duration = any(w in text for w in DUR)
    accompany = any(w in text for w in ACCOMPANY)
    st['body'] = st['body'] or body
    st['dur'] = st['dur'] or duration
    if not st['acc'] and (accompany or "无" in text or "没有" in text):
        st['acc'] = True
    if not st['body']:
        return 'CLARIFY:bodyPart'
    if not st['dur']:
        return 'CLARIFY:duration'
    if not st['acc']:
        return 'CLARIFY:accompany'
    return 'PROCEED'


print('=== 场景：症状描述含「无」，是否被误判为「已回答无伴随症状」===')
print()
tests = [
    ('无力', '乏力类症状，用户在描述症状本身'),
    ('无法呼吸', '危急征象（红旗含「呼吸困难」，但用户说的是「无法呼吸」）'),
    ('无意识', '昏迷征象'),
]
for phrase, note in tests:
    st = {'body': False, 'dur': False, 'acc': False}
    r1 = decide('我头痛，' + phrase, st)
    r2 = decide(phrase, st)
    verdict = '误判！用户在说症状，不是回答「无」' if st['acc'] else '正确'
    print('  输入「%s」（%s）' % (phrase, note))
    print('     第1轮 -> %-18s 第2轮 -> %s' % (r1, r2))
    print('     accompanyKnown=%-6s ← %s' % (st['acc'], verdict))
    print()

print('=== 对比：真正的否定回答 ===')
for phrase in ['没有其他症状', '无明显伴随症状']:
    st = {'body': False, 'dur': False, 'acc': False}
    decide('头痛，3 天了', st)
    r = decide(phrase, st)
    print('  「%s」-> %-18s accompanyKnown=%s' % (phrase, r, st['acc']))

print()
print('=== 危害 ===')
print('用户说「无力」时，系统认为三字段已齐 → 直接 PROCEED 交给模型分诊，')
print('**跳过了「有无伴随症状」的追问**。而「无力」是需要评估的症状，不是「没有症状」。')
