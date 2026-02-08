package dev.belikhun.lunamanhunt.service.integration;

import org.mvplugins.multiverse.core.MultiverseCoreApi;

public class MultiverseService {
    private final MultiverseCoreApi coreApi;

    public MultiverseService() {
        this.coreApi = MultiverseCoreApi.get();
    }

    public MultiverseCoreApi getCoreApi() {
        return coreApi;
    }
}
