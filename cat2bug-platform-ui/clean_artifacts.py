import numpy as np
from PIL import Image
import scipy.ndimage as ndimage
import glob

def clean_artifacts(img_path):
    img = Image.open(img_path).convert('RGBA')
    arr = np.array(img)
    alpha = arr[:, :, 3]
    
    # Opaque mask
    mask = alpha > 0
    
    # Label connected components of opaque pixels
    labeled_array, num_features = ndimage.label(mask)
    
    # Calculate sizes of each component
    sizes = ndimage.sum(mask, labeled_array, range(num_features + 1))
    
    # We want to keep large components (e.g. > 100 pixels, which correspond to the mice).
    # Small components (< 100 pixels) are likely noise/artifacts.
    # Note: Sometimes a whisker or tail tip could be disconnected? 
    # Let's see the sizes of the components first.
    small_components = [i for i, size in enumerate(sizes) if i > 0 and size < 100]
    
    if len(small_components) > 0:
        print(f"{img_path}: Found {len(small_components)} small disconnected components.")
        for i in small_components:
            # Set them to fully transparent
            arr[labeled_array == i] = [0, 0, 0, 0]
        
        out_img = Image.fromarray(arr, 'RGBA')
        out_img.save(img_path)
    else:
        print(f"{img_path}: No artifacts found.")

for p in glob.glob('cat2bug-platform-ui/src/assets/images/mouse_sprite_*.png'):
    clean_artifacts(p)
