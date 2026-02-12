#!/bin/bash

echo "========================================"
echo "   GGUF SURGEON - COMPLETE DIAGNOSTIC  "
echo "========================================"
echo "Started: $(date)"
echo ""

LOG_FILE="diagnostic_log_$(date +%Y%m%d_%H%M%S).txt"
exec 2>&1 | tee "$LOG_FILE"

echo "🔍 SYSTEM INFORMATION"
echo "----------------------------------------"
echo "Device: $(uname -a)"
echo "Architecture: $(uname -m)"
echo "Android: $(getprop ro.build.version.release 2>/dev/null || echo 'Not Android')"
echo "Termux: $(pkg list-installed 2>/dev/null | head -1 || echo 'Not Termux')"
echo ""

echo "🔍 LLAMA.CPP COMPILATION STATUS"
echo "----------------------------------------"
if [ -f ~/llama.cpp-b8012/tools/quantize/llama-quantize ]; then
    echo "✅ QUANTIZE BINARY: FOUND"
    file ~/llama.cpp-b8012/tools/quantize/llama-quantize
    ls -la ~/llama.cpp-b8012/tools/quantize/llama-quantize
else
    echo "❌ QUANTIZE BINARY: NOT FOUND"
    echo "   Location searched: ~/llama.cpp-b8012/tools/quantize/llama-quantize"
fi

if [ -f ~/llama.cpp-b8012/libllama.so ]; then
    echo "✅ libllama.so: FOUND"
elif [ -f ~/llama.cpp-b8012/build/libllama.so ]; then
    echo "✅ libllama.so: FOUND in build/"
else
    echo "❌ libllama.so: NOT FOUND"
fi
echo ""

echo "🔍 PROJECT STRUCTURE"
echo "----------------------------------------"
cd ~/GGUF-Surgeon-main 2>/dev/null || { echo "❌ Project directory not found!"; exit 1; }
echo "✅ Project root: $(pwd)"

echo ""
echo "📁 CRITICAL FILES CHECK"
echo "----------------------------------------"

# AndroidManifest.xml
if [ -f app/src/main/AndroidManifest.xml ]; then
    echo "✅ AndroidManifest.xml: FOUND"
else
    echo "❌ AndroidManifest.xml: MISSING"
fi

# build.gradle.kts
if [ -f app/build.gradle.kts ]; then
    echo "✅ app/build.gradle.kts: FOUND"
else
    echo "❌ app/build.gradle.kts: MISSING"
fi

echo ""
echo "🐍 PYTHON GGUF PACKAGE"
echo "----------------------------------------"
if [ -d app/src/main/assets/python/gguf ]; then
    echo "✅ Python GGUF package: FOUND"
    echo "   Files: $(find app/src/main/assets/python/gguf -name "*.py" | wc -l) .py files"
else
    echo "❌ Python GGUF package: MISSING"
fi

if [ -f app/src/main/assets/python/gguf_android_bridge.py ]; then
    echo "✅ gguf_android_bridge.py: FOUND"
else
    echo "❌ gguf_android_bridge.py: MISSING"
fi

echo ""
echo "📱 ANDROID JAVA SOURCES"
echo "----------------------------------------"
# Core files
for file in \
    app/src/main/java/com/ggufsurgeon/core/python/PythonGgufBridge.kt \
    app/src/main/java/com/ggufsurgeon/core/GgufBinaryEditor.kt \
    app/src/main/java/com/ggufsurgeon/core/GgufValidator.kt \
    app/src/main/java/com/ggufsurgeon/data/ModelRepository.kt \
    app/src/main/java/com/ggufsurgeon/di/AppModule.kt \
    app/src/main/java/com/ggufsurgeon/ui/screens/MainViewModel.kt
do
    if [ -f "$file" ]; then
        echo "✅ $(basename $file): FOUND"
    else
        echo "❌ $(basename $file): MISSING"
    fi
done

echo ""
echo "🔴 NATIVE LIBRARIES (CRITICAL)"
echo "----------------------------------------"
if [ -d app/src/main/jniLibs ]; then
    echo "📁 jniLibs directory: EXISTS"
    find app/src/main/jniLibs -type f -name "*.so" 2>/dev/null | while read so; do
        echo "   📦 $(basename $so): $(file $so | grep -o 'ELF.*' || echo 'Not ELF')"
    done
else
    echo "❌ jniLibs directory: MISSING"
    echo "   ⚠️  This is why NativeGgufParser will crash!"
fi

echo ""
echo "🔍 MODEL REPOSITORY ANALYSIS"
echo "----------------------------------------"
if [ -f app/src/main/java/com/ggufsurgeon/data/ModelRepository.kt ]; then
    echo "📄 ModelRepository.kt contents:"
    echo "----------------------------------------"
    # Check which parser is being used
    if grep -q "NativeGgufParser" app/src/main/java/com/ggufsurgeon/data/ModelRepository.kt; then
        echo "⚠️  WARNING: Using NativeGgufParser - will CRASH without native libs!"
        grep -n "NativeGgufParser" app/src/main/java/com/ggufsurgeon/data/ModelRepository.kt | head -3
    fi
    if grep -q "PythonGgufBridge" app/src/main/java/com/ggufsurgeon/data/ModelRepository.kt; then
        echo "✅ Using PythonGgufBridge - SAFE!"
    fi
