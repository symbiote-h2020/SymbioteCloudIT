This is the repository with integartion tests for SymbIoTe Cloud.

It tests:
- getting guest token from core
- registering/unregistering/updating default (dummy) resources in RAP
- accessing default (dummy) resources in the RAP

If you want to test it in your platfrom you have to do following:
- in `src/test/resources/application.properties` change:
    - core parameters (now it is using open environment: `https://symbiote-open.man.poznan.pl`)
    - cloud parameters to your platform parameters
- in getting configuration of your platfrom ([documentation section 1.1.3](https://github.com/symbiote-h2020/SymbioteCloud/wiki/1.1-Register-user-and-configure-platform-in-symbIoTe-Core#113-getting-all-configuration-files-in-one-zip-optional)) choose that you want to use build in plig-in provided by rap.
- run unit tests in this rpository

