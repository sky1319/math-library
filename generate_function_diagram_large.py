#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""极限大字版 - 每个字都清晰可见，填满画布"""

import matplotlib
import os
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
import gc

plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# ============ 画布 ============
FW, FH = 100, 72   # 英寸
DPI = 150           # 用稍低DPI避免内存爆炸
fig, ax = plt.subplots(1, 1, figsize=(FW, FH))
ax.set_xlim(0, FW)
ax.set_ylim(0, FH)
ax.axis('off')
ax.set_facecolor('#f5f7fa')

COLOR_ROOT = '#1a237e'
COLOR_LEVEL1 = ['#1565c0', '#00838f', '#2e7d32', '#e65100', '#6a1b9a', '#c62828', '#37474f', '#4e342e']

# ============ 极限字体 ============
FONT_TITLE      = 260
FONT_ROOT       = 140
FONT_MODULE     = 100
FONT_SUB        = 75
FONT_TECH_HEAD  = 90
FONT_TECH_BODY  = 66
FONT_BOTTOM     = 85

# ============ 布局 ============
ML, MR = 0.6, 0.6
GX = 0.25
mw = (FW - ML - MR - 3 * GX) / 4
def mx(col): return ML + col * (mw + GX)

# 标题
ax.text(FW/2, FH - 2.0, '智能图书推荐与管理系统 - 功能模块图',
        ha='center', va='top', fontsize=FONT_TITLE, fontweight='bold', color='#1a237e')

# 根节点
rh = 7.0
ry = FH - 11.0
rw = FW * 0.7
rx = (FW - rw) / 2
root = FancyBboxPatch((rx, ry), rw, rh,
                       boxstyle="round,pad=0.8",
                       facecolor=COLOR_ROOT, edgecolor='#0d1b5e', linewidth=8)
ax.add_patch(root)
ax.text(FW/2, ry + rh/2, '智能图书推荐与管理系统', ha='center', va='center',
        fontsize=FONT_ROOT, fontweight='bold', color='white')

# 两行模块
mh = 17.0
row1_y = ry - 2.0 - mh
row2_y = row1_y - 1.5 - mh

modules = [
    {'name': '用户认证模块', 'x': mx(0), 'y': row1_y, 'w': mw, 'h': mh,
     'subs': ['用户登录/登出', 'JWT Token认证', 'BCrypt密码加密', '角色权限控制\n(管理员/读者)']},
    {'name': '图书管理模块', 'x': mx(1), 'y': row1_y, 'w': mw, 'h': mh,
     'subs': ['图书信息增删改查', 'ISBN快速查询\n(HashMap O(1))', '多条件组合检索', '分类筛选与搜索']},
    {'name': '借阅管理模块', 'x': mx(2), 'y': row1_y, 'w': mw, 'h': mh,
     'subs': ['图书借阅处理', '图书归还处理', '借阅记录查询', '逾期自动预警']},
    {'name': '个性化推荐模块', 'x': mx(3), 'y': row1_y, 'w': mw, 'h': mh,
     'subs': ['借阅关联分析\n(链表存储)', '同分类图书推荐', '推荐列表展示\n(≥3本)', '关联度权重计算']},
    {'name': '愿望单模块', 'x': mx(0), 'y': row2_y, 'w': mw, 'h': mh,
     'subs': ['添加图书到愿望单', '查看我的愿望单', '从愿望单移除图书', '愿望单直接借阅']},
    {'name': '智能问答模块', 'x': mx(1), 'y': row2_y, 'w': mw, 'h': mh,
     'subs': ['自然语言输入解析', '关键词提取与分词', '多字段模糊匹配', '智能结果返回']},
    {'name': '数据统计模块', 'x': mx(2), 'y': row2_y, 'w': mw, 'h': mh,
     'subs': ['分类借阅统计\n(树结构聚合)', '热门图书排行', '读者借阅统计', '流通数据分析']},
    {'name': '系统管理模块', 'x': mx(3), 'y': row2_y, 'w': mw, 'h': mh,
     'subs': ['操作日志记录', 'LRU查询缓存\n(LinkedHashMap)', '数据初始化加载', '用户账号管理']},
]

