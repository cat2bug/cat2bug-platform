# Cat2Bug 登录页 3D 场景（Blender）

为登录页 `CatIllustration.vue` 配套的 **stylized low-poly** 3D 场景生成工具。一键创建猫咪（含可摆动尾巴骨骼）、笔记本、花盆雏菊、马克杯，并导出 **GLB** 供 Three.js / Vue 使用。

## 目录结构

```
assets/blender/
├── generate_login_scene.py   # 主生成脚本
├── run_generate.sh             # macOS/Linux 快捷运行
└── README.md                   # 本文档

cat2bug-platform-ui/src/assets/models/
├── login-scene.blend           # 生成后：Blender 源文件
└── login-scene.glb             # 生成后：Web 用 glTF 二进制
```

## 环境要求

- **Blender 3.6+** 或 **Blender 4.x**（需带 Python `bpy`）
- macOS 示例路径：`/Applications/Blender.app/Contents/MacOS/Blender`

## 如何运行脚本

### 方式一：命令行（推荐，无界面）

```bash
cd /path/to/cat2bug-platform

# macOS
/Applications/Blender.app/Contents/MacOS/Blender \
  --background \
  --python assets/blender/generate_login_scene.py

# 若 blender 已在 PATH
blender --background --python assets/blender/generate_login_scene.py
```

### 方式二：快捷脚本

```bash
chmod +x assets/blender/run_generate.sh
./assets/blender/run_generate.sh
```

### 方式三：Blender GUI

1. 打开 Blender → **Scripting** 工作区  
2. 打开 `assets/blender/generate_login_scene.py`  
3. **Alt+P** 运行  

运行成功后会在 `cat2bug-platform-ui/src/assets/models/` 生成：

| 文件 | 说明 |
|------|------|
| `login-scene.blend` | 可继续手工微调、烘焙、加老鼠等 |
| `login-scene.glb` | 直接给前端 Three.js 加载 |

## 场景内容

| 物体 | 说明 |
|------|------|
| **Cat** | Chibi 灰虎斑猫，趴睡于键盘，闭眼，粉内耳、白口鼻；**尾巴 5 段骨骼** + 循环 wag 动画 |
| **Laptop** | 约 110° 开合，深炭灰机身，简化键帽，屏幕背面浅灰猫爪 logo |
| **FlowerPot** | 深灰圆柱盆，单茎两叶，顶部 5 瓣白雏菊 + 黄心 |
| **Mug** | 矮圆柱马克杯 + 把手，侧面白猫爪 logo，杯口贝塞尔蒸汽曲线 |

风格：clean low-poly + 轻微 Bevel / Shade Smooth，哑光 Principled BSDF，与 2D 插画夜间金色 rim light 配色一致。

## 尾巴骨骼结构

Armature 对象名：**`Cat_Armature`**

```
Cat_Root
└── Cat_Spine
    └── Cat_Tail.001
        └── Cat_Tail.002
            └── Cat_Tail.003
                └── Cat_Tail.004
                    └── Cat_Tail.005   ← 尾尖
```

| 骨骼名 | 用途 |
|--------|------|
| `Cat_Root` | 根骨骼，位于躯干附近 |
| `Cat_Spine` | 脊柱 |
| `Cat_Tail.001` … `Cat_Tail.005` | 尾巴链，**001=靠近身体，005=尾尖** |

### 动画

- Action 名称：**`Cat_TailWag`**
- 帧范围：1–48（24fps，约 2 秒/周期）
- 已加 **Cycles** 修饰器，导出 GLB 后在 Three.js 中可循环播放

### Web 端命名约定

在 Three.js 中查找骨骼：

```javascript
const tailBones = []
for (let i = 1; i <= 5; i++) {
  const name = `Cat_Tail.${String(i).padStart(3, '0')}`
  const bone = skeleton.getBoneByName(name)
  if (bone) tailBones.push(bone)
}
```

也可通过前缀过滤：`bone.name.startsWith('Cat_Tail.')`

## 导出说明（glTF / GLB）

脚本已自动调用 `export_scene.gltf`，主要参数：

| 选项 | 值 | 说明 |
|------|-----|------|
| `export_format` | `GLB` | 单文件，适合 Web |
| `export_yup` | `true` | Y-up，与 Three.js 默认一致 |
| `export_animations` | `true` | 含 `Cat_TailWag` |
| `export_skins` | `true` | 蒙皮网格 |
| `export_def_bones` | `true` | 骨骼层级 |
| `export_lights` | `false` | 灯光由前端自行设置 |

