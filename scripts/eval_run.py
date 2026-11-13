# -*- coding: utf-8 -*-
"""G1-G10 全量评估 runner：逐例采集原始测量数据，输出 markdown 行。"""
import json, re, sys, time, urllib.request

BASE = "http://127.0.0.1:8080/api/v1/assistant"
TOKEN = sys.argv[1]
H = {"Content-Type": "application/json; charset=utf-8", "Authorization": TOKEN}

CASES = [
    ("G1",  "突然胸痛伴大汗、恶心，持续不缓解",            "EMERGENCY", "急诊科/心内科"),
    ("G2",  "一侧肢体无力、口角歪斜、说话含糊",            "EMERGENCY", "急诊科/神经内科"),
    ("G3",  "突发剧烈头痛伴喷射性呕吐",                    "EMERGENCY", "急诊科/神经内科"),
    ("G4",  "呼吸困难、憋醒，下肢水肿",                    "URGENT",    "心内科/呼吸内科"),
    ("G5",  "发烧咳嗽 5 天，痰黄，胸片无异常",             "URGENT",    "呼吸内科"),
    ("G6",  "上腹痛烧心反复 2 个月，餐后加重",             "ROUTINE",   "消化内科"),
    ("G7",  "膝盖下楼痛 3 周，无外伤",                     "ROUTINE",   "骨科/运动医学科"),
    ("G8",  "手抖、心慌、消瘦、易怒 1 个月",               "ROUTINE",   "内分泌科"),
    ("G9",  "皮肤红斑瘙痒 1 周，无发热",                   "ROUTINE",   "皮肤科"),
    ("G10", "儿童发热 39℃ 伴抽搐",                        "EMERGENCY", "急诊科/儿科"),
]

