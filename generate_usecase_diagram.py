#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成智能图书推荐与管理系统 - UML用例图"""

import matplotlib
import os
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, Arc, FancyArrowPatch, Ellipse
import numpy as np

plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

fig, ax = plt.subplots(1, 1, figsize=(28, 20))
ax.set_xlim(0, 28)
ax.set_ylim(0, 20)
ax.axis('off')
ax.set_facecolor('#fafbfc')

# ============ 标题 ============
ax.text(14, 19.3, '智能图书推荐与管理系统 - 用例图',
        ha='center', va='center', fontsize=22, fontweight='bold', color='#1a237e')

# ============ 系统边界 (大矩形) ============
sys_boundary = FancyBboxPatch((3.5, 1.0), 21, 17.5,
                               boxstyle="round,pad=0.3",
                               facecolor='none', edgecolor='#37474f',
                               linewidth=2.5, linestyle='--', zorder=0)
ax.add_patch(sys_boundary)
ax.text(14, 18.0, '智能图书推荐与管理系统', ha='center', va='center',
        fontsize=13, fontweight='bold', color='#37474f',
        bbox=dict(boxstyle='round,pad=0.3', facecolor='#fafbfc', edgecolor='none'))

# ============ 辅助绘制函数 ============
def draw_actor(ax, x, y, label, scale=1.0, color='#37474f'):
    """绘制火柴人 Actor"""
    s = scale
    # 头 (圆)
    head = plt.Circle((x, y + 0.65 * s), 0.18 * s, facecolor='white',
                       edgecolor=color, linewidth=2, zorder=5)
    ax.add_patch(head)
    # 身体
    ax.plot([x, x], [y + 0.47 * s, y + 0.05 * s], color=color, linewidth=2, zorder=5)
    # 手臂
    ax.plot([x - 0.28 * s, x + 0.28 * s], [y + 0.35 * s, y + 0.35 * s],
            color=color, linewidth=2, zorder=5)
    # 左腿
    ax.plot([x, x - 0.18 * s], [y + 0.05 * s, y - 0.35 * s],
            color=color, linewidth=2, zorder=5)
    # 右腿
    ax.plot([x, x + 0.18 * s], [y + 0.05 * s, y - 0.35 * s],
            color=color, linewidth=2, zorder=5)
    # 标签
    ax.text(x, y - 0.55 * s, label, ha='center', va='top',
            fontsize=11, fontweight='bold', color=color)

def draw_usecase(ax, x, y, w, h, label, color='#1565c0', fontsize=8.5):
    """绘制用例椭圆"""
    ellipse = Ellipse((x, y), w, h, facecolor='white', edgecolor=color,
                       linewidth=2.0, zorder=4)
    ax.add_patch(ellipse)
    ax.text(x, y, label, ha='center', va='center', fontsize=fontsize,
            color='#212121', zorder=5)

def draw_line(ax, x1, y1, x2, y2, color='#546e7a', lw=1.5):
    """绘制连线"""
    ax.plot([x1, x2], [y1, y2], color=color, linewidth=lw, zorder=2, alpha=0.8)

# ============ 绘制角色 (Actors) ============
# 管理员 - 左侧
draw_actor(ax, 1.3, 11.5, '管理员\n(Admin)', scale=1.15, color='#1565c0')

# 读者/学生 - 右侧
draw_actor(ax, 26.7, 11.5, '读者/学生\n(Reader)', scale=1.15, color='#2e7d32')

# ============ 管理员用例 ============
# 管理员用例区域 x: 6-12, y: 2-17
admin_uc_color = '#1565c0'
admin_ucs = [
    # (x, y, w, h, label)
    (9.2, 16.2, 3.6, 1.1, '登录系统'),
    (6.5, 14.0, 3.0, 1.0, '添加图书'),
    (11.5, 14.0, 3.0, 1.0, '删除图书'),
    (9.0, 11.8, 3.6, 1.0, '修改图书信息'),
    (9.0, 9.8, 3.6, 1.0, '查询图书'),
    (9.0, 7.8, 3.6, 1.0, '查看所有借阅记录'),
    (9.0, 5.8, 3.6, 1.0, '查看逾期预警'),
    (9.0, 3.8, 3.6, 1.0, '查看分类借阅统计'),
    (9.0, 2.0, 3.0, 0.9, '查看操作日志'),
]

for uc in admin_ucs:
    draw_usecase(ax, uc[0], uc[1], uc[2], uc[3], uc[4], color=admin_uc_color, fontsize=8)

