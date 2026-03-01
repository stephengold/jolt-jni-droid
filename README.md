[The jolt-jni-droid project][project] provides
sample Android applications
for [the Jolt-JNI physics library][joltjni].

It contains 2 modules/subprojects:

+ HelloJoltJni: the HelloJoltJni sample app
<img height="150" src="https://raw.githubusercontent.com/stephengold/jolt-jni-droid/refs/heads/master/HelloJoltJni/screenshot.png" alt="HelloJoltJni screenshot">
+ SmokeTestAll: the SmokeTestAll test app

Complete source code is provided under
[a 3-clause BSD license][license].


## How to build and run jolt-jni-droid from source

1. Install [Android Studio][studio],
   version 2025.1.3 or higher,
   if you don't already have it.
2. Download and extract the jolt-jni-droid source code from GitHub:
  + using [Git]: `git clone https://github.com/stephengold/jolt-jni-droid.git`
3. Run Android Studio and open the extracted project.
4. In the top bar, select the desired run/debug configuration
   ("HelloJoltJni" or "SmokeTestAll").
5. Press Shift+F10 .


[git]: https://git-scm.com "Git version-control system"
[joltjni]: https://stephengold.github.io/jolt-jni-docs "Jolt-JNI project"
[license]: https://github.com/stephengold/jolt-jni-droid/blob/master/LICENSE "jolt-jni-droid license"
[project]: https://github.com/stephengold/jolt-jni-droid "jolt-jni-droid project"
[studio]: https://developer.android.com/studio "Android-Studio IDE"
