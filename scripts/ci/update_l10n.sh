#!/bin/bash

# This script is used to update the l10n files in the repository.
# It is meant to be run by the CI system.

set -e


# Clone the station localization repository if it does not exist yet
if [ ! -d "station.localization" ]; then
  git clone https://github.com/ruuvi/station.localization.git
fi

json_file="./station.localization/station.localization.json"

append_xml() {
  local filename=$1
  local ident_android=$2
  local source_string=$3
  
  result="$source_string"

  # Replace newline characters \\n with \\\\n
  result=$(echo "$source_string" | sed 's/\\\\n/\\\\\\\\n/g')

  # Replace single quotes with escaped single quotes
  result=$(echo "$result" | sed "s/'/\\\\'/g")

  # Replace Unicode ampersand with escaped Unicode ampersand
  result=$(echo "$result" | sed 's/&/\\u0026/g')

  # TODO: @rinat-enikeev does not know for what is it. @denisandreev please check
  # https://github.com/ruuvi/station.localization/blob/master/localize.converter.android/src/model/TranslationString.kt#L35-L41
  # and if needed implement it here
  regex='\{[^}]*\^([^}]*)\}'

  while [[ $result =~ $regex ]]; do
      # Extract the desired part
      extracted="${BASH_REMATCH[1]}"
      # The entire matched string is in BASH_REMATCH[0]
      full_match="${BASH_REMATCH[0]}"
      # Replace the first occurrence of the full match with the extracted part
      result="${result/$full_match/$extracted}"
  done
  
  printf "    <string name=\"%s\">%s</string>\n" "$ident_android" "$result" >> "$filename"
}

append_required_language_strings() {
  local filename=$1

  if ! grep -q 'name="language_polish"' "$filename"; then
    append_xml "$filename" "language_polish" "Polski"
  fi
}

# Extract translations. Russian is intentionally not shipped by the Android app.
languages=$(jq -r '.translations[0] | keys[]' "$json_file" | grep -v 'ident_' | grep -vx 'ru')

echo "Languages found:$languages"

# Remove generated locale directories that are no longer present in station.localization.
for dir in app/src/main/res/values-*; do
  [ -d "$dir" ] || continue
  base=$(basename "$dir")
  case "$base" in
    values-night|values-night-*|values-notnight|values-sw*|values-v*)
      continue
      ;;
  esac

  lang="${base#values-}"
  if ! echo "$languages" | grep -qx "$lang"; then
    rm -rf "$dir"
  fi
done

# Generate XML files
for lang in $languages; do
  echo "Processing language: $lang"

  # Define file path
  if [ "$lang" = "en" ]; then
    filename="app/src/main/res/values/strings.xml"
  else
    filename="app/src/main/res/values-${lang}/strings.xml"
  fi

  mkdir -p "$(dirname "$filename")"

  # Create a new file and write the XML header
  echo '<?xml version="1.0" encoding="utf-8"?>' > "$filename"
  echo '<resources>' >> "$filename"

  rows=$(jq -r --arg lang "$lang" '.translations[] | select(.ident_android != "" and .ident_android != "language_russian") | @base64' "$json_file")

  if [ -z "$rows" ]; then
    echo "No rows found for language $lang with non-empty ident_android"
  else
    echo "Rows found for language $lang with non-empty ident_android"
    for row in $rows; do
      _jq() {
       echo ${row} | base64 --decode | jq -r --arg lang "$lang" ${1}
      }

      ident=$(_jq '.ident_android')
      text=$(_jq '.[$lang]')
      if [ -z "$text" ]; then
        text=$(_jq '.en')
      fi

      append_xml "$filename" "$ident" "$text"
    done
  fi

  append_required_language_strings "$filename"
  echo '</resources>' >> "$filename"
done

rm -r -f station.localization