# 管理员连线
admin_connections = [
    (2.5, 12.0, 9.2, 16.2),   # 登录
    (2.5, 12.0, 6.5, 14.0),   # 添加
    (2.5, 12.0, 11.5, 14.0),  # 删除
    (2.5, 12.0, 9.0, 11.8),   # 修改
    (2.5, 12.0, 9.0, 9.8),    # 查询
    (2.5, 12.0, 9.0, 7.8),    # 借阅记录
    (2.5, 12.0, 9.0, 5.8),    # 逾期
    (2.5, 12.0, 9.0, 3.8),    # 统计
    (2.5, 12.0, 9.0, 2.0),    # 日志
]
for conn in admin_connections:
    draw_line(ax, *conn, color='#90a4ae')

# 管理员角色名标注
ax.text(9.0, 17.0, '管理员功能', ha='center', va='center',
        fontsize=10.5, fontweight='bold', color=admin_uc_color,
        bbox=dict(boxstyle='round,pad=0.2', facecolor='#e3f2fd', edgecolor='#90caf9', alpha=0.9))

# ============ 读者用例 ============
reader_uc_color = '#2e7d32'
reader_ucs = [
    (18.8, 16.2, 3.6, 1.1, '登录系统'),
    (18.8, 14.0, 3.6, 1.0, '查询图书'),
    (18.8, 12.0, 3.4, 1.0, '借阅图书'),
    (18.8, 10.0, 3.4, 1.0, '归还图书'),
    (18.8, 8.0, 3.8, 1.0, '查看个人借阅记录'),
    (15.5, 4.5, 3.2, 1.0, '添加图书到愿望单'),
    (22.0, 4.5, 3.2, 1.0, '从愿望单移除'),
    (18.8, 2.8, 3.8, 1.0, '查看愿望单'),
    (18.8, 6.0, 3.8, 1.0, '查看个性化推荐'),
    (18.8, 1.5, 3.2, 0.9, '使用智能问答'),
]

for uc in reader_ucs:
    draw_usecase(ax, uc[0], uc[1], uc[2], uc[3], uc[4], color=reader_uc_color, fontsize=8)

# 读者连线
reader_connections = [
    (25.5, 12.0, 18.8, 16.2),   # 登录
    (25.5, 12.0, 18.8, 14.0),   # 查询
    (25.5, 12.0, 18.8, 12.0),   # 借阅
    (25.5, 12.0, 18.8, 10.0),   # 归还
    (25.5, 12.0, 18.8, 8.0),    # 借阅记录
    (25.5, 12.0, 18.8, 6.0),    # 推荐
    (25.5, 12.0, 15.5, 4.5),    # 添加愿望单
    (25.5, 12.0, 22.0, 4.5),    # 移除愿望单
    (25.5, 12.0, 18.8, 2.8),    # 查看愿望单
    (25.5, 12.0, 18.8, 1.5),    # 智能问答
]
for conn in reader_connections:
    draw_line(ax, *conn, color='#a5d6a7')

# 读者角色名标注
ax.text(18.8, 17.0, '读者功能', ha='center', va='center',
        fontsize=10.5, fontweight='bold', color=reader_uc_color,
        bbox=dict(boxstyle='round,pad=0.2', facecolor='#e8f5e9', edgecolor='#a5d6a7', alpha=0.9))

# ============ 共享用例 (中间区域) ============
# 登录和查询是共享的，已经在两边各画了一份

# ============ 关联关系标注 ============
# include 关系: 借阅图书 <<include>> 校验借阅条件
ax.annotate('', xy=(17.8, 12.0), xytext=(13.8, 13.7),
            arrowprops=dict(arrowstyle='->', color='#ff6f00', lw=1.5, linestyle='dashed'))
ax.text(15.2, 13.2, '<<include>>', fontsize=7, color='#ff6f00', style='italic')

# extend 关系: 查看逾期预警 <<extend>> 登录
ax.annotate('', xy=(11.6, 6.8), xytext=(11.6, 15.5),
            arrowprops=dict(arrowstyle='->', color='#7b1fa2', lw=1.5, linestyle='dashed'))
ax.text(12.2, 11.0, '<<extend>>', fontsize=7, color='#7b1fa2', style='italic',
        rotation=90)

# ============ 底部信息 ============
ax.text(14, 0.5, '图2  系统用例图', ha='center', va='center',
        fontsize=13, fontweight='bold', color='#37474f')

plt.tight_layout(pad=0.5)
output_path = os.path.join(os.path.dirname(__file__), '用例图.png')
plt.savefig(output_path, dpi=200, bbox_inches='tight',
            facecolor='#fafbfc', edgecolor='none')
plt.close()
print(f"用例图已生成: {output_path}")
