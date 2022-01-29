#!/usr/bin/env bash

#
# Make a copy of the template-service.
#
# Usage: copy-template.sh service-name [package]
#
# If package is not given it is derived from the service-name
#
#

TEMPLATE_PACKAGE="net.bluewizardhat.demoapp.template"

scriptDir=$(readlink -f $(dirname "$0"))
projectDir=$(dirname "$scriptDir")
templateDir="$projectDir/template-service"


function confirm() {
	local p="$1"
	while [ -z "$exitCode" ]; do
		read -p "$p" confirm
		case "$confirm" in
			y|Y)
				return 0
			;;
			n|N)
				return 1
			;;
		esac
	done

	return $exitCode
}

# All lowercase service name
NEW_SERVICE="${1,,}"

if [ -z "$NEW_SERVICE" ]; then
	echo "Need a name for the new service"
	exit 1
fi

if [[ "$NEW_SERVICE" != *-service ]]; then
	if confirm "'$NEW_SERVICE' doesn't end in '-service', add '-service' to it? (y/n): "; then
		NEW_SERVICE="$NEW_SERVICE-service"
	fi
fi


if [ -e "$NEW_SERVICE" ]; then
	echo "'$NEW_SERVICE' already exists"
	exit 1
fi

NEW_PACKAGE="$2"
if [ -z "$NEW_PACKAGE" ]; then
	packageEnd=$(echo "${NEW_SERVICE%-service}" | sed 's|/|.|g')
	NEW_PACKAGE="${TEMPLATE_PACKAGE/template/$packageEnd}"

	if ! confirm "Use '$NEW_PACKAGE' as package name (y/n): "; then
		echo "Cancelled"
		exit 0
	fi

	echo
fi


#
echo "Creating '$NEW_SERVICE' with package '$NEW_PACKAGE' from 'template-service'"

# Uppercase first letter in each word (separated by '-')
function upperEachWord() {
	local serviceName="$1"
	local words=(${serviceName//-/ })
	local uppered=""
	for w in "${words[@]}"; do
		if [ -z "$uppered" ]; then
			uppered="${w^}"
		else
			uppered="$uppered-${w^}"
		fi
	done
	echo "$uppered"
}

PRETTY_SERVICE=$(upperEachWord "$NEW_SERVICE")

cd "$projectDir"
rsync -r "$templateDir/" "$NEW_SERVICE" --exclude=target

cd "$NEW_SERVICE"

echo
echo "Renaming modules in '$NEW_SERVICE':"
for entry in $(ls -A .); do
	if [[ "$entry" == template-service* ]]; then
		newEntry="${entry/template-service/$NEW_SERVICE}"
		echo "  * '$entry' -> '$newEntry'"
		mv "$entry" "$newEntry"
	fi
done

echo
echo "Replace in '$NEW_SERVICE' files:"
echo "  'template-service' -> '$NEW_SERVICE'"
echo "  'Template-Service' -> '$PRETTY_SERVICE'"
echo
files=$(find . -name target -prune -o -type f \( -regex ".*\.kts?" -o -name "*.java" -o -regex ".*\.ya?ml" -o -name "*.xml" -o -name "*.properties" -o -name "*.md" -o -name "Dockerfile*" \) | grep -v -e "/target$")
for file in $files; do
	if [ -f "$file" ]; then
		echo "  * Processing file $file"
		sed -i "s|template-service|$NEW_SERVICE|g;s|Template-Service|$PRETTY_SERVICE|g" "$file"
	fi
done
echo
echo "  'template' -> '${NEW_SERVICE%-service}'"
files=$(find localdev-config -type f)
for file in $files; do
	echo "  * Processing file $file"
	sed -i "s|template|${NEW_SERVICE%-service}|g" "$file"
done

# Rename packages
$scriptDir/rename-packages.sh "$TEMPLATE_PACKAGE" "$NEW_PACKAGE"

# Renaming files with 'template-service' in their name.
echo
echo "Rename in '$NEW_SERVICE' files with 'template-service' in their name:"
files=$(find . -name target -prune -o -type f -name "*template-service*" | grep -v -e "/target$")
for file in $files; do
	newName="${file/template-service/$NEW_SERVICE}"
	echo "  * $file -> $newName"
	mv "$file" "$newName"
done


cd "$projectDir"
./runLocal.sh refresh

# Add service to modules section
sed -i "s|</modules>|\t<module>$NEW_SERVICE</module>\n\t</modules>|" pom.xml
