import numpy as np
from PIL import Image
import scipy.ndimage as ndimage
import glob

def fix_image(img_path):
    img = Image.open(img_path).convert('RGBA')
    arr = np.array(img).astype(np.float32)
    alpha = arr[:, :, 3]

    # 1. Fill SMALL internal transparent pixels (eyes)
    mask = alpha == 0
    labeled_array, num_features = ndimage.label(mask)
    sizes = ndimage.sum(mask, labeled_array, range(num_features + 1))

    # Also, it must be completely surrounded by non-transparent pixels.
    # To be safe, we only fill holes that are < 30 pixels in size.
    holes_filled = 0
    for i in range(1, num_features + 1):
        if sizes[i] < 30:
            region_mask = (labeled_array == i)
            # Make sure it doesn't touch the image boundary
            y_coords, x_coords = np.where(region_mask)
            if (x_coords.min() > 0 and x_coords.max() < arr.shape[1] - 1 and
                y_coords.min() > 0 and y_coords.max() < arr.shape[0] - 1):
                # Fill them with white
                arr[region_mask, :3] = 255
                arr[region_mask, 3] = 255
                holes_filled += sizes[i]

    print(f"{img_path}: Filled {holes_filled} small internal transparent pixels.")

    # 2. Fix pixel white borders
    # Re-calculate alpha since we filled holes
    alpha = arr[:, :, 3]
    alpha_mask = alpha > 0
    shifted_up = np.roll(alpha == 0, 1, axis=0); shifted_up[0,:] = False
    shifted_down = np.roll(alpha == 0, -1, axis=0); shifted_down[-1,:] = False
    shifted_left = np.roll(alpha == 0, 1, axis=1); shifted_left[:,0] = False
    shifted_right = np.roll(alpha == 0, -1, axis=1); shifted_right[:,-1] = False

    transparent_neighbors = shifted_up | shifted_down | shifted_left | shifted_right
    edge_mask = alpha_mask & transparent_neighbors

    brightness = np.mean(arr[:, :, :3], axis=2)

    # If an edge pixel is bright, it might be a white artifact from anti-aliasing
    # The user said: "边框也不要像素化" (don't pixelate the border).
    # We will change the color of these pixels to dark gray, keeping their alpha intact.
    # This removes the white halo while preserving the anti-aliased smooth edge!
    # The dark stroke color is around [30, 30, 30] to [60, 60, 60]. Let's use [45, 45, 45].
    white_halo_mask = edge_mask & (brightness > 140)
    arr[white_halo_mask, :3] = [45, 45, 45]
    
    print(f"{img_path}: Fixed {np.sum(white_halo_mask)} white edge pixels.")

    out_img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')
    out_img.save(img_path)

if __name__ == "__main__":
    for img_path in glob.glob('cat2bug-platform-ui/src/assets/images/mouse_sprite_*.png'):
        fix_image(img_path)
