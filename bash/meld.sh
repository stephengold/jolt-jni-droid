#!/bin/bash

set -e

GitDir=~/NetBeansProjects
#GitDir="/c/Users/sgold/My Documents/NetBeansProjects"

S1="$GitDir/jolt-jni-droid/HelloJoltJni/src/main/res"
D1="$GitDir/jolt-jni-droid/SmokeTestAll/src/main/res"

S2="$GitDir/jolt-jni-droid/HelloJoltJni/src/main/java/com/github/stephengold/joltjni/droid"
D2="$GitDir/jolt-jni-droid/SmokeTestAll/src/main/java/com/github/stephengold/joltjni/droidsta"

Meld="/usr/bin/meld"
#Meld="/c/Program Files/Meld/meld"

"$Meld" --diff "$S1" "$D1" --diff "$S2" "$D2"
