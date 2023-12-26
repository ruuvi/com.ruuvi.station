#!/bin/sh

# This script is used to update the l10n files in the repository.
# It is meant to be run by the CI system.

set -e


# Clone the station localization repository if it does not exist yet
if [ ! -d "station.localization" ]; then
  git clone git@github.com:ruuvi/station.localization.git
fi

json_file="./station.localization/station.localization.json"

append_xml() {
  local filename=$1
  local ident_android=$2
  local source_string=$3

  # Replace newline characters \\n with \\\\n
  result=${source_string//\\n/\\\\n}

  # Replace single quotes with escaped single quotes
  result=${source_string//\'/\\\'}

  # Replace Unicode ampersand with escaped Unicode ampersand
  result=${result//&/\\u0026}

  # TODO: @rinat-enikeev does not know for what is it. @denisandreev please check
  # https://github.com/ruuvi/station.localization/blob/master/localize.converter.android/src/model/TranslationString.kt#L35-L41
  # and if needed implement it here
  # while [[ $result =~ \{(.+?)\^(.+?)\} ]]; do
  #     # Extracting the second group from the regex match
  #     replacement=${BASH_REMATCH[2]}
  #     # Replace the first occurrence of the pattern with the second group match
  #     result=${result/\{${BASH_REMATCH[1]}\^${BASH_REMATCH[2]}\}/$replacement}
  # done
  
  printf "    <string name=\"%s\">%s</string>\n" "$ident_android" "$result" >> "$filename"
}

# Extract translations
languages=$(jq -r '.translations[0] | keys[]' "$json_file" | grep -v 'ident_')

echo "Languages found:$languages"

# Generate XML files
for lang in $languages; do
  echo "Processing language: $lang"

  # Define file path
  if [ "$lang" = "en" ]; then
    filename="app/src/main/res/values/strings.xml"
  else
    filename="app/src/main/res/values-${lang}/strings.xml"
  fi

  # Create a new file and write the XML header
  echo '<?xml version="1.0" encoding="utf-8"?>' > "$filename"
  echo '<resources>' >> "$filename"

  rows=$(jq -r --arg lang "$lang" '.translations[] | select(.ident_android != "") | @base64' "$json_file")

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
    echo '</resources>' >> "$filename"
  fi
done

rm -r -f station.localization