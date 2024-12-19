package com.nickcoblentz.montoya.utils

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.ui.contextmenu.*
import com.nickcoblentz.montoya.LogLevel
import com.nickcoblentz.montoya.MontoyaLogger
import java.awt.Component
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.*
import javax.swing.JMenuItem

class CopyRequestResponseContextMenuProvider(private val _api: MontoyaApi) : ContextMenuItemsProvider, ClipboardOwner, ActionListener {
    private val _MenuItemList: List<Component>
    private var currentEvent: ContextMenuEvent? = null
    private var currentWSEvent: WebSocketContextMenuEvent? = null
    private var currentAuditEvent: AuditIssueContextMenuEvent? = null
    private var currentEventType = 0

    private val _EventTypeHTTP = 1
    private val _EventTypeWS = 2
    private val _EventTypeAudit = 3
    private val _CopyRequestAndResponseName = "Full Rq/Rs"
    private val _CopyRequestAndResponseHeaderName = "Full Rq, Rs Header"
    private val _CopyURLAndResponseName = "URL, Rs"
    private val _CopyURLAndResponseHeaderName = "URL, Rs Header"
    private val _CopyRequestAndResponseIncludeName = "Full Rq/Rs (incl. all)"
    private val _CopyURLAndResponseHeaderIncludeName = "URL, Rs Header (incl. all)"
    private val _CopyResponseBodyOnlyName = "Rs Body Only"
    private val _StripHeadersName = "Strip Headers"
    private val _StripHeadersForTheseCommands: List<String> = listOf(_CopyRequestAndResponseName,_CopyRequestAndResponseHeaderName,_CopyURLAndResponseName,_CopyURLAndResponseHeaderName)

    private val _CopyRequestAndResponseJMenuItem = JMenuItem(_CopyRequestAndResponseName)
    private val _CopyRequestAndResponseHeaderJMenuItem = JMenuItem(_CopyRequestAndResponseHeaderName)
    private val _CopyURLAndResponseJMenuItem = JMenuItem(_CopyURLAndResponseName)
    private val _CopyURLAndResponseHeadersJMenuItem = JMenuItem(_CopyURLAndResponseHeaderName)
    private val _CopyRequestAndResponseIncludeJMenuItem = JMenuItem(_CopyRequestAndResponseIncludeName)
    private val _CopyURLAndResponseHeaderIncludeJMenuItem = JMenuItem(_CopyURLAndResponseHeaderIncludeName)
    private val _CopyResponseBodyOnlyJMenuItem = JMenuItem(_CopyResponseBodyOnlyName)


    private val _requestHeadersToStrip: Set<String> = setOf<String>(
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
        "Priority"
    )

    private val _responseHeadersToStrip: Set<String> = setOf<String>(
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
        "Strict-Transport-Security",
        "Last-Modified",
        "Vary",
        "X-Ua-Compatible",
        "Report-To",
        "Nel",
        "Reporting-Endpoints",
        "Etag"
    )

    var logger: MontoyaLogger = MontoyaLogger(_api, LogLevel.DEBUG)

    init {
        _CopyRequestAndResponseHeaderJMenuItem.addActionListener(this)
        _CopyRequestAndResponseJMenuItem.addActionListener(this)
        _CopyURLAndResponseJMenuItem.addActionListener(this)
        _CopyURLAndResponseHeadersJMenuItem.addActionListener(this)
        _CopyRequestAndResponseIncludeJMenuItem.addActionListener(this)
        _CopyURLAndResponseHeaderIncludeJMenuItem.addActionListener(this)
        _CopyResponseBodyOnlyJMenuItem.addActionListener(this)
        _MenuItemList = listOf(_CopyRequestAndResponseJMenuItem,
            _CopyRequestAndResponseHeaderJMenuItem,
            _CopyURLAndResponseJMenuItem,
            _CopyURLAndResponseHeadersJMenuItem,
            _CopyRequestAndResponseIncludeJMenuItem,
            _CopyURLAndResponseHeaderIncludeJMenuItem,
            _CopyResponseBodyOnlyJMenuItem)
    }

    override fun provideMenuItems(event: ContextMenuEvent): List<Component> {
        currentEvent = event
        currentEventType = _EventTypeHTTP
        if (!event.selectedRequestResponses().isEmpty() || event.messageEditorRequestResponse().isPresent) {
            return _MenuItemList
        }
        return emptyList<Component>()
    }

    override fun provideMenuItems(event: WebSocketContextMenuEvent): List<Component> {
        currentWSEvent = event
        currentEventType = _EventTypeWS
        if (!event.selectedWebSocketMessages().isEmpty() || event.messageEditorWebSocket().isPresent) {
            return _MenuItemList
        }
        return emptyList<Component>()
    }

    override fun provideMenuItems(event: AuditIssueContextMenuEvent): List<Component> {
        currentAuditEvent = event
        currentEventType = _EventTypeAudit
        if (event.selectedIssues().isNotEmpty()) {
            return _MenuItemList
        }
        return emptyList<Component>()
    }

    fun surroundWithMarkdown(s: String): String {
        return String.format("```http\n%s\n```\n\n", s.trim { it <= ' ' })
    }


    override fun actionPerformed(e: ActionEvent) {
        val copyMe = if (currentEventType == _EventTypeWS) {
            handleWSEvent(e)
        }
        else if(currentEventType == _EventTypeHTTP) {
            handleHTTPEvent(e)
        }
        else {
            handleAuditEvent(e)
        }

        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val transferable: Transferable = StringSelection(copyMe.toString())
        clipboard.setContents(transferable, this)
    }

