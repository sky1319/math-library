#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""极限大字版 - UML用例图，字体最大化"""

import matplotlib
import os
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Arc, Ellipse
import gc

plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# ============ 超巨画布 ============
FW, FH = 110, 80
DPI = 150
fig, ax = plt.subplots(1, 1, figsize=(FW, FH))
ax.set_xlim(0, FW)
ax.set_ylim(0, FH)
ax.axis('off')
ax.set_facecolor('#fafbfc')

# ============ 极限字体 ============
FONT_TITLE       = 260
FONT_SYSTEM      = 140
FONT_ACTOR       = 120
FONT_USECASE     = 72
FONT_SECTION     = 110
FONT_RELATION    = 62
FONT_BOTTOM      = 140

LINE_W = 5           # 线条宽度
ELLIPSE_LW = 6       # 椭圆线条宽度
ARROW_LW = 4.5       # 箭头线宽

# ============ 标题 ============
ax.text(FW/2, FH - 2.5, '智能图书推荐与管理系统 - 用例图',
        ha='center', va='top', fontsize=FONT_TITLE, fontweight='bold', color='#1a237e')

# ============ 系统边界 ============
sys_box = FancyBboxPatch((14, 4.0), FW - 28, FH - 15,
                          boxstyle="round,pad=1.0",
                          facecolor='none', edgecolor='#37474f',
                          linewidth=6, linestyle='--', zorder=0)
ax.add_patch(sys_box)
ax.text(FW/2, FH - 7.5, '智能图书推荐与管理系统', ha='center', va='center',
        fontsize=FONT_SYSTEM, fontweight='bold', color='#37474f',
        bbox=dict(boxstyle='round,pad=0.6', facecolor='#fafbfc', edgecolor='none'))

# ============ 辅助函数 ============
def draw_actor(ax, x, y, label, scale=1.0, color='#37474f'):
    s = scale
    # 头
    head = plt.Circle((x, y + 0.65 * s), 0.18 * s, facecolor='white',
                       edgecolor=color, linewidth=LINE_W, zorder=5)
    ax.add_patch(head)
    # 身体
    ax.plot([x, x], [y + 0.47 * s, y + 0.05 * s], color=color, linewidth=LINE_W, zorder=5)
    # 手臂
    ax.plot([x - 0.28 * s, x + 0.28 * s], [y + 0.35 * s, y + 0.35 * s],
            color=color, linewidth=LINE_W, zorder=5)
    # 腿
    ax.plot([x, x - 0.18 * s], [y + 0.05 * s, y - 0.35 * s], color=color, linewidth=LINE_W, zorder=5)
    ax.plot([x, x + 0.18 * s], [y + 0.05 * s, y - 0.35 * s], color=color, linewidth=LINE_W, zorder=5)
    # 标签
    ax.text(x, y - 0.55 * s, label, ha='center', va='top',
            fontsize=FONT_ACTOR, fontweight='bold', color=color)

def draw_usecase(ax, x, y, w, h, label, color='#1565c0'):
    ellipse = Ellipse((x, y), w, h, facecolor='white', edgecolor=color,
                       linewidth=ELLIPSE_LW, zorder=4)
    ax.add_patch(ellipse)
    ax.text(x, y, label, ha='center', va='center', fontsize=FONT_USECASE,
            color='#212121', zorder=5)

def draw_line(ax, x1, y1, x2, y2, color='#546e7a'):
    ax.plot([x1, x2], [y1, y2], color=color, linewidth=LINE_W, zorder=2, alpha=0.75)

# ============ 角色 ============
# 管理员 - 左侧
draw_actor(ax, 5.0, 41.0, '管理员\n(Admin)', scale=3.5, color='#1565c0')
# 读者/学生 - 右侧
draw_actor(ax, FW - 5.0, 41.0, '读者/学生\n(Reader)', scale=3.5, color='#2e7d32')

