#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成智能图书推荐与管理系统 - 系统功能模块图"""

import matplotlib
import os
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

fig, ax = plt.subplots(1, 1, figsize=(26, 18))
ax.set_xlim(0, 26)
ax.set_ylim(0, 18)
ax.axis('off')
ax.set_facecolor('#f5f7fa')

# ============ 颜色方案 ============
COLOR_ROOT = '#1a237e'
COLOR_LEVEL1 = ['#1565c0', '#00838f', '#2e7d32', '#e65100', '#6a1b9a', '#c62828', '#37474f', '#4e342e']
COLOR_LEVEL2 = ['#bbdefb', '#b2ebf2', '#c8e6c9', '#ffe0b2', '#e1bee7', '#ffcdd2', '#cfd8dc', '#d7ccc8']
COLOR_LEVEL2_TEXT = ['#0d47a1', '#006064', '#1b5e20', '#bf360c', '#4a148c', '#b71c1c', '#263238', '#3e2723']

# ============ 标题 ============
ax.text(13, 17.2, '智能图书推荐与管理系统 - 功能模块图',
        ha='center', va='center', fontsize=22, fontweight='bold', color='#1a237e')

# ============ 绘制根节点 ============
root = FancyBboxPatch((7.5, 15.5), 11, 1.0,
                       boxstyle="round,pad=0.15",
                       facecolor=COLOR_ROOT, edgecolor='#0d1b5e', linewidth=2.5)
ax.add_patch(root)
ax.text(13, 16.0, '智能图书推荐与管理系统', ha='center', va='center',
        fontsize=14, fontweight='bold', color='white')

# ============ 定义模块数据 ============
modules = [
    {
        'name': '用户认证模块',
        'x': 0.4, 'y': 11.5, 'w': 5.8, 'h': 3.2,
        'subs': [
            '用户登录/登出',
            'JWT Token认证',
            'BCrypt密码加密',
            '角色权限控制\n(管理员/读者)',
        ]
    },
    {
        'name': '图书管理模块',
        'x': 6.8, 'y': 11.5, 'w': 5.8, 'h': 3.2,
        'subs': [
            '图书信息增删改查',
            'ISBN快速查询\n(HashMap O(1))',
            '多条件组合检索',
            '分类筛选与搜索',
        ]
    },
    {
        'name': '借阅管理模块',
        'x': 13.2, 'y': 11.5, 'w': 5.8, 'h': 3.2,
        'subs': [
            '图书借阅处理',
            '图书归还处理',
            '借阅记录查询',
            '逾期自动预警',
        ]
    },
    {
        'name': '个性化推荐模块',
        'x': 19.6, 'y': 11.5, 'w': 5.8, 'h': 3.2,
        'subs': [
            '借阅关联分析\n(链表存储)',
            '同分类图书推荐',
            '推荐列表展示\n(≥3本)',
            '关联度权重计算',
        ]
    },
    # ---- Row 2 ----
    {
        'name': '愿望单模块',
        'x': 0.4, 'y': 6.5, 'w': 5.8, 'h': 3.0,
        'subs': [
            '添加图书到愿望单',
            '查看我的愿望单',
            '从愿望单移除图书',
            '愿望单直接借阅',
        ]
    },
    {
        'name': '智能问答模块',
        'x': 6.8, 'y': 6.5, 'w': 5.8, 'h': 3.0,
        'subs': [
            '自然语言输入解析',
            '关键词提取与分词',
            '多字段模糊匹配',
            '智能结果返回',
        ]
    },
    {
        'name': '数据统计模块',
        'x': 13.2, 'y': 6.5, 'w': 5.8, 'h': 3.0,
        'subs': [
            '分类借阅统计\n(树结构聚合)',
            '热门图书排行',
            '读者借阅统计',
            '流通数据分析',
        ]
    },
    {
        'name': '系统管理模块',
        'x': 19.6, 'y': 6.5, 'w': 5.8, 'h': 3.0,
        'subs': [
            '操作日志记录',
            'LRU查询缓存\n(LinkedHashMap)',
            '数据初始化加载',
            '用户账号管理',
        ]
    },
]

