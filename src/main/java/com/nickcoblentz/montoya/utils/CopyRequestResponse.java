package com.nickcoblentz.montoya.utils;


import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import com.nickcoblentz.montoya.MontoyaLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CopyRequestResponse implements BurpExtension {
    private MontoyaApi _api;
    private Registration _contextItemsProviderRegistration;



    @Override
    public void initialize(MontoyaApi api)
    {
        _api = api;
        MontoyaLogger logger = new MontoyaLogger(api,MontoyaLogger.DebugLogLevel);
        logger.debugLog(this.getClass().getName(),"Plugin Starting...");
        api.extension().setName("Copy Request/Response");
        _contextItemsProviderRegistration = api.userInterface().registerContextMenuItemsProvider(new CopyRequestResponseContextMenuProvider(api));
        logger.debugLog(this.getClass().getName(),"Finished");
/*
        api.logging().logToOutput(api.persistence().preferences().getString("com.nickcoblentz.montoya.explorepreferences.keyname"));
        api.persistence().preferences().stringKeys().forEach(key ->{
            api.logging().logToOutput(key);
        });

 */
    }



}
