#!/bin/bash

# Read the current version name from build.gradle
current_version_name=$(grep -oP 'versionName "\K\d+\.\d+\.\d+' app/build.gradle)

# Extract major, minor, and patch version numbers
major_version=$(echo $current_version_name | cut -d'.' -f1)
minor_version=$(echo $current_version_name | cut -d'.' -f2)
patch_version=$(echo $current_version_name | cut -d'.' -f3)

# Increment the patch version
((patch_version++))

# Construct the incremented version name
incremented_version_name="$major_version.$minor_version.$patch_version"

# Update the version name in build.gradle
sed -i "s/versionName \"$current_version_name\"/versionName \"$incremented_version_name\"/" app/build.gradle

# Read the current version code from build.gradle
current_version_code=$(grep -oP 'versionCode \d+' app/build.gradle | grep -oP '\d+')

# Increment the version code
incremented_version_code=$((current_version_code + 1))

# Update the version code in build.gradle
sed -i "s/versionCode $current_version_code/versionCode $incremented_version_code/" app/build.gradle

echo "Incremented version name: $incremented_version_name"
echo "Incremented version code: $incremented_version_code"

git config --local user.email "action@github.com"
git config --local user.name "GitHub Action"
git add app/build.gradle
git commit -m "Increment version and version code"
git push
