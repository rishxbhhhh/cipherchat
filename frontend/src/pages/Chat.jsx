import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import useStomp from '../hooks/useStomp';
import api from '../api/client';
import ConversationList from '../components/ConversationList';
import MessageList from '../components/MessageList';
import MessageInput from '../components/MessageInput';

export default function Chat() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [conversations, setConversations] = useState([]);
  const [activeConv, setActiveConv] = useState(null);
  const [messages, setMessages] = useState([]);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newEmail, setNewEmail] = useState('');
  const [groupEmails, setGroupEmails] = useState('');
  const [groupMode, setGroupMode] = useState(false);
  const [createError, setCreateError] = useState('');

  const onMessage = useCallback((convId, msg) => {
    setMessages((prev) => {
      // Dedupe — STOMP may deliver the same message twice on reconnect
      if (prev.some((m) => m.id === msg.messageId)) return prev;
      return [...prev, { ...msg, id: msg.messageId || Date.now() }];
    });
  }, []);

  const stomp = useStomp(onMessage);

  // Connect STOMP on mount
  useEffect(() => {
    if (user?.token) {
      stomp.connect(user.token);
    }
    return () => stomp.disconnect();
  }, [user?.token]);

  // Load conversations
  useEffect(() => {
    api.get('/conversations').then((res) => {
      setConversations(res.data?.content || res.data || []);
    }).catch(() => {});
  }, []);

  // Load history when conversation selected
  useEffect(() => {
    if (!activeConv) return;
    api.get(`/messages/history?conversationId=${activeConv.id}&page=0&size=50`)
      .then((res) => {
        const history = (res.data?.content || []).reverse();
        setMessages(history);
      })
      .catch(() => {});

    const unsub = stomp.subscribe(activeConv.id);
    return () => unsub?.();
  }, [activeConv?.id]);

  const handleSend = (text) => {
    if (!activeConv) return;
    stomp.send(activeConv.id, text);
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setCreateError('');
    const type = groupMode ? 'GROUP' : 'PRIVATE';
    const emails = (groupMode
      ? groupEmails.split(',').map((s) => s.trim()).filter(Boolean)
      : [newEmail.trim()])
      .map((u) => u.includes('@') ? u : u + '@cc.io');

    if (emails.length === 0) return;

    try {
      const res = await api.post('/conversations/create', {
        type,
        participantEmails: emails,
      });
      const conv = {
        id: res.data.conversationId,
        name: groupMode ? `Group:${res.data.conversationId}` : emails[0],
        type,
      };
      setConversations((prev) => {
        if (prev.find((c) => c.id === conv.id)) return prev;
        return [...prev, conv];
      });
      setActiveConv(conv);
      setShowCreate(false);
      setNewEmail('');
      setGroupEmails('');
      setGroupMode(false);
    } catch (err) {
      setCreateError(err.response?.data?.message || 'Failed to create conversation');
    }
  };

  const handleLogout = async () => {
    stomp.disconnect();
    await logout();
    navigate('/login');
  };

  const handleRename = async (convId, newName) => {
    try {
      await api.put(`/conversations/${convId}/rename`, { name: newName });
      setConversations((prev) =>
        prev.map((c) => (c.id === convId ? { ...c, name: newName } : c))
      );
    } catch {}
  };

  return (
    <div className="h-screen bg-gray-950 flex overflow-hidden" style={{ height: '100dvh' }}>
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="md:hidden fixed inset-0 bg-black/50 z-20"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <ConversationList
        conversations={conversations}
        activeId={activeConv?.id}
        onSelect={setActiveConv}
        onCreate={() => setShowCreate(true)}
        onRename={handleRename}
        show={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />

      {/* Main chat area — header (shrink) + messages (grow) + input (shrink) */}
      <div className="flex-1 flex flex-col min-w-0 min-h-0">
        {/* Top bar */}
        <div className="h-14 border-b border-gray-800 flex items-center px-3 md:px-4 gap-3 shrink-0 bg-gray-900">
          <button
            onClick={() => setSidebarOpen(true)}
            className="md:hidden text-gray-400 hover:text-white p-1"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>

          {activeConv ? (
            <div className="flex items-center gap-2 min-w-0">
              <div className="w-7 h-7 rounded-full bg-gray-700 flex items-center justify-center text-xs font-medium text-gray-300 shrink-0">
                {activeConv.type === 'GROUP' ? '#' : '@'}
              </div>
              <span className="text-white text-sm font-medium truncate">
                {activeConv.name || `Chat ${activeConv.id}`}
              </span>
            </div>
          ) : (
            <span className="text-gray-400 text-sm">Select a conversation</span>
          )}

          <div className="ml-auto flex items-center gap-2">
            <span className="text-gray-500 text-xs hidden sm:block truncate max-w-[120px]">
              {user?.email}
            </span>
            {user?.role === 'ADMIN' && (
              <button
                onClick={() => navigate('/admin')}
                className="text-indigo-400 hover:text-indigo-300 text-sm transition-colors"
              >
                Admin
              </button>
            )}
            <button
              onClick={handleLogout}
              className="text-gray-400 hover:text-red-400 text-sm transition-colors"
            >
              Logout
            </button>
          </div>
        </div>

        {/* Messages + Input */}
        <MessageList messages={messages} currentUserEmail={user?.email} />
        {activeConv && <MessageInput onSend={handleSend} />}

        {!activeConv && (
          <div className="flex-1 flex items-center justify-center text-gray-500 text-sm">
            Select a chat or create a new one to start messaging
          </div>
        )}

        {!activeConv && !sidebarOpen && (
          <div className="md:hidden fixed bottom-16 right-4 z-10">
            <button
              onClick={() => setSidebarOpen(true)}
              className="bg-indigo-600 text-white rounded-full p-3 shadow-lg"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </button>
          </div>
        )}
      </div>

      {/* Create conversation modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center px-4">
          <div className="bg-gray-900 rounded-xl p-6 border border-gray-800 w-full max-w-sm">
            <h3 className="text-white font-semibold mb-4">
              New {groupMode ? 'group' : 'private chat'}
            </h3>

            {/* Toggle */}
            <div className="flex gap-1 mb-3 bg-gray-800 rounded-lg p-0.5 text-sm">
              <button type="button"
                onClick={() => setGroupMode(false)}
                className={`flex-1 py-1.5 rounded-md transition-colors ${!groupMode ? 'bg-indigo-600 text-white' : 'text-gray-400'}`}
              >Private</button>
              <button type="button"
                onClick={() => setGroupMode(true)}
                className={`flex-1 py-1.5 rounded-md transition-colors ${groupMode ? 'bg-indigo-600 text-white' : 'text-gray-400'}`}
              >Group</button>
            </div>

            <form onSubmit={handleCreate} className="space-y-3">
              {createError && (
                <p className="text-red-400 text-sm">{createError}</p>
              )}
              {groupMode ? (
                <input
                  type="text"
                  value={groupEmails}
                  onChange={(e) => setGroupEmails(e.target.value)}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5 text-white text-sm focus:outline-none focus:border-indigo-500"
                  placeholder="user1, user2, user3"
                  autoFocus
                />
              ) : (
                <input
                  type="text"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5 text-white text-sm focus:outline-none focus:border-indigo-500"
                  placeholder="username"
                  required
                  autoFocus
                />
              )}
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => { setShowCreate(false); setCreateError(''); setGroupMode(false); }}
                  className="px-4 py-2 text-gray-400 hover:text-white text-sm transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm rounded-lg transition-colors"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
