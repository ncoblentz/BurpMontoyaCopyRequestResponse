package com.nickcoblentz.montoya.utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class CopyRequestResponseContextMenuProvider implements ContextMenuItemsProvider, ClipboardOwner, ActionListener {
    private final List<Component> _MenuItemList;
    private MontoyaApi _api;
    private ContextMenuEvent _Event;
    private WebSocketContextMenuEvent _WSEvent;
    private int _EventType=0;
    private static int _EventTypeHTTP=1;
    private static int _EventTypeWS=2;
    private static String _CopyRequestAndResponseName = "Full Request/Response";
    private static String _CopyRequestAndResponseHeaderName = "Request (Full), Response (Header)";
    private JMenuItem _CopyRequestAndResponseJMenuItem = new JMenuItem(_CopyRequestAndResponseName);
    private JMenuItem _CopyRequestAndResponseHeaderJMenuItem = new JMenuItem(_CopyRequestAndResponseHeaderName);

    public CopyRequestResponseContextMenuProvider(MontoyaApi api)
    {
        _api=api;
        _CopyRequestAndResponseHeaderJMenuItem.addActionListener(this);
        _CopyRequestAndResponseJMenuItem.addActionListener(this);
        _MenuItemList = new ArrayList<Component>();
        _MenuItemList.add(_CopyRequestAndResponseJMenuItem);
        _MenuItemList.add(_CopyRequestAndResponseHeaderJMenuItem);
    }
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        _Event=event;
        _EventType=_EventTypeHTTP;
        if(!event.selectedRequestResponses().isEmpty() || event.messageEditorRequestResponse().isPresent())
        {
            return _MenuItemList;
        }
        return Collections.emptyList();
    }

    @Override
    public List<Component> provideMenuItems(WebSocketContextMenuEvent event) {
        _WSEvent=event;
        _EventType=_EventTypeWS;
        if(!event.selectedWebSocketMessages().isEmpty() || event.messageEditorWebSocket().isPresent())
        {
            return _MenuItemList;
        }
        return Collections.emptyList();
    }

    @Override
    public List<Component> provideMenuItems(AuditIssueContextMenuEvent event) {
        return Collections.emptyList();
    }

    public String surroundWithMarkdown(String s) {
        return String.format("```http\n%s\n```\n\n", s.trim());
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        StringBuilder copyMe;
        if(_EventType==_EventTypeWS)
        {
            copyMe = handleWSEvent(e);
        }
        else
        {
            copyMe = handleHTTPEvent(e);
        }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = new StringSelection(copyMe.toString());
        clipboard.setContents(transferable, this);

    }

    public StringBuilder handleWSEvent(ActionEvent e)
    {
        StringBuilder copyMe = new StringBuilder();
        List<WebSocketMessage> targetWebSocketMessages;
        if(!_WSEvent.selectedWebSocketMessages().isEmpty())
        {
            targetWebSocketMessages = _WSEvent.selectedWebSocketMessages();
        }
        else if(_WSEvent.messageEditorWebSocket().isPresent())
        {
            targetWebSocketMessages = new LinkedList<WebSocketMessage>();
            targetWebSocketMessages.add(_WSEvent.messageEditorWebSocket().get().webSocketMessage());
        }
        else
        {
            return copyMe;
        }

        if(!targetWebSocketMessages.isEmpty())
        {
            for (WebSocketMessage wsMessage : targetWebSocketMessages)
            {
                if(wsMessage!=null && wsMessage.upgradeRequest()!=null)
                {
                    copyMe.append(String.format("__%s", wsMessage.upgradeRequest().url()));
                    copyMe.append(String.format(" (%s)__\n\n", wsMessage.direction().toString()));
                    copyMe.append(this.surroundWithMarkdown(_api.utilities().byteUtils().convertToString(wsMessage.payload().getBytes())));
                }
            }
        }

        return copyMe;
    }

    public StringBuilder handleHTTPEvent(ActionEvent e)
    {
        StringBuilder copyMe = new StringBuilder();
        List<HttpRequestResponse> targetRequestResponses;
        if(!_Event.selectedRequestResponses().isEmpty())
        {
            targetRequestResponses = _Event.selectedRequestResponses();
        }
        else if (_Event.messageEditorRequestResponse().isPresent())
        {
            targetRequestResponses = new LinkedList<HttpRequestResponse>();
            targetRequestResponses.add(_Event.messageEditorRequestResponse().get().requestResponse());
        }
        else
        {
            return copyMe;
        }

        if (!targetRequestResponses.isEmpty()) {
            for (HttpRequestResponse requestResponse : targetRequestResponses) {

                if (requestResponse.request() != null) {
                    copyMe.append(String.format("__%s__\n\n", requestResponse.url()));
                    copyMe.append(this.surroundWithMarkdown(_api.utilities().byteUtils().convertToString(requestResponse.request().toByteArray().getBytes())));
                }

                if (requestResponse.response() != null) {
                    if (e.getActionCommand().equals(this._CopyRequestAndResponseName)) {
                        copyMe.append(this.surroundWithMarkdown(_api.utilities().byteUtils().convertToString(requestResponse.response().toByteArray().getBytes())));
                    } else {
                        byte[] responseHeaders = Arrays.copyOfRange(requestResponse.response().toByteArray().getBytes(), 0, requestResponse.response().bodyOffset());
                        copyMe.append(this.surroundWithMarkdown(_api.utilities().byteUtils().convertToString(responseHeaders)));
                    }
                }
            }
        }

        return copyMe;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }
}