# ============ 绘制模块 ============
for i, mod in enumerate(modules):
    c1 = COLOR_LEVEL1[i % len(COLOR_LEVEL1)]

    box = FancyBboxPatch((mod['x'], mod['y']), mod['w'], mod['h'],
                          boxstyle="round,pad=0.5",
                          facecolor='white', edgecolor=c1, linewidth=7,
                          zorder=2)
    ax.add_patch(box)

    th = 2.8
    tb = FancyBboxPatch((mod['x'] + 0.25, mod['y'] + mod['h'] - th - 0.25),
                         mod['w'] - 0.5, th,
                         boxstyle="round,pad=0.25",
                         facecolor=c1, edgecolor='none', zorder=3)
    ax.add_patch(tb)
    ax.text(mod['x'] + mod['w']/2, mod['y'] + mod['h'] - th/2 - 0.25,
            mod['name'], ha='center', va='center',
            fontsize=FONT_MODULE, fontweight='bold', color='white', zorder=4)

    n = len(mod['subs'])
    top = mod['y'] + mod['h'] - th - 0.9
    bot = mod['y'] + 0.6
    sh = (top - bot) / n
    for j, sub in enumerate(mod['subs']):
        cy = bot + (n - 1 - j) * sh + sh/2
        dot = plt.Circle((mod['x'] + 0.9, cy), 0.32,
                          facecolor=c1, edgecolor='none', zorder=3)
        ax.add_patch(dot)
        ax.text(mod['x'] + 1.8, cy, sub,
                ha='left', va='center', fontsize=FONT_SUB, color='#333333', zorder=3,
                linespacing=1.22)

# ============ 连接线 ============
rcx, rcy = FW/2, ry
for mod in modules:
    mcx = mod['x'] + mod['w']/2
    mcy = mod['y'] + mod['h']
    ax.plot([rcx, mcx], [rcy, mcy + 0.1],
            color='#90a4ae', linewidth=4.5, zorder=0, alpha=0.6)
    ax.plot(mcx, mcy + 0.1, 'o', color='#90a4ae', markersize=14, zorder=1)

# ============ 技术栈 ============
tech_bot = 3.0
tech_h = row2_y - tech_bot - 1.5
tech = FancyBboxPatch((0.5, tech_bot), FW - 1.0, tech_h,
                       boxstyle="round,pad=0.6",
                       facecolor='#eceff1', edgecolor='#b0bec5', linewidth=4, zorder=0)
ax.add_patch(tech)

ty = tech_bot + tech_h - 2.0
dy = 4.0
ax.text(FW/2, ty, '核心技术栈', ha='center', va='center',
        fontsize=FONT_TECH_HEAD, fontweight='bold', color='#455a64')

ty -= dy
ax.text(FW/2, ty, '后端: Spring Boot 3.2.0 + JPA + SQLite    前端: Vue 3 + Element Plus + Axios',
        ha='center', va='center', fontsize=FONT_TECH_BODY, color='#607d8b')

ty -= dy * 0.85
ax.text(FW/2, ty, '安全: Spring Security + JWT + BCrypt    构建: Maven + Vite',
        ha='center', va='center', fontsize=FONT_TECH_BODY, color='#607d8b')

ty -= dy
ax.text(FW/2, ty, '核心数据结构: HashMap (ISBN快速查询 O(1))    树结构 (图书分类管理)',
        ha='center', va='center', fontsize=FONT_TECH_BODY, color='#607d8b')

ty -= dy * 0.85
ax.text(FW/2, ty, '链表 (借阅关联存储)    LinkedHashMap (LRU缓存淘汰)',
        ha='center', va='center', fontsize=FONT_TECH_BODY, color='#607d8b')

ty -= dy
ax.text(FW/2, ty, '系统角色: 管理员 (图书管理/逾期预警/数据统计)    读者/学生 (借阅归还/愿望单/个性化推荐/智能问答)',
        ha='center', va='center', fontsize=FONT_TECH_BODY, color='#607d8b')

# ============ 底部 ============
ax.text(FW/2, 1.5, '图1  系统功能模块结构图', ha='center', va='center',
        fontsize=FONT_BOTTOM, fontweight='bold', color='#37474f')

# ============ 保存 ============
output_path = os.path.join(os.path.dirname(__file__), '功能模块图_大字体.png')
print(f"生成中... 画布{FW}x{FH}英寸 @ {DPI}dpi → {FW*DPI}x{FH*DPI}px")
plt.savefig(output_path, dpi=DPI, bbox_inches='tight',
            facecolor='#f5f7fa', edgecolor='none')
plt.close()
gc.collect()
print(f"完成: {output_path}")