    fun handleWSEvent(e: ActionEvent?): StringBuilder {
        val copyMe = StringBuilder()
        val targetWebSocketMessages: MutableList<WebSocketMessage>
        if (!currentWSEvent!!.selectedWebSocketMessages().isEmpty()) {
            targetWebSocketMessages = currentWSEvent!!.selectedWebSocketMessages()
        } else if (currentWSEvent!!.messageEditorWebSocket().isPresent) {
            targetWebSocketMessages = LinkedList()
            targetWebSocketMessages.add(currentWSEvent!!.messageEditorWebSocket().get().webSocketMessage())
        } else {
            return copyMe
        }

        if (!targetWebSocketMessages.isEmpty()) {
            for (wsMessage in targetWebSocketMessages) {
                if (wsMessage?.upgradeRequest() != null) {
                    copyMe.append(String.format("**%s", wsMessage.upgradeRequest().url()))
                    copyMe.append(String.format(" (%s)**\n\n", wsMessage.direction().toString()))
                    copyMe.append(
                        this.surroundWithMarkdown(
                            _api.utilities().byteUtils().convertToString(wsMessage.payload().bytes)
                        )
                    )
                }
            }
        }

        return copyMe
    }

    fun handleAuditEvent(e: ActionEvent?): StringBuilder {
        val copyMe = StringBuilder()

        currentAuditEvent?.let { event ->
            for(selectedIssue in event.selectedIssues()) {
                selectedIssue.requestResponses()?.let {
                    requestResponses ->
                    for(requestResponse in requestResponses) {
                        copyMe.append(
                            String.format(
                                "**%s**\n\n",
                                requestResponse.request().url()
                            )
                        )
                        copyMe.append(this.surroundWithMarkdown(requestResponse.request().toString()))
                        if(requestResponse.hasResponse())
                            copyMe.append(this.surroundWithMarkdown(requestResponse.response().toString()))
                    }
                }
                /* later
                selectedIssue.collaboratorInteractions()?.let { interactions ->
                    for(interaction in interactions) {
                        interaction.
                    }
                }*/
            }
        }


        return copyMe
    }

    fun handleHTTPEvent(e: ActionEvent): StringBuilder {
        val copyMe = StringBuilder()
        val targetRequestResponses: MutableList<HttpRequestResponse>
        if (!currentEvent!!.selectedRequestResponses().isEmpty()) {
            targetRequestResponses = currentEvent!!.selectedRequestResponses()
        } else if (currentEvent!!.messageEditorRequestResponse().isPresent) {
            targetRequestResponses = LinkedList()
            targetRequestResponses.add(currentEvent!!.messageEditorRequestResponse().get().requestResponse())
        } else {
            return copyMe
        }

        if (!targetRequestResponses.isEmpty()) {
            for (requestResponse in targetRequestResponses) {
                if (requestResponse.request() != null) {
                    if (e.actionCommand != _CopyResponseBodyOnlyName) copyMe.append(
                        String.format(
                            "**%s**\n\n",
                            requestResponse.request().url()
                        )
                    )
                    if (e.actionCommand == _CopyRequestAndResponseHeaderName || e.actionCommand == _CopyRequestAndResponseName || e.actionCommand == _CopyRequestAndResponseIncludeName) {
                        var strippedRequest = requestResponse.request()
                        if (stripHeaders(e.actionCommand)) {
                            for (headerName in _requestHeadersToStrip) {
                                if(strippedRequest.hasHeader(headerName)) {
                                    strippedRequest = strippedRequest.withRemovedHeader(headerName)
                                }
                            }
                        }
                        copyMe.append(
                            this.surroundWithMarkdown(
                                _api.utilities().byteUtils().convertToString(strippedRequest.toByteArray().bytes)
                            )
                        )
                    }
                }

                if (requestResponse.response() != null) {
                    var strippedResponse = requestResponse.response()
                    if (stripHeaders(e.actionCommand)) {
                        for (headerName in _responseHeadersToStrip) {
                            if(strippedResponse.hasHeader(headerName)) {
                                strippedResponse = strippedResponse.withRemovedHeader(headerName)
                            }
                        }
                    }

                    if (e.actionCommand == _CopyResponseBodyOnlyName) {
                        copyMe.append(
                            this.surroundWithMarkdown(
                                _api.utilities().byteUtils().convertToString(strippedResponse.body().bytes)
                            )
                        )
                    } else if (e.actionCommand == _CopyRequestAndResponseName || e.actionCommand == _CopyURLAndResponseName || e.actionCommand == _CopyRequestAndResponseIncludeName) {
                        copyMe.append(
                            this.surroundWithMarkdown(
                                _api.utilities().byteUtils().convertToString(strippedResponse.toByteArray().bytes)
                            )
                        )
                    } else {
                        val responseHeaders =
                            Arrays.copyOfRange(strippedResponse.toByteArray().bytes, 0, strippedResponse.bodyOffset())
                        copyMe.append(
                            this.surroundWithMarkdown(
                                _api.utilities().byteUtils().convertToString(responseHeaders)
                            )
                        )
                    }
                }
            }
        }

        return copyMe
    }

    override fun lostOwnership(clipboard: Clipboard, contents: Transferable) {
    }

    private fun stripHeaders(command: String): Boolean {
        return _StripHeadersForTheseCommands.contains(command)
    }

}