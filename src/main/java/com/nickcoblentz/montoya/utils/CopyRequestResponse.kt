package com.nickcoblentz.montoya.utils

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Registration
import com.nickcoblentz.montoya.LogLevel
import com.nickcoblentz.montoya.MontoyaLogger


class CopyRequestResponse : BurpExtension {
    private lateinit var _api: MontoyaApi
    private lateinit var _contextItemsProviderRegistration: Registration


    override fun initialize(api: MontoyaApi) {
        _api = api
        val logger = MontoyaLogger(api, LogLevel.DEBUG)
        logger.debugLog(this.javaClass.getName(), "Plugin Starting...")
        api.extension().setName("Copy Request/Response")
        _contextItemsProviderRegistration =
            api.userInterface().registerContextMenuItemsProvider(CopyRequestResponseContextMenuProvider(api))
        logger.debugLog(this.javaClass.getName(), "Finished")
        /*
        api.logging().logToOutput(api.persistence().preferences().getString("com.nickcoblentz.montoya.explorepreferences.keyname"));
        api.persistence().preferences().stringKeys().forEach(key ->{
            api.logging().logToOutput(key);
        });

 */
    }
}