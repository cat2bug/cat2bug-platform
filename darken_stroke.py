from PIL import Image
import numpy as np
import glob

def process_image(img_path, out_path):
    img = Image.open(img_path).convert('RGBA')
    arr = np.array(img).astype(np.float32)
    
    rgb = arr[:,:,:3]
    alpha = arr[:,:,3:4]
    
    # Calculate brightness
    b = np.mean(rgb, axis=2, keepdims=True)
    
    # Create a darkening factor. 
    # We want to darken colors that are part of the stroke. The stroke is around brightness 50-100.
    # Let's say if b < 120, we apply a curve to darken it.
    # new_b = b * 0.5 for b around 60?
    # Let's use a factor: factor = min(1.0, (b / 120.0) ** 0.5) -- wait, (b/120)^0.5 is > b/120, so it's larger.
    # We want to DARKEN. So factor should be < 1.
    # factor = b / 120.0 ?
    # If b=60, factor = 0.5. new_rgb = rgb * 0.5 (brightness becomes 30)
    # If b=110, factor = 0.91.
    # If b > 120, factor = 1.0.
    
    factor = np.clip((b / 120.0) ** 0.8, 0.4, 1.0)
    
    # We also want to only apply this where alpha > 0
    mask = alpha > 0
    
    new_rgb = rgb * factor
    
    # Combine back
    arr[:,:,:3] = new_rgb
    
    out_img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')
    out_img.save(out_path)

process_image('cat2bug-platform-ui/src/assets/images/mouse_sprite_run_bottom_left.png', 'cat2bug-platform-ui/test_darker.png')
print("Saved test_darker.png")
