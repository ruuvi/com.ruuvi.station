#!/bin/bash

# Read the current version name and code from build.gradle
current_version_name=$(grep -oP 'versionName "\K\d+\.\d+\.\d+' app/build.gradle)
current_version_code=$(grep -oP 'versionCode \d+' app/build.gradle | grep -oP '\d+')

# Fetch all remote branch tips to detect higher version codes already deployed from other branches
git fetch origin --depth 1 '+refs/heads/*:refs/remotes/origin/*' 2>/dev/null || true

# Find the highest version code across all remote branches to avoid conflicts
highest_version_code=$current_version_code
highest_version_name=$current_version_name

for remote_ref in $(git for-each-ref --format='%(refname:short)' 'refs/remotes/origin/' 2>/dev/null | grep -v 'HEAD'); do
    remote_code=$(git show "${remote_ref}:app/build.gradle" 2>/dev/null | grep -oP 'versionCode \d+' | grep -oP '\d+') || continue
    if [[ -n "$remote_code" ]] && (( remote_code > highest_version_code )); then
        highest_version_code=$remote_code
        remote_name=$(git show "${remote_ref}:app/build.gradle" 2>/dev/null | grep -oP 'versionName "\K[0-9.]+')
        if [[ -n "$remote_name" ]]; then
            highest_version_name=$remote_name
        fi
    fi
done

# Extract major, minor, and patch version numbers from the highest known version
major_version=$(echo $highest_version_name | cut -d'.' -f1)
minor_version=$(echo $highest_version_name | cut -d'.' -f2)
patch_version=$(echo $highest_version_name | cut -d'.' -f3)

# Increment the patch version and version code from the highest base
((patch_version++))
incremented_version_name="$major_version.$minor_version.$patch_version"
incremented_version_code=$((highest_version_code + 1))

# Update the version name in build.gradle
sed -i "s/versionName \"$current_version_name\"/versionName \"$incremented_version_name\"/" app/build.gradle

# Update the version code in build.gradle
sed -i "s/versionCode $current_version_code/versionCode $incremented_version_code/" app/build.gradle

echo "Incremented version name: $incremented_version_name"
echo "Incremented version code: $incremented_version_code"

git config --local user.email "action@github.com"
git config --local user.name "GitHub Action"
git add app/build.gradle
git commit -m "Increment version and version code"
git push