# ============ 管理员用例 (左侧区域) ============
admin_color = '#1565c0'
# 椭圆 (x, y, w, h, label) — 超大椭圆容纳72pt文字
# 纵向分布: y 从 60 到 10，9个用例
admin_ucs = [
    (35.0, 60.0, 22.0, 5.5, '登录系统'),
    (26.0, 52.0, 22.0, 5.5, '添加图书'),
    (44.0, 52.0, 22.0, 5.5, '删除图书'),
    (35.0, 44.5, 28.0, 5.5, '修改图书信息'),
    (35.0, 37.5, 22.0, 5.5, '查询图书'),
    (35.0, 30.0, 32.0, 5.5, '查看所有借阅记录'),
    (35.0, 22.5, 28.0, 5.5, '查看逾期预警'),
    (35.0, 15.0, 32.0, 5.5, '查看分类借阅统计'),
    (35.0, 8.0,  28.0, 5.0, '查看操作日志'),
]

for uc in admin_ucs:
    draw_usecase(ax, uc[0], uc[1], uc[2], uc[3], uc[4], color=admin_color)

# 管理员连线 (从 actor 到手用例中心)
admin_actor_pt = (9.0, 43.5)
for uc in admin_ucs:
    draw_line(ax, admin_actor_pt[0], admin_actor_pt[1], uc[0] - uc[2]/2, uc[1])

# 管理员区域标签
ax.text(35.0, 64.5, '管理员功能', ha='center', va='center',
        fontsize=FONT_SECTION, fontweight='bold', color=admin_color,
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#e3f2fd', edgecolor='#90caf9', alpha=0.9))

# ============ 读者用例 (右侧区域) ============
reader_color = '#2e7d32'
reader_ucs = [
    (73.0, 60.0, 22.0, 5.5, '登录系统'),
    (73.0, 52.5, 22.0, 5.5, '查询图书'),
    (73.0, 45.0, 22.0, 5.5, '借阅图书'),
    (73.0, 37.5, 22.0, 5.5, '归还图书'),
    (73.0, 30.0, 32.0, 5.5, '查看个人借阅记录'),
    (73.0, 22.5, 32.0, 5.5, '查看个性化推荐'),
    (60.0, 15.0, 24.0, 5.5, '添加图书到愿望单'),
    (86.0, 15.0, 24.0, 5.5, '从愿望单移除'),
    (73.0, 8.0,  24.0, 5.5, '查看愿望单'),
    (73.0, 1.5,  28.0, 5.0, '使用智能问答'),
]

for uc in reader_ucs:
    draw_usecase(ax, uc[0], uc[1], uc[2], uc[3], uc[4], color=reader_color)

# 读者连线
reader_actor_pt = (FW - 9.0, 43.5)
for uc in reader_ucs:
    draw_line(ax, reader_actor_pt[0], reader_actor_pt[1], uc[0] + uc[2]/2, uc[1])

# 读者区域标签
ax.text(73.0, 64.5, '读者功能', ha='center', va='center',
        fontsize=FONT_SECTION, fontweight='bold', color=reader_color,
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#e8f5e9', edgecolor='#a5d6a7', alpha=0.9))

# ============ 关联关系标注 ============
# include: 借阅图书 <<include>> 校验借阅条件
ax.annotate('', xy=(67.5, 45.0), xytext=(52.0, 50.0),
            arrowprops=dict(arrowstyle='->', color='#ff6f00', lw=ARROW_LW, linestyle='dashed'))
ax.text(57.0, 48.5, '<<include>>', fontsize=FONT_RELATION, color='#ff6f00', style='italic')

# extend: 查看逾期预警 <<extend>> 登录
ax.annotate('', xy=(41.0, 30.0), xytext=(41.0, 56.0),
            arrowprops=dict(arrowstyle='->', color='#7b1fa2', lw=ARROW_LW, linestyle='dashed'))
ax.text(44.0, 42.0, '<<extend>>', fontsize=FONT_RELATION, color='#7b1fa2', style='italic',
        rotation=90)

# ============ 底部 ============
ax.text(FW/2, FH - 78.5, '图2  系统用例图', ha='center', va='center',
        fontsize=FONT_BOTTOM, fontweight='bold', color='#37474f')

# ============ 保存 ============
output_path = os.path.join(os.path.dirname(__file__), '用例图_大字体.png')
print(f"生成中... 画布{FW}x{FH}英寸 @ {DPI}dpi")
plt.savefig(output_path, dpi=DPI, bbox_inches='tight',
            facecolor='#fafbfc', edgecolor='none')
plt.close()
gc.collect()
print(f"完成: {output_path}")
