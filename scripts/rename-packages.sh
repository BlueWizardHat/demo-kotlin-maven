#!/usr/bin/env bash

#
# Rename package names in the current directory.
#
# Usage: rename-packages.sh [original-package] [new-package]
#     or rename-packages.sh [new-package]
#
# If only new-package if given then original-package is assumed
# to be "net.bluewizardhat.demoapp.template". If neither original
# nor new-package are given then the script will prompt for them.
#
# The script always operates in the current directory.
#

TEMPLATE_PACKAGE="net.bluewizardhat.demoapp.template"

if [[ $# == 0 ]]; then
	read -p "Orig package (enter for $TEMPLATE_PACKAGE): " ORIG_PACKAGE
	if [ -z "$ORIG_PACKAGE" ]; then
		ORIG_PACKAGE="$TEMPLATE_PACKAGE"
	fi

	read -p "New package: " NEW_PACKAGE
	if [ -z "$NEW_PACKAGE" ]; then
		echo "Nothing for new package"
		exit 0
	fi

	read -p "Rename '$ORIG_PACKAGE' to '$NEW_PACKAGE' in '$PWD' (Y/n): " confirm
	if [ "$confirm" != "Y" ]; then
		echo "Cancelled"
		exit 0
	fi
elif [[ $# == 1 ]]; then
	ORIG_PACKAGE="$TEMPLATE_PACKAGE"
	NEW_PACKAGE="$1"
elif [[ $# > 1 ]]; then
	ORIG_PACKAGE="$1"
	NEW_PACKAGE="$2"
fi

echo
echo "Renaming '$ORIG_PACKAGE' to '$NEW_PACKAGE' in '$PWD'"


ORIG_DIR=$(echo "$ORIG_PACKAGE" | sed 's|\.|/|g')
NEW_DIR=$(echo "$NEW_PACKAGE" | sed 's|\.|/|g')

# Find the source directories to operate in
dirs=""
for dir in $(find . -name src); do
	for srcDir in main/java test/java main/kotlin test/kotlin main/resources test/resources; do
		if [ -d "$dir/$srcDir" ]; then
			dirs="$dirs $dir/$srcDir"
		fi
	done
done

# Move files around
ROOT="$PWD"
for dir in $dirs; do
	cd "$ROOT"
	cd "$dir"
	echo -e "\nProcessing directory $PWD"
	if [ -d "$ORIG_DIR" ]; then
		mkdir -p "$NEW_DIR"

		# Move entries
		for entry in $(ls -A "$ORIG_DIR"); do
			echo "  * Moving $ORIG_DIR/$entry to $NEW_DIR/"
			mv "$ORIG_DIR/$entry" "$NEW_DIR/"
		done

		# Remove old dir
		rmdir --ignore-fail-on-non-empty -p "$ORIG_DIR"
	fi

done

echo

# Replace package in files
ORIG_PACKAGE_EXPR=$(echo "$ORIG_PACKAGE" | sed 's|\.|\\\.|g')
cd "$ROOT"
echo "Replace in files:"
echo "  '$ORIG_PACKAGE' -> '$NEW_PACKAGE'"
echo "  '$ORIG_DIR' -> '$NEW_DIR'"
echo
files=$(find . -name target -prune -o -type f \( -regex ".*\.kts?" -o -name "*.java" -o -regex ".*\.ya?ml" -o -name "*.xml" -o -name "*.properties" \) | grep -v -e "/target$")
for file in $files; do
	if [ -f "$file" ]; then
		echo "  * Processing file $file"
		sed -i "s|$ORIG_DIR|$NEW_DIR|g;s|$ORIG_PACKAGE_EXPR|$NEW_PACKAGE|g" "$file"
	fi
done
