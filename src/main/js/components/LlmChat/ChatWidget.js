import React, { Component } from 'react';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faRobot, faTimes, faPaperPlane, faTrash, faCircle, faSpinner } from '@fortawesome/free-solid-svg-icons';
import axios from 'axios';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import './LlmChat.scss';
import { extractSseData, splitSseEvents } from './chatStreamUtils';

// Configure marked for safe, chat-friendly rendering
marked.setOptions({
    breaks: true,
    gfm: true
});

class ChatWidget extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isOpen: false,
            messages: [],
            inputText: '',
            isLoading: false,
            sessionId: this.generateSessionId(),
            llmStatus: null,
            isStreaming: false,
            streamingMessage: ''
        };
        this.messagesEndRef = React.createRef();
        this.inputRef = React.createRef();
    }

    componentDidMount() {
        this.checkLlmStatus();
        this.loadHistory();
    }

    generateSessionId() {
        return 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    checkLlmStatus = async () => {
        try {
            const res = await axios.get('/api/llm/status');
            this.setState({ llmStatus: res.data });
        } catch (err) {
            this.setState({ llmStatus: { status: 'error', enabled: false } });
        }
    }

    loadHistory = async () => {
        try {
            const token = localStorage.jwtToken;
            const headers = token ? { Authorization: token } : {};
            const res = await axios.get('/api/llm/history', { headers });
            if (res.data && res.data.length > 0) {
                this.setState({ messages: res.data });
            }
        } catch (err) {
            // No history available, that's fine
        }
    }

    toggleChat = () => {
        this.setState(prev => ({ isOpen: !prev.isOpen }), () => {
            if (this.state.isOpen && this.inputRef.current) {
                this.inputRef.current.focus();
            }
        });
    }

    scrollToBottom = () => {
        if (this.messagesEndRef.current) {
            this.messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    }

    handleInputChange = (e) => {
        this.setState({ inputText: e.target.value });
    }

    handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            this.sendMessage();
        }
    }

    getCurrentPageContext = () => {
        const path = window.location.pathname;
        const pageMap = {
            '/cuttingPlan': 'Cutting Plan Management',
            '/demande-de-coupe': 'Cutting Request (Demande de Coupe)',
            '/quality': 'Quality Management',
            '/machines': 'Machine Management',
            '/cnc': 'CNC Control',
            '/gamme': 'Gamme Technique',
            '/placement': 'Placement Management',
            '/stock': 'Stock Management',
            '/kpi': 'KPI Dashboard',
            '/rapport': 'Reports',
            '/planDeCharge': 'Plan de Charge',
            '/scheduling': 'Scheduling Dashboard',
            '/': 'Home / Dashboard'
        };

        for (const [key, value] of Object.entries(pageMap)) {
            if (path.includes(key)) return value;
        }
        return path;
    }

    finalizeStreamingMessage = (message, extraFields = {}) => {
        const normalizedMessage = typeof message === 'string'
            ? message.replace(/\s+$/, '')
            : '';

        if (!normalizedMessage.trim()) {
            this.setState({
                isLoading: false,
                isStreaming: false,
                streamingMessage: ''
            }, this.scrollToBottom);
            return;
        }

        const assistantMessage = {
            message: normalizedMessage,
            role: 'assistant',
            timestamp: new Date().toISOString(),
            ...extraFields
        };

        this.setState(prev => ({
            messages: [...prev.messages, assistantMessage],
            isLoading: false,
            isStreaming: false,
            streamingMessage: ''
        }), this.scrollToBottom);
    }

    sendMessage = async () => {
        const { inputText, sessionId, messages } = this.state;
        if (!inputText.trim()) return;

        const userMessage = {
            message: inputText.trim(),
            role: 'user',
            timestamp: new Date().toISOString()
        };

        this.setState({
            messages: [...messages, userMessage],
            inputText: '',
            isLoading: true,
            isStreaming: true,
            streamingMessage: ''
        }, this.scrollToBottom);

        try {
            // Use SSE streaming endpoint
            const response = await fetch('/api/llm/chat/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.jwtToken
                },
                body: JSON.stringify({
                    message: userMessage.message,
                    sessionId: sessionId,
                    pageContext: this.getCurrentPageContext()
                })
            });

            if (!response.ok || !response.body) {
                throw new Error(`Streaming request failed with status ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let fullMessage = '';
            let buffer = '';
            let streamFinalized = false;

            const processEventBlock = (eventBlock) => {
                const data = extractSseData(eventBlock);

                if (!data) {
                    return;
                }

                if (data === '[DONE]') {
                    streamFinalized = true;
                    this.finalizeStreamingMessage(fullMessage);
                    return;
                }

                fullMessage += data;
                this.setState({
                    streamingMessage: fullMessage
                }, this.scrollToBottom);
            };

            while (true) {
                const { done, value } = await reader.read();

                if (done) {
                    buffer += decoder.decode();
                    break;
                }

                buffer += decoder.decode(value, { stream: true });

                const { events, remainingBuffer } = splitSseEvents(buffer);
                buffer = remainingBuffer;
                events.forEach(processEventBlock);
            }

            if (buffer.trim()) {
                processEventBlock(buffer);
            }

            if (!streamFinalized) {
                this.finalizeStreamingMessage(fullMessage);
            }
        } catch (err) {
            // Fallback to non-streaming endpoint
            try {
                const res = await axios.post('/api/llm/chat', {
                    message: userMessage.message,
                    sessionId: sessionId,
                    pageContext: this.getCurrentPageContext()
                });

                this.finalizeStreamingMessage(res.data.message, {
                    modelName: res.data.modelName
                });

            } catch (fallbackErr) {
                this.finalizeStreamingMessage(
                    'Sorry, the AI assistant is not available right now. Please try again later.'
                );
            }
        }
    }

    clearChat = async () => {
        try {
            const token = localStorage.jwtToken;
            const headers = token ? { Authorization: token } : {};
            await axios.delete('/api/llm/history', { headers });
        } catch (err) { /* ignore */ }
        this.setState({
            messages: [],
            streamingMessage: '',
            sessionId: this.generateSessionId()
        });
    }

    renderMessage = (msg, index) => {
        const isUser = msg.role === 'user';
        // Use a stable key combining index and a hash of the message content
        const key = `msg-${index}-${(msg.message || '').length}`;
        return (
            <div key={key} className={`llm-chat-message ${isUser ? 'user' : 'assistant'}`}>
                <div className="message-avatar">
                    {isUser ? (
                        <span className="user-avatar">{(this.props.security?.user?.fullName || 'U')[0]}</span>
                    ) : (
                        <FontAwesomeIcon icon={faRobot} />
                    )}
                </div>
                <div className="message-content">
                    <div className="message-text" dangerouslySetInnerHTML={{ __html: this.formatMessage(msg.message, false) }} />
                </div>
            </div>
        );
    }

    formatMessage = (text, isStreaming = false) => {
        if (!text) return '';
        try {
            // Strip [SQL_QUERY]...[/SQL_QUERY] tags from display (backend handles execution)
            let cleaned = text.replace(/\[SQL_QUERY\][\s\S]*?\[\/SQL_QUERY\]/g, '');
            // Clean up excess whitespace from removed tags
            cleaned = cleaned.replace(/\n{3,}/g, '\n\n').trim();
            if (!cleaned) return '';

            // During streaming, close any unclosed markdown fences to prevent broken rendering
            if (isStreaming) {
                const fenceCount = (cleaned.match(/```/g) || []).length;
                if (fenceCount % 2 !== 0) {
                    cleaned += '\n```';
                }
            }

            const rawHtml = marked.parse(cleaned);
            return DOMPurify.sanitize(rawHtml, {
                ALLOWED_TAGS: [
                    'p', 'br', 'strong', 'b', 'em', 'i', 'code', 'pre',
                    'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
                    'blockquote', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
                    'a', 'span', 'hr', 'del', 'sup', 'sub'
                ],
                ALLOWED_ATTR: ['href', 'target', 'rel', 'class']
            });
        } catch (e) {
            // Fallback to escaped plain text
            return text
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/\n/g, '<br/>');
        }
    }

    getStatusColor = () => {
        const { llmStatus } = this.state;
        if (!llmStatus) return '#999';
        switch (llmStatus.status) {
            case 'ready': return '#4caf50';
            case 'loading': return '#ff9800';
            case 'disabled': return '#999';
            default: return '#f44336';
        }
    }

    render() {
        const { isOpen, messages, inputText, isLoading, llmStatus, isStreaming, streamingMessage } = this.state;

        // Don't render if LLM is disabled
        if (llmStatus && !llmStatus.enabled) return null;

        return (
            <div className="llm-chat-widget">
                {/* Floating Chat Button */}
                {!isOpen && (
                    <button className="llm-chat-fab" onClick={this.toggleChat} title="AI Assistant">
                        <FontAwesomeIcon icon={faRobot} size="lg" />
                        <FontAwesomeIcon icon={faCircle} className="status-dot" style={{ color: this.getStatusColor() }} />
                    </button>
                )}

                {/* Chat Panel */}
                {isOpen && (
                    <div className="llm-chat-panel">
                        {/* Header */}
                        <div className="llm-chat-header">
                            <div className="header-left">
                                <FontAwesomeIcon icon={faRobot} />
                                <span className="header-title">MG-CMS Assistant</span>
                                {llmStatus && (
                                    <span className={`status-badge ${llmStatus.status}`}>
                                        {llmStatus.status === 'loading' && <FontAwesomeIcon icon={faSpinner} spin />}
                                        {llmStatus.status}
                                    </span>
                                )}
                            </div>
                            <div className="header-actions">
                                <button onClick={this.clearChat} title="Clear chat" className="header-btn">
                                    <FontAwesomeIcon icon={faTrash} />
                                </button>
                                <button onClick={this.toggleChat} title="Close" className="header-btn">
                                    <FontAwesomeIcon icon={faTimes} />
                                </button>
                            </div>
                        </div>

                        {/* Messages */}
                        <div className="llm-chat-messages">
                            {messages.length === 0 && !isStreaming && (
                                <div className="chat-welcome">
                                    <FontAwesomeIcon icon={faRobot} size="2x" />
                                    <h4>Hello! I'm the MG-CMS Assistant</h4>
                                    <p>Ask me anything about cutting plans, quality, scheduling, machines, or any MG-CMS feature.</p>
                                    <div className="suggested-questions">
                                        <button onClick={() => this.setState({ inputText: 'How do I create a cutting plan?' }, this.sendMessage)}>
                                            How to create a cutting plan?
                                        </button>
                                        <button onClick={() => this.setState({ inputText: 'Show me today\'s production status' }, this.sendMessage)}>
                                            Today's production status
                                        </button>
                                        <button onClick={() => this.setState({ inputText: 'Explain the ordonnancement process' }, this.sendMessage)}>
                                            Explain ordonnancement
                                        </button>
                                    </div>
                                </div>
                            )}

                            {messages.map(this.renderMessage)}

                            {/* Streaming message */}
                            {isStreaming && streamingMessage && (
                                <div className="llm-chat-message assistant">
                                    <div className="message-avatar">
                                        <FontAwesomeIcon icon={faRobot} />
                                    </div>
                                    <div className="message-content">
                                        <div className="message-text" dangerouslySetInnerHTML={{ __html: this.formatMessage(streamingMessage, true) }} />
                                        <span className="typing-cursor">|</span>
                                    </div>
                                </div>
                            )}

                            {/* Loading indicator */}
                            {isLoading && !streamingMessage && (
                                <div className="llm-chat-message assistant">
                                    <div className="message-avatar">
                                        <FontAwesomeIcon icon={faRobot} />
                                    </div>
                                    <div className="message-content">
                                        <div className="typing-indicator">
                                            <span></span><span></span><span></span>
                                        </div>
                                    </div>
                                </div>
                            )}

                            <div ref={this.messagesEndRef} />
                        </div>

                        {/* Input */}
                        <div className="llm-chat-input">
                            <textarea
                                ref={this.inputRef}
                                value={inputText}
                                onChange={this.handleInputChange}
                                onKeyPress={this.handleKeyPress}
                                placeholder="Ask me anything..."
                                rows="1"
                                disabled={isLoading}
                            />
                            <button
                                onClick={this.sendMessage}
                                disabled={isLoading || !inputText.trim()}
                                className="send-btn"
                            >
                                <FontAwesomeIcon icon={isLoading ? faSpinner : faPaperPlane} spin={isLoading} />
                            </button>
                        </div>

                        {/* Footer */}
                        {llmStatus && llmStatus.modelName && (
                            <div className="llm-chat-footer">
                                Model: {llmStatus.modelName} | RAM: {llmStatus.ramTier}
                            </div>
                        )}
                    </div>
                )}
            </div>
        );
    }
}

const mapStateToProps = state => ({
    security: state.security
});

export default connect(mapStateToProps, null)(ChatWidget);
