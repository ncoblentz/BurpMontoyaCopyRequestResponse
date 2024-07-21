package com.nickcoblentz.montoya.utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.*;
import com.nickcoblentz.montoya.LogLevel;
import com.nickcoblentz.montoya.MontoyaLogger;

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
    private static String _CopyRequestAndResponseName = "Full Rq/Rs";
    private static String _CopyRequestAndResponseHeaderName = "Full Rq, Rs Header";
    private static String _CopyURLAndResponseName = "URL, Rs";
    private static String _CopyURLAndResponseHeaderName = "URL, Rs Header";
    private static String _CopyRequestAndResponseIncludeName = "Full Rq/Rs (incl. all)";
    private static String _CopyURLAndResponseHeaderIncludeName = "URL, Rs Header (incl. all)";
    private static String _CopyResponseBodyOnlyName = "Rs Body Only";
    private static List<String> _StripHeadersForTheseCommands = List.of(_CopyRequestAndResponseName,_CopyRequestAndResponseHeaderName,_CopyURLAndResponseName,_CopyURLAndResponseHeaderName);
    private JMenuItem _CopyRequestAndResponseJMenuItem = new JMenuItem(_CopyRequestAndResponseName);
    private JMenuItem _CopyRequestAndResponseHeaderJMenuItem = new JMenuItem(_CopyRequestAndResponseHeaderName);
    private JMenuItem _CopyURLAndResponseJMenuItem = new JMenuItem(_CopyURLAndResponseName);
    private JMenuItem _CopyURLAndResponseHeadersJMenuItem = new JMenuItem(_CopyURLAndResponseHeaderName);
    private JMenuItem _CopyRequestAndResponseIncludeJMenuItem = new JMenuItem(_CopyRequestAndResponseIncludeName);
    private JMenuItem _CopyURLAndResponseHeaderIncludeJMenuItem = new JMenuItem(_CopyURLAndResponseHeaderIncludeName);
    private JMenuItem _CopyResponseBodyOnlyJMenuItem = new JMenuItem(_CopyResponseBodyOnlyName);

    private Set<String> _requestHeadersToStrip = Set.of(
            "Sec-Ch-Ua",
            "Sec-Ch-Ua-Mobile",
            "Sec-Ch-Ua-Full-Version",
            "Sec-Ch-Ua-Arch",
            "Sec-Ch-Ua-Platform",
            "Sec-Ch-Ua-Platform-Version",
            "Sec-Ch-Ua-Model",
            "Sec-Ch-Ua-Bitness",
            "Sec-Ch-Ua-Wow64",
            "Sec-Ch-Ua-Full-Version-List",
            "Upgrade-Insecure-Requests",
            "Sec-Fetch-Site",
            "Sec-Fetch-Mode",
            "Sec-Fetch-User",
            "Sec-Fetch-Dest",
            "Accept-Language",
            "Accept-Encoding",
            "Accept",
            "Priority");

    private Set<String> _responseHeadersToStrip = Set.of(
            "Accept-Ch",
            "P3p",
            "Cache-Control",
            "Pragma",
            "Expires",
            "X-Frame-Options",
            "X-Robots-Tag",
            "X-XSS-Protection",
            "X-Content-Type-Options",
            "Content-Security-Policy",
            "Strict-Transport-Security");

    MontoyaLogger logger = new MontoyaLogger(_api, LogLevel.DEBUG);

    public CopyRequestResponseContextMenuProvider(MontoyaApi api)
    {
        _api=api;
        _CopyRequestAndResponseHeaderJMenuItem.addActionListener(this);
        _CopyRequestAndResponseJMenuItem.addActionListener(this);
        _CopyURLAndResponseJMenuItem.addActionListener(this);
        _CopyURLAndResponseHeadersJMenuItem.addActionListener(this);
        _CopyRequestAndResponseIncludeJMenuItem.addActionListener(this);
        _CopyURLAndResponseHeaderIncludeJMenuItem.addActionListener(this);
        _CopyResponseBodyOnlyJMenuItem.addActionListener(this);
        _MenuItemList = new ArrayList<Component>();
        _MenuItemList.add(_CopyRequestAndResponseJMenuItem);
        _MenuItemList.add(_CopyRequestAndResponseHeaderJMenuItem);
        _MenuItemList.add(_CopyURLAndResponseJMenuItem);
        _MenuItemList.add(_CopyURLAndResponseHeadersJMenuItem);
        _MenuItemList.add(_CopyRequestAndResponseIncludeJMenuItem);
        _MenuItemList.add(_CopyURLAndResponseHeaderIncludeJMenuItem);
        _MenuItemList.add(_CopyResponseBodyOnlyJMenuItem);
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
                    copyMe.append(String.format("**%s", wsMessage.upgradeRequest().url()));
                    copyMe.append(String.format(" (%s)**\n\n", wsMessage.direction().toString()));
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
                    if(!e.getActionCommand().equals(_CopyResponseBodyOnlyName))
                        copyMe.append(String.format("**%s**\n\n", requestResponse.request().url()));
                    if(e.getActionCommand().equals(this._CopyRequestAndResponseHeaderName) || e.getActionCommand().equals(this._CopyRequestAndResponseName) || e.getActionCommand().equals(this._CopyRequestAndResponseIncludeName)) {
                        HttpRequest strippedRequest = requestResponse.request();
                        if(stripHeaders(e.getActionCommand())) {
                            for (String headerName : _requestHeadersToStrip) {
                                while (strippedRequest.hasHeader(headerName)) {
                                    strippedRequest = strippedRequest.withRemovedHeader(headerName);
                                }
                            }
                        }
                        copyMe.append(this.surroundWithMarkdown(_api.utilities().byteUtils().convertToString(strippedRequest.toByteArray().getBytes())));
                    }
                }

                if (requestResponse.response() != null) {

                    HttpResponse strippedResponse = requestResponse.response();
                    if(stripHeaders(e.getActionCommand())) {
                        for (String headerName : _responseHeadersToStrip) {

                            while (strippedResponse.hasHeader(headerName)) {
                                strippedResponse = strippedResponse.withRemovedHeader(headerName);
                            }
                        }
                    }

                    if(e.getActionCommand().equals(_CopyResponseBodyOnlyName)) {

                        copyMe.append(this.surroundWithMarkdown(_api.utilities().byteUtils().convertToString(strippedResponse.body().getBytes())));
                    }
                    else if (e.getActionCommand().equals(this._CopyRequestAndResponseName) || e.getActionCommand().equals(this._CopyURLAndResponseName) || e.getActionCommand().equals(_CopyRequestAndResponseIncludeName)) {
                        copyMe.append(this.surroundWithMarkdown(_api.utilities().byteUtils().convertToString(strippedResponse.toByteArray().getBytes())));
                    } else {
                        byte[] responseHeaders = Arrays.copyOfRange(strippedResponse.toByteArray().getBytes(), 0, strippedResponse.bodyOffset());
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

    private boolean stripHeaders(String command)
    {
        return _StripHeadersForTheseCommands.contains(command);
    }
}
