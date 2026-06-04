import numpy as np
from PIL import Image
import scipy.ndimage as ndimage

img_path = 'cat2bug-platform-ui/src/assets/images/mouse_sprite_sit_v3.png'
img = Image.open(img_path).convert('RGBA')
arr = np.array(img).astype(np.float32)
alpha = arr[:, :, 3]

# 1. Fill internal transparent pixels (eyes)
mask = alpha == 0
labeled_array, num_features = ndimage.label(mask)

# Find background component (usually component 1, which touches the border 0,0)
bg_label = labeled_array[0, 0]

for i in range(1, num_features + 1):
    if i != bg_label:
        region_mask = (labeled_array == i)
        # Any transparent region not connected to background is an internal hole (e.g. eye highlight)
        # Let's fill them with white
        arr[region_mask, :3] = 255
        arr[region_mask, 3] = 255

# 2. Fix pixel white borders
# Any edge pixel (alpha > 0 and alpha < 255 OR alpha > 0 adjacent to alpha=0)
alpha_mask = arr[:, :, 3] > 0
shifted_up = np.roll(arr[:,:,3] == 0, 1, axis=0); shifted_up[0,:] = False
shifted_down = np.roll(arr[:,:,3] == 0, -1, axis=0); shifted_down[-1,:] = False
shifted_left = np.roll(arr[:,:,3] == 0, 1, axis=1); shifted_left[:,0] = False
shifted_right = np.roll(arr[:,:,3] == 0, -1, axis=1); shifted_right[:,-1] = False

transparent_neighbors = shifted_up | shifted_down | shifted_left | shifted_right
edge_mask = alpha_mask & transparent_neighbors

brightness = np.mean(arr[:, :, :3], axis=2)

# If an edge pixel is bright (e.g. > 100), it might be a white artifact from anti-aliasing
white_halo_mask = edge_mask & (brightness > 120)

# Make these white border pixels transparent to remove the white edge
arr[white_halo_mask, 3] = 0

out_img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')
out_img.save('cat2bug-platform-ui/src/assets/images/mouse_sprite_sit_v3.png')
print("Fixed mouse_sprite_sit_v3.png")
