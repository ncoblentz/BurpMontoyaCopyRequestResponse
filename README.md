# Burp CopyRequestResponse (Using the Montoya API)
__Author: Nick Coblentz__

CopyRequestResponse is a Burp Suite plugin that exposes a context-menu (right-click -> Extensions -> CopyRequest/Response) to copy one or more selected HTTP1/2 or Web Socket requests and paste it as markdown into your notes. 

## How to build this plugin
### Command-Line
```bash
$ ./gradlew jar
```
### InteliJ
1. Open the project in Intellij
2. Open the Gradle sidebar on the right hand side
3. Choose Tasks -> Build -> Jar

## How to add this plugin to Burp
1. Open Burp Suite
2. Go to Extensions -> Installed -> Add
   - Extension Type: Java
   - Extension file: build/libs/CopyRequestResponseMontoya-1.0-SNAPSHOT.jar
