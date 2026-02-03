#!/bin/bash

echo "=== Verifying HUDTracker Project Structure ==="
echo ""

errors=0

# Check required files
required_files=(
    "build.gradle"
    "settings.gradle"
    "gradle.properties"
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
)

echo "Checking required build files..."
for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ Missing: $file"
        ((errors++))
    fi
done

echo ""
echo "Checking Kotlin source files..."
kt_files=$(find app/src/main/java -name "*.kt" | wc -l)
echo "Found $kt_files Kotlin files"
if [ "$kt_files" -lt 10 ]; then
    echo "✗ Warning: Expected at least 10 Kotlin files"
    ((errors++))
else
    echo "✓ Kotlin files present"
fi

echo ""
echo "Checking XML layout files..."
xml_files=$(find app/src/main/res/layout -name "*.xml" 2>/dev/null | wc -l)
echo "Found $xml_files layout files"
if [ "$xml_files" -lt 2 ]; then
    echo "✗ Warning: Expected at least 2 layout files"
    ((errors++))
else
    echo "✓ Layout files present"
fi

echo ""
echo "Checking resource files..."
if [ -f "app/src/main/res/values/strings.xml" ]; then
    echo "✓ strings.xml"
else
    echo "✗ Missing: strings.xml"
    ((errors++))
fi

if [ -f "app/src/main/res/values/colors.xml" ]; then
    echo "✓ colors.xml"
else
    echo "✗ Missing: colors.xml"
    ((errors++))
fi

if [ -f "app/src/main/res/values/themes.xml" ]; then
    echo "✓ themes.xml"
else
    echo "✗ Missing: themes.xml"
    ((errors++))
fi

echo ""
if [ $errors -eq 0 ]; then
    echo "=== All checks passed! ==="
    exit 0
else
    echo "=== Found $errors issues ==="
    exit 1
fi