### 手动导出（Blender GUI）

1. **File → Export → glTF 2.0 (.glb/.gltf)**  
2. 勾选：**Animation**、**Skinning**、**Shape Keys**（如有）  
3. Transform：**+Y Up**  
4. 输出至 `cat2bug-platform-ui/src/assets/models/login-scene.glb`

## Vue / Three.js 集成建议

当前 `CatIllustration.vue` 使用 2D PNG。若要切换为 3D：

### 1. 安装依赖

```bash
cd cat2bug-platform-ui
npm install three@^0.160.0
# 可选：封装组件
npm install @tresjs/core @tresjs/cjs   # Vue 2 需查兼容版本，或直接用 three 裸 API
```

Vue 2.7 项目建议 **直接使用 Three.js**（无官方 Vue 3 TresJS 时最稳）。

### 2. 最小加载示例

```javascript
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

const scene = new THREE.Scene()
const camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100)
camera.position.set(0, -0.55, 0.22)
camera.lookAt(0, 0, 0.08)

// 与 Blender 脚本一致的夜间金色 rim light
const rim = new THREE.RectAreaLight(0xfde047, 2.5, 0.8, 0.8)
rim.position.set(0.5, -0.4, 0.55)
scene.add(rim)

const loader = new GLTFLoader()
let mixer = null

loader.load('/src/assets/models/login-scene.glb', (gltf) => {
  scene.add(gltf.scene)

  if (gltf.animations.length) {
    mixer = new THREE.AnimationMixer(gltf.scene)
    const clip = gltf.animations.find(a => a.name === 'Cat_TailWag') || gltf.animations[0]
    const action = mixer.clipAction(clip)
    action.setLoop(THREE.LoopRepeat)
    action.play()
  }
})

function animate() {
  requestAnimationFrame(animate)
  if (mixer) mixer.update(clock.getDelta())
  renderer.render(scene, camera)
}
```

### 3. 程序化驱动尾巴（不用 keyframe 时）

```javascript
const tailBones = []
gltf.scene.traverse((obj) => {
  if (obj.isBone && obj.name.startsWith('Cat_Tail.')) {
    tailBones.push(obj)
  }
})
tailBones.sort((a, b) => a.name.localeCompare(b.name))

// 在 animate 循环中
const t = performance.now() * 0.001
tailBones.forEach((bone, i) => {
  const amp = 0.18 - i * 0.025
  bone.rotation.y = Math.sin(t * 3 + i * 0.8) * amp
})
```

### 4. 组件集成要点

- 容器尺寸参考现有 CSS：`380×240px`，`object-fit: contain`  
- 保留 `.celestial-body` 月亮/太阳为 **CSS 2D 层**，3D 场景只替换 `.cat-scene` 图片  
- `pointer-events: none` 保持不变  
- 暗色主题下加强 `RectAreaLight` 或 `AmbientLight`，匹配 `html.dark` 下的金色月光  

### 5. 性能

- GLB 为 low-poly，单场景 draw call 可控  
- 登录页单实例即可，无需 DRACO（若文件变大再启用 gltf-pipeline 压缩）  

## 可选扩展：底部老鼠

已有 2D 精灵资源：`src/assets/images/mouse_sprite*.png`。3D 可后续在 `generate_login_scene.py` 中增加 `build_mouse()`，或继续用现有 `MouseRunner.vue` 2D 动画，二者可并存。

## 故障排查

| 问题 | 处理 |
|------|------|
| `blender: command not found` | 使用 Blender.app 完整路径（见上文） |
| 导出 GLB 无动画 | 确认 Armature 的 Action 已赋值，重新运行脚本 |
| Three.js 中尾巴不动 | 检查 `mixer.update(delta)`；或改用程序化 rotation |
| 材质过暗 | 前端加强 rim light；或 Blender 中提高 `Light_RimMoon.energy` |

## 重新生成

修改 `generate_login_scene.py` 后重新运行即可覆盖 `.blend` / `.glb`。建议在 git 中提交生成产物，便于 CI 与设计师无需本地 Blender 即可预览。

---

**维护：** Cat2Bug Platform · 登录页 3D 管线