fi

echo ""
echo "🔍 APP MODULE ANALYSIS"
echo "----------------------------------------"
if [ -f app/src/main/java/com/ggufsurgeon/di/AppModule.kt ]; then
    if grep -q "providePythonBridge" app/src/main/java/com/ggufsurgeon/di/AppModule.kt; then
        echo "✅ PythonGgufBridge provider: FOUND"
    else
        echo "❌ PythonGgufBridge provider: MISSING"
    fi
fi

echo ""
echo "🔍 QUANTIZATION STATUS"
echo "----------------------------------------"
if [ -f app/src/main/assets/llama-quantize ]; then
    echo "✅ llama-quantize binary: FOUND in assets/"
    file app/src/main/assets/llama-quantize
else
    echo "❌ llama-quantize binary: NOT FOUND in assets/"
    echo "   Run: cp ~/llama.cpp-b8012/tools/quantize/llama-quantize app/src/main/assets/"
fi

echo ""
echo "🔍 SIMULATION/FAKE FILES"
echo "----------------------------------------"
fake_files=0
for file in \
    app/src/main/java/com/ggufsurgeon/core/GgufEditor.kt \
    app/src/main/java/com/ggufsurgeon/core/GgufParser.kt \
    app/src/main/java/com/ggufsurgeon/core/ModelOperations.kt
do
    if [ -f "$file" ]; then
        echo "❌ FAKE FILE FOUND: $file"
        fake_files=$((fake_files+1))
    fi
done
if [ $fake_files -eq 0 ]; then
    echo "✅ No simulation files found - CLEAN!"
fi

echo ""
echo "🔍 TEST DIRECTORIES"
echo "----------------------------------------"
if [ -d app/src/test ]; then
    echo "⚠️  test/ directory exists - NOT NEEDED"
else
    echo "✅ test/ directory: CLEAN"
fi
if [ -d app/src/androidTest ]; then
    echo "⚠️  androidTest/ directory exists - NOT NEEDED"
else
    echo "✅ androidTest/ directory: CLEAN"
fi

echo ""
echo "🔍 CMAKE/CPP DIRECTORIES"
echo "----------------------------------------"
if [ -d app/src/main/cpp ]; then
    echo "⚠️  cpp/ directory exists - NOT NEEDED for Python path"
else
    echo "✅ cpp/ directory: CLEAN"
fi

echo ""
echo "🔍 GRADLE BUILD CONFIGURATION"
echo "----------------------------------------"
if [ -f app/build.gradle.kts ]; then
    if grep -q "cmake" app/build.gradle.kts; then
        echo "⚠️  CMake configuration found in build.gradle.kts"
        grep -n "cmake\|externalNativeBuild" app/build.gradle.kts | head -5
    else
        echo "✅ No CMake configuration - GOOD!"
    fi
fi

echo ""
echo "========================================"
echo "🏁 DIAGNOSTIC COMPLETE"
echo "========================================"
echo "Log saved to: $LOG_FILE"
echo ""
echo "📋 SUMMARY:"
echo "----------------------------------------"

# Final summary
if [ -f app/src/main/assets/llama-quantize ]; then
    echo "✅ QUANTIZATION: READY (binary present)"
else
    echo "❌ QUANTIZATION: NOT READY (missing binary)"
fi

if [ -d app/src/main/jniLibs ] && [ "$(ls -A app/src/main/jniLibs 2>/dev/null)" ]; then
    echo "✅ NATIVE LIBS: PRESENT"
else
    echo "❌ NATIVE LIBS: MISSING - NativeGgufParser will CRASH!"
fi

if grep -q "PythonGgufBridge" app/src/main/java/com/ggufsurgeon/data/ModelRepository.kt 2>/dev/null; then
    echo "✅ PYTHON PATH: ACTIVE - Safe path ready!"
else
    echo "⚠️  PYTHON PATH: NOT ACTIVE - Using native code that may crash"
fi

echo ""
echo "▶️  NEXT STEPS:"
if [ ! -f app/src/main/assets/llama-quantize ]; then
    echo "  1. Copy quantize binary: cp ~/llama.cpp-b8012/tools/quantize/llama-quantize app/src/main/assets/"
fi
if [ ! -d app/src/main/jniLibs ] || [ ! "$(ls -A app/src/main/jniLibs 2>/dev/null)" ]; then
    echo "  2. Switch to Python-only path (recommended) OR compile native libraries"
fi
if ! grep -q "PythonGgufBridge" app/src/main/java/com/ggufsurgeon/data/ModelRepository.kt 2>/dev/null; then
    echo "  3. Update ModelRepository.kt to use PythonGgufBridge instead of Native parsers"
fi

echo ""
echo "========================================"
