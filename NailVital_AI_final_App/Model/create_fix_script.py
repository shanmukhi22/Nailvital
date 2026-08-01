import os
import subprocess

def find_gradle():
    root = r'C:\Users\gumma\.gradle\wrapper\dists'
    for r, d, files in os.walk(root):
        for f in files:
            if f == 'gradle.bat':
                # Return the first 8.x or 9.x version found
                if 'gradle-8' in r or 'gradle-9' in r:
                    return os.path.join(r, f)
    return None

gradle_path = find_gradle()
if gradle_path:
    with open('c:/projects/NailVital_AI/nailvital-ai-android/fix_gradle.bat', 'w') as f:
        f.write(f'"{gradle_path}" wrapper --gradle-version 8.2\n')
        f.write('pause\n')
    print(f"SUCCESS: Created fix_gradle.bat at {gradle_path}")
else:
    print("ERROR: Could not find any local Gradle installation.")
