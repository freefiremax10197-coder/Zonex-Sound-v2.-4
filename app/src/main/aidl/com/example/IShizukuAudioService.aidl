package com.example;

interface IShizukuAudioService {
    String getShellIdentity();
    String runAudioDiagnostic();
    String enableMicTransferRouting();
    String disableMicTransferRouting();
    String enableOptimizedMicRouting(String packageName, int appPid, boolean forceSpeaker);
    String disableOptimizedMicRouting(String packageName);
    void destroy();
}
