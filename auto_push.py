import os
import time

# কয়বার change করবে
repeat = 100   # এখানে তুমি সংখ্যা change করতে পারো

file_name = "extra_file.txt"

# যদি file না থাকে, তাহলে create করবে
if not os.path.exists(file_name):
    with open(file_name, "w") as f:
        f.write("Start\n")

for i in range(1, repeat + 1):
    # File modify
    with open(file_name, "a") as f:
        f.write(f"Update number {i}\n")

    # Git commands
    os.system("git add .")
    os.system(f'git commit -m "Auto update {i}"')
    os.system("git push")

    print(f"Pushed update {i}")

    time.sleep(2)   # 2 second wait (optional)