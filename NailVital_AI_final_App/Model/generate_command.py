import os
import sys

root = r'C:\Users\gumma\.gradle\wrapper\dists'
matches = []
for r, d, files in os.walk(root):
    for f in files:
        if f == 'gradle.bat' and '8.9-bin' in r:
            matches.append(os.path.join(r, f))

if matches:
    cmd = f'& "{matches[0]}" wrapper --gradle-version 8.2'
    with open('c:/projects/NailVital_AI/final_ps_command.txt', 'w', encoding='utf-8') as f:
        f.write(cmd)
    print("Success")
else:
    print("No matches found")
