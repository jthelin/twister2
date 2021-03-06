---
id: macos
title: MacOS
sidebar_label: MacOS
---

## Prerequisites

Twister2 build needs several software installed on your system.

1. Operating System
   * Twister2 is tested and known to work on,
     * MacOS High Sierra (10.13.6)

2. Java
   * Download Oracle JDK 8 from [http://www.oracle.com/technetwork/java/javase/downloads/index.html](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
   * Install Oracle JDK 8 using jdk-8uxxx-macosx-x64.dmg
   * Set the following environment variables.

      ```bash
     JAVA_HOME=$(/usr/libexec/java_home)
     export JAVA_HOME
     ```
 
You can check weather Java is installed correctly

```java
which java
``` 
     
3. Install Homebrew
   
```bash
   /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```


4. Installing maven :

```bash
  brew install maven
```

5. Install bazel 1.1.0

   ```bash
      wget https://github.com/bazelbuild/bazel/releases/download/1.1.0/bazel-1.1.0-installer-darwin-x86_64.sh
      chmod +x bazel-1.1.0-installer-darwin-x86_64.sh
      ./bazel-1.1.0-installer-darwin-x86_64.sh --user
   ```

   Make sure to add the bazel bin to PATH. You can add the following line to ```~/.bash_profile``` file.

   ```text
   export PATH="$PATH:$HOME/bin"
   ```
   
 Now you check with the following command to check weather bazel is install.
 
 ```bash
 souce ~/.bash_profile 
 
 bazel version
 ```  
 
 It will give the following output
 
 ```bash
 Build label: 1.1.0
 Build target: bazel-out/k8-opt/bin/src/main/java/com/google/devtools/build/lib/bazel/BazelServer_deploy.jar
 Build time: Fri Jul 19 15:19:51 2019 (1563549591)
 Build timestamp: 1563549591
 Build timestamp as int: 1563549591
 ```
 
 6. Install python3
 
 ```
 brew install python3
 ```
 
 Make sure python3 is installed
 
 ```java
python3 --version
```
 

Okay now you are ready to compile Twister2.

## Compiling Twister2 

Now lets get a clone of the source code.

```bash
git clone https://github.com/DSC-SPIDAL/twister2.git
```

You can compile the Twister2 distribution by using the bazel target as below.

```bash
cd twister2
bazel build --config=darwin scripts/package:tarpkgs --action_env=JAVA_HOME
```

This will build twister2 distribution in the file

```bash
bazel-bin/scripts/package/twister2-0.4.0.tar.gz
```

If you would like to compile the twister2 without building the distribution packages use the command

```bash
bazel build --config=darwin twister2/... --action_env=JAVA_HOME
```

For compiling a specific target such as communications

```bash
bazel build --config=darwin twister2/comms/src/java:comms-java --action_env=JAVA_HOME
```

## Twister2 Distribution

After you've build the Twister2 distribution, you can extract it and use it to submit jobs.

```bash
cd bazel-bin/scripts/package/
tar -xvf twister2-0.4.0.tar.gz
```