def chat(sid, msg, timeout=150):
    body = json.dumps({"sessionId": sid, "message": msg}, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(BASE + "/chat", data=body, method="POST", headers=H)
    t0 = time.time()
    with urllib.request.urlopen(req, timeout=timeout) as r:
        raw = r.read().decode("utf-8")
    wall = round((time.time() - t0) * 1000)
    m = {"events": {}, "tokens": 0, "tools": 0, "results": 0, "clarify": None,
         "agent": None, "triage": None, "answer": "", "sources": 0, "plans": [],
         "tool_results": [], "raw": raw}
    ev = None
    for line in raw.splitlines():
        line = line.strip()
        if line.startswith("event:"):
            ev = line[6:].strip(); m["events"][ev] = m["events"].get(ev, 0) + 1
        elif line.startswith("data:"):
            try:
                d = json.loads(line[5:].strip())
            except Exception:
                continue
            if ev == "token": m["tokens"] += 1; m["answer"] += d if isinstance(d, str) else ""
            elif ev == "tool_call": m["tools"] += 1
            elif ev == "tool_result":
                m["results"] += 1
                m["tool_results"].append({"name": d.get("name"), "result": d.get("result")})
            elif ev == "clarify": m["clarify"] = d
            elif ev == "plan": m["plans"].append(d.get("decision"))
            elif ev == "done":
                m["agent"] = d.get("agent"); m["triage"] = d.get("triage")
                m["sources"] = len(d.get("sources") or [])
                if d.get("answer"): m["answer"] = d.get("answer")
    m["wall"] = wall
    return m

def score(cexp, cact, dexp, dact_list):
    order = {"ROUTINE": 0, "URGENT": 1, "EMERGENCY": 2}
    if cact == "-":
        return 0.0, 0.0
    u = 1.0 if cexp == cact else (0.5 if abs(order[cexp] - order[cact]) == 1 else 0.0)
    hits = [d for d in dexp.split("/") if d in (dact_list or [])]
    return u, (1.0 if hits else 0.0)

def chat_flow(sid, msg):
    """带澄清续答的完整轮次：状态机追问则补答『有点乏力，没有发热』，累计墙钟。"""
    r = chat(sid, msg)
    total_wall = r["wall"]; turns = 1
    while r["clarify"] and turns < 4:
        r2 = chat(sid, "有点乏力，没有发热")
        total_wall += r2["wall"]; turns += 1
        r2["tokens"] += r["tokens"]; r2["tools"] += r["tools"]; r2["results"] += r["results"]
        r2["sources"] = max(r2["sources"], r["sources"])
        r = r2
    r["wall"] = total_wall; r["turns"] = turns
    return r

rows = []
us, ds = 0.0, 0.0
det_ms, llm_ms = [], []
for gid, msg, cexp, dexp in CASES:
    r = chat_flow("ev2-" + gid, msg)
    t = r["triage"] or {}
    cact = t.get("urgency") or "-"
    dact = t.get("departments") or []
    u, d = score(cexp, cact, dexp, dact)
    us += u; ds += d
    turn = "EMERGENCY(确定性)" if r["events"].get("done") == 1 and r["tokens"] == 0 and cact == "EMERGENCY" and (r["tools"] == 0) and "clarify" not in r["events"] else "LLM"
    row = dict(gid=gid, cexp=cexp, cact=cact, dexp=dexp,
               dact="/".join(dact) if dact else "-", u=u, d=d,
               conf=t.get("confidence"), wall=r["wall"],
               tokens=r["tokens"], tools=r["tools"], results=r["results"],
               sources=r["sources"], agent=r["agent"], events=r["events"],
               turns=r.get("turns", 1))
    rows.append(row)
    if turn == "LLM": llm_ms.append(r["wall"])
    else: det_ms.append(r["wall"])
    print(f'| {gid} | {cexp} | {cact} | {row["dact"]} | {u} | {d} | {t.get("confidence")} | {r["wall"]} | {r["tokens"]} | {r["tools"]}/{r["results"]} | {r["sources"]} | {r.get("turns",1)} |', flush=True)

print(f"TOTAL us={us} ds={ds} total={(us*0.6+ds*0.4):.2f}")
if det_ms: print(f"DET avg={sum(det_ms)/len(det_ms):.0f}ms max={max(det_ms)}ms n={len(det_ms)}")
if llm_ms: print(f"LLM avg={sum(llm_ms)/len(llm_ms):.0f}ms min={min(llm_ms)} max={max(llm_ms)} n={len(llm_ms)}")

# 澄清流程测量（2 轮）
c1 = chat("ev2-clar", "我最近不太舒服")
c2 = chat("ev2-clar", "左边头痛，持续3天，有点恶心")
t2 = c2["triage"] or {}
print(f"CLARIFY r1 events={c1['events']} q_len={len((c1['clarify'] or {}).get('question',''))} missing={((c1['clarify'] or {}).get('missing') or [])}")
print(f"CLARIFY r2 agent={c2['agent']} urgency={t2.get('urgency')} wall={c2['wall']} tokens={c2['tokens']} sources={c2['sources']}")

# 稳定性：G6 连跑 3 次（不同会话）
for i in (1, 2, 3):
    r = chat(f"ev2-stab-{i}", "上腹痛烧心反复 2 个月，餐后加重")
    t = r["triage"] or {}
    print(f"STAB{i} urgency={t.get('urgency')} dept_hit={'消化内科' in (t.get('departments') or [])} wall={r['wall']} conf={t.get('confidence')}")

# 幂等：两个不同会话、完全相同参数 → createPreOrder 应返回同一预订单号
a = chat("ev2-idem-1", "请给会员孙明伟预约呼吸内科2026年9月20日，主诉咳嗽两周，直接下单")
b = chat("ev2-idem-2", "请给会员孙明伟预约呼吸内科2026年9月20日，主诉咳嗽两周，直接下单")
def po(m):
    for t in m["tool_results"]:
        if t["name"] == "createPreOrder":
            return t["result"]
    return "未调用"
ra, rb = po(a), po(b)
same = (ra.split("#")[1].split(" ")[0] if "#" in ra else "?") == (rb.split("#")[1].split(" ")[0] if "#" in rb else "?")
print(f"IDEM a={ra}")
print(f"IDEM b={rb}")
print(f"IDEM 同一预订单={same}")

# metrics 快照
try:
    req = urllib.request.Request(BASE + "/metrics", headers=H)
    with urllib.request.urlopen(req, timeout=10) as r:
        mm = json.loads(r.read().decode("utf-8"))["data"]
    print(f"METRICS total={mm['total']} avgMs={mm['avgMs']} toolCalls={mm['toolCalls']} blocked={mm['blocked']} byAgent={mm['byAgent']} byUrgency={mm['byUrgency']} byTurnType={mm['byTurnType']}")
except Exception as e:
    print("METRICS err", e)

# 401 鉴权
try:
    noh = urllib.request.Request(BASE + "/chat", data=b"{}", method="POST",
                                 headers={"Content-Type": "application/json"})
    urllib.request.urlopen(noh, timeout=10)
    print("AUTH unexpected 200")
except urllib.error.HTTPError as e:
    print("AUTH", e.code)
except Exception as e:
    print("AUTH err", e)
