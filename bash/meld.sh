#!/bin/bash

set -e

GitDir=~/NetBeansProjects
#GitDir="/c/Users/sgold/My Documents/NetBeansProjects"

L1="$GitDir/jolt-jni-droid/HelloJoltJni"
R1="$GitDir/jolt-jni-droid/SmokeTestAll"

L2="$GitDir/jolt-jni-droid/HelloJoltJni/src/main/java/com/github/stephengold/joltjni/droid"
R2="$GitDir/jolt-jni-droid/SmokeTestAll/src/main/java/com/github/stephengold/joltjni/droidsta"

L3="$GitDir/jolt-jni-droid/HelloJoltJni/src/main/java/com/github/stephengold/joltjni/droid/HelloJoltJni.java"
R3="$GitDir/LbjDroid/HelloLibbulletjme/src/main/java/com/github/stephengold/lbjdroid/HelloLibbulletjme.java"

L4="$GitDir/jolt-jni-droid/HelloJoltJni"
R4="$GitDir/LbjDroid/HelloLibbulletjme"

L5="$GitDir/jolt-jni-droid/HelloJoltJni/src/main/java/com/github/stephengold/joltjni/droid"
R5="$GitDir/LbjDroid/HelloLibbulletjme/src/main/java/com/github/stephengold/lbjdroid"

Meld="/usr/bin/meld"
#Meld="/c/Program Files/Meld/meld"

"$Meld" --diff "$L1" "$R1" --diff "$L2" "$R2" --diff "$L3" "$R3" \
        --diff "$L4" "$R4" --diff "$L5" "$R5"
