import numpy as np
from PIL import Image
import scipy.ndimage as ndimage
import glob

def process_image(img_path):
    print(f"Processing {img_path}...")
    img = Image.open(img_path).convert('RGBA')
    arr = np.array(img).astype(np.float32)
    alpha = arr[:, :, 3]

    # 1. Fill SMALL internal transparent pixels (eyes)
    mask = alpha == 0
    labeled_array, num_features = ndimage.label(mask)
    sizes = ndimage.sum(mask, labeled_array, range(num_features + 1))

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
    print(f"  Filled {holes_filled} internal transparent pixels.")

    # Update alpha after filling
    alpha = arr[:, :, 3]

    # 2. Remove small isolated opaque components (artifacts / black blocks)
    opaque_mask = alpha > 0
    labeled_opaque, num_opaque = ndimage.label(opaque_mask)
    opaque_sizes = ndimage.sum(opaque_mask, labeled_opaque, range(num_opaque + 1))
    
    removed_components = 0
    for i in range(1, num_opaque + 1):
        # Remove small isolated blocks that look like noise or dirt.
        # But wait! A tail tip or paw could be disconnected and small?
        # The black blocks were very small, e.g. < 20.
        if opaque_sizes[i] < 20:
            arr[labeled_opaque == i] = [0, 0, 0, 0]
            removed_components += 1
    print(f"  Removed {removed_components} isolated artifact components (black blocks).")

    # Update alpha after removing
    alpha = arr[:, :, 3]
    alpha_mask = alpha > 0

    # 3. Fix white halo on the border
    shifted_up = np.roll(alpha == 0, 1, axis=0); shifted_up[0,:] = False
    shifted_down = np.roll(alpha == 0, -1, axis=0); shifted_down[-1,:] = False
    shifted_left = np.roll(alpha == 0, 1, axis=1); shifted_left[:,0] = False
    shifted_right = np.roll(alpha == 0, -1, axis=1); shifted_right[:,-1] = False

    transparent_neighbors = shifted_up | shifted_down | shifted_left | shifted_right
    edge_mask = alpha_mask & transparent_neighbors

    brightness = np.mean(arr[:, :, :3], axis=2)

    # Change bright edge pixels to a dark gray stroke color [45, 45, 45] to remove the white edge.
    white_halo_mask = edge_mask & (brightness > 140)
    arr[white_halo_mask, :3] = [45, 45, 45]
    print(f"  Fixed {np.sum(white_halo_mask)} white edge pixels.")

    # 4. Darken the stroke
    # After fixing the edges, we darken all pixels that represent the stroke (brightness < 120).
    b = np.mean(arr[:, :, :3], axis=2)
    factor = np.ones_like(b)
    mask_dark = b < 90
    mask_mid = (b >= 90) & (b < 120)
    
    factor[mask_dark] = 0.45
    factor[mask_mid] = 0.45 + 0.55 * ((b[mask_mid] - 90) / 30.0)
    
    arr[:, :, :3] = arr[:, :, :3] * factor[:, :, np.newaxis]

    out_img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')
    out_img.save(img_path)

if __name__ == "__main__":
    for img_path in glob.glob('cat2bug-platform-ui/src/assets/images/mouse_sprite_*.png'):
        process_image(img_path)