# ============ 绘制模块 ============
for i, mod in enumerate(modules):
    color1 = COLOR_LEVEL1[i % len(COLOR_LEVEL1)]
    color2 = COLOR_LEVEL2[i % len(COLOR_LEVEL2)]
    color2text = COLOR_LEVEL2_TEXT[i % len(COLOR_LEVEL2_TEXT)]

    # 模块背景
    box = FancyBboxPatch((mod['x'], mod['y']), mod['w'], mod['h'],
                          boxstyle="round,pad=0.1",
                          facecolor='white', edgecolor=color1, linewidth=2.2,
                          zorder=2)
    ax.add_patch(box)

    # 模块标题栏
    title_bar = FancyBboxPatch((mod['x'] + 0.05, mod['y'] + mod['h'] - 0.65),
                                mod['w'] - 0.1, 0.55,
                                boxstyle="round,pad=0.05",
                                facecolor=color1, edgecolor='none', zorder=3)
    ax.add_patch(title_bar)
    ax.text(mod['x'] + mod['w'] / 2, mod['y'] + mod['h'] - 0.375,
            mod['name'], ha='center', va='center',
            fontsize=10.5, fontweight='bold', color='white', zorder=4)

    # 子功能项
    n_subs = len(mod['subs'])
    sub_h = (mod['h'] - 0.9) / n_subs
    for j, sub in enumerate(mod['subs']):
        sy = mod['y'] + 0.08 + (n_subs - 1 - j) * sub_h

        # 小圆点
        circle = plt.Circle((mod['x'] + 0.35, sy + sub_h / 2), 0.08,
                             facecolor=color1, edgecolor='none', zorder=3)
        ax.add_patch(circle)

        # 文字
        ax.text(mod['x'] + 0.6, sy + sub_h / 2, sub,
                ha='left', va='center', fontsize=8.2, color='#333333', zorder=3,
                linespacing=1.3)

# ============ 绘制连接线 ============
# 从根节点到各一级模块的连线
root_center_x, root_center_y = 13, 15.5
for mod in modules:
    mod_cx = mod['x'] + mod['w'] / 2
    mod_cy = mod['y'] + mod['h']
    ax.plot([root_center_x, mod_cx], [root_center_y, mod_cy + 0.05],
            color='#90a4ae', linewidth=1.2, zorder=0, alpha=0.7)
    # 小圆点连接
    ax.plot(mod_cx, mod_cy + 0.05, 'o', color='#90a4ae', markersize=3, zorder=1)

# ============ 图例 / 技术标注 ============
tech_box = FancyBboxPatch((0.4, 3.5), 25.2, 2.2,
                           boxstyle="round,pad=0.15",
                           facecolor='#eceff1', edgecolor='#b0bec5', linewidth=1.2, zorder=0)
ax.add_patch(tech_box)
ax.text(13, 5.25, '核心技术栈', ha='center', va='center',
        fontsize=11, fontweight='bold', color='#455a64')
ax.text(13, 4.55, '后端: Spring Boot 3.2.0 + JPA + SQLite  |  前端: Vue 3 + Element Plus + Axios  |  安全: Spring Security + JWT + BCrypt  |  构建: Maven + Vite',
        ha='center', va='center', fontsize=8.5, color='#607d8b')

# 数据结构标注
ax.text(13, 3.95, '核心数据结构: HashMap (ISBN快速查询 O(1))  |  树结构 (图书分类管理)  |  链表 (借阅关联存储)  |  LinkedHashMap (LRU缓存淘汰)',
        ha='center', va='center', fontsize=8.5, color='#607d8b')

# 角色说明
ax.text(13, 3.35, '系统角色: 管理员 (图书管理/逾期预警/数据统计)  |  读者/学生 (借阅归还/愿望单/个性化推荐/智能问答)',
        ha='center', va='center', fontsize=8.5, color='#607d8b')

# ============ 底部信息 ============
ax.text(13, 0.8, '图1  系统功能模块结构图', ha='center', va='center',
        fontsize=12, fontweight='bold', color='#37474f')

plt.tight_layout(pad=0.5)
output_path = os.path.join(os.path.dirname(__file__), '功能模块图.png')
plt.savefig(output_path, dpi=200, bbox_inches='tight',
            facecolor='#f5f7fa', edgecolor='none')
plt.close()
print(f"功能模块图已生成: {output_path}")
