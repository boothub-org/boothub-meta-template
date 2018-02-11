### Getting started

{{#if ghApiUsed}}
- `git clone https://github.com/{{ghProjectOwner}}/{{ghProjectId}}.git`
{{else}}
- download the generated zip file and unpack it.
{{/if}}

- customize the script files available in the `{{ghProjectId}}/skeleton/template/script` directory.
- customize the content of the `{{ghProjectId}}/skeleton/template/files` and `{{ghProjectId}}/skeleton/template/files-src` directories.
- test your template:
  - _Using your IDE_:
Create a run configuration with org.boothub.BootHubCli as main class and the folowing VM options:
`-DboothubRepoClass=org.boothub.repo.SingleSkeletonRepo -DboothubRepoPath=<your-base-directory>/{{ghProjectId}}/skeleton`

  - _Using the locally-installed BootHub-CLI_:
Set the environment variable `BOOTHUB_OPTS=-DboothubRepoClass=org.boothub.repo.SingleSkeletonRepo -DboothubRepoPath=<your-base-directory>/{{ghProjectId}}/skeleton`

See the [skeleton documentation](http://meta-template.boothub.org) for more info.
