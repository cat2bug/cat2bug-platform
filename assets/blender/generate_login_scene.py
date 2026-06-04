#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cat2Bug 登录页 3D 场景一键生成脚本

用法:
  blender --background --python generate_login_scene.py
  或 Blender 文本编辑器中 Alt+P 运行

输出:
  cat2bug-platform-ui/src/assets/models/login-scene.glb
  cat2bug-platform-ui/src/assets/models/login-scene.blend
"""

from __future__ import annotations

import math
import os
import sys

import bpy
from mathutils import Euler, Vector

# ---------------------------------------------------------------------------
# 路径与调色板（与 CatIllustration.vue 夜间金色 rim light 协调）
# ---------------------------------------------------------------------------

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))
OUTPUT_DIR = os.path.join(REPO_ROOT, "cat2bug-platform-ui", "src", "assets", "models")
BLEND_PATH = os.path.join(OUTPUT_DIR, "login-scene.blend")
GLB_PATH = os.path.join(OUTPUT_DIR, "login-scene.glb")

# 骨骼命名约定 — Web 端通过名称查找尾巴骨骼
ARMATURE_NAME = "Cat_Armature"
BONE_ROOT = "Cat_Root"
BONE_SPINE = "Cat_Spine"
BONE_TAIL_PREFIX = "Cat_Tail"  # Cat_Tail.001 … Cat_Tail.005
TAIL_BONE_COUNT = 5

COLORS = {
    "cat_gray": (0.55, 0.56, 0.58, 1.0),
    "cat_dark_stripe": (0.38, 0.39, 0.42, 1.0),
    "cat_white": (0.95, 0.94, 0.92, 1.0),
    "cat_pink": (0.92, 0.65, 0.72, 1.0),
    "cat_nose": (0.85, 0.55, 0.60, 1.0),
    "laptop_body": (0.12, 0.13, 0.15, 1.0),
    "laptop_screen": (0.08, 0.09, 0.11, 1.0),
    "laptop_keyboard": (0.18, 0.19, 0.22, 1.0),
    "laptop_logo": (0.75, 0.76, 0.78, 1.0),
    "pot": (0.22, 0.23, 0.25, 1.0),
    "stem": (0.35, 0.55, 0.32, 1.0),
    "leaf": (0.42, 0.62, 0.38, 1.0),
    "daisy_white": (0.96, 0.95, 0.93, 1.0),
    "daisy_yellow": (0.95, 0.78, 0.22, 1.0),
    "mug": (0.24, 0.25, 0.27, 1.0),
    "mug_logo": (0.88, 0.89, 0.91, 1.0),
    "steam": (0.85, 0.87, 0.90, 0.55),
    "rim_light": (0.98, 0.88, 0.45, 1.0),
    "fill_light": (0.55, 0.60, 0.75, 1.0),
}


# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------

def clear_scene() -> None:
    """清空场景并移除孤立数据块。"""
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for block in (
        bpy.data.meshes,
        bpy.data.materials,
        bpy.data.armatures,
        bpy.data.actions,
        bpy.data.curves,
    ):
        for item in list(block):
            if item.users == 0:
                block.remove(item)


def make_principled_material(
    name: str,
    rgba: tuple,
    roughness: float = 0.88,
    metallic: float = 0.0,
    alpha: float = 1.0,
) -> bpy.types.Material:
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.inputs["Base Color"].default_value = rgba
    bsdf.inputs["Roughness"].default_value = roughness
    bsdf.inputs["Metallic"].default_value = metallic
    if alpha < 1.0:
        mat.blend_method = "BLEND"
        bsdf.inputs["Alpha"].default_value = alpha
    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    return mat


def assign_material(obj: bpy.types.Object, mat: bpy.types.Material) -> None:
    if obj.data.materials:
        obj.data.materials[0] = mat
    else:
        obj.data.materials.append(mat)


def add_mesh_from_verts_faces(
    name: str,
    verts: list,
    faces: list,
    location=(0.0, 0.0, 0.0),
    rotation=(0.0, 0.0, 0.0),
    scale=(1.0, 1.0, 1.0),
) -> bpy.types.Object:
    mesh = bpy.data.meshes.new(name + "_Mesh")
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    obj.location = Vector(location)
    obj.rotation_euler = Euler(rotation, "XYZ")
    obj.scale = Vector(scale)
    return obj


def shade_smooth(obj: bpy.types.Object, angle: float = math.radians(35)) -> None:
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.shade_smooth()
    mod = obj.modifiers.new("EdgeSplit", "EDGE_SPLIT")
    mod.split_angle = angle
    obj.select_set(False)


def add_bevel(obj: bpy.types.Object, width: float = 0.008, segments: int = 2) -> None:
    mod = obj.modifiers.new("Bevel", "BEVEL")
    mod.width = width
    mod.segments = segments
    mod.limit_method = "ANGLE"
    mod.angle_limit = math.radians(30)


def create_collection(name: str, parent: bpy.types.Collection | None = None) -> bpy.types.Collection:
    col = bpy.data.collections.new(name)
    if parent:
        parent.children.link(col)
    else:
        bpy.context.scene.collection.children.link(col)
    return col


def link_to_collection(obj: bpy.types.Object, collection: bpy.types.Collection) -> None:
    for col in obj.users_collection:
        col.objects.unlink(obj)
    collection.objects.link(obj)


def create_paw_logo_mesh(name: str, size: float = 0.04) -> bpy.types.Object:
    """简化猫爪 logo：4 圆垫 + 1 掌垫。"""
    parts = []
    pad_r = size * 0.22
    toe_offsets = [
        (-pad_r * 1.2, pad_r * 1.5, 0),
        (-pad_r * 0.4, pad_r * 1.8, 0),
        (pad_r * 0.4, pad_r * 1.8, 0),
        (pad_r * 1.2, pad_r * 1.5, 0),
    ]
    for i, off in enumerate(toe_offsets):
        bpy.ops.mesh.primitive_uv_sphere_add(
            segments=8, ring_count=6, radius=pad_r, location=off
        )
        toe = bpy.context.active_object
        toe.name = f"{name}_Toe{i}"
        parts.append(toe)
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=8, ring_count=6, radius=pad_r * 1.4, location=(0, 0, 0)
    )
    palm = bpy.context.active_object
    palm.name = f"{name}_Palm"
    palm.scale = (1.1, 0.85, 0.35)
    parts.append(palm)

    bpy.ops.object.select_all(action="DESELECT")
    for p in parts:
        p.select_set(True)
    bpy.context.view_layer.objects.active = parts[0]
    bpy.ops.object.join()
    logo = bpy.context.active_object
    logo.name = name
    return logo


# ---------------------------------------------------------------------------
# 笔记本
# ---------------------------------------------------------------------------

def build_laptop(collection: bpy.types.Collection) -> bpy.types.Object:
    mat_body = make_principled_material("Mat_LaptopBody", COLORS["laptop_body"])
    mat_screen = make_principled_material("Mat_LaptopScreen", COLORS["laptop_screen"])
    mat_keys = make_principled_material("Mat_LaptopKeys", COLORS["laptop_keyboard"])
    mat_logo = make_principled_material("Mat_LaptopLogo", COLORS["laptop_logo"])

    # 底座
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.015))
    base = bpy.context.active_object
    base.name = "Laptop_Base"
    base.scale = (0.28, 0.20, 0.015)
    assign_material(base, mat_body)
    add_bevel(base, 0.004)

    # 键盘区（略凹陷）
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.02, 0.032))
    keyboard = bpy.context.active_object
    keyboard.name = "Laptop_Keyboard"
    keyboard.scale = (0.24, 0.14, 0.004)
    assign_material(keyboard, mat_keys)

    # 简化键帽网格
    for row in range(4):
        for col in range(10):
            x = -0.10 + col * 0.022
            y = -0.06 + row * 0.028
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x, y, 0.038))
            key = bpy.context.active_object
            key.name = f"Laptop_Key_{row}_{col}"
            key.scale = (0.008, 0.008, 0.002)
            assign_material(key, mat_keys)

    # 屏幕转轴
    hinge_y = 0.10
    bpy.ops.mesh.primitive_cylinder_add(
        radius=0.006, depth=0.26, location=(0, hinge_y, 0.03), rotation=(math.pi / 2, 0, 0)
    )
    hinge = bpy.context.active_object
    hinge.name = "Laptop_Hinge"
    assign_material(hinge, mat_body)

    # 屏幕 — 约 110° 开合
    screen_angle = math.radians(110 - 90)  # 相对垂直面向后倾 20°
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, hinge_y + 0.01, 0.14))
    screen = bpy.context.active_object
    screen.name = "Laptop_Screen"
    screen.scale = (0.28, 0.008, 0.18)
    screen.rotation_euler = (screen_angle, 0, 0)
    assign_material(screen, mat_screen)
    add_bevel(screen, 0.003)

    # 屏幕背面 logo
    logo = create_paw_logo_mesh("Laptop_Logo", size=0.05)
    logo.location = (0, hinge_y + 0.022, 0.17)
    logo.rotation_euler = (screen_angle + math.pi / 2, 0, 0)
    logo.scale = (1.0, 0.15, 1.0)
    assign_material(logo, mat_logo)

    parts = [base, keyboard, hinge, screen, logo]
    # 收集键帽
    for obj in bpy.data.objects:
        if obj.name.startswith("Laptop_Key_"):
            parts.append(obj)

    bpy.ops.object.select_all(action="DESELECT")
    for p in parts:
        p.select_set(True)
    bpy.context.view_layer.objects.active = base
    bpy.ops.object.join()
    laptop = bpy.context.active_object
    laptop.name = "Laptop"
    shade_smooth(laptop)
    link_to_collection(laptop, collection)
    return laptop


# ---------------------------------------------------------------------------
# 花盆与雏菊
# ---------------------------------------------------------------------------

def build_flower_pot(collection: bpy.types.Collection) -> bpy.types.Object:
    mat_pot = make_principled_material("Mat_Pot", COLORS["pot"])
    mat_stem = make_principled_material("Mat_Stem", COLORS["stem"])
    mat_leaf = make_principled_material("Mat_Leaf", COLORS["leaf"])
    mat_petal = make_principled_material("Mat_DaisyWhite", COLORS["daisy_white"])
    mat_center = make_principled_material("Mat_DaisyCenter", COLORS["daisy_yellow"])

    bpy.ops.mesh.primitive_cylinder_add(
        vertices=12, radius=0.035, depth=0.05, location=(0.22, -0.08, 0.025)
    )
    pot = bpy.context.active_object
    pot.name = "FlowerPot"
    assign_material(pot, mat_pot)
    add_bevel(pot, 0.003)

    # 茎
    bpy.ops.mesh.primitive_cylinder_add(
        vertices=8, radius=0.004, depth=0.12, location=(0.22, -0.08, 0.11)
    )
    stem = bpy.context.active_object
    stem.name = "Flower_Stem"
    assign_material(stem, mat_stem)

    # 两片叶子
    for side, rot_z in [(-1, 0.5), (1, -0.5)]:
        bpy.ops.mesh.primitive_uv_sphere_add(
            segments=6, ring_count=4, radius=0.025, location=(0.22 + side * 0.02, -0.08, 0.07)
        )
        leaf = bpy.context.active_object
        leaf.name = f"Flower_Leaf_{side}"
        leaf.scale = (0.5, 1.2, 0.15)
        leaf.rotation_euler = (0.3, 0, rot_z)
        assign_material(leaf, mat_leaf)

    # 雏菊 — 5 白瓣 + 黄心
    center = (0.22, -0.08, 0.19)
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=8, ring_count=6, radius=0.012, location=center
    )
    core = bpy.context.active_object
    core.name = "Daisy_Center"
    assign_material(core, mat_center)

    for i in range(5):
        angle = i * (2 * math.pi / 5)
        px = center[0] + math.cos(angle) * 0.022
        py = center[1] + math.sin(angle) * 0.022
        bpy.ops.mesh.primitive_uv_sphere_add(
            segments=6, ring_count=4, radius=0.014, location=(px, py, center[2])
        )
        petal = bpy.context.active_object
        petal.name = f"Daisy_Petal_{i}"
        petal.scale = (0.45, 1.3, 0.12)
        petal.rotation_euler = (0, 0, angle)
        assign_material(petal, mat_petal)

    parts = [obj for obj in bpy.data.objects if obj.name.startswith(("Flower", "Daisy"))]
    parts.append(pot)
    bpy.ops.object.select_all(action="DESELECT")
    for p in parts:
        p.select_set(True)
    bpy.context.view_layer.objects.active = pot
    bpy.ops.object.join()
    flower = bpy.context.active_object
    flower.name = "FlowerPot"
    shade_smooth(flower)
    link_to_collection(flower, collection)
    return flower


# ---------------------------------------------------------------------------
# 马克杯与蒸汽
# ---------------------------------------------------------------------------

def build_mug(collection: bpy.types.Collection) -> bpy.types.Object:
    mat_mug = make_principled_material("Mat_Mug", COLORS["mug"])
    mat_logo = make_principled_material("Mat_MugLogo", COLORS["mug_logo"])
    mat_steam = make_principled_material("Mat_Steam", COLORS["steam"], alpha=0.55)

    loc = (-0.24, -0.06, 0.04)
    bpy.ops.mesh.primitive_cylinder_add(
        vertices=16, radius=0.035, depth=0.07, location=loc
    )
    cup = bpy.context.active_object
    cup.name = "Mug_Body"
    assign_material(cup, mat_mug)
    add_bevel(cup, 0.003)

    # 把手 —  torus 段
    bpy.ops.mesh.primitive_torus_add(
        major_radius=0.022,
        minor_radius=0.006,
        major_segments=16,
        minor_segments=8,
        location=(loc[0] + 0.045, loc[1], loc[2]),
        rotation=(0, math.pi / 2, 0),
    )
    handle = bpy.context.active_object
    handle.name = "Mug_Handle"
    handle.scale = (1.0, 0.6, 1.2)
    assign_material(handle, mat_mug)

    logo = create_paw_logo_mesh("Mug_Logo", size=0.035)
    logo.location = (loc[0] - 0.038, loc[1], loc[2] + 0.01)
    logo.rotation_euler = (0, math.pi / 2, 0)
    assign_material(logo, mat_logo)

    # 蒸汽 — 3 条贝塞尔曲线管
    steam_curves = []
    for i, x_off in enumerate([-0.008, 0.0, 0.008]):
        curve_data = bpy.data.curves.new(f"SteamCurve_{i}", type="CURVE")
        curve_data.dimensions = "3D"
        curve_data.bevel_depth = 0.003
        curve_data.bevel_resolution = 2
        spline = curve_data.splines.new("BEZIER")
        spline.bezier_points.add(2)
        z0 = loc[2] + 0.038
        points = [
            (loc[0] + x_off, loc[1], z0),
            (loc[0] + x_off + 0.01, loc[1] + 0.01, z0 + 0.04),
            (loc[0] + x_off - 0.005, loc[1] - 0.005, z0 + 0.08),
        ]
        for j, pt in enumerate(spline.bezier_points):
            pt.co = pt.handle_left = pt.handle_right = Vector(points[j])
        steam_obj = bpy.data.objects.new(f"Steam_{i}", curve_data)
        bpy.context.collection.objects.link(steam_obj)
        assign_material(steam_obj, mat_steam)
        steam_curves.append(steam_obj)

    # 杯体与把手合并；蒸汽曲线保持独立（曲线与 mesh join 在 GLB 导出时不稳定）
    bpy.ops.object.select_all(action="DESELECT")
    for p in (cup, handle, logo):
        p.select_set(True)
    bpy.context.view_layer.objects.active = cup
    bpy.ops.object.join()
    mug = bpy.context.active_object
    mug.name = "Mug"
    for steam_obj in steam_curves:
        steam_obj.parent = mug
        link_to_collection(steam_obj, collection)
    link_to_collection(mug, collection)
    return mug


# ---------------------------------------------------------------------------
# 猫咪 + 尾巴骨骼
# ---------------------------------------------------------------------------

def build_cat_body_parts() -> list[bpy.types.Object]:
    """构建 chibi 灰虎斑猫各部件（未合并，便于权重绘制）。"""
    mat_gray = make_principled_material("Mat_CatGray", COLORS["cat_gray"])
    mat_stripe = make_principled_material("Mat_CatStripe", COLORS["cat_dark_stripe"])
    mat_white = make_principled_material("Mat_CatWhite", COLORS["cat_white"])
    mat_pink = make_principled_material("Mat_CatPink", COLORS["cat_pink"])

    parts = []

    # 躯干 — 趴姿
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=12, ring_count=8, radius=0.09, location=(0, -0.02, 0.075)
    )
    body = bpy.context.active_object
    body.name = "Cat_Body"
    body.scale = (1.3, 1.6, 0.75)
    body.rotation_euler = (math.radians(-8), 0, 0)
    assign_material(body, mat_gray)
    parts.append(body)

    # 头
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=12, ring_count=8, radius=0.065, location=(0.12, -0.04, 0.09)
    )
    head = bpy.context.active_object
    head.name = "Cat_Head"
    head.scale = (1.1, 1.0, 0.95)
    assign_material(head, mat_gray)
    parts.append(head)

    # 口鼻白区
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=8, ring_count=6, radius=0.035, location=(0.17, -0.05, 0.08)
    )
    muzzle = bpy.context.active_object
    muzzle.name = "Cat_Muzzle"
    muzzle.scale = (0.8, 0.7, 0.55)
    assign_material(muzzle, mat_white)
    parts.append(muzzle)

    # 耳朵 x2 + 内耳
    for side, sx in [("L", -1), ("R", 1)]:
        bpy.ops.mesh.primitive_cone_add(
            vertices=4, radius1=0.028, depth=0.045,
            location=(0.10, -0.04 + sx * 0.045, 0.135),
            rotation=(0.3, 0, sx * 0.4),
        )
        ear = bpy.context.active_object
        ear.name = f"Cat_Ear_{side}"
        assign_material(ear, mat_gray)
        parts.append(ear)

        bpy.ops.mesh.primitive_cone_add(
            vertices=4, radius1=0.015, depth=0.025,
            location=(0.10, -0.04 + sx * 0.038, 0.132),
            rotation=(0.3, 0, sx * 0.4),
        )
        inner = bpy.context.active_object
        inner.name = f"Cat_InnerEar_{side}"
        assign_material(inner, mat_pink)
        parts.append(inner)

    # 闭眼 — 小扁盒
    for side, sx in [("L", -1), ("R", 1)]:
        bpy.ops.mesh.primitive_cube_add(
            size=1.0,
            location=(0.155, -0.05 + sx * 0.022, 0.102),
            rotation=(0.1, 0.2, sx * 0.15),
        )
        eye = bpy.context.active_object
        eye.name = f"Cat_EyeClosed_{side}"
        eye.scale = (0.018, 0.004, 0.003)
        assign_material(eye, mat_stripe)
        parts.append(eye)

    # 虎斑纹 — 简化条带
    for i, (px, py, pz, sx) in enumerate([
        (-0.02, -0.05, 0.10, 0.8),
        (0.04, -0.03, 0.11, 1.0),
        (0.08, -0.06, 0.095, 0.7),
    ]):
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(px, py, pz))
        stripe = bpy.context.active_object
        stripe.name = f"Cat_Stripe_{i}"
        stripe.scale = (0.06 * sx, 0.008, 0.015)
        stripe.rotation_euler = (0, 0.3, 0.2)
        assign_material(stripe, mat_stripe)
        parts.append(stripe)

    # 前爪 x2 — 趴在键盘上
    for side, sx in [("L", -1), ("R", 1)]:
        bpy.ops.mesh.primitive_uv_sphere_add(
            segments=8, ring_count=6, radius=0.025,
            location=(0.04 + sx * 0.05, -0.08, 0.042),
        )
        paw = bpy.context.active_object
        paw.name = f"Cat_PawFront_{side}"
        paw.scale = (0.9, 1.2, 0.5)
        assign_material(paw, mat_gray)
        parts.append(paw)

    # 后爪
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=8, ring_count=6, radius=0.028, location=(-0.10, -0.06, 0.038)
    )
    back_paw = bpy.context.active_object
    back_paw.name = "Cat_PawBack"
    back_paw.scale = (1.0, 1.3, 0.45)
    assign_material(back_paw, mat_gray)
    parts.append(back_paw)

    # 尾巴 mesh — 分段便于蒙皮
    tail_segments = []
    tail_start = Vector((-0.12, -0.02, 0.07))
    for i in range(TAIL_BONE_COUNT):
        t = i / max(TAIL_BONE_COUNT - 1, 1)
        pos = tail_start + Vector(
            (-0.05 - t * 0.08, 0.02 + t * 0.06, 0.01 + math.sin(t * math.pi) * 0.02)
        )
        bpy.ops.mesh.primitive_uv_sphere_add(
            segments=8, ring_count=6, radius=0.022 - t * 0.006, location=pos
        )
        seg = bpy.context.active_object
        seg.name = f"Cat_TailMesh_{i + 1:03d}"
        seg.scale = (0.7, 1.4, 0.7)
        assign_material(seg, mat_gray)
        tail_segments.append(seg)
        parts.append(seg)

    for p in parts:
        shade_smooth(p)
        add_bevel(p, 0.002, 1)

    return parts


def create_cat_armature() -> bpy.types.Object:
    """创建猫咪骨骼，尾巴 5 段链式骨骼。"""
    arm_data = bpy.data.armatures.new(ARMATURE_NAME + "_Data")
    arm_obj = bpy.data.objects.new(ARMATURE_NAME, arm_data)
    bpy.context.collection.objects.link(arm_obj)
    bpy.context.view_layer.objects.active = arm_obj
    arm_obj.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")

    bones = arm_data.edit_bones

    root = bones.new(BONE_ROOT)
    root.head = (-0.05, -0.02, 0.06)
    root.tail = (0.0, -0.02, 0.07)

    spine = bones.new(BONE_SPINE)
    spine.parent = root
    spine.head = root.tail
    spine.tail = (0.08, -0.03, 0.085)

    # 尾巴骨骼链
    tail_start = Vector((-0.12, -0.02, 0.07))
    prev_name = BONE_SPINE
    for i in range(TAIL_BONE_COUNT):
        bone_name = f"{BONE_TAIL_PREFIX}.{i + 1:03d}"
        t0 = i / TAIL_BONE_COUNT
        t1 = (i + 1) / TAIL_BONE_COUNT
        head = tail_start + Vector(
            (-0.05 * t0, 0.02 * t0 + 0.01 * t0, 0.01 * math.sin(t0 * math.pi))
        )
        tail = tail_start + Vector(
            (-0.05 * t1 - 0.02, 0.02 * t1 + 0.04 * t1, 0.01 * math.sin(t1 * math.pi))
        )
        b = bones.new(bone_name)
        b.head = head if i > 0 else tail_start
        b.tail = tail
        b.parent = bones[prev_name]
        b.use_connect = False
        prev_name = bone_name

    bpy.ops.object.mode_set(mode="OBJECT")
    return arm_obj


def join_and_rig_cat(parts: list[bpy.types.Object], arm_obj: bpy.types.Object) -> bpy.types.Object:
    """合并猫部件并自动权重绑定到骨骼。"""
    bpy.ops.object.select_all(action="DESELECT")
    for p in parts:
        p.select_set(True)
    bpy.context.view_layer.objects.active = parts[0]
    bpy.ops.object.join()
    cat = bpy.context.active_object
    cat.name = "Cat"

    cat.select_set(True)
    arm_obj.select_set(True)
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.parent_set(type="ARMATURE_AUTO")
    return cat


def animate_tail_wag(arm_obj: bpy.types.Object, action_name: str = "Cat_TailWag") -> bpy.types.Action:
    """
    为尾巴骨骼添加循环摆动 keyframe。
    动画帧 1–48，24fps，约 2 秒一周期。
    """
    scene = bpy.context.scene
    scene.frame_start = 1
    scene.frame_end = 48
    scene.render.fps = 24

    if arm_obj.animation_data is None:
        arm_obj.animation_data_create()
    action = bpy.data.actions.new(action_name)
    arm_obj.animation_data.action = action

    bpy.context.view_layer.objects.active = arm_obj
    arm_obj.select_set(True)
    bpy.ops.object.mode_set(mode="POSE")

    tail_bone_names = [f"{BONE_TAIL_PREFIX}.{i + 1:03d}" for i in range(TAIL_BONE_COUNT)]

    for frame in (1, 12, 24, 36, 48):
        scene.frame_set(frame)
        phase = (frame - 1) / 47.0 * 2 * math.pi
        for i, bn in enumerate(tail_bone_names):
            pb = arm_obj.pose.bones.get(bn)
            if pb is None:
                continue
            pb.rotation_mode = "XYZ"
            amp = 0.18 - i * 0.025  # 越靠近尖端幅度越小
            pb.rotation_euler = (
                math.sin(phase + i * 0.6) * amp * 0.3,
                math.sin(phase + i * 0.8) * amp,
                math.cos(phase + i * 0.5) * amp * 0.4,
            )
            pb.keyframe_insert(data_path="rotation_euler", frame=frame)

    # 循环（Blender 4.x: modifiers.new 仅接受 type 参数）
    for fc in action.fcurves:
        for mod in list(fc.modifiers):
            fc.modifiers.remove(mod)
        mod = fc.modifiers.new(type="CYCLES")
        mod.mode_before = "REPEAT"
        mod.mode_after = "REPEAT"

    bpy.ops.object.mode_set(mode="OBJECT")
    return action


# ---------------------------------------------------------------------------
# 灯光与场景
# ---------------------------------------------------------------------------

def setup_lighting() -> None:
    """夜间温馨场景 — 金色 rim light + 冷色补光。"""
    # 主 rim（月光感）
    bpy.ops.object.light_add(type="AREA", location=(0.5, -0.4, 0.55))
    rim = bpy.context.active_object
    rim.name = "Light_RimMoon"
    rim.data.energy = 85
    rim.data.color = COLORS["rim_light"][:3]
    rim.data.size = 0.8
    rim.rotation_euler = (math.radians(50), math.radians(15), math.radians(25))

    # 补光
    bpy.ops.object.light_add(type="AREA", location=(-0.4, 0.3, 0.35))
    fill = bpy.context.active_object
    fill.name = "Light_Fill"
    fill.data.energy = 25
    fill.data.color = COLORS["fill_light"][:3]
    fill.data.size = 1.2

    # 世界背景 — 暗色
    world = bpy.context.scene.world
    if world is None:
        world = bpy.data.worlds.new("World")
        bpy.context.scene.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs["Color"].default_value = (0.02, 0.025, 0.04, 1.0)
        bg.inputs["Strength"].default_value = 0.3


def setup_camera() -> bpy.types.Object:
    bpy.ops.object.camera_add(location=(0.0, -0.55, 0.22))
    cam = bpy.context.active_object
    cam.name = "Camera_Login"
    cam.rotation_euler = (math.radians(72), 0, 0)
    cam.data.lens = 42
    bpy.context.scene.camera = cam
    return cam


# ---------------------------------------------------------------------------
# 导出
# ---------------------------------------------------------------------------

def export_glb(filepath: str) -> None:
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=filepath,
        export_format="GLB",
        use_selection=False,
        export_apply=True,
        export_yup=True,
        export_animations=True,
        export_skins=True,
        export_def_bones=True,
        export_materials="EXPORT",
        export_cameras=False,
        export_lights=False,
    )
    print(f"[Cat2Bug] GLB 已导出: {filepath}")


def save_blend(filepath: str) -> None:
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=filepath)
    print(f"[Cat2Bug] Blend 已保存: {filepath}")


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def main() -> None:
    print("[Cat2Bug] 开始生成登录页 3D 场景…")
    clear_scene()

    root_col = create_collection("LoginScene")
    props_col = create_collection("Props", root_col)
    cat_col = create_collection("Cat", root_col)

    laptop = build_laptop(props_col)
    flower = build_flower_pot(props_col)
    mug = build_mug(props_col)

    cat_parts = build_cat_body_parts()
    for p in cat_parts:
        link_to_collection(p, cat_col)

    arm = create_cat_armature()
    link_to_collection(arm, cat_col)
    cat = join_and_rig_cat(cat_parts, arm)
    link_to_collection(cat, cat_col)

    animate_tail_wag(arm)

    setup_lighting()
    setup_camera()

    # 场景原点微调 — 与 2D 插画构图对齐
    for obj in (laptop, flower, mug, cat, arm):
        obj.location.z += 0.0

    save_blend(BLEND_PATH)
    export_glb(GLB_PATH)

    print("[Cat2Bug] 完成！")
    print(f"  Blend: {BLEND_PATH}")
    print(f"  GLB:   {GLB_PATH}")
    print(f"  尾巴骨骼: {BONE_TAIL_PREFIX}.001 … {BONE_TAIL_PREFIX}.00{TAIL_BONE_COUNT}")


if __name__ == "__main__":
    main()
