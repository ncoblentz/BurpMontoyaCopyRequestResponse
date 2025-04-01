package com.nickcoblentz.montoya.utils

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.ui.hotkey.HotKeyContext
import com.nickcoblentz.montoya.LogLevel
import com.nickcoblentz.montoya.MontoyaLogger


class CopyRequestResponse : BurpExtension {
    private lateinit var _api: MontoyaApi
    private lateinit var _copyHandler: CopyRequestResponseHandler



    override fun initialize(api: MontoyaApi) {
        _api = api
        val logger = MontoyaLogger(api, LogLevel.DEBUG)
        logger.debugLog(this.javaClass.getName(), "Plugin Starting...")
        api.extension().setName("Copy Request/Response")
        _copyHandler = CopyRequestResponseHandler(_api)
        api.userInterface().registerContextMenuItemsProvider(CopyRequestResponseContextMenuProvider(api,_copyHandler))
        api.userInterface().registerHotKeyHandler(
            HotKeyContext.HTTP_MESSAGE_EDITOR,
            "Ctrl+Shift+C",
            CopyRequestResponseHotKeyProvider(api,true,CopyRequestResponseHandler.CopyMode.RequestFullResponseFull))
        api.userInterface().registerHotKeyHandler(
            HotKeyContext.HTTP_MESSAGE_EDITOR,
            "Ctrl+Alt+C",
            CopyRequestResponseHotKeyProvider(api,true,CopyRequestResponseHandler.CopyMode.RequestFullResponseHeaders))
        /*api.userInterface().registerHotKeyHandler(
            HotKeyContext.HTTP_MESSAGE_EDITOR,
            "",
            CopyRequestResponseHotKeyProvider(api,true,CopyRequestResponseHandler.CopyMode.URLResponseFull))
        api.userInterface().registerHotKeyHandler(
            HotKeyContext.HTTP_MESSAGE_EDITOR,
            "",
            CopyRequestResponseHotKeyProvider(api,true,CopyRequestResponseHandler.CopyMode.URLResponseHeaders))
        api.userInterface().registerHotKeyHandler(
            HotKeyContext.HTTP_MESSAGE_EDITOR,
            "",
            CopyRequestResponseHotKeyProvider(api,false,CopyRequestResponseHandler.CopyMode.ResponseBody))*/
        logger.debugLog(this.javaClass.getName(), "Finished")
        /*
        api.logging().logToOutput(api.persistence().preferences().getString("com.nickcoblentz.montoya.explorepreferences.keyname"));
        api.persistence().preferences().stringKeys().forEach(key ->{
            api.logging().logToOutput(key);
        });

 */
    }
}