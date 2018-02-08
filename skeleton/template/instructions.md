### Getting started

{{#if ghApiUsed}}
```
git clone https://github.com/{{ghProjectOwner}}/{{ghProjectId}}.git
cd {{ghProjectId}}
```
{{else}}
Download the generated zip file and unpack it. In the {{ghProjectId}} directory execute:
{{/if}}

&#8226; *On Linux or Mac OS:*
```
./gradlew installDist
cd build/install/{{ghProjectId}}/bin
./{{ghProjectId}}
```

&#8226; *On Windows:*
```
gradlew installDist
cd build\install\ {{~ghProjectId}}\bin
{{ghProjectId}}
```

The following text should appear on your screen:

```
Hello from {{appMainClass}}!
```

See the [skeleton documentation](http://meta-template.boothub.org) for more info.